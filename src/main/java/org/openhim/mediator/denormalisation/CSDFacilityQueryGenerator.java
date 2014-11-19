package org.openhim.mediator.denormalisation;

import java.util.Map;

import org.mule.api.MuleMessage;
import org.mule.api.transformer.TransformerException;
import org.mule.transformer.AbstractMessageTransformer;

public class CSDFacilityQueryGenerator extends AbstractMessageTransformer {
	
	@Override
	public Object transformMessage(MuleMessage message, String outputEncoding)
			throws TransformerException {
		
		@SuppressWarnings("unchecked")
		Map<String, String> idMap = (Map<String, String>) message.getPayload();
		
		String localLocationID = idMap.get("localLocationID");
		String localLocationIDAssigningAuthority = idMap.get("localLocationIDAssigningAuthority");
		
		String csdTemplate = "<csd:careServicesRequest xmlns='urn:ihe:iti:csd:2013' xmlns:csd='urn:ihe:iti:csd:2013'>"
				+ "	<function urn='urn:ihe:iti:csd:2014:stored-function:facility-search'>"
				+ "		<requestParams>"
				+ "			<otherID code='" + localLocationID + "' assigningAuthorityName='" + localLocationIDAssigningAuthority + "'/>"
				+ "		</requestParams>"
				+ "	</function>"
				+ "</csd:careServicesRequest>";
		
		return csdTemplate;
	}

}
