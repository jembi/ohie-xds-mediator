package org.openhim.mediator.mule;

import org.apache.log4j.Logger;
import org.mule.api.MuleMessage;
import org.mule.api.transformer.TransformerException;
import org.openhim.mediator.mule.CoreResponseToken.Orchestration;
import org.openhim.mediator.mule.MediatorMuleTransformer;

public class CoreResponseTokenEnricher extends MediatorMuleTransformer {
    
    Logger log = Logger.getLogger(this.getClass());
    
    private boolean enrichResponse = false;
    private boolean enrichOrchestrationRequest = false;
    private boolean enrichOrchestrationResponse = false;
    private boolean returnTokenAsPayload = false;
    private String orchestrationName;
	

    @Override
    public Object transformMessage(MuleMessage message, String outputEncoding) throws TransformerException {
        try {
            if (enrichResponse) {
            	String ct = (String) message.getOutboundProperty("Content-Type");
                setResponseBody(message, message.getPayloadAsString());
                getCoreResponseToken(message).getResponse().putHeader("Content-Type", ct);
                getCoreResponseToken(message).getResponse().setStatus(getHTTPStatus(message));
                getCoreResponseToken(message).setStatus("Successful");
            }

            if (orchestrationName!=null && !orchestrationName.isEmpty()) {
                Orchestration o = fetchOrchestration(message, orchestrationName);
                if (enrichOrchestrationRequest) {
                    o.getRequest().setMethod(getHTTPMethod(message));
                    o.getRequest().setBody(message.getPayloadAsString());
                } else if (enrichOrchestrationResponse) {
                	String ct = (String) message.getOutboundProperty("Content-Type");
                    o.getResponse().setStatus(getHTTPStatus(message));
                    o.getResponse().putHeader("Content-Type", ct);
                    o.getResponse().setBody(message.getPayloadAsString());
                }
            }
            
            if (returnTokenAsPayload) {
                CoreResponseToken token = getCoreResponseToken(message);
                clearToken(message);
                return token;
            }
        } catch (Exception ex) {
            log.error(ex);
            ex.printStackTrace();
        }

        return message;
    }


    public boolean getEnrichResponse() {
        return enrichResponse;
    }


    public void setEnrichResponse(boolean enrichResponse) {
        this.enrichResponse = enrichResponse;
    }


    public boolean getEnrichOrchestrationRequest() {
        return enrichOrchestrationRequest;
    }


    public void setEnrichOrchestrationRequest(boolean enrichOrchestrationRequest) {
        this.enrichOrchestrationRequest = enrichOrchestrationRequest;
    }


    public boolean getEnrichOrchestrationResponse() {
        return enrichOrchestrationResponse;
    }


    public void setEnrichOrchestrationResponse(boolean enrichOrchestrationResponse) {
        this.enrichOrchestrationResponse = enrichOrchestrationResponse;
    }


    public String getOrchestrationName() {
        return orchestrationName;
    }


    public void setOrchestrationName(String orchestrationName) {
        this.orchestrationName = orchestrationName;
    }


    public boolean getReturnTokenAsPayload() {
        return returnTokenAsPayload;
    }


    public void setReturnTokenAsPayload(boolean returnTokenAsPayload) {
        this.returnTokenAsPayload = returnTokenAsPayload;
    }
}
