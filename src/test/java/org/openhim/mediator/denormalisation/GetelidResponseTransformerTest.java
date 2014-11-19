package org.openhim.mediator.denormalisation;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Map;

import org.junit.Test;
import org.mule.api.MuleMessage;
import org.mule.api.transformer.TransformerException;
import org.openhim.mediator.Constants;

public class GetelidResponseTransformerTest {

	@Test
	public void transformMessage_shouldextractAndSetUUIDAndAppropriateAssigningAuthority() throws TransformerException {
		// Given
		GetelidResponseTransformer t = new GetelidResponseTransformer();
		MuleMessage msgMock = mock(MuleMessage.class);
		when(msgMock.getInvocationProperty("entityID")).thenReturn("urn:uuid:5b311c3b-67f3-4a7b-9a96-b2dd127af828");
		
		// When
		Object object = t.transformMessage(msgMock, "");
		
		// Then
		assertNotNull(object);
		assertTrue(object instanceof Map);
		Map<String, String> map = (Map<String, String>) object;
		assertEquals(map.get(Constants.ELID_MAP_ID), "5b311c3b-67f3-4a7b-9a96-b2dd127af828");
		assertEquals(map.get(Constants.ELID_AUTHORITY_MAP_ID), "2.25");
	}
	
	@Test
	public void transformMessage_shouldextractAndSetOIDAndAppropriateAssigningAuthority() throws TransformerException {
		// Given
		GetelidResponseTransformer t = new GetelidResponseTransformer();
		MuleMessage msgMock = mock(MuleMessage.class);
		when(msgMock.getInvocationProperty("entityID")).thenReturn("urn:oid:1.2.3.55555");
		
		// When
		Object object = t.transformMessage(msgMock, "");
		
		// Then
		assertNotNull(object);
		assertTrue(object instanceof Map);
		Map<String, String> map = (Map<String, String>) object;
		assertEquals(map.get(Constants.ELID_MAP_ID), "55555");
		assertEquals(map.get(Constants.ELID_AUTHORITY_MAP_ID), "1.2.3");
	}

}
