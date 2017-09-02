package kr.co.future.sslvpn.xtmconf.system;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;

import kr.co.future.msgbus.Session;
import kr.co.future.sslvpn.xtmconf.KLogWriter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ServiceControl {
	private static final String STOP_SERVICE_FILE = "/usr/local/etc/stop_service";
	private static final String CONF_HOME = "/etc/webadmin/conf";

	public static void halt() throws IOException {
		Runtime.getRuntime().exec("halt");
	}

	public static void startService(Session session) throws IOException {
		Logger logger = LoggerFactory.getLogger(ServiceControl.class.getName());

		// delete stop policy file
		File stopPolicy = new File(CONF_HOME, "fw_service_stop.policy");
		boolean result = stopPolicy.delete();
		if (!result)
			logger.error("frodo xtmconf: cannot delete " + stopPolicy.getAbsolutePath());

		File stopService = new File(STOP_SERVICE_FILE);
		result = stopService.delete();
		if (!result)
			logger.error("frodo xtmconf: cannot delete " + stopService.getAbsolutePath());

		Runtime.getRuntime().exec("/usr/sbin/iptables-restore " + CONF_HOME + "/firewall_policy.policy");
		KLogWriter.write(0x12030012, session, "Service Restart");
	}

	public static void stopService(Session session) throws IOException {
		File f = new File(CONF_HOME, "fw_service_stop.policy");
		Charset utf8 = Charset.forName("utf-8");

		FileOutputStream os = null;
		try {
			os = new FileOutputStream(f);
			OutputStreamWriter w = new OutputStreamWriter(os, utf8);
			w.append("*filter   \n");
			w.append(":INPUT ACCEPT [0:0] \n");
			w.append(":FORWARD ACCEPT [0:0]\n");
			w.append(":OUTPUT ACCEPT [0:0]\n");

			w.append("-A FORWARD -m state --state ESTABLISHED,RELATED -j ACCEPT\n");
			w.append("-A FORWARD -p tcp -m tcp ! --tcp-flags FIN,SYN,RST,ACK SYN -m state --state NEW -j DROP\n");
			w.append("-A FORWARD -s 127.0.0.1 -d 127.0.0.1 -j ACCEPT\n");

			int httpsPort = getHttpsPort();
			int sshdPort = getSshdPort();
			int webPort = getWebPort();
			int websocketPort = getWebsocketPort();
			w.append(String.format("-A FORWARD -S 1 -p tcp --dport %d:%d -j ACCEPT\n", httpsPort, httpsPort));
			w.append(String.format("-A FORWARD -S 2 -p tcp --dport %d:%d -j ACCEPT\n", sshdPort, sshdPort));
			w.append(String.format("-A FORWARD -S 3 -p tcp --dport %d:%d -j ACCEPT\n", webPort, webPort));
			w.append(String.format("-A FORWARD -S 4 -p tcp --dport %d:%d -j ACCEPT\n", websocketPort, websocketPort));

			// skip center forwarding
			// skip RTM check_use setting

			w.append("-A OUTPUT -S 0 -p icmp -j DROP\n");
			w.append("-A FORWARD -S 5 -j DROP\n");
			w.append("-A FORWARD -S 6 -j DROP\n");
			w.append("COMMIT\n");
			w.close();
			os.close();

			Runtime.getRuntime().exec("/usr/sbin/iptables-restore " + CONF_HOME + "/fw_service_stop.policy");
			KLogWriter.write(0x12050011, session, "Service Stop");

			writeStopServiceFile();

			// TODO: add log

		} finally {
			// ensure file close
			if (os != null)
				os.close();
		}
	}

	public static boolean isServiceRunning() {
		return !(new File(STOP_SERVICE_FILE).exists());
	}

	private static int getWebsocketPort() {
		return 4502;
	}

	private static int getWebPort() {
		return 943;
	}

	private static void writeStopServiceFile() throws IOException {
		File f = new File(STOP_SERVICE_FILE);
		FileOutputStream os = null;

		try {
			Charset utf8 = Charset.forName("utf-8");
			os = new FileOutputStream(f);
			OutputStreamWriter w = new OutputStreamWriter(os, utf8);
			w.append("STOP SERVICE");
			w.close();
		} finally {
			if (os != null)
				os.close();
		}
	}

	private static int getHttpsPort() throws IOException {
		return 443;
		// FileInputStream is = null;
		// try {
		// is = new FileInputStream(new File("/etc/lighttpd.conf"));
		// BufferedReader br = new BufferedReader(new InputStreamReader(is));
		//
		// while (true) {
		// String line = br.readLine();
		// if (line == null)
		// break;
		//
		// line = line.trim();
		// if (line.startsWith("server.port")) {
		// String[] tokens = line.split("=");
		// return Integer.valueOf(tokens[1].trim());
		// }
		// }
		//
		// } finally {
		// if (is != null)
		// is.close();
		// }
		//
		// return -1;
	}

	private static int getSshdPort() throws IOException {
		FileInputStream is = null;
		BufferedReader br = null;
		try {
			is = new FileInputStream(new File("/etc/webadmin/conf/sshd_config"));
			br = new BufferedReader(new InputStreamReader(is));

			while (true) {
				String line = br.readLine();
				if (line == null)
					break;

				line = line.trim();
				if (line.startsWith("Port") || line.startsWith("#Port")) {
					String[] tokens = line.split(" ");
					return Integer.valueOf(tokens[1].trim());
				}
			}

		} finally {
			if (br != null){
				try {
					br.close();
				} catch (IOException e) {
				}
			}
			if (is != null)
				is.close();
		}

		return -1;
	}

}
