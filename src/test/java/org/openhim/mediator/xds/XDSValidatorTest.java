package org.openhim.mediator.xds;

import static org.junit.Assert.*;
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

import oasis.names.tc.ebxml_regrep.xsd.rim._3.ExtrinsicObjectType;
import oasis.names.tc.ebxml_regrep.xsd.rim._3.RegistryPackageType;

import org.dcm4chee.xds2.common.XDSConstants;
import org.dcm4chee.xds2.infoset.util.InfosetUtil;
import org.junit.Test;
import org.mule.api.MuleMessage;
import org.mule.module.client.MuleClient;
import org.openhim.mediator.orchestration.exceptions.ValidationException;

public class XDSValidatorTest {
	
    private <T> T parseRequestFromResourceName(String resourceName) throws JAXBException, FileNotFoundException {
        JAXBContext jaxbContext = JAXBContext.newInstance("ihe.iti.xds_b._2007:oasis.names.tc.ebxml_regrep.xsd.lcm._3:oasis.names.tc.ebxml_regrep.xsd.query._3:oasis.names.tc.ebxml_regrep.xsd.rim._3:oasis.names.tc.ebxml_regrep.xsd.rs._3:org.hl7.v3");
        Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();

        InputStream is = this.getClass().getClassLoader().getResourceAsStream(resourceName);
        JAXBElement<T> request = (JAXBElement<T>) unmarshaller.unmarshal(is);

        return request.getValue();
    }

	@Test
	public void validateAndEnrichClient_shouldSendExpectedPIXRequests() throws Exception {
		// given
		ProvideAndRegisterDocumentSetRequestType pnr = parseRequestFromResourceName("pnr1.xml");
		XDSValidator xdsValidator = configureXDSValidatorForPix("1234567890");
		
		// when
		xdsValidator.validateAndEnrichClient(pnr);
		
		// then
		Map<String, String> idMap1 = new HashMap<>();
		idMap1.put("id", "1111111111");
		idMap1.put("idType", "1.2.3");
		verify(xdsValidator.getClient()).send("vm://getecid-pix", idMap1, null, 5000);
		
		Map<String, String> idMap2 = new HashMap<>();
		idMap2.put("id", "76cc765a442f410");
		idMap2.put("idType", "1.3.6.1.4.1.21367.2005.3.7");
		verify(xdsValidator.getClient()).send("vm://getecid-pix", idMap2, null, 5000);
	}
	
	@Test
	public void validateAndEnrichClient_shouldEnrichPNRWithECIDForSubmissionSet() throws Exception {
		// given
		ProvideAndRegisterDocumentSetRequestType pnr = parseRequestFromResourceName("pnr1.xml");
		XDSValidator xdsValidator = configureXDSValidatorForPix("1234567890");
		
		// when
		xdsValidator.validateAndEnrichClient(pnr);
		
		// then
		RegistryPackageType regPac = InfosetUtil.getRegistryPackage(pnr.getSubmitObjectsRequest(), XDSConstants.UUID_XDSSubmissionSet);
		String submissionPatCX = InfosetUtil.getExternalIdentifierValue(XDSConstants.UUID_XDSSubmissionSet_patientId, regPac);
		assertEquals("1234567890^^^&amp;1.2.3&amp;ISO", submissionPatCX);
	}
	
	@Test
	public void validateAndEnrichClient_shouldEnrichPNRWithECIDForDocumentEntries() throws Exception {
		// given
		ProvideAndRegisterDocumentSetRequestType pnr = parseRequestFromResourceName("pnr1.xml");
		XDSValidator xdsValidator = configureXDSValidatorForPix("1111111111");
		
		// when
		xdsValidator.validateAndEnrichClient(pnr);
		
		// then
		ExtrinsicObjectType eo = InfosetUtil.getExtrinsicObjects(pnr.getSubmitObjectsRequest()).get(0);
		String documentPatCX = InfosetUtil.getExternalIdentifierValue(XDSConstants.UUID_XDSDocumentEntry_patientId, eo);
		assertEquals("1111111111^^^&amp;1.2.3&amp;ISO", documentPatCX);
	}
	
	@Test
	public void validateProviderAndFacility_shouldReturnOnSuccessfulValidation() throws Exception {
		// given
		ProvideAndRegisterDocumentSetRequestType pnr = parseRequestFromResourceName("pnr1.xml");
		XDSValidator xdsValidator = configureXDSValidatorForCSD(true);
		
		// when
		xdsValidator.validateProviderAndFacility(pnr);
		
		// then no exception should be thrown
	}
	
	@Test
	public void validateProviderAndFacility_shouldThrowValidationExceptionWhenNoEpid() throws Exception {
		// given
		ProvideAndRegisterDocumentSetRequestType pnr = parseRequestFromResourceName("pnr-no-epid.xml");
		XDSValidator xdsValidator = configureXDSValidatorForCSD(true);
		
		// when
		try {
			xdsValidator.validateProviderAndFacility(pnr);
			
		// then
		} catch (ValidationException e) {
			return;
		}
		
		// else
		fail();
	}
	
	@Test
	public void validateProviderAndFacility_shouldThrowValidationExceptionWhenNoElid() throws Exception {
		// given
		ProvideAndRegisterDocumentSetRequestType pnr = parseRequestFromResourceName("pnr-no-elid.xml");
		XDSValidator xdsValidator = configureXDSValidatorForCSD(true);
		
		// when
		try {
			xdsValidator.validateProviderAndFacility(pnr);
			
		// then
		} catch (ValidationException e) {
			return;
		}
		
		// else
		fail();
	}
	
	@Test
	public void validateProviderAndFacility_shouldThrowValidationExceptionIfCSDQueryNotSuccessful() throws Exception {
		// given
		ProvideAndRegisterDocumentSetRequestType pnr = parseRequestFromResourceName("pnr1.xml");
		XDSValidator xdsValidator = configureXDSValidatorForCSD(false);
		
		// when
		try {
			xdsValidator.validateProviderAndFacility(pnr);
			
		// then
		} catch (ValidationException e) {
			return;
		}
		
		// else
		fail();
	}

	private XDSValidator configureXDSValidatorForPix(String ecidToReturn) throws Exception {
		XDSValidator xdsValidator = new XDSValidator();
		xdsValidator.setEcidAssigningAuthority("1.2.3");
		MuleClient muleClient = mock(MuleClient.class);
		MuleMessage mockMuleMessage = mock(MuleMessage.class);
		when(mockMuleMessage.getPayloadAsString()).thenReturn(ecidToReturn);
		when(mockMuleMessage.getInboundProperty("success")).thenReturn("true");
		when(muleClient.send(eq("vm://getecid-pix"), anyObject(), anyMap(), eq(5000))).thenReturn(mockMuleMessage);
		
		xdsValidator.setClient(muleClient);
		return xdsValidator;
	}
	
	private XDSValidator configureXDSValidatorForCSD(Boolean success) throws Exception {
		XDSValidator xdsValidator = new XDSValidator();
		xdsValidator.setEcidAssigningAuthority("1.2.3");
		MuleClient muleClient = mock(MuleClient.class);
		MuleMessage mockMuleMessage = mock(MuleMessage.class);
		when(mockMuleMessage.getInboundProperty("success")).thenReturn(success.toString());
		when(muleClient.send(eq("vm://validate-epid-elid"), anyObject(), anyMap(), eq(5000))).thenReturn(mockMuleMessage);
		
		xdsValidator.setClient(muleClient);
		return xdsValidator;
	}

}
