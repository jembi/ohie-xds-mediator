package org.openhim.mediator.xds;

import java.io.CharArrayReader;
import java.io.IOException;
import java.io.StringWriter;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.dom4j.DocumentHelper;
import org.jfree.util.Log;
import org.mule.api.MuleMessage;
import org.mule.api.transformer.TransformerException;
import org.mule.transformer.AbstractMessageTransformer;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
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
		for (int i = 0; i < docs.getLength(); i++) {
			Node node = docs.item(i);
			root.removeChild(node);
		}
		
		// Add saved docs
		org.dom4j.Element element4j = message.getInvocationProperty("docs");
		
		// Convert dom4j document to w3c document
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
		Node addedNode = root.appendChild(doc.importNode(element, true));
		
		// Warning nasty hack ahead!! - deleting duplicate namespaces - oh, and why does mule use so many XML frameworks??
		boolean hasNSBeenSeenAlready = false;
		NamedNodeMap attributes = addedNode.getAttributes();
		for (int i = 0 ; i < attributes.getLength() ; i++) {
			logger.info("Looping through attribute " + i);
			Attr attr = (Attr) attributes.item(i);
			if (attr.getValue().equals("urn:ihe:iti:xds-b:2007")) {
				if (hasNSBeenSeenAlready) {
					logger.info("Attempting to remove Attr...");
					element.removeChild(attr);
				} else {
					hasNSBeenSeenAlready = true;	
				}
			}
		}
		
		return message;
	}

}
