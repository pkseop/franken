package kr.co.future.sslvpn.core.servlet;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import kr.co.future.sslvpn.core.AuthCode;
import kr.co.future.sslvpn.core.AuthService;
import kr.co.future.sslvpn.core.DupLoginCheck;
import kr.co.future.sslvpn.core.ExternalAuthService;
import kr.co.future.sslvpn.core.SqlAuthService;
import kr.co.future.sslvpn.core.TunnelEventListener;
import kr.co.future.sslvpn.core.UserLimitService;
import kr.co.future.sslvpn.core.cluster.ClusterService;
import kr.co.future.sslvpn.model.AccessGateway;
import kr.co.future.sslvpn.model.AccessGateway.LdapMode;
import kr.co.future.sslvpn.model.AccessProfile;
import kr.co.future.sslvpn.model.UserExtension;
import kr.co.future.sslvpn.model.api.AccessGatewayApi;
import kr.co.future.sslvpn.model.api.AccessProfileApi;
import kr.co.future.sslvpn.model.api.AuthorizedDeviceApi;
import kr.co.future.sslvpn.model.api.IpLeaseApi;
import kr.co.future.sslvpn.model.api.UserApi;
import kr.co.future.sslvpn.xtmconf.BridgeTunCachingService;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Invalidate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.apache.felix.ipojo.annotations.Validate;
import org.json.JSONConverter;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import kr.co.future.dom.api.OrganizationUnitApi;
import kr.co.future.dom.api.TimetableApi;
import kr.co.future.dom.model.User;
import kr.co.future.httpd.HttpContext;
import kr.co.future.httpd.HttpService;
import kr.co.future.ldap.LdapProfile;
import kr.co.future.ldap.LdapServerType;
import kr.co.future.ldap.LdapService;
import kr.co.future.ldap.LdapUser;
import kr.co.future.sslvpn.core.login.CheckAccountLockStage;
import kr.co.future.sslvpn.core.login.CheckCertLoginStage;
import kr.co.future.sslvpn.core.login.CheckDuplicateLogin;
import kr.co.future.sslvpn.core.login.CheckPasswordStateStage;
import kr.co.future.sslvpn.core.login.CheckPwLoginStage;
import kr.co.future.sslvpn.core.login.CheckScheduleStage;
import kr.co.future.sslvpn.core.login.CheckValidClientIpStage;
import kr.co.future.sslvpn.core.login.CheckValidDateStage;
import kr.co.future.sslvpn.core.login.DeviceAuthStage;
import kr.co.future.sslvpn.core.login.ErrorStage;
import kr.co.future.sslvpn.core.login.InitialStage;
import kr.co.future.sslvpn.core.login.IpLeaseStage;
import kr.co.future.sslvpn.core.login.LdapAccountPWCheckStage;
import kr.co.future.sslvpn.core.login.LoginContext;
import kr.co.future.sslvpn.core.login.LoginPipeLine;
import kr.co.future.sslvpn.core.login.LoginUtil;
import kr.co.future.sslvpn.core.login.SuccessStage;
import kr.co.future.sslvpn.core.xenics.XenicsService;
import kr.co.future.sslvpn.core.xenics.impl.Users;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(name = "auth-service-servlet")
@Provides
public class AuthServiceServlet extends HttpServlet implements AuthService {
	private static final long serialVersionUID = 1L;
	private final Logger logger = LoggerFactory.getLogger(AuthServiceServlet.class);
	
	private static final LoginPipeLine loginPipeLine = new LoginPipeLine();
	
	static {
		loginPipeLine.addStage(new InitialStage());
		//stages that check the user state can login
		loginPipeLine.addStage(new CheckAccountLockStage());
		loginPipeLine.addStage(new CheckValidDateStage());
		loginPipeLine.addStage(new CheckScheduleStage());
		loginPipeLine.addStage(new CheckValidClientIpStage());
		loginPipeLine.addStage(new LdapAccountPWCheckStage());
		//verify user stages
		loginPipeLine.addStage(new CheckPwLoginStage());
		loginPipeLine.addStage(new CheckCertLoginStage());
		loginPipeLine.addStage(new CheckPasswordStateStage());
		loginPipeLine.addStage(new DeviceAuthStage());
		//verify succeeded
		loginPipeLine.addStage(new IpLeaseStage());
		loginPipeLine.addStage(new CheckDuplicateLogin());
		loginPipeLine.addStage(new SuccessStage());
		
		loginPipeLine.addErrorStage(new ErrorStage());
	}

	@Requires
	private HttpService httpd;
	
	@Requires
	private XenicsService xenicsService;
	
	@Requires
	private OrganizationUnitApi orgUnitApi;

	@Requires
	private TimetableApi timetableApi;

	@Requires
	private kr.co.future.dom.api.UserApi domUserApi;

	@Requires
	private UserApi userApi;

	@Requires
	private AccessProfileApi profileApi;

	@Requires
	private AccessGatewayApi gatewayApi;

	@Requires
	private AuthorizedDeviceApi authDeviceApi;

	@Requires
	private IpLeaseApi ipLeaseApi;

	@Requires
	private LdapService ldap;
	
	@Requires
	private SqlAuthService sqlAuth;

	@Requires
	private ExternalAuthService externalAuth;

	@Requires
	private ClusterService cluster;

	@Requires
	private UserLimitService userLimitApi;

	@Requires
	private BridgeTunCachingService bridgeCache;
	
	@Requires
	private DupLoginCheck dupLoginCheck;
	
	private int maxTunnel;
	
	private LoginUtil loginUtil;
	
	@Validate
	public void start() {
		//maxTunnel = SerialKey.getMaxTunnel();
		//장비 최대 터널 수는 장비에 발급된 라이센스 수로 변경
        maxTunnel = userLimitApi.getUserLimit();
		HttpContext ctx = httpd.ensureContext("frodo");
		ctx.addServlet("auth", this, "/auth/*");
		
		loginUtil = new LoginUtil(gatewayApi, domUserApi, profileApi, userApi, 
				orgUnitApi, externalAuth, authDeviceApi, cluster, bridgeCache, ipLeaseApi, ldap, timetableApi, sqlAuth,
				dupLoginCheck, xenicsService);
	}

	@Invalidate
	public void stop() {
		if (httpd != null) {
			HttpContext ctx = httpd.ensureContext("frodo");
			ctx.removeServlet("auth");
		}
		
		if(loginUtil != null)
			loginUtil.stop();
	}
	
	private void setHttpServletResponseHeader(HttpServletResponse resp) {
		resp.setHeader("Access-Control-Allow-Origin", "*");
		resp.setHeader("Access-Control-Allow-Methods", "GET,POST");
		resp.setHeader("Access-Control-Max-Age", "360");
		resp.setHeader("Access-Control-Allow-Headers", "X-PINGOTHER, Origin, X-Requested-With, Content-Type, Accept");
		resp.setHeader("Access-Control-Allow-Credentials", "true");
		resp.setContentType("application/json; charset=UTF-8");
	}
	
	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		String host = req.getHeader("Host");
		String path = req.getPathInfo();
		
		if ((host.equals("localhost") == false && host.equals("127.0.0.1") == false) || path == null) {
			resp.sendError(404);
			return;
		}
		
		setHttpServletResponseHeader(resp);
		if(path.equals("/login")) {
			login(req, resp);
		} else if(path.equals("/logout")) {
			logout(req, resp);
		}
	}
	
	private Map<String, Object> retrieveDataFromJson(HttpServletRequest req, Map<String, Object> params) {
		try {
			BufferedReader reader = new BufferedReader(new InputStreamReader(req.getInputStream(), "UTF-8"));
			String json = reader.readLine();
			
			logger.debug("json : " + json);
			
			JSONTokener tokener = new JSONTokener(json);
			JSONObject jsonObject = null;
		
			jsonObject = new JSONObject(tokener);
			return (Map<String, Object>) JSONConverter.parse(jsonObject);
		} catch (JSONException e) {
			logger.error("cannot convert object to json string", e);
		} catch (IOException e) {
			logger.error("cannot read stream", e);
		}
		
		return null;
	}
	
	private void login(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		Map<String, Object> props = null;
		props = retrieveDataFromJson(req, props);					//parse json data
		if(props == null) {
			resp.sendError(400);
			return;
		}
		
		Map<String, Object> result = login(props);					//execute login
		if(result == null) {
			resp.sendError(400);
			return;
		}
		
		try {
			String json = JSONConverter.jsonize(result);			//response result
			PrintWriter out = resp.getWriter();
			out.print(json);
		} catch (JSONException e) {
			logger.error("convert object to json error", e);
			resp.sendError(500);
		}
		
	}
	
	private void logout(HttpServletRequest req, HttpServletResponse resp) {
		int tunnelId = Integer.parseInt(req.getParameter("tunnel_id"));
		logout(tunnelId);
	}
	
	@Override
	public AccessProfile determineProfile(String loginName) {
		return loginUtil.determineProfile(loginName);
	}

	@Override
	public AccessProfile determineProfile(String loginName, User user) {
		return loginUtil.determineProfile(loginName, user);
	}

	@Override
	public void deployPolicy() {
		List<AccessProfile> profiles = profileApi.getAccessProfiles();
		xenicsService.updateAccessProfileInfo(profiles);
	}

	@Override
	public void killTunnel(int id) {
		xenicsService.killTunnel(id);
	}

	@Override
	public Map<String, Object> login(Map<String, Object> props) {
		if(xenicsService.getTotalNumOfTunnels() >= maxTunnel) {
			return loginUtil.fail(AuthCode.TunnelLimit, null);
		}
		
		try {
			LoginContext loginContext = new LoginContext(loginUtil, props);
			loginPipeLine.execute(loginContext);
			return loginContext.getResult();
		} catch (Exception e) {
			logger.error("login error", e);
			return null;
		}
	}

	@Override
	public AuthCode checkPassword(String loginName, String password) {
		Map<String, Object> m = new HashMap<String, Object>();
		m.put("id", loginName);
		m.put("pw", password);
		m.put("client_otp", "password_change");	// Peter H. Nahm
		return loginUtil.checkPasswordLogin(m);
	}

	@Override
	public void logout(int tunnelId) {
		ipLeaseApi.release(tunnelId);		
		Users tunnelInfo = xenicsService.getTunnelInfo(tunnelId);
		if (tunnelInfo != null) {
			User user = loginUtil.findUser(tunnelInfo.user_id);
			if (user != null) {
				UserExtension ext = userApi.getUserExtension(user);
				if (ext == null)
					ext = loginUtil.createUserExtension(user, tunnelInfo.rent_ip);

				ext.setLastLogoutTime(new Date());
				ext.setUpdateDateTime(new Date());
				loginUtil.submitUpdate(ext.getUser());
			}
			logger.info("frodo core: logout [{}]", tunnelInfo);
		} else {
			logger.info("frodo core: logout tunnel [{}] not found", tunnelId);
		}
		
		//check for duplicate login
		final String loginName = tunnelInfo.user_id;
		if(dupLoginCheck.useDuplicateLoginCheck()) {
			Thread t = new Thread(){
				public void run(){
					dupLoginCheck.sendLogoutInfo(loginName);
			    }
			};
			t.start();
		}
	}

	@Override
	public void addTunnelEventListener(TunnelEventListener listener) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void removeTunnelEventListener(TunnelEventListener listener) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void changePassword(String loginName, String newPassword) {
		AccessGateway gw = gatewayApi.getCurrentAccessGateway();
		User user = domUserApi.getUser("localhost", loginName);

		Map<String, Object> ext = user.getExt();
		@SuppressWarnings("unchecked")
		Map<String, Object> frodo = (Map<String, Object>) ext.get("frodo");
		if(frodo != null) {
			frodo.put("force_password_change", false);
			ext.put("frodo", frodo);
			user.setExt(ext);
		}
		
		String sourceType = user.getSourceType();
		Integer loginMethod = loginUtil.getLoginMethod(gw, loginName, user);	// Peter H. Nahm

		if (sourceType == null) {
			if (user.getPassword() != null) {
				user.setPassword(newPassword);
				domUserApi.updateUser("localhost", user, true);
				return;
			}
		} else {
			if (sourceType.equals("local") || loginMethod == LoginUtil.LoginMethod_PW_OTP) {
				user.setPassword(newPassword);
			} else if (sourceType.equals("ldap")) {
				if (gw.getLdapMode() == null || gw.getLdapMode() == LdapMode.NONE) {
					throw new IllegalStateException("ldap auth is disabled");
				} else if (gw.getLdapMode() == LdapMode.LDAP_SYNC) {
					LdapProfile profile = loginUtil.getLdapProfile();
					if (profile == null)
						throw new IllegalStateException("ldap auth config not set");

					logger.debug("frodo core: changing ldap password, profile [{}], login name [{}]", profile.getName(),
							loginName);
					ldap.changePassword(profile, loginName, newPassword);
				} else if (gw.getLdapMode() == LdapMode.LDAP_SYNC_EXTEND) {
					List<LdapProfile> profiles = new ArrayList<LdapProfile>();

					LdapProfile defaultProfile = loginUtil.getLdapProfile();
					if (defaultProfile != null)
						profiles.add(defaultProfile);
					profiles.addAll(ldap.getProfiles());

					for (LdapProfile profile : profiles) {
						if (profile.getServerType() != LdapServerType.ActiveDirectory)
							continue;

						LdapUser ldapUser = ldap.findUser(profile, loginName);
						if (ldapUser != null) {
							ldap.changePassword(profile, loginName, newPassword);
							break;
						}
					}

				}
			} else if (sourceType.equals("external")) {
				if (!externalAuth.isEnabled())
					throw new IllegalStateException("external auth is disabled");

				if (!externalAuth.isPasswordChangeSupported())
					throw new IllegalStateException("external auth module does not support password change");

				logger.debug("frodo core: changing external password, login name [{}]", loginName);
				externalAuth.changePassword(loginName, newPassword);
				
				return;
			}

			domUserApi.updateUser("localhost", user, true);
			return;
		}

		throw new IllegalStateException("cannot change password, login name=" + loginName);
	}

}
