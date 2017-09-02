package kr.co.future.sslvpn.core.servlet;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Invalidate;
import org.apache.felix.ipojo.annotations.Requires;
import org.apache.felix.ipojo.annotations.Validate;

import kr.co.future.sslvpn.core.AuthCode;
import kr.co.future.sslvpn.core.AuthService;
import kr.co.future.sslvpn.core.login.LoginUtil;
import kr.co.future.sslvpn.core.servlet.IosServlet;
import kr.co.future.sslvpn.model.api.VpnServerConfigApi;
import kr.co.future.httpd.HttpContext;
import kr.co.future.httpd.HttpService;

import org.json.JSONConverter;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(name = "frodo-ios-servlet")
public class IosServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;
	private final Logger logger = LoggerFactory.getLogger(IosServlet.class);

	@Requires
	private HttpService httpd;
	
	@Requires
	private AuthService auth;
	
	@Requires
	private VpnServerConfigApi vpnServerConfigApi;
	
	private BundleContext bc;

	public IosServlet(BundleContext bc) {
		this.bc = bc;
	}

	@Validate
	public void start() {
		HttpContext ctx = httpd.ensureContext("frodo");
		ctx.addServlet("ios", this, "/ios/*");
	}

	@Invalidate
	public void stop() {
		if (httpd != null) {
			HttpContext ctx = httpd.ensureContext("frodo");
			ctx.removeServlet("ios");
		}
	}

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		resp.setContentType("text/xml;charset=utf-8");
		PrintWriter out = null;
		try {
			out = resp.getWriter();
			String host = req.getHeader("Host");

			if (host == null) {
				host = req.getLocalAddr();
				logger.trace("frodo core: IosServlet, request header does not exist, replace local address [{}]", host);
			}

			String pathInfo = req.getPathInfo();
			if (pathInfo == null) {
				resp.sendError(404);
				return;
			}

			if (!pathInfo.equals("/openvpnconnect-test.mobileconfig"))
				resp.setContentType("text/xml;charset=utf-8");
			if (pathInfo.equals("/SSLplus.mobileconfig")) {
				logger.trace("frodo core: generating mobileconfig of device [{}]", host);
				String result = readXml("mobileconfig.xml").replaceFirst("%DEVICE_IP%", host);
				out.print(result);
				return;
			} else if (pathInfo.equals("/SSLplus.plist")) {
				logger.trace("frodo core: generating plist of device [{}]", host);
				String result = readXml("plist.xml").replaceFirst("%DEVICE_IP%", host);
				result = result.replaceFirst("%IPA_VERSION%", "SSLplus");
				out.print(result);
				return;
			} else if (pathInfo.equals("/SSLplus4.plist")) {
				logger.trace("frodo core: generating version 4 plist of device [{}]", host);
				String result = readXml("plist.xml").replaceFirst("%DEVICE_IP%", host);
				result = result.replaceFirst("%IPA_VERSION%", "SSLplus4");
				out.print(result);
				return;
			} else if (pathInfo.equals("/openvpnconnect-test.mobileconfig")) {
//				String loginName = req.getParameter("login_name");
//				String password = req.getParameter("password");
//				// id password 확인 후 정상 인증 된 경우 설정파일 다운로드  
//				AuthCode authCode = auth.checkPassword(loginName, password);
//				if (authCode.getCode() != 0) {
//					// 인증 실패시 예외 처리
//					out.print("fail.");
//					return;
//				}
				resp.setContentType("application/x-apple-aspen-config;charset=utf-8");
//				resp.setHeader("Content-Disposition", "attachment; filename=\"openvpnconnect-test.mobileconfig\"");
				logger.trace("frodo core: generating openvpn config of device [{}]", host);
				String result = readXml("openvpn-mobileconfig.xml").replaceFirst("%LOGINNAME%", "test").replaceAll("%DEVICE_IP%", host);
				result = result.replaceFirst("%PASSWORD%", "future_01");
				result = result.replaceAll("%PORT%", "4885");
				result = result.replaceAll("%CIPHER%", "aes-128-cbc");
				if (logger.isTraceEnabled())
					logger.trace("frodo core: generated xml [{}]", result);
				out.print(result);
				return;
			}
		} finally {
			if (out != null)
				out.close();
		}
	}
	
	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		PrintWriter out = null;
		if (req.getPathInfo() == null) {
			resp.sendError(404);
			return;
		}
		try {
			out = resp.getWriter();
			String host = req.getHeader("Host");
			if (req.getPathInfo().equals("/openvpnconnect.mobileconfig")) {
				resp.setContentType("text/xml;charset=utf-8");
				String loginName = req.getParameter("login_name");
				String password = req.getParameter("password");
				// id password 확인 후 정상 인증 된 경우 설정파일 다운로드  
				AuthCode authCode = auth.checkPassword(loginName, password);
				boolean useIOS = vpnServerConfigApi.getCurrentIOSVpnServerConfig().getUseIOS() && auth.determineProfile(loginName).getUseIOS();
				if (authCode.getCode() != 0 || !useIOS) {
					logger.info("frodo core: ios not accepted for [{}]", loginName);
					// 인증 실패시 예외 처리
					resp.sendError(500);
					return;
				}
				resp.setContentType("application/x-apple-aspen-config;charset=utf-8");
				logger.trace("frodo core: generating openvpn config of device [{}]", host);
				String result = readXml("openvpn-mobileconfig.xml").replaceFirst("%LOGINNAME%", loginName).replaceAll("%DEVICE_IP%", host);
				result = result.replaceFirst("%PASSWORD%", password);
				result = result.replaceAll("%PORT%", String.valueOf(vpnServerConfigApi.getCurrentIOSVpnServerConfig().getSslPort()));
				String encryption = vpnServerConfigApi.getCurrentIOSVpnServerConfig().getEncryptions().get(0);
				String cipher = "";
				if (encryption.equals("AES128"))
					cipher = "aes-128-cbc";
				else if (encryption.equals("AES256"))
					cipher = "aes-256-cbc";
				else if (encryption.equals("AES192"))
					cipher = "aes-192-cbc";
				
				result = result.replaceAll("%CIPHER%", cipher);
				if (logger.isTraceEnabled())
					logger.trace("frodo core: generated xml [{}]", result);
				out.print(result);
				return;
			} else if (req.getPathInfo().equals("/check_password")){
//				resp.setContentType("text/json;charset=utf-8");
				String loginName = req.getParameter("login_name");
				String password = req.getParameter("password");
				// id password 확인 후 정상 인증 된 경우 설정파일 다운로드  
				AuthCode authCode = null;
				Map<String, Object> result = new HashMap<String, Object>();
				
				if ((auth.determineProfile(loginName).getUseIOS() != null ? auth.determineProfile(loginName).getUseIOS() : false) 
						&& vpnServerConfigApi.getCurrentIOSVpnServerConfig() != null 
						&& vpnServerConfigApi.getCurrentIOSVpnServerConfig().getUseIOS() != null ? vpnServerConfigApi.getCurrentIOSVpnServerConfig().getUseIOS() : false){
					authCode = auth.checkPassword(loginName, password);
					
				} else {
					authCode = AuthCode.IOSNoUse;
				}
				result.put("auth_code", authCode.getCode());
				result.put("msg", authCode.getStatus());
				out.write(JSONConverter.jsonize(result));
				out.close();
				return;
			}
		} catch (Exception e) {
			logger.error("frodo core: cert center servlet error", e);
			resp.sendError(500);
		} finally {
			if (out != null)
				out.close();
		}
	}

	private String readXml(String fileName) throws IOException {
		String xmlString = "";
		Bundle bundle = bc.getBundle();
		BufferedReader br = null;

		URL resource = bundle.getResource("ios/" + fileName);
		try {
			InputStream is = resource.openStream();
			br = new BufferedReader(new InputStreamReader(is, "utf-8"));
			String xmlLine;
			while ((xmlLine = br.readLine()) != null) {
				xmlString += xmlLine + "\n";
			}

			return xmlString;
		} finally {
			if (br != null)
				br.close();
		}
	}
}
