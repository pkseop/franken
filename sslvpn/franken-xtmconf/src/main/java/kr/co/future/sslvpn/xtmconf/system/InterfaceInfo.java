package kr.co.future.sslvpn.xtmconf.system;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import kr.co.future.msgbus.Marshalable;

public class InterfaceInfo implements Marshalable {
	private String name;
	private boolean up;
	private String port;
	private String mac;
	private String speed;
	private String duplex;
	private List<String> ipAddr = new ArrayList<String>();
	private Long error;
	private Long collision;
	private String mode;

	public static List<InterfaceInfo> getInterfaceInfos() {
		List<InterfaceInfo> ifaceInfos = new ArrayList<InterfaceInfo>();

		Map<String, String[]> netDev = new HashMap<String, String[]>();
		List<String> dev = readFileLines("/proc/net/dev");
		for (int i = 2; i < dev.size(); i++) {
			String d = dev.get(i);
			String[] v = d.trim().replaceAll(" +", " ").split(" ");
			netDev.put(v[0].substring(0, v[0].length() - 1), v);
		}

		List<String> infos = readFileLines("/proc/utm/net/info_iface");
		InterfaceInfo info = null;
		if (infos != null) {
			for (String l : infos) {
				String[] tokens = l.trim().split(" ");
				if (tokens.length == 2) {
					info.ipAddr.add(tokens[1]);
				} else if (tokens.length == 3) {
					info.ipAddr.add(tokens[1] + "/" + tokens[2]);
				} else if (tokens.length == 4) {
					if (info != null)
						ifaceInfos.add(info);

					info = new InterfaceInfo();
					info.name = tokens[0];
					info.mac = tokens[2];
					if (tokens[3].equals("1"))
						info.mode = "Internal";
					else if (tokens[3].equals("2"))
						info.mode = "External";
					else if (tokens[3].equals("3"))
						info.mode = "DMZ";

					List<String> ethtool = exec("ethtool " + info.name);
					for (String t : ethtool) {
						t = t.trim();
						if (t.startsWith("Speed"))
							info.speed = t.substring(t.indexOf(":") + 1).trim();
						else if (t.startsWith("Duplex"))
							info.duplex = t.substring(t.indexOf(":") + 1).trim();
						else if (t.startsWith("Port"))
							info.port = t.substring(t.indexOf(":") + 1).trim();
					}

					List<String> ifconfig = exec("ifconfig " + info.name);
					for (String line : ifconfig) {
						if (line.contains("UP"))
							info.up = true;
					}

					if (netDev.containsKey(info.name)) {
						String[] v = netDev.get(info.name);
						info.error = Long.parseLong(v[11]);
						info.collision = Long.parseLong(v[14]);
					}
				}
			}
		}
		if (info != null)
			ifaceInfos.add(info);

		return ifaceInfos;
	}

	private static List<String> readFileLines(String path) {
		File f = new File(path);
		if (!f.exists())
			return null;

		List<String> str = new ArrayList<String>();
		BufferedReader br = null;
		try {
			br = new BufferedReader(new InputStreamReader(new FileInputStream(f)));
			while (true) {
				String s = br.readLine();
				if (s == null)
					break;
				str.add(s);
			}
		} catch (IOException e) {
		} finally {
			try {
				if (br != null)
					br.close();
			} catch (IOException e) {
			}
		}

		return str;
	}

	private static List<String> exec(String cmd) {
		Process p = null;
		BufferedReader br = null;
		List<String> str = new ArrayList<String>();
		try {
			p = Runtime.getRuntime().exec(cmd);
			br = new BufferedReader(new InputStreamReader(p.getInputStream()));
			try {
				p.waitFor();
			} catch (InterruptedException e) {
			}
			while (true) {
				String s = br.readLine();
				if (s == null)
					break;
				str.add(s);
			}
		} catch (IOException e) {
		} finally {
			try {
				if (br != null)
					br.close();
			} catch (IOException e) {
			}
		}
		return str;
	}

	public String getName() {
		return name;
	}

	public boolean isUp() {
		return up;
	}

	public String getPort() {
		return port;
	}

	public String getMac() {
		return mac;
	}

	public String getSpeed() {
		return speed;
	}

	public String getDuplex() {
		return duplex;
	}

	public List<String> getIpAddr() {
		return ipAddr;
	}

	public Long getError() {
		return error;
	}

	public Long getCollision() {
		return collision;
	}

	public String getMode() {
		return mode;
	}

	@Override
	public Map<String, Object> marshal() {
		Map<String, Object> m = new HashMap<String, Object>();
		m.put("name", name);
		m.put("up", up);
		m.put("port", port);
		m.put("mac", mac);
		m.put("speed", speed);
		m.put("duplex", duplex);
		m.put("ip_addr", ipAddr);
		m.put("error", error);
		m.put("collision", collision);
		m.put("ip_mode", mode);
		return m;
	}
}
