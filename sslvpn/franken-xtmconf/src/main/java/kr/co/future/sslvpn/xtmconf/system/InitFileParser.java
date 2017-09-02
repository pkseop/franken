package kr.co.future.sslvpn.xtmconf.system;

import java.io.File;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class InitFileParser {
	public static void parse(File initialXmlfile) {
		Document doc = XMLParser.createDoc(initialXmlfile);
		InitFileKey key = new InitFileKey();

		Set<String> masterKeys = key.getMasterKeys();
		for (String masterKey : masterKeys) {
			Set<String> secondKeys = key.getSecondKeys(masterKey);

			for (String secondKey : secondKeys) {
				if (secondKey.equals("center_setup")) {
					Node n = doc.getElementsByTagName("center_setup").item(0);
					String flag = n.getAttributes().getNamedItem("chk_use")
							.getNodeValue();

					if (flag.equals("on"))
						createInitFile(key, n.getChildNodes(), secondKey, true);
					else
						createInitFile(key, n.getChildNodes(), secondKey, false);
					continue;
				}

				NodeList nList = XMLParser.getNodeList(key, doc, secondKey);
				if (nList == null)
					continue;

				if (secondKey.equals("firewall_policy")) {
					// compare fid.
				}
				createInitFile(key, nList, secondKey, false);
			}
		}
	}

	private static void createInitFile(InitFileKey key, NodeList nList,
			String secondKey, boolean flag) {
		try {
			DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory
					.newInstance();
			DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();
			Document doc = docBuilder.newDocument();

			String rootNode = key.getRootNode(secondKey);
			Element order = doc.createElement(rootNode);
			if (secondKey.equals("center_setup")) {
				if (flag)
					order.setAttribute("chk_use", "on");
				else
					order.setAttribute("chk_use", "off");
			}
			doc.appendChild(order);

			for (int i = 0; i < nList.getLength(); i++) {
				doc.getDocumentElement().appendChild(
						doc.importNode(nList.item(i), true));
			}

			doc.normalize();
			doc.normalizeDocument();
			Source source = new DOMSource(doc);

			File file = new File("/home/mindori/etc/webadmin/init_xml/" + secondKey
					+ ".xml");
			Result result = new StreamResult(file);

			Transformer xformer = TransformerFactory.newInstance()
					.newTransformer();
			xformer.transform(source, result);

		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		} catch (TransformerConfigurationException e) {
			e.printStackTrace();
		} catch (TransformerFactoryConfigurationError e) {
			e.printStackTrace();
		} catch (TransformerException e) {
			e.printStackTrace();
		}
	}
}