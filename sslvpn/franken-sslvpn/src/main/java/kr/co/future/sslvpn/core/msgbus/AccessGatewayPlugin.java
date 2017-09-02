package kr.co.future.sslvpn.core.msgbus;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import kr.co.future.sslvpn.core.AuthService;
import kr.co.future.sslvpn.core.DownloadService;
import kr.co.future.sslvpn.core.PasswordHash;
import kr.co.future.sslvpn.core.SqlAuthService;
import kr.co.future.sslvpn.model.AccessGateway;
import kr.co.future.sslvpn.model.ExternalVpn;
import kr.co.future.sslvpn.model.IOSVpnServerConfig;
import kr.co.future.sslvpn.model.IOSVpnServerConfigParams;
import kr.co.future.sslvpn.model.SSLConfig;
import kr.co.future.sslvpn.model.SplitRoutingEntry;
import kr.co.future.sslvpn.model.VpnServerConfigParams;
import kr.co.future.sslvpn.model.VpnServerConfig;
import kr.co.future.sslvpn.model.api.AccessGatewayApi;
import kr.co.future.sslvpn.model.api.VpnServerConfigApi;
import kr.co.future.sslvpn.xtmconf.XtmConfig;
import kr.co.future.sslvpn.xtmconf.ha.SyncPolicySynchronize;
import kr.co.future.sslvpn.xtmconf.ha.SyncPolicySynchronize.IntervalType;
import kr.co.future.sslvpn.xtmconf.manage.LogSetting;
import kr.co.future.sslvpn.xtmconf.network.Radius;
import kr.co.future.sslvpn.xtmconf.network.Radius.Fileserver;
import kr.co.future.sslvpn.xtmconf.system.Backup;
import kr.co.future.sslvpn.xtmconf.system.Restore;
import kr.co.future.sslvpn.xtmconf.system.ServiceControl;
import kr.co.future.sslvpn.xtmconf.system.SettingOption;
import kr.co.future.sslvpn.xtmconf.system.Status;
import kr.co.future.sslvpn.xtmconf.system.Upgrade;

import org.apache.commons.codec.binary.Base64;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.apache.felix.ipojo.annotations.ServiceProperty;

import kr.co.future.confdb.ConfigService;
import kr.co.future.cron.CronService;
import kr.co.future.cron.Schedule;
import kr.co.future.dom.api.FileUploadApi;
import kr.co.future.dom.model.UploadedFile;
import kr.co.future.ldap.LdapProfile;
import kr.co.future.ldap.LdapServerType;
import kr.co.future.ldap.LdapService;
import kr.co.future.logstorage.Log;
import kr.co.future.logstorage.LogStorage;
import kr.co.future.logstorage.LogStorageMonitor;
import kr.co.future.msgbus.MsgbusException;
import kr.co.future.msgbus.Request;
import kr.co.future.msgbus.Response;
import kr.co.future.msgbus.Session;
import kr.co.future.msgbus.handler.AllowGuestAccess;
import kr.co.future.msgbus.handler.CallbackType;
import kr.co.future.msgbus.handler.MsgbusMethod;
import kr.co.future.msgbus.handler.MsgbusPlugin;
import kr.co.future.radius.client.RadiusClient;
import kr.co.future.radius.client.auth.Authenticator;
import kr.co.future.radius.client.auth.ChapAuthenticator;
import kr.co.future.radius.client.auth.PapAuthenticator;
import kr.co.future.radius.protocol.AccessAccept;
import kr.co.future.radius.protocol.RadiusPacket;
import kr.co.future.sslvpn.core.xenics.XenicsConfig;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@MsgbusPlugin
@Component(name = "frodo-gw-plugin")
@Provides
public class AccessGatewayPlugin implements Runnable {
	private static final String UPGRADE_FILE_KEY = "frodo_upgrade_file";
	private static final String LOCALHOST = "localhost";
	private static final String TOKEN_KEY = "download_token";

	private final Logger logger = LoggerFactory.getLogger(AccessGatewayPlugin.class.getName());

	@Requires
	private AccessGatewayApi gatewayApi;

	@Requires
	private LdapService ldap;

	@Requires
	private FileUploadApi upload;

	@Requires
	private DownloadService download;

	@Requires
	private AuthService auth;

	@ServiceProperty(name = "instance.name")
	private String instanceName;

	@Requires
	private CronService cron;

	@Requires
	private LogStorage logStorage;

	@Requires
	private LogStorageMonitor logStorageMonitor;

	@Requires
	private ConfigService conf;
	
	@Requires
	private SqlAuthService sqlAuthService;
	
	@Requires 
	private VpnServerConfigApi vpnServerConfigApi;

	@Requires 
	private XenicsConfig xenicsConfig;
	
	@AllowGuestAccess
	@MsgbusMethod
	public void useTMover(Request req, Response resp) {
		resp.put("use", false);
	}

	@MsgbusMethod
	public void halt(Request req, Response resp) throws IOException {
		logStorage.flush();
		try {
			Thread.sleep(3000);
		} catch (InterruptedException e) {
		}
		ServiceControl.halt();
	}

	@MsgbusMethod
	public void reboot(Request req, Response resp) throws IOException {
		boolean scheduled = req.getBoolean("scheduled");
		reboot(scheduled);
	}

	private void reboot(boolean scheduled) throws IOException {
		if (scheduled) {
			try {
				List<Status> config = XtmConfig.readConfig(Status.class);
				Status reboot = null;
				for (Status status : config) {
					if (status.getType().equals(Status.Type.Reboot))
						reboot = status;
				}
				if (reboot == null || !reboot.isUse())
					throw new IOException("frodo core: invalid xml (system_status.xml)");

				Calendar c = Calendar.getInstance();
				c.setTime(reboot.getDate());
				int dayOfMonth = c.get(Calendar.DAY_OF_MONTH);
				int month = c.get(Calendar.MONTH) + 1;

				String exp = String.format("%d %d %d %d *", reboot.getMinute(), reboot.getHour(), dayOfMonth, month);
				Schedule schedule = new Schedule.Builder(instanceName).build(exp);
				cronUnregister();
				cron.registerSchedule(schedule);
			} catch (Exception e) {
				logger.error("frodo core: reboot reservation failed", e);
			}
		} else {
			shutdown();
		}
	}

	@Override
	public void run() {
		List<Status> config = XtmConfig.readConfig(Status.class);
		Status reboot = null;
		for (Status status : config) {
			if (status.getType().equals(Status.Type.Reboot))
				reboot = status;
		}

		if (reboot == null || !reboot.isUse()) {
			cronUnregister();
			return;
		}

		Calendar c = Calendar.getInstance();
		c.setTime(reboot.getDate());
		int y1 = c.get(Calendar.YEAR);

		c.setTime(new Date());
		int y2 = c.get(Calendar.YEAR);

		if (y1 != y2)
			return;

		shutdown();
	}

	private void cronUnregister() {
		Map<Integer, Schedule> schedules = cron.getSchedules();
		for (int id : schedules.keySet()) {
			Schedule schedule = schedules.get(id);
			if (instanceName.equals(schedule.getTaskName()))
				cron.unregisterSchedule(id);
		}
	}

	private void shutdown() {
		cronUnregister();

		try {
			List<LogSetting> settings = XtmConfig.readConfig(LogSetting.class);
			boolean writeLog = false;
			for (LogSetting setting : settings) {
				if (setting.getType() == LogSetting.Type.Setting) {
					if (setting.getSystem().ordinal() <= 6 && setting.getSystem().ordinal() != 0)
						writeLog = true;
				}
			}

			if (writeLog) {
				Map<String, Object> data = new HashMap<String, Object>();
				for (String key : new String[] { "nat_sip", "sip", "nat_dip", "dip" })
					data.put(key, "0.0.0.0");
				for (String key : new String[] { "dport", "nat_dport", "sport", "nat_sport", "rule" })
					data.put(key, 0);
				for (String key : new String[] { "dpi_group", "usage", "user" })
					data.put(key, null);
				for (String key : new String[] { "oip", "iface" })
					data.put(key, "");
				data.put("note", "SYSTEM reboot");
				data.put("type", "xtm");
				data.put("proto", "ip");
				SettingOption option = XtmConfig.readConfig(SettingOption.class).get(0);
				data.put("prod", option.getName());
				data.put("category", "system");
				data.put("logtype", 0x11060009);
				data.put("level", 6);
				logStorage.write(new Log("xtm", new Date(), data));
			}
			logStorage.flush();

			try {
				Thread.sleep(3000);
			} catch (InterruptedException e) {
			}

			Runtime.getRuntime().exec("reboot");
		} catch (IOException e) {
			logger.error("frodo core: reboot failed", e);
		}
	}	

	@MsgbusMethod
	public void deployPolicy(Request req, Response resp) {
		// check and update sync policy
		updatePolicySyncSchedule(gatewayApi);

		// send policy to marlin
		auth.deployPolicy();
		logger.info("frodo core: deployed access profiles");
	}

	public static void updatePolicySyncSchedule(AccessGatewayApi gatewayApi) {
		List<SyncPolicySynchronize> l = XtmConfig.readConfig(SyncPolicySynchronize.class);
		if (l.size() == 1) {
			SyncPolicySynchronize s = l.get(0);
			if (s.isUse() && s.isInterval()) {
				if (s.getIntervalType() == IntervalType.Day) {
					gatewayApi.updatePolicySyncSchedule("0 0 * * " + s.getIntervalTerm());
				} else if (s.getIntervalType() == IntervalType.Hour)
					gatewayApi.updatePolicySyncSchedule("0 " + s.getIntervalTerm() + " * * *");
			} else {
				gatewayApi.updatePolicySyncSchedule(null);
			}
		} else {
			gatewayApi.updatePolicySyncSchedule(null);
		}
	}

	@MsgbusMethod
	public void backupConfig(Request req, Response resp) throws IOException, InterruptedException {
		File f = Backup.backup(req.getSession(), req.getString("password"));
		resp.put("file_path", f.getName());

		logger.trace("frodo core: backup completed");
	}

	@MsgbusMethod
	public void issueDownloadToken(Request req, Response resp) {
		Session session = req.getSession();
		if (session.getString(TOKEN_KEY) == null)
			session.setProperty(TOKEN_KEY, UUID.randomUUID().toString());

		String token = session.getString(TOKEN_KEY);
		download.addDownloadToken(token);
		resp.put(TOKEN_KEY, token);

		logger.trace("frodo core: issue download token [{}] from session [{}]", token, session.getRemoteAddress());
	}

	@MsgbusMethod(type = CallbackType.SessionClosed)
	public void cleanDownloadToken(Session session) {
		String token = session.getString(TOKEN_KEY);
		if (token != null)
			download.removeDownloadToken(token);

		FileOutputStream os = (FileOutputStream) session.get(UPGRADE_FILE_KEY);
		if (os != null) {
			try {
				os.close();
				session.unsetProperty(UPGRADE_FILE_KEY);
				removeTempUpgradeFile(session);
				logger.trace("kraken core: close and delete temporary upgrade file");
			} catch (IOException e) {
			}
		}

		logger.trace("frodo core: clean download token [{}] from session [{}]", token, session.getRemoteAddress());
	}

	private void removeTempUpgradeFile(Session session) {
		@SuppressWarnings("unchecked")
		Map<String, Object> m = (Map<String, Object>) session.get("upload");
		if (m == null)
			return;

		UploadMetadata meta = (UploadMetadata) m.get("data");
		if (meta == null)
			return;

		if (meta.temp.exists())
			meta.temp.delete();
	}

	@MsgbusMethod
	public void restoreConfig(Request req, Response resp) {
		String password = req.getString("password");
		String resourceId = req.getString("resource_id");

		UploadedFile metadata = null;
		try {
			metadata = upload.getFileMetadata(LOCALHOST, resourceId);
		} catch (IllegalStateException e) {
			throw new MsgbusException("frodo", "restore-file-not-found");
		}

		try {
			Restore.restore(conf, req.getSession(), password, metadata.getFile());
			List<LogSetting> settings = XtmConfig.readConfig(LogSetting.class);
			boolean writeLog = false;
			for (LogSetting setting : settings) {
				if (setting.getType() == LogSetting.Type.Setting) {
					if (setting.getSystem().ordinal() <= 3 && setting.getSystem().ordinal() != 0)
						writeLog = true;
				}
			}
			if (writeLog) {
				Map<String, Object> data = new HashMap<String, Object>();
				for (String key : new String[] { "nat_sip", "sip", "nat_dip", "dip" })
					data.put(key, "0.0.0.0");
				for (String key : new String[] { "dport", "nat_dport", "sport", "nat_sport", "rule" })
					data.put(key, 0);
				for (String key : new String[] { "dpi_group", "usage", "user" })
					data.put(key, null);
				for (String key : new String[] { "oip", "iface" })
					data.put(key, "");
				data.put("note", "Object Restore");
				data.put("type", "xtm");
				data.put("proto", "ip");
				SettingOption option = XtmConfig.readConfig(SettingOption.class).get(0);
				data.put("prod", option.getName());
				data.put("category", "system");
				data.put("logtype", 0x12030014);
				data.put("level", 3);
				logStorage.write(new Log("xtm", new Date(), data));
			}
		} catch (IOException e) {
			throw new MsgbusException("frodo", e.getMessage());
		}

		logger.info("frodo core: config restored");

		// reboot
		try {
			reboot(false);
		} catch (IOException e) {
			throw new MsgbusException("frodo", "reboot-failed");
		}
	}

	@SuppressWarnings("unchecked")
	@MsgbusMethod
	public void prepareUpload(Request req, Response resp) throws IOException {
		String fileName = req.getString("filename");
		int fileSize = req.getInteger("filesize");

		Session session = req.getSession();
		Map<String, Object> m = (Map<String, Object>) session.get("upload");
		if (m == null) {
			m = new ConcurrentHashMap<String, Object>();
			session.setProperty("upload", m);
		}

		String dataDir = System.getProperty("kraken.data.dir");
		File uploadDir = new File(dataDir, "frodo-core");
		uploadDir.mkdirs();

		File temp = File.createTempFile("upload-", ".tmp", uploadDir);
		temp.getParentFile().mkdirs();
		m.put("data", new UploadMetadata(fileName, fileSize, temp));
	}

	private static class UploadMetadata {
		public String fileName;
		public int fileSize;
		public File temp;

		public UploadMetadata(String fileName, int fileSize, File temp) {
			this.fileName = fileName;
			this.fileSize = fileSize;
			this.temp = temp;
		}
	}

	@MsgbusMethod
	public void uploadPart(Request req, Response resp) throws IOException {
		String data = req.getString("data");
		String flag = req.getString("flag");
		Session session = req.getSession();
		byte[] b = Base64.decodeBase64(data);

		logger.trace("frodo core: flag=[{}]", flag);

		UploadMetadata meta = null;
		if (!flag.equals("close"))
			meta = getUploadMetadata(req);

		FileOutputStream os = (FileOutputStream) session.get(UPGRADE_FILE_KEY);
		if (os == null) {
			os = new FileOutputStream(meta.temp, true);
			session.setProperty(UPGRADE_FILE_KEY, os);
			logger.trace("frodo core: start upload upgrade file");
		}

		if (flag.equals("upload")) {
			os.write(b);
		} else { // flag == close
			if (os != null)
				try {
					os.close();
					session.unsetProperty(UPGRADE_FILE_KEY);
					logger.trace("frodo core: upgrade file stream is closed");
				} catch (IOException e) {
				}
		}
	}

	@SuppressWarnings("unchecked")
	private UploadMetadata getUploadMetadata(Request req) {
		Session session = req.getSession();

		Map<String, Object> m = (Map<String, Object>) session.get("upload");
		if (m == null)
			throw new MsgbusException("frodo", "upload-data-not-found");

		UploadMetadata meta = (UploadMetadata) m.get("data");
		if (meta == null)
			throw new MsgbusException("frodo", "upload-data-not-found");
		return meta;
	}

	@MsgbusMethod
	public void upgrade(Request req, Response resp) {
		UploadMetadata meta = getUploadMetadata(req);
		Session session = req.getSession();

		if (meta.temp.length() != meta.fileSize) {
			logger.trace("frodo core: meta temp size=[{}], meta file size=[{}]", meta.temp.length(), meta.fileSize);
			throw new MsgbusException("frodo", "upload-size-mismatch");
		}

		try {
			Upgrade.upgrade(meta.temp, meta.fileName);
			session.unsetProperty(UPGRADE_FILE_KEY);
		} catch (Exception e) {
			logger.error("frodo core: upgrade failed", e);
			throw new MsgbusException("frodo", "upgrade-failure");
		}
	}

	@MsgbusMethod
	public void getAccessGateway(Request req, Response resp) {
		AccessGateway gw = gatewayApi.getCurrentAccessGateway();
		resp.put("gateway", (gw != null) ? gw.marshal() : null);
	}

	@MsgbusMethod
	public void testRadiusConfig(Request req, Response resp) {
		try {
			List<Radius> readConfig = XtmConfig.readConfig(Radius.class);
			if (readConfig.isEmpty())
				throw new IllegalStateException("no external-auth configuration");

			Radius radius = null;
			for (Radius r : readConfig) {
				if (r.getType() == Radius.Type.Radius)
					radius = r;
			}

			if (radius == null)
				throw new IllegalStateException("no RADIUS configuration");

			InetAddress addr = InetAddress.getByName(radius.getRadiusIp());
			int port = radius.getAuthPort();
			String sharedSecret = radius.getRadiusPassword();
			String authMethod = radius.getAuthMethod().toString();
			if (authMethod == null)
				authMethod = "PAP";

			String testAccount = req.getString("test_account");
			String testPassword = req.getString("test_password");

			RadiusClient client = new RadiusClient(addr, port, sharedSecret);

			Authenticator auth = null;
			if (authMethod.equals("PAP"))
				auth = new PapAuthenticator(client, testAccount, testPassword);
			else
				auth = new ChapAuthenticator(client, testAccount, testPassword);

			RadiusPacket response = client.authenticate(auth);
			if (response instanceof AccessAccept)
				resp.put("result", "accept");
			else
				resp.put("result", "reject");
		} catch (IOException e) {
			resp.put("result", "error");
			resp.put("reason", e.getMessage());
		}
		if (logger.isDebugEnabled()) {
			logger.debug("RADIUS connection test: result: {}", resp.get("result"));
			if (resp.containsKey("reason"))
				logger.debug("RADIUS connection test: reason: {}", resp.get("reason"));
		}
	}

	@MsgbusMethod
	public void testLdapConfig(Request req, Response resp) {
		List<Radius> readConfig = XtmConfig.readConfig(Radius.class);
		if (readConfig.isEmpty())
			throw new IllegalStateException("no external-auth configuration");

		Radius radius = null;
		for (Radius r : readConfig) {
			if (r.getType() == Radius.Type.Domain)
				radius = r;
		}

		if (radius == null)
			throw new IllegalStateException("no RADIUS configuration");

		String addr = radius.getLdapAddress();
		String account = radius.getLdapAccount();
		String password = radius.getLdapPassword();
		String trustStore = radius.getLdapTrustStore();
		String baseDn = radius.getLdapBaseDn();

		String testAccount = req.getString("test_account");
		String testPassword = req.getString("test_password");

		LdapProfile profile = new LdapProfile();
		try {
			profile.setName("frodo-test");
			profile.setServerType(getLdapServerType(radius));
			profile.setDc(addr);
			profile.setBaseDn(baseDn);
			profile.setAccount(account);
			profile.setPassword(password);
			if (radius.isLdapUseTrustStore())
				profile.setTrustStore(LdapProfile.CertificateType.X509, trustStore);

			logger.debug("frodo core: try ldap auth with " + profile);
			boolean result = ldap.verifyPassword(profile, testAccount, testPassword, 5000);
			resp.put("result", result ? "accept" : "reject");
		} catch (Exception e) {
			resp.put("result", "error");
			resp.put("reason", e.getMessage());
			logger.error("frodo core: ldap auth failed with " + profile, e);
		}
		if (logger.isDebugEnabled()) {
			logger.debug("LDAP connection test: result: {}", resp.get("result"));
			if (resp.containsKey("reason"))
				logger.debug("LDAP connection test: reason: {}", resp.get("reason"));
		}
	}

	private LdapServerType getLdapServerType(Radius radius) {
		return radius.getLdapType() == Fileserver.Active_Directory ? LdapServerType.ActiveDirectory
				: LdapServerType.SunOneDirectory;
	}

	@SuppressWarnings("unchecked")
	@MsgbusMethod
	public void setSSLConfig(Request req, Response resp) {
		SSLConfig c = new SSLConfig();
		c.setProtocol(req.getString("protocol"));
		
		c.setLoginMethod(req.getInteger("login_method"));
		c.setUseDeviceAuth(req.getBoolean("use_device_auth"));
		c.setUseRadius(req.getBoolean("radius_use"));
		c.setUseLdap(req.getBoolean("ldap_use"));
		c.setRadiusOrgUnitId(req.getString("radius_org_unit_id"));
		c.setUseSplitRouting(req.getBoolean("use_split_routing"));
		c.setSplitRoutingEntries(parseRoutingEntries((List<Object>) req.get("split_routing_entries")));
//		c.setDnsPostfix(req.getString("dns_postfix"));
		
		c.setNotice(req.getString("notice"));
		c.setLnkName(req.getString("lnk_name"));
		c.setUseSqlAuth(req.getBoolean("use_sql_auth"));
		c.setUserSql(req.getString("user_sql"));
		c.setAuthSql(req.getString("auth_sql"));
		c.setIdnSql(req.getString("idn_sql"));
		c.setDbAccount(req.getString("db_account"));
		c.setDbPassword(req.getString("db_password"));
		c.setDbConnectionString(req.getString("db_connstring"));
		c.setPasswordHashType(req.getString("password_hash_type"));
		c.setSaltLocation(req.getInteger("salt_location"));
		c.setSaltSql(req.getString("salt_sql"));
		c.setDeviceAuthMode(req.getString("device_auth_mode"));
		c.setDeviceKeyType((List<String>) req.get("device_key_type"));
		c.setPasswordResetMessage(req.getString("password_reset_msg"));
		c.setIdLabel(req.getString("id_label"));
		c.setIdentificationMode(req.getString("identification_mode"));
		c.setSubjectDnSql(req.getString("subject_dn_sql"));
		c.setTopImage(req.getString("top_image"));
		c.setNoticeImage(req.getString("notice_image"));
		c.setCertDialogImage(req.getString("cert_dialog_image"));
		c.setSubjectDnHashType(req.getString("subject_dn_hash_type"));
		c.setPasswordExpirySql(req.getString("password_expiry_sql"));
		c.setPasswordEncoding(req.getString("password_encoding"));
		c.setSubjectDnCharset(req.getString("subject_dn_charset"));
		c.setSubjectDnEncoding(req.getString("subject_dn_encoding"));
		c.setAutoUserLockDate(req.getInteger("auto_user_lock_date"));
		c.setPageTitle(req.getString("page_title"));
		c.setAdminServletName(req.getString("admin_servlet_name"));

		ExternalVpn extVpn = new ExternalVpn();
		extVpn.setType(req.getString("external_vpn_type"));
		extVpn.setAddress(parseExternalVpn((List<Object>) req.get("external_vpn_address")));
		c.setExtVpn(extVpn);
		c.setDeviceExpireMsg(req.getString("device_expire_msg"));
		c.setLdapMode(req.getString("ldap_mode") == null ? "NONE" : req.getString("ldap_mode"));
		c.setUseAutoReconnect(req.getBoolean("use_auto_reconnect"));
		c.setUseClientAutoUninstall(req.getBoolean("use_client_auto_uninstall"));
		c.setUseWebProxy(req.getBoolean("use_web_proxy"));
		c.setWebProxyCacheSize(req.getInteger("web_proxy_cache_size"));
		c.setWebProxyPort(req.getInteger("web_proxy_port"));
		c.setDisasterRecoveryList((List<String>) req.get("disaster_recovery_list"));
		c.setUseClientType((List<String>) req.get("use_client_type"));
		c.setUseIntegratedManagement(req.getBoolean("use_integrated_management"));
		c.setParentNode(req.getString("parent_node"));
		c.setUsePasswordReset(req.getBoolean("use_password_reset"));
		c.setUseAuthCenter(req.getBoolean("use_auth_center"));
		c.setClientComment(req.getString("client_comment"));

		gatewayApi.setSSLConfig(c);
		
		sqlAuthService.settingChanged();
	}
	
	@SuppressWarnings("unchecked")
	@MsgbusMethod
	public void setVpnServerConfig(Request req, Response resp) {
		VpnServerConfigParams c = new VpnServerConfigParams();
		
		VpnServerConfig config = vpnServerConfigApi.getCurrentVpnServerConfig();
		if(isPortUsing(config.getSslPort(), req.getInteger("ssl_port"))) {
			throw new MsgbusException("frodo", "ssl-port-is-using");
		}
		
		if(isPortUsing(config.getProxyPort(), req.getInteger("proxy_port"))) {
			throw new MsgbusException("frodo", "proxy-port-is-using");
		}
		
		c.setVpnIp(req.getString("vpn_ip"));
		c.setVpnNetmask(req.getString("vpn_netmask"));
		c.setSslPort(req.getInteger("ssl_port"));
		c.setUseObfuscationKey(req.getBoolean("use_obfuscation_key"));
		c.setEncryptions((List<String>) req.get("encryptions"));
		c.setDnsAddr1(req.getString("dns_addr1"));
		c.setDnsAddr2(req.getString("dns_addr2"));
		c.setUseRemoteDb(req.getBoolean("use_remote_db"));
		c.setRemoteKillIp(req.getString("remote_kill_ip"));
		c.setRemoteDbHostName(req.getString("remote_db_host_name"));
		c.setRemoteDbLoginName(req.getString("remote_db_login_name"));
		c.setRemoteDbPassword(req.getString("remote_db_password"));
		c.setRemoteDbName(req.getString("remote_db_name"));
		c.setRemoteDbTableName(req.getString("remote_db_table_name"));
		c.setRemoteDbPort(req.getString("remote_db_port"));
		c.setRemoteDbSocket(req.getString("remote_db_socket"));
		c.setUseTcpAcceleration(req.getBoolean("use_tcp_acceleration"));
		c.setUsePacketCompress(req.getBoolean("use_packet_compress"));
		c.setProxyPort(req.getInteger("proxy_port"));
		
		vpnServerConfigApi.setVpnServerConfig(c);
	}
	
	@SuppressWarnings("unchecked")
	@MsgbusMethod
	public void setIOSVpnServerConfig(Request req, Response resp) {
		IOSVpnServerConfigParams c = new IOSVpnServerConfigParams();
		IOSVpnServerConfig config = vpnServerConfigApi.getCurrentIOSVpnServerConfig();
		if(isPortUsing( config.getSslPort(), req.getInteger("ssl_port"))) {
			throw new MsgbusException("frodo", "ssl-port-is-using");
		}
		
		c.setVpnIp(req.getString("vpn_ip"));
		c.setVpnNetmask(req.getString("vpn_netmask"));
		c.setSslPort(req.getInteger("ssl_port"));
		c.setUseIOS(req.getBoolean("use_ios"));
		c.setEncryptions((List<String>) req.get("encryptions"));
		c.setDnsAddr1(req.getString("dns_addr1"));
		c.setDnsAddr2(req.getString("dns_addr2"));
		
		vpnServerConfigApi.setIOSVpnServerConfig(c);
	}
	
	private boolean isPortUsing(int currentPort, int changePort) {
		if(currentPort == changePort)	//사용중인 포트와 변경할 포트가 같을 경우.
			return false;
		
		String[] cmd = new String[]{"/bin/sh", "-c","netstat -an | grep " + changePort};
		Process p = null;
        BufferedReader br = null;
        try {
            p = Runtime.getRuntime().exec(cmd);
            br = new BufferedReader(new InputStreamReader(p.getInputStream()));
            try {
                    p.waitFor();
            } catch (InterruptedException e) {
            }

            String s = br.readLine();
            if (s == null)
            	return false;
            else
            	return true;
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (br != null)
                        br.close();
            } catch (IOException e) {
            }
        }
        return true;
	}
	
	@MsgbusMethod
	public void getVpnServerConfig(Request req, Response resp) {
		VpnServerConfig config = vpnServerConfigApi.getCurrentVpnServerConfig();
		resp.put("vpn_config", (config != null) ? config.marshal() : null);
	}
	
	@MsgbusMethod
	public void getIOSVpnServerConfig(Request req, Response resp) {
		IOSVpnServerConfig config = vpnServerConfigApi.getCurrentIOSVpnServerConfig();
		resp.put("vpn_config", (config != null) ? config.marshal() : null);
	}

	@SuppressWarnings("unchecked")
	private List<Map<String, Object>> parseExternalVpn(List<Object> extVpn) {
		if (extVpn == null)
			return null;

		logger.trace("frodo core: request add external vpn information [{}]", extVpn);
		List<Map<String, Object>> l = new ArrayList<Map<String, Object>>();

		for (Object o : extVpn)
			l.add((Map<String, Object>) o);

		return l;
	}

	@SuppressWarnings("unchecked")
	private List<SplitRoutingEntry> parseRoutingEntries(List<Object> entries) {
		List<SplitRoutingEntry> s = new ArrayList<SplitRoutingEntry>();

		for (Object entry : entries)
			s.add(parseRoutingEntry((Map<String, Object>) entry));

		return s;
	}

	private SplitRoutingEntry parseRoutingEntry(Map<String, Object> m) {
		SplitRoutingEntry e = new SplitRoutingEntry();
		e.setIp((String) m.get("ip"));
		e.setCidr((Integer) m.get("cidr"));
		e.setGateway((String) m.get("gateway"));
		return e;
	}

	@MsgbusMethod
	public void testDbConnection(Request req, Response resp) {
		String dbAccount = req.getString("db_account");
		String dbPassword = req.getString("db_password");
		String dbConnectionString = req.getString("db_connstring");

		@SuppressWarnings("unused")
		Connection con = null;

		try {
			con = getConnection(dbAccount, dbPassword, dbConnectionString);
		} catch (SQLException e) {
			throw new MsgbusException("frodo", "rawmsg", newErrorMsg(e.toString()));
		} catch (ClassNotFoundException e) {
			throw new MsgbusException("frodo", "rawmsg", newErrorMsg(e.toString()));
		}
	}

	private Map<String, Object> newErrorMsg(String msg) {
		Map<String, Object> m = new HashMap<String, Object>();
		m.put("msg", msg);
		return m;
	}

	@MsgbusMethod
	public void testUserSql(Request req, Response resp) {

		String dbAccount = req.getString("db_account");
		String dbPassword = req.getString("db_password");
		String dbConnectionString = req.getString("db_connstring");
		String selectQuery = req.getString("user_sql");

		String loginName = req.getString("login_name");

		Connection con = null;
		PreparedStatement psmt = null;

		try {

			con = getConnection(dbAccount, dbPassword, dbConnectionString);

			psmt = con.prepareStatement(selectQuery);
			psmt.setString(1, loginName);
			ResultSet rs = psmt.executeQuery();

			// 결과값 반환
			if (rs.next()) {
				resp.put("name", rs.getString(1));
				resp.put("dept_name", rs.getString(2));
			}

		} catch (Exception e) {
			throw new MsgbusException("frodo", "rawmsg", newErrorMsg(e.toString()));
		} finally {
			close(psmt, con);
		}

	}

	@MsgbusMethod
	public void testAuthSql(Request req, Response resp) {
		String dbAccount = req.getString("db_account");
		String dbPassword = req.getString("db_password");
		String dbConnectionString = req.getString("db_connstring");
		String authQuery = req.getString("auth_sql");
		String saltQuery = req.getString("salt_sql");
		String hashType = req.getString("hash_type");
		Integer saltLocation = req.getInteger("salt_location");
		String passwordEnconding = req.getString("password_encoding");

		logger.debug("frodo core: sql auth, authSql=[{}]", authQuery);

		String loginName = req.getString("login_name");
		String loginPassword = req.getString("password");

		Connection con = null;
		PreparedStatement psmt = null;
		String salt = null;

		try {

			con = getConnection(dbAccount, dbPassword, dbConnectionString);
			ResultSet rs = null;
			if (saltQuery != null && !saltQuery.trim().isEmpty()) {
				psmt = con.prepareStatement(saltQuery);

				psmt.setString(1, loginName);
				rs = psmt.executeQuery();

				// 결과값 반환
				if (rs.next()) {
					salt = rs.getString(1);
				}
			}
			psmt.close();
			AccessGateway gw = gatewayApi.getCurrentAccessGateway();

			if (gw == null || !gw.isUseSqlAuth()) {
				logger.warn("frodo core: sql auth, config not set");
			}

			logger.debug("frodo core: hash_typt [{}]", hashType);

			if (hashType != null) {

				String hashSource = loginPassword;

				logger.debug("frodo core: sql auth, login name=[{}], source=[{}], encoding=[{}]", new Object[] { loginName,
						hashSource, passwordEnconding });
				logger.debug("frodo core: sql auth, salt=[{}]", salt);

				if (salt != null && !salt.isEmpty() && saltLocation != null) {
					// front(1), rear(2)
					if (saltLocation == 1)
						hashSource = salt + loginPassword;
					else if (saltLocation == 2)
						hashSource = loginPassword + salt;
				}

				loginPassword = PasswordHash.makeHash(hashType, hashSource, passwordEnconding);

				logger.debug("frodo core: sql auth, makeHash=[{}]", loginPassword);
			}

			psmt = con.prepareStatement(authQuery);
			psmt.setString(1, loginName);
			psmt.setString(2, loginPassword);
			rs = psmt.executeQuery();

			// 결과값 반환
			if (rs.next()) {
				resp.put("name", rs.getString(1));
				resp.put("dept_name", rs.getString(2));
			}

		} catch (Exception e) {
			throw new MsgbusException("frodo", "rawmsg", newErrorMsg(e.toString()));
		} finally {
			close(psmt, con);
		}
	}

	@MsgbusMethod
	public void testIdnSql(Request req, Response resp) {
		String dbAccount = req.getString("db_account");
		String dbPassword = req.getString("db_password");
		String dbConnectionString = req.getString("db_connstring");
		String selectQuery = req.getString("idn_sql");

		String loginName = req.getString("login_name");

		logger.debug("frodo core: sql auth, idnSql=[{}]", selectQuery);

		Connection con = null;
		PreparedStatement psmt = null;

		try {

			con = getConnection(dbAccount, dbPassword, dbConnectionString);

			psmt = con.prepareStatement(selectQuery);
			psmt.setString(1, loginName);
			ResultSet rs = psmt.executeQuery();

			// 결과값 반환
			if (rs.next()) {
				resp.put("idn", rs.getString(1));
			}

		} catch (Exception e) {
			throw new MsgbusException("frodo", "rawmsg", newErrorMsg(e.toString()));
		} finally {
			close(psmt, con);
		}
	}

	@MsgbusMethod
	public void testPasswordExpirySql(Request req, Response resp) {
		String dbAccount = req.getString("db_account");
		String dbPassword = req.getString("db_password");
		String dbConnectionString = req.getString("db_connstring");
		String passwordExpirySql = req.getString("password_expiry_sql");

		String loginName = req.getString("login_name");

		logger.debug("frodo core: password expiry sql [{}]", passwordExpirySql);

		Connection con = null;
		PreparedStatement psmt = null;

		try {

			con = getConnection(dbAccount, dbPassword, dbConnectionString);

			psmt = con.prepareStatement(passwordExpirySql);
			psmt.setString(1, loginName);
			ResultSet rs = psmt.executeQuery();

			// 결과값 반환
			if (rs.next()) {
				resp.put("password_expiry", rs.getString(1));
			}

		} catch (Exception e) {
			throw new MsgbusException("frodo", "rawmsg", newErrorMsg(e.toString()));
		} finally {
			close(psmt, con);
		}
	}

	@MsgbusMethod
	public void testSaltSql(Request req, Response resp) {

		String dbAccount = req.getString("db_account");
		String dbPassword = req.getString("db_password");
		String dbConnectionString = req.getString("db_connstring");
		String selectQuery = req.getString("salt_sql");

		String loginName = req.getString("login_name");

		logger.debug("frodo core: sql auth, saltSql=[{}]", selectQuery);

		Connection con = null;
		PreparedStatement psmt = null;

		try {

			con = getConnection(dbAccount, dbPassword, dbConnectionString);

			psmt = con.prepareStatement(selectQuery);
			psmt.setString(1, loginName);
			ResultSet rs = psmt.executeQuery();

			// 결과값 반환
			if (rs.next()) {
				resp.put("salt", rs.getString(1));
			}

		} catch (Exception e) {
			throw new MsgbusException("frodo", "rawmsg", newErrorMsg(e.toString()));
		} finally {
			close(psmt, con);
		}
	}

	@MsgbusMethod
	public void testSubjectDnSql(Request req, Response resp) {

		String dbAccount = req.getString("db_account");
		String dbPassword = req.getString("db_password");
		String dbConnectionString = req.getString("db_connstring");
		String selectQuery = req.getString("subject_dn_sql");

		String loginName = req.getString("login_name");

		logger.debug("frodo core: sql auth, subjectDnSql=[{}]", selectQuery);

		Connection con = null;
		PreparedStatement psmt = null;

		try {

			con = getConnection(dbAccount, dbPassword, dbConnectionString);

			psmt = con.prepareStatement(selectQuery);
			psmt.setString(1, loginName);
			ResultSet rs = psmt.executeQuery();

			// 결과값 반환
			if (rs.next()) {
				resp.put("subject_dn", rs.getString(1));
			}

		} catch (Exception e) {
			throw new MsgbusException("frodo", "rawmsg", newErrorMsg(e.toString()));
		} finally {
			close(psmt, con);
		}
	}

	private Connection getConnection(String id, String pw, String url) throws SQLException, ClassNotFoundException {

		logger.debug("frodo core: sql auth, connection string [{}]", url);

		String[] urlSplit = url.split(":");
		String urlKey = urlSplit[1].toLowerCase();

		if (urlKey.equals("oracle")) {
			// oracle
			Class.forName("oracle.jdbc.OracleDriver");
		} else if (urlKey.equals("sqlserver")) {
			// mssql
			Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
		} else if (urlKey.equals("mysql")) {
			// mysql
			Class.forName("com.mysql.jdbc.Driver");
		} else if (urlKey.equals("sybase")) {
			// sybase
			Class.forName("com.sybase.jdbc4.jdbc.SybDriver");
		}

		// Set Connection Time out in second
		DriverManager.setLoginTimeout(10);

		// return Connection
		Connection con = DriverManager.getConnection(url, id, pw);
		return con;

	}

	private void close(PreparedStatement psmt, Connection con) {
		logger.trace("frodo-external-auth: close sql jdbc");
		if (psmt != null)
			try {
				psmt.close();
			} catch (SQLException e) {
				throw new MsgbusException("frodo", e.toString());
			}
		if (con != null)
			try {
				con.close();
			} catch (SQLException e) {
				throw new MsgbusException("frodo", e.toString());
			}
	}

	@MsgbusMethod
	public void applyVpnConfig(Request req, Response resp) {
		try {
			xenicsConfig.applyVpnServerConf();
			if(xenicsConfig.isTunInteferfaceOk("tun0", false) == false)
				throw new MsgbusException("frodo", "tun0-not-loaded");
		} catch (IOException e) {
			logger.error("apply vpn server config failed", e);
			throw new MsgbusException("frodo", e.toString());
		} catch (InterruptedException e) {
			logger.error("apply vpn server config failed", e);
			throw new MsgbusException("frodo", e.toString());
		}
	}
	
	@MsgbusMethod
	public void applyIOSVpnConfig(Request req, Response resp) {
		try {
			xenicsConfig.applyIOSVpnServerConf();
			if(xenicsConfig.isTunInteferfaceOk("tun1", true) == false)
				throw new MsgbusException("frodo", "tun1-not-loaded");
		} catch (IOException e) {
			logger.error("apply ios vpn server config failed", e);
			throw new MsgbusException("frodo", e.toString());
		} catch (InterruptedException e) {
			logger.error("apply ios vpn server config failed", e);
			throw new MsgbusException("frodo", e.toString());
		}
	}
}
