package org.openhim.mediator.xds;


import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import ihe.iti.atna.AuditMessage;
import ihe.iti.atna.EventIdentificationType;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;

import oasis.names.tc.ebxml_regrep.xsd.query._3.AdhocQueryResponse;
import oasis.names.tc.ebxml_regrep.xsd.rim._3.ExtrinsicObjectType;
import oasis.names.tc.ebxml_regrep.xsd.rim._3.IdentifiableType;

import org.apache.log4j.Logger;
import org.dcm4chee.xds2.common.XDSConstants;
import org.dcm4chee.xds2.infoset.util.InfosetUtil;
import org.mule.api.MuleMessage;
import org.mule.api.transformer.TransformerException;
import org.mule.api.transport.PropertyScope;
import org.mule.transformer.AbstractMessageTransformer;
import org.openhim.mediator.ATNAUtil;
import org.openhim.mediator.ATNAUtil.ParticipantObjectDetail;
import org.openhim.mediator.Constants;
import org.openhim.mediator.orchestration.exceptions.ValidationException;

/**
 * Mule transformer that "deriches" an XDS.b Adhoc Query Response, replacing the ECID with the original requested patient ID
 * 
 * @see XDSQueryTransformer
 */
public class XDSQueryResponseTransformer extends AbstractMessageTransformer {
	
    Logger log = Logger.getLogger(this.getClass());
	
    private String xdsRegistryHost = "";
    private String xdsRegistryPath = "";
    private String xdsRegistryPort = "";
    private String xdsRegistrySecurePort = "";
    private String iheSecure = "";
    private String requestedAssigningAuthority = "";
    private String homeCommunityId;

	
    @Override
    public Object transformMessage(MuleMessage message, String outputEncoding) throws TransformerException {
        try {
            AdhocQueryResponse aqRequest = (AdhocQueryResponse) message.getPayload();
            String pid = message.getProperty(XDSQueryTransformer.SESSION_PROP_REQUEST_PID, PropertyScope.SESSION);
            derichAdhocQueryResponse(pid, aqRequest);
            return aqRequest;
        } catch (ValidationException | JAXBException ex) {
            throw new TransformerException(this, ex);
        } finally {
            try {
                //generate audit message
                String request = (String)message.getProperty(Constants.XDS_ITI_18_PROPERTY, PropertyScope.SESSION);
                String uniqueId = (String)message.getProperty(Constants.XDS_ITI_18_UNIQUEID_PROPERTY, PropertyScope.SESSION);
                String patientId = (String)message.getProperty(Constants.XDS_ITI_18_PATIENTID_PROPERTY, PropertyScope.SESSION);
                ATNAUtil.dispatchAuditMessage(muleContext, generateATNAMessage(request, patientId, uniqueId, true));
                log.info("Dispatched ATNA message");
            } catch (Exception e) {
                //If the auditing breaks, it shouldn't break the flow, so catch and log
                log.error("Failed to dispatch ATNA message", e);
            }
        }
    }
    
    protected void derichAdhocQueryResponse(String pid, AdhocQueryResponse aqResponse) throws ValidationException, JAXBException {
		for (JAXBElement<? extends IdentifiableType> elem : aqResponse.getRegistryObjectList().getIdentifiable()) {
		    if (elem.getValue() instanceof ExtrinsicObjectType) {
		        InfosetUtil.addOrOverwriteSlot(((ExtrinsicObjectType)elem.getValue()), "sourcePatientId", pid);
		        InfosetUtil.setExternalIdentifierValue(XDSConstants.UUID_XDSDocumentEntry_patientId, pid, (ExtrinsicObjectType)elem.getValue());
		    }
		}
    }

    /* Auditing */
    
    protected String generateATNAMessage(String request, String patientId, String uniqueId, boolean outcome) throws JAXBException {
        AuditMessage res = new AuditMessage();
        
        EventIdentificationType eid = new EventIdentificationType();
        eid.setEventID( ATNAUtil.buildCodedValueType("DCM", "110112", "Query") );
        eid.setEventActionCode("E");
        eid.setEventDateTime( ATNAUtil.newXMLGregorianCalendar() );
        eid.getEventTypeCode().add( ATNAUtil.buildCodedValueType("IHE Transactions", "ITI-18", "Registry Stored Query") );
        eid.setEventOutcomeIndicator(outcome ? BigInteger.ONE : BigInteger.ZERO);
        res.setEventIdentification(eid);
        
        res.getActiveParticipant().add( ATNAUtil.buildActiveParticipant(ATNAUtil.WSA_REPLYTO_ANON, ATNAUtil.getProcessID(), true, ATNAUtil.getHostIP(), (short)2, "DCM", "110153", "Source"));
        res.getActiveParticipant().add( ATNAUtil.buildActiveParticipant(buildRegistryPath(), xdsRegistryHost, false, xdsRegistryHost, (short)1, "DCM", "110152", "Destination"));
        
        res.getAuditSourceIdentification().add(ATNAUtil.buildAuditSource("openhim"));
        
        res.getParticipantObjectIdentification().add(
            ATNAUtil.buildParticipantObjectIdentificationType(String.format("%s^^^&%s&ISO", patientId, requestedAssigningAuthority), (short)1, (short)1, "RFC-3881", "2", "PatientNumber", null)
        );
        
        List<ParticipantObjectDetail> pod = new ArrayList<ParticipantObjectDetail>();
        pod.add(new ParticipantObjectDetail("QueryEncoding", "UTF-8".getBytes()));
        if (homeCommunityId!=null) pod.add(new ParticipantObjectDetail("urn:ihe:iti:xca:2010:homeCommunityId", homeCommunityId.getBytes()));
        
        res.getParticipantObjectIdentification().add(
            ATNAUtil.buildParticipantObjectIdentificationType(
                uniqueId, (short)2, (short)24, "IHE Transactions", "ITI-18", "Registry Stored Query", request, pod
            )
        );
        
        return ATNAUtil.marshallATNAObject(res);
    }

    private String buildRegistryPath() {
        return String.format("%s:%s/%s", xdsRegistryHost, ((iheSecure.equalsIgnoreCase("true")) ? xdsRegistrySecurePort : xdsRegistryPort), xdsRegistryPath);
    }

    public String getXdsRegistryHost() {
        return xdsRegistryHost;
    }

    public void setXdsRegistryHost(String xdsRegistryHost) {
        this.xdsRegistryHost = xdsRegistryHost;
    }

    public String getXdsRegistryPath() {
        return xdsRegistryPath;
    }

    public void setXdsRegistryPath(String xdsRegistryPath) {
        this.xdsRegistryPath = xdsRegistryPath;
    }

    public String getXdsRegistryPort() {
        return xdsRegistryPort;
    }

    public void setXdsRegistryPort(String xdsRegistryPort) {
        this.xdsRegistryPort = xdsRegistryPort;
    }

    public String getXdsRegistrySecurePort() {
        return xdsRegistrySecurePort;
    }

    public void setXdsRegistrySecurePort(String xdsRegistrySecurePort) {
        this.xdsRegistrySecurePort = xdsRegistrySecurePort;
    }

    public String getIheSecure() {
        return iheSecure;
    }

    public void setIheSecure(String iheSecure) {
        this.iheSecure = iheSecure;
    }

    public String getRequestedAssigningAuthority() {
        return requestedAssigningAuthority;
    }

    public void setRequestedAssigningAuthority(String requestedAssigningAuthority) {
        this.requestedAssigningAuthority = requestedAssigningAuthority;
    }

    public String getHomeCommunityId() {
        return homeCommunityId;
    }

    public void setHomeCommunityId(String homeCommunityId) {
        this.homeCommunityId = homeCommunityId;
    }
}
