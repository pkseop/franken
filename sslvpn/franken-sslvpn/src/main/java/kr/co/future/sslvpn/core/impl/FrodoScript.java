package kr.co.future.sslvpn.core.impl;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigInteger;
import java.net.InetAddress;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import kr.co.future.api.BundleManager;
import kr.co.future.api.Script;
import kr.co.future.api.ScriptArgument;
import kr.co.future.api.ScriptContext;
import kr.co.future.api.ScriptUsage;
import kr.co.future.confdb.ConfigService;
import kr.co.future.dom.api.UserApi;
import kr.co.future.dom.model.User;
import kr.co.future.httpd.BundleResourceServlet;
import kr.co.future.httpd.HttpContext;
import kr.co.future.httpd.HttpService;
import kr.co.future.ldap.LdapProfile;
import kr.co.future.ldap.LdapProfile.CertificateType;
import kr.co.future.ldap.LdapService;
import kr.co.future.radius.client.RadiusClient;
import kr.co.future.radius.client.auth.Authenticator;
import kr.co.future.radius.client.auth.ChapAuthenticator;
import kr.co.future.radius.client.auth.PapAuthenticator;
import kr.co.future.radius.protocol.AccessAccept;
import kr.co.future.radius.protocol.RadiusPacket;
import kr.co.future.sslvpn.core.AlertCategory;
import kr.co.future.sslvpn.core.AuthCode;
import kr.co.future.sslvpn.core.AuthService;
import kr.co.future.sslvpn.core.CrlValidator;
import kr.co.future.sslvpn.core.DashboardService;
import kr.co.future.sslvpn.core.ExternalAuthConfig;
import kr.co.future.sslvpn.core.ExternalAuthService;
import kr.co.future.sslvpn.core.GlobalConfigApi;
import kr.co.future.sslvpn.core.ImportCsv;
import kr.co.future.sslvpn.core.InstallMonitor;
import kr.co.future.sslvpn.core.InstallerApi;
import kr.co.future.sslvpn.core.NetworkService;
import kr.co.future.sslvpn.core.TestCrl;
import kr.co.future.sslvpn.core.TestCrlApi;
import kr.co.future.sslvpn.model.AccessGateway;
import kr.co.future.sslvpn.model.AuthorizedDevice;
import kr.co.future.sslvpn.model.IpLease;
import kr.co.future.sslvpn.model.SSLConfig;
import kr.co.future.sslvpn.model.SplitRoutingEntry;
import kr.co.future.sslvpn.model.UserExtension;
import kr.co.future.sslvpn.model.api.AccessGatewayApi;
import kr.co.future.sslvpn.model.api.AuthorizedDeviceApi;
import kr.co.future.sslvpn.model.api.ClientAppApi;
import kr.co.future.sslvpn.model.api.IpLeaseApi;
import kr.co.future.sslvpn.xtmconf.XtmConfig;
import kr.co.future.sslvpn.xtmconf.network.Radius;
import kr.co.future.sslvpn.xtmconf.system.License;

import org.apache.commons.lang.StringUtils;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;

public class FrodoScript implements Script {

	private ScriptContext context;
	private BundleContext bc;
	private ConfigService conf;
	private AuthService auth;
	private IpLeaseApi leaseApi;
	private DashboardService dash;
	private HttpService httpd;
	private ExternalAuthService externalAuth;
	private LdapService ldap;
	private UserApi domUserApi;
	private AccessGatewayApi gatewayApi;
	private TestCrlApi testCrlApi;
	private ClientAppApi clientAppApi;
	private GlobalConfigApi configApi;
	private kr.co.future.sslvpn.model.api.UserApi userApi;
	private final BundleManager bundleManager;
	private InstallerApi installer;
	private ImportCsv importCsv;
	private AuthorizedDeviceApi authDeviceApi;

	public FrodoScript(BundleContext bc, ConfigService conf, AuthService auth, IpLeaseApi leaseApi, DashboardService dash,
			HttpService httpd, ExternalAuthService externalAuth, LdapService ldap, UserApi domUserApi,
			AccessGatewayApi gatewayApi, TestCrlApi testCrlApi, ClientAppApi clientAppApi, GlobalConfigApi configApi,
			kr.co.future.sslvpn.model.api.UserApi userApi, BundleManager bundleManager, InstallerApi installer, ImportCsv importCsv,
			AuthorizedDeviceApi authDeviceApi) {
		this.bc = bc;
		this.conf = conf;
		this.auth = auth;
		this.leaseApi = leaseApi;
		this.dash = dash;
		this.httpd = httpd;
		this.externalAuth = externalAuth;
		this.ldap = ldap;
		this.domUserApi = domUserApi;
		this.gatewayApi = gatewayApi;
		this.testCrlApi = testCrlApi;
		this.clientAppApi = clientAppApi;
		this.configApi = configApi;
		this.userApi = userApi;
		this.bundleManager = bundleManager;
		this.installer = installer;
		this.importCsv = importCsv;
		this.authDeviceApi = authDeviceApi;
	}

	@ScriptUsage(description = "set subjectDN charset", arguments = { @ScriptArgument(name = "charset", type = "string", description = "charset") })
	public void setSqlSubjectDnCharset(String[] args) {
		SSLConfig c = transAccessGateway();
		String charset = args[0];
		c.setSubjectDnCharset(charset);

		gatewayApi.setSSLConfig(c);
		context.println("set");
	}

	@ScriptUsage(description = "set subjectDN encoding", arguments = { @ScriptArgument(name = "encoding", type = "string", description = "hex or base64") })
	public void setSqlSubjectDnEncoding(String[] args) {
		SSLConfig c = transAccessGateway();
		String encoding = args[0];
		c.setSubjectDnEncoding(encoding);

		gatewayApi.setSSLConfig(c);
		context.println("set");
	}

	@ScriptUsage(description = "set password expired", arguments = {
			@ScriptArgument(name = "login name", type = "string", description = "login name"),
			@ScriptArgument(name = "date ( yyyy-MM-dd ) ", type = "string", description = "date") })
	public void setPasswordExpireTime(String[] args) {
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");

		String loginName = args[0];
		String StringDate = args[1];

		Date expiredDate = null;
		try {
			expiredDate = sdf.parse(StringDate);
		} catch (ParseException e) {
			context.println("invalid date format");
			return;
		}

		User user = domUserApi.findUser("localhost", loginName);

		if (user == null) {
			context.println("no user");
		}

		UserExtension ext = userApi.getUserExtension(user);

		if (ext == null) {
			ext = new UserExtension();
			ext.setUser(user);
		}

		ext.setLastPasswordChange(expiredDate);
		userApi.setUserExtension(ext);
		context.println("set");

	}

	@ScriptUsage(description = "show version information")
	public void version(String[] args) {
		String xenicsVer = getXenicsVer();
		WindowsClientVersion clientVer = getWindowsClientVer();

		context.printf("xenics          : %s\n", xenicsVer);
		context.printf("Windows client  : %s, %s\n", clientVer.revision(), clientVer.buildNumber());
		context.printf("ActiveX website : %s\n", clientVer.webRevision());
		context.printf("Linux client    : information not supported\n");
		context.printf("Android client  : information not supported\n");
		context.printf("iOS client      : information not supported\n");
	}

	private WindowsClientVersion getWindowsClientVer() {
		WindowsClientVersion wcv = new WindowsClientVersion(bc, bundleManager, configApi);
		wcv.load();
		return wcv;
	}

	private String getXenicsVer() {
		BufferedReader reader = null;
		try {
			reader = new BufferedReader(new InputStreamReader(new FileInputStream("/usr/sslplus/xenics/version")));

			List<String> lines = new ArrayList<String>();

			while (true) {
				String line = reader.readLine();
				if (line == null)
					break;
				lines.add(line);
			}

			return StringUtils.join(lines.toArray(new String[0]), " ");
		} catch (FileNotFoundException e) {
			return "not supported on this machine";
		} catch (IOException e) {
			return "not supported on this machine";
		} finally {
			safeClose(reader);
		}
	}

	private void safeClose(Closeable reader) {
		if (reader == null)
			return;
		try {
			reader.close();
		} catch (IOException e) {
		}
	}

	@Override
	public void setScriptContext(ScriptContext context) {
		this.context = context;
	}

	public void externalAuth(String[] args) {
		ExternalAuthConfig c = externalAuth.getConfig();
		if (c == null) {
			context.println("external auth config not set");
			return;
		}

		context.println(c);
	}

	@ScriptUsage(description = "get all test crls")
	public void getTestCrls(String[] args) {
		context.println("test crls");
		context.println("--------------");
		for (TestCrl c : testCrlApi.getTestCrls()) {
			context.println(c.toString());
		}
	}

	@ScriptUsage(description = "migrate client apps")
	public void migrateApps(String[] args) {
		clientAppApi.migrateClientApps();
		context.println("completed");
	}

	@ScriptUsage(description = "test crl\n\tex) frodo.testCrl ldap://ds.yessign.or.kr:389/ou=dp4p3976,ou=AccreditedCA,o=yessign,c=kr?certificateRevocationList 272105169", arguments = {
			@ScriptArgument(name = "test url", type = "string", description = "url"),
			@ScriptArgument(name = "serial", type = "string", description = "serial") })
	public void testCrl(String[] args) {
		String url = args[0];
		String serial = args[1];

		try {
			boolean isRevoked = CrlValidator.isRevoked(url, new BigInteger(serial));
			context.println("revoked: " + isRevoked);
		} catch (Exception e) {
			context.println(e.getMessage());
		}

	}

	@ScriptUsage(description = "set trace on/off", arguments = { @ScriptArgument(name = "ues trace", type = "boolean", description = "true/false") })
	public void setCrlTestTrace(String[] args) {
		boolean useTrace = Boolean.parseBoolean(args[0]);
		testCrlApi.setTrace(useTrace);
		context.println("trace " + (useTrace ? "on" : "off"));
	}

	@ScriptUsage(description = "add test CRL", arguments = {
			@ScriptArgument(name = "alias", type = "string", description = "alias"),
			@ScriptArgument(name = "url", type = "string", description = "url"),
			@ScriptArgument(name = "serial", type = "string", description = "serial") })
	public void addTestCrl(String[] args) {
		TestCrl crl = new TestCrl();
		crl.setAlias(args[0]);
		crl.setUrl(args[1]);
		crl.setSerial(args[2]);
		testCrlApi.addTestCrl(crl);
		context.println("test crl [" + args[0] + "] added");
	}

	@ScriptUsage(description = "remove test CRL", arguments = { @ScriptArgument(name = "alias", type = "string", description = "alias") })
	public void removeTestCrl(String[] args) {
		testCrlApi.removeTestCrl(args[0]);
		context.println("test crl [" + args[0] + "] removed");
	}

	@ScriptUsage(description = "start CRL check", arguments = { @ScriptArgument(name = "seconds", type = "integer", description = "seconds of cycle") })
	public void startCrlTest(String[] args) {
		int seconds = Integer.parseInt(args[0]);
		testCrlApi.start(seconds);
		context.println("CRL checker started");
	}

	@ScriptUsage(description = "stop CRL check")
	public void stopCrlTest(String[] args) {
		testCrlApi.stop();
		context.println("CRL checker stopped");
	}

	@ScriptUsage(description = "check RADIUS connection", arguments = {
			@ScriptArgument(name = "test account", type = "string", description = "test account"),
			@ScriptArgument(name = "test password", type = "string", description = "test password") })
	public void checkRadius(String[] args) {
		String account = args[0];
		String password = args[1];

		try {
			List<Radius> radiusSettings = XtmConfig.readConfig(Radius.class);
			Radius radius = null;
			for (Radius r : radiusSettings)
				if (r.getType() == Radius.Type.Radius)
					radius = r;

			InetAddress addr = InetAddress.getByName(radius.getRadiusIp());
			int port = radius.getAuthPort();
			String sharedSecret = radius.getRadiusPassword();
			String authMethod = radius.getAuthMethod().toString();
			if (authMethod == null)
				authMethod = "PAP";

			RadiusClient client = new RadiusClient(addr, port, sharedSecret);

			Authenticator auth = null;
			if (authMethod.equals("PAP"))
				auth = new PapAuthenticator(client, account, password);
			else
				auth = new ChapAuthenticator(client, account, password);

			RadiusPacket response = client.authenticate(auth);
			if (response instanceof AccessAccept) {
				context.println("RADIUS Connection Success");
			} else {
				context.println("RADIUS Connection Fail");
			}
		} catch (Exception e) {
			context.println("RADIUS Connection Fail: " + e.getMessage());
		}
	}

	@ScriptUsage(description = "check LDAP connection", arguments = {
			@ScriptArgument(name = "test account", type = "string", description = "test account"),
			@ScriptArgument(name = "test password", type = "string", description = "test password") })
	public void checkLdap(String[] args) {
		Radius radius = null;
		List<Radius> radiusSettings = XtmConfig.readConfig(Radius.class);
		for (Radius r : radiusSettings)
			if (r.getType() == Radius.Type.Domain)
				radius = r;

		try {
			LdapProfile profile = new LdapProfile();
			profile.setName("frodo-test");
			profile.setDc(radius.getLdapAddress());
			profile.setAccount(radius.getLdapAccount());
			profile.setPassword(radius.getLdapPassword());
			if (radius.isLdapUseTrustStore() && radius.getLdapTrustStore() != null)
				profile.setTrustStore(CertificateType.X509, radius.getLdapTrustStore());

			boolean ret = ldap.verifyPassword(profile, args[0], args[1], 5000);
			if (ret)
				context.println("AD / LDAP Connection Success");
			else
				context.println("AD / LDAP Connection Fail");
		} catch (Exception e) {
			context.println("AD / LDAP Connection Fail: " + e.getMessage());
		}
	}

	@ScriptUsage(description = "set external auth", arguments = { @ScriptArgument(name = "enabled", type = "string", description = "true or false") })
	public void setExternalAuth(String[] args) {
		ExternalAuthConfig c = new ExternalAuthConfig();
		c.setEnabled(Boolean.parseBoolean(args[0]));
		externalAuth.setConfig(c);
		context.println("set");
	}

	public void issueLicense(String[] args) throws IOException, InterruptedException {
		try {
			String authKey = License.receive();
			if (authKey.length() == 120) {
				context.println("auth key: " + authKey);
				License.write(authKey);
				context.println("validated");
			} else {
				context.println(authKey);
			}
		} catch (Exception e) {
			context.println(e.toString());
		}
	}

	public void install(String[] args) {
		try {
			installer.install(new ScriptInstallMonitor());
		} catch (Exception e) {
			context.println(e.toString());
		}
	}

	private class ScriptInstallMonitor implements InstallMonitor {
		@Override
		public void println(Object value) {
			context.println(value);
		}
	}

	public void startweb(String[] args) {
		Bundle b = findUiBundle();
		if (b != null) {
			HttpContext ctx = httpd.ensureContext("frodo");
			ctx.addServlet("console", new BundleResourceServlet(b, "/WEB-INF"), "/admin/*");
			context.println("web ui loaded");
		} else
			context.println("webui bundle not found");
	}

	private Bundle findUiBundle() {
		for (Bundle b : bc.getBundles())
			if (b.getSymbolicName().equals("kr.co.future.watchcat.ui"))
				return b;

		return null;
	}

	public void dashboard(String[] args) {
		for (NetworkService ns : NetworkService.values()) {
			context.println(ns + ": " + dash.getServiceStatus(ns));
		}

		for (AlertCategory category : AlertCategory.values()) {
			context.println(category + ": " + dash.getAlert(category));
		}
	}

	@ScriptUsage(description = "", arguments = { @ScriptArgument(name = "id", type = "int", description = "tunnel id") })
	public void kill(String[] args) {
		auth.killTunnel(Integer.valueOf(args[0]));
	}

//	public void sync(String[] args) {
//		try {
//			auth.syncTunnel();
//			context.println("sync requested");
//		} catch (Exception e) {
//			context.println(e.toString());
//		}
//	}

	public void deploy(String[] args) {
		try {
			auth.deployPolicy();
			context.println("completed");
		} catch (Exception e) {
			context.println(e.getMessage());
		}
	}

//	public void tunnels(String[] args) {
//		context.println("Tunnels");
//		context.println("---------");
//
//		for (Tunnel t : auth.getTunnels()) {
//			context.println(t);
//		}
//	}

	@ScriptUsage(description = "authenticate", arguments = {
			@ScriptArgument(name = "tunnel", type = "int", description = "tunnel id"),
			@ScriptArgument(name = "id", type = "string", description = "id"),
			@ScriptArgument(name = "pw", type = "string", description = "password") })
	public void login(String[] args) {
		Map<String, Object> props = new HashMap<String, Object>();
		props.put("tunnel", Integer.valueOf(args[0]));
		props.put("id", args[1]);
		props.put("pw", args[2]);
		props.put("remote_ip", "127.0.0.1");
		props.put("remote_port", 23812);
		props.put("issuer_cn", "sslplus-ca");

		long begin = new Date().getTime();
		Map<String, Object> result = auth.login(props);
		long duration = new Date().getTime() - begin;

		for (String key : result.keySet()) {
			Object value = result.get(key);
			context.println(key + "=" + value);
			if (key.equals("auth_code"))
				context.println("auth_msg=" + AuthCode.parse((Integer) value));
		}

		context.println(duration + "ms elapsed");
	}

	@ScriptUsage(description = "pseudo logout tunnel", arguments = { @ScriptArgument(name = "tunnel id", type = "int", description = "tunnel id") })
	public void logout(String[] args) {
		int tunnelId = Integer.valueOf(args[0]);
		auth.logout(tunnelId);
		context.println("disconnected tunnel " + tunnelId);
	}

	@ScriptUsage(description = "list all ip leases", arguments = { @ScriptArgument(name = "login name", type = "int", description = "filtering login name", optional = true) })
	public void leases(String[] args) {
		List<IpLease> leases = null;
		if (args.length > 0)
			leases = leaseApi.getLeases(args[0]);
		else
			leases = leaseApi.getAllLeases();

		context.println("IP Leases");
		context.println("------------");
		for (IpLease l : leases)
			context.println(l);
	}

	/**
	 * setUserSql setAuthSql setIdnSql
	 */

	@ScriptUsage(description = "sql authenticate", arguments = {
			@ScriptArgument(name = "db account", type = "string", description = "database account"),
			@ScriptArgument(name = "db password", type = "string", description = "database password"),
			@ScriptArgument(name = "db connection String", type = "string", description = "database connection string"),
			@ScriptArgument(name = "enabled", type = "string", description = "true or false") })
	public void setSqlAuth(String[] args) {
		SSLConfig c = transAccessGateway();

		c.setUseSqlAuth(Boolean.getBoolean(args[3]));
		c.setDbAccount(args[0]);
		c.setDbPassword(args[1]);
		c.setDbConnectionString(args[2]);

		gatewayApi.setSSLConfig(c);

		context.println("set");
	}

	public void sqlAuth(String[] args) {
		AccessGateway gw = gatewayApi.getCurrentAccessGateway();
		if (gw == null) {
			context.println("sql auth config not set");
			return;
		}

		context.println("dbAccount=" + gw.getDbAccount() + ", dbPassword=" + gw.getDbPassword() + ", dbConnectionString="
				+ gw.getDbConnectionString() + ", enabled=" + String.valueOf(gw.isUseSqlAuth()));
		context.println("userSql=" + gw.getUserSql());
		context.println("authSql=" + gw.getAuthSql());
		context.println("idnSql=" + gw.getIdnSql());
	}

	@ScriptUsage(description = "set User verify Sql", arguments = { @ScriptArgument(name = "sql", type = "string", description = "userSql") })
	public void setUserSql(String[] args) {
		SSLConfig c = transAccessGateway();

		c.setUserSql(args[0]);

		gatewayApi.setSSLConfig(c);

		context.println("set");
	}

	@ScriptUsage(description = "set Auth Sql", arguments = { @ScriptArgument(name = "sql", type = "string", description = "authSql") })
	public void setAuthSql(String[] args) {
		SSLConfig c = transAccessGateway();

		c.setAuthSql(args[0]);

		gatewayApi.setSSLConfig(c);

		context.println("set");
	}

	@ScriptUsage(description = "set password expiry Sql", arguments = { @ScriptArgument(name = "sql", type = "string", description = "authSql") })
	public void setPasswordExpirySql(String[] args) {
		SSLConfig c = transAccessGateway();

		c.setPasswordExpirySql(args[0]);

		gatewayApi.setSSLConfig(c);

		context.println("set");
	}

	@ScriptUsage(description = "set Idn verify Sql", arguments = { @ScriptArgument(name = "sql", type = "string", description = "idnSql") })
	public void setIdnSql(String[] args) {
		SSLConfig c = transAccessGateway();
		c.setIdnSql(args[0]);
		gatewayApi.setSSLConfig(c);

		context.println("set");
	}

	private SSLConfig transAccessGateway() {
		AccessGateway gw = gatewayApi.getCurrentAccessGateway();
		SSLConfig c = new SSLConfig();

		c.setProtocol(gw.getProtocol());
		c.setLoginMethod(gw.getLoginMethod());
		c.setUseDeviceAuth(gw.isUseDeviceAuth());
		c.setUseRadius(gw.isUseRadiusAuth());
		c.setUseLdap(gw.isUseLdapAuth());
		c.setRadiusOrgUnitId(gw.getRadiusOrgUnitId());

		c.setUseSplitRouting(gw.isUseSplitRouting());

		c.getSplitRoutingEntries().clear();

		for (SplitRoutingEntry e : gw.getSplitRoutingEntries()) {
			c.getSplitRoutingEntries().add(e);
		}

		c.setNotice(gw.getNotice());
		c.setLnkName(gw.getLnkName());

		c.setUseSqlAuth(gw.isUseSqlAuth());
		c.setUserSql(gw.getUserSql());
		c.setAuthSql(gw.getAuthSql());
		c.setIdnSql(gw.getIdnSql());
		c.setDbAccount(gw.getDbAccount());
		c.setDbPassword(gw.getDbPassword());
		c.setDbConnectionString(gw.getDbConnectionString());
		c.setDeviceAuthMode(gw.getDeviceAuthMode().toString());
		c.setPasswordExpirySql(gw.getPasswordExpirySql());
		c.setPasswordHashType(gw.getPasswordHashType());
		c.setSubjectDnCharset(gw.getSubjectDnCharset());
		c.setSubjectDnEncoding(gw.getSubjectDnEncoding());

		return c;
	}
			
	@ScriptUsage(description = "initaialize all user's last used ip", arguments = { @ScriptArgument(name = "init", type = "boolean", description = "init last ip info of whole users") })
	public void initWholeUsersLastIp(String[] args) {
		if(args[0].equals("true")) {
			Collection<User>allUsers = domUserApi.getUsers("localhost");
			for(User user : allUsers) {
				UserExtension ext = userApi.getUserExtension(user);
				if (ext != null) {
					ext.setLastIp(null);
					user.getExt().put(userApi.getExtensionName(), ext);
				}
			}			
			domUserApi.updateUsers("localhost", allUsers, false);						
			context.println("initialized");
		} else {
			context.println("");
		}
	}
	
	@ScriptUsage(description = "set all user's device os to windows os", arguments = { @ScriptArgument(name = "set", type = "boolean", description = "set device type to windows os of whole users") })
   public void setAuthorizedDeviceWinOS(String[] args) {
       if(args[0].equals("true")) {
           List<AuthorizedDevice> devices = authDeviceApi.getDevices();

           for (AuthorizedDevice device : devices) {
               device.setType(1);
           }

           authDeviceApi.updateDevices(devices);

           context.println("set Windows OS");
       } else {
           context.println("");
       }

   }
	
}
