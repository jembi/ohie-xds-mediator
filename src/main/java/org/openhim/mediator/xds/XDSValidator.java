package org.openhim.mediator.xds;

import ihe.iti.atna.AuditMessage;
import ihe.iti.atna.EventIdentificationType;
import ihe.iti.xds_b._2007.ObjectFactory;
import ihe.iti.xds_b._2007.ProvideAndRegisterDocumentSetRequestType;
import ihe.iti.xds_b._2007.ProvideAndRegisterDocumentSetRequestType.Document;

import java.math.BigInteger;
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
import org.mule.api.transport.PropertyScope;
import org.mule.module.client.MuleClient;
import org.openhim.mediator.ATNAUtil;
import org.openhim.mediator.ATNAUtil.ParticipantObjectDetail;
import org.openhim.mediator.Constants;
import org.openhim.mediator.Util;
import org.openhim.mediator.mule.MediatorMuleTransformer;
import org.openhim.mediator.orchestration.exceptions.ValidationException;
import org.openhim.mediator.pixpdq.PixProcessor;

public class XDSValidator extends MediatorMuleTransformer {
	
	Logger log = Logger.getLogger(this.getClass());
	
	private String ecidAssigningAuthority = "";
	private String homeCommunityId;
	private MuleClient client;
	
	@Override
	public Object onCall(MuleEventContext eventContext) throws Exception {
		MuleContext muleContext = eventContext.getMuleContext();

		this.setClient(new MuleClient(muleContext));

		MuleMessage msg = eventContext.getMessage();
        //getCoreResponseToken will ensure that a correlation id is initialized
        getCoreResponseToken(msg);

		ProvideAndRegisterDocumentSetRequestType pnr = (ProvideAndRegisterDocumentSetRequestType) msg.getPayload();

        RegistryPackageType regPac = InfosetUtil.getRegistryPackage(pnr.getSubmitObjectsRequest(), XDSConstants.UUID_XDSSubmissionSet);
        String uniqueId = InfosetUtil.getExternalIdentifierValue(XDSConstants.UUID_XDSSubmissionSet_uniqueId, regPac);
        String pid = InfosetUtil.getExternalIdentifierValue(XDSConstants.UUID_XDSSubmissionSet_patientId, regPac);

        try {
            //generate audit message
            //we'll audit the original request before enrichment
            String request = Util.marshallJAXBObject("ihe.iti.xds_b._2007", new ObjectFactory().createProvideAndRegisterDocumentSetRequest(pnr), false);
            String sourceIP = msg.getInboundProperty("X-Forwarded-For")!=null ? (String)msg.getInboundProperty("X-Forwarded-For") : "unknown";

            ATNAUtil.dispatchAuditMessage(muleContext, generateATNAMessage(request, pid, uniqueId, sourceIP, true));
            log.info("Dispatched ATNA message");
        } catch (Exception e) {
            //If the auditing breaks, it shouldn't break the flow, so catch and log
            log.error("Failed to dispatch ATNA message", e);
        }
		
		JAXBElement<ProvideAndRegisterDocumentSetRequestType> newPNR = validateAndEnrichPNR(pnr, msg.getCorrelationId());
		msg.setPayload(newPNR);
		
        msg.setProperty("audit", Boolean.TRUE, PropertyScope.SESSION);
        msg.setProperty(Constants.XDS_ITI_41, Util.marshallJAXBObject("ihe.iti.xds_b._2007", newPNR, false), PropertyScope.SESSION);
        msg.setProperty(Constants.XDS_ITI_41_UNIQUEID, uniqueId, PropertyScope.SESSION);
        msg.setProperty(Constants.XDS_ITI_41_PATIENTID, pid, PropertyScope.SESSION);

		return msg;
	}
	
	public JAXBElement<ProvideAndRegisterDocumentSetRequestType> validateAndEnrichPNR(ProvideAndRegisterDocumentSetRequestType pnr, String correlationId)
			throws ValidationException {
		
		validateAndEnrichClient(pnr, correlationId);
		validateProviderAndFacility(pnr, correlationId);
		validateTerminology(pnr);
			
		int size = pnr.getDocument().size();
		log.info("Number of documents in this request: " + size);
		List<Document> docs = pnr.getDocument();
		int n = 1;
		for (Document document : docs) {
			// TODO: make this log.debug
			log.info("Document " + n + " contents" + new String(document.getValue()));
			n++;
		}
		
		ObjectFactory of = new ObjectFactory();

		return of.createProvideAndRegisterDocumentSetRequest(pnr);
	}

	protected void validateAndEnrichClient(ProvideAndRegisterDocumentSetRequestType pnr, String correlationId) throws ValidationException {
		
	    PixProcessor pixProcessor = new PixProcessor(client, correlationId);
		RegistryPackageType regPac = InfosetUtil.getRegistryPackage(pnr.getSubmitObjectsRequest(), XDSConstants.UUID_XDSSubmissionSet);
		String CX = InfosetUtil.getExternalIdentifierValue(XDSConstants.UUID_XDSSubmissionSet_patientId, regPac);
		String submissionECID = pixProcessor.resolveECID(CX);
		String newSubmissionPatCx = submissionECID + "^^^&" + ecidAssigningAuthority + "&ISO";
		InfosetUtil.setExternalIdentifierValue(XDSConstants.UUID_XDSSubmissionSet_patientId, newSubmissionPatCx, regPac);
		
		List<ExtrinsicObjectType> eos = InfosetUtil.getExtrinsicObjects(pnr.getSubmitObjectsRequest());
		for (ExtrinsicObjectType eo : eos) {
			String documentPatCX = InfosetUtil.getExternalIdentifierValue(XDSConstants.UUID_XDSDocumentEntry_patientId, eo);
			
			String docECID = pixProcessor.resolveECID(documentPatCX);
				
			if (docECID == null) {
				throw new ValidationException("Query for client document entry ecid failed.");
			}
			
			String newDocPatCx = docECID + "^^^&" + ecidAssigningAuthority + "&ISO";
			InfosetUtil.setExternalIdentifierValue(XDSConstants.UUID_XDSDocumentEntry_patientId, newDocPatCx, eo);
			
			// TODO: Add to CDA identifier list
		}
	}

	
	protected void validateProviderAndFacility(ProvideAndRegisterDocumentSetRequestType pnr, String correlationId) throws ValidationException {
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
					
					// loop through all values and find the first one with an ID and assigning authority
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
						Map<String, String> epidMap = getEpid(localProviderID, localProviderIDAssigningAuthority, correlationId);
						Map<String, String> elidMap = getElid(localLocationID, localLocationIDAssigningAuthority, correlationId);
						
						if (epidMap == null || elidMap == null) {
							throw new ValidationException("Query for provider or facility failed.");
						} else {
							setValListToEPID(personSlotValList, epidMap);
							setValListToELID(localLocationName, institutionSlotValList, elidMap);
						}
					} catch (MuleException e) {
						log.error(e);
						throw new ValidationException("Query for provider and facility failed.", e);
					}
				} else if (localProviderID != null) { // if we just have a local provider ID
					try {
						Map<String, String> enterpriseIDs = getEpid(localProviderID, localProviderIDAssigningAuthority, correlationId);
						
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
						Map<String, String> enterpriseIDs = getElid(localLocationID, localLocationIDAssigningAuthority, correlationId);
						
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
		String newInstitutionXON = createXON(localLocationName, enterpriseIDs.get(Constants.ELID_MAP_ID), enterpriseIDs.get(Constants.ELID_AUTHORITY_MAP_ID));
		institutionSlotValList.clear();
		institutionSlotValList.add(newInstitutionXON);
	}

	private void setValListToEPID(List<String> personSlotValList,
			Map<String, String> enterpriseIDs) {
		String newPersonXCN = createXCN(enterpriseIDs.get(Constants.EPID_MAP_ID), enterpriseIDs.get(Constants.EPID_AUTHORITY_MAP_ID));
		personSlotValList.clear();
		personSlotValList.add(newPersonXCN);
	}

	protected String createXON(String organisationName, String identifier, String assigingAuthority) {
		return organisationName + "^^^^^&" + assigingAuthority + "&ISO" + "^^^^" + identifier;
	}

	protected String createXCN(String identifier, String assigingAuthority) {
		return identifier + "^^^^^^^^&" + assigingAuthority + "&ISO";
	}

	@SuppressWarnings("unchecked")
	private Map<String, String> getElid(String localLocationID, String localLocationIDAssigningAuthority, String correlationId) throws MuleException {
		Map<String, String> idMap = new HashMap<>();
		Map<String, Object> props = new HashMap<>();
		idMap.put(Constants.LOCAL_LOCATION_MAP_ID, localLocationID);
		idMap.put(Constants.LOCAL_LOCATION_AUTHORITY_MAP_ID, localLocationIDAssigningAuthority);
		props.put("MULE_CORRELATION_ID", correlationId);
		
		MuleMessage response = client.send("vm://get-elid", idMap, props, 5000);
		
		String success = response.getInboundProperty("success");
		if (success != null && success.equals("true")) {
			return (Map<String, String>) response.getPayload();
		} else {
			return null;
		}
	}

	@SuppressWarnings("unchecked")
	private Map<String, String> getEpid(String localProviderID, String localProviderIDAssigningAuthority, String correlationId) throws MuleException {
		Map<String, String> idMap = new HashMap<>();
		Map<String, Object> props = new HashMap<>();
		idMap.put(Constants.LOCAL_PROVIDER_MAP_ID, localProviderID);
		idMap.put(Constants.LOCAL_PROVIDER_AUTHORITY_MAP_ID, localProviderIDAssigningAuthority);
		props.put("MULE_CORRELATION_ID", correlationId);
		
		MuleMessage response = client.send("vm://get-epid", idMap, props, 5000);
		
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

    /* Auditing */
    
    protected String generateATNAMessage(String request, String patientId, String uniqueId, String sourceIP, boolean outcome) throws JAXBException {
        AuditMessage res = new AuditMessage();
        
        EventIdentificationType eid = new EventIdentificationType();
        eid.setEventID( ATNAUtil.buildCodedValueType("DCM", "110107", "Import") );
        eid.setEventActionCode("C");
        eid.setEventDateTime( ATNAUtil.newXMLGregorianCalendar() );
        eid.getEventTypeCode().add( ATNAUtil.buildCodedValueType("IHE Transactions", "ITI-41", "Provide and Register Document Set-b") );
        eid.setEventOutcomeIndicator(outcome ? BigInteger.ONE : BigInteger.ZERO);
        res.setEventIdentification(eid);
        
        res.getActiveParticipant().add( ATNAUtil.buildActiveParticipant(ATNAUtil.WSA_REPLYTO_ANON, "client", true, sourceIP, (short)2, "DCM", "110153", "Source"));
        res.getActiveParticipant().add( ATNAUtil.buildActiveParticipant(ATNAUtil.WSA_REPLYTO_ANON, ATNAUtil.getProcessID(), false, ATNAUtil.getHostIP(), (short)2, "DCM", "110152", "Destination"));
        
        res.getAuditSourceIdentification().add(ATNAUtil.buildAuditSource("openhim"));
        
        res.getParticipantObjectIdentification().add(
            ATNAUtil.buildParticipantObjectIdentificationType(patientId, (short)1, (short)1, "RFC-3881", "2", "PatientNumber", null)
        );
        
        List<ParticipantObjectDetail> pod = new ArrayList<ParticipantObjectDetail>();
        pod.add(new ParticipantObjectDetail("QueryEncoding", "UTF-8".getBytes()));
        if (homeCommunityId!=null) pod.add(new ParticipantObjectDetail("urn:ihe:iti:xca:2010:homeCommunityId", homeCommunityId.getBytes()));
        
        res.getParticipantObjectIdentification().add(
            ATNAUtil.buildParticipantObjectIdentificationType(
                uniqueId, (short)2, (short)20, "IHE XDS Metadata", "urn:uuid:a54d6aa5-d40d-43f9-88c5-b4633d873bdd", "submission set classificationNode", request, pod
            )
        );
        
        return ATNAUtil.marshallATNAObject(res);
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
