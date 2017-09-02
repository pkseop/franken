package kr.co.future.sslvpn.xtmconf.system;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import kr.co.future.confdb.ConfigDatabase;
import kr.co.future.confdb.ConfigService;
import kr.co.future.msgbus.Session;
import kr.co.future.sslvpn.xtmconf.XtmConfig;
import kr.co.future.sslvpn.xtmconf.sql.csv.ImportCsvSqlDb;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.NodeList;

public class Restore {
	private static final String tmpPath = XtmConfig.UTM_LOG + "tmp";

	private static File directory;
	private static final String obj = "object.zip";

	public static void restore(ConfigService conf, Session session, String password, File backupFile) throws IOException {
		restore(conf, password, backupFile);
	}
	
	public static void restore(ConfigService conf, String password, File backupFile) throws IOException {
		Logger logger = LoggerFactory.getLogger(Restore.class.getName());

		CommandUtil.run(new File("/"), "rm", "-rf", XtmConfig.UTM_LOG + "tmp/object");
		CommandUtil.run(new File("/"), "rm", "-rf", XtmConfig.UTM_LOG + "tmp/object.zip");

		directory = new File(tmpPath);
		File originalFile = new File(tmpPath + "/" + backupFile.getName());

		logger.trace("frodo xtmconf: try to move file from [{}] to [{}]", backupFile.getAbsolutePath(),
				originalFile.getAbsolutePath());
		CommandUtil.run(new File("/"), "mv", backupFile.getAbsolutePath(), originalFile.getAbsolutePath());

		File objectFile = new File(directory.getAbsolutePath() + "/" + obj);
		logger.trace("frodo xtmconf: try to move file from [{}] to [{}]", originalFile.getAbsolutePath(),
				objectFile.getAbsolutePath());

		CommandUtil.run(new File("/"), "mv", originalFile.getAbsolutePath(), objectFile.getAbsolutePath());
		logger.trace("frodo xtmconf: try to unzip file [{}] in dir [{}]", obj, directory.getAbsolutePath());

		int rc = CommandUtil.run(directory, "unzip", "-P", password, obj);
		if (rc != 0) {
			/* rollback */
			logger.trace("frodo xtmconf: unzip failed, rollback to old version, removing [{}]", obj);
			CommandUtil.run(directory, "rm", "-rf", obj, "object/");
			throw new IOException("invalid-password");
		}

		File f1 = new File(directory.getAbsolutePath() + "/object");
		if (!f1.exists()) {
			logger.trace("frodo xtmconf: try to delete dir [{}]", obj, directory.getAbsolutePath());
			List<String> commands = new ArrayList<String>();
			commands.add("rm");
			for (File tmpFile : directory.listFiles())
				commands.add(tmpFile.getAbsolutePath());
			CommandUtil.run(new File("/"), commands);
			throw new IOException("unzip-failed");
		}

		logger.trace("frodo xtmconf: ensure " + XtmConfig.UTM_LOG + "db/ips");
		File f2 = new File(XtmConfig.UTM_LOG + "db/ips");
		f2.mkdirs();

		if (f1.isDirectory()) {
			try {
				policyRestore(conf);
			} catch (IOException e) {
				throw new IOException("policy-restore-failed");
			}
		}

		File f4 = new File(XtmConfig.UTM_LOG + "tmp/GateInitInfo.xml");
		if (f4.exists() && f4.isFile()) {
			logger.trace("frodo xtmconf: applying init file [{}]", f4.getAbsolutePath());
			try {
				applyInitFile(f4);
			} catch (IOException e) {
				throw new IOException("apply-init-failed");
			}
		}

		delSpdState();
	}

	private static void policyRestore(ConfigService conf) throws IOException {
		Logger logger = LoggerFactory.getLogger(Restore.class.getName());

		File objectDir = new File(tmpPath + "/object");

		BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(new File("/proc/utm/conf/confdev"))));
		String device = br.readLine().trim();
		if (br != null){
			try {
				br.close();
			} catch (IOException e) {
			}
		}

		logger.trace("frodo xtmconf: trying mount [{}]", device);
		CommandUtil.run(new File("/"), "mount", "-t", "ext3", device, "/utm/conf");

		logger.trace("frodo xtmconf: trying to restore conf");
		File confList = new File(objectDir.getAbsolutePath());
		List<String> command = new ArrayList<String>();
		command.add("cp");
		command.add("-rf");
		for (File confFile : confList.listFiles()) {
			command.add(confFile.getName());
		}
		command.add("/utm/conf");
		CommandUtil.run(confList, command);

		logger.trace("frodo xtmconf: trying umount [{}]", device);
		CommandUtil.run(new File("/"), "umount", "/utm/conf");

		logger.trace("frodo xtmconf: trying to restore xml");
		File xmlList = new File(objectDir.getAbsolutePath() + "/xml");
		List<String> command2 = new ArrayList<String>();
		command2.add("cp");
		command2.add("-rf");
		for (File xml : xmlList.listFiles()) {
			command2.add(xml.getName());
		}
		command2.add("/etc/webadmin/xml");
		CommandUtil.run(xmlList, command2);

		logger.trace("frodo xtmconf: trying to restore kraken data");
		String dataDir = System.getProperty("kraken.data.dir");
		File krakenData = new File(dataDir);

		CommandUtil.run(krakenData, "rm", "-rf", "kraken-dom/");

		File krakenDom = new File(objectDir, "kraken-dom/");
		if (krakenDom.exists())
			CommandUtil.run(krakenData, "cp", "-rf", krakenDom.getAbsolutePath(), dataDir);

		File pemFile = new File(objectDir, "local.pem");
		if (pemFile.exists())
			CommandUtil.run(objectDir, "cp", "-rf", pemFile.getAbsolutePath(), "/utm/log/kraken/data/kraken-ca/CA/local/");

		File krakenConfdb = new File(objectDir, "kraken-confdb/");
		String[] dbNames = { "kraken-ldap", "kraken-dom", "kraken-dom-localhost", "frodo", "kraken-ca-local", "kraken-ca" };

		for (String dbName : dbNames) {
			File cdbFile = new File(krakenConfdb, dbName + ".cdb");
			File backupDb = new File(krakenConfdb, dbName);
			if (cdbFile.exists()) {
				logger.trace("frodo xtmconf: trying to import " + dbName);
				importData(conf, dbName, cdbFile);
			} else if (backupDb.exists()) {
				logger.trace("frodo xtmconf: trying to copy " + backupDb.getAbsolutePath() + " to " + dataDir + "/kraken-confdb/");
				CommandUtil.run(krakenData, "rm", "-rf", "kraken-confdb/" + dbName);
				CommandUtil.run(krakenData, "cp", "-rf", backupDb.getAbsolutePath(), dataDir + "/kraken-confdb/");
			} else {
				logger.error("frodo xtmconf: cannot find backup cdb file or directory, name=" + dbName);
			}
		}
		
		ImportCsvSqlDb importCsv = new ImportCsvSqlDb();
		importCsv.importCsv(objectDir + "/KRAKEN_DB");

		CommandUtil.run(new File("/"), "rm", "-rf", XtmConfig.UTM_LOG + "tmp/object");
		CommandUtil.run(new File("/"), "rm", "-rf", XtmConfig.UTM_LOG + "tmp/object.zip");
		CommandUtil.run(new File("/"), "sync");
		logger.trace("frodo xtmconf: policy restore completed");
	}

	private static void importData(ConfigService conf, String dbName, File backupFile) {
		Logger logger = LoggerFactory.getLogger(Restore.class.getName());
		InputStream is = null;
		try {
			ConfigDatabase db = conf.ensureDatabase(dbName);
			is = new FileInputStream(backupFile);
			db.importData(is);
		} catch (IOException e) {
			logger.error("frodo xtmconf: cannot import data " + backupFile.getAbsolutePath(), e);
		} finally {
			if (is != null)
				try {
					is.close();
				} catch (IOException e) {
				}
		}

	}

	private static void applyInitFile(File initFile) throws IOException {
		InitFileParser.parse(initFile);
		File f = new File(XtmConfig.UTM_LOG + "tmp/GateInitInfo.xml");
		if (!f.delete()) {
			throw new RuntimeException("rm error: " + f.getAbsolutePath());
		}

		File f2 = new File("/etc/webadmin/xml/center_setup.xml");
		BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(f2)));
		String line = br.readLine();
		if (br != null){
			try {
				br.close();
			} catch (IOException e) {
			}
		}
		int pos = line.indexOf("=");
		if (line.substring(pos + 2, pos + 4).equals("on")) {
			useCenter();
		}

		CommandUtil.run(new File(""), "php", "/var/www/webadmin/send_policy_object.php");
	}

	private static void useCenter() {
		createCenterConfFile();
		makeCenterUseScript();
		makeMasterKey();
	}

	private static void createCenterConfFile() {
		Logger logger = LoggerFactory.getLogger(Restore.class.getName());

		OutputStream fos = null;
		try {
			Document doc = XMLParser.createDoc(new File("/etc/webadmin/xml/center_setup.xml"));
			Document doc2 = XMLParser.createDoc(new File("/etc/webadmin/xml/system_setting_option.xml"));

			NamedNodeMap gateAttrs = XMLParser.getAttrs(doc, "gate");
			NamedNodeMap centerAttrs = XMLParser.getAttrs(doc, "center");
			NodeList base = XMLParser.getNodeList(doc2, "option");

			String uuid = gateAttrs.item(0).toString().replace("cid=", "").replaceAll("\"", "");
			String cid = centerAttrs.item(0).toString().replace("cid=", "").replaceAll("\"", "");
			String name = null;
			for (int i = 0; i < base.getLength(); i++) {
				if (base.item(i).getNodeName().equals("name"))
					name = base.item(i).getTextContent();
			}

			String serial = uuid.substring(0, 16);
			StringBuilder builder = new StringBuilder();
			builder.append("CENTER_USE=1\n");
			builder.append("CENTER_TYPE=XTM\n");
			builder.append("CENTER_SERIAL=" + serial + "\n");
			builder.append("GATE_UUID=" + uuid.substring(16, 20) + "-" + uuid.substring(20, 24) + "-" + uuid.substring(24, 32)
					+ "\n");
			builder.append("CENTER_UUID=" + cid.substring(16, 20) + "-" + cid.substring(20, 24) + "-" + cid.substring(24, 32)
					+ "\n");
			builder.append("GATE_NAME=" + new String(name.getBytes("utf-8"), "euc-kr") + "\n");
			builder.append("FIRST_CENTER_IP=" + doc.getElementsByTagName("first_ip").item(0).getTextContent() + "\n");
			builder.append("SECOND_CENTER_IP=" + doc.getElementsByTagName("second_ip").item(0).getTextContent() + "\n");
			builder.append("DR_CENTER_IP=" + doc.getElementsByTagName("dr_ip").item(0).getTextContent() + "\n");
			builder.append("MAC_PORT=" + doc.getElementsByTagName("mac_port").item(0).getTextContent() + "\n");
			builder.append("ENC_ALGO=" + doc.getElementsByTagName("enc_algo").item(0).getTextContent() + "\n");
			builder.append("AUTH_ALGO=" + doc.getElementsByTagName("auth_algo").item(0).getTextContent() + "\n");
			builder.append("SPD_INDEX=0\n");
			builder.append("PROTECTION_USE=" + doc.getElementsByTagName("protection").item(0).getTextContent() + "\n");
			builder.append("SMC_VERSION=" + doc.getElementsByTagName("smc_version").item(0).getTextContent() + "\n");
			fos = new FileOutputStream(new File("/etc/webadmin/conf/center_setup.conf"));
			fos.write(builder.toString().getBytes());
		} catch (IOException e) {
			logger.error("frodo xtmconf: cannot create center conf file", e);
		} finally {
			try {
				if (fos != null)
					fos.close();
			} catch (IOException e) {
				logger.error("frodo xtmconf: cannot close center conf file", e);
			}
		}
	}

	private static void makeCenterUseScript() {
		Logger logger = LoggerFactory.getLogger(Restore.class.getName());

		OutputStream fos = null;
		try {
			fos = new FileOutputStream(new File("/etc/webadmin/script/center_use.sh"));
			fos.write(new String("#!/bin/sh\n").getBytes());
			fos.write(new String("fmasd start > /dev/null 2>&1 &").getBytes());

			CommandUtil.run(new File("/etc/webadmin/script"), "chmod", "+x", "center_use.sh");
		} catch (IOException e) {
			logger.error("frodo xtmconf: cannot make center use script", e);
		} finally {
			try {
				if (fos != null)
					fos.close();
			} catch (IOException e) {
				logger.error("frodo xtmconf: cannot close center use script", e);
			}
		}
	}

	private static void makeMasterKey() {
		Logger logger = LoggerFactory.getLogger(Restore.class.getName());

		OutputStream fos = null;
		try {
			Document doc = XMLParser.createDoc(new File("/etc/webadmin/init_xml/center_setup.xml"));
			NodeList nList = XMLParser.getNodeList(doc, "master_key");
			String masterKey = nList.item(0).getTextContent();

			fos = new FileOutputStream(new File("/usr/local/key/masterkey"));

			for (int i = 2; i <= masterKey.length(); i += 2)
				fos.write(Integer.parseInt(masterKey.substring(i - 2, i), 16));
		} catch (Exception e) {
			logger.error("frodo xtmconf: cannot make master key", e);
		} finally {
			try {
				if (fos != null)
					fos.close();
			} catch (IOException e) {
				logger.error("frodo xtmconf: cannot close center setup", e);
			}
		}
	}

	private static void delSpdState() {
		// Logger logger = LoggerFactory.getLogger(Restore.class.getName());
		try {
			delSpdFiles();
			// delSpdStateTable();
		} catch (Exception e) {
			// logger.error("frodo xtmconf: cannot delete spd state", e);
		}
	}

	private static void delSpdFiles() throws IOException, InterruptedException {
		Logger logger = LoggerFactory.getLogger(Restore.class.getName());
		logger.trace("frodo xtmconf: deleting spd files " + XtmConfig.UTM_LOG + "stat");

		List<String> command = new ArrayList<String>();
		command.add("rm");
		command.add("-rf");
		File f = new File(XtmConfig.UTM_LOG + "stat");
		String[] list = f.list();
		for (String file : list)
			if (file.endsWith("spd"))
				command.add(file);

		CommandUtil.run(f, command);
	}
}