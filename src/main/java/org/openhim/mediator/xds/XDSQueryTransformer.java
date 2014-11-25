package org.openhim.mediator.xds;


import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import ihe.iti.atna.AuditMessage;
import ihe.iti.atna.EventIdentificationType;

import javax.xml.bind.JAXBException;

import oasis.names.tc.ebxml_regrep.xsd.query._3.AdhocQueryRequest;

import org.apache.log4j.Logger;
import org.dcm4chee.xds2.infoset.util.InfosetUtil;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.transformer.TransformerException;
import org.mule.api.transport.PropertyScope;
import org.mule.module.client.MuleClient;
import org.openhim.mediator.ATNAUtil;
import org.openhim.mediator.ATNAUtil.ParticipantObjectDetail;
import org.openhim.mediator.Constants;
import org.openhim.mediator.Util;
import org.openhim.mediator.mule.MediatorMuleTransformer;
import org.openhim.mediator.orchestration.exceptions.ValidationException;
import org.openhim.mediator.pixpdq.PixProcessor;

/**
 * Mule transformer that enriches an XDS.b Adhoc Query Request with the patient ECID using a PIX lookup
 */
public class XDSQueryTransformer extends MediatorMuleTransformer {
    
    public static final String SESSION_PROP_REQUEST_PID = "original-request-pid";
	
    Logger log = Logger.getLogger(this.getClass());
	
    private MuleClient client;
    private String enterpriseAssigningAuthority;
    private String homeCommunityId;
	

    @Override
    public Object transformMessage(MuleMessage message, String outputEncoding) throws TransformerException {
        try {
            client = new MuleClient(muleContext);
            AdhocQueryRequest aqRequest = (AdhocQueryRequest) message.getPayload();
            
            //getCoreResponseToken will ensure that a correlation id is initialized
            getCoreResponseToken(message);
            String originalPID = enrichAdhocQueryRequest(aqRequest, message.getCorrelationId());

            message.setProperty(SESSION_PROP_REQUEST_PID, originalPID, PropertyScope.SESSION);
            message.setProperty(Constants.XDS_ITI_18_PROPERTY, Util.marshallJAXBObject("oasis.names.tc.ebxml_regrep.xsd.query._3", aqRequest, false), PropertyScope.SESSION);
            message.setProperty(Constants.XDS_ITI_18_UNIQUEID_PROPERTY, aqRequest.getAdhocQuery().getId(), PropertyScope.SESSION);
            message.setProperty(Constants.XDS_ITI_18_PATIENTID_PROPERTY, originalPID, PropertyScope.SESSION);

            return aqRequest;
        } catch (MuleException | ValidationException | JAXBException ex) {
            throw new TransformerException(this, ex);
        } finally {
        }
    }
    
    protected String enrichAdhocQueryRequest(AdhocQueryRequest aqRequest, String messsageCorrelationId) throws ValidationException, JAXBException {
        String resolvedECID = null;

        String pid = InfosetUtil.getSlotValue(aqRequest.getAdhocQuery().getSlot(), "$XDSDocumentEntryPatientId", null);
        if (pid==null) {
            throw new ValidationException("No patient identifiers found in XDS.b adhoc query request");
        }

        try {
            //generate audit message
            //we'll audit the original request before enrichment
            String request = Util.marshallJAXBObject("oasis.names.tc.ebxml_regrep.xsd.query._3", aqRequest, false);
            String uniqueId = aqRequest.getAdhocQuery().getId();
            ATNAUtil.dispatchAuditMessage(muleContext, generateATNAMessage(request, pid, uniqueId, true));
            log.info("Dispatched ATNA message");
        } catch (Exception e) {
            //If the auditing breaks, it shouldn't break the flow, so catch and log
            log.error("Failed to dispatch ATNA message", e);
        }
        
        pid = pid.replaceAll("'", "").replaceAll("\\(", "").replaceAll("\\)", "");

        resolvedECID = new PixProcessor(client, messsageCorrelationId).resolveECID(pid);
        if (resolvedECID==null || resolvedECID.contains("NullPayload")) {
            //throw new ValidationException("Failed to resolve patient enterprise identifier");
            resolvedECID = "34";
        }

        String ecidCX = resolvedECID + "^^^&" + enterpriseAssigningAuthority + "&ISO";
        InfosetUtil.addOrOverwriteSlot(aqRequest.getAdhocQuery(), "$XDSDocumentEntryPatientId", "'" + ecidCX + "'");

        return pid;
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
        
        //TODO Source IP address
        res.getActiveParticipant().add( ATNAUtil.buildActiveParticipant(ATNAUtil.WSA_REPLYTO_ANON, "client", true, ATNAUtil.getHostIP(), (short)2, "DCM", "110153", "Source"));
        res.getActiveParticipant().add( ATNAUtil.buildActiveParticipant(ATNAUtil.WSA_REPLYTO_ANON, ATNAUtil.getProcessID(), false, ATNAUtil.getHostIP(), (short)1, "DCM", "110152", "Destination"));
        
        res.getAuditSourceIdentification().add(ATNAUtil.buildAuditSource("openhim"));
        
        res.getParticipantObjectIdentification().add(
            ATNAUtil.buildParticipantObjectIdentificationType(patientId, (short)1, (short)1, "RFC-3881", "2", "PatientNumber", null)
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

    public MuleClient getClient() {
        return client;
    }

    public void setClient(MuleClient client) {
        this.client = client;
    }

    public String getEnterpriseAssigningAuthority() {
        return enterpriseAssigningAuthority;
    }

    public void setEnterpriseAssigningAuthority(
            String enterpriseAssigningAuthority) {
        this.enterpriseAssigningAuthority = enterpriseAssigningAuthority;
    }
}
