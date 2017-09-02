package kr.co.future.sslvpn.xtmconf;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import kr.co.future.msgbus.Marshalable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public abstract class XtmConfig implements Marshalable {
	public static final String UTM_LOG = "/utm/log/";

	private static Logger logger = LoggerFactory.getLogger(XtmConfig.class);
	private static File confDirectory = new File("/etc/webadmin/conf/");
	private static File xmlDirectory = new File("/etc/webadmin/xml/");

	public static File getConfDir() {
		return confDirectory;
	}

	public static void setConfDir(File confDirectory) {
		XtmConfig.confDirectory = confDirectory;
	}

	public static File getXmlDir() {
		return xmlDirectory;
	}

	public static void setXmlDir(File xmlDirectory) {
		XtmConfig.xmlDirectory = xmlDirectory;
	}

	public abstract String getXmlFilename();

	public abstract String getRootTagName();

	protected abstract Element convertToElement(Document doc);

	protected static Element appendChild(Document doc, Element parent, String name, int value) {
		return appendChild(doc, parent, name, String.valueOf(value));
	}

	protected static Element appendChild(Document doc, Element parent, String name, boolean value) {
		return appendChild(doc, parent, name, value ? "on" : "off");
	}

	protected static Element appendChild(Document doc, Element parent, String name, Object value) {
		return appendChild(doc, parent, name, value, null);
	}

	protected static Element appendChild(Document doc, Element parent, String name, int value, AttributeBuilder attr) {
		return appendChild(doc, parent, name, String.valueOf(value), attr);
	}

	protected static Element appendChild(Document doc, Element parent, String name, boolean value, AttributeBuilder attr) {
		return appendChild(doc, parent, name, value ? "on" : "off", attr);
	}

	protected static Element appendChild(Document doc, Element parent, String name, Object value, AttributeBuilder attr) {
		Element e = doc.createElement(name);

		if (attr != null) {
			Map<String, String> m = attr.get();
			for (String key : m.keySet())
				e.setAttribute(key, m.get(key));
		}

		if (value != null)
			e.setTextContent(value.toString());

		parent.appendChild(e);

		return e;
	}

	public static <T extends XtmConfig> List<T> readConfig(Class<T> cls) throws IllegalArgumentException {
		return readConfig(cls, xmlDirectory);
	}

	@SuppressWarnings("unchecked")
	public static <T extends XtmConfig> List<T> readConfig(Class<T> cls, File xmlDir) throws IllegalArgumentException {
		try {
			File xml = new File(xmlDir, (String) cls.getMethod("getXmlFilename").invoke(cls.newInstance()));
			if (!xml.exists())
				return new ArrayList<T>();

			String rootTagName = (String) cls.getMethod("getRootTagName").invoke(cls.newInstance());

			List<T> ret = new ArrayList<T>();
			Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(xml);
			NodeList objs = doc.getElementsByTagName(rootTagName);

			if (objs.getLength() == 0)
				return ret;

			for (Node n = objs.item(0).getFirstChild(); n != null; n = n.getNextSibling()) {
				try {
					T obj = (T) cls.getMethod("parse", NodeWrapper.class).invoke(null, new NodeWrapper(n));
					if (obj != null)
						ret.add(obj);
				} catch (NoSuchMethodError e) {
					logger.error("frodo-xtmconf: cannot find 'public static TYPE parse(NodeWrapper)' in " + cls.getName());
				}
			}

			return ret;
		} catch (Exception e) {
			logger.error("frodo-xtmconf: read config error.", e);
			throw new IllegalArgumentException(e);
		}
	}

	public static <T extends XtmConfig> void writeConfig(Class<T> cls, List<T> config) {
		FileOutputStream fos = null;

		try {
			File xml = new File(xmlDirectory, (String) cls.getMethod("getXmlFilename").invoke(cls.newInstance()));
			String rootTagName = (String) cls.getMethod("getRootTagName").invoke(cls.newInstance());

			if (!xml.getParentFile().exists())
				xml.getParentFile().mkdirs();
			byte[] b = toXml(rootTagName, config).getBytes("UTF-8");

			fos = new FileOutputStream(xml);
			fos.write(b);
		} catch (Exception e) {
			logger.error("frodo-xtmconf: write config error.", e);
			throw new IllegalArgumentException(e);
		} finally {
			try {
				if (fos != null)
					fos.close();
			} catch (IOException e) {
				logger.error("frodo-xtmconf: io error.", e);
			}
		}
	}

	private static String toXml(String rootTagName, List<? extends XtmConfig> objs) throws Exception {
		try {
			DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder builder = documentBuilderFactory.newDocumentBuilder();
			Document doc = builder.newDocument();
			Element e = doc.createElement(rootTagName);

			for (XtmConfig obj : objs)
				e.appendChild(obj.convertToElement(doc));
			doc.appendChild(e);

			return toXmlString(doc);
		} catch (Exception e) {
			logger.error("frodo-xtmconf: xml build error.", e);
			throw e;
		}
	}

	private static String toXmlString(Document document) {
		StringWriter sw = new StringWriter();
		StreamResult result = new StreamResult(sw);
		TransformerFactory tf = TransformerFactory.newInstance();
		tf.setAttribute("indent-number", 4);

		try {
			Transformer transformer = tf.newTransformer();
			transformer.setOutputProperty(OutputKeys.INDENT, "yes");
			transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
			transformer.setOutputProperty(OutputKeys.METHOD, "html");
			transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
			transformer.transform(new DOMSource(document.getLastChild()), result);
		} catch (Exception e) {
			logger.error("frodo-xtmconf: xml build error.", e);
		}

		return sw.toString();
	}
}
