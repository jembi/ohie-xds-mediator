package org.openhim.mediator.pixpdq;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.module.client.MuleClient;
import org.openhim.mediator.orchestration.exceptions.ValidationException;
import org.openhim.mediator.test.Util;

public class PixProcessorTest {
    
    @SuppressWarnings("unchecked")
    @Test
    public void testResolveECID() {
        MuleClient mockClient = mock(MuleClient.class);
		Map<String, String> idMap = new HashMap<>();
		idMap.put("id", "1234");
		idMap.put("idType", "TEST");

        try {
            MuleMessage mockResponse = Util.buildMockMuleResponse(true, "testECID");
            when(mockClient.send(eq("vm://getecid-pix"), anyMap(), anyMap(), anyInt())).thenReturn(mockResponse);
            String result = new PixProcessor(mockClient, "1").resolveECID("1234^^^&TEST&ISO");
            assertEquals("testECID", result);
            verify(mockClient).send(eq("vm://getecid-pix"), eq(idMap), anyMap(), anyInt());
        } catch (MuleException | ValidationException e) {
            fail();
            e.printStackTrace();
        }
    }

}
