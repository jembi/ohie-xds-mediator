package org.openhim.mediator.xds;

import java.util.HashMap;
import java.util.Map;

import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.transformer.TransformerException;
import org.mule.module.client.MuleClient;
import org.openhim.mediator.mule.MediatorMuleTransformer;


public class ValidateAndEnrichClient extends MediatorMuleTransformer {
	
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
		return message;
	}

}
