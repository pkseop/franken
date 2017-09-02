package kr.co.future.sslvpn.xtmconf.network;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import kr.co.future.msgbus.Marshalable;
import kr.co.future.sslvpn.xtmconf.XtmConfig;

public class DhcpLease implements Marshalable {
	private String ip;
	private Date begin;
	private Date end;
	private String mac;
	private String hostName;

	public static List<DhcpLease> load() throws IOException {
		List<DhcpLease> logs = new LinkedList<DhcpLease>();

		BufferedReader br = null;
		try {
			br = new BufferedReader(new InputStreamReader(new FileInputStream(new File(XtmConfig.UTM_LOG
					+ "etc/dhcpd.lease"))));
			DhcpLease lease = null;
			SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");

			// all times in dhcpd.lease are written in UTC (GMT)
			Calendar cal = Calendar.getInstance();
			int zoneOffset = cal.get(Calendar.ZONE_OFFSET);

			while (true) {
				String line = br.readLine();
				if (line == null)
					break;

				line = line.trim();
				if (line.endsWith(";"))
					line = line.substring(0, line.length() - 1);

				if (line.startsWith("#") || line.isEmpty())
					continue;

				if (line.startsWith("lease")) {
					String[] tokens = line.split(" ");
					lease = new DhcpLease();
					lease.setIp(tokens[1].trim());
				} else if (line.startsWith("starts")) {
					String[] tokens = line.split(" ");
					Date starts = dateFormat.parse(tokens[2] + " " + tokens[3]);
					lease.setBegin(new Date(starts.getTime() + zoneOffset));
				} else if (line.startsWith("ends")) {
					String[] tokens = line.split(" ");
					Date ends = dateFormat.parse(tokens[2] + " " + tokens[3]);
					lease.setEnd(new Date(ends.getTime() + zoneOffset));
				} else if (line.startsWith("hardware")) {
					String[] tokens = line.split(" ");
					lease.setMac(tokens[2]);
				} else if (line.startsWith("client-hostname")) {
					String[] tokens = line.split(" ");
					lease.setHostName(tokens[1]);
				} else if (line.equals("}")) {
					logs.add(lease);
					lease = null;
				}
			}
		} catch (ParseException e) {
		} finally {
			if (br != null)
				try {
					br.close();
				} catch (IOException e) {
				}
		}
		return logs;
	}

	public String getIp() {
		return ip;
	}

	public void setIp(String ip) {
		this.ip = ip;
	}

	public Date getBegin() {
		return begin;
	}

	public void setBegin(Date begin) {
		this.begin = begin;
	}

	public Date getEnd() {
		return end;
	}

	public void setEnd(Date end) {
		this.end = end;
	}

	public String getMac() {
		return mac;
	}

	public void setMac(String mac) {
		this.mac = mac;
	}

	public String getHostName() {
		return hostName;
	}

	public void setHostName(String hostName) {
		this.hostName = hostName;
	}

	@Override
	public Map<String, Object> marshal() {
		Map<String, Object> m = new HashMap<String, Object>();
		m.put("ip", ip);
		m.put("begin", begin);
		m.put("end", end);
		m.put("mac", mac);
		m.put("hostname", hostName);
		return m;
	}
}
