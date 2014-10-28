package org.openhim.mediator.xds;

import static org.junit.Assert.fail;
import static org.mockito.Matchers.anyMap;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import ihe.iti.xds_b._2007.ProvideAndRegisterDocumentSetRequestType;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import org.junit.Test;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.module.client.MuleClient;

public class XDSValidatorTest {
	
    private <T> T parseRequestFromResourceName(String resourceName) throws JAXBException, FileNotFoundException {
        JAXBContext jaxbContext = JAXBContext.newInstance("ihe.iti.xds_b._2007:oasis.names.tc.ebxml_regrep.xsd.lcm._3:oasis.names.tc.ebxml_regrep.xsd.query._3:oasis.names.tc.ebxml_regrep.xsd.rim._3:oasis.names.tc.ebxml_regrep.xsd.rs._3:org.hl7.v3");
        Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();

        InputStream is = this.getClass().getClassLoader().getResourceAsStream(resourceName);
        JAXBElement<T> request = (JAXBElement<T>) unmarshaller.unmarshal(is);

        return request.getValue();
    }

	@Test
	public void validateAndEnrichClient_shouldSendExpectedPIXRequests() throws FileNotFoundException, JAXBException, MuleException {
		ProvideAndRegisterDocumentSetRequestType pnr = parseRequestFromResourceName("pnr1.xml");
		
		// given
		XDSValidator xdsValidator = new XDSValidator();
		MuleClient muleClient = mock(MuleClient.class);
		MuleMessage mockMuleMessage = mock(MuleMessage.class);
		when(mockMuleMessage.getPayload()).thenReturn("PAYLOAD");
		when(muleClient.send(eq("vm://getecid-pix"), anyObject(), anyMap(), eq(5000))).thenReturn(mockMuleMessage);
		
		xdsValidator.setClient(muleClient);
		
		// when
		xdsValidator.validateAndEnrichClient(pnr);
		
		// then
		Map<String, String> idMap1 = new HashMap<>();
		idMap1.put("id", "1111111111");
		idMap1.put("idType", "1.2.3");
		verify(muleClient).send("vm://getecid-pix", idMap1, null, 5000);
		
		Map<String, String> idMap2 = new HashMap<>();
		idMap2.put("id", "76cc765a442f410");
		idMap2.put("idType", "1.3.6.1.4.1.21367.2005.3.7");
		verify(muleClient).send("vm://getecid-pix", idMap2, null, 5000);
	}

}
