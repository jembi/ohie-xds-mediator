package org.openhim.mediator.denormalisation;
import java.util.HashMap;
import java.util.Map;

import org.mule.api.MuleMessage;
import org.mule.api.transformer.TransformerException;
import org.mule.transformer.AbstractMessageTransformer;
import org.openhim.mediator.Constants;
import org.openhim.mediator.orchestration.exceptions.ValidationException;


public class GetepidResponseTransformer extends AbstractMessageTransformer {

	@Override
	public Object transformMessage(MuleMessage message, String outputEncoding)
			throws TransformerException {
		String entityID = message.getInvocationProperty("entityID");
		
		Map<String, String> resultMap = new HashMap<>();
	
		if (entityID.startsWith("urn:uuid:")) {
			entityID = entityID.replace("urn:uuid:", "");
			resultMap.put(Constants.EPID_MAP_ID, entityID);
			resultMap.put(Constants.EPID_AUTHORITY_MAP_ID, Constants.UUID_OID_AUTHORITY);
		} else if (entityID.startsWith("urn:oid:")) {
			entityID = entityID.replace("urn:oid:", "");
			resultMap.put(Constants.EPID_MAP_ID, entityID.substring(entityID.lastIndexOf('.') + 1));
			resultMap.put(Constants.EPID_AUTHORITY_MAP_ID, entityID.substring(0, entityID.lastIndexOf('.')));
		} else {
			throw new TransformerException(this, new ValidationException("Unsupported id recieved."));
		}
		
		return resultMap;
	}

}
