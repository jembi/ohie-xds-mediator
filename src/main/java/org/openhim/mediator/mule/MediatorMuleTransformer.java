package org.openhim.mediator.mule;

import org.mule.RequestContext;
import org.mule.api.MuleMessage;
import org.mule.api.transport.PropertyScope;
import org.mule.transformer.AbstractMessageTransformer;
import org.openhim.mediator.mule.CoreResponseToken.Orchestration;
import org.openhim.mediator.mule.CoreResponseToken.Request;
import org.openhim.mediator.mule.CoreResponseToken.Response;

@SuppressWarnings("deprecation")
public abstract class MediatorMuleTransformer extends AbstractMessageTransformer {
    private static final String CORE_RESPONSE_TOKEN_PROP = "core-response-token";

    protected CoreResponseToken getCoreResponseToken() {
        MuleMessage msg = RequestContext.getEvent().getMessage();
        if (msg.getProperty(CORE_RESPONSE_TOKEN_PROP, PropertyScope.SESSION) == null) {
            msg.setProperty(CORE_RESPONSE_TOKEN_PROP, new CoreResponseToken(), PropertyScope.SESSION);
        }
        return msg.getProperty(CORE_RESPONSE_TOKEN_PROP, PropertyScope.SESSION);
    }

    protected void setResponseBody(String payload) {
        Response response = getCoreResponseToken().getResponse();
        if (response==null) {
            response = new Response();
        }
        response.setBody(payload);
        getCoreResponseToken().setResponse(response);
    }
    
    /**
     * Fetch an orchestration from the response token with a particular name.
     * If it is not found, a new orchestration will be created with that name.
     */
    protected Orchestration fetchOrchestration(String orchestrationName) {
        for (Orchestration o : getCoreResponseToken().getOrchestrations()) {
            if (o.getName().equals(orchestrationName)) {
                return o;
            }
        }
        
        Orchestration o = new Orchestration();
        o.setName(orchestrationName);
        o.setRequest(new Request());
        o.setResponse(new Response());
        getCoreResponseToken().addOrchestration(o);
        return o;
    }
    
    protected static String getHTTPStatus(MuleMessage msg) {
        return (String)msg.getProperty("http.status", PropertyScope.INBOUND);
    }
    
    protected static String getHTTPMethod(MuleMessage msg) {
        return (String)msg.getProperty("http.method", PropertyScope.INBOUND);
    }
}
