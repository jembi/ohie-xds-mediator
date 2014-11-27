package org.openhim.mediator.xds;

import java.io.CharArrayReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.dom4j.DocumentHelper;
import org.jfree.util.Log;
import org.mule.api.MuleMessage;
import org.mule.api.transformer.TransformerException;
import org.mule.transformer.AbstractMessageTransformer;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public class InjectDocumentsTransformer extends AbstractMessageTransformer {

	@Override
	public Object transformMessage(MuleMessage message, String outputEncoding)
			throws TransformerException {
		Document doc = (Document) message.getPayload();
		Node root = doc.getElementsByTagNameNS("urn:ihe:iti:xds-b:2007", "ProvideAndRegisterDocumentSetRequest").item(0);
		NodeList docs = doc.getElementsByTagNameNS("urn:ihe:iti:xds-b:2007", "Document");
		
		// remove docs
		List<Node> nodesToRemove = new ArrayList<Node>();
		for (int i = 0; i < docs.getLength(); i++) {
			Node node = docs.item(i);
			nodesToRemove.add(node);
		}
		for (Node node : nodesToRemove) {
			root.removeChild(node);
			logger.info("Removed old document element");
		}
		
		// Add saved docs
		Object elementsObj = message.getInvocationProperty("docs");
		if (elementsObj instanceof org.dom4j.Element) {
			org.dom4j.Element element4j = message.getInvocationProperty("docs");
			Element element = convertDom4jElementToW3C(element4j);
			root.appendChild(doc.importNode(element, true));
		} else if (elementsObj instanceof List<?>) {
			List<org.dom4j.Element> elements = (List) elementsObj;
			for (org.dom4j.Element element4j : elements) {
				Element element = convertDom4jElementToW3C(element4j);
				root.appendChild(doc.importNode(element, true));
			}
		}
		
		return message;
	}

	private Element convertDom4jElementToW3C(org.dom4j.Element element4j) {
		// Convert dom4j document to w3c document - why mule why?!?
		org.dom4j.Document doc4j = DocumentHelper.createDocument((org.dom4j.Element) element4j.detach());
		
		Document docw3c = null;
		try {
			DocumentBuilderFactory domFact = DocumentBuilderFactory.newInstance();
			DocumentBuilder builder = domFact.newDocumentBuilder();
			docw3c = builder.parse(new InputSource(new CharArrayReader(doc4j.asXML().toCharArray())));
		} catch (ParserConfigurationException e) {
			Log.error(e);
			e.printStackTrace();
		} catch (SAXException e) {
			Log.error(e);
			e.printStackTrace();
		} catch (IOException e) {
			Log.error(e);
			e.printStackTrace();
		}
		
		Element element = docw3c.getDocumentElement();
		return element;
	}

}
