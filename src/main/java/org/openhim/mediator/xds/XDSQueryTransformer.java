package org.openhim.mediator.xds;


import javax.xml.bind.JAXBException;

import oasis.names.tc.ebxml_regrep.xsd.query._3.AdhocQueryRequest;

import org.apache.log4j.Logger;
import org.dcm4chee.xds2.infoset.util.InfosetUtil;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.transformer.TransformerException;
import org.mule.api.transport.PropertyScope;
import org.mule.module.client.MuleClient;
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
        }
    }
    
    protected String enrichAdhocQueryRequest(AdhocQueryRequest aqRequest, String messsageCorrelationId) throws ValidationException, JAXBException {
        String resolvedECID = null;

        String pid = InfosetUtil.getSlotValue(aqRequest.getAdhocQuery().getSlot(), "$XDSDocumentEntryPatientId", null);
        if (pid==null) {
            throw new ValidationException("No patient identifiers found in XDS.b adhoc query request");
        }
        
        pid = pid.replaceAll("'", "").replaceAll("\\(", "").replaceAll("\\)", "");

        resolvedECID = new PixProcessor(client, messsageCorrelationId).resolveECID(pid);
        if (resolvedECID==null || resolvedECID.contains("NullPayload")) {
            throw new ValidationException("Failed to resolve patient enterprise identifier");
        }

        String ecidCX = resolvedECID + "^^^&" + enterpriseAssigningAuthority + "&ISO";
        InfosetUtil.addOrOverwriteSlot(aqRequest.getAdhocQuery(), "$XDSDocumentEntryPatientId", "'" + ecidCX + "'");

        return pid;
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
