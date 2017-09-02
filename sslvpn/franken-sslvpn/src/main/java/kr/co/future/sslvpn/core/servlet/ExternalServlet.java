package kr.co.future.sslvpn.core.servlet;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.security.spec.AlgorithmParameterSpec;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import kr.co.future.sslvpn.core.AuthService;
import kr.co.future.sslvpn.core.DupLoginCheck;
import kr.co.future.sslvpn.core.ExternalAuthService;
import kr.co.future.sslvpn.core.GlobalConfig;
import kr.co.future.sslvpn.core.GlobalConfigApi;
import kr.co.future.sslvpn.core.cluster.ClusterNode;
import kr.co.future.sslvpn.core.cluster.ClusterService;
import kr.co.future.sslvpn.core.servlet.ExternalServlet;
import kr.co.future.sslvpn.model.AccessGateway;
import kr.co.future.sslvpn.model.AccessGateway.LdapMode;
import kr.co.future.sslvpn.model.AccessProfile;
import kr.co.future.sslvpn.model.ClientCheckProfile;
import kr.co.future.sslvpn.model.ExternalVpn;
import kr.co.future.sslvpn.model.UserExtension;
import kr.co.future.sslvpn.model.VpnServerConfig;
import kr.co.future.sslvpn.model.api.AccessGatewayApi;
import kr.co.future.sslvpn.model.api.AccessProfileApi;
import kr.co.future.sslvpn.model.api.VpnServerConfigApi;
import kr.co.future.sslvpn.xtmconf.BridgeTunCachingService;

import org.apache.commons.codec.binary.Base64;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Invalidate;
import org.apache.felix.ipojo.annotations.Requires;
import org.apache.felix.ipojo.annotations.Validate;
import org.json.JSONConverter;
import org.json.JSONException;

import kr.co.future.dom.api.UserApi;
import kr.co.future.dom.model.User;
import kr.co.future.httpd.HttpContext;
import kr.co.future.httpd.HttpService;
import kr.co.future.ldap.LdapService;
import kr.co.future.sslvpn.core.xenics.XenicsConfig;
import kr.co.future.sslvpn.core.xenics.XenicsService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(name = "frodo-external-servlet")
public class ExternalServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;
	private final Logger logger = LoggerFactory.getLogger(ExternalServlet.class);

	@Requires
	private HttpService httpd;

	@Requires
	private AccessGatewayApi gwApi;

	@Requires
	private AccessProfileApi profileApi;

	@Requires
	private AuthService auth;

	@Requires
	private UserApi domUserApi;

	@Requires
	private ExternalAuthService externalService;

	@Requires
	private GlobalConfigApi configApi;

	@Requires
	private ClusterService cluster;

	@Requires
	private BridgeTunCachingService bridgeCache;

	@Requires
	private kr.co.future.sslvpn.model.api.UserApi userApi;
	
	@Requires 
	private LdapService ldap;
	
	@Requires
	private XenicsService xenicsService;
	
	@Requires
	private XenicsConfig xenicsConfig;
	
	@Requires
	private VpnServerConfigApi vpnServerConfigApi;
	
	@Requires
	private DupLoginCheck dupLoginCheck;
	
	@Validate
	public void start() {
		HttpContext ctx = httpd.ensureContext("frodo");
		ctx.addServlet("external", this, "/external/*");
	}

	@Invalidate
	public void stop() {
		if (httpd != null) {
			HttpContext ctx = httpd.ensureContext("frodo");
			ctx.removeServlet("external");
		}
	}

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		String pathInfo = req.getPathInfo();
		AccessGateway gw = gwApi.getCurrentAccessGateway();

		if (pathInfo.equals("/top_image")) {
			if (gw.getTopImage() == null) {
				resp.sendError(404);
			} else {
				resp.setHeader("Content-Type", "application/octet-stream");
				resp.getOutputStream().write(Base64.decodeBase64(gw.getTopImage()));
				resp.getOutputStream().close();
			}
			return;
		} else if (pathInfo.equals("/notice_image")) {
			if (gw.getNoticeImage() == null) {
				resp.sendError(404);
			} else {
				resp.setHeader("Content-Type", "application/octet-stream");
				resp.getOutputStream().write(Base64.decodeBase64(gw.getNoticeImage()));
				resp.getOutputStream().close();
			}
			return;
		}

		PrintWriter out = resp.getWriter();
		if (pathInfo.equals("/notice")) {
			String notice = gw.getNotice();
			out.print(notice == null ? "" : notice);
			out.close();
			return;

		} else if (pathInfo.equals("/setup.ini")) {
			String ini = "DesktopIconName=" + (gw.getLnkName() != null ? gw.getLnkName() : "");
			out.print(ini);
			out.close();
			return;
		}

		try {
			if (pathInfo.equals("/authmethod")) {
				handleAuthMethod(req, resp, out);
				return;
			} else if (pathInfo.equals("/vpninfo")) {
				handleVpnInfo(req, resp, out, false);
				return;
			} else if (pathInfo.equals("/vpninfo.ini")) {
				handleVpnInfo(req, resp, out, true);
				return;
			} else if (pathInfo.equals("/clientcheck")) {
				/* nac 보안설정 */
				handleClientSecurityInfo(req, resp, out);
				return;
			} else if (pathInfo.equals("/global_config")) {
				handleGlobalConfig(out);
				return;
			} else if (pathInfo.equals("/client_icon_config")) {
				handleClientIconConfig(out);
			} 
		} catch (JSONException e) {
			logger.error("frodo core: cannot convert object to json string", e);
			resp.sendError(500);
		} catch (IOException e) {
			if (((e instanceof java.net.SocketException) || (e instanceof java.net.SocketException)) && (e.getMessage().equals("Broken pipe") || e.getMessage().contains("Connection reset"))) {
				//Broken pipe나 connection reset 경우에는 에러 메시지를 찍지 않음.
				logger.debug("frodo externalServlet: ", e);
			} else {
				logger.error("frodo externalServlet: cannot send json message", e);	
			}
		} finally {
			out.close();
		}

		resp.sendError(500);
	}

	private void handleClientIconConfig(PrintWriter out) throws JSONException {
		GlobalConfig config = configApi.getGlobalConfig();
		Map<String, Object> m = new HashMap<String, Object>();
		m.put("windows_client", true);
		m.put("windows_client_connection_add", true);
		if (config != null) {
			m.put("windows_client", config.isShowWindowsClientIcon());
			m.put("windows_client_connection_add", config.isShowWindowsClientConnectionIcon());
		}

		Map<String, Object> result = new HashMap<String, Object>();
		result.put("client_icon_config", m);
		out.print(JSONConverter.jsonize(result));
	}

	private void handleGlobalConfig(PrintWriter out) throws JSONException {
		GlobalConfig config = configApi.getGlobalConfig();
		Map<String, Object> m = new HashMap<String, Object>();
		m.put("show_windows_client_download", true);
		m.put("show_linux_client_download", true);
		m.put("show_ios_client_download", true);
		m.put("show_android_client_download", true);
		m.put("show_manual_download", true);
		if (config != null) {
			m.put("show_windows_client_download", config.isShowWindowsClientDownload());
			m.put("show_linux_client_download", config.isShowLinuxClientDownload());
			m.put("show_ios_client_download", config.isShowIosClientDownload());
			m.put("show_android_client_download", config.isShowAndroidClientDownload());
			m.put("show_manual_download", config.isShowManualDownload());
		}

		Map<String, Object> result = new HashMap<String, Object>();
		result.put("global_config", m);
		out.print(JSONConverter.jsonize(result));
	}

	/**
	 * nac 보안설정 관련
	 * 
	 * @param req
	 * @param resp
	 * @param out
	 * @throws IOException
	 * @throws JSONException
	 */
	private void handleClientSecurityInfo(HttpServletRequest req, HttpServletResponse resp, PrintWriter out) throws IOException,
			JSONException {
		String loginName = req.getParameter("login_name");

		// 추후 안드로이드 작업을 위한 포석
		String osType = req.getParameter("os_type");

		if (loginName == null) {
			logger.error("frodo core: login name is null");
			resp.sendError(500);
			return;
		}

		if (osType == null) {
			logger.error("frodo core: os type is null");
			resp.sendError(500);
			return;
		}

		AccessProfile profile = auth.determineProfile(loginName);
		if (profile == null) {
			logger.trace("frodo core: user not found, user [{}]", loginName);
			resp.sendError(404);
			return;
		}

		ClientCheckProfile clientCheckProfile = profile.getClientCheckProfile();

		if (clientCheckProfile == null) {
			logger.trace("frodo core: client check profile not found, access profile [{}]", profile.getName());
			resp.sendError(404);
			return;
		}

		String clientCheckProfileToJson = JSONConverter.jsonize(clientCheckProfile.marshal());

		// 한글 출력 처리
		resp.setContentType("text/html; charset=utf-8");

		out.print(clientCheckProfileToJson);

		return;

	}

	private void handleVpnInfo(HttpServletRequest req, HttpServletResponse resp, PrintWriter out, boolean isIni)
			throws IOException, JSONException {
		Map<String, Object> m = getVpnInfo();
		resp.setContentType("text/html; charset=utf-8");
		String vpninfo = "";
		if (!isIni) {
			vpninfo = JSONConverter.jsonize(m);
		} else {
			for (String s : m.keySet()) {
				vpninfo += s + "=" + m.get(s) + "\n";
			}
		}
		vpninfo = encryptVpninfo(vpninfo);
		out.print(vpninfo);
	}
	
	private String encryptVpninfo(String vpninfo) {
		GlobalConfig config = configApi.getGlobalConfig();
		if(config != null) {
			Boolean encryptVpninfo = config.isEncryptVpninfo();
			if(encryptVpninfo != null && encryptVpninfo){
				vpninfo = genEncryptedVpninfo(config, vpninfo);
			} 
		}
		
		return vpninfo;
	}
	
	private static final String KEY_FOR_VPNINFO = "27d4114dc84036dfe0f434b82ce21110";
	
	private String genEncryptedVpninfo(GlobalConfig config, String vpninfo) {
		byte[] encryptedVpninfo = AESEncrypt(vpninfo, KEY_FOR_VPNINFO);		
		String base64Str = kr.co.future.codec.Base64.encodeLines(encryptedVpninfo);
		return base64Str;
	}
	
	private byte[] AESEncrypt(String str, String key) {
		byte[] result = null;
		try{
			byte[] ivBytes = { 0x24, 0x22, 0x7E, 0x37, 0x3B, 0x59, 0x17, 0x09, 0x25, 0x5F, 0x6A, 0x73, 0x68, 0x49, 0x10, 0x39 };
			byte[] textBytes = str.getBytes("UTF-8");
			AlgorithmParameterSpec ivSpec = new IvParameterSpec(ivBytes);
		    SecretKeySpec newKey = new SecretKeySpec(key.getBytes("UTF-8"), "AES");
		    Cipher cipher = null;
			cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
			cipher.init(Cipher.ENCRYPT_MODE, newKey, ivSpec);
			result = cipher.doFinal(textBytes);
		} catch (Exception e) {
			logger.error("frodo core: vpninfo encryption failed", e);
			return null;
		}
		return result;
	}

	private void handleAuthMethod(HttpServletRequest req, HttpServletResponse resp, PrintWriter out) throws IOException,
			JSONException {
		String loginName = req.getParameter("login_name");
		if (loginName == null) {
			logger.error("frodo core: login name is null");
			resp.sendError(500);
			return;
		}

		AccessGateway gw = gwApi.getCurrentAccessGateway();
		VpnServerConfig vpnConfig = vpnServerConfigApi.getCurrentVpnServerConfig();
		AccessProfile profile = auth.determineProfile(loginName);
		if (profile == null) {
			logger.trace("frodo core: user [{}] not found", loginName);
			resp.sendError(404);
			return;
		}

		Map<String, Object> m = new HashMap<String, Object>();
		Integer loginMethod = gw.getLoginMethod();
		if (profile.getLoginMethod() != null) {
			loginMethod = profile.getLoginMethod();
		}

		User user = domUserApi.findUser("localhost", loginName);
		
		boolean extPassChangeSupp;

        if (externalService.isEnabled()) {
            extPassChangeSupp = externalService.isPasswordChangeSupported();
        } else {
            extPassChangeSupp = false;
        }

		boolean resetable = UserExtension.getPasswordResetable(user, loginMethod, extPassChangeSupp);

		if (user.getSourceType() != null && user.getSourceType().equals("ldap") && gw.getLdapMode() == LdapMode.LDAP_SYNC_EXTEND
				&& profile.getLdapAttributes() != null)
			resetable = profile.getLdapAttributes().isUsePasswordChange();

		m.put("internal_ip", getInternalIp());
		m.put("auth_method", loginMethod);
		m.put("encryptions", xenicsConfig.getCipherConfig(vpnConfig.getEncryptions().get(0)));
		m.put("password_resetable", resetable);
		m.put("password_fail_limit", profile.getFailLimitCount());
		
		m.put("use_proxy", (profile.getUseProxy()==null ? false : profile.getUseProxy()));
		m.put("proxy_port", (vpnConfig.getProxyPort() == null ? 24886 : vpnConfig.getProxyPort()));
		
		UserExtension ext = userApi.getUserExtension(user);
		if (ext != null)
			m.put("force_password_change", ext.isForcePasswordChange());

		if (profile.getPopupUrl() != null && !profile.getPopupUrl().trim().isEmpty())
			m.put("popup_url", profile.getPopupUrl());

		m.put("use_auto_reconnect", profile.getUseAutoReconnect());
		m.put("user_unlock_time", (profile.getUserUnlockTime() == null ? 0 : profile.getUserUnlockTime()));
		m.put("duplicate_login", isDuplicatedLogin(loginName));
		
		String authMethod = JSONConverter.jsonize(m);
		out.print(authMethod);
		
		return;
	}
	
	private boolean isDuplicatedLogin(String loginName) {
//		ClusterConfig clusterConfig = cluster.getConfig();
//		if(clusterConfig.isEnabled()) {
//			return cluster.isDuplicatedLogin(loginName);
//		}
		
//		if(xenicsConfig.getRemoteDbUsing()) {
//			return xenicsService.isDupLoginByRemoteDb(loginName);
//		} 
		if(dupLoginCheck.isBlock() == false) {
			if(dupLoginCheck.useDuplicateLoginCheck()) {
				return dupLoginCheck.isDuplicateLogin(loginName);
			} else {
				return xenicsService.isAlreadyLoggedin(loginName);
			}
		} else		//dup-login.block이 true로 되어 있으면 클라이언트에서 중복 로그인 메시지를 보이지 않도록 false를 리턴한다. 
			return false;
	}

	private Map<String, Object> getVpnInfo() throws IOException, JSONException {
		AccessGateway currentAccessGateway = gwApi.getCurrentAccessGateway();
		VpnServerConfig vpnConfig = vpnServerConfigApi.getCurrentVpnServerConfig();
		if (currentAccessGateway == null) {
			logger.error("frodo core: cannot find current access gateway.");
			return null;
		}

		String internalIp = getInternalIp();
		String resetMsg = currentAccessGateway.getPasswordResetMessage();

		Map<String, Object> m = new HashMap<String, Object>();
		int authMethod = profileApi.isAllAuthMethodSame() ? currentAccessGateway.getLoginMethod() : 0;
		m.put("auth_method", authMethod);

		String encryption = vpnConfig.getEncryptions().get(0);	//profileApi.isAllEncryptionsSame() ? vpnConfig.getEncryptions().get(0) : "";
		m.put("encryptions", xenicsConfig.getCipherConfig(encryption));

		m.put("internal_ip", internalIp);
		m.put("protocol", currentAccessGateway.getProtocol().toLowerCase());
		m.put("port", vpnConfig.getSslPort());
		m.put("password_reset_msg", resetMsg);
		m.put("id_label", currentAccessGateway.getIdLabel());
		m.put("cert_dialog_image", currentAccessGateway.getCertDialogImage());
		m.put("page_title", currentAccessGateway.getPageTitle() == null ? null : currentAccessGateway.getPageTitle().trim());

		if (vpnConfig.isUseObfuscationKey())
			m.put("obfuscation_key", vpnConfig.getObfuscationKey());

		m.put("device_key_type", currentAccessGateway.getDeviceKeyType());

		if (cluster.getConfig().isEnabled()) {
			List<Object> nodes = new ArrayList<Object>();
			Collection<Integer> tunnelCountList = new ArrayList<Integer>();
			cluster.updateClusterTunnelCount();
			for (ClusterNode node : cluster.getClusterNodes()) {
				try {
					Map<String, Object> nodeMap = new HashMap<String, Object>();
					nodeMap.put("ip", node.getPublicIp().getHostAddress());
					// TODO: 추후 화면에 설정 추가하여 바꿔야 함
					nodeMap.put("tunnel_port", 4886);
					tunnelCountList.add(node.getTunnelCount());
					nodes.add(nodeMap);
				} catch (Throwable t) {
					logger.error("frodo core: cannot add node info to vpninfo", t);
				}
			}

			// 현재 장비 터널수 등록
			int currentTunnelCount = xenicsService.getTotalNumOfTunnels();
			tunnelCountList.add(currentTunnelCount);
			int tunnelCountMin = Collections.min(tunnelCountList);
			
			Boolean useLoadBalancing = cluster.getConfig().getUseLoadBalancing();
			if(useLoadBalancing != null && useLoadBalancing)	//active-active 해서 터널 접속 수를 나누려면 이 설정을 두 장비에 모두 true로 설정해야 한다.
				m.put("accept", currentTunnelCount <= tunnelCountMin ? true : false);	//active-standby 인 경우 false가 리턴되어 터널이 안붙는 문제 발생.
			m.put("cluster", nodes);

		}

		ExternalVpn externalVpn = currentAccessGateway.getExternalVpn();
		if (externalVpn != null && externalVpn.getType() != null) {
			if (externalVpn.getType().equals("client"))
				m.put("external_vpn_process", externalVpn.getAddress());
			else if (externalVpn.getType().equals("url")) {
				List<Map<String, Object>> retUrls = new ArrayList<Map<String, Object>>();
				for (Map<String, Object> urls : externalVpn.getAddress()) {
					Map<String, Object> m2 = new HashMap<String, Object>();
					for (String title : urls.keySet()) {
						m2.put("title", title);
						m2.put("url", urls.get(title));
					}
					retUrls.add(m2);
				}
				m.put("external_vpn_url", retUrls);
			}
		}

		m.put("disaster_recovery_list", currentAccessGateway.getDisasterRecoveryList());
		m.put("use_client_type", currentAccessGateway.getUseClientType());
		m.put("use_packet_compress", vpnConfig.getUsePacketCompress());

		if (currentAccessGateway.getDeviceExpireMsg() != null && !currentAccessGateway.getDeviceExpireMsg().trim().isEmpty())
			m.put("device_expire_msg", currentAccessGateway.getDeviceExpireMsg());

		m.put("use_auto_reconnect", currentAccessGateway.getUseAutoReconnect());
		m.put("use_tcp_acceleration", vpnConfig.getUseTcpAcceleration());
		m.put("web_proxy_port", currentAccessGateway.getWebProxyPort());
		m.put("use_password_reset", currentAccessGateway.getUsePasswordReset());
		m.put("use_auth_center", currentAccessGateway.getUseAuthCenter());
		m.put("client_comment", currentAccessGateway.getClientComment());
		
		return m;
	}

	private String getInternalIp() {
		String bridgeTunIp = bridgeCache.getBridgedTunIp();
		if (bridgeTunIp != null)
			return bridgeTunIp;

		Inet4Address internalIp = null;
		try {
			NetworkInterface iface = NetworkInterface.getByName("tun0");
			if (iface == null) {
				logger.error("frodo core: tun0 address not set");
				return null;
			}

			Enumeration<InetAddress> inetAddresses = iface.getInetAddresses();
			while (inetAddresses.hasMoreElements()) {
				InetAddress inetAddr = inetAddresses.nextElement();
				if (inetAddr instanceof Inet4Address) {
					internalIp = (Inet4Address) inetAddr;
				}
			}
			if (internalIp == null) {
				logger.error("frodo core: cannot obtain ipv4 address of tun0");
				return null;
			}
		} catch (SocketException e) {
			logger.error("frodo core: cannot obtain information of tun0", e);
			return null;
		}

		return internalIp.getHostAddress();
	}

}
