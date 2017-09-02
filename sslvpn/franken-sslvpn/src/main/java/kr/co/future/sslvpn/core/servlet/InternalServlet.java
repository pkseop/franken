package kr.co.future.sslvpn.core.servlet;

import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import kr.co.future.sslvpn.core.AuthService;
import kr.co.future.sslvpn.core.ExternalAuthService;
import kr.co.future.sslvpn.core.SqlAuthService;
import kr.co.future.sslvpn.core.servlet.InternalServlet;
import kr.co.future.sslvpn.model.AccessGateway;
import kr.co.future.sslvpn.model.AccessGateway.LdapMode;
import kr.co.future.sslvpn.model.AccessProfile;
import kr.co.future.sslvpn.model.ClientApp;
import kr.co.future.sslvpn.model.UserExtension;
import kr.co.future.sslvpn.model.api.AccessGatewayApi;
import kr.co.future.sslvpn.model.api.AccessProfileApi;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Invalidate;
import org.apache.felix.ipojo.annotations.Requires;
import org.apache.felix.ipojo.annotations.Validate;
import org.json.JSONConverter;
import org.json.JSONException;

import kr.co.future.codec.Base64;
import kr.co.future.dom.api.FileUploadApi;
import kr.co.future.dom.api.UserApi;
import kr.co.future.dom.model.UploadedFile;
import kr.co.future.dom.model.User;
import kr.co.future.httpd.HttpContext;
import kr.co.future.httpd.HttpService;
import kr.co.future.sslvpn.core.xenics.XenicsService;
import kr.co.future.sslvpn.core.xenics.impl.Users;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(name = "frodo-internal-servlet")
public class InternalServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;
	private final String LOCALHOST = "localhost";
	private final Logger logger = LoggerFactory.getLogger(InternalServlet.class);

	private enum ApplicationType {
		WebApp, NativeApp, UnknownAppType
	};

	private enum OsType {
		Windows, WindowsMobile, Mac, iOS, Linux, Android, Symbian, Others;
	}

	private enum DeviceType {
		PC, Phone, Pad, UnknownDevType;
	}

	@Requires
	private HttpService httpd;

	@Requires
	private UserApi domUserApi;

	@Requires
	private kr.co.future.sslvpn.model.api.UserApi userApi;

	@Requires
	private FileUploadApi fileUploadApi;

	@Requires
	private AuthService auth;

	@Requires
	private AccessProfileApi profileApi;

	@Requires
	private AccessGatewayApi gwApi;

	@Requires
	private ExternalAuthService externalService;

	@Requires
	private SqlAuthService sqlService;
	
	@Requires
	private XenicsService xenicsService;

	private BundleContext bc;
	
	private String sslplusIcon;

	public InternalServlet(BundleContext bc) {
		this.bc = bc;
	}

	@Validate
	public void start() {
		HttpContext ctx = httpd.ensureContext("frodo");
		ctx.addServlet("internal", this, "/internal/*");
		
		sslplusIcon = new String(Base64.encode(getIcon("icon/sslplus.png")));
	}

	@Invalidate
	public void stop() {
		if (httpd != null) {
			HttpContext ctx = httpd.ensureContext("frodo");
			ctx.removeServlet("internal");
		}
	}

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		logger.trace("frodo core: check login use remote address [{}]", req.getRemoteAddr());

		Users tunnelInfo = xenicsService.getConnectedTunnelInfo(req.getRemoteAddr());
		if(tunnelInfo == null) {
			logger.error("frodo core: remote address [{}] does not login", req.getRemoteAddr());
			resp.sendError(401);
			return;
		}

		resp.setContentType("text/html;charset=utf-8");
		PrintWriter out = resp.getWriter();

		User domUser = domUserApi.getUser(LOCALHOST, tunnelInfo.user_id);
		logger.trace("frodo core: appinfo req from user [{}], ip [{}]", tunnelInfo.user_id, req.getRemoteAddr());

		AccessProfile profile = profileApi.determineProfile(domUser.getLoginName());
		logger.trace("frodo core: appinfo req from user [{}], profile [{}:{}]", new Object[] { tunnelInfo.user_id, profile.getId(),
				profile.getName() });

		try {
			if (req.getPathInfo().equals("/appinfo")) {
				if (req.getParameter("devtype") == null || req.getParameter("ostype") == null ||
						req.getParameter("login_name") == null) {
					logger.error("frodo core: devtype or ostype or login_name is null");
					resp.sendError(400);
					out.close();
					return;
				}
				
				List<ClientApp> clientApps = profile.getClientApps();
				int deviceType = Integer.parseInt(req.getParameter("devtype"));
				int osType = Integer.parseInt(req.getParameter("ostype"));
				String apps = getAppInfos(clientApps, deviceType, osType);

				if (apps == null) {
					logger.error("frodo core: cannot find app list");
					resp.sendError(400);
					out.close();
					return;
				}

				out.print(apps);
				out.close();
				return;
			}
			if (req.getPathInfo().equals("/userinfo")) {
				if(req.getParameter("login_name") == null) {
					resp.sendError(400);
					out.close();
					return;
				}
												
				out.print(getUserInfo(tunnelInfo, domUser, profile));
				out.close();
				return;
			}
		} catch (JSONException e) {
			logger.error("frodo core: cannot serialize json", e);
			resp.sendError(500);
			return;
		}

		resp.sendError(400);
	}

	private String getUserInfo(Users tunnelInfo, User domUser, AccessProfile profile) throws JSONException {
		logger.trace("frodo core: try to return user info about [{}]", domUser.getLoginName());
		String sourceType = domUser.getSourceType();

		UserExtension ext = userApi.getUserExtension(domUser);

		long deadLine = 0;
		Long accountExpiryRemain = null;
		if (ext == null)
			deadLine = profile.getPasswordExpiry();
		else {
			if (ext.getExpireDateTime() != null) {
				long betweenTime = ext.getExpireDateTime().getTime() - new Date().getTime();
				if (betweenTime < 0)
					accountExpiryRemain = 0l;
				else
					accountExpiryRemain = betweenTime / 1000;
			}
			deadLine = ext.getPasswordChangeDeadline(profile.getPasswordExpiry());
		}

		if (sourceType != null) {
			if (sourceType.equals("external")) {
				if (externalService.isPasswordExpirySupported())
					deadLine = externalService.getPasswordExpiry(domUser.getLoginName());
				if (externalService.isAccountExpirySupported()) {
					long betweenTime = externalService.getAccountExpiry(domUser.getLoginName()) - new Date().getTime();
					if (betweenTime < 0)
						accountExpiryRemain = 0l;
					else
						accountExpiryRemain = betweenTime / 1000;
				}
			} else if (sourceType.equals("sql") && sqlService.isPasswordExpirySupported()) {
				deadLine = sqlService.getPasswordExpiry(domUser.getLoginName());
			}
		}

		int loginMethod = gwApi.getCurrentAccessGateway().getLoginMethod();
		if (profile.getLoginMethod() != null) {
			loginMethod = profile.getLoginMethod();
		}

		boolean extPassChangeSupp;

        if (externalService.isEnabled()) {
            extPassChangeSupp = externalService.isPasswordChangeSupported();
        } else {
            extPassChangeSupp = false;
        }

		boolean resetable = UserExtension.getPasswordResetable(domUser, loginMethod, extPassChangeSupp);

		AccessGateway gw = gwApi.getCurrentAccessGateway();
		boolean ldapPwdChange = domUser.getSourceType() != null && domUser.getSourceType().equals("ldap")
				&& gw.getLdapMode() == LdapMode.LDAP_SYNC_EXTEND && profile.getLdapAttributes() != null;

		if (ldapPwdChange)
			resetable = profile.getLdapAttributes().isUsePasswordChange();

		Map<String, Object> m = new HashMap<String, Object>();
		m.put("login_name", domUser.getLoginName());
		m.put("name", domUser.getName());
		m.put("login_date_time", tunnelInfo.login_date.getTime());
		m.put("idle_time_out", profile.isUseClientTimeout() ? profile.getClientTimeout() : 0);
		m.put("user_data", new HashMap<String, String>());
		m.put("password_change_deadline", deadLine);
		m.put("password_change_alert", profile.getPasswordChangeAlert());

		// ExternalServlet의 vpnifo를 통해 전달이 되지만 클라이언트 편의성을 위해 추가
		m.put("password_resetable", resetable);
		m.put("password_reset_msg", gwApi.getCurrentAccessGateway().getPasswordResetMessage());
		m.put("force_password_change", (ext == null ? false : ext.isForcePasswordChange()) && resetable);

		// 클라이언트 앱 매크로 확장 지원용
		Map<String, Object> principal = new HashMap<String, Object>();
//		principal.put("subject_dn", t.getSubjectDn());
		principal.put("sso_token", null); // 추후 확장 예정
		m.put("principal", principal);

		if (accountExpiryRemain != null) {
			m.put("account_expiry_alert", profile.getAccountExpiryAlert());
			m.put("account_expiry_remain", accountExpiryRemain);
		}

		Boolean useClientAutoUninstall = gw.getUseClientAutoUninstall();
		if (profile.getUseClientAutoUninstall() != null)
			useClientAutoUninstall = profile.getUseClientAutoUninstall();
		m.put("use_client_auto_uninstall", useClientAutoUninstall);

		if (externalService.isEnabled() && externalService.useSso()
				&& externalService.getSsoToken(domUser.getLoginName()) != null) {
			m.put("sso_token", externalService.getSsoToken(domUser.getLoginName()));
		}
		
		//사용자 유효기간.
		SimpleDateFormat sdFormat = new SimpleDateFormat("yyyy-MM-dd");
		if(ext != null) {
			m.put("start_at", ext.getStartDateTime() == null ? "" : sdFormat.format(ext.getStartDateTime()));
			m.put("expire_at", ext.getExpireDateTime() == null ? "" : sdFormat.format(ext.getExpireDateTime()));
		} else {
			m.put("start_at", "");
			m.put("expire_at", "");
		}

		return JSONConverter.jsonize(m);
	}

	private String getAppInfos(List<ClientApp> clientApps, int devType, int osType) throws JSONException {
		if (devType < 1 || devType > 4 || osType < 1 || osType > 8) {
			return null;
		}

		List<Object> apps = new ArrayList<Object>();
		OsType os = checkOsType(osType);
		DeviceType deviceType = checkDevType(devType);
		for (ClientApp c : clientApps) {
			if (!isMatchingClientApp(c, deviceType, os)) {
				logger.trace("frodo core: skipping client app [name={}, appguid={}]", c.getName(), c.getGuid());
				continue;
			}

			String icon = null;
			if (c.getIcon() == null) {
				icon = sslplusIcon;
			} else {
				try {
					byte[] iconBinary = getIconFromFileUploadApi(c.getIcon());
					if (iconBinary != null)
						icon = new String(Base64.encode(iconBinary));
					else
						icon = sslplusIcon;
				} catch (Exception e) {
					logger.warn("frodo client adapter: getIconFromFileUploadApi failed", e);
					icon = sslplusIcon;
				}
			}

			Map<String, Object> m = new HashMap<String, Object>();
			m.put("guid", c.getGuid());
			m.put("icon", icon);
			m.put("name", c.getName());
			m.put("description", c.getDescription());
			Map<String, Object> operator = new HashMap<String, Object>();
			operator.put("tel", c.getPhone());
			operator.put("mobile", c.getMobile());
			operator.put("email", c.getEmail());
			operator.put("name", c.getOperator());
			m.put("operator", operator);

			ApplicationType appType = getApplicationType(c.getPlatform(), deviceType, os);

			m.put("app_type", appType.toString());
			m.put("app_data", c.getMetadata());
			apps.add(m);

		}
		return JSONConverter.jsonize(apps);
	}

	private ApplicationType getApplicationType(String platform, DeviceType devType, OsType ost) {
		if (platform.equals("windows")) {
			return ApplicationType.NativeApp;
		} else if (platform.equals("web")) {
			return ApplicationType.WebApp;
		} else if (platform.equals("android")) {
			return ApplicationType.NativeApp;
		} else {
			return ApplicationType.UnknownAppType;
		}
	}

	private byte[] getIconFromFileUploadApi(String guid) {
		byte[] b = null;

		ByteArrayOutputStream bos = null;
		InputStream fis = null;
		try {
			UploadedFile fileMetadata = fileUploadApi.getFileMetadata(LOCALHOST, guid);
			if (fileMetadata == null)
				return null;

			fis = new FileInputStream(fileMetadata.getFile());
			bos = new ByteArrayOutputStream();

			byte[] buf = new byte[1024];
			for (int readNum; (readNum = fis.read(buf)) != -1;) {
				bos.write(buf, 0, readNum);
			}
			b = bos.toByteArray();
		} catch (FileNotFoundException e) {
			logger.error("frodo core: cannot download icon guid=" + guid);
		} catch (IOException e) {
			logger.error("frodo core: cannot read icon guid=" + guid, e);
		} finally {
			close(fis);
			close(bos);
		}

		return b;
	}

	private byte[] getIcon(String path) {
		byte[] b = null;

		ByteArrayOutputStream bos = null;
		InputStream fis = null;
		try {
			Bundle bundle = bc.getBundle();
			if (bundle == null) {
				logger.error("frodo core: client adapter bundle not found, icon search failed");
				return null;
			}

			URL resource = bundle.getResource(path);
			if (resource == null) {
				logger.trace("frodo core: icon path [{}] not found in bundle [{}]", path, bundle.getBundleId());
				return null;
			}

			fis = resource.openStream();
			bos = new ByteArrayOutputStream();

			byte[] buf = new byte[1024];
			for (int readNum; (readNum = fis.read(buf)) != -1;) {
				bos.write(buf, 0, readNum);
			}
			b = bos.toByteArray();
		} catch (FileNotFoundException e) {
			logger.error("frodo core: icon file not found, path=" + path);
		} catch (IOException e) {
			logger.error("frodo core: cannot read icon, path=" + path, e);
		} finally {
			close(fis);
			close(bos);
		}

		return b;
	}

	private void close(Closeable bos) {
		if (bos == null)
			return;
		try {
			bos.close();
		} catch (IOException e) {
		}
	}

	private boolean isMatchingClientApp(ClientApp clientApp, DeviceType devType, OsType ost) {
		String platform = clientApp.getPlatform();
		if (platform.equals("windows")) {
			if (devType == DeviceType.PC && ost == OsType.Windows)
				return true;
			else {
				logger.trace("frodo core: mismatched type, os type [{}], device type [{}] and platform [{}]",
						new Object[] { ost.toString(), devType.toString(), platform });
				return false;
			}
		} else if (platform.equals("web")) {
			return true;
		} else if (platform.equals("android")) {
			if (ost == OsType.Android)
				return true;
			else {
				logger.trace("frodo core: mismatched type, os type [{}], device type [{}] and platform [{}]",
						new Object[] { ost.toString(), devType.toString(), platform });
				return false;
			}
		} else {
			logger.trace("frodo core: mismatched type, os type [{}], device type [{}] and platform [{}]",
					new Object[] { ost.toString(), devType.toString(), platform });
			return false;
		}
	}

	/**
	 * 
	 * @param remoteAddr
	 *            the leased ip address
	 * @return mapped tunnel
	 */
//	private Tunnel checkLogin(String remoteAddr) throws UnknownHostException {
//		return auth.getTunnel(InetAddress.getByName(remoteAddr));
//	}

	private OsType checkOsType(int type) {
		switch (type) {
		case 1:
			return OsType.Windows;
		case 2:
			return OsType.WindowsMobile;
		case 3:
			return OsType.Mac;
		case 4:
			return OsType.iOS;
		case 5:
			return OsType.Linux;
		case 6:
			return OsType.Android;
		case 7:
			return OsType.Symbian;
		case 8:
			return OsType.Others;
		default:
			return null;
		}
	}

	private DeviceType checkDevType(int type) {
		switch (type) {
		case 1:
			return DeviceType.PC;
		case 2:
			return DeviceType.Phone;
		case 3:
			return DeviceType.Pad;
		case 4:
			return DeviceType.UnknownDevType;
		default:
			return null;
		}
	}

}
