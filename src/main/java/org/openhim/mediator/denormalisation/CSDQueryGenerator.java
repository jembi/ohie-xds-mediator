package org.openhim.mediator.denormalisation;

import java.util.Map;

import org.mule.api.MuleMessage;
import org.mule.api.transformer.TransformerException;
import org.mule.transformer.AbstractMessageTransformer;

public class CSDQueryGenerator extends AbstractMessageTransformer {
	
	@Override
	public Object transformMessage(MuleMessage message, String outputEncoding)
			throws TransformerException {
		
		Map<String, String> idMap = (Map<String, String>) message.getPayload();
		
		String localProviderID = idMap.get("localProviderID");
		String localLocationID = idMap.get("localLocationID");
		String localProviderIDAssigningAuthority = idMap.get("localProviderIDAssigningAuthority");
		String localLocationIDAssigningAuthority = idMap.get("localLocationIDAssigningAuthority");
		
		String csdTemplate = "<csd:careServicesRequest xmlns='urn:ihe:iti:csd:2013' xmlns:csd='urn:ihe:iti:csd:2013'>"
				+ "	<function uuid='4e8bbeb9-f5f5-11e2-b778-0800200c9a66'>"
				+ "		<requestParams>"
				//+ "			<id oid='" + localProviderID + "'/>"
				+ "			<otherID code='" + localProviderID + "' assigningAuthorityName='" + localProviderIDAssigningAuthority + "'/>"
				+ "			<facilities>"
				+ "				<facility oid='" + localLocationID + "'/>"
				+ "			</facilities>"
				+ "		</requestParams>"
				+ "	</function>"
				+ "</csd:careServicesRequest>";
		
		return csdTemplate;
	}

}
