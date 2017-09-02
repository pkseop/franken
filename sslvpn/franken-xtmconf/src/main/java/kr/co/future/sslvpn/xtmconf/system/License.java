package kr.co.future.sslvpn.xtmconf.system;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import kr.co.future.msgbus.Marshalable;
import kr.co.future.sslvpn.xtmconf.SSLTrustHelper;
import kr.co.future.sslvpn.xtmconf.XtmConfig;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class License implements Marshalable {
	private String key;
	private String serial;
	private String type;
	private String date;

	public static License load() throws IOException {
		License license = new License();

		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd");
		File keyPath = new File("/proc/utm/conf/founder");
		File datePath = new File("/proc/utm/conf/founder_date");
		File statPath = new File("/proc/utm/conf/license");
		File serialPath = new File("/proc/utm/serial");

		String key = "";
		String serial = "";
		int begin = 0;
		int end = 0;
		int stmode = 0;

		if (keyPath.exists() && keyPath.canRead())
			key = file(keyPath).get(0);

		if (serialPath.exists() && serialPath.canRead())
			serial = file(serialPath).get(0);

		if (datePath.exists() && datePath.canRead()) {
			String[] tokens = file(datePath).get(0).split(" ");
			begin = Integer.valueOf(tokens[0]);
			end = Integer.valueOf(tokens[1]);
		}

		if (statPath.exists() && statPath.canRead()) {
			stmode = Integer.valueOf(file(statPath).get(0));
		}

		if (stmode == 1 || stmode == 3) {
			license.setType("registered");
			if (begin != 0 && end != 0) {
				if ((begin > 2114348340 || begin < 1230757660 || end > 2114348340 || end < 1230757660))
					license.setType("license bad");
				else {
					license.setDate(String.format("%s - %s", dateFormat.format(new Date(begin * 1000L)),
							dateFormat.format(new Date(end * 1000L))));
				}
			} else {
				license.setDate("unlimit");
			}
		} else if (stmode == 2) {
			license.setType("expired");
			if (begin != 0 && end != 0) {
				license.setDate(String.format("%s - %s", dateFormat.format(new Date(begin * 1000L)),
						dateFormat.format(new Date(end * 1000L))));
			}
		} else if (stmode == 0) {
			license.setType("unregistered");
		} else {
			license.setType("license error");
		}

		license.setSerial(serial);
		license.setKey(key);

		return license;
	}

	public static String receive() throws IOException {
		Logger logger = LoggerFactory.getLogger(License.class.getName());

		boolean timesync = useTimesync();
		boolean domain = useDomain();

		logger.trace("frodo xtmconf: checking timesync [{}], domain [{}] for license issue", timesync, domain);

		if (timesync && domain) {
			BufferedReader br = null;
			try {
				SSLTrustHelper.trustAll();
				String url = String.format("https://license.future.co.kr/index.php?code=%s&s=%s&process=%d&cpuname=%s&cfid=%s",
						code(), serial(), process(), cpuname(), cfid());

				logger.info("frodo xtmconf: request license to " + url);

				URLConnection conn = new URL(url).openConnection();
				br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
				String result = "";
				while (true) {
					String s = br.readLine();
					if (s == null)
						break;
					result += s;
				}
				if (result.startsWith("Have already"))
					return result;
				else if (result.length() != 120)
					return "Serial is not registered on license server";
				else
					return result;
			} catch (Exception e) {
				logger.error("frodo xtmconf: cannot connect license server", e);
				return "Cannot connect server";
			} finally{
				if (br != null){
					try {
						br.close();
					} catch (IOException e) {
					}
				}
			}
		} else if (timesync && !domain)
			return "Check NameServer";
		else if (!timesync && domain)
			return "Check the timesync";
		else
			return "Check the timesync and NameServer";
	}

	private static boolean useTimesync() {
		List<Basic> conf = XtmConfig.readConfig(Basic.class);
		for (Basic basic : conf) {
			if (basic.getSyncType() != null)
				return true;
		}
		return false;
	}

	private static boolean useDomain() throws IOException {
		List<String> resolv = file("/etc/resolv.conf");
		for (String s : resolv) {
			if (s.contains(" ") && !s.substring(s.indexOf(" ")).trim().isEmpty())
				return true;
		}
		return false;
	}

	private static String code() throws IOException {
		List<String> founder = file("/proc/utm/conf/founder");
		return founder.get(0);
	}

	private static String serial() throws IOException {
		List<String> serial = file("/proc/utm/serial");
		return serial.get(0);
	}

	private static Integer process() throws IOException {
		Integer processor = null;
		for (String line : file("/proc/cpuinfo")) {
			if (line.startsWith("process"))
				processor = Integer.parseInt(line.split(":")[1].trim());
		}
		return processor + 1;
	}

	private static String cpuname() throws IOException {
		String name = null;
		for (String line : file("/proc/cpuinfo")) {
			if (line.startsWith("model name"))
				name = line.split(":")[1];
		}

		name = name.replace("(R)", "").replace("(TM)", "").replace(" ", "");

		return name;
	}

	private static String cfid() throws IOException {
		List<String> serial = file("/proc/utm/cfid");
		return serial.get(0).replace(" ", "");
	}

	public static void write(String authKey) throws IOException, InterruptedException {
		int ret = CommandUtil.run("/usr/sbin/validate", "-l", authKey);
		if (ret != 0)
			throw new IllegalStateException("invalid license");

		mountMsdosPartition();
		CommandUtil.run("cp", "-rf", "/etc/webadmin/license", "/mnt/kernel/");
		CommandUtil.run("umount", "/mnt/kernel");
		CommandUtil.run("rm", "-f", XtmConfig.UTM_LOG + "upgrade/backup_db/version_key", XtmConfig.UTM_LOG + "db/auth_mysql",
				XtmConfig.UTM_LOG + "time_hexdump");
		CommandUtil.run("sync");
	}

	public static void writeTemp() throws IOException, InterruptedException {
		String serial = file("/proc/utm/serial").get(0).trim();
		String license = file("/proc/utm/license").get(0).trim();
		String magicNumber = serial + "future_01";
		String date = process("date", "+%Y%m%d").get(0);
		String dateDummy = process("date", "+%s").get(0);
		int hashOk = 0;

		String versionKey = null;
		if (file(XtmConfig.UTM_LOG + "upgrade/backup_db/version_key") == null) {
			versionKey = hashHmac("SHA1", date, magicNumber);
			hashOk++;
		} else
			versionKey = hashHmac("RipeMD160", date + dateDummy, magicNumber);
		writeFile(XtmConfig.UTM_LOG + "upgrade/backup_db/version_key", versionKey);

		String authMysql = null;
		if (file(XtmConfig.UTM_LOG + "db/auth_mysql") == null) {
			authMysql = hashHmac("SHA1", versionKey, versionKey);
			hashOk++;
		} else
			authMysql = hashHmac("RipeMD160", versionKey, versionKey + dateDummy);
		writeFile(XtmConfig.UTM_LOG + "db/auth_mysql", authMysql);

		String timeHexdump = null;
		if (file(XtmConfig.UTM_LOG + "time_hexdump") == null) {
			timeHexdump = hashHmac("SHA1", authMysql, authMysql);
			hashOk++;
		} else
			timeHexdump = hashHmac("RipeMD160", authMysql, authMysql + dateDummy);
		writeFile(XtmConfig.UTM_LOG + "time_hexdump", timeHexdump);

		if (hashOk == 3 && license.isEmpty()) {
			CommandUtil.run("/usr/sbin/validate", "-l", "auth", "-s", date);
			if (file("/etc/webadmin/license") != null) {
				CommandUtil.run("/usr/local/bin/flogs", "reinit");
				mountMsdosPartition();
				CommandUtil.run("cp", "-rf", "/etc/webadmin/license", "/mnt/kernel/");
				CommandUtil.run("umount", "/mnt/kernel");
				CommandUtil.run("sync");
			}
		} else
			throw new IllegalStateException("Have already been issued a temp_license");
	}

	private static String hashHmac(String algorithm, String value, String key) {
		try {
			byte[] keyBytes = key.getBytes();
			SecretKeySpec signingKey = new SecretKeySpec(keyBytes, "Hmac" + algorithm);

			Mac mac = Mac.getInstance("Hmac" + algorithm);
			mac.init(signingKey);

			byte[] rawHmac = mac.doFinal(value.getBytes());

			return convertToHex(rawHmac);
		} catch (Exception e) {
			return null;
		}
	}

	private static String convertToHex(byte[] data) {
		StringBuffer buf = new StringBuffer();
		for (int i = 0; i < data.length; i++) {
			int halfbyte = (data[i] >>> 4) & 0x0F;
			int two_halfs = 0;
			do {
				if ((0 <= halfbyte) && (halfbyte <= 9))
					buf.append((char) ('0' + halfbyte));
				else
					buf.append((char) ('a' + (halfbyte - 10)));
				halfbyte = data[i] & 0x0F;
			} while (two_halfs++ < 1);
		}
		return buf.toString();
	}

	private static void writeFile(String filename, String value) {
		File f = new File(filename);
		OutputStream os = null;
		try {
			os = new FileOutputStream(f);
			os.write(value.getBytes());
		} catch (IOException e) {
		} finally {
			if (os != null) {
				try {
					os.close();
				} catch (IOException e) {
				}
			}
		}
	}

	private static boolean mountMsdosPartition() {
		try {
			List<String> firmdev = file("/etc/firmdev");
			if (firmdev == null)
				return false;

			CommandUtil.run("mount", "-t", "msdos", firmdev.get(0), "/mnt/kernel");
			return true;
		} catch (Exception e) {
		}
		return false;
	}

	private static List<String> process(String... commands) throws IOException {
		Process p = new ProcessBuilder(commands).directory(new File("/")).start();
		try {
			p.waitFor();
		} catch (InterruptedException e) {
		}
		List<String> result = new ArrayList<String>();
		BufferedReader br = null;
		try {
			br = new BufferedReader(new InputStreamReader(p.getInputStream()));
			while (true) {
				String s = br.readLine();
				if (s == null)
					break;
				result.add(s);
			}
		} finally {
			if (br != null)
				br.close();
		}
		return result;
	}

	private static List<String> file(String s) throws IOException {
		return file(new File(s));
	}

	private static List<String> file(File f) throws IOException {
		List<String> l = new ArrayList<String>();
		BufferedReader br = null;

		try {
			try {
				br = new BufferedReader(new InputStreamReader(new FileInputStream(f)));
			} catch (FileNotFoundException e) {
				return null;
			}

			while (true) {
				String line = br.readLine();

				if (line == null)
					break;

				l.add(line);
			}

		} finally {
			if (br != null)
				br.close();
		}

		return l;
	}

	public String getKey() {
		return key;
	}

	public void setKey(String key) {
		this.key = key;
	}

	public String getSerial() {
		return serial;
	}

	public void setSerial(String serial) {
		this.serial = serial;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getDate() {
		return date;
	}

	public void setDate(String date) {
		this.date = date;
	}

	@Override
	public Map<String, Object> marshal() {
		Map<String, Object> m = new HashMap<String, Object>();
		m.put("key", key);
		m.put("serial", serial);
		m.put("type", type);
		m.put("date", date);
		return m;
	}

}
