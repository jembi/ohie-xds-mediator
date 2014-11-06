package org.openhim.mediator.xds;

import ihe.iti.xds_b._2007.ObjectFactory;
import ihe.iti.xds_b._2007.ProvideAndRegisterDocumentSetRequestType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;

import oasis.names.tc.ebxml_regrep.xsd.rim._3.ClassificationType;
import oasis.names.tc.ebxml_regrep.xsd.rim._3.ExtrinsicObjectType;
import oasis.names.tc.ebxml_regrep.xsd.rim._3.RegistryPackageType;
import oasis.names.tc.ebxml_regrep.xsd.rim._3.SlotType1;

import org.apache.log4j.Logger;
import org.dcm4chee.xds2.common.XDSConstants;
import org.dcm4chee.xds2.infoset.util.InfosetUtil;
import org.mule.api.MuleContext;
import org.mule.api.MuleEventContext;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.lifecycle.Callable;
import org.mule.api.transformer.TransformerException;
import org.mule.module.client.MuleClient;
import org.openhim.mediator.orchestration.exceptions.ValidationException;

public class XDSValidator implements Callable {
	
	Logger log = Logger.getLogger(this.getClass());
	
	private String ecidAssigningAuthority;
	private MuleClient client;
	
	@Override
	public Object onCall(MuleEventContext eventContext) throws Exception {
		MuleContext muleContext = eventContext.getMuleContext();
		this.setClient(new MuleClient(muleContext));
		MuleMessage msg = eventContext.getMessage();
		ProvideAndRegisterDocumentSetRequestType pnr = (ProvideAndRegisterDocumentSetRequestType) msg.getPayload();
		
		JAXBElement<ProvideAndRegisterDocumentSetRequestType> newPNR = validateAndEnrichPNR(pnr);
		msg.setPayload(newPNR);
		
		return msg;
	}
	
	public JAXBElement<ProvideAndRegisterDocumentSetRequestType> validateAndEnrichPNR(ProvideAndRegisterDocumentSetRequestType pnr)
			throws ValidationException {
		
		validateAndEnrichClient(pnr);
		validateProviderAndFacility(pnr);
		validateTerminology(pnr);
			
		int size = pnr.getDocument().size();
		log.info("Number of documents in this request: " + size);
		
		ObjectFactory of = new ObjectFactory();

		return of.createProvideAndRegisterDocumentSetRequest(pnr);
	}

	protected void validateAndEnrichClient(
			ProvideAndRegisterDocumentSetRequestType pnr) throws ValidationException {
		
		RegistryPackageType regPac = InfosetUtil.getRegistryPackage(pnr.getSubmitObjectsRequest(), XDSConstants.UUID_XDSSubmissionSet);
		String submissionPatCX = InfosetUtil.getExternalIdentifierValue(XDSConstants.UUID_XDSSubmissionSet_patientId, regPac);
		
		submissionPatCX.replaceAll("&amp;", "&");
		String patId = submissionPatCX.substring(0, submissionPatCX.indexOf('^'));
		String assigningAuthority = submissionPatCX.substring(submissionPatCX.indexOf('&') + 1, submissionPatCX.lastIndexOf('&'));
		
		String submissionECID = null;
		try {
			submissionECID = sendPIXMessage(patId, assigningAuthority);
			
			if (submissionECID == null) {
				throw new ValidationException("Query for client submission set ecid failed.");
			}
		} catch (MuleException e) {
			log.error(e);
			throw new ValidationException("Query for client submission set ecid failed.", e);
		}
		
		String newSubmissionPatCx = submissionECID + "^^^&amp;" + ecidAssigningAuthority + "&amp;ISO";
		InfosetUtil.setExternalIdentifierValue(XDSConstants.UUID_XDSSubmissionSet_patientId, newSubmissionPatCx, regPac);
		
		List<ExtrinsicObjectType> eos = InfosetUtil.getExtrinsicObjects(pnr.getSubmitObjectsRequest());
		for (ExtrinsicObjectType eo : eos) {
			String documentPatCX = InfosetUtil.getExternalIdentifierValue(XDSConstants.UUID_XDSDocumentEntry_patientId, eo);
			
			documentPatCX.replaceAll("&amp;", "&");
			String docPatId = documentPatCX.substring(0, documentPatCX.indexOf('^'));
			String docAssigningAuthority = documentPatCX.substring(documentPatCX.indexOf('&') + 1, documentPatCX.lastIndexOf('&'));
			
			String docECID = null;
			try {
				docECID = sendPIXMessage(docPatId, docAssigningAuthority);
				
				if (docECID == null) {
					throw new ValidationException("Query for client document entry ecid failed.");
				}
			} catch (MuleException e) {
				log.error(e);
				throw new ValidationException("Query for client document entry ecid failed.", e);
			}
			
			String newDocPatCx = docECID + "^^^&amp;" + ecidAssigningAuthority + "&amp;ISO";
			InfosetUtil.setExternalIdentifierValue(XDSConstants.UUID_XDSDocumentEntry_patientId, newDocPatCx, eo);
			
			// TODO: Add to CDA identifier list
		}
	}

	private String sendPIXMessage(String patId, String assigningAuthority) throws MuleException {
		Map<String, String> idMap = new HashMap<>();
		idMap.put("id", patId);
		idMap.put("idType", assigningAuthority);

		MuleMessage response = client.send("vm://getecid-pix", idMap, null, 5000);
		
		String success = response.getInboundProperty("success");
		if (success != null && success.equals("true")) {
			try {
				return response.getPayloadAsString();
			} catch (Exception ex) {
				log.error(ex);
				return null;
			}
		}
		
		return null;
	}
	
	protected void validateProviderAndFacility(
			ProvideAndRegisterDocumentSetRequestType pnr) throws ValidationException {
		List<ExtrinsicObjectType> eos = InfosetUtil.getExtrinsicObjects(pnr.getSubmitObjectsRequest());
		for (ExtrinsicObjectType eo : eos) {
			List<Map<String, SlotType1>> authorClassSlots = null;
			try {
				authorClassSlots = this.getClassificationSlotsFromExtrinsicObject(XDSConstants.UUID_XDSDocumentEntry_author, eo);
			} catch (JAXBException e) {
				log.error(e);
			}
			
			for (Map<String, SlotType1> slotMap : authorClassSlots) {
				
				String localProviderID = null;
				String localProviderIDAssigningAuthority = null;
				String localLocationID = null;
				String localLocationIDAssigningAuthority = null;
				String localLocationName = null;
				List<String> personSlotValList = null;
				List<String> institutionSlotValList = null;
				
				if (slotMap.containsKey(XDSConstants.SLOT_NAME_AUTHOR_PERSON)) {
					SlotType1 personSlot = slotMap.get(XDSConstants.SLOT_NAME_AUTHOR_PERSON);
					personSlotValList = personSlot.getValueList().getValue();
					
					// loop through all values and find the first one with an ID
					for (String val : personSlotValList) {
						String[] xcnComponents = val.split("\\^", -1);
						
						// if the identifier component exists
						if (!xcnComponents[0].isEmpty() && !xcnComponents[8].isEmpty()) {
							localProviderID = xcnComponents[0];
							localProviderIDAssigningAuthority = xcnComponents[8].substring(xcnComponents[8].indexOf('&') +1 , xcnComponents[8].lastIndexOf('&'));
							break;
						}
					}
				}
				
				if (slotMap.containsKey(XDSConstants.SLOT_NAME_AUTHOR_INSTITUTION)) {
					SlotType1 institutionSlot = slotMap.get(XDSConstants.SLOT_NAME_AUTHOR_INSTITUTION);
					institutionSlotValList = institutionSlot.getValueList().getValue();
					
					// loop through all values and find the first one with an ID
					for (String val : institutionSlotValList) {
						String[] xonComponents = val.split("\\^", -1);
						
						// if the identifier component exists
						if (xonComponents.length >= 10 && !xonComponents[5].isEmpty() && !xonComponents[9].isEmpty()) {
							localLocationID = xonComponents[9];
							localLocationName = xonComponents[0];
							localLocationIDAssigningAuthority = xonComponents[5].substring(xonComponents[5].indexOf('&') +1 , xonComponents[5].lastIndexOf('&'));
						}
					}
					
				}
				
				// if we have both IDs
				if (localProviderID != null && localLocationID != null) {
					try {
						Map<String, String> enterpriseIDs = getEpidElid(localProviderID, localProviderIDAssigningAuthority, localLocationID, localLocationIDAssigningAuthority);
						
						if (enterpriseIDs == null) {
							throw new ValidationException("Query for provider and facility failed.");
						} else {
							setValListToEPID(personSlotValList, enterpriseIDs);
							setValListToELID(localLocationName, institutionSlotValList, enterpriseIDs);
						}
					} catch (MuleException e) {
						log.error(e);
						throw new ValidationException("Query for provider and facility failed.", e);
					}
				} else if (localProviderID != null) { // if we just have a local provider ID
					try {
						Map<String, String> enterpriseIDs = getEpid(localProviderID);
						
						if (enterpriseIDs == null) {
							throw new ValidationException("Query for provider failed.");
						} else {
							setValListToEPID(personSlotValList, enterpriseIDs);
						}
					} catch (MuleException e) {
						log.error(e);
						throw new ValidationException("Query for provider failed.", e);
					}
				} else if (localLocationID != null) { // if we just have a local location ID
					try {
						Map<String, String> enterpriseIDs = getElid(localLocationID);
						
						if (enterpriseIDs == null) {
							throw new ValidationException("Query for location failed.");
						} else {
							setValListToELID(localLocationName, institutionSlotValList, enterpriseIDs);
						}
					} catch (MuleException e) {
						log.error(e);
						throw new ValidationException("Query for location failed.", e);
					}
				} else {
					throw new ValidationException("EPID and ELID could not be extracted from the CDS metadata");
				}
			}
		}
	}

	private void setValListToELID(String localLocationName, List<String> institutionSlotValList,
			Map<String, String> enterpriseIDs) {
		String newInstitutionXON = createXON(localLocationName, enterpriseIDs.get("elid"), enterpriseIDs.get("elidAssigningAuthorityId"));
		institutionSlotValList.clear();
		institutionSlotValList.add(newInstitutionXON);
	}

	private void setValListToEPID(List<String> personSlotValList,
			Map<String, String> enterpriseIDs) {
		String newPersonXCN = createXCN(enterpriseIDs.get("epid"), enterpriseIDs.get("epidAssigningAuthorityId"));
		personSlotValList.clear();
		personSlotValList.add(newPersonXCN);
	}

	protected String createXON(String organisationName, String identifier, String assigingAuthority) {
		return organisationName + "^^^^^&amp;" + assigingAuthority + "&amp;ISO" + "^^^^" + identifier;
	}

	protected String createXCN(String identifier, String assigingAuthority) {
		return identifier + "^^^^^^^^&amp;" + assigingAuthority + "&amp;ISO";
	}

	@SuppressWarnings("unchecked")
	private Map<String, String> getElid(String localLocationID) throws MuleException {
		Map<String, String> idMap = new HashMap<>();
		idMap.put("localFacilityId", localLocationID);
		
		MuleMessage response = client.send("vm://get-elid", idMap, null, 5000);
		
		String success = response.getInboundProperty("success");
		if (success != null && success.equals("true")) {
			return (Map<String, String>) response.getPayload();
		} else {
			return null;
		}
	}

	@SuppressWarnings("unchecked")
	private Map<String, String> getEpid(String localProviderID) throws MuleException {
		Map<String, String> idMap = new HashMap<>();
		idMap.put("localProviderID", localProviderID);
		
		MuleMessage response = client.send("vm://get-epid", idMap, null, 5000);
		
		String success = response.getInboundProperty("success");
		if (success != null && success.equals("true")) {
			return (Map<String, String>) response.getPayload();
		} else {
			return null;
		}
	}

	@SuppressWarnings("unchecked")
	private Map<String, String> getEpidElid(String localProviderID, String localProviderIDAssigningAuthority, String localLocationID, String localLocationIDAssigningAuthority)
			throws MuleException {
		Map<String, String> idMap = new HashMap<>();
		idMap.put("localProviderID", localProviderID);
		idMap.put("localLocationID", localLocationID);
		idMap.put("localProviderIDAssigningAuthority", localProviderIDAssigningAuthority);
		idMap.put("localLocationIDAssigningAuthority", localLocationIDAssigningAuthority);
		
		MuleMessage response = client.send("vm://get-epid-elid", idMap, null, 5000);
		
		String success = response.getInboundProperty("success");
		if (success != null && success.equals("true")) {
			return (Map<String, String>) response.getPayload();
		} else {
			return null;
		}
	}
	
	protected void validateTerminology(
			ProvideAndRegisterDocumentSetRequestType pnr) {
		// TODO Auto-generated method stub
		
	}

	/**
	 * @param classificationScheme - The classification scheme to look for
	 * @param eo - The extrinsic object to process
	 * @return A list of maps, each item in the list represents a classification definition for
	 * this scheme. There may be multiple of these. Each list item contains a map of SlotType1
	 * objects keyed by their slot name.
	 * @throws JAXBException
	 */
	protected List<Map<String, SlotType1>> getClassificationSlotsFromExtrinsicObject(String classificationScheme, ExtrinsicObjectType eo) throws JAXBException {
		List<ClassificationType> classifications = eo.getClassification();
		
		List<Map<String, SlotType1>> classificationMaps = new ArrayList<Map<String, SlotType1>>();
		for (ClassificationType c : classifications) {
			if (c.getClassificationScheme().equals(classificationScheme)) {
				Map<String, SlotType1> slotsFromRegistryObject = InfosetUtil.getSlotsFromRegistryObject(c);
				classificationMaps.add(slotsFromRegistryObject);
			}
		}
		return classificationMaps;
	}
	
	public String getEcidAssigningAuthority() {
		return ecidAssigningAuthority;
	}

	public void setEcidAssigningAuthority(String ecidAssigningAuthority) {
		this.ecidAssigningAuthority = ecidAssigningAuthority;
	}
	
	public MuleClient getClient() {
		return client;
	}

	public void setClient(MuleClient client) {
		this.client = client;
	}
	
}
