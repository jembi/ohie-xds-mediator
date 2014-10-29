package org.openhim.mediator.xds;

import ihe.iti.xds_b._2007.ObjectFactory;
import ihe.iti.xds_b._2007.ProvideAndRegisterDocumentSetRequestType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.bind.JAXBException;

import oasis.names.tc.ebxml_regrep.xsd.rim._3.ClassificationType;
import oasis.names.tc.ebxml_regrep.xsd.rim._3.ExtrinsicObjectType;
import oasis.names.tc.ebxml_regrep.xsd.rim._3.RegistryPackageType;
import oasis.names.tc.ebxml_regrep.xsd.rim._3.SlotType1;

import org.apache.log4j.Logger;
import org.dcm4chee.xds2.common.XDSConstants;
import org.dcm4chee.xds2.infoset.util.InfosetUtil;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.transformer.TransformerException;
import org.mule.module.client.MuleClient;
import org.openhim.mediator.mule.MediatorMuleTransformer;
import org.openhim.mediator.orchestration.exceptions.ClientValidationException;


public class XDSValidator extends MediatorMuleTransformer {
	
	Logger log = Logger.getLogger(this.getClass());
	
	private String ecidAssigningAuthority;
	private MuleClient client;
	
	public XDSValidator() throws MuleException {
		setClient(new MuleClient(this.muleContext));
	}
	
	@Override
	public Object transformMessage(MuleMessage message, String outputEncoding)
			throws TransformerException {
		
		ProvideAndRegisterDocumentSetRequestType pnr = (ProvideAndRegisterDocumentSetRequestType) message.getPayload();
		
		validateAndEnrichClient(pnr);
		validateProviderAndFacility(pnr);
		validateTerminology(pnr);
			
		int size = pnr.getDocument().size();
		log.info("Number of documents in this request: " + size);
		
		ObjectFactory of = new ObjectFactory();
		message.setPayload(of.createProvideAndRegisterDocumentSetRequest(pnr));

		return message;
	}

	protected void validateAndEnrichClient(
			ProvideAndRegisterDocumentSetRequestType pnr) {
		
		RegistryPackageType regPac = InfosetUtil.getRegistryPackage(pnr.getSubmitObjectsRequest(), XDSConstants.UUID_XDSSubmissionSet);
		String submissionPatCX = InfosetUtil.getExternalIdentifierValue(XDSConstants.UUID_XDSSubmissionSet_patientId, regPac);
		
		submissionPatCX.replaceAll("&amp;", "&");
		String patId = submissionPatCX.substring(0, submissionPatCX.indexOf('^'));
		String assigningAuthority = submissionPatCX.substring(submissionPatCX.indexOf('&') + 1, submissionPatCX.lastIndexOf('&'));
		
		String submissionECID = null;
		try {
			submissionECID = sendPIXMessage(patId, assigningAuthority);
		} catch (MuleException e) {
			log.error(e);
		}
		
		String newSubmissionPatCx = submissionECID + "^^^&amp;" + ecidAssigningAuthority + "&amp;ISO";
		InfosetUtil.setExternalIdentifierValue(XDSConstants.UUID_XDSSubmissionSet_patientId, newSubmissionPatCx, regPac);
		
		List<ExtrinsicObjectType> eos = InfosetUtil.getExtrinsicObjects(pnr.getSubmitObjectsRequest());
		for (ExtrinsicObjectType eo : eos) {
			String documentPatCX = InfosetUtil.getExternalIdentifierValue(XDSConstants.UUID_XDSDocumentEntry_patientId, eo);
			
			documentPatCX.replaceAll("&amp;", "&");
			String docPatId = documentPatCX.substring(0, documentPatCX.indexOf('^'));
			String docAssigningAuthority = documentPatCX.substring(documentPatCX.indexOf('&') + 1, documentPatCX.lastIndexOf('&'));
			
			List<Map<String, SlotType1>> authorClassSlots = null;
			try {
				authorClassSlots = this.getClassificationSlotsFromExtrinsicObject(XDSConstants.UUID_XDSDocumentEntry_author, eo);
			} catch (JAXBException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			for (Map<String, SlotType1> slotMap : authorClassSlots) {
				
			}
			
			try {
				sendPIXMessage(docPatId, docAssigningAuthority);
			} catch (MuleException e) {
				log.error(e);
			}
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
			ProvideAndRegisterDocumentSetRequestType pnr) {
		// TODO Auto-generated method stub
		
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
	private List<Map<String, SlotType1>> getClassificationSlotsFromExtrinsicObject(String classificationScheme, ExtrinsicObjectType eo) throws JAXBException {
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
