package org.openhim.mediator.mule;

import org.mule.RequestContext;
import org.mule.api.MuleMessage;
import org.mule.api.transport.PropertyScope;
import org.mule.transformer.AbstractMessageTransformer;

public abstract class MediatorMuleTransformer extends AbstractMessageTransformer {
    private static final String CORE_RESPONSE_TOKEN_PROP = "core-response-token";

    protected CoreResponseToken getCoreResponseToken() {
        MuleMessage msg = RequestContext.getEvent().getMessage();
        if (msg.getProperty(CORE_RESPONSE_TOKEN_PROP, PropertyScope.SESSION) == null) {
            msg.setProperty(CORE_RESPONSE_TOKEN_PROP, new CoreResponseToken(), PropertyScope.SESSION);
        }
        return msg.getProperty(CORE_RESPONSE_TOKEN_PROP, PropertyScope.SESSION);
    }
}
