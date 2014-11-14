package org.openhim.mediator.xds;


import oasis.names.tc.ebxml_regrep.xsd.query._3.AdhocQueryRequest;
import oasis.names.tc.ebxml_regrep.xsd.rim._3.SlotType1;
import oasis.names.tc.ebxml_regrep.xsd.rim._3.ValueListType;

import org.apache.log4j.Logger;
import org.mule.api.MuleContext;
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

	    for (SlotType1 slot : aqRequest.getAdhocQuery().getSlot()) {
	        if ("$XDSDocumentEntryPatientId".equals(slot.getName())) {
	            ValueListType vlt = slot.getValueList();
	            if (vlt.getValue().size()!=0) {
	                String pid = vlt.getValue().get(0);
	                String ecid = new PixProcessor(client).resolveECID(pid);
	                //TODO enrich
	            }
	        }
	    }
	    } catch (MuleException ex) {
	        throw new TransformerException(this, ex);
	    } catch (ValidationException ex) {
	        //TODO validation failure handling
	        log.warn("Validation failure", ex);
        }

		return message;
	}
}
