package kr.co.future.sslvpn.syslog.impl;

import java.io.File;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import kr.co.future.sslvpn.core.AuthCode;
import kr.co.future.sslvpn.core.PerformanceMonitor;
import kr.co.future.sslvpn.core.cluster.ClusterNode;
import kr.co.future.sslvpn.core.cluster.ClusterService;
import kr.co.future.sslvpn.core.log.AuthLog;
import kr.co.future.sslvpn.core.log.FlowLog;
import kr.co.future.sslvpn.core.log.OID;
import kr.co.future.sslvpn.core.log.SystemLog;
import kr.co.future.sslvpn.xtmconf.system.InterfaceInfo;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Invalidate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.apache.felix.ipojo.annotations.Validate;

import kr.co.future.confdb.Config;
import kr.co.future.confdb.ConfigDatabase;
import kr.co.future.confdb.ConfigIterator;
import kr.co.future.confdb.ConfigService;
import kr.co.future.confdb.Predicate;
import kr.co.future.confdb.Predicates;
import kr.co.future.dom.api.UserApi;
import kr.co.future.dom.model.User;
import kr.co.future.linux.api.CpuStat;
import kr.co.future.linux.api.MemoryStat;
import kr.co.future.snmp.SnmpTrap;
import kr.co.future.snmp.SnmpTrapReceiver;
import kr.co.future.snmp.SnmpTrapService;
import kr.co.future.sslvpn.syslog.SyslogRelayConfig;
import kr.co.future.sslvpn.syslog.SyslogRelayServer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(name = "frodo-syslog-relay-server")
@Provides(specifications = { SyslogRelayServer.class })
public class SyslogRelayServerImpl implements SnmpTrapReceiver, SyslogRelayServer {
	private final Logger logger = LoggerFactory.getLogger(SyslogRelayServerImpl.class.getName());
	private List<SyslogRelayConfig> configs;
	private List<DatagramSocket> sockets;

	@Requires
	private SnmpTrapService trapService;

	@Requires
	private ConfigService conf;

	@Requires
	private UserApi domUserApi;

	@Requires
	private PerformanceMonitor perf;

	@Requires
	private ClusterService cluster;

	private Charset utf8;

	@Validate
	public void start() {
		utf8 = Charset.forName("utf-8");
		trapService.addReceiver(this);
		reload();
	}

	@Invalidate
	public void stop() {
		if (trapService != null)
			trapService.removeReceiver(this);

		// close all sockets
		if (sockets != null) {
			for (DatagramSocket s : sockets)
				s.close();

			sockets.clear();
		}
	}

	@Override
	public List<SyslogRelayConfig> getConfigs() {
		return configs;
	}

	@Override
	public void addConfig(SyslogRelayConfig c) {
		ConfigDatabase db = conf.ensureDatabase("frodo-syslog-relay");

		// check duplicates
		if (find(db, c.getIp(), c.getPort()) != null)
			throw new IllegalStateException("duplicated config");

		db.add(c, "script", "added new relay config");

		reload();
	}

	@Override
	public void removeConfig(SyslogRelayConfig config) {
		ConfigDatabase db = conf.ensureDatabase("frodo-syslog-relay");
		Config c = find(db, config.getIp(), config.getPort());
		if (c == null)
			throw new IllegalStateException("config not found");

		db.remove(c, false, "script", "added new relay config");

		reload();
	}

	private Config find(ConfigDatabase db, String ip, int port) {
		Predicate p = Predicates.and(Predicates.field("ip", ip), Predicates.field("port", port));
		return db.findOne(SyslogRelayConfig.class, p);
	}

	private void reload() {
		configs = new ArrayList<SyslogRelayConfig>();

		ConfigDatabase db = conf.ensureDatabase("frodo-syslog-relay");
		ConfigIterator it = db.findAll(SyslogRelayConfig.class);
		while (it.hasNext()) {
			Config c = it.next();
			SyslogRelayConfig s = c.getDocument(SyslogRelayConfig.class);
			configs.add(s);
		}

		it.close();

		// connect new sockets
		List<DatagramSocket> newSockets = new ArrayList<DatagramSocket>();
		for (SyslogRelayConfig c : configs) {
			try {
				DatagramSocket s = new DatagramSocket();
				s.connect(InetAddress.getByName(c.getIp()), c.getPort());
				newSockets.add(s);
			} catch (Throwable t) {
				if (logger.isDebugEnabled())
					logger.error("frodo syslog relay: cannot connect to " + c, t);
			}
		}

		List<DatagramSocket> oldSockets = this.sockets;
		this.sockets = newSockets;

		// close all current sockets
		if (oldSockets != null) {
			for (DatagramSocket s : oldSockets)
				s.close();
		}
	}

	@Override
	public void handle(SnmpTrap trap) {
		Map<String, Object> bindings = trap.getVariableBindings();
		if (!bindings.containsKey(OID.MSGID))
			return;

		int type = (Integer) bindings.get(OID.MSGID);

		String syslog = null;
		if (type == 1000) { // Authlog
			if (logger.isDebugEnabled())
				logger.debug("frodo-syslog-relay: snmp type [{}]", type);

			AuthLog log = parseAuthLog(bindings);
			if (log == null)
				return;

			syslog = formatSyslog(log);

			// send
			sendSyslog(syslog);
		} else if (type == 3000) { // flowlog
			if (logger.isDebugEnabled())
				logger.debug("frodo-syslog-relay: snmp type [{}]", type);

			FlowLog log = parseFlowLog(bindings);

			if (log == null)
				return;

			syslog = formatSyslog(log);
			// send
			sendSyslog(syslog);

		} else if (type == 5000) { // systemlog
			if (logger.isDebugEnabled())
				logger.debug("frodo-syslog-relay: snmp type [{}]", type);
			SystemLog log = parseSystemLog(bindings);
			syslog = formatSyslog(log);
			// send
			sendSyslog(syslog);
		} else {
			return;
		}

	}

	private FlowLog parseFlowLog(Map<String, Object> bindings) {
		Date now = new Date();
		FlowLog log = new FlowLog();
		String loginName = (String) bindings.get(OID.FLOW_LOGIN);

		if (loginName == null) {
			if (logger.isDebugEnabled())
				logger.debug("frodo-syslog-relay: login name is null");
			return null;
		}

		User user = domUserApi.findUser("localhost", loginName);
		if (user == null) {
			if (logger.isDebugEnabled())
				logger.debug("frodo-syslog-relay: user not found");
			return null;
		}

		String destination = (String) bindings.get(OID.FLOW_SERVER_IP);

		// 이벤트 시간
		log.setDate(now);

		// id
		log.setLogin(loginName);
		// 이름
		log.setUserName(user.getName());

		// 목적지
		log.setServerIp(destination);

		return log;
	}

	private SystemLog parseSystemLog(Map<String, Object> bindings) {

		SystemLog log = new SystemLog();

		try {
			Date now = new Date();

			// 이벤트 시간 (접속, 종료)
			log.setDate(now);
			// 1.cpu상태
			int cpuUsage = CpuStat.getCpuUsage().getUsage();
			log.setCpuIdle(CpuStat.getCpuUsage().getIdle());
			log.setCpuSystem(CpuStat.getCpuUsage().getSystem());
			log.setCpuUser(CpuStat.getCpuUsage().getUser());
			log.setCpuUsage(cpuUsage);

			// 2.메모리상태
			MemoryStat mem = MemoryStat.getMemoryStat();
			int memUsage = (int) ((mem.getMemTotal() - mem.getMemFree() - mem.getCached()) * 100 / mem.getMemTotal());
			log.setMemoryCached(mem.getCached());
			log.setMemoryFree(mem.getMemFree());
			log.setMemoryTotal(mem.getMemTotal());
			log.setMemoryUsage(memUsage);

			// 3.인터페이스정보
			List<InterfaceInfo> ifaceInfos = new ArrayList<InterfaceInfo>();
			for (InterfaceInfo i : InterfaceInfo.getInterfaceInfos()) {
				if (i.getIpAddr() == null || i.getIpAddr().isEmpty())
					continue;

				ifaceInfos.add(i);
			}
			log.setIface(ifaceInfos);

			// 4.하드상태
			File f = new File("/utm/log");
			long totalUsed = f.getTotalSpace() - f.getFreeSpace();
			log.setDiskFree(f.getFreeSpace());
			log.setDiskTotal(f.getTotalSpace());
			log.setDiskUsage((int) totalUsed);

		} catch (InterruptedException e) {
			if (logger.isDebugEnabled())
				logger.debug("frodo-syslog-relay: cpu status interrupted.");
		} catch (IOException e) {
			if (logger.isDebugEnabled())
				logger.debug("frodo-syslog-relay: status File error.");
		}

		return log;

	}

	private AuthLog parseAuthLog(Map<String, Object> bindings) {
		Date now = new Date();
		AuthLog log = new AuthLog();
		Integer code = (Integer) bindings.get(OID.AUTH_CODE);
		AuthCode authCode = AuthCode.parse(code);
		String loginType = (String) bindings.get(OID.AUTH_TYPE);
		String loginName = (String) bindings.get(OID.AUTH_LOGIN);
		User user = domUserApi.findUser("localhost", loginName);

		// 이벤트 시간 (접속, 종료)
		log.setDate(now);

		// 로그인 타입 (로그인, 로그아웃)
		log.setType(loginType);
		if (authCode != null)
			log.setCode(authCode.getStatus());

		// 사용자 원격지 ip
		log.setRemoteIp((String) bindings.get(OID.AUTH_REMOTE_IP));

		// 사용자 원격지 port
		log.setRemotePort((Integer) bindings.get(OID.AUTH_REMOTE_PORT));

		// 가상으로 할당한 ip
		log.setNatIp((String) bindings.get(OID.AUTH_NAT_IP));

		// 사용자 ID
		log.setLogin(loginName);

		// 사용자 이름
		log.setUserName(user != null ? user.getName() : "");

		// 접속 유지 시간
		if (loginType.equals("logout")) {
			@SuppressWarnings("unchecked")
			Map<String, Object> frodo = (Map<String, Object>) user.getExt().get("frodo");
			Date lastLoginAt = frodo != null ? (Date) frodo.get("last_login_at") : null;
			Date currentLogoutTime = new Date();

			if(lastLoginAt != null) {
                long diff = currentLogoutTime.getTime() - lastLoginAt.getTime();
                long diffMin = diff / 60 * 1000;
                log.setConnectionDurationTime(Integer.valueOf((int) diffMin) != null ? (int) diffMin : 0);
            }
		}

		// 정상/비정상 종료 유무
		log.setCloseCondition(bindings.get(OID.AUTH_DISCONNECT_FORCED) != null ? (Integer) bindings
				.get(OID.AUTH_DISCONNECT_FORCED) : -1);

		return log;
	}

	private void sendSyslog(String syslog) {
		if (logger.isDebugEnabled())
			logger.debug("frodo syslog relay: try to send [{}]", syslog);

		byte[] b = syslog.getBytes(utf8);
		for (DatagramSocket s : sockets) {
			try {
				s.send(new DatagramPacket(b, b.length));
				if (logger.isDebugEnabled())
					logger.debug("frodo syslog relay: send syslog to [{}] - [{}]", s.getRemoteSocketAddress(), syslog);
			} catch (IOException e) {
				if (logger.isDebugEnabled())
					logger.error("frodo syslog relay: cannot relay log", e);
			}
		}
	}

	private String formatSyslog(FlowLog log) {
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ssZ");
		StringBuilder sb = new StringBuilder(1000);

		// 이벤트 시간
		sb.append(dateFormat.format(new Date()));
		sb.append(",");

		// 아이디
		sb.append(log.getLogin());
		sb.append(",");

		// 이름
		sb.append(log.getUserName());
		sb.append(",");

		// 목적지 ip
		sb.append(log.getServerIp());

		return sb.toString();
	}

	private String formatSyslog(SystemLog log) {
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ssZ");
		StringBuilder sb = new StringBuilder(1000);

		// 이벤트 시간 (접속, 종료)
		sb.append(dateFormat.format(new Date()));
		sb.append(",");

		// 1.cpu상태
		sb.append(log.getCpuUsage());
		sb.append(",");

		// 2.메모리상태
		sb.append(log.getMemoryUsage());
		sb.append(",");

		// 3.하드상태
		sb.append(log.getDiskUsage());
		sb.append(",");

		// 4.인터페이스정보
		List<InterfaceInfo> ifaces = log.getIface();
		for (InterfaceInfo i : ifaces) {
			sb.append(i.getName());
			sb.append(",");
			for (String addr : i.getIpAddr()) {
				sb.append(addr);
				sb.append(",");
			}
			sb.append(i.getDuplex());
			sb.append(",");
			sb.append(i.getSpeed());
			sb.append(",");
			sb.append(i.getError() != null ? i.getError().toString() : 0);
			sb.append(",");
		}

		// 마지막 쉼표 제거
		int length = sb.length();
		sb.delete(length - 1, length);

		return sb.toString();
	}

	private String formatSyslog(AuthLog log) {
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ssZ");
		StringBuilder sb = new StringBuilder(1000);

		// 이벤트 시간 (접속, 종료)
		sb.append(dateFormat.format(new Date()));
		sb.append(",");

		// 로그인 타입 (로그인, 로그아웃)
		sb.append(log.getType());
		sb.append(",");

		// 사용자 원격지 ip
		sb.append(log.getRemoteIp());
		sb.append(",");

		// 사용자 원격지 port
		sb.append(log.getRemotePort());
		sb.append(",");

		// 가상으로 할당한 ip
		sb.append(wrap(log.getNatIp()));
		sb.append(",");

		// 사용자 ID
		sb.append(log.getLogin());
		sb.append(",");

		// 사용자 이름
		sb.append(log.getUserName());
		sb.append(",");

		// 접속 유지 시간 (분)
		sb.append(log.getConnectionDurationTime() == null ? "" : log.getConnectionDurationTime());
		sb.append(",");

		// 정상/비정상 종료 유무
		sb.append(log.getCloseCondition());

		return sb.toString();
	}

	private String wrap(String t) {
		if (t == null)
			return "";
		return t;
	}

	private List<Map<String, Object>> getClusterStatus() {
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ssZ");
		List<Map<String, Object>> l = new ArrayList<Map<String, Object>>();
		for (ClusterNode node : cluster.getClusterNodes()) {
			Map<String, Object> m = new HashMap<String, Object>();
			m.put("node_id", node.getId());
			m.put("public_ip", node.getPublicIp());
			m.put("timestamp", node.getTimestamp() != null ? dateFormat.format(node.getTimestamp()) : null);
			m.put("available", node.isAvailable());
			l.add(m);
		}
		return l;
	}
}
