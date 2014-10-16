package org.openhim.mediator.xds;

import ihe.iti.xds_b._2007.ObjectFactory;
import ihe.iti.xds_b._2007.ProvideAndRegisterDocumentSetRequestType;

import org.apache.log4j.Logger;
import org.mule.api.MuleMessage;
import org.mule.api.transformer.TransformerException;
import org.openhim.mediator.mule.MediatorMuleTransformer;


public class ValidateAndEnrichClient extends MediatorMuleTransformer {
	
	Logger log = Logger.getLogger(this.getClass());
	
	@Override
	public Object transformMessage(MuleMessage message, String outputEncoding)
			throws TransformerException {
		/*try {
			MuleClient client = new MuleClient(this.muleContext);
			
			String id = "test";
			String idType = "test";
			
			Map<String, String> idMap = new HashMap<String, String>();
			idMap.put("id", id);
			idMap.put("idType", idType);
			
			MuleMessage response = client.send("vm://getecid-pix", idMap, null, 5000);
			
			String success = response.getInboundProperty("success");
			if (success != null && success.equals("true")) {
				// do something
			}
			
			return null;
		} catch (MuleException e) {
			throw new TransformerException(this, e.getCause());
		}*/
		
		ProvideAndRegisterDocumentSetRequestType pnr = (ProvideAndRegisterDocumentSetRequestType) message.getPayload();
		
		int size = pnr.getDocument().size();
		log.info("Number of documents in this request: " + size);
		
		ObjectFactory of = new ObjectFactory();
		message.setPayload(of.createProvideAndRegisterDocumentSetRequest(pnr));
		return message;
	}

}
