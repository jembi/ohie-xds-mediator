package org.openhim.mediator.pixpdq;

import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.module.client.MuleClient;
import org.openhim.mediator.orchestration.exceptions.ValidationException;

public class PixProcessor {

	Logger log = Logger.getLogger(this.getClass());

	private MuleClient client;
	private String correlationId;


	public PixProcessor(MuleClient client, String correlationId) {
        this.client = client;
        this.correlationId = correlationId;
    }


    public String resolveECID(String CX) throws ValidationException {
        String submissionPatCX = CX.replaceAll("&amp;", "&");
		String patId = submissionPatCX.substring(0, submissionPatCX.indexOf('^'));
		String assigningAuthority = submissionPatCX.substring(submissionPatCX.indexOf('&') + 1, submissionPatCX.lastIndexOf('&'));
		
		try {
			String ecid = sendPIXMessage(patId, assigningAuthority);
			if (ecid == null) {
				throw new ValidationException("Query for client submission set ecid failed.");
			}
			return ecid;
		} catch (MuleException e) {
			log.error(e);
			throw new ValidationException("Query for client submission set ecid failed.", e);
		}
	}

	private String sendPIXMessage(String patId, String assigningAuthority) throws MuleException {
		Map<String, String> idMap = new HashMap<>();
		Map<String, Object> props = new HashMap<>();
		idMap.put("id", patId);
		idMap.put("idType", assigningAuthority);
		props.put("MULE_CORRELATION_ID", correlationId);

		MuleMessage response = client.send("vm://getecid-pix", idMap, props, 5000);
		
		String success = response.getInboundProperty("success");
		if (success != null && success.equals("true")) {
			try {
				return response.getPayloadAsString();
			} catch (Exception ex) {
				log.error(ex);
				return null;
			}
		}
		
		return null;
	}
}
