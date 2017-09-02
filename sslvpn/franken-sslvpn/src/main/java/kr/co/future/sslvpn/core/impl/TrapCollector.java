package kr.co.future.sslvpn.core.impl;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import kr.co.future.sslvpn.core.AlertCategory;
import kr.co.future.sslvpn.core.AuthCode;
import kr.co.future.sslvpn.core.DashboardService;
import kr.co.future.sslvpn.core.log.AccessLog;
import kr.co.future.sslvpn.core.log.AuthLog;
import kr.co.future.sslvpn.core.log.FlowLog;
import kr.co.future.sslvpn.core.log.NicLog;
import kr.co.future.sslvpn.core.log.OID;
import kr.co.future.sslvpn.model.AccessProfile;
import kr.co.future.sslvpn.model.api.AccessProfileApi;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Invalidate;
import org.apache.felix.ipojo.annotations.Requires;
import org.apache.felix.ipojo.annotations.Validate;

import kr.co.future.msgbus.PushApi;
import kr.co.future.logstorage.Log;
import kr.co.future.logstorage.LogStorage;
import kr.co.future.logstorage.LogStorageStatus;
import kr.co.future.logstorage.LogTableRegistry;
import kr.co.future.snmp.SnmpTrap;
import kr.co.future.snmp.SnmpTrapBinding;
import kr.co.future.snmp.SnmpTrapReceiver;
import kr.co.future.snmp.SnmpTrapService;
import kr.co.future.sslvpn.core.PerformanceMonitor;
import kr.co.future.sslvpn.core.impl.LoginBruteforceDetector;
import kr.co.future.sslvpn.core.impl.TrapCollector;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(name = "frodo-trap-collector")
public class TrapCollector implements SnmpTrapReceiver {
	private final Logger logger = LoggerFactory.getLogger(TrapCollector.class.getName());

	@Requires
	private SnmpTrapService trapService;

	@Requires
	private LogTableRegistry tableRegistry;

	@Requires
	private LogStorage storage;

	@Requires
	private PushApi pushApi;

	@Requires
	private DashboardService dash;

	@Requires
	private PerformanceMonitor perf;
	
	@Requires
	private AccessProfileApi profileApi;

	private LoginBruteforceDetector bruteforceDetector;

	@Validate
	public void start() throws IOException {
		bruteforceDetector = new LoginBruteforceDetector(10, 5, 5000);

		if (trapService.getBinding(new InetSocketAddress(162)) == null) {
			SnmpTrapBinding binding = new SnmpTrapBinding();
			binding.setName("frodo-trap");
			binding.setBindAddress(new InetSocketAddress(162));
			trapService.open(binding);
		}

		if (!tableRegistry.exists("ssl-auth")) {
			storage.createTable("ssl-auth");
			logger.info("frodo core: created ssl-auth logtable");
		}

		if (!tableRegistry.exists("ssl-access")) {
			storage.createTable("ssl-access");
			logger.info("frodo core: created ssl-access logtable");
		}

		if (!tableRegistry.exists("ssl-flow")) {
			storage.createTable("ssl-flow");
			logger.info("frodo core: created ssl-flow logtable");
		}

		trapService.addReceiver(this);
	}

	@Invalidate
	public void stop() {
		if (trapService != null) {
			trapService.removeReceiver(this);
			try {
				trapService.close("frodo-trap");
			} catch (IOException e) {
				logger.error("frodo core: cannot close trap server", e);
			}
		}
	}

	@Override
	public void handle(SnmpTrap trap) {
		Map<String, Object> bindings = trap.getVariableBindings();
		if (!bindings.containsKey(OID.MSGID))
			return;

		int type = (Integer) bindings.get(OID.MSGID);
		if (type == 1000)
			handleAuthTrap(bindings);
		else if (type == 2000)
			handleAccessTrap(bindings);
		else if (type == 3000)
			handleFlowTrap(bindings);
		else if (type == 4000)
			handleNicTrap(bindings);
	}

	private void handleAuthTrap(Map<String, Object> bindings) {
		try{
			// parse
			Date now = new Date();
			AuthLog log = new AuthLog();
			Integer code = (Integer) bindings.get(OID.AUTH_CODE);
			if (code == null) {
				logger.error("frodo core: auth code is null, bindings [{}]", bindings);
				return;
			}
			
			// 상위 3비트는 로그인 실패 횟수를 기록하는데 사용하므로 마스크를 함.
			code = code & 0xFFF;
			AuthCode authCode = AuthCode.parse(code);
			
			if (authCode == null) {
				logger.error("frodo core: auth code not found, bindings [{}]", bindings);
				return;
			}
	
			log.setDate(now);
			log.setType((String) bindings.get(OID.AUTH_TYPE));
			if (authCode != null)
				log.setCode(authCode.getStatus());
	
			String loginName = (String) bindings.get(OID.AUTH_LOGIN); 
			log.setLogin(loginName);
			AccessProfile profile = profileApi.determineProfile(loginName);
			log.setProfile(profile != null ? profile.getName() : ""/*(String) bindings.get(OID.AUTH_PROFILE)*/);
			log.setRemoteIp((String) bindings.get(OID.AUTH_REMOTE_IP));
			log.setRemotePort((Integer) bindings.get(OID.AUTH_REMOTE_PORT));
			log.setTunnel((Integer) bindings.get(OID.AUTH_TUNNEL));
			log.setNatIp((String) bindings.get(OID.AUTH_NAT_IP));
			
			log.setOsType((Integer) bindings.get(OID.AUTH_OS_TYPE));
			log.setDeviceKey((String) bindings.get(OID.AUTH_DEVICE_KEY));
			
			logger.debug("frodo core: handle auth log [{}]", log.toString());
	
			// check bruteforce attack
			try {
				if (log.getCode() != null) {
					boolean success = log.getCode().equals("success");
					InetAddress ip = InetAddress.getByName(log.getRemoteIp());
					boolean alert = bruteforceDetector.check(now, ip, log.getLogin(), success);
					if (alert)
						dash.setAlert(AlertCategory.LoginBruteforce, now, alert);
				}
			} catch (UnknownHostException e) {
			}
	
			// check policy violation
			if (log.getCode().startsWith("policy-"))
				dash.setAlert(AlertCategory.AuthPolicyViolation, log.getDate(), true);
	
			// push and archive
			Log l = log.toLog();
			push("frodo-auth-log", l);

			if (storage.getStatus() == LogStorageStatus.Open)
				storage.write(l);
		}catch(Exception e) {
			logger.error("error occurred during trap auth log", e);
		}
	}

	private void handleAccessTrap(Map<String, Object> bindings) {
		try{
			AccessLog log = new AccessLog();
	//		Integer sessionId = (Integer) bindings.get(OID.ACCESS_SESSION);
	
			log.setDate(new Date());
			log.setLogin((String) bindings.get(OID.ACCESS_LOGIN));
			log.setTunnel((Integer) bindings.get(OID.ACCESS_TUNNEL));
	//		log.setSession(sessionId & 0xffffffffl);
			log.setAction((String) bindings.get(OID.ACCESS_ACTION));
			log.setClientIp((String) bindings.get(OID.ACCESS_CLIENT_IP));
			log.setClientPort((Integer) bindings.get(OID.ACCESS_CLIENT_PORT));
			log.setServerIp((String) bindings.get(OID.ACCESS_SERVER_IP));
			log.setServerPort((Integer) bindings.get(OID.ACCESS_SERVER_PORT));
			log.setProtocol((String) bindings.get(OID.ACCESS_PROTOCOL));
	
			if (log.getAction().equals("deny"))
				dash.setAlert(AlertCategory.AccessViolation, log.getDate(), true);
	
			Log l = log.toLog();
			push("frodo-access-log", l);
	
			if (storage.getStatus() == LogStorageStatus.Open)
				storage.write(l);
		}catch(Exception e) {
			logger.error("error occurred during trap access log", e);
		}
	}

	private void handleFlowTrap(Map<String, Object> bindings) {
		try{
			FlowLog log = new FlowLog();
	//		Integer sessionId = (Integer) bindings.get(OID.FLOW_SESSION);
			String eosStr = (String) bindings.get(OID.FLOW_EOS);
			boolean eos = false;
			if (eosStr != null)
				eos = eosStr.equals("FIN") || eosStr.equals("RST");
	
			log.setDate(new Date());
			log.setLogin((String) bindings.get(OID.FLOW_LOGIN));
			log.setTunnel((Integer) bindings.get(OID.FLOW_TUNNEL));
	//		log.setSession(sessionId & 0xffffffffl);
			log.setClientIp((String) bindings.get(OID.FLOW_CLIENT_IP));
			log.setClientPort((Integer) bindings.get(OID.FLOW_CLIENT_PORT));
			log.setServerIp((String) bindings.get(OID.FLOW_SERVER_IP));
			log.setServerPort((Integer) bindings.get(OID.FLOW_SERVER_PORT));
			log.setProtocol((String) bindings.get(OID.FLOW_PROTOCOL));
			log.setTxBytes((Long) bindings.get(OID.FLOW_TX_BYTES));
			log.setTxPackets((Long) bindings.get(OID.FLOW_TX_PACKETS));
			log.setRxBytes((Long) bindings.get(OID.FLOW_RX_BYTES));
			log.setRxPackets((Long) bindings.get(OID.FLOW_RX_PACKETS));
			log.setEos(eos);
	
			Log l = log.toLog();
	
			if (storage.getStatus() == LogStorageStatus.Open)
				storage.write(l);
		}catch(Exception e) {
			logger.error("error occurred during trap flow log", e);
		}
	}

	private void handleNicTrap(Map<String, Object> bindings) {
		try{
			NicLog log = new NicLog();
	
			String nicDate = (String) bindings.get(OID.NIC_DATE);
			Date date = null;
	      try {
		      date = new SimpleDateFormat("yyyyMMddHHmmss").parse(nicDate);
	      } catch (ParseException e) {
		      logger.error("date format parse error.", e);
		      return;
	      }
			log.setDate(date);
			log.setIface((String) bindings.get(OID.NIC_IFACE));
			log.setTxBytes((Long) bindings.get(OID.NIC_TX_BYTES));
			log.setRxBytes((Long) bindings.get(OID.NIC_RX_BYTES));
			log.setTxPackets((Long) bindings.get(OID.NIC_TX_PACKETS));
			log.setRxPackets((Long) bindings.get(OID.NIC_RX_PACKETS));
			
			logger.trace("date [{}], rx [{}], tx [{}],", new Object[]{date, log.getRxBytes(), log.getTxBytes()});
			
			if (log.getIface().equals("sslvpn0") || log.getIface().equals("tap0") || log.getIface().equals("tun0"))
				perf.addNicStat(log);
		}catch(Exception e) {
			logger.error("error occurred during trap nic log", e);
		}
	}

	private void push(String callback, Log log) {
		Map<String, Object> m = new HashMap<String, Object>(log.getData());
		m.put("_date", log.getDate());
		m.put("_id", log.getId());
		pushApi.push("localhost", callback, m);
	}
}
