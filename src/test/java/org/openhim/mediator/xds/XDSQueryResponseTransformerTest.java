package org.openhim.mediator.xds;

import static org.junit.Assert.*;

import java.io.FileNotFoundException;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;

import oasis.names.tc.ebxml_regrep.xsd.query._3.AdhocQueryResponse;
import oasis.names.tc.ebxml_regrep.xsd.rim._3.ExtrinsicObjectType;
import oasis.names.tc.ebxml_regrep.xsd.rim._3.IdentifiableType;

import org.dcm4chee.xds2.common.XDSConstants;
import org.dcm4chee.xds2.infoset.util.InfosetUtil;
import org.junit.Test;
import org.openhim.mediator.orchestration.exceptions.ValidationException;
import org.openhim.mediator.test.Util;

public class XDSQueryResponseTransformerTest {

    @Test
    public void testDerichAdhocQueryResponse() throws FileNotFoundException, JAXBException, ValidationException {
        final String TEST_PID = "1234^^^&1.2.3.4&ISO";
        AdhocQueryResponse testResponse = Util.parseRequestFromClass(AdhocQueryResponse.class, "adhocQueryResponse.xml");
        XDSQueryResponseTransformer transformer = new XDSQueryResponseTransformer();

        transformer.derichAdhocQueryResponse(TEST_PID, testResponse);
		
		for (JAXBElement<? extends IdentifiableType> elem : testResponse.getRegistryObjectList().getIdentifiable()) {
		    if (elem.getValue() instanceof ExtrinsicObjectType) {
		        String value1 = InfosetUtil.getSlotValue(((ExtrinsicObjectType)elem.getValue()).getSlot(), "sourcePatientId", null);
		        assertEquals(TEST_PID, value1);
		        String value2 = InfosetUtil.getExternalIdentifierValue(XDSConstants.UUID_XDSDocumentEntry_patientId, (ExtrinsicObjectType)elem.getValue());
		        assertEquals(TEST_PID, value2);
		    }
		}
    }

}
