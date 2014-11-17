package org.openhim.mediator.xds;


import oasis.names.tc.ebxml_regrep.xsd.query._3.AdhocQueryRequest;
import oasis.names.tc.ebxml_regrep.xsd.rim._3.SlotType1;
import oasis.names.tc.ebxml_regrep.xsd.rim._3.ValueListType;

import org.apache.log4j.Logger;
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
        } catch (MuleException|ValidationException ex) {
            throw new TransformerException(this, ex);
        }
    }
    
    protected void enrichAdhocQueryRequest(AdhocQueryRequest aqRequest) throws ValidationException {
        for (SlotType1 slot : aqRequest.getAdhocQuery().getSlot()) {
            if ("$XDSDocumentEntryPatientId".equals(slot.getName())) {
                ValueListType vlt = slot.getValueList();
                if (vlt.getValue().size()!=0) {
                    String pid = vlt.getValue().get(0);
                    String ecid = new PixProcessor(client).resolveECID(pid);
                    break;
                }
            }
        }
    }
}
