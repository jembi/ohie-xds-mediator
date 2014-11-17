package org.openhim.mediator.test;

import static org.mockito.Mockito.*;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import org.mule.api.MuleMessage;

public class Util {
    
    public static String getResourceAsString(String resource) throws IOException {
        InputStream is = Util.class.getClassLoader().getResourceAsStream(resource);
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        String         line = null;
        StringBuilder  stringBuilder = new StringBuilder();
        String         ls = System.getProperty("line.separator");

        while((line = reader.readLine()) != null ) {
            stringBuilder.append(line);
            stringBuilder.append(ls);
        }

        return stringBuilder.toString();
    }

    public static MuleMessage buildMockMuleResponse(boolean successful) {
        return buildMockMuleResponse(successful, null);
    }
    
    public static MuleMessage buildMockMuleResponse(boolean successful, Object payload) {
        MuleMessage mockResponse = mock(MuleMessage.class);
        
        when(mockResponse.getInboundProperty(eq("success"))).thenReturn(successful ? "true" : "false");
        try {
            if (payload!=null) {
                if (payload instanceof String)
                    when(mockResponse.getPayloadAsString()).thenReturn((String)payload);
                when(mockResponse.getPayload()).thenReturn(payload);
            }
        } catch (Exception e) { /* Quiet! */ }
        
        return mockResponse;
    }
    
    /**
     * Removes newlines and whitespace around tags
     */
    public static String trimXML(String xml) {
        return xml.replace("\n", "").replaceAll(">\\s*<", "><");
    }
    
	
    @SuppressWarnings("unchecked")
    public static <T> T parseRequestFromResourceName(String resourceName) throws JAXBException, FileNotFoundException {
        JAXBContext jaxbContext = JAXBContext.newInstance("ihe.iti.xds_b._2007:oasis.names.tc.ebxml_regrep.xsd.lcm._3:oasis.names.tc.ebxml_regrep.xsd.query._3:oasis.names.tc.ebxml_regrep.xsd.rim._3:oasis.names.tc.ebxml_regrep.xsd.rs._3:org.hl7.v3");
        Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();

        InputStream is = Util.class.getClassLoader().getResourceAsStream(resourceName);
        JAXBElement<T> request = (JAXBElement<T>) unmarshaller.unmarshal(is);

        return request.getValue();
    }

    public static <T> T parseRequestFromClass(Class<T> clazz, String resource) throws JAXBException, FileNotFoundException {
        JAXBContext jaxbContext = JAXBContext.newInstance("ihe.iti.xds_b._2007:oasis.names.tc.ebxml_regrep.xsd.lcm._3:oasis.names.tc.ebxml_regrep.xsd.query._3:oasis.names.tc.ebxml_regrep.xsd.rim._3:oasis.names.tc.ebxml_regrep.xsd.rs._3:org.hl7.v3");
        Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();

        InputStream is = Util.class.getClassLoader().getResourceAsStream(resource);
        T result = clazz.cast(unmarshaller.unmarshal(is));

        return result;
    }
}