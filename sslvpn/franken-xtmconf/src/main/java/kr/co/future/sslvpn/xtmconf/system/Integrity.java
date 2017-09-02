package kr.co.future.sslvpn.xtmconf.system;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import kr.co.future.sslvpn.xtmconf.KLogWriter;

public class Integrity {
	private static final String xmlDir = "/etc/webadmin/xml";
	private static final String daemonDir = "/usr/sbin";
	private static final String usrbinDir = "/usr/bin";
	private static final String binDir = "/bin";
	private static final String sbinDir = "/sbin";
	private static final String localDir = "/usr/local";

	public static Map<String, Object> checkIntegrity() throws IOException, NoSuchAlgorithmException,
			InterruptedException {
		if (!new File(xmlDir).exists())
			return null;

		KLogWriter.write(0x12020007, null, "start check integrity");

		Map<String, Object> m = new HashMap<String, Object>();

		List<String> command = new ArrayList<String>();
		File xmls = new File(xmlDir);
		command.add("tar");
		command.add("cvf");
		command.add("hash.tar");

		String[] list = xmls.list();
		Arrays.sort(list);

		for (String fileName : list) {
			command.add(fileName);
		}

		Process p = new ProcessBuilder(command).directory(new File(xmlDir)).start();
		p.waitFor();

		String xmlHash = getHash(xmlDir + "/hash.tar");
		File f = new File(xmlDir + "/hash.tar");
		f.delete();

		String flogsHash = getHash(localDir + "/bin/" + "flogs");
		String flookHash = getHash(daemonDir + "/flook");
		String snmpdHash = getHash(daemonDir + "/snmpd");
		String iptablesHash = getHash(daemonDir + "/iptables");
		String ip6tablesHash = getHash(daemonDir + "/ip6tables");
		String sslvpnHash = getHash(daemonDir + "/sslvpn");
		String zebraHash = getHash(sbinDir + "/zebra");
		String phpHash = getHash(usrbinDir + "/php");
		String busyboxHash = getHash(binDir + "/busybox");
		String mysqlHash = getHash(localDir + "/mysql/libexec/mysqld");
		String fmondHash = getHash(localDir + "/rrdtool/fmond");

		File originalHashFile = new File("/usr/local/var/hash");
		if (!originalHashFile.exists()) {
			List<String> hashList = new ArrayList<String>();
			hashList.add(xmlHash);
			hashList.add(flogsHash);
			hashList.add(flookHash);
			hashList.add(snmpdHash);
			hashList.add(iptablesHash);
			hashList.add(ip6tablesHash);
			hashList.add(sslvpnHash);
			hashList.add(zebraHash);
			hashList.add(phpHash);
			hashList.add(busyboxHash);
			hashList.add(mysqlHash);
			hashList.add(fmondHash);
			createHashFile(originalHashFile, hashList);
		}

		BufferedReader br = new BufferedReader(new InputStreamReader((new FileInputStream(originalHashFile))));
		String line;

		while ((line = br.readLine()) != null) {
			if (line.startsWith("XML_HASH")) {
				if (!xmlHash.equals(line.substring(9)))
					putHashValue(m, "XML", xmlHash, line.substring(9));
			} else if (line.startsWith("FLOGS_HASH")) {
				if (!flogsHash.equals(line.substring(11)))
					putHashValue(m, "FLOGS", flogsHash, line.substring(11));
			} else if (line.startsWith("FLOOK_HASH")) {
				if (!flookHash.equals(line.substring(11)))
					putHashValue(m, "FLOOK", flookHash, line.substring(11));
			} else if (line.startsWith("SNMPD_HASH")) {
				if (!snmpdHash.equals(line.substring(11)))
					putHashValue(m, "SNMPD", snmpdHash, line.substring(11));
			} else if (line.startsWith("IPTABLES_HASH")) {
				if (!iptablesHash.equals(line.substring(14)))
					putHashValue(m, "IPTABLES", iptablesHash, line.substring(14));
			} else if (line.startsWith("IP6TABLES_HASH")) {
				if (!ip6tablesHash.equals(line.substring(15)))
					putHashValue(m, "IP6TABLES", ip6tablesHash, line.substring(15));
			} else if (line.startsWith("SSLVPN_HASH")) {
				if (!sslvpnHash.equals(line.substring(12)))
					putHashValue(m, "SSLVPN", sslvpnHash, line.substring(12));
			} else if (line.startsWith("ZEBRA_HASH")) {
				if (!zebraHash.equals(line.substring(11)))
					putHashValue(m, "ZEBRA", zebraHash, line.substring(11));
			} else if (line.startsWith("PHP_HASH")) {
				if (!phpHash.equals(line.substring(9)))
					putHashValue(m, "PHP", phpHash, line.substring(9));
			} else if (line.startsWith("BUSYBOX_HASH")) {
				if (!busyboxHash.equals(line.substring(13)))
					putHashValue(m, "BUSYBOX", busyboxHash, line.substring(13));
			} else if (line.startsWith("MYSQL_HASH")) {
				if (!mysqlHash.equals(line.substring(11)))
					putHashValue(m, "MYSQL", mysqlHash, line.substring(11));
			} else if (line.startsWith("FMOND_HASH")) {
				if (!fmondHash.equals(line.substring(11)))
					putHashValue(m, "FMOND", fmondHash, line.substring(11));
			}
		}
		if (br != null){
			try {
				br.close();
			} catch (IOException e) {
			}
		}
		if (m.isEmpty())
			KLogWriter.write(0x12030009, null, "check success.");
		else
			KLogWriter.write(0x1205000a, null, "check fail");

		KLogWriter.write(0x12020008, null, "end check integrity");
		return m;
	}

	private static void putHashValue(Map<String, Object> m, String fileName, String currentHash, String originalHash) {
		Map<String, String> m2 = new HashMap<String, String>();
		m2.put("current", currentHash);
		m2.put("original", originalHash);
		m.put(fileName, m2);
	}

	private static String getHash(String filePath) throws NoSuchAlgorithmException {
		try {
			File f = new File(filePath);
			InputStream is = new FileInputStream(f.getAbsolutePath());

			byte[] content = new byte[(int) f.length()];
			is.read(content);
			if (is != null){
				try {
					is.close();
				} catch (IOException e) {
				}
			}
			MessageDigest md = MessageDigest.getInstance("SHA-256");
			md.reset();
			md.update(content);

			byte[] mdbytes = md.digest();
			StringBuffer xmlHash = new StringBuffer();
			for (int i = 0; i < mdbytes.length; i++) {
				String hexStr = Integer.toHexString(0xff & mdbytes[i]);
				if (hexStr.length() <= 1)
					xmlHash.append("0");
				xmlHash.append(hexStr);
			}
			return xmlHash.toString();
		} catch (IOException e) {
			return "null";
		}
	}

	private static void createHashFile(File f, List<String> hashList) throws IOException {
		StringBuilder builder = new StringBuilder();
		builder.append("XML_HASH=" + hashList.get(0) + "\n");
		builder.append("FLOGS_HASH=" + hashList.get(1) + "\n");
		builder.append("FLOOK_HASH=" + hashList.get(2) + "\n");
		builder.append("SNMPD_HASH=" + hashList.get(3) + "\n");
		builder.append("IPTABLES_HASH=" + hashList.get(4) + "\n");
		builder.append("IP6TABLES_HASH=" + hashList.get(5) + "\n");
		builder.append("SSLVPN_HASH=" + hashList.get(6) + "\n");
		builder.append("ZEBRA_HASH=" + hashList.get(7) + "\n");
		builder.append("PHP_HASH=" + hashList.get(8) + "\n");
		builder.append("BUSYBOX_HASH=" + hashList.get(9) + "\n");
		builder.append("MYSQL_HASH=" + hashList.get(10) + "\n");
		builder.append("FMOND_HASH=" + hashList.get(11) + "\n");

		OutputStream fos = new FileOutputStream(f);
		fos.write(builder.toString().getBytes());
		fos.close();
	}
}