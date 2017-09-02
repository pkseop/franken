package kr.co.future.sslvpn.xtmconf.msgbus;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import kr.co.future.sslvpn.model.SystemStatusBackup;
import kr.co.future.sslvpn.model.UserExtension;
import kr.co.future.sslvpn.model.api.SystemStatusBackupApi;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Invalidate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.apache.felix.ipojo.annotations.ServiceProperty;
import org.apache.felix.ipojo.annotations.Validate;

import kr.co.future.api.PrimitiveConverter;
import kr.co.future.cron.CronService;
import kr.co.future.cron.Schedule;
import kr.co.future.dom.api.AdminApi;
import kr.co.future.dom.api.ConfigManager;
import kr.co.future.dom.api.DefaultEntityEventListener;
import kr.co.future.dom.api.OrganizationApi;
import kr.co.future.dom.model.Admin;
import kr.co.future.dom.model.Organization;
import kr.co.future.dom.model.User;
import kr.co.future.msgbus.Marshaler;
import kr.co.future.msgbus.MsgbusException;
import kr.co.future.msgbus.Request;
import kr.co.future.msgbus.Response;
import kr.co.future.msgbus.handler.MsgbusMethod;
import kr.co.future.msgbus.handler.MsgbusPlugin;
import kr.co.future.ntp.NtpClient;
import kr.co.future.ntp.NtpSyncService;
import kr.co.future.sslvpn.xtmconf.BridgeTunCachingService;
import kr.co.future.sslvpn.xtmconf.Utils;
import kr.co.future.sslvpn.xtmconf.XtmConfig;
import kr.co.future.sslvpn.xtmconf.ha.SyncSessionBackup;
import kr.co.future.sslvpn.xtmconf.manage.SyslogSetting;
import kr.co.future.sslvpn.xtmconf.manage.SyslogSetting.Format;
import kr.co.future.sslvpn.xtmconf.network.Interface;
import kr.co.future.sslvpn.xtmconf.object.UserList;
import kr.co.future.sslvpn.xtmconf.object.UserList.AuthType;
import kr.co.future.sslvpn.xtmconf.system.Basic;
import kr.co.future.sslvpn.xtmconf.system.CommandUtil;
import kr.co.future.sslvpn.xtmconf.system.InterfaceInfo;
import kr.co.future.sslvpn.xtmconf.system.License;
import kr.co.future.sslvpn.xtmconf.system.Periodic;
import kr.co.future.sslvpn.xtmconf.system.Reservation;
import kr.co.future.sslvpn.xtmconf.system.ServiceControl;
import kr.co.future.sslvpn.xtmconf.system.SettingOption;
import kr.co.future.sslvpn.xtmconf.system.Snmp;
import kr.co.future.sslvpn.xtmconf.system.Status;
import kr.co.future.sslvpn.xtmconf.system.UpgradeMus;
import kr.co.future.sslvpn.xtmconf.system.UserAccess;
import kr.co.future.sslvpn.xtmconf.system.UserAdmin;
import kr.co.future.sslvpn.xtmconf.system.Basic.SyncType;
import kr.co.future.sslvpn.xtmconf.system.Basic.TimeZone;
import kr.co.future.sslvpn.xtmconf.system.Status.Type;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(name = "frodo-xtmconf-system-plugin")
@MsgbusPlugin
@Provides
public class SystemPlugin extends DefaultEntityEventListener<Organization> implements Runnable {
	private static final String DOM_ADMIN_TRUST_HOSTS = "dom.admin.trust_hosts";
	private static final String SSHD_CONFIG = "/etc/ssh/sshd_config";
	private Logger logger = LoggerFactory.getLogger(SystemPlugin.class);
	private static final String MOUNT_HDA_DISK = "/dev/hda2";
	private static final String MOUNT_HDC_DISK = "/dev/hdc2";	//old device setting.

	@ServiceProperty(name = "instance.name")
	private String instanceName;

	@Requires
	private CronService cron;

	@Requires
	private NtpSyncService ntp;

	@Requires
	private OrganizationApi orgApi;

	@Requires
	private AdminApi adminApi;

	@Requires
	private kr.co.future.dom.api.UserApi domUserApi;

	@Requires
	private kr.co.future.sslvpn.model.api.UserApi userApi;

	@Requires
	private ConfigManager cfg;

	@Requires
	BridgeTunCachingService bridgeCache;
	
	@Requires
	private SystemStatusBackupApi systemStatusBackupApi;

	private UserEntityEventListener userEntityEventListener = new UserEntityEventListener();
	private OrgEntityEventListener orgEntityEventListener = new OrgEntityEventListener();
	private ConcurrentMap<String, UserAdmin> adminCache;
	private String ramdiskVersion;
	private String firmwareVersion;

	@Validate
	public void start() {
		orgApi.addEntityEventListener(orgEntityEventListener);
		domUserApi.addEntityEventListener(userEntityEventListener);
		adminCache = new ConcurrentHashMap<String, UserAdmin>();

		try {
			for (UserAdmin ua : XtmConfig.readConfig(UserAdmin.class))
				adminCache.put(ua.getId(), ua);
		} catch (Throwable t) {
			logger.error("frodo xtmconf: cannot read admin xml", t);
		}

		ntp.stop();
		for (Basic basic : XtmConfig.readConfig(Basic.class)) {
			if (basic.isUseSync()) {
				ntp.start();
				break;
			}
		}

		// load initial trust host ip address
		if (orgApi.findOrganization("localhost") != null) {
			Organization org = orgApi.getOrganization("localhost");
			if (org != null) {
				String hosts = (String) org.getParameters().get(DOM_ADMIN_TRUST_HOSTS);
				logger.info("frodo xtmconf: load dom.admin.trust_hosts [{}]", hosts);
				if (hosts != null) {
					logger.trace("frodo xtmconf: ignoring trust_host_ip update");
					// applyTrustHosts(hosts);
				}
			}
		}

		ReadAdminVersion readAdminVersion = new ReadAdminVersion();
		readAdminVersion.start();
	}
	
	private class ReadAdminVersion extends Thread {
		public void run() {
			try {
				logger.info("start execution of 'admin_version' command to read admin version");
				CommandUtil.run(new File("/sbin"), "admin_version");
				//If system in busy status then file wouldn't be created immediately. So try 10 times more. 
				for(int i = 0; i < 10; i++) {
					Thread.sleep(500);
					List<String> versions = readFileLines("/utm/log/tmp/version");
					if(versions != null) {
						parseVersion(versions);
						logger.info("reading admin version completed.");
						break;
					}
				}
			} catch (Exception e) {
				logger.error("frodo xtmconf: cannot obtain version", e);
			}
		}
		
		private void parseVersion(List<String> versions) {
			try{
				String fversion = "";
				String rversion = "";
				String[] stringFversion = versions.get(1).split(":")[1].trim().split("\\.");
				String[] stringRversion = versions.get(2).split(":")[1].trim().split("\\.");
				for (int index = 0; index < 4; index++) {
					fversion += stringFversion[index] + ".";
					rversion += stringRversion[index] + ".";
				}
		
				ramdiskVersion = rversion;
				firmwareVersion = fversion;
			} catch (Exception e) {
				logger.error("frodo xtmconf: cannot obtain version", e);
			}
		}
	}

	@Invalidate
	public void stop() {
		if (orgApi != null)
			orgApi.removeEntityEventListener(orgEntityEventListener);

		if (domUserApi != null)
			domUserApi.removeEntityEventListener(userEntityEventListener);

		if (ntp != null)
			ntp.stop();
	}

	private class OrgEntityEventListener extends DefaultEntityEventListener<Organization> {
		@Override
		public void entityUpdated(String domain, Organization obj, Object state) {
			String trustHosts = (String) obj.getParameters().get(DOM_ADMIN_TRUST_HOSTS);
			if (trustHosts != null) {
				logger.trace("frodo xtmconf: ignoring trust_host_ip update");
				// applyTrustHosts(trustHosts);
			}
		}
	}

	/**
	 * @param hosts
	 *            allowed ip addresses separated by comma
	 */
	@SuppressWarnings("unused")
	private void applyTrustHosts(String hosts) {
		logger.trace("frodo xtmconf: updating trust_host_ip to [{}]", hosts);

		if (hosts == null || hosts.isEmpty()) {
			writeTrustHostIp(" ");
		} else {
			hosts = hosts.replaceAll(",", " ") + "\n";
			writeTrustHostIp(hosts);
		}
	}

	private void writeTrustHostIp(String hosts) {
		FileOutputStream os = null;
		try {
			File f = new File("/proc/utm/conf/trust_host_ip");
			if (!f.exists())
				return;
			os = new FileOutputStream(f);
			os.write(hosts.getBytes());
		} catch (IOException e) {
			logger.error("frodo xtmconf: cannot set trust host ip to ssh daemon", e);
		} finally {
			if (os != null)
				try {
					os.close();
				} catch (IOException e) {
				}
		}
	}

	private class UserEntityEventListener extends DefaultEntityEventListener<User> {
		@Override
		public void entityUpdated(String domain, User user, Object state) {
			// dom user admin attributes
			Admin admin = PrimitiveConverter.parse(Admin.class, user.getExt().get("admin"), cfg.getParseCallback(domain));

			// from xml cache
			UserAdmin found = adminCache.get(user.getLoginName());

			if (admin != null) {
				// add new admin if not found
				UserAdmin ua = found;
				if (found == null) {
					ua = new UserAdmin();
					ua.setCid(UUID.randomUUID().toString());
					adminCache.put(user.getLoginName(), ua);
				}

				setUserAdmin(ua, user, admin.getRole().getLevel() == 4);

				List<UserAdmin> newAdmins = new ArrayList<UserAdmin>(adminCache.values());
				Utils.setConfigNum(newAdmins);
				XtmConfig.writeConfig(UserAdmin.class, newAdmins);
			} else if (found != null) {
				// remove if exists in admin xml
				adminCache.remove(user.getLoginName());

				List<UserAdmin> newAdmins = new ArrayList<UserAdmin>(adminCache.values());
				Utils.setConfigNum(newAdmins);
				XtmConfig.writeConfig(UserAdmin.class, newAdmins);
			}
		}

		@Override
		public void entityRemoved(String domain, User user, Object state) {
			if (adminCache.containsKey(user.getLoginName())) {
				adminCache.remove(user.getLoginName());
				List<UserAdmin> newAdmins = new ArrayList<UserAdmin>(adminCache.values());
				Utils.setConfigNum(newAdmins);
				XtmConfig.writeConfig(UserAdmin.class, newAdmins);
			}
		}
	}

	private void setUserAdmin(UserAdmin ua, User user, boolean settingConfig) {
		// just for email alarm (other values are unreal)
		ua.setId(user.getLoginName());
		ua.setSalt(user.getSalt());
		ua.setPassword(user.getPassword());
		ua.setMail(user.getEmail());
		ua.setPhone(user.getPhone());
		ua.setHost("");
		ua.setSettingConfig(settingConfig);
		ua.setSettingMonitor(true);
		ua.setSettingOtp(false);

		// master
		if (settingConfig) {
			List<UserAccess> config = XtmConfig.readConfig(UserAccess.class);
			if (config.isEmpty())
				config.add(new UserAccess());
			Admin admin = adminApi.getAdmin("localhost", user);
			File loginAccess = new File("/etc/webadmin/conf/login_access.conf");
			if (admin.isUseLoginLock()) {
				config.get(0).setLimit(admin.getLoginLockCount());

				FileOutputStream fos = null;
				try {
					fos = new FileOutputStream(loginAccess);
					fos.write(Integer.toString(admin.getLoginLockCount()).getBytes());
				} catch (IOException e) {
				} finally {
					try {
						if (fos != null)
							fos.close();
					} catch (IOException e1) {
					}
				}
			} else {
				config.get(0).setLimit(null);
				loginAccess.delete();
			}
			XtmConfig.writeConfig(UserAccess.class, config);
		}
	}

	@MsgbusMethod
	public void getGateInfo(Request req, Response resp) {
		resp.put("firmware_version", firmwareVersion);
		resp.put("ramdisk_version", ramdiskVersion);
		resp.put("model", readFileLine("/proc/utm/model"));
		resp.put("gate_serial", readFileLine("/proc/utm/serial"));

		String dpi = readFileLine("/proc/utm/ips/version");
		if (dpi != null) {
			if (dpi.equals("00.00.00.00"))
				dpi = null;
		}
		resp.put("dpi_pattern", dpi);

		List<SyncSessionBackup> ssb = XtmConfig.readConfig(SyncSessionBackup.class);
		String sessionBackup = null;
		if (ssb != null && ssb.size() > 0 && ssb.get(0).isUse() && ssb.get(0).getSelectMode() != null) {
			sessionBackup = String.format("%s (%s)", ssb.get(0).getMode().name().replace("Active", "Active ").trim(), ssb.get(0)
					.getSelectMode().name());
		}
		resp.put("session_backup", sessionBackup);

		List<String> cpu = readFileLines("/proc/cpuinfo");
		String cpuInfo = null;
		Integer coreCount = null;
		for (String l : cpu) {
			if (l.startsWith("processor")) {
				if (coreCount == null)
					coreCount = 0;
				coreCount++;
			}
			if (cpuInfo == null && l.startsWith("model name")) {
				cpuInfo = l.substring(l.indexOf(":") + 1);
				cpuInfo = cpuInfo.replaceAll(" +", " ").trim();
			}
		}
		resp.put("cpu_info", cpuInfo);
		resp.put("core_count", coreCount);

		String mem = readFileLine("/proc/utm/conf/main_memory");
		if (mem != null) {
			mem = mem.substring(0, mem.indexOf(" ")) + " MB";
		}
		resp.put("main_memory", mem);

		List<String> diskInfo = new ArrayList<String>();
		List<String> disk = exec("fdisk -l");
		for (String l : disk) {
			if (!l.startsWith("Disk "))
				continue;
			if (l.contains(","))
				diskInfo.add(l.substring(10, l.indexOf(",")));
		}
		resp.put("disk", diskInfo);

		List<String> gate = readFileLines("/etc/webadmin/conf/center_setup.conf");
		String gateCid = null;
		if (gate != null) {
			for (String l : gate) {
				if (l.startsWith("CENTER_SERIAL")) {
					if (gateCid == null)
						gateCid = l.substring(l.indexOf("=") + 1) + "-";
					else
						gateCid = l.substring(l.indexOf("=") + 1) + "-" + gateCid;
				} else if (l.startsWith("GATE_UUID")) {
					if (gateCid == null)
						gateCid = l.substring(l.indexOf("=") + 1);
					else
						gateCid = gateCid + l.substring(l.indexOf("=") + 1);
				}
			}
		}
		resp.put("gate_cid", gateCid);

		List<String> df = exec("df");
		String[] dfHeader = null;
		List<Object> filesystem = new ArrayList<Object>();
		Map<String, String> v = new HashMap<String, String>();
		int index = 0;
		for (String l : df) {
			String[] tokens = l.trim().replaceAll(" +", " ").split(" ");
			if (dfHeader == null)
				dfHeader = tokens;
			else {
				for (int i = 0; i < tokens.length; i++)
					v.put(dfHeader[index++], tokens[i]);
				if (index < dfHeader.length - 1)
					continue;
				filesystem.add(v);
				v = new HashMap<String, String>();
				index = 0;
			}
		}
		resp.put("filesystem", filesystem);

		List<InterfaceInfo> ifaceInfos = InterfaceInfo.getInterfaceInfos();
		resp.put("iface_info", Marshaler.marshal(ifaceInfos));
	}

	private String readFileLine(String path) {
		List<String> lines = readFileLines(path);
		if (lines == null || lines.size() == 0)
			return null;
		return lines.get(0);
	}

	private List<String> readFileLines(String path) {
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
			logger.error("frodo xtmconf: failed read file {}, {}", path, e.getMessage());
		} finally {
			try {
				if (br != null)
					br.close();
			} catch (IOException e) {
			}
		}

		return str;
	}

	private List<String> exec(String cmd) {
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
			logger.error("frodo xtmconf: failed execute {}, {}", cmd, e.getMessage());
		} finally {
			try {
				if (br != null)
					br.close();
			} catch (IOException e) {
			}
		}
		return str;
	}

	@MsgbusMethod
	public void startService(Request req, Response resp) throws IOException {
		ServiceControl.startService(req.getSession());
		File file = new File("/usr/local/etc/stop_service");
		if (file.exists())
			file.delete();
	}

	@MsgbusMethod
	public void stopService(Request req, Response resp) throws IOException {
		ServiceControl.stopService(req.getSession());
	}

	@MsgbusMethod
	public void isServiceRunning(Request req, Response resp) {
		resp.put("result", ServiceControl.isServiceRunning());
	}

	@MsgbusMethod
	public void deployPolicy(Request req, Response resp) throws IOException, InterruptedException {
//정책 전송이 오래걸려 제외함.
//		syncUserList();
		addLocalhostSyslog();
		addDefaultNics();
		syncTime();
		cronUnregister();
		Reservation reservation = new Reservation();
		XtmConfig.writeConfig(Reservation.class, Arrays.asList(reservation));

		logger.info("frodo xtmconf: deploy policy");
		// send policy
		Runtime r = Runtime.getRuntime();
		Process p = r.exec("/usr/bin/php /var/www/webadmin/send_policy_object.php");
		BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
		String message = null;
		while (true) {
			String msg = br.readLine();
			if (msg == null)
				break;
			message = msg;
		}
		br.close();

		if (message != null) {
			Pattern pattern = Pattern.compile("(?<=').+?(?=')");
			Matcher matcher = pattern.matcher(message);
			String msg = null;
			while (matcher.find()) {
				msg = matcher.group();
			}
			logger.info("frodo xtmconf: deploy policy message [{}]", msg);

			if (msg != null && !msg.contains("Complete to apply policy object"))
				throw new MsgbusException("frodo", msg);
		}

		bridgeCache.reload();

		if (resp != null)
			resp.put("exit_code", p.waitFor());
	}

	private void syncUserList() {
		List<UserList> config = new ArrayList<UserList>();

		int num = 1;
		for (User user : domUserApi.getUsers("localhost")) {
			UserExtension ext = userApi.getUserExtension(user);
			boolean radius = user.getExt().containsKey("radius");
			if (ext == null) {
				ext = new UserExtension();
				ext.setUser(user);
				userApi.setUserExtension(ext);
			}

			UserList l = new UserList();
			l.setNum(num++);
			l.setCid(ext.getCid());
			l.setAuthType(radius ? AuthType.Radius : AuthType.Local);
			l.setId(user.getLoginName());
			l.setName(user.getName());
			l.setPassword(user.getPassword());
			l.setGroupCid("");
			l.setDesc(user.getDescription());
			l.setModeAllow(true);
			l.setModeAllow(ext.isLocked());

			config.add(l);
		}

		XtmConfig.writeConfig(UserList.class, config);
	}

	private void addLocalhostSyslog() {
		List<SyslogSetting> settings = XtmConfig.readConfig(SyslogSetting.class);

		SyslogSetting local = null;
		for (SyslogSetting s : settings) {
			if (s.getType() == SyslogSetting.Type.Standard && s.getIp().equals("127.0.0.1") && s.getPort() == 514)
				local = s;
		}

		if (local == null) {
			local = new SyslogSetting();
			local.setType(SyslogSetting.Type.Standard);
			local.setFormat(Format.Standard);
			local.setIp("127.0.0.1");
			local.setPort(514);
			settings.add(0, local);
			Utils.setConfigNum(settings);
			XtmConfig.writeConfig(SyslogSetting.class, settings);
		}
	}

	private void addDefaultNics() {
		List<Interface> ifaces = XtmConfig.readConfig(Interface.class);
		List<String> names = Utils.getInterfaceNames();

		for (Interface iface : ifaces)
			names.remove(iface.getIfaceName());

		for (String name : names) {
			Interface iface = new Interface();
			iface.setType(Interface.Type.Interface);
			iface.setIfaceName(name);
			iface.setIfaceType(Interface.IfaceType.None);
			iface.setDuplex(Interface.Duplex.Auto);
			iface.setSpeed(Interface.Speed.Auto);
			iface.setMode(Interface.InterfaceType.Internal);
			iface.setFrag(true);
			iface.setModem(Interface.Modem.None);
			ifaces.add(iface);
		}
		XtmConfig.writeConfig(Interface.class, ifaces);
	}

	private void syncTime() {
		List<Basic> config = XtmConfig.readConfig(Basic.class);
		Basic basic = (config.isEmpty()) ? new Basic() : config.get(0);
		if (basic.isUseSync()) {
			try {
				new NtpClient(basic.getSyncServer()).sync();
			} catch (Exception e) {
			}
		}
	}

	@MsgbusMethod
	public void getLicense(Request req, Response resp) throws IOException {
		Map<String, Object> m = License.load().marshal();

		//130K Performance
		//int current = cfg.count("localhost", User.class, null);
		int current = userApi.getDomUserCount();
		int total = getUserLimit();
		m.put("current_user_count", current);
		m.put("total_user_count", total == Integer.MAX_VALUE ? null : total);

		resp.put("license", m);
	}

	private int getUserLimit() {
		BufferedReader br = null;
		try {
			br = new BufferedReader(new FileReader("/proc/utm/conf/sslplus_user_count"));
			String sslplusUserCount = br.readLine().trim();
			String[] strCounts = sslplusUserCount.split(":");
			int count = 0;
			for(String str : strCounts) {
				count += Integer.parseInt(str);
			}
			logger.debug("frodo core: current user limit [{}]", count);
			return count;
		} catch (FileNotFoundException e) {
			logger.error("frodo core: user count file not found");
			// 기존의 구버전 펌웨어에서는 파일이 존재하지 않으므로 무조건 추가가 되게 한다.
			return Integer.MAX_VALUE;
		} catch (IOException e) {
			logger.error("frodo core: user count read error");
			throw new IllegalStateException("user count read error");
		} finally {
			try {
				if (br != null)
					br.close();
			} catch (IOException e) {
			}
		}
	}

	@MsgbusMethod
	public void getOnlineLicense(Request req, Response resp) throws InterruptedException {
		try {
			String authKey = License.receive();
			if (authKey.length() == 120)
				License.write(authKey);
			else
				throw new MsgbusException("frodo", authKey);
		} catch (IOException e) {
			throw new MsgbusException("frodo", e.getMessage());
		}
	}

	@MsgbusMethod
	public void setLicense(Request req, Response resp) {
		try {
			License.write(req.getString("license_key"));
		} catch (Exception e) {
			throw new MsgbusException("frodo", e.getMessage());
		}

	}

	@MsgbusMethod
	public void getBasic(Request req, Response resp) {
		resp.put("config", Marshaler.marshal(XtmConfig.readConfig(Basic.class)));
		resp.put("time", new Date());
	}

	@MsgbusMethod
	public void setBasic(Request req, Response resp) {
		Basic b = new Basic();
		b.setZone(TimeZone.get(req.getInteger("zone")));
		b.setUseSync(Utils.getBooleanFromMap(req.get("sync"), "use"));
		b.setSyncType(SyncType.get(Utils.getStringFromMap(req.get("sync"), "type")));
		String server = Utils.getStringFromMap(req.get("sync"), "server");
		b.setSyncServer(server);
		try {
			ntp.setTimeServer(InetAddress.getByName(server));
		} catch (UnknownHostException e) {
			throw new MsgbusException("frodo", "ntp-invalid-host");
		}

		if (b.isUseSync())
			ntp.start();
		else
			ntp.stop();

		XtmConfig.writeConfig(Basic.class, Arrays.asList(b));
	}

	@MsgbusMethod
	public void syncTime(Request req, Response resp) throws UnknownHostException {
		try {
			String type = req.getString("type");
			if (type.equals("timeserver")) {
				NtpClient ntpClient = new NtpClient(req.getString("server"));
				resp.put("result", ntpClient.sync());
				Runtime.getRuntime().exec("hwclock -w -u");
			} else if (type.equals("manual")) {
				Date date = req.getDate("date");
				String newTime = new SimpleDateFormat("MMddHHmmyyyy.ss").format(date);
				Runtime.getRuntime().exec("date " + newTime);
				Runtime.getRuntime().exec("hwclock -w -u");
			}
		} catch (IOException e) {
			throw new MsgbusException("frodo", "ntp-sync-error");
		}
	}

	@MsgbusMethod
	public void getReservation(Request req, Response resp) {
		resp.put("config", Marshaler.marshal(XtmConfig.readConfig(Reservation.class)));
	}

	@MsgbusMethod
	public void setReservation(Request req, Response resp) {
		Reservation r = new Reservation();
		r.setUse(req.getBoolean("use"));
		r.setDate(req.getDate("date"));
		r.setHour(req.getInteger("hour"));
		r.setMinute(req.getInteger("minute"));

		XtmConfig.writeConfig(Reservation.class, Arrays.asList(r));

		if (r.isUse()) {
			try {
				Calendar c = Calendar.getInstance();
				c.setTime(r.getDate());
				int dayOfMonth = c.get(Calendar.DAY_OF_MONTH);
				int month = c.get(Calendar.MONTH) + 1;

				String exp = String.format("%d %d %d %d *", r.getMinute(), r.getHour(), dayOfMonth, month);
				Schedule schedule = new Schedule.Builder(instanceName).build(exp);
				cronUnregister();
				cron.registerSchedule(schedule);
			} catch (Exception e) {
				logger.error("frodo core: policy deploy reservation failed", e);
			}
		} else {
			cronUnregister();
		}
	}

	private void cronUnregister() {
		Map<Integer, Schedule> schedules = cron.getSchedules();
		for (int id : schedules.keySet()) {
			Schedule schedule = schedules.get(id);
			if (instanceName.equals(schedule.getTaskName()))
				cron.unregisterSchedule(id);
		}
	}

	@Override
	public void run() {
		try {
			List<Reservation> config = XtmConfig.readConfig(Reservation.class);
			Reservation reservation = (config.isEmpty()) ? null : config.get(0);

			if (reservation == null || !reservation.isUse()) {
				cronUnregister();
				return;
			}

			Calendar c = Calendar.getInstance();
			c.setTime(reservation.getDate());
			int y1 = c.get(Calendar.YEAR);

			c.setTime(new Date());
			int y2 = c.get(Calendar.YEAR);

			if (y1 != y2)
				return;

			deployPolicy(null, null);
		} catch (Exception e) {
			logger.error("kraken xtmconf: deploy policy failed", e);
		}
	}

	@MsgbusMethod
	public void getSettingOption(Request req, Response resp) {
		resp.put("config", Marshaler.marshal(XtmConfig.readConfig(SettingOption.class)));
	}

	@MsgbusMethod
	public void setSettingOption(Request req, Response resp) {
		SettingOption so = new SettingOption();
		so.setName(req.getString("name"));
		so.setStateful(req.getBoolean("stateful"));
		so.setTimeout(req.getInteger("timeout"));
		so.setPolicyPort(req.getInteger("policy_port"));
		so.setSshPort(req.getInteger("ssh_port"));
		so.setBp1(Utils.getBooleanFromMap(req.get("bypass"), "bp1"));
		so.setBp2(Utils.getBooleanFromMap(req.get("bypass"), "bp2"));
		so.setBp3(Utils.getBooleanFromMap(req.get("bypass"), "bp3"));
		so.setBp4(Utils.getBooleanFromMap(req.get("bypass"), "bp4"));

		XtmConfig.writeConfig(SettingOption.class, Arrays.asList(so));
	}

	@MsgbusMethod
	public void getSnmp(Request req, Response resp) {
		resp.put("config", Marshaler.marshal(XtmConfig.readConfig(Snmp.class)));
	}

	@SuppressWarnings("unchecked")
	@MsgbusMethod
	public void setSnmp(Request req, Response resp) {
		List<Snmp> snmp = new ArrayList<Snmp>();

		List<Object> objs = (List<Object>) req.get("snmp");
		for (Object obj : objs) {
			Snmp s = new Snmp();
			s.setSettingUse(Utils.getBooleanFromMap(obj, "use"));
			s.setSettingAuthtrap(Utils.getBooleanFromMap(obj, "authtrap"));
			s.setCommunity(Utils.getStringFromMap(obj, "community"));
			s.setLocation(Utils.getStringFromMap(obj, "location"));
			snmp.add(s);
		}

		XtmConfig.writeConfig(Snmp.class, snmp);
	}

	@MsgbusMethod
	public void getStatus(Request req, Response resp) {
		List<Object> list = Marshaler.marshal(XtmConfig.readConfig(Status.class));
		getStatusBackup(list);
		
		resp.put("config", list);
	}
	
	private void getStatusBackup(List<Object> list) {
		SystemStatusBackup systemStatusBackup = systemStatusBackupApi.getSystemStatusBackup();
		Map<String, Object> m = new HashMap<String, Object>();
		m.put("type", "backup");
		m.put("password", systemStatusBackup.getPassword());
		m.put("use_backup", systemStatusBackup.getUseBackup());
		m.put("schedule", systemStatusBackup.getSchedule());
		m.put("use_ftp", systemStatusBackup.getUseFtp());
		
		list.add(m);
	}
	
	@MsgbusMethod
	public void setStatusBackup(Request req, Response resp) {
		try{
			SystemStatusBackup systemStatusBackup = systemStatusBackupApi.getSystemStatusBackup();
			systemStatusBackup.setPassword(req.getString("password"));
			systemStatusBackup.setUseBackup(req.getBoolean("use_backup"));
			systemStatusBackup.setSchedule(req.getString("schedule"));
			systemStatusBackup.setUseFtp(req.getBoolean("use_ftp"));
			
			systemStatusBackupApi.setSystemStatusBackup(systemStatusBackup);
		} catch(Exception e) {
			throw new MsgbusException("frodo", "backup-save-failed");
		}
	}

	@MsgbusMethod
	public void setStatus(Request req, Response resp) {
		Type type = Type.get(req.getString("type"));
		if(type == null)		//only use type 'integrity' and 'reboot' 
			return;
		List<Status> config = XtmConfig.readConfig(Status.class);
		Status obj = null;
		for (Status s : config) {
			if (s.getType() == type)
				obj = s;
		}

		if (obj == null) {
			obj = new Status();
			obj.setType(type);
			config.add(obj);
		}

		setStatus(obj, req);

		XtmConfig.writeConfig(Status.class, config);
	}

	private void setStatus(Status s, Request req) {
		s.setUse(req.getBoolean("use"));

		if (s.getType() == Type.Integrity) {
			s.setPeriodic(Periodic.get(Utils.getStringFromMap(req.get("periodic"), "type")));
			s.setSubtype(Utils.getIntegerFromMap(req.get("periodic"), "subtype"));
		}
		/*
		else if (s.getType() == Type.Backup) {
			s.setPeriodic(Periodic.get(Utils.getStringFromMap(req.get("periodic"), "type")));
			s.setSubtype(Utils.getIntegerFromMap(req.get("periodic"), "subtype"));
			s.setPassword(req.getString("password"));
		}
		*/ 
		else if (s.getType() == Type.Reboot) {
			s.setDate(req.getDate("date"));
			s.setHour(req.getInteger("hour"));
			s.setMinute(req.getInteger("minute"));
		}
	}

	@MsgbusMethod
	public void getUpgradeMus(Request req, Response resp) {
		resp.put("config", Marshaler.marshal(XtmConfig.readConfig(UpgradeMus.class)));
	}

	@MsgbusMethod
	public void setUpgradeMus(Request req, Response resp) {
		UpgradeMus um = new UpgradeMus();
		um.setUse(req.getBoolean("use"));
		um.setIp(req.getString("ip"));
		um.setInterval(Utils.getBooleanFromMap(req.get("interval"), "use"));
		um.setIntervalType(Periodic.get(Utils.getStringFromMap(req.get("interval"), "type")));
		um.setIntervalTerm(Utils.getIntegerFromMap(req.get("interval"), "term"));

		XtmConfig.writeConfig(UpgradeMus.class, Arrays.asList(um));
	}

	@MsgbusMethod
	public void getUserAccess(Request req, Response resp) {
		resp.put("config", Marshaler.marshal(XtmConfig.readConfig(UserAccess.class)));
	}

	@MsgbusMethod
	public void setUserAccess(Request req, Response resp) {
		UserAccess ua = new UserAccess();
		ua.setUser(req.getInteger("user"));
		ua.setLimit(req.getInteger("limit"));

		XtmConfig.writeConfig(UserAccess.class, Arrays.asList(ua));
	}

	@MsgbusMethod
	public void getUserAdmin(Request req, Response resp) {
		resp.put("config", Marshaler.marshal(XtmConfig.readConfig(UserAdmin.class)));
	}

	@MsgbusMethod
	public void addUserAdmin(Request req, Response resp) {
		UserAdmin ua = new UserAdmin();
		ua.setCid(UUID.randomUUID().toString());
		setUserAdmin(ua, req, Utils.salt(0));

		List<UserAdmin> config = XtmConfig.readConfig(UserAdmin.class);
		config.add(ua);
		Utils.setConfigNum(config);
		XtmConfig.writeConfig(UserAdmin.class, config);
	}

	@MsgbusMethod
	public void modifyUserAdmin(Request req, Response resp) {
		String cid = req.getString("cid");

		List<UserAdmin> config = XtmConfig.readConfig(UserAdmin.class);
		for (UserAdmin ua : config) {
			if (cid.equals(ua.getCid()))
				setUserAdmin(ua, req, ua.getSalt());
		}
		XtmConfig.writeConfig(UserAdmin.class, config);
	}

	@SuppressWarnings("unchecked")
	@MsgbusMethod
	public void removeUserAdmin(Request req, Response resp) {
		List<String> cids = (List<String>) req.get("cid");

		List<UserAdmin> config = XtmConfig.readConfig(UserAdmin.class);
		List<UserAdmin> objs = new ArrayList<UserAdmin>();
		for (UserAdmin ua : config) {
			if (cids.contains(ua.getCid()))
				objs.add(ua);
		}
		for (UserAdmin obj : objs)
			config.remove(obj);
		Utils.setConfigNum(config);
		XtmConfig.writeConfig(UserAdmin.class, config);
	}

	private void setUserAdmin(UserAdmin ua, Request req, String salt) {
		ua.setSettingConfig(Utils.getBooleanFromMap(req.get("setting"), "config"));
		ua.setSettingMonitor(Utils.getBooleanFromMap(req.get("setting"), "monitor"));
		ua.setSettingOtp(Utils.getBooleanFromMap(req.get("setting"), "otp"));
		ua.setId(req.getString("id"));
		ua.setPassword(domUserApi.hashPassword(salt, req.getString("password")));
		ua.setMail(req.getString("mail"));
		ua.setPhone(req.getString("phone"));
		ua.setHost(req.getString("host"));
	}

	private void moutUtmConf(String disk) {
		try {
			CommandUtil.run(new File("/"), "mount", "-t", "ext3", disk, "/utm/conf");
		} catch (IOException e) {
			logger.error("frodo xtmconf: mount error", e);
		}
	}
	
	private void umountUtmConf() {
		try {
			CommandUtil.run(new File("/"), "umount", "/utm/conf");
		} catch (IOException e) {
			logger.error("frodo xtmconf: umount error", e);
		}
	}
	
	@MsgbusMethod
	public void getSshdPort(Request req, Response resp) {
		moutUtmConf(MOUNT_HDA_DISK);

		Integer sshdPort = getPort();
		if(sshdPort == null) {
			moutUtmConf(MOUNT_HDC_DISK);
			sshdPort = getPort();
		}

		umountUtmConf();
		resp.put("sshd_port", sshdPort);

	}

	private Integer getPort() {
		BufferedReader br = null;
		try {
			br = new BufferedReader(new FileReader(SSHD_CONFIG));
			String line = "";
			while ((line = br.readLine()) != null) {
				if (line.contains("#Port ") || line.contains("Port ")) {
					String[] portLine = line.split(" ");
					Integer sshdPort = Integer.parseInt(portLine[1]);
					return sshdPort;
				}
			}
		} catch (FileNotFoundException e) {
			logger.error("frodo xtmconf: sshd config file not found", e);
			return null;
		} catch (IOException e) {
			logger.error("frodo xtmconf: sshd config read error", e);
			return null;
		} finally {
			try {
				if (br != null)
					br.close();
			} catch (IOException e) {
			}
		}
		return null;
	}

	@MsgbusMethod
	public void setSshdPort(Request req, Response resp) {
		Integer sshdPort = req.getInteger("sshd_port");
		if (sshdPort == null)
			throw new MsgbusException("frodo", "sshd-port-not-found");

		moutUtmConf(MOUNT_HDA_DISK);

		if(replacePort(sshdPort) == false) {
			moutUtmConf(MOUNT_HDC_DISK);
			replacePort(sshdPort);
		}

		umountUtmConf();
	}

	private boolean replacePort(Integer sshdPort) {
		String text = "";
		BufferedReader br = null;
		try {
			br = new BufferedReader(new FileReader(SSHD_CONFIG));
			String line = "";
			while ((line = br.readLine()) != null) {
				if (line.contains("#Port ") || line.contains("Port "))
					line = "Port " + sshdPort;
				text += line + "\n";
			}
		} catch (FileNotFoundException e) {
			logger.error("frodo xtmconf: sshd config file not found", e);
			return false;
		} catch (IOException e) {
			logger.error("frodo xtmconf: sshd config read error", e);
			return false;
		} finally {
			try {
				if (br != null)
					br.close();
			} catch (IOException e) {
				logger.error("frodo xtmconf: sshd config read error", e);
				return false;
			}
		}

		BufferedWriter bw = null;
		try {
			bw = new BufferedWriter(new FileWriter(SSHD_CONFIG));
			bw.write(text);
		} catch (IOException e) {
			logger.error("frodo xtmconf: sshds config write error", e);
			return false;
		} finally {
			try {
				if (bw != null)
					bw.close();
			} catch (IOException e) {
				logger.error("frodo xtmconf: sshds config write error", e);
				return false;
			}
		}
		return true;
	}
}
