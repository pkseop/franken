package kr.co.future.sslvpn.xtmconf.msgbus;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import kr.co.future.sslvpn.model.api.LogSettingApi;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Requires;

import kr.co.future.msgbus.Marshaler;
import kr.co.future.msgbus.Request;
import kr.co.future.msgbus.Response;
import kr.co.future.msgbus.handler.MsgbusMethod;
import kr.co.future.msgbus.handler.MsgbusPlugin;
import kr.co.future.sslvpn.xtmconf.Utils;
import kr.co.future.sslvpn.xtmconf.XtmConfig;
import kr.co.future.sslvpn.xtmconf.manage.AlarmSetting;
import kr.co.future.sslvpn.xtmconf.manage.LogRttSetting;
import kr.co.future.sslvpn.xtmconf.manage.LogSetting;
import kr.co.future.sslvpn.xtmconf.manage.Monitor;
import kr.co.future.sslvpn.xtmconf.manage.SyslogSetting;
import kr.co.future.sslvpn.xtmconf.manage.AlarmSetting.Type;
import kr.co.future.sslvpn.xtmconf.manage.LogSetting.CompressType;
import kr.co.future.sslvpn.xtmconf.manage.LogSetting.FreezeType;
import kr.co.future.sslvpn.xtmconf.manage.LogSetting.FtpType;
import kr.co.future.sslvpn.xtmconf.manage.LogSetting.LogLevel;
import kr.co.future.sslvpn.xtmconf.manage.LogSetting.SaveType;
import kr.co.future.sslvpn.xtmconf.manage.LogSetting.SearchType;
import kr.co.future.sslvpn.xtmconf.manage.SyslogSetting.Format;

@Component(name = "frodo-xtmconf-manage-plugin")
@MsgbusPlugin
public class ManagePlugin {
	
	@Requires
	private LogSettingApi logSettingApi;
	
	@MsgbusMethod
	public void getAlarmSetting(Request req, Response resp) {
		resp.put("config", Marshaler.marshal(XtmConfig.readConfig(AlarmSetting.class)));
	}

	@MsgbusMethod
	public void setAlarmSetting(Request req, Response resp) {
		List<AlarmSetting> config = new ArrayList<AlarmSetting>();
		for (Type type : Type.values()) {
			AlarmSetting as = new AlarmSetting();
			as.setType(type);
			setAlarmSetting(as, req.get(type.toString()));
			config.add(as);
		}

		XtmConfig.writeConfig(AlarmSetting.class, config);
	}

	private void setAlarmSetting(AlarmSetting as, Object map) {
		if (as.getType() == Type.System) {
			as.setCpu(Utils.getBooleanFromMap(map, "cpu"));
			as.setMem(Utils.getBooleanFromMap(map, "mem"));
			as.setSession(Utils.getBooleanFromMap(map, "session"));
			as.setLog(Utils.getBooleanFromMap(map, "log"));
			as.setObject(Utils.getBooleanFromMap(map, "object"));
			as.setSpd(Utils.getBooleanFromMap(map, "spd"));
			as.setBoot(Utils.getBooleanFromMap(map, "boot"));
			as.setStop(Utils.getBooleanFromMap(map, "stop"));
			as.setLogin(Utils.getBooleanFromMap(map, "login"));
			as.setIke(Utils.getBooleanFromMap(map, "ike"));
			as.setMb(Utils.getBooleanFromMap(map, "mb"));
			as.setBm(Utils.getBooleanFromMap(map, "bm"));
			as.setIntegrity(Utils.getBooleanFromMap(map, "integrity"));
		} else if (as.getType() == Type.DPI) {
			as.setDetect(Utils.getBooleanFromMap(map, "detect"));
			as.setBlock(Utils.getBooleanFromMap(map, "block"));
		} else if (as.getType() == Type.DOS)
			as.setDos(Utils.getBooleanFromMap(map, "dos"));
		else if (as.getType() == Type.DDOS)
			as.setDdos(Utils.getBooleanFromMap(map, "ddos"));
	}

	@MsgbusMethod
	public void getLogRttSetting(Request req, Response resp) {
		resp.put("config", Marshaler.marshal(XtmConfig.readConfig(LogRttSetting.class)));
	}

	@SuppressWarnings("unchecked")
	@MsgbusMethod
	public void setLogRttSetting(Request req, Response resp) {
		List<LogRttSetting> config = new ArrayList<LogRttSetting>();

		List<Object> c = (List<Object>) resp.get("config");
		for (Object map : c) {
			LogRttSetting lrs = new LogRttSetting();
			lrs.setName(Utils.getStringFromMap(map, "name"));
			lrs.setIp(Utils.getStringFromMap(map, "ip"));
			config.add(lrs);
		}

		XtmConfig.writeConfig(LogRttSetting.class, config);
	}

	@MsgbusMethod
	public void getLogSetting(Request req, Response resp) {
		List<Object> list = Marshaler.marshal(XtmConfig.readConfig(LogSetting.class));
		getLogSetting(list);
		
		resp.put("config", list);
	}
	
	private void getLogSetting(List<Object> list) {
		list.add(getFtpLogSetting());
		list.add(getBackupLogSetting());
	}
	
	private Map<String,Object> getFtpLogSetting() {
		kr.co.future.sslvpn.model.LogSetting logSetting = logSettingApi.getLogSetting();
		
		Map<String, Object> m = new HashMap<String, Object>();
		m.put("type", "ftp");
		m.put("use", logSetting.getFtpUse());
		m.put("ftp_type", logSetting.getFtpType());
		m.put("ip", logSetting.getFtpIp());
		m.put("id", logSetting.getFtpId());
		m.put("password", logSetting.getFtpPw());
		m.put("path", logSetting.getFtpPath());
		
		return m;
	}
	
	private Map<String,Object> getBackupLogSetting() {
		kr.co.future.sslvpn.model.LogSetting logSetting = logSettingApi.getLogSetting();
		
		Map<String, Object> m = new HashMap<String, Object>();
		m.put("type", "backup");
		m.put("use", logSetting.getBackupUse());
		m.put("maintain_days", logSetting.getBackupMaintainDays());
		m.put("schedule", logSetting.getBackupSchedule());
		
		return m;
	}
	
	@MsgbusMethod
	public void setLogSetting(Request req, Response resp) {
		List<LogSetting> config = new ArrayList<LogSetting>();
		for (LogSetting.Type type : LogSetting.Type.values()) {
			LogSetting as = new LogSetting();
			as.setType(type);
			setLogSetting(as, req.get(type.toString()));
			config.add(as);
		}

		XtmConfig.writeConfig(LogSetting.class, config);
		
		setLogSetting(req);
	}

	private void setLogSetting(LogSetting ls, Object map) {
		if (ls.getType() == LogSetting.Type.Setting) {
			ls.setSystem(LogLevel.valueOf(Utils.getStringFromMap(Utils.getFromMap(map, "level"), "system")));
			ls.setNetwork(LogLevel.valueOf(Utils.getStringFromMap(Utils.getFromMap(map, "level"), "network")));
			ls.setIpsec(LogLevel.valueOf(Utils.getStringFromMap(Utils.getFromMap(map, "level"), "ipsec")));
			ls.setNac(LogLevel.valueOf(Utils.getStringFromMap(Utils.getFromMap(map, "level"), "nac")));
			ls.setWaf(LogLevel.valueOf(Utils.getStringFromMap(Utils.getFromMap(map, "level"), "waf")));
			ls.setAvas(LogLevel.valueOf(Utils.getStringFromMap(Utils.getFromMap(map, "level"), "avas")));
			ls.setDpi(LogLevel.valueOf(Utils.getStringFromMap(Utils.getFromMap(map, "level"), "dpi")));
			ls.setAnomaly(LogLevel.valueOf(Utils.getStringFromMap(Utils.getFromMap(map, "level"), "anomaly")));
			ls.setFreeze(FreezeType.get(Utils.getStringFromMap(Utils.getFromMap(map, "freeze"), "type")));
			ls.setFreezeValue(Utils.getIntegerFromMap(Utils.getFromMap(map, "freeze"), "value"));
			ls.setOptimization(Utils.getBooleanFromMap(map, "optimization"));
			ls.setHddSave(Utils.getBooleanFromMap(map, "hdd_save"));
			ls.setTcp(Utils.getBooleanFromMap(map, "tcp"));
			ls.setSession(Utils.getBooleanFromMap(map, "session"));
			ls.setSearch(SearchType.get(Utils.getStringFromMap(Utils.getFromMap(map, "search"), "type")));
			ls.setSearchValue(Utils.getStringFromMap(Utils.getFromMap(map, "search"), "value"));
			ls.setRtm(Utils.getBooleanFromMap(Utils.getFromMap(map, "rtm"), "use"));
			ls.setRtmRpm(Utils.getBooleanFromMap(Utils.getFromMap(map, "rtm"), "rpm"));
			ls.setRtmId(Utils.getStringFromMap(Utils.getFromMap(map, "rtm"), "id"));
			ls.setRtmPassword(Utils.getStringFromMap(Utils.getFromMap(map, "rtm"), "password"));
			ls.setRtmServer(Utils.getStringFromMap(Utils.getFromMap(map, "rtm"), "server"));
			ls.setSave(Utils.getBooleanFromMap(Utils.getFromMap(map, "save"), "use"));
			ls.setSaveType(SaveType.get(Utils.getStringFromMap(Utils.getFromMap(map, "save"), "type")));
			ls.setSaveValue(Utils.getIntegerFromMap(Utils.getFromMap(map, "save"), "value"));
			ls.setCompress(Utils.getBooleanFromMap(Utils.getFromMap(map, "compress"), "use"));
			ls.setCompressType(CompressType.get(Utils.getStringFromMap(Utils.getFromMap(map, "compress"), "type")));
			ls.setCompressSubtype(Utils.getIntegerFromMap(Utils.getFromMap(map, "compress"), "subtype"));
			ls.setCompressFtp(Utils.getBooleanFromMap(Utils.getFromMap(map, "compress"), "ftp"));
			ls.setCompressDel(Utils.getBooleanFromMap(Utils.getFromMap(map, "compress"), "del"));
			ls.setCompressBackup(Utils.getIntegerFromMap(Utils.getFromMap(map, "compress"), "backup"));
			ls.setSendPolicy(Utils.getBooleanFromMap(map, "send_policy"));
		} else if (ls.getType() == LogSetting.Type.Report) {
			ls.setReport(Utils.getBooleanFromMap(map, "use"));
			ls.setHour(Utils.getIntegerFromMap(map, "hour"));
			ls.setTrafficSip(Utils.getBooleanFromMap(Utils.getFromMap(map, "traffic"), "sip"));
			ls.setTrafficDip(Utils.getBooleanFromMap(Utils.getFromMap(map, "traffic"), "dip"));
			ls.setTrafficService(Utils.getBooleanFromMap(Utils.getFromMap(map, "traffic"), "service"));
			ls.setTrafficProtocol(Utils.getBooleanFromMap(Utils.getFromMap(map, "traffic"), "protocol"));
			ls.setDpiSip(Utils.getBooleanFromMap(Utils.getFromMap(map, "dpi"), "sip"));
			ls.setDpiDip(Utils.getBooleanFromMap(Utils.getFromMap(map, "dpi"), "dip"));
			ls.setDpiType(Utils.getBooleanFromMap(Utils.getFromMap(map, "dpi"), "type"));
			ls.setDpiFlow(Utils.getBooleanFromMap(Utils.getFromMap(map, "dpi"), "flow"));
			ls.setUrlUser(Utils.getBooleanFromMap(Utils.getFromMap(map, "url"), "user"));
			ls.setUrlDomain(Utils.getBooleanFromMap(Utils.getFromMap(map, "url"), "domain"));
			ls.setAmountInterface(Utils.getBooleanFromMap(Utils.getFromMap(map, "amount"), "interface"));
			ls.setAmountSystem(Utils.getBooleanFromMap(Utils.getFromMap(map, "amount"), "system"));
			ls.setAmountPolicy(Utils.getBooleanFromMap(Utils.getFromMap(map, "amount"), "policy"));
			ls.setAmountLog(Utils.getBooleanFromMap(Utils.getFromMap(map, "amount"), "log"));
		}
//		else if (ls.getType() == LogSetting.Type.FTP) {
//			ls.setFtp(Utils.getBooleanFromMap(map, "use"));
//			ls.setFtpType(FtpType.get(Utils.getStringFromMap(map, "ftp_type")));
//			ls.setFtpIp(Utils.getStringFromMap(map, "ip"));
//			ls.setFtpId(Utils.getStringFromMap(map, "id"));
//			ls.setFtpPassword(Utils.getStringFromMap(map, "password"));
//			ls.setFtpPath(Utils.getStringFromMap(map, "path"));
//		}
	}
	
	private void setLogSetting(Request req) {
		Object map = req.get("ftp");	
		kr.co.future.sslvpn.model.LogSetting logSetting = logSettingApi.getLogSetting();
		logSetting.setFtpUse(Utils.getBooleanFromMap(map, "use"));
		logSetting.setFtpType(Utils.getBooleanFromMap(map, "ftp_type"));
		logSetting.setFtpIp(Utils.getStringFromMap(map, "ip"));
		logSetting.setFtpId(Utils.getStringFromMap(map, "id"));
		logSetting.setFtpPw(Utils.getStringFromMap(map, "password"));
		logSetting.setFtpPath(Utils.getStringFromMap(map, "path"));
		
		map = req.get("backup");
		logSetting.setBackupUse(Utils.getBooleanFromMap(map,"use"));
		logSetting.setBackupMaintainDays(Utils.getIntegerFromMap(map, "maintain_days"));
		logSetting.setBackupSchedule(Utils.getStringFromMap(map,"schedule"));
		
		logSettingApi.setLogSetting(logSetting);
	}

	@MsgbusMethod
	public void getMonitor(Request req, Response resp) {
		resp.put("config", Marshaler.marshal(XtmConfig.readConfig(Monitor.class)));
	}

	@MsgbusMethod
	public void setMonitor(Request req, Response resp) {
		Monitor m = new Monitor();
		m.setSession(req.getBoolean("session"));
		m.setHost(req.getBoolean("host"));
		m.setService(req.getBoolean("service"));
		m.setFlow(req.getBoolean("flow"));
		m.setEth(req.getString("eth"));

		XtmConfig.writeConfig(Monitor.class, Arrays.asList(m));
	}

	@MsgbusMethod
	public void getSyslogSetting(Request req, Response resp) {
		List<SyslogSetting> settings = XtmConfig.readConfig(SyslogSetting.class);

		// hide hard-coded local address for ui
		SyslogSetting local = null;
		for (SyslogSetting s : settings) {
			if (s.getType() == SyslogSetting.Type.Standard && s.getIp().equals("127.0.0.1") && s.getPort() == 514)
				local = s;
		}

		if (local != null)
			settings.remove(local);

		resp.put("config", Marshaler.marshal(settings));
	}

	@MsgbusMethod
	public void setSyslogSettingXtm(Request req, Response resp) {
		List<SyslogSetting> config = XtmConfig.readConfig(SyslogSetting.class);
		SyslogSetting xtm = null;
		for (SyslogSetting ss : config) {
			if (ss.getType() == SyslogSetting.Type.XTM)
				xtm = ss;
		}

		if (xtm == null) {
			xtm = new SyslogSetting();
			xtm.setType(SyslogSetting.Type.XTM);
			config.add(xtm);
		}

		setSyslogSetting(xtm, req);

		XtmConfig.writeConfig(SyslogSetting.class, config);
	}

	@MsgbusMethod
	public void addSyslogSettingStandard(Request req, Response resp) {
		SyslogSetting ss = new SyslogSetting();
		ss.setType(SyslogSetting.Type.Standard);
		setSyslogSetting(ss, req);

		List<SyslogSetting> config = XtmConfig.readConfig(SyslogSetting.class);
		config.add(ss);
		Utils.setConfigNum(config);
		XtmConfig.writeConfig(SyslogSetting.class, config);
	}

	@MsgbusMethod
	public void modifySyslogSettingStandard(Request req, Response resp) {
		int num = req.getInteger("num");

		List<SyslogSetting> config = XtmConfig.readConfig(SyslogSetting.class);
		for (SyslogSetting ss : config) {
			if (ss.getNum() == num)
				setSyslogSetting(ss, req);
		}
		XtmConfig.writeConfig(SyslogSetting.class, config);
	}

	@SuppressWarnings("unchecked")
	@MsgbusMethod
	public void removeSyslogSettingStandard(Request req, Response resp) {
		List<Integer> nums = (List<Integer>) req.get("num");

		List<SyslogSetting> config = XtmConfig.readConfig(SyslogSetting.class);
		List<SyslogSetting> objs = new ArrayList<SyslogSetting>();
		for (SyslogSetting ss : config) {
			if (nums.contains(ss.getNum()))
				objs.add(ss);
		}
		for (SyslogSetting obj : objs)
			config.remove(obj);
		Utils.setConfigNum(config);
		XtmConfig.writeConfig(SyslogSetting.class, config);
	}

	private void setSyslogSetting(SyslogSetting ss, Request req) {
		if (ss.getType() == SyslogSetting.Type.XTM)
			ss.setIpsec(req.getBoolean("ipsec"));
		else if (ss.getType() == SyslogSetting.Type.Standard)
			ss.setFormat(Format.valueOf(req.getString("format")));

		ss.setIp(req.getString("ip"));
		ss.setPort(req.getInteger("port"));
	}
}
