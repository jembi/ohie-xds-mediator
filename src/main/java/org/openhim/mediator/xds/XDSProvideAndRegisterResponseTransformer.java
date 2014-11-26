package org.openhim.mediator.xds;

import java.math.BigInteger;

import ihe.iti.atna.AuditMessage;
import ihe.iti.atna.EventIdentificationType;

import javax.xml.bind.JAXBException;

import org.apache.log4j.Logger;
import org.mule.api.MuleMessage;
import org.mule.api.transformer.TransformerException;
import org.mule.api.transport.PropertyScope;
import org.mule.transformer.AbstractMessageTransformer;
import org.openhim.mediator.ATNAUtil;
import org.openhim.mediator.Constants;

public class XDSProvideAndRegisterResponseTransformer extends
        AbstractMessageTransformer {

    Logger log = Logger.getLogger(this.getClass());
    private String xdsRepositoryHost = "";

    @Override
    public Object transformMessage(MuleMessage message, String outputEncoding)
            throws TransformerException {
        Object audit = message.getProperty("audit", PropertyScope.SESSION);
        if (audit!=null && (Boolean)audit) {
            try {
                //generate audit message
                //TODO determine outcome
                String request = (String)message.getProperty(Constants.XDS_ITI_41, PropertyScope.SESSION);
                String uniqueId = (String)message.getProperty(Constants.XDS_ITI_41_UNIQUEID, PropertyScope.SESSION);
                String patientId = (String)message.getProperty(Constants.XDS_ITI_41_PATIENTID, PropertyScope.SESSION);
                ATNAUtil.dispatchAuditMessage(muleContext, generateATNAMessage(request, patientId, uniqueId, true));
                log.info("Dispatched ATNA message");
            } catch (Exception e) {
                //If the auditing breaks, it shouldn't break the flow, so catch and log
                log.error("Failed to dispatch ATNA message", e);
            }
        }

        return message;
    }

    /* Auditing */
    
    protected String generateATNAMessage(String request, String patientId, String uniqueId, boolean outcome) throws JAXBException {
        AuditMessage res = new AuditMessage();
        
        EventIdentificationType eid = new EventIdentificationType();
        eid.setEventID( ATNAUtil.buildCodedValueType("DCM", "110106", "Export") );
        eid.setEventActionCode("R");
        eid.setEventDateTime( ATNAUtil.newXMLGregorianCalendar() );
        eid.getEventTypeCode().add( ATNAUtil.buildCodedValueType("IHE Transactions", "ITI-41", "Provide and Register Document Set-b") );
        eid.setEventOutcomeIndicator(outcome ? BigInteger.ZERO : new BigInteger("4"));
        res.setEventIdentification(eid);
        
        res.getActiveParticipant().add( ATNAUtil.buildActiveParticipant(ATNAUtil.WSA_REPLYTO_ANON, ATNAUtil.getProcessID(), true, ATNAUtil.getHostIP(), (short)2, "DCM", "110153", "Source"));
        res.getActiveParticipant().add( ATNAUtil.buildActiveParticipant(xdsRepositoryHost, false, xdsRepositoryHost, (short)1, "DCM", "110152", "Destination"));
        
        res.getAuditSourceIdentification().add(ATNAUtil.buildAuditSource("openhim"));
        
        res.getParticipantObjectIdentification().add(
            ATNAUtil.buildParticipantObjectIdentificationType(patientId, (short)1, (short)1, "RFC-3881", "2", "PatientNumber", null)
        );
        res.getParticipantObjectIdentification().add(
            ATNAUtil.buildParticipantObjectIdentificationType(
                uniqueId, (short)2, (short)20, "IHE XDS Metadata", "urn:uuid:a54d6aa5-d40d-43f9-88c5-b4633d873bdd", "submission set classificationNode", request
            )
        );
        
        return ATNAUtil.marshallATNAObject(res);
    }

    public String getXdsRepositoryHost() {
        return xdsRepositoryHost;
    }

    public void setXdsRepositoryHost(String xdsRepositoryHost) {
        this.xdsRepositoryHost = xdsRepositoryHost;
    }

}
