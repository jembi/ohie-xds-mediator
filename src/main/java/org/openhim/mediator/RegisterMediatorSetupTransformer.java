package org.openhim.mediator;

import java.io.IOException;

import org.mule.api.MuleMessage;
import org.mule.api.transformer.TransformerException;
import org.mule.transformer.AbstractMessageTransformer;

public class RegisterMediatorSetupTransformer extends AbstractMessageTransformer {
    @Override
    public Object transformMessage(MuleMessage message, String outputEncoding) throws TransformerException {
        try {
            message.setPayload(Util.getResourceAsString("mediator-registration-info.json"));
        } catch (IOException ex) {
            throw new TransformerException(this, ex);
        }
        return message;
    }

}
