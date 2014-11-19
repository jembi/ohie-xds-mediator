package org.openhim.mediator.xds;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.anyMap;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import ihe.iti.xds_b._2007.ProvideAndRegisterDocumentSetRequestType;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.bind.JAXBException;

import oasis.names.tc.ebxml_regrep.xsd.rim._3.ExtrinsicObjectType;
import oasis.names.tc.ebxml_regrep.xsd.rim._3.RegistryPackageType;
import oasis.names.tc.ebxml_regrep.xsd.rim._3.SlotType1;

import org.dcm4chee.xds2.common.XDSConstants;
import org.dcm4chee.xds2.infoset.util.InfosetUtil;
import org.junit.Test;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.module.client.MuleClient;
import org.openhim.mediator.orchestration.exceptions.ValidationException;
import org.openhim.mediator.test.Util;

public class XDSValidatorTest {

	@Test
	public void validateAndEnrichClient_shouldSendExpectedPIXRequests() throws Exception {
		// given
		ProvideAndRegisterDocumentSetRequestType pnr = Util.parseRequestFromResourceName("pnr1.xml");
		XDSValidator xdsValidator = configureXDSValidatorForPix(true, "1234567890");
		
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
		ProvideAndRegisterDocumentSetRequestType pnr = Util.parseRequestFromResourceName("pnr1.xml");
		XDSValidator xdsValidator = configureXDSValidatorForPix(true, "1234567890");
		
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
		ProvideAndRegisterDocumentSetRequestType pnr = Util.parseRequestFromResourceName("pnr1.xml");
		XDSValidator xdsValidator = configureXDSValidatorForPix(true, "1111111111");
		
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
		ProvideAndRegisterDocumentSetRequestType pnr = Util.parseRequestFromResourceName("pnr1.xml");
		XDSValidator xdsValidator = configureXDSValidatorForCSD(true, new HashMap(), new String[] {"vm://get-epid", "vm://get-elid"});
		
		// when
		xdsValidator.validateProviderAndFacility(pnr);
		
		// then no exception should be thrown
	}
	
	@Test
	public void validateProviderAndFacility_shouldOnlyCallGetElidWhenNoEpidIsSupplied() throws Exception {
		// given
		ProvideAndRegisterDocumentSetRequestType pnr = Util.parseRequestFromResourceName("pnr-no-epid.xml");
		XDSValidator xdsValidator = configureXDSValidatorForCSD(true, new HashMap(), new String[] {"vm://get-epid", "vm://get-elid"});
		
		// when
		xdsValidator.validateProviderAndFacility(pnr);
			
		// then no exception is thrown
	}
	
	@Test
	public void validateProviderAndFacility_shouldOnlyCallGetEpidWhenNoElidIsSupplied() throws Exception {
		// given
		ProvideAndRegisterDocumentSetRequestType pnr = Util.parseRequestFromResourceName("pnr-no-elid.xml");
		XDSValidator xdsValidator = configureXDSValidatorForCSD(true, new HashMap(), new String[] {"vm://get-epid", "vm://get-elid"});
		
		// when
		xdsValidator.validateProviderAndFacility(pnr);
			
		// then no exception is thrown
	}
	
	@Test
	public void validateProviderAndFacility_shouldThrowValidationExceptionIfCSDQueryNotSuccessful() throws Exception {
		// given
		ProvideAndRegisterDocumentSetRequestType pnr = Util.parseRequestFromResourceName("pnr1.xml");
		XDSValidator xdsValidator = configureXDSValidatorForCSD(false, null, new String[] {"vm://get-epid", "vm://get-elid"});
		
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
	public void validateProviderAndFacility_shouldEnrichPNRWithEPID() throws Exception {
		// given
		ProvideAndRegisterDocumentSetRequestType pnr = Util.parseRequestFromResourceName("pnr1.xml");
		Map<String, String> map = new HashMap<>();
		map.put("epid", "123456789");
		map.put("epidAssigningAuthorityId", "1.2.3");
		XDSValidator xdsValidator = configureXDSValidatorForCSD(true, map, new String[] {"vm://get-epid", "vm://get-elid"});
		
		// when
		try {
			xdsValidator.validateProviderAndFacility(pnr);
			
			// then
			List<ExtrinsicObjectType> eos = InfosetUtil.getExtrinsicObjects(pnr.getSubmitObjectsRequest());
			for (ExtrinsicObjectType eo : eos) {
				List<Map<String, SlotType1>> authorClassSlots = null;
				try {
					authorClassSlots = xdsValidator.getClassificationSlotsFromExtrinsicObject(XDSConstants.UUID_XDSDocumentEntry_author, eo);
				} catch (JAXBException e) {
					fail(e.getMessage());
				}
				
				for (Map<String, SlotType1> slotMap : authorClassSlots) {
					if (slotMap.containsKey(XDSConstants.SLOT_NAME_AUTHOR_PERSON)) {
						SlotType1 personSlot = slotMap.get(XDSConstants.SLOT_NAME_AUTHOR_PERSON);
						List<String> valueList = personSlot.getValueList().getValue();
						
						for (String val : valueList) {
							assertEquals("123456789^^^^^^^^&amp;1.2.3&amp;ISO", val);
						}
					}
				}
			}
		} catch (ValidationException e) {
			fail(e.getMessage());
		}
	}
	
	@Test
	public void validateProviderAndFacility_shouldEnrichPNRWithELID() throws Exception {
		// given
		ProvideAndRegisterDocumentSetRequestType pnr = Util.parseRequestFromResourceName("pnr1.xml");
		Map<String, String> map = new HashMap<>();
		map.put("elid", "53");
		map.put("elidAssigningAuthorityId", "1.2.3");
		XDSValidator xdsValidator = configureXDSValidatorForCSD(true, map, new String[] {"vm://get-epid", "vm://get-elid"});
		
		// when
		try {
			xdsValidator.validateProviderAndFacility(pnr);
			
			// then
			List<ExtrinsicObjectType> eos = InfosetUtil.getExtrinsicObjects(pnr.getSubmitObjectsRequest());
			for (ExtrinsicObjectType eo : eos) {
				List<Map<String, SlotType1>> authorClassSlots = null;
				try {
					authorClassSlots = xdsValidator.getClassificationSlotsFromExtrinsicObject(XDSConstants.UUID_XDSDocumentEntry_author, eo);
				} catch (JAXBException e) {
					fail(e.getMessage());
				}
				
				for (Map<String, SlotType1> slotMap : authorClassSlots) {
					if (slotMap.containsKey(XDSConstants.SLOT_NAME_AUTHOR_INSTITUTION)) {
						SlotType1 institutionSlot = slotMap.get(XDSConstants.SLOT_NAME_AUTHOR_INSTITUTION);
						List<String> valueList = institutionSlot.getValueList().getValue();
						
						for (String val : valueList) {
							assertTrue(val.contains("^^^^^&amp;1.2.3&amp;ISO^^^^53"));
						}
					}
				}
			}
		} catch (ValidationException e) {
			fail(e.getMessage());
		}
	}
	
	@Test
	public void createXON_shouldCreateAValidXON() throws MuleException {
		XDSValidator xdsValidator = new XDSValidator();
		String xon = "Some Hospital^^^^^&amp;1.2.3.4.5.6.7.8.9.1789&amp;ISO^^^^45";
		String xonResult = xdsValidator.createXON("Some Hospital", "45", "1.2.3.4.5.6.7.8.9.1789");
		assertEquals(xon, xonResult);
	}
	
	@Test
	public void createXCN_shouldCreateAValidXCN() throws MuleException {
		XDSValidator xdsValidator = new XDSValidator();
		String xcn = "11375^^^^^^^^&amp;1.2.840.113619.6.197&amp;ISO";
		String xcnResult = xdsValidator.createXCN("11375", "1.2.840.113619.6.197");
		assertEquals(xcn, xcnResult);
	}

	private XDSValidator configureXDSValidatorForPix(Boolean success, String ecidToReturn) throws Exception {
		XDSValidator xdsValidator = new XDSValidator();
		xdsValidator.setEcidAssigningAuthority("1.2.3");
		MuleClient muleClient = mock(MuleClient.class);
		MuleMessage mockMuleMessage = mock(MuleMessage.class);
		when(mockMuleMessage.getPayloadAsString()).thenReturn(ecidToReturn);
		when(mockMuleMessage.getInboundProperty("success")).thenReturn(success.toString());
		when(muleClient.send(eq("vm://getecid-pix"), anyObject(), anyMap(), eq(5000))).thenReturn(mockMuleMessage);
		
		xdsValidator.setClient(muleClient);
		return xdsValidator;
	}
	
	private XDSValidator configureXDSValidatorForCSD(Boolean success, Map<String, String> mapToReturn, String[]  vmQueues) throws Exception {
		XDSValidator xdsValidator = new XDSValidator();
		xdsValidator.setEcidAssigningAuthority("1.2.3");
		MuleClient muleClient = mock(MuleClient.class);
		MuleMessage mockMuleMessage = mock(MuleMessage.class);
		when(mockMuleMessage.getPayload()).thenReturn(mapToReturn);
		when(mockMuleMessage.getInboundProperty("success")).thenReturn(success.toString());
		for (String queue : vmQueues) {
			when(muleClient.send(eq(queue), anyObject(), anyMap(), eq(5000))).thenReturn(mockMuleMessage);
		}
		
		xdsValidator.setClient(muleClient);
		return xdsValidator;
	}

}
