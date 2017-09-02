package kr.co.future.sslvpn.xtmconf.system;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.net.URL;
import java.net.URLConnection;
import java.net.UnknownHostException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import kr.co.future.sslvpn.model.DataExporter;
import kr.co.future.sslvpn.xtmconf.KLogWriter;
import kr.co.future.sslvpn.xtmconf.SSLTrustHelper;
import kr.co.future.confdb.ConfigDatabase;
import kr.co.future.confdb.file.FileConfigDatabase;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class PolicySync {
	private static final File rootDir = new File("/");

	public static String sync() throws IOException, NumberFormatException, InterruptedException, NoSuchAlgorithmException,
			KeyManagementException {
		Document doc = XMLParser.createDoc(new File("/etc/webadmin/xml/sync_policy_synchronize.xml"));
		String chkUse = getAttrValue(doc, "synchronize", "chk_use");
		if (chkUse.equals("on"))
			return sessionSyncPolicy();
		return "error";
	}

	private static String sessionSyncPolicy() throws NumberFormatException, UnknownHostException, IOException,
			InterruptedException, NoSuchAlgorithmException, KeyManagementException {
		Logger logger = LoggerFactory.getLogger(PolicySync.class.getName());
		Document doc = XMLParser.createDoc(new File("/etc/webadmin/xml/sync_policy_synchronize.xml"));

		String chkUse = getAttrValue(doc, "synchronize", "chk_use");
		if (!chkUse.equals("on"))
			return "Deactivate policy synchronize error";

		String actMode = getAttrValue(doc, "act", "mode");
		if (!actMode.equals("master"))
			return "MASTER error";

		String ip = getNodeValue(doc, "ip");
		int port = Integer.parseInt(getNodeValue(doc, "port"));
		if (!isConnect(ip, port, 2)) {
			KLogWriter.write(0x12030023, null, "SLAVE connection error");
			return "SLAVE connection error";
		}

		byte[] b = createMasterPolicy(doc);
		if (b == null) {
			KLogWriter.write(0x12030023, null, "Create policy error");
			return "Create policy error";
		}

		String password = getNodeValue(doc, "password");
		String url = "https://" + ip + ":" + port + "/policy/sync?password=" + password;
		logger.trace("frodo core: sending policy to [{}], size [{}]", url, b.length);

		String resp = postHttpRequest(url, b);
		logger.trace("frodo core: receved sync result [{}]", resp);

		String[] token = resp.split(" ");
		if (token[0].equals("ok")) {
			KLogWriter.write(0x12030023, null, "Policy synchronize to SLAVE");
			return "Policy synchronize to SLAVE";
		} else if (token[0].equals("error")) {
			if (token[1].equals("off")) {
				KLogWriter.write(0x12030023, null, "SLAVE don't use policy synchronize");
				return "SLAVE don't use policy synchronize";
			} else if (token[1].equals("master")) {
				KLogWriter.write(0x12030023, null, "SLAVE mode is Master");
				return "SLAVE mode is Master";
			} else {
				KLogWriter.write(0x12030023, null, "SLAVE upgrade error " + token[1]);
				return "SLAVE upgrade error " + token[1];
			}
		} else {
			KLogWriter.write(0x12030023, null, "Synchronize error");
			return "Synchronize error";
		}
	}

	private static byte[] createMasterPolicy(Document doc) throws IOException, InterruptedException {
		Logger logger = LoggerFactory.getLogger(PolicySync.class.getName());

		Process p = new ProcessBuilder("cat", "/proc/utm/conf/confdev").directory(rootDir).start();
		BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
		String confDev = br.readLine();
		br.close();

		// mount error
		if (confDev == null) {
			logger.trace("frodo core: cannot mount conf device");
			return null;
		}

		logger.trace("frodo core: mounting [{}] to /utm/conf", confDev);
		CommandUtil.run(rootDir, "mount", "-t", "ext3", confDev, "/utm/conf");
		try {
			File confXmlDir = new File("/utm/conf/xml");
			if (!confXmlDir.exists())
				return null;

			NodeList nl = XMLParser.getNodeList(doc, "option");
			if (nl == null)
				return null;

			ByteArrayOutputStream os = new ByteArrayOutputStream();
			zipXmls(doc, os);
			return os.toByteArray();
		} finally {
			logger.trace("frodo core: umounting /utm/conf");
			CommandUtil.run(rootDir, "umount", "/utm/conf");
		}
	}

	private static String getNodeValue(Document doc, String tag) {
		NodeList n = doc.getElementsByTagName(tag);
		return n.item(0).getTextContent();
	}

	private static String getAttrValue(Document doc, String tag, String attrKey) {
		NodeList n = doc.getElementsByTagName(tag);
		return n.item(0).getAttributes().getNamedItem(attrKey).getTextContent();
	}

	private static boolean isConnect(String host, int port, int timeout) throws NumberFormatException, UnknownHostException,
			IOException {
		Socket soc = new Socket(host, port);
		return soc.isConnected();
	}

	private static void zipXmls(Document doc, ByteArrayOutputStream os) throws IOException, InterruptedException {
		Logger logger = LoggerFactory.getLogger(PolicySync.class.getName());
		logger.trace("frodo core: zipping xml files");

		ZipOutputStream zos = new ZipOutputStream(os);
		ZipEntry object = new ZipEntry("object/");
		zos.putNextEntry(object);
		zos.closeEntry();

		ZipEntry objectXml = new ZipEntry("object/xml/");
		zos.putNextEntry(objectXml);
		zos.closeEntry();

		MasterPolicy policy = new MasterPolicy();
		NodeList nl = XMLParser.getNodeList(doc, "option");

		for (int i = 0; i < nl.getLength(); i++) {
			Node n = nl.item(i);
			if (n.getNodeName().equals("#text"))
				continue;

			String key = n.getNodeName();
			if (!policy.isKey(key))
				continue;

			NamedNodeMap attr = n.getAttributes();
			Set<String> attrKeys = policy.getArrKeys(key);
			for (String attrKey : attrKeys) {
				Node n2 = attr.getNamedItem(attrKey);
				if (n2 == null)
					continue;

				if (n2.getNodeValue().equals("on")) {
					Set<String> xmls = policy.getXmls(n2.getNodeName());
					for (String xml : xmls) {
						if (xml.equals("kraken-dom-localhost.cdb")) {
							ConfigDatabase db = new FileConfigDatabase(new File(System.getProperty("kraken.data.dir")
									+ "/kraken-confdb"), "kraken-dom-localhost");
							DataExporter.exportData(db, new File("/utm/conf/xml", xml));
						} else if (xml.equals("devices.tmp")) {
							ConfigDatabase db = new FileConfigDatabase(new File(System.getProperty("kraken.data.dir")
									+ "/kraken-confdb"), "frodo");
							DataExporter.exportDevices(db, new File("/utm/conf/xml", xml));
						}
						writeZipEntry(zos, new File("/utm/conf/xml"), xml);
					}
				}
			}
		}

		zos.close();
		logger.trace("frodo core: zip completed");
	}

	private static void writeZipEntry(ZipOutputStream os, File parent, String xmlName) throws IOException {
		Logger logger = LoggerFactory.getLogger(PolicySync.class.getName());
		logger.trace("frodo core: zipping xml file [{}]", xmlName);
		File f = new File(parent, xmlName);

		ZipEntry e = new ZipEntry("object/xml/" + xmlName);
		os.putNextEntry(e);

		FileInputStream is = null;
		is = new FileInputStream(f);
		byte[] b = new byte[8096];
		while (true) {
			int read = is.read(b);
			if (read <= 0)
				break;
			os.write(b, 0, read);
		}
		if (is != null){
			try {
				is.close();
			} catch (IOException ex) {
			}
		}
		os.closeEntry();
	}

	private static String postHttpRequest(String url, byte[] b) throws NoSuchAlgorithmException, IOException,
			KeyManagementException {
		/* Disabling Certification Validation in HTTPS */
		SSLTrustHelper.trustAll();

		/* Post */
		URLConnection conn = new URL(url).openConnection();
		conn.setRequestProperty("Content-Type", "application/octet-stream");
		conn.setDoOutput(true);

		OutputStream os = conn.getOutputStream();
		os.write(b);
		os.flush();
		os.close();

		/* Get response */
		BufferedReader responseReader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
		String response = responseReader.readLine();
		responseReader.close();
		return response;
	}
}