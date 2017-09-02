package kr.co.future.sslvpn.xtmconf;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Utils {
	private static Logger logger = LoggerFactory.getLogger(Utils.class);

	public static int getEthCount() {
		int count = 0;
		for (String name : getInterfaceNames()) {
			if (name.startsWith("eth"))
				count++;
		}
		return count;
	}

	public static int getInterfaceCount() {
		return getInterfaceNames().size();
	}

	public static List<String> getInterfaceNames() {
		File file = new File("/proc/utm/net/info_iface");

		if (!file.exists())
			return new ArrayList<String>();

		List<String> interfaceNames = new ArrayList<String>();
		BufferedReader reader = null;
		try {
			reader = new BufferedReader(new InputStreamReader(new FileInputStream(file)));
			while (true) {
				String line = reader.readLine();
				if (line == null)
					break;

				logger.trace("frodo xtmconf: checking interface info line [{}]", line);
				if (line.startsWith("eth") || line.startsWith("tap") || line.startsWith("tun")) {
					String name = line.substring(0, line.indexOf(' '));
					interfaceNames.add(name);
					logger.trace("frodo xtmconf: adding interface name [{}]", name);
				}
			}
		} catch (IOException e) {
			logger.error("frodo-xtmconf: io error", e);
		} finally {
			try {
				if (reader != null)
					reader.close();
			} catch (IOException e) {
				logger.error("frodo-xtmconf: close error", e);
			}
		}

		Collections.sort(interfaceNames);
		return interfaceNames;
	}

	public static String bool(boolean b) {
		return (b ? "on" : "off");
	}

	public static String ipVersion(String ip) {
		if (ip == null || ip.isEmpty())
			return null;

		String[] split = { "-", "~", "/" };

		for (String c : split) {
			if (ip.contains(c))
				ip = ip.substring(0, ip.indexOf(c));
		}

		if (ip.split("\\.").length == 4)
			return "v4";
		return "v6";
	}

	public static String ipType(String ip) {
		if (ip == null || ip.isEmpty()) {
			return null;
		} else if (ip.contains("-") || ip.contains("~")) {
			return "range";
		} else if (ip.contains("/")) {
			String s = ip.substring(ip.indexOf("/") + 1);
			if (s.split("\\.").length > 2 || s.contains(":"))
				return "netmask";
			else
				return "prefix";
		}

		return "single";
	}

	@SuppressWarnings("unchecked")
	public static Object getFromMap(Object map, String key) {
		if (!(map instanceof Map))
			return null;

		Map<String, Object> m = (Map<String, Object>) map;
		Object v = m.get(key);
		return v;
	}

	public static String getStringFromMap(Object map, String key) {
		Object v = getFromMap(map, key);
		if (v == null || !(v instanceof String))
			return null;
		return (String) v;
	}

	public static Double getDoubleFromMap(Object map, String key) {
		Object v = getFromMap(map, key);
		if (v == null || !(v instanceof Double))
			return null;
		return (Double) v;
	}

	public static Integer getIntegerFromMap(Object map, String key) {
		Object v = getFromMap(map, key);
		if (v == null || !(v instanceof Integer))
			return null;
		return (Integer) v;
	}

	public static Boolean getBooleanFromMap(Object map, String key) {
		Object v = getFromMap(map, key);
		if (v == null || !(v instanceof Boolean))
			return null;
		return (Boolean) v;
	}

	public static Date getDateFromMap(Object map, String key) {
		Object v = getFromMap(map, key);
		if (v == null)
			return null;
		else if (v instanceof Date)
			return (Date) v;
		else if (v instanceof String) {
			SimpleDateFormat dateFormat1 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ssZ");
			SimpleDateFormat dateFormat2 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
			try {
				return dateFormat1.parse((String) v);
			} catch (ParseException e) {
				try {
					return dateFormat2.parse((String) v);
				} catch (ParseException e1) {
					throw new IllegalArgumentException("invalid date format");
				}
			}
		} else
			return null;
	}

	@SuppressWarnings("unchecked")
	public static List<Object> getListFromMap(Object map, String key) {
		Object v = getFromMap(map, key);
		if (v == null || !(v instanceof List))
			return null;
		return (List<Object>) v;
	}

	@SuppressWarnings("unchecked")
	public static <T> List<T> convertList(Class<T> cls, Object list) {
		if (!(list instanceof List))
			return new ArrayList<T>();
		return (List<T>) list;
	}

	public static void setConfigNum(List<? extends XtmConfig> config) {
		setConfigNum(config, null);
	}

	public static void setConfigNum(List<? extends XtmConfig> config, Object type) {
		try {
			int num = 1;

			for (XtmConfig x : config) {
				if (type != null) {
					Object t = x.getClass().getMethod("getType").invoke(x);
					if (!type.equals(t))
						continue;
				}

				x.getClass().getMethod("setNum", int.class).invoke(x, num++);
			}
		} catch (Exception e) {
			logger.error("frodo-xtmconf: error in set object number", e);
			return;
		}
	}

	public static String dateFormat(Date date) {
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		return (date == null) ? null : dateFormat.format(date);
	}

	public static String cid(String cid) {
		if (cid != null)
			return cid;
		return "null";
	}

	private static final char[] chars = new char[52];
	static {
		char c = 'a';
		for (int i = 0; i < 26; i++)
			chars[i] = c++;
		c = 'A';
		for (int i = 26; i < 52; i++)
			chars[i] = c++;
	}

	public static String salt(int length) {
		Random random = new Random();
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < length; i++)
			sb.append(chars[random.nextInt(chars.length)]);
		return sb.toString();
	}
}
