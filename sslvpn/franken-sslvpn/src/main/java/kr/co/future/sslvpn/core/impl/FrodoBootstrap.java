package kr.co.future.sslvpn.core.impl;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import kr.co.future.sslvpn.core.ActivationListener;
import kr.co.future.sslvpn.core.GlobalConfig;
import kr.co.future.sslvpn.core.GlobalConfigApi;
import kr.co.future.sslvpn.core.InstallMonitor;
import kr.co.future.sslvpn.core.InstallerApi;
import kr.co.future.sslvpn.core.servlet.AdminConsoleResourceServlet;
import kr.co.future.sslvpn.model.AccessGateway;
import kr.co.future.sslvpn.model.api.AccessGatewayApi;
import kr.co.future.sslvpn.xtmconf.BridgeTunCachingService;
import kr.co.future.sslvpn.xtmconf.KLogWriter;
import kr.co.future.sslvpn.xtmconf.system.CommandUtil;
import kr.co.future.sslvpn.xtmconf.system.Integrity;
import kr.co.future.sslvpn.xtmconf.system.InterfaceInfo;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Invalidate;
import org.apache.felix.ipojo.annotations.Requires;
import org.apache.felix.ipojo.annotations.Validate;

import kr.co.future.confdb.ConfigService;
import kr.co.future.dom.api.AdminApi;
import kr.co.future.dom.api.DOMException;
import kr.co.future.dom.api.DefaultEntityEventListener;
import kr.co.future.dom.api.FileUploadApi;
import kr.co.future.dom.api.LoginCallback;
import kr.co.future.dom.api.OrganizationApi;
import kr.co.future.dom.model.Admin;
import kr.co.future.dom.model.Organization;
import kr.co.future.dom.model.OrganizationUnit;
import kr.co.future.httpd.HttpConfiguration;
import kr.co.future.httpd.HttpContext;
import kr.co.future.httpd.HttpServer;
import kr.co.future.httpd.HttpService;
import kr.co.future.logdb.LogQuery;
import kr.co.future.logdb.LogQueryService;
import kr.co.future.logdb.LogResultSet;
import kr.co.future.logdb.LookupHandler;
import kr.co.future.logdb.LookupHandlerRegistry;
import kr.co.future.logstorage.Log;
import kr.co.future.logstorage.LogStorage;
import kr.co.future.msgbus.Message;
import kr.co.future.msgbus.MessageBus;
import kr.co.future.msgbus.MessageListener;
import kr.co.future.msgbus.Session;
import kr.co.future.ntp.NtpSyncListener;
import kr.co.future.ntp.NtpSyncService;
import kr.co.future.radius.server.RadiusModule;
import kr.co.future.radius.server.RadiusModuleType;
import kr.co.future.radius.server.RadiusPortType;
import kr.co.future.radius.server.RadiusProfile;
import kr.co.future.radius.server.RadiusServer;
import kr.co.future.radius.server.RadiusVirtualServer;
import kr.co.future.rpc.RpcAgent;
import kr.co.future.rpc.RpcBindingProperties;
import kr.co.future.sslvpn.core.msgbus.AccessGatewayPlugin;
import kr.co.future.sslvpn.core.servlet.AdminConsoleBundleResourceServlet;
import kr.co.future.sslvpn.core.xenics.XenicsService;
import kr.co.future.syslog.SyslogProfile;
import kr.co.future.syslog.SyslogServer;
import kr.co.future.syslog.SyslogServerRegistry;
import kr.co.future.webconsole.servlet.FileUploadServlet;
import kr.co.future.webconsole.servlet.KeyDistributorServlet;
import kr.co.future.webconsole.servlet.MsgbusServlet;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(name = "frodo-bootstrap")
public class FrodoBootstrap implements ActivationListener, NtpSyncListener, MessageListener {
	private Logger logger = LoggerFactory.getLogger(FrodoBootstrap.class);
	private static final String flogsFile = "/etc/webadmin/conf/logsystem.conf";
	private static final String flogsFileOnCf = "/utm/conf/conf/logsystem.conf";
	private static final String logSettingXml = "/etc/webadmin/xml/log_setting.xml";
	private static final String logSettingXmlOnCf = "/utm/conf/xml/log_setting.xml";
	private List<String> msgbusMethod;

	@Requires
	private AdminApi adminApi;
	private AdminLoginLogWriter loginLogWriter = new AdminLoginLogWriter();

	@Requires
	private OrganizationApi orgApi;
	
	@Requires
	private SyslogServerRegistry syslogRegistry;

	@Requires
	private LogStorage storage;

	@Requires
	private AccessGatewayApi gatewayApi;

	@Requires
	private HttpService httpd;

	@Requires
	private MessageBus msgbus;

	@Requires
	private FileUploadApi fileUpload;

	@Requires
	private RpcAgent rpcAgent;

	@Requires
	private RadiusServer radius;

	@Requires
	private ConfigService conf;

	@Requires
	private NtpSyncService ntpSync;

	@Requires
	private LookupHandlerRegistry lookup;

	@Requires
	private InstallerApi installer;

	@Requires
	private BridgeTunCachingService bridgeApi;

	@Requires
	private LogQueryService qs;
	
	@Requires
	private XenicsService xenicsService;
	
	@Requires
	private GlobalConfigApi configApi;

	private BundleContext bc;

	private LookupHandler lookupHandler = new SSLplusAuditLookupHandler();
	
	private AdminConsoleBundleResourceServlet admBundleResourceServlet;
	
	private AdminConsoleResourceServlet adminConsoleResourceServlet;

	public FrodoBootstrap(BundleContext bc) {
		this.bc = bc;
	}

	@Validate
	public void validate() {
		// create LogStorage tables
		ensureTable("xtm");
		ensureTable("ssl-auth");
		ensureTable("ssl-access");
		ensureTable("ssl-flow");
		ensureTable("ssl-ca");
		ensureTable("ssl-revoked");
		ensureTable("perf");
		ensureTable("ifstats");
//		ensureTable("ssl-tunnelcount");
		ensureTable("audit");
		ensureTable("dnsproxy");
		ensureTable("log-capacity");
		ensureTable("watchcat-auth");
		
		// create xenics db tables
		xenicsService.createTables();

		// add msgbusMethodList
		msgbusMethod = getMsgbusMethodList();

		// add ntp callback
		ntpSync.addListener(this);

		// add msgbus callback
		msgbus.addMessageListener(this);

		// add lookup handler
		lookup.addLookupHandler("sslplus_audit", lookupHandler);

		try {
			Integrity.checkIntegrity();
		} catch (NullPointerException e) {
			logger.error("frodo core: cannot load integrity file");
		} catch (Exception e) {
			logger.error("frodo core: integrity failed", e);
		}

		try {
			for (InterfaceInfo info : InterfaceInfo.getInterfaceInfos()) {
				if (!info.getName().startsWith("eth"))
					continue;

				if (info.isUp()) {
					String desc = String.format("%s: Link Up %s %s Duplex", info.getName(), info.getSpeed(),
							info.getDuplex());
					KLogWriter.write(0x11060008, null, desc);
				}
			}
		} catch (NullPointerException e) {
			logger.error("frodo core: cannot load interface data");
		}

		logger.trace("frodo core: hook login event callback");
		adminApi.registerLoginCallback(loginLogWriter);

		// open syslog server
		try {
			SyslogServer server = syslogRegistry.findServer(new InetSocketAddress(514));
			if (server == null) {
				SyslogProfile profile = new SyslogProfile();
				profile.setName("frodo");
				syslogRegistry.open(profile);
				logger.info("frodo core: syslog server loaded [{}]", profile);
			}
		} catch (Exception e) {
			logger.error("frodo core: cannot start syslog server", e);
		}

		// open radius server
		tryOpenRadiusServer();

		// check and update sync policy
		AccessGatewayPlugin.updatePolicySyncSchedule(gatewayApi);

		// open webconsole
		openManagementConsole();

		// redirect set context frodo
		removeRedirectServlet();

		// flogs reinit
		flogsReinit();

		try {
			installer.install(new ScriptInstallMonitor());
		} catch (Exception e) {
			logger.error("frodo core: cannot frodo install", e);
		}

		openRpcPort();

		orgApi.addEntityEventListener(orgUnitEventListner);
	}
	
	private DefaultEntityEventListener<Organization> orgUnitEventListner = new DefaultEntityEventListener<Organization>() {
		@Override
		public void entityAdded(String domain, Organization obj, Object state) {
			if (obj.getParameters() == null)
				return;
			
			String trustHosts = (String)obj.getParameters().get("dom.admin.trust_hosts");
			
			if(admBundleResourceServlet != null)
				admBundleResourceServlet.setTrustHosts(trustHosts);
			
			if(adminConsoleResourceServlet != null)
				adminConsoleResourceServlet.setTrustHosts(trustHosts);
		}

		@Override
		public void entityUpdated(String domain, Organization obj, Object state) {
			if (obj.getParameters() == null)
				return;

			String trustHosts = (String)obj.getParameters().get("dom.admin.trust_hosts");
			
			if(admBundleResourceServlet != null)
				admBundleResourceServlet.setTrustHosts(trustHosts);
			
			if(adminConsoleResourceServlet != null)
				adminConsoleResourceServlet.setTrustHosts(trustHosts);
		}

		@Override
		public void entityRemoved(String domain, Organization obj, Object state) {
			if(admBundleResourceServlet != null)
				admBundleResourceServlet.setTrustHosts(null);
			
			if(adminConsoleResourceServlet != null)
				adminConsoleResourceServlet.setTrustHosts(null);
		}
	};

	private List<String> getMsgbusMethodList() {
		List<String> methodList = new ArrayList<String>();
		BufferedReader br = null;
		InputStream is = null;
		try {
			is = getClass().getResourceAsStream("/msgbusMethod");
			br = new BufferedReader(new InputStreamReader(is));
			String line = null;
			while ((line = br.readLine()) != null) {
				line = line.trim();
				methodList.add(line);
			}
		} catch (IOException e) {
			logger.error("frodo core: msgbusMethod file error", e);
		} finally {
			if (br != null) {
				try {
					br.close();
				} catch (IOException e) {
				}
			}
		}

		return methodList;
	}

	private class ScriptInstallMonitor implements InstallMonitor {
		@Override
		public void println(Object value) {
			logger.info("frodo core: " + value);
		}
	}

	private void flogsReinit() {

		// fix conf, conf on CF
		fixConfFile(flogsFile, flogsFileOnCf, "HDD_LOG_USE=1", "HDD_LOG_USE=0");

		// fix xml , xml on CF
		fixConfFile(logSettingXml, logSettingXmlOnCf, "<hddsave chk_use=\"off\"></hddsave>",
				"<hddsave chk_use=\"on\"></hddsave>");

		// flogs reinit
		try {
			CommandUtil.run("/usr/local/bin/flogs", "reinit");
		} catch (IOException e) {
			logger.error("frodo core: flogs reinit error", e);
			return;
		}

	}

	private void fixConfFile(String filePath, String cfFilePath, String search, String dest) {

		File file = new File(filePath);
		File cfFile = new File(cfFilePath);

		String oldtext = "";
		BufferedReader br = null;

		try {
			br = new BufferedReader(new FileReader(file));
			String line = "";
			while ((line = br.readLine()) != null) {
				oldtext += line + "\n";
			}
		} catch (FileNotFoundException e) {
			logger.error("frodo core: [{}] file not found", filePath, e);
			return;
		} catch (IOException e) {
			logger.error("frodo core: [{}] read error", filePath, e);
			return;
		} finally {
			try {
				if (br != null)
					br.close();
			} catch (IOException e) {
			}
		}

		String newtext = oldtext.replaceAll(search, dest);

		// write conf and cf_conf
		BufferedWriter bw = null;
		BufferedWriter cfbw = null;
		try {
			bw = new BufferedWriter(new FileWriter(file));
			bw.write(newtext);

			CommandUtil.run(new File("/"), "mount", "-t", "ext3", "/dev/hda2", "/utm/conf");
			cfbw = new BufferedWriter(new FileWriter(cfFile));
			cfbw.write(newtext);
		} catch (IOException e) {
			logger.error("frodo core: [{}] write error", filePath, e);
			return;
		} finally {
			try {
				if (bw != null)
					bw.close();

				if (cfbw != null)
					cfbw.close();

				CommandUtil.run(new File("/"), "umount", "/utm/conf");

			} catch (IOException e) {
			}
		}

	}

	private void tryOpenRadiusServer() {
		RadiusModule userModule = radius.getModule(RadiusModuleType.UserDatabase);
		RadiusModule authModule = radius.getModule(RadiusModuleType.Authenticator);
		if (userModule.getFactory("ssluserdb") != null && authModule.getFactory("pap") != null) {
			openRadiusServer();
			return;
		}

		Timer t = new Timer("RADIUS Bootstrapper");
		t.scheduleAtFixedRate(new RadiusBootstrapper(), 0, 1000);
	}

	private class RadiusBootstrapper extends TimerTask {
		@Override
		public void run() {
			if (radius == null)
				return;

			RadiusModule userModule = radius.getModule(RadiusModuleType.UserDatabase);
			RadiusModule authModule = radius.getModule(RadiusModuleType.Authenticator);

			// if user module or auth module is null, it's stopping
			if (userModule == null || authModule == null) {
				cancel();
				return;
			}

			if (userModule.getFactory("ssluserdb") != null && authModule.getFactory("pap") != null) {
				openRadiusServer();
				cancel();
				return;
			}
		}
	}

	private void openRadiusServer() {
		if (radius.getModuleInstance(RadiusModuleType.UserDatabase, "ssluserdb") == null)
			radius.createModuleInstance(RadiusModuleType.UserDatabase, "ssluserdb", "ssluserdb",
					new HashMap<String, Object>());

		if (radius.getModuleInstance(RadiusModuleType.Authenticator, "pap") == null)
			radius.createModuleInstance(RadiusModuleType.Authenticator, "pap", "pap", new HashMap<String, Object>());

		if (radius.getProfile("sslplus") == null) {
			RadiusProfile profile = new RadiusProfile();
			profile.setName("sslplus");
			profile.setSharedSecret("future_01");
			profile.setAuthenticators(Arrays.asList("pap"));
			profile.setUserDatabases(Arrays.asList("ssluserdb"));
			radius.createProfile(profile);
		}

		InetSocketAddress bindAddr = null;
		try {
			bindAddr = new InetSocketAddress(InetAddress.getLocalHost(), 1812);
		} catch (UnknownHostException e) {
		}

		if (radius.getVirtualServer("sslplus") == null) {
			RadiusVirtualServer server = radius.createVirtualServer("sslplus", RadiusPortType.Authentication,
					"sslplus", bindAddr);
			try {
				server.open();
				logger.info("frodo core: opened radius server [{}]", server.getBindAddress());
			} catch (IOException e) {
				logger.error("frodo core: cannot open radius server", e);
			}
		}
	}

	private void removeRedirectServlet() {
		InetSocketAddress port80 = new InetSocketAddress(80);
		HttpServer server = httpd.getServer(port80);
		if (server != null) {
			HttpConfiguration configuration = server.getConfiguration();
			String defaultHttpContext = configuration.getDefaultHttpContext();
			if (defaultHttpContext != null) {
				if (defaultHttpContext.equals("redirect")) {
					httpd.removeServer(port80);
					configuration.setDefaultHttpContext("frodo");
					configuration.setIdleTimeout(120);
					server = httpd.createServer(configuration);
					server.open();
				}
			}
		}
	}

	private void openRpcPort() {
		for (RpcBindingProperties r : rpcAgent.getBindings())
			if (r.getPort() == 7140)
				return;

		RpcBindingProperties prop = new RpcBindingProperties("0.0.0.0", 7140, "rpc-agent", "rpc-ca");
		rpcAgent.open(prop);

	}

	private void ensureTable(String tableName) {
		try {
			storage.createTable(tableName);
		} catch (IllegalStateException e) {
		}
	}

	private void openManagementConsole() {
		GlobalConfig config = configApi.getGlobalConfig();

		HttpContext ctx = null;
		AccessGateway gw = gatewayApi.getCurrentAccessGateway();
		Boolean donotShowAdmin = false;
		String trustHosts = null;
		try{
            trustHosts = (String)orgApi.getOrganizationParameter("localhost", "dom.admin.trust_hosts");
        }catch(DOMException e){
            trustHosts = "";
        }
		
		try{
			if (config != null) {
				String adminConsolePath = config.getAdminConsolePath();
				donotShowAdmin = config.isDoNotShowAdmin();
				if (adminConsolePath != null && !adminConsolePath.trim().isEmpty()) {
					ctx = httpd.ensureContext(config.getAdminConsoleContext());
					adminConsoleResourceServlet = new AdminConsoleResourceServlet(adminConsolePath, donotShowAdmin, trustHosts);
					ctx.addServlet("console", adminConsoleResourceServlet, gw.getAdminConsolePath()
							+ "/*");
					logger.trace("frodo core: admin console loaded path [{}]", config.getAdminConsolePath());
				}
			}
	
			if (ctx == null) {
				Bundle ui = findUiBundle();
				if (ui != null) {
					ctx = httpd.ensureContext(config.getAdminConsoleContext());
					admBundleResourceServlet =  new AdminConsoleBundleResourceServlet(ui, "/WEB-INF", donotShowAdmin, trustHosts);
					if (gw == null) {
						logger.trace("frodo core: admin console loaded path [admin], currentAccessGateway not found");
						ctx.addServlet("console", admBundleResourceServlet, "/admin/*");
					} else {
						ctx.addServlet("console", admBundleResourceServlet, gw.getAdminConsolePath()
								+ "/*");
					}
				}
			}
		} catch(IllegalStateException e) {
			logger.info("frodo-bootstrap: console servlet already exist.");
		}

		if (ctx != null) {
			//to prevent invalid instance.
			try{
				// add keydist servlet
				KeyDistributorServlet keydist = new KeyDistributorServlet();
				keydist.setMessageBus(msgbus);
				keydist.setHttpService(httpd);
				ctx.addServlet("keydist", keydist, "/keydist");
			} catch(IllegalStateException e) {
				logger.info("frodo-bootstrap: keydist servlet already exist.");
			}
			
			try{
				// add msgbus servlet
				MsgbusServlet msgbusServlet = new MsgbusServlet();
				msgbusServlet.setHttpService(httpd);
				msgbusServlet.setMessageBus(msgbus);
				ctx.addServlet("msgbus", msgbusServlet, "/msgbus/*");
			} catch(IllegalStateException e) {
				logger.info("frodo-bootstrap: msgbus servlet already exist.");
			}
			
			try{
				// add upload servlet
				FileUploadServlet uploadServlet = new FileUploadServlet();
				uploadServlet.setFileUploadApi(fileUpload);
				ctx.addServlet("upload", uploadServlet, "/upload");
			} catch(IllegalStateException e) {
				logger.info("frodo-bootstrap: upload servlet already exist.");
			}
		}
	}

	private Bundle findUiBundle() {
		for (Bundle b : bc.getBundles())
			if (b.getSymbolicName().equals("kr.co.future.watchcat.ui"))
				return b;

		return null;
	}

	@Invalidate
	public void invalidate() {
		if (adminApi != null)
			adminApi.unregisterLoginCallback(loginLogWriter);

		if (httpd != null) {
			HttpContext ctx = httpd.ensureContext("frodo");
			ctx.removeServlet("console");
		}

		if (ntpSync != null)
			ntpSync.removeListener(this);

		if (msgbus != null)
			msgbus.removeMessageListener(this);

		if (lookup != null)
			lookup.removeLookupHandler("sslplus_audit");
	}

	private class AdminLoginLogWriter implements LoginCallback {
		@Override
		public void onLoginSuccess(Admin admin, Session session) {
			KLogWriter.write(0x12030003, session, "Login Success ID : " + admin.getUser().getLoginName());
			Map<String, Object> data = new HashMap<String, Object>();
			data.put("type", "login");
			data.put("login_name", admin.getUser().getLoginName());
			OrganizationUnit orgUnit = admin.getUser().getOrgUnit();
			data.put("org_unit_name", orgUnit == null ? null : orgUnit.getName());
			data.put("org_unit_guid", orgUnit == null ? null : orgUnit.getGuid());
			data.put("remote_address", session.getRemoteAddress().getHostAddress());
			Log log = new Log("watchcat-auth", new Date(), data);
			writeToLogstorage(log);
		}

		@Override
		public void onLoginFailed(Admin admin, Session session, DOMException e) {
			logger.trace("frodo core: login fail [{}] for admin [{}]", e.getErrorCode(), admin.getUser().getLoginName());

			String msg = "로그인에 실패했습니다.";
			if (e.getErrorCode().equals("invalid-password"))
				msg = "아이디와 패스워드가 일치하지 않습니다.";
			else if (e.getErrorCode().equals("invalid-otp-password"))
				msg = "아이디와 OTP 패스워드가 일치하지 않습니다.";
			else if (e.getErrorCode().equals("not-trust-host"))
				msg = "허가되지 않은 IP에서 접속하였습니다.";
			KLogWriter.write(0x12040004, session, msg + " ID : " + admin.getUser().getLoginName());
			Map<String, Object> data = new HashMap<String, Object>();
			data.put("type", "login-fail");
			data.put("login_name", admin.getUser().getLoginName());
			OrganizationUnit orgUnit = admin.getUser().getOrgUnit();
			data.put("org_unit_name", orgUnit == null ? null : orgUnit.getName());
			data.put("org_unit_guid", orgUnit == null ? null : orgUnit.getGuid());
			data.put("remote_address", session.getRemoteAddress().getHostAddress());
			data.put("reason", msg);
			Log log = new Log("watchcat-auth", new Date(), data);
			writeToLogstorage(log);
		}

		@Override
		public void onLoginLocked(Admin admin, Session session) {
			KLogWriter.write(0x12060005, session, "연속 로그인 실패로 접속이 제한됩니다. ID : " + admin.getUser().getLoginName());
		}

		@Override
		public void onLogout(Admin admin, Session session) {
			KLogWriter.write(0x12030006, session, "Logout ID : " + admin.getUser().getLoginName());
			Map<String, Object> data = new HashMap<String, Object>();
			data.put("type", "logout");
			data.put("login_name", admin.getUser().getLoginName());
			OrganizationUnit orgUnit = admin.getUser().getOrgUnit();
			data.put("org_unit_name", orgUnit == null ? null : orgUnit.getName());
			data.put("org_unit_guid", orgUnit == null ? null : orgUnit.getGuid());
			data.put("remote_address", session.getRemoteAddress().getHostAddress());
			Log log = new Log("watchcat-auth", new Date(), data);
			writeToLogstorage(log);
		}
		
		private void writeToLogstorage(Log log) {
			try{
				storage.write(log);
			} catch(Exception e) {
				logger.error("write admin login log error", e);
			}
		}
	}

	@Override
	public void onActivated() {
		openManagementConsole();
	}

	@Override
	public void onSetTime(Date newTime) {
		String s = new SimpleDateFormat("MMddHHmmyyyy.ss").format(newTime);
		try {
			Runtime.getRuntime().exec("hwclock -w -u");
		} catch (Throwable t) {
			logger.error("frodo core: cannot set hwclock to [{}]", s);
		}
	}

	@Override
	public void onMessage(Session session, Message message) {
		if (msgbusMethod.contains(message.getMethod())) {
			String change = "";
			logger.debug("frodo core: admin control msgbusMethod [{}]", message.getMethod());
			Map<String, Object> m = new HashMap<String, Object>();

			Object nParams = message.getParameters();
			Object oParams = (Object) getLastLog(message.getMethod());

			
			m.put("login_name", session.getAdminLoginName());
			m.put("method", message.getMethod());
			m.put("parameters", nParams);
			m.put("before_parameters", oParams);
			m.put("error", message.getErrorMessage());

			/*
			if (message.getMethod().contains("UserPlugin")) {
				change = userAudit(message.getMethod(), oParams, nParams);
			} else {
				if (oParams.getClass().getSimpleName().contains("String"))
					change = diffString(oParams, nParams);
				else
					change = diffMap(oParams, nParams);
			}
			logger.debug("New Parameters ==> {}", nParams);
			logger.debug("Old Parameters ==> {}", oParams);
			logger.debug("Message Method ==> {} : {}", message.getMethod(), change);

			m.put("differences", change);
			*/

			Log log = new Log("audit", new Date(), m);
			storage.write(log);
		}
	}

	private Object getLastLog(String method) {
		String query = "table audit | fields method, parameters | search method == " + method;

		LogQuery lq = null;
		try {
			lq = qs.createQuery(query);
			qs.startQuery(lq.getId());

			do {
				try {
					Thread.sleep(100);
				} catch (InterruptedException e) {
				}
			} while (!lq.isEnd());
			
			LogResultSet rs = lq.getResult();
			if(rs == null)
				return "";
			if (!rs.hasNext())
				return "";
			Map<String, Object> before = rs.next();
			return before.get("parameters");
		} catch (Exception e) {
			logger.error("frodo core: cannot obtain before parameter log, method [{}]", method);
			return "";
		} finally { 
			if(lq != null) 
				qs.removeQuery(lq.getId()); 
		} 
	}

	private String diffString(Object obj1, Object obj2) {
		String diffStr = "";

		if (!obj1.equals(obj2)) {
			diffStr = String.format("[ %s -> %s ] ", obj1.toString(), obj2.toString());
			logger.info("==> {}", diffStr);
		}

		return diffStr;
	}

	@SuppressWarnings("unchecked")
	private String diffMap(Object obj1, Object obj2) {
		String diffStr = "";
		String type = "";
		Map<String, Object> oObj = (Map<String, Object>) obj1;
		Map<String, Object> nObj = (Map<String, Object>) obj2;

		for (String key : oObj.keySet()) {
			Object oldVal = oObj.get(key);
			Object newVal = nObj.get(key);

			if (newVal != null)
				type = newVal.getClass().getSimpleName();
			else
				type = "null";

			logger.info("==> {} {} ", key, type);

			if (type == "null") {
				if (oldVal != null) {
					diffStr = String.format("[ %s -> null ] ", oldVal);
				}
			} else if (type.contains("Boolean")) {
				if (oldVal == null)
					diffStr = String.format("[ false -> %s ] ", newVal);
				else if (!oldVal.equals(newVal))
					diffStr = String.format("[ %s -> %s ] ", oldVal, newVal);
			} else if (type.contains("String")) {
				if (oldVal == null)
					diffStr = String.format("[ [] -> %s ] ", newVal);
				else if (!oldVal.equals(newVal))
					diffStr = String.format("[ %s -> %s ] ", oldVal, newVal);
			} else if (type.contains("Integer")) {
				if (oldVal == null)
					diffStr = String.format("[ 0 -> %d ] ", newVal);
				else if (!oldVal.equals(newVal))
					diffStr = String.format("[ %d -> %d ] ", oldVal, newVal);
			} else if (type.contains("ArrayList")) {
				if (oldVal == null) {
					diffStr = String.format("[ null -> %s ] ", newVal);
					continue;
				}

				Object[] array = (Object[]) oldVal;
				List<Object> arrO = new ArrayList<Object>();
				List<Object> arrN = (List<Object>) newVal;

				for (int i = 0; i < array.length; i++) {
					Map<String, String> old1 = (Map<String, String>) array[i];
					Map<String, String> old2 = new LinkedHashMap<String, String>();
					if (key.contains("ip_lease_ranges")) {
						old2.put("ip_from", old1.get("ip_from"));
						old2.put("ip_to", old1.get("ip_to"));
						// logger.info("============> {}", old2.toString());
						arrO.add(old2);
					} else if (key.contains("internal")) {
						old2.put("ip", old1.get("ip"));
						old2.put("cidr", old1.get("cidr"));
						arrO.add(old2);
					} else
						arrO.add(old1);
				}
				// logger.info("============> {}", arrO.toString());
				// logger.info("============> {}", arrN.toString());

				if (arrO.size() != arrN.size())
					diffStr = String.format("[ %s -> %s ] ", arrO.toString(), arrN.toString());
				else {
					boolean flag = false;
					for (int i = 0; i < arrO.size(); i++) {
						flag = true;
						for (int j = 0; j < arrN.size(); j++) {
							if (arrO.get(i).equals(arrN.get(j))) {
								flag = false;
								break;
							}
						}
						if (flag)
							break;
					}
					if (flag)
						diffStr = String.format("[ %s -> %s ] ", arrO.toString(), arrN.toString());
				}
			}
		}
		logger.info("==> {}", diffStr);
		return diffStr;
	}

	@SuppressWarnings("unchecked")
	private String userAudit(String method, Object oldP, Object newP) {
		String retStr = "";
		Map<String, Object> mObj = (Map<String, Object>) newP;

		if (method.contains("removeUsers"))
			retStr = "remove user " + mObj.get("login_names");
		else if (method.contains("createUser"))
			retStr = "create user " + mObj.get("login_name");
		else if (method.contains("updateUser")) {
			if (mObj.get("password") != null)
				retStr = "update password [" + mObj.get("login_name") + "]";
			else
				retStr = "update user information : " + newP.toString();
		}

		return retStr;
	}

	private class SSLplusAuditLookupHandler implements LookupHandler {
		private static final String XTMCONF = "kr.co.future.frodo.xtmconf.msgbus.";
		private static final String FRODO_CORE = "kr.co.future.frodo.core.msgbus.";
		private static final String KRAKEN_DOM = "kr.co.future.dom.msgbus.";
		private static final String KRAKEN_LDAP = "kr.co.future.ldap.msgbus.";
		private Map<String, String> methodMenuMappings = new HashMap<String, String>();
		private Map<String, String> methodOperationMappings = new HashMap<String, String>();

		public SSLplusAuditLookupHandler() {

			/** 시스템 **/
			/* 기본 설정 */
			add(XTMCONF + "SystemPlugin.setBasic", "시스템 > 기본 설정", "시간 설정 저장");
			add(XTMCONF + "SystemPlugin.syncTime", "시스템 > 기본 설정", "시간동기화");
			add(XTMCONF + "SystemPlugin.setSettingOption", "시스템 > 기본 설정", "옵션 저장");
			/* 라이센스 */
			add(XTMCONF + "SystemPlugin.setLicense", "시스템 > 라이센스", "온라인 인증");
			/* 정책 예약 전송 */
			add(XTMCONF + "SystemPlugin.setReservation", "시스템 > 정책 예약 전송", "설정 저장");
			/* snmp */
			add(XTMCONF + "SystemPlugin.setSnmp", "시스템 > snmp", "snmp 설정 저장");
			/* 업그레이드 */
			add(FRODO_CORE + "AccessGatewayPlugin.upgrade", "시스템 > 업그레이드", "업그레이드");
			add(XTMCONF + "RollbackPlugin.rollback", "시스템 > 업그레이드", "롤백");
			/* 시스템 상태 */
			add(FRODO_CORE + "AccessGatewayPlugin.backupConfig", "시스템 > 시스템상태", "백업");
			add(FRODO_CORE + "AccessGatewayPlugin.restoreConfig", "시스템 > 시스템상태", "복원");
			add(XTMCONF + "SystemPlugin.startService", "시스템 > 시스템상태", "서비스 시작");
			add(XTMCONF + "SystemPlugin.stopService", "시스템 > 시스템상태", "서비스 중단");
			add(FRODO_CORE + "AccessGatewayPlugin.halt", "시스템 > 시스템상태", "시스템 종료");
			add(FRODO_CORE + "AccessGatewayPlugin.reboot", "시스템 > 시스템상태", "시스템 재시작");
			add(XTMCONF + "IntegrityPlugin.checkIntegrity", "시스템 > 시스템상태", "무결성 검사");

			/** 네트워크 **/
			/* 인터페이스 설정 */
			add(XTMCONF + "NetworkPlugin.setInterface", "네트워크 > 인터페이스 설정", "인터페이스 설정 저장");
			add(XTMCONF + "NetworkPlugin.addBridge", "네트워크 > 인터페이스 설정", "브릿지 추가");
			add(XTMCONF + "NetworkPlugin.modifyBridge", "네트워크 > 인터페이스 설정", "브릿지 수정");
			add(XTMCONF + "NetworkPlugin.removeBridge", "네트워크 > 인터페이스 설정", "브릿지 삭제");
			add(XTMCONF + "NetworkPlugin.addBonding", "네트워크 > 인터페이스 설정", "본딩 추가");
			add(XTMCONF + "NetworkPlugin.modifyBonding", "네트워크 > 인터페이스 설정", "본딩 수정");
			add(XTMCONF + "NetworkPlugin.removeBonding", "네트워크 > 인터페이스 설정", "본딩 삭제");
			add(XTMCONF + "NetworkPlugin.addVlan", "네트워크 > 인터페이스 설정", "VLAN 추가");
			add(XTMCONF + "NetworkPlugin.modifyVlan", "네트워크 > 인터페이스 설정", "VLAN 수정");
			add(XTMCONF + "NetworkPlugin.removeVlan", "네트워크 > 인터페이스 설정", "VLAN 삭제");
			add(XTMCONF + "NetworkPlugin.addVirtualIp", "네트워크 > 인터페이스 설정", "가상 IPv4 추가");
			add(XTMCONF + "NetworkPlugin.modifyVirtualIp", "네트워크 > 인터페이스 설정", "가상 IPv4 수정");
			add(XTMCONF + "NetworkPlugin.removeVirtualIp", "네트워크 > 인터페이스 설정", "가상 IPv4 삭제");
			add(XTMCONF + "NetworkPlugin.addVirtualIpv6", "네트워크 > 인터페이스 설정", "가상 IPv6 생성");
			add(XTMCONF + "NetworkPlugin.modifyVirtualIpv6", "네트워크 > 인터페이스 설정", "가상 IPv6 수정");
			add(XTMCONF + "NetworkPlugin.removeVirtualIpv6", "네트워크 > 인터페이스 설정", "가상 IPv6 삭제");
			/* 외부 인증 설정 */
			add(XTMCONF + "NetworkPlugin.setRadius", "네트워크 > 외부 인증 설정", "외부 인증 설정 저장");
			/* DHCP서버 */
			add(XTMCONF + "NetworkPlugin.addDhcpServer", "네트워크 > DHCP서버", "DHCP 서버 추가");
			add(XTMCONF + "NetworkPlugin.modifyDhcpServer", "네트워크 > DHCP서버", "DHCP 서버 수정");
			add(XTMCONF + "NetworkPlugin.removeDhcpServer", "네트워크 > DHCP서버", "DHCP 서버 삭제");
			add(XTMCONF + "NetworkPlugin.setDhcpRelay", "네트워크 > DHCP서버", "DHCP 릴레이 여부 저장");
			/* DNS */
			add(XTMCONF + "NetworkPlugin.setDdns", "네트워크 > DNS", "다이나믹 DNS 서비스 사용 설정 저장");
			add(XTMCONF + "NetworkPlugin.setInnerDns", "네트워크 > DNS", "내부 DNS 설정 저장");
			add(XTMCONF + "NetworkPlugin.setOuterDns", "네트워크 > DNS", "외부 DNS 설정 저장");

			add(FRODO_CORE + "DnsProxyPlugin.getDnsZone", "네트워크 > DNS", "DNS 상세 보기");
			add(FRODO_CORE + "DnsProxyPlugin.createDnsZone", "네트워크 > DNS", "DNS 추가");
			add(FRODO_CORE + "DnsProxyPlugin.updateDnsZone", "네트워크 > DNS", "DNS 수정");
			add(FRODO_CORE + "DnsProxyPlugin.removeDnsZones", "네트워크 > DNS", "DNS 삭제");
			add(FRODO_CORE + "DnsProxyPlugin.importDnsZone", "네트워크 > DNS", "DNS CSV 일괄입력");

			/* IPv6 터널링 */
			add(XTMCONF + "NetworkPlugin.addSixInFour", "네트워크 > IPv6 터널링", "터널링 추가");
			add(XTMCONF + "NetworkPlugin.modifySixInFour", "네트워크 > IPv6 터널링", "터널링 수정");
			add(XTMCONF + "NetworkPlugin.removeSixInFour", "네트워크 > IPv6 터널링", "터널링 삭제");
			add(XTMCONF + "NetworkPlugin.setSixToFour", "네트워크 > IPv6 터널링", "6to4 사용 저장");
			/* 라우팅 */
			add(XTMCONF + "NetworkPlugin.addRouterStatic", "네트워크 > 라우팅", "Static 추가");
			add(XTMCONF + "NetworkPlugin.modifyRouterStatic", "네트워크 > 라우팅", "Static 수정");
			add(XTMCONF + "NetworkPlugin.removeRouterStatic", "네트워크 > 라우팅", "Static 삭제");
			add(XTMCONF + "NetworkPlugin.addRouterPolicy", "네트워크 > 라우팅", "정책 추가");
			add(XTMCONF + "NetworkPlugin.modifyRouterPolicy", "네트워크 > 라우팅", "정책 수정");
			add(XTMCONF + "NetworkPlugin.removeRouterPolicy", "네트워크 > 라우팅", "정책 삭제");
			add(XTMCONF + "NetworkPlugin.setRouterMulticast", "네트워크 > 라우팅", "멀티캐스트 저장");
			add(XTMCONF + "NetworkPlugin.addRouterVrrp", "네트워크 > 라우팅", "VRRP 추가");
			add(XTMCONF + "NetworkPlugin.modifyRouterVrrp", "네트워크 > 라우팅", "VRRP 수정");
			add(XTMCONF + "NetworkPlugin.removeRouterVrrp", "네트워크 > 라우팅", "VRRP 삭제");
			add(XTMCONF + "NetworkPlugin.addRouterChecker", "네트워크 > 라우팅", "CHECKER 추가");
			add(XTMCONF + "NetworkPlugin.modifyRouterChecker", "네트워크 > 라우팅", "CHECKER 수정");
			add(XTMCONF + "NetworkPlugin.removeRouterChecker", "네트워크 > 라우팅", "CHECKER 삭제");
			add(XTMCONF + "NetworkPlugin.setRouterScript", "네트워크 > 라우팅", "스크립트 저장");
			/* ARP 테이블 */
			/* L2TP 설정 */
			add(XTMCONF + "NetworkPlugin.setL2tpConfig", "네트워크 > L2TP 설정", "L2TP 서비스 사용 저장");

			/** HA **/
			/* SSL 클러스터 */
			add(FRODO_CORE + "ClusterPlugin.setClusterConfig", "HA > SSL 클러스터", "클러스터 사용 저장");
			add(FRODO_CORE + "ClusterPlugin.addClusterNode", "HA > SSL 클러스터", "클러스터 노드 추가");
			add(FRODO_CORE + "ClusterPlugin.removeClusterNode", "HA > SSL 클러스터", "클러스터 노드 삭제");
			/* 세션 백업 */
			add(XTMCONF + "HaPlugin.setSyncSessionBackup", "HA > 세션 백업", "세션백업 사용 저장");
			/* 정책 동기화 */
			add(XTMCONF + "HaPlugin.setSyncPolicySynchronize", "HA > 정책 동기화", "정책동기화 사용 저장");
			add(XTMCONF + "PolicySyncPlugin.sync", "HA > 정책 동기화", "정책동기화 사용 즉시실행");
			/* 본점 */
			add(XTMCONF + "HaPlugin.setHeadScript", "HA > 본점", "본점 스크립트 저장");
			/* 지점 */
			add(XTMCONF + "HaPlugin.setBranchScript", "HA > 지점", "지점 스크립스 저장");
			/* 물리적 링크 */
			add(XTMCONF + "HaPlugin.setLinkPackScript", "HA > 물리적 링크", "물리적 링크 스크립트 저장");

			/** 객체 **/
			/* IP주소 */
			add(XTMCONF + "ObjectPlugin.addIpAddress", "객체 > IP주소", "IPv4주소 추가");
			add(XTMCONF + "ObjectPlugin.modifyIpAddress", "객체 > IP주소", "IPv4주소 수정");
			add(XTMCONF + "ObjectPlugin.removeIpAddress", "객체 > IP주소", "IPv4주소 삭제");
			add(XTMCONF + "ObjectPlugin.addIpGroup", "객체 > IP주소", "IPv4그룹 추가 ");
			add(XTMCONF + "ObjectPlugin.modifyIpGroup", "객체 > IP주소", "Ipv4그룹 수정");
			add(XTMCONF + "ObjectPlugin.removeIpGroup", "객체 > IP주소", "IPv4그룹 삭제");
			add(XTMCONF + "ObjectPlugin.addIpv6Address", "객체 > IP주소", "IPv6주소 추가");
			add(XTMCONF + "ObjectPlugin.modifyIpv6Address", "객체 > IP주소", "IPv6주소 수정");
			add(XTMCONF + "ObjectPlugin.removeIpv6Address", "객체 > IP주소", "IPv6주소 삭제");
			add(XTMCONF + "ObjectPlugin.addIpv6Group", "객체 > IP주소", "IPv6그룹 추가");
			add(XTMCONF + "ObjectPlugin.modifyIpv6Group", "객체 > IP주소", "IPv6그룹 수정");
			add(XTMCONF + "ObjectPlugin.removeIpv6Group", "객체 > IP주소", "IPv6그룹 삭제");
			add(XTMCONF + "ObjectPlugin.addIpv6Header", "객체 > IP주소", "IPv6헤더 추가");
			add(XTMCONF + "ObjectPlugin.modifyIpv6Header", "객체 > IP주소", "IPv6헤더 수정");
			add(XTMCONF + "ObjectPlugin.removeIpv6Header", "객체 > IP주소", "IPv6헤더 삭제");
			/* 서비스 */
			add(XTMCONF + "ObjectPlugin.addServicePort", "객체 > 서비스", "서비스포트 추가");
			add(XTMCONF + "ObjectPlugin.modifyServicePort", "객체 > 서비스", "서비스포트 수정");
			add(XTMCONF + "ObjectPlugin.removeServicePort", "객체 > 서비스", "서비스포트 삭제");
			add(XTMCONF + "ObjectPlugin.addServiceGroup", "객체 > 서비스", "서비스그룹 추가");
			add(XTMCONF + "ObjectPlugin.modifyServiceGroup", "객체 > 서비스", "서비스그룹 수정");
			add(XTMCONF + "ObjectPlugin.modifyServiceGroup", "객체 > 서비스", "서비스그룹 삭제");
			/* 스케쥴 */
			add(XTMCONF + "ObjectPlugin.addSchedule", "객체 > 스케쥴", "스케쥴 추가");
			add(XTMCONF + "ObjectPlugin.modifySchedule", "객체 > 스케쥴", "스케쥴 수정");
			add(XTMCONF + "ObjectPlugin.removeSchedule", "객체 > 스케쥴", "스케쥴 삭제");
			/* 사용자 */
			add(KRAKEN_DOM + "OrganizationUnitPlugin.createOrganizationUnit", "객체 > 사용자", "부서 추가");
			add(KRAKEN_DOM + "OrganizationUnitPlugin.updateOrganizationUnit", "객체 > 사용자", "부서 수정");
			add(KRAKEN_DOM + "OrganizationUnitPlugin.removeOrganizationUnit", "객체 > 사용자", "부서 삭제");
			add(FRODO_CORE + "UserPlugin.setOrgUnitExtension", "객체 > 사용자", "액세스 정책 저장");
			add(KRAKEN_DOM + "UserPlugin.createUser", "객체 > 사용자", "사용자 추가");
			add(KRAKEN_DOM + "UserPlugin.updateUser", "객체 > 사용자", "사용자 수정");
			add(KRAKEN_DOM + "UserPlugin.removeUser", "객체 > 사용자", "사용자 삭제");
			add(KRAKEN_DOM + "UserPlugin.moveUsers", "객체 > 사용자", "부서 이동");
			add(KRAKEN_DOM + "UserPlugin.removeUsers", "객체 > 사용자", "사용자 일괄 삭제");
			add(KRAKEN_DOM + "UserPlugin.removeAllUsers", "객체 > 사용자", "모든 사용자 삭제");
			add(KRAKEN_LDAP + "LdapPlugin.sync", "객체 > 사용자", "LDAP 사용자 및 부서 동기화");
			add(FRODO_CORE + "UserPlugin.setForcePasswordChanges", "객체 > 사용자", "사용자 암호 변경 강제");
			add(FRODO_CORE + "UserPlugin.generateAuthKeys", "객체 > 사용자", "단말 등록키 발급");
			add(FRODO_CORE + "UserPlugin.removeAuthKeys", "객체 > 사용자", "단말 등록키 회수");
			add(FRODO_CORE + "UserPlugin.resetVids", "객체 > 사용자", "공인 인증서 일괄 초기화");
			add(KRAKEN_DOM + "AdminPlugin.setAdmin", "객체 > 사용자", "관리자 설정");
			add(KRAKEN_DOM + "AdminPlugin.unsetAdmin", "객체 > 사용자", "관리자 해제");
			add(FRODO_CORE + "UserPlugin.setUserExtension", "객체 > 사용자", "SSLVPN 저장");
			/* Qos */
			add(XTMCONF + "ObjectPlugin.addQos", "객체 > Qos", "QoS 추가");
			add(XTMCONF + "ObjectPlugin.modifyQos", "객체 > Qos", "QoS 수정");
			add(XTMCONF + "ObjectPlugin.removeQos", "객체 > Qos", "QoS 삭제");
			/* 세션 사용량 제한 */
			add(XTMCONF + "ObjectPlugin.addSession", "객체 > 세션 사용량 제한", "세션 사용량 제한 추가");
			add(XTMCONF + "ObjectPlugin.modifySession", "객체 > 세션 사용량 제한", "세션 사용량 제한 수정");
			add(XTMCONF + "ObjectPlugin.removeSession", "객체 > 세션 사용량 제한", "세션 사용량 제한 삭제");

			/** 필터링 **/
			/* IPv4 필터링 */
			add(XTMCONF + "FilteringPlugin.addFirewallPolicy", "필터링 > IPv4 필터링", "IPv4 필터링 추가");
			add(XTMCONF + "FilteringPlugin.modifyFirewallPolicy", "필터링 > IPv4 필터링", "IPv4 필터링 수정");
			add(XTMCONF + "FilteringPlugin.removeFirewallPolicy", "필터링 > IPv4 필터링", "IPv4 필터링 삭제");
			add(XTMCONF + "FilteringPlugin.addVlanSetting", "필터링 > IPv4 필터링", "VLAN 필터링 추가");
			add(XTMCONF + "FilteringPlugin.modifyVlanSetting", "필터링 > IPv4 필터링", "VLAN 필터링 수정");
			add(XTMCONF + "FilteringPlugin.removeVlanSetting", "필터링 > IPv4 필터링", "VLAN 필터링 삭제");
			/* IPv4 주소변환 */
			add(XTMCONF + "FilteringPlugin.addFirewallNat", "필터링 > IPv4 주소변환", "IPv4 주소변환 추가");
			add(XTMCONF + "FilteringPlugin.modifyFirewallNat", "필터링 > IPv4 주소변환", "IPv4 주소변환 수정");
			add(XTMCONF + "FilteringPlugin.removeFirewallNat", "필터링 > IPv4 주소변환", "IPv4 주소변환 삭제");
			/* IPv6 필터링 */
			add(XTMCONF + "FilteringPlugin.addFirewallIpv6", "필터링 > IPv6 필터링", "IPv6 필터링 추가");
			add(XTMCONF + "FilteringPlugin.modifyFirewallIpv6", "필터링 > IPv6 필터링", "IPv6 필터링 수정");
			add(XTMCONF + "FilteringPlugin.removeFirewallIpv6", "필터링 > IPv6 필터링", "IPv6 필터링 삭제");
			/* IPv6-v4 주소 변환 */
			add(XTMCONF + "FilteringPlugin.addFirewallIpv6Nat", "필터링 > IPv6-v4 주소 변환", "IPv6-v4 주소 변환 추가");
			add(XTMCONF + "FilteringPlugin.modifyFirewallIpv6Nat", "필터링 > IPv6-v4 주소 변환", "IPv6-v4 주소 변환 수정");
			add(XTMCONF + "FilteringPlugin.removeFirewallIpv6Nat", "필터링 > IPv6-v4 주소 변환", "IPv6-v4 주소 변환 삭제");
			add(XTMCONF + "FilteringPlugin.setFirewallIpv6NatPrefix", "필터링 > IPv6-v4 주소 변환", "IPv6-v4 주소 변환 PREFIX 저장");

			/** VPN **/
			/* SSLVPN 설정 */
			add(FRODO_CORE + "AccessGatewayPlugin.setSSLConfig", "VPN > SSLVPN 설정", "SSLVPN 설정 저장");
			/* SQL 연동 설정 */
			add(FRODO_CORE + "AccessGatewayPlugin.testDbConnection", "VPN > SSLVPN 설정 > SQL 연동 설정", "SQL 연동 설정 접속 테스트");
			add(FRODO_CORE + "AccessGatewayPlugin.testUserSql", "VPN > SSLVPN 설정 > SQL 연동 설정",
					"SQL 연동 설정 사용자 검증 쿼리 테스트");
			add(FRODO_CORE + "AccessGatewayPlugin.testAuthSql", "VPN > SSLVPN 설정 > SQL 연동 설정", "SQL 연동 설정 암호 검증 쿼리 테스트");
			add(FRODO_CORE + "AccessGatewayPlugin.testIdnSql", "VPN > SSLVPN 설정 > SQL 연동 설정",
					"SQL 연동 설정 주민등록번호 조회 쿼리 테스트");
			add(FRODO_CORE + "AccessGatewayPlugin.testPasswordExpirySql", "VPN > SSLVPN 설정 > SQL 연동 설정",
					"SQL 연동 설정 암호 만료 시각 쿼리 테스트");
			add(FRODO_CORE + "AccessGatewayPlugin.testSaltSql", "VPN > SSLVPN 설정 > SQL 연동 설정",
					"SQL 연동 설정 SALT 조회 쿼리 테스트");
			add(FRODO_CORE + "AccessGatewayPlugin.testSubjectDnSql", "VPN > SSLVPN 설정 > SQL 연동 설정",
					"SQL 연동 설정 인증 주체 DN 쿼리 테스트");
			/* IPSEC 기본설정 */
			add(XTMCONF + "IpsecPlugin.setVpnSetting", "VPN > IPSEC 기본설정", "인터페이스 저장");
			add(XTMCONF + "IpsecPlugin.setVpnScript", "VPN > IPSEC 기본설정", "본점/지점/기타 저장");
			/* IPSEC 보안설정 */
			add(XTMCONF + "IpsecPlugin.addVpnIpsecsa", "VPN > IPSEC 보안설정", "IPSEC SA 추가");
			add(XTMCONF + "IpsecPlugin.modifyVpnIpsecsa", "VPN > IPSEC 보안설정", "IPSEC SA  수정");
			add(XTMCONF + "IpsecPlugin.removeVpnIpsecsa", "VPN > IPSEC 보안설정", "IPSEC SA  삭제");
			add(XTMCONF + "IpsecPlugin.addVpnIsakmpsa", "VPN > IPSEC 보안설정", "ISAKMP SA 추가");
			add(XTMCONF + "IpsecPlugin.modifyVpnIsakmpsa", "VPN > IPSEC 보안설정", "ISAKMP SA 수정");
			add(XTMCONF + "IpsecPlugin.removeVpnIsakmpsa", "VPN > IPSEC 보안설정", "ISAKMP SA 삭제");
			add(XTMCONF + "IpsecPlugin.addVpnHost", "VPN > IPSEC 보안설정", "보안 호스트 추가");
			add(XTMCONF + "IpsecPlugin.modifyVpnHost", "VPN > IPSEC 보안설정", "보안 호스트 수정");
			add(XTMCONF + "IpsecPlugin.removeVpnHost", "VPN > IPSEC 보안설정", "보안 호스트 삭제");
			add(XTMCONF + "IpsecPlugin.addVpnIpsec", "VPN > IPSEC 보안설정", "IPSEC 대상 추가 ");
			add(XTMCONF + "IpsecPlugin.modifyVpnIpsec", "VPN > IPSEC 보안설정", "IPSEC 대상 수정");
			add(XTMCONF + "IpsecPlugin.removeVpnIpsec", "VPN > IPSEC 보안설정", "IPSEC 대상 삭제");
			/* 서버 */
			add(FRODO_CORE + "ServerPlugin.createServer", "VPN > 서버", "서버 추가");
			add(FRODO_CORE + "ServerPlugin.updateServer", "VPN > 서버", "서버 수정");
			add(FRODO_CORE + "ServerPlugin.removeServer", "VPN > 서버", "서버 삭제");
			add(FRODO_CORE + "ServerPlugin.removeServers", "VPN > 서버", "서버 삭제");
			/* NAC정책 */
			add(FRODO_CORE + "ClientCheckPlugin.createProfile", "VPN > NAC정책", "NAC 정책 추가");
			add(FRODO_CORE + "ClientCheckPlugin.updateProfile", "VPN > NAC정책", "NAC 정책 수정");
			add(FRODO_CORE + "ClientCheckPlugin.removeProfile", "VPN > NAC정책", "NAC 정책 삭제");
			add(FRODO_CORE + "ClientCheckPlugin.removeProfiles", "VPN > NAC정책", "NAC 정책 삭제");
			/* 액세스정책 */
			add(FRODO_CORE + "AccessProfilePlugin.createProfile", "VPN > 액세스정책", "액세스정책 추가");
			add(FRODO_CORE + "AccessProfilePlugin.updateProfile", "VPN > 액세스정책", "액세스정책 수정");
			add(FRODO_CORE + "AccessProfilePlugin.removeProfile", "VPN > 액세스정책", "액세스정책 삭제");
			add(FRODO_CORE + "AccessProfilePlugin.removeProfiles", "VPN > 액세스정책", "액세스정책 삭제");
			/* 단말관리 */
			add(FRODO_CORE + "AuthorizedDevicePlugin.removeAndGenerateKey", "VPN > 단말관리", "단말 교체");
			add(FRODO_CORE + "AuthorizedDevicePlugin.removeDevice", "VPN > 단말관리", "단말 삭제");
			add(FRODO_CORE + "AuthorizedDevicePlugin.removeDevices", "VPN > 단말관리", "단말 삭제");
			add(FRODO_CORE + "AuthorizedDevicePlugin.setExpiration", "VPN > 단말관리", "만료 시간 설정");
			add(FRODO_CORE + "AuthorizedDevicePlugin.blockDevice", "VPN > 단말관리", "단말 차단");
			/* 인증센터 */

			/** 관리 **/
			/* 로그 */
			add(XTMCONF + "ManagePlugin.setLogSetting", "관리 > 로그", "로그 설정 저장");
			add(FRODO_CORE + "AccessGatewayPlugin.updateLogStorageSetting", "관리 > 로그", "로그 설정 저장");
			add(XTMCONF + "ManagePlugin.addSyslogSettingStandard", "관리 > 로그", "Syslog 설정 추가");
			add(XTMCONF + "ManagePlugin.modifySyslogSettingStandard", "관리 > 로그", "Syslog 설정 수정");
			add(XTMCONF + "ManagePlugin.removeSyslogSettingStandard", "관리 > 로그", "Syslog 설정 삭제");
			add(XTMCONF + "ManagePlugin.setAlarmSetting", "관리 > 로그", "알람 설정 저장");
			/* 방화벽 차단 진단 */
			add(FRODO_CORE + "DnsCheckPlugin.createDnsCheck", "관리 > 방화벽 차단 진단", "방화벽 차단 진단 추가");
			add(FRODO_CORE + "DnsCheckPlugin.udpateDnsCheck", "관리 > 방화벽 차단 진단", "방화벽 차단 진단 수정");
			add(FRODO_CORE + "DnsCheckPlugin.removeDnsChecks", "관리 > 방화벽 차단 진단", "방화벽 차단 진단 삭제");

			/** 객체 관리자 **/
			/* 파일브라우저 */
			add(KRAKEN_DOM + "FileUploadPlugin.createFileSpace", "파일브라우저", "폴더추가");
			add(KRAKEN_DOM + "FileUploadPlugin.setUploadToken", "파일브라우저", "업로드");
			add(KRAKEN_DOM + "FileUploadPlugin.deleteFiles", "파일브라우저", "선택삭제");
			/* 스케쥴 */
			add(KRAKEN_DOM + "TimetablePlugin.createTimetable", "스케쥴", "스케쥴추가");
			add(KRAKEN_DOM + "TimetablePlugin.updateTimetable", "스케쥴", "스케쥴수정");
			add(KRAKEN_DOM + "TimetablePlugin.removeTimetable", "스케쥴", "스케쥴삭제");
			add(KRAKEN_LDAP + "LdapPlugin.createProfile", "LDAP 설정", "프로파일 추가");
			add(KRAKEN_LDAP + "LdapPlugin.updateProfile", "LDAP 설정", "프로파일 수정");
			add(KRAKEN_LDAP + "LdapPlugin.removeProfile", "LDAP 설정", "프로파일 삭제");
			add(KRAKEN_LDAP + "LdapPlugin.removeProfiles", "LDAP 설정", "프로파일 삭제");

			/** 정책전송 **/
			add(XTMCONF + "SystemPlugin.deployPolicy", "정책전송", "정책전송");
			add(FRODO_CORE + "AccessGatewayPlugin.deployPolicy", "정책전송", "정책전송");

			/** csv import **/
			add(FRODO_CORE + "kr.co.future.frodo.core.msgbus.ImportCsvPlugin.importServers", "VPN > 서버 ", "csv 입력");
			add(FRODO_CORE + "kr.co.future.frodo.core.msgbus.ImportCsvPlugin.importAccessProfiles", "VPN > 액세스 정책", "csv 입력");
			add(FRODO_CORE + "kr.co.future.frodo.core.msgbus.ImportCsvPlugin.importAuthorizedDevices", "VPN > 단말관리", "csv 입력");
			
			/** 관리자 조회 **/
			add(FRODO_CORE + "kr.co.future.frodo.core.msgbus.UserPlugin.getAdminUsers", "조직도", "관리자 조회");
			add(FRODO_CORE + "kr.co.future.frodo.core.msgbus.UserPlugin.getAdminUserExtensions", "객체 > 사용자", "관리자 조회");
		}

		private void add(String method, String menu, String operation) {
			methodMenuMappings.put(method, menu);
			methodOperationMappings.put(method, operation);
		}

		@Override
		public Object lookup(String srcField, String dstField, Object value) {
			if (srcField.equals("method") && dstField.equals("operation")) {
				if (value == null)
					return null;
				return getOperationDesc(value.toString());
			} else if (srcField.equals("method") && dstField.equals("menu")) {
				if (value == null)
					return null;
				return getmenuDesc(value.toString());
			}

			return null;
		}

		private String getOperationDesc(String value) {
			String translated = methodOperationMappings.get(value);
			if (translated != null)
				return translated;
			return "알 수 없음";
		}

		private String getmenuDesc(String value) {
			String translated = methodMenuMappings.get(value);
			if (translated != null)
				return translated;
			return "알 수 없음";
		}

	}
}
