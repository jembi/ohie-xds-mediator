package org.openhim.mediator.denormalisation;

import java.util.Map;

import org.mule.api.MuleMessage;
import org.mule.api.transformer.TransformerException;
import org.mule.transformer.AbstractMessageTransformer;

public class CSDProviderQueryGenerator extends AbstractMessageTransformer {
	
	@Override
	public Object transformMessage(MuleMessage message, String outputEncoding)
			throws TransformerException {
		
		@SuppressWarnings("unchecked")
		Map<String, String> idMap = (Map<String, String>) message.getPayload();
		
		String localProviderID = idMap.get("localProviderID");
		String localProviderIDAssigningAuthority = idMap.get("localProviderIDAssigningAuthority");
		
		String csdTemplate = "<csd:careServicesRequest xmlns='urn:ihe:iti:csd:2013' xmlns:csd='urn:ihe:iti:csd:2013'>"
				+ "	<function urn='urn:ihe:iti:csd:2014:stored-function:provider-search'>"
				+ "		<requestParams>"
				+ "			<otherID code='" + localProviderID + "' assigningAuthorityName='" + localProviderIDAssigningAuthority + "'/>"
				+ "		</requestParams>"
				+ "	</function>"
				+ "</csd:careServicesRequest>";
		
		return csdTemplate;
	}

}
