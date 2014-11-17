package org.openhim.mediator.xds;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.io.FileNotFoundException;
import java.util.Map;

import javax.xml.bind.JAXBException;

import oasis.names.tc.ebxml_regrep.xsd.query._3.AdhocQueryRequest;

import org.dcm4chee.xds2.infoset.util.InfosetUtil;
import org.junit.Test;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.module.client.MuleClient;
import org.openhim.mediator.orchestration.exceptions.ValidationException;
import org.openhim.mediator.test.Util;

public class XDSQueryTransformerTest {

    @SuppressWarnings("unchecked")
    @Test
    public void testEnrichAdhocQueryRequest() throws FileNotFoundException, JAXBException, ValidationException, MuleException {
        MuleClient mockClient = mock(MuleClient.class);
        MuleMessage mockResponse = Util.buildMockMuleResponse(true, "testECID");
        when(mockClient.send(eq("vm://getecid-pix"), anyMap(), isNull(Map.class), anyInt())).thenReturn(mockResponse);

        AdhocQueryRequest aqRequest = Util.parseRequestFromClass(AdhocQueryRequest.class, "adhocQuery.xml");
        XDSQueryTransformer transformer = new XDSQueryTransformer();
        transformer.setClient(mockClient);
        transformer.enrichAdhocQueryRequest(aqRequest);

        assertEquals("testECID^^^&amp;ECID&amp;ISO", InfosetUtil.getSlotValue(aqRequest.getAdhocQuery().getSlot(), "$XDSDocumentEntryPatientId", null));
    }

}
