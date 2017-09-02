package kr.co.future.sslvpn.core.impl;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Map;

import kr.co.future.sslvpn.core.AuthService;
import kr.co.future.sslvpn.core.log.XtmLog;
import kr.co.future.sslvpn.xtmconf.LastHitService;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Invalidate;
import org.apache.felix.ipojo.annotations.Requires;
import org.apache.felix.ipojo.annotations.Validate;

import kr.co.future.logstorage.Log;
import kr.co.future.logstorage.LogStorage;
import kr.co.future.logstorage.LogStorageStatus;
import kr.co.future.logstorage.LogTableRegistry;
import kr.co.future.msgbus.PushApi;
import kr.co.future.sslvpn.core.impl.SyslogCollector;
import kr.co.future.syslog.Syslog;
import kr.co.future.syslog.SyslogListener;
import kr.co.future.syslog.SyslogServerRegistry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(name = "frodo-syslog-collector")
public class SyslogCollector implements SyslogListener {
	private final Logger logger = LoggerFactory.getLogger(SyslogCollector.class.getName());

	@Requires
	private SyslogServerRegistry syslogRegistry;

	@Requires
	private LogTableRegistry tableRegistry;

	@Requires
	private LogStorage storage;

	@Requires
	private PushApi pushApi;

	// for XAuth and L2TP tunnel synchronize
	@Requires
	private AuthService auth;

	@Requires
	private LastHitService lasthit;

	@Validate
	public void start() {
		if (!tableRegistry.exists("xtm")) {
			storage.createTable("xtm");
			logger.info("frodo core: created xtm logtable");
		}

		syslogRegistry.addSyslogListener(this);
	}

	@Invalidate
	public void stop() {
		if (syslogRegistry != null)
			syslogRegistry.removeSyslogListener(this);
	}

	@Override
	public void onReceive(Syslog syslog) {
		// Facility: system daemons (3) only
		if (syslog.getFacility() != -1 && syslog.getFacility() != 3)
			return;

		String line = syslog.getMessage();
		try {
			XtmLog xtmlog = parse(line);
			if (xtmlog != null) {
				logger.trace("frodo core: saved xtm syslog [{}]", xtmlog);
				Log log = xtmlog.toLog();
				push(log);

				if (storage.getStatus() == LogStorageStatus.Open)
					storage.write(log);

				// for tunnel sync
//				handleL2tpLogs(xtmlog);
//				handleXAuthLogs(xtmlog);

				// for integrity alert
				handleIntegrityLogs(xtmlog);

				// log type is fw
				if (xtmlog.getCategory().equals("fw"))
					updateLastHit(xtmlog);
			}

		} catch (Exception e) {
			logger.error("frodo core: log error", e);
			logger.error("log with error: {}", line);
		}
	}

//	private void handleL2tpLogs(XtmLog log) throws UnknownHostException {
//		String note = log.getNote();
//		long logtype = log.getLogType();
//
//		logger.debug("frodo core: start l2tp log handling, {}", logtype);
//
//		if (logtype != 0x59040002 && logtype != 0x59040004 && logtype != 0x59040005)
//			return;
//
//		logger.debug("frodo core: start l2tp log handling #2");
//
//		String[] tokens = splitNote(note);
//		String loginName = tokens[0];
//		InetAddress leaseIp = InetAddress.getByName(tokens[1]);
//		InetSocketAddress remote = new InetSocketAddress(InetAddress.getByName(tokens[2]), Integer.valueOf(tokens[3]));
//
//		// l2tp auth success
//		if (logtype == 0x59040002)
//			auth.openL2TPTunnel(loginName, leaseIp, remote);
//
//		// l2tp connect close
//		if (logtype == 0x59040004)
//			auth.closeL2TPTunnel(leaseIp);
//
//		// l2tp duplicated user session close
//		if (logtype == 0x59040005)
//			auth.closeL2TPTunnel(leaseIp);
//	}
//
//	private void handleXAuthLogs(XtmLog log) throws UnknownHostException {
//		String note = log.getNote();
//		long logtype = log.getLogType();
//
//		logger.debug("frodo core: start xauth log handling, {}", logtype);
//		if (logtype != 0x30040027 && logtype != 0x30040028)
//			return;
//
//		logger.debug("frodo core: start xauth log handling #2");
//
//		String[] tokens = splitNote(note);
//		String loginName = tokens[0];
//		InetAddress leaseIp = InetAddress.getByName(tokens[1]);
//		InetSocketAddress remote = new InetSocketAddress(InetAddress.getByName(tokens[2]), Integer.valueOf(tokens[3]));
//
//		// xauth client ip lease
//		if (logtype == 0x30040027)
//			auth.openXAuthTunnel(loginName, leaseIp, remote);
//
//		// xauth client ip release
//		if (logtype == 0x30040028)
//			auth.closeXAuthTunnel(leaseIp);
//	}

	private void updateLastHit(XtmLog log) {
		// normal session close code : 0x2004000b
		// ipv6 to ipv4 change adress session close code : 0x20040013
		// ipv4 to ipv6 change adress session close code : 0x20040014
		if (log.getLogType() == 537133067) {
			lasthit.hit(log.getRule());
		}
	}

	private String[] splitNote(String note) {
		int begin = note.indexOf('(');
		int end = note.indexOf(')');
		String props = note.substring(begin + 1, end);
		return props.split(":");
	}

	private void handleIntegrityLogs(XtmLog log) {

	}

	private void push(Log log) {
		Map<String, Object> m = new HashMap<String, Object>(log.getData());
		m.put("_date", log.getDate());
		m.put("_id", log.getId());
		pushApi.push("localhost", "frodo-xtm-log", m);
	}

	public static XtmLog parse(String line) {
		String[] tokens = split(line);
		int type = Integer.valueOf(tokens[0]);

		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd HHmmss");

		XtmLog log = new XtmLog();
		if (type == 0)
			log.setType("xtm");
		else if (type == 1)
			log.setType("fw");
		else if (type == 2)
			log.setType("ddos");

		try {
			log.setDate(dateFormat.parse(tokens[1]));
		} catch (ParseException e) {
			Logger logger = LoggerFactory.getLogger(SyslogCollector.class.getName());
			logger.error("frodo core: cannot parse date, original msg [{}]", line);
			return null;
		}

		log.setOriginIp(tokens[2]);
		log.setSourceIp(tokens[3]);
		log.setNatSourceIp(tokens[4]);
		log.setSourcePort(parseInt(tokens[5]));
		log.setNatSourcePort(parseInt(tokens[6]));
		log.setDestinationIp(tokens[7]);
		log.setNatDestinationIp(tokens[8]);
		log.setDestinationPort(parseInt(tokens[9]));
		log.setNatDestinationPort(parseInt(tokens[10]));
		log.setProtocol(toProtocol(parseInt(tokens[11])));
		log.setCategory(toCategory(parseInt(tokens[12])));
		log.setLogType(parseInt(tokens[13]) & 0xffffffffl);
		log.setLevel(parseInt(tokens[14]));
		log.setProduct(tokens[15]);
		log.setNote(tokens[16]);
		log.setRule(tokens[19]);
		log.setUsage(parseLong(tokens[21]));
		log.setIface(tokens[23]);

		return log;
	}

	private static String toCategory(int num) {
		switch (num) {
		case 0:
			return "system";
		case 1:
			return "fw";
		case 2:
			return "dpi";
		case 3:
			return "network";
		case 4:
			return "av";
		case 5:
			return "nac";
		case 6:
			return "waf";
		case 7:
			return "vpn";
		case 8:
			return "ssl";
		case 9:
			return "etc";
		default:
			return "" + num;
		}
	}

	private static String toProtocol(int num) {
		switch (num) {
		case 0:
			return "ip";
		case 1:
			return "icmp";
		case 3:
			return "ggp";
		case 6:
			return "tcp";
		case 8:
			return "egp";
		case 12:
			return "pup";
		case 17:
			return "udp";
		default:
			return "" + num;
		}
	}

	private static String[] split(String line) {
		String[] tokens = new String[27];
		int last = 0;

		int index = 0;
		for (int i = 0; i < line.length(); i++) {
			if (line.charAt(i) == ';') {
				tokens[index++] = line.substring(last, i).trim();
				last = i + 1;
			}
		}

		tokens[index] = line.substring(last);

		return tokens;
	}

	private static Long parseLong(String token) {
		if (token == null || token.isEmpty())
			return null;

		return Long.valueOf(token);
	}

	private static Integer parseInt(String token) {
		if (token == null || token.isEmpty())
			return null;

		return Integer.valueOf(token);
	}

}
