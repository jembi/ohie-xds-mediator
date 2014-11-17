package org.openhim.mediator.xds;


import javax.xml.bind.JAXBException;

import oasis.names.tc.ebxml_regrep.xsd.query._3.AdhocQueryRequest;

import org.apache.log4j.Logger;
import org.dcm4chee.xds2.infoset.util.InfosetUtil;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.transformer.TransformerException;
import org.mule.module.client.MuleClient;
import org.mule.transformer.AbstractMessageTransformer;
import org.openhim.mediator.orchestration.exceptions.ValidationException;
import org.openhim.mediator.pixpdq.PixProcessor;

public class XDSQueryTransformer extends AbstractMessageTransformer {
	
    Logger log = Logger.getLogger(this.getClass());
	
    private MuleClient client;
	
    @Override
    public Object transformMessage(MuleMessage message, String outputEncoding) throws TransformerException {
        try {
            client = new MuleClient(muleContext);
            AdhocQueryRequest aqRequest = (AdhocQueryRequest) message.getPayload();
            enrichAdhocQueryRequest(aqRequest);
            return aqRequest;
        } catch (MuleException | ValidationException | JAXBException ex) {
            throw new TransformerException(this, ex);
        }
    }
    
    protected void enrichAdhocQueryRequest(AdhocQueryRequest aqRequest) throws ValidationException, JAXBException {
        String resolvedECID = null;

        String pid = InfosetUtil.getSlotValue(aqRequest.getAdhocQuery().getSlot(), "$XDSDocumentEntryPatientId", null);
        if (pid==null) {
            throw new ValidationException("No patient identifiers found in XDS.b adhoc query request");
        }

        resolvedECID = new PixProcessor(client).resolveECID(pid);
        if (resolvedECID==null) {
            throw new ValidationException("Failed to resolve patient ECID");
        }

        String ecidCX = resolvedECID + "^^^&amp;ECID&amp;ISO";
        InfosetUtil.addOrOverwriteSlot(aqRequest.getAdhocQuery(), "$XDSDocumentEntryPatientId", ecidCX);
    }

    public MuleClient getClient() {
        return client;
    }

    public void setClient(MuleClient client) {
        this.client = client;
    }
}
