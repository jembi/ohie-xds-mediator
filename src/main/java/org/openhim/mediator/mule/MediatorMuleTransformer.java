package org.openhim.mediator.mule;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.mule.api.MuleEventContext;
import org.mule.api.MuleMessage;
import org.mule.api.lifecycle.Callable;
import org.mule.api.transformer.TransformerException;
import org.mule.api.transport.PropertyScope;
import org.mule.transformer.AbstractMessageTransformer;
import org.openhim.mediator.mule.CoreResponseToken.Orchestration;
import org.openhim.mediator.mule.CoreResponseToken.Request;
import org.openhim.mediator.mule.CoreResponseToken.Response;

public abstract class MediatorMuleTransformer extends AbstractMessageTransformer implements Callable {

    private static final String CORRELATION_ID_PREFIX = "OPENHIM-MEDIATOR-TRX-";
    
    private static final Map<String, CoreResponseToken> tokenStore = new ConcurrentHashMap<>();
    private static final Map<String, String> idStore = new ConcurrentHashMap<>();


	@Override
	public Object onCall(MuleEventContext eventContext) throws Exception {
		MuleMessage msg = eventContext.getMessage();
		return msg;
	}

    @Override
    public Object transformMessage(MuleMessage message, String outputEncoding) throws TransformerException {
        return message;
    }

    protected CoreResponseToken getCoreResponseToken(MuleMessage msg) {
        if (msg.getCorrelationId()==null || !msg.getCorrelationId().startsWith(CORRELATION_ID_PREFIX)) {
            initCorrelationId(msg);
        }

        if (!tokenStore.containsKey(msg.getCorrelationId())) {
            tokenStore.put(msg.getCorrelationId(), new CoreResponseToken());
        }
        return tokenStore.get(msg.getCorrelationId());
    }
    
    /**
     * Initialize a correlation id for the specified message.
     * 
     * The message's root id will be first be used to see if a correlation id
     * is already linked to that unique id, otherwise a new correlation id will be generated.
     */
    private void initCorrelationId(MuleMessage msg) {
        String corrId = idStore.get(msg.getMessageRootId());
        if (corrId!=null) {
            msg.setCorrelationId(corrId);
        } else {
            msg.setCorrelationId(CORRELATION_ID_PREFIX + UUID.randomUUID().toString());
            idStore.put(msg.getMessageRootId(), msg.getCorrelationId());
        }
    }
    
    //Clear out the token from the memory store
    protected void clearToken(MuleMessage message) {
        try {
            tokenStore.remove(message.getCorrelationId());
            idStore.remove(message.getMessageRootId());
        } catch (Exception ex) { /* Quiet you! */ }
    }

    protected void setResponseBody(MuleMessage msg, String payload) {
        Response response = getCoreResponseToken(msg).getResponse();
        if (response==null) {
            response = new Response();
        }
        response.setBody(payload);
        getCoreResponseToken(msg).setResponse(response);
    }
    
    /**
     * Fetch an orchestration from the response token with a particular name.
     * If it is not found, a new orchestration will be created with that name.
     */
    protected Orchestration fetchOrchestration(MuleMessage msg, String orchestrationName) {
        for (Orchestration o : getCoreResponseToken(msg).getOrchestrations()) {
            if (o.getName().equals(orchestrationName)) {
                return o;
            }
        }
        
        Orchestration o = new Orchestration();
        o.setName(orchestrationName);
        o.setRequest(new Request());
        o.setResponse(new Response());
        getCoreResponseToken(msg).addOrchestration(o);
        return o;
    }
    
    protected static Integer getHTTPStatus(MuleMessage msg) {
    	Object status = msg.getProperty("http.status", PropertyScope.OUTBOUND);
    	if (status == null) {
    		return null;
    	} else if (status instanceof Integer) {
    		Integer statusInt = (Integer) status;
    		return statusInt;
    	} else if (status instanceof String) {
    		String statusStr = (String) status;
    		return Integer.parseInt(statusStr);
    	} else {
    		return null;
    	}
    }
    
    protected static String getHTTPMethod(MuleMessage msg) {
        Object method = msg.getProperty("http.method", PropertyScope.INBOUND);
    	if (method != null) {
    		return (String) method;
    	} else {
    		return null;
    	}
    }
}
