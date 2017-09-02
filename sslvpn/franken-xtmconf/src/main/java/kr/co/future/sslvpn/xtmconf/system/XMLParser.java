package kr.co.future.sslvpn.xtmconf.system;

import java.io.File;
import java.io.IOException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class XMLParser {
	public static Document createDoc(File file) {
		try {
			DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory
					.newInstance();
			DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();
			Document doc = docBuilder.parse(file);
			doc.getDocumentElement().normalize();
			return doc;
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
			return null;
		} catch (SAXException e) {
			e.printStackTrace();
			return null;
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
	}

	public static NamedNodeMap getAttrs(Document doc, String tag) {
		NodeList n = doc.getElementsByTagName(tag);
		return n.item(0).getAttributes();
	}
	
	public static NodeList getNodeList(Document doc, String tag) {
		NodeList n = doc.getElementsByTagName(tag);
		return n.item(0).getChildNodes();
	}
	
	public static NodeList getNodeList(InitFileKey key, Document doc, String tag) {
		NodeList n = doc.getElementsByTagName(tag);
		Node node = null;

		if (n.getLength() >= 2) {
			for (int i = 0; i < n.getLength(); i++) {
				if (key.isMasterKey(n.item(i).getParentNode().getNodeName())) {
					node = n.item(i);
					break;
				}
			}
		} else
			node = n.item(0);

		if (node == null)
			return null;
		return node.getChildNodes();
	}
}