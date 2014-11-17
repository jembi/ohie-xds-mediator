package org.openhim.mediator.xds;


import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;

import oasis.names.tc.ebxml_regrep.xsd.query._3.AdhocQueryResponse;
import oasis.names.tc.ebxml_regrep.xsd.rim._3.ExtrinsicObjectType;
import oasis.names.tc.ebxml_regrep.xsd.rim._3.IdentifiableType;

import org.apache.log4j.Logger;
import org.dcm4chee.xds2.common.XDSConstants;
import org.dcm4chee.xds2.infoset.util.InfosetUtil;
import org.mule.api.MuleMessage;
import org.mule.api.transformer.TransformerException;
import org.mule.api.transport.PropertyScope;
import org.mule.transformer.AbstractMessageTransformer;
import org.openhim.mediator.orchestration.exceptions.ValidationException;

/**
 * Mule transformer that "deriches" an XDS.b Adhoc Query Response, replacing the ECID with the original requested patient ID
 * 
 * @see XDSQueryTransformer
 */
public class XDSQueryResponseTransformer extends AbstractMessageTransformer {
	
    Logger log = Logger.getLogger(this.getClass());
	
	
    @Override
    public Object transformMessage(MuleMessage message, String outputEncoding) throws TransformerException {
        try {
            AdhocQueryResponse aqRequest = (AdhocQueryResponse) message.getPayload();
            String pid = message.getProperty(XDSQueryTransformer.SESSION_PROP_REQUEST_PID, PropertyScope.SESSION);
            derichAdhocQueryResponse(pid, aqRequest);
            return aqRequest;
        } catch (ValidationException | JAXBException ex) {
            throw new TransformerException(this, ex);
        }
    }
    
    protected void derichAdhocQueryResponse(String pid, AdhocQueryResponse aqResponse) throws ValidationException, JAXBException {
		for (JAXBElement<? extends IdentifiableType> elem : aqResponse.getRegistryObjectList().getIdentifiable()) {
		    if (elem.getValue() instanceof ExtrinsicObjectType) {
		        InfosetUtil.addOrOverwriteSlot(((ExtrinsicObjectType)elem.getValue()), "sourcePatientId", pid);
		        InfosetUtil.setExternalIdentifierValue(XDSConstants.UUID_XDSDocumentEntry_patientId, pid, (ExtrinsicObjectType)elem.getValue());
		    }
		}
    }
}
