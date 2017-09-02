package kr.co.future.sslvpn.auth.kibo.impl;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.apache.felix.ipojo.annotations.Validate;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import kr.co.future.api.PrimitiveConverter;
import kr.co.future.confdb.Config;
import kr.co.future.confdb.ConfigDatabase;
import kr.co.future.confdb.ConfigService;
import kr.co.future.ldap.LdapProfile;
import kr.co.future.ldap.LdapService;
import kr.co.future.ldap.LdapUser;
import kr.co.future.rpc.RpcException;
import kr.co.future.sslvpn.auth.BaseExternalAuthApi;
import kr.co.future.sslvpn.auth.kibo.KiboAuthApi;
import kr.co.future.sslvpn.auth.kibo.KiboConfig;
import kr.co.future.sslvpn.auth.kibo.ResultBean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import MTransKeySrvLib.MTransKeySrv;

@Component(name = "kibo-auth-service")
@Provides
public class KiboAuthService extends BaseExternalAuthApi implements KiboAuthApi {

	private final Logger logger = LoggerFactory.getLogger(KiboAuthService.class.getName());

	private String USER_AGENT = "MGUARD_WEB_API";

	private KiboConfig config;

	// MDM, OTP 로그인에 사용되는 공통 주소
	private String loginUrl = "/jsonService/login";

	// JSON RPC 서비스 호출용 주소
	private String serviceUrl = "/jsonService/service.do?ltf_outputMessageType=json";

	@Requires
	private ConfigService conf;

	@Requires
	private LdapService ldapApi;

	@Validate
	public void start() {
		// 번들 시작시 설정파일 읽어서 객체 할당
		this.config = getConfig();
	}

	final static HostnameVerifier DO_NOT_VERIFY = new HostnameVerifier() {
		@Override
		public boolean verify(String hostname, SSLSession session) {
			return true;
		}
	};

	@Override
	public Object login(Map<String, Object> props) {
		if (config == null) {
			logger.warn("frodo-auth-kibo: config not set");
			throw new RpcException("config not set");
		}

		String loginName = (String) props.get("id");
		String password = (String) props.get("pw");
		String otp = (String) props.get("otp");
		String deviceKey = (String) props.get("device_key");
		String decryptPassword = decryptKey(password);

		logger.trace("frodo core: kibo auth, loginName=[{}], pw=[{}], otp=[{}], device_key=[{}]", new Object[] { loginName,
				decryptPassword, otp, deviceKey });

		Map<String, Object> m = new HashMap<String, Object>();
		Map<String, Object> verify = new HashMap<String, Object>();

		// verify user -> active directory check
		verify = verifyUser(loginName);
		boolean isSuccess = (Boolean) verify.get("result");
		if (isSuccess) {
			m.put("name", verify.get("name"));
			m.put("org_unit_name", verify.get("org_unit_name"));

			logger.trace("frodo core: kibo auth, ldap success, name=[{}], org unit=[{}]", verify.get("name"),
					verify.get("org_unit_name"));
		} else {
			// user not found
			m.put("auth_code", 1);
			logger.trace("frodo core: kibo auth, ldap verify user [{}] fail", loginName);
			return m;
		}

		// check device key -> MDM
		verify = verifyDevice(deviceKey);
		boolean result = (Boolean) verify.get("success");

		if (!result) {
			// return device_fail
			m.put("auth_code", 20);
			logger.trace("frodo-core: kibo auth, MDM verify fail");
			return m;
		}

		logger.trace("frodo-core: kibo auth, MDM verify success");

		// check otp value -> OTP
		verify = verifyOtp(deviceKey, otp);
		result = (Boolean) verify.get("success");

		if (!result) {
			// return otp fail
			m.put("auth_code", 25);
			logger.trace("frodo-core: kibo auth, OTP verify fail");
			return m;
		}

		logger.trace("frodo-core: kibo auth, OTP verify success");

		// check active directory (using decrypted password)
		LdapProfile profile = ldapApi.getProfile("kibo");
		Boolean verifyPassword = ldapApi.verifyPassword(profile, loginName, decryptPassword);

		if (!verifyPassword) {
			// return password fail
			m.put("auth_code", 2);
			logger.trace("frodo-core: kibo auth, AD verify password fail");
			return m;
		}

		logger.trace("frodo-core: kibo auth, AD verify password success");

		m.put("auth_code", 0);

		return m;
	}

	@Override
	public String getIdn(String loginName) {
		return null;
	}

	@Override
	public Map<String, Object> verifyUser(String loginName) {
		logger.trace("frodo-auth-kibo: verifying user [{}]", loginName);

		LdapProfile profile = ldapApi.getProfile("kibo");
		LdapUser ldapUser = ldapApi.findUser(profile, loginName);

		Map<String, Object> m = new HashMap<String, Object>();

		if (ldapUser != null) {
			m.put("name", ldapUser.getDisplayName());
			m.put("org_unit_name", ldapUser.getOrganizationUnitName());
			m.put("result", true);
		} else {
			m.put("result", false);
		}

		return m;
	}

	@Override
	public KiboConfig getConfig() {
		ConfigDatabase db = conf.ensureDatabase("frodo-auth-kibo");
		Config c = db.findOne(KiboConfig.class, null);
		if (c != null)
			return c.getDocument(KiboConfig.class);
		return null;
	}

	@Override
	public void setConfig(KiboConfig config) {
		ConfigDatabase db = conf.ensureDatabase("frodo-auth-kibo");
		Config c = db.findOne(KiboConfig.class, null);
		if (c != null) {
			// 이전 저장 설정값 유지 시키며 새로 업데이트
			KiboConfig oldConfig = c.getDocument(KiboConfig.class);
			oldConfig.setUrl(config.getUrl());
			oldConfig.setAdminAccount(config.getAdminAccount());
			oldConfig.setAdminPassword(config.getAdminPassword());
			oldConfig.setLdapAdminAccount(config.getLdapAdminAccount());
			oldConfig.setLdapAdminPassword(config.getLdapAdminPassword());
			oldConfig.setSecureKey(config.getSecureKey());
			c.setDocument(PrimitiveConverter.serialize(oldConfig));
			c.update();
			this.config = oldConfig;
		} else {
			db.add(config);
			this.config = config;
		}

	}

	@Override
	public String decryptKey(String data) {
		byte[] key = config.getSecureKey().getBytes();
		String output = null;

		logger.debug("frodo-auth-kibo: secureKey=[{}], data=[{}]", key.toString(), data);

		try {
			output = MTransKeySrv.Decrypt(key, data);
			logger.debug("frodo-auth-kibo: decryption result=[{}]", output);
		} catch (Exception e) {
			logger.error("frodo-auth-kibo: keypad decrypt fail", e);
		}
		return output;
	}

	@Override
	public Map<String, Object> verifyDevice(String deviceId) {

		URL url = config.getUrl();
		String adminAccount = config.getAdminAccount();
		String adminPassword = config.getAdminPassword();

		// 관리자 계정으로 로그인 인증
		String authCookie = null;
		try {
			authCookie = getAuthCookie(url.toString(), true, adminAccount, adminPassword);
		} catch (Exception e) {
			logger.error("frodo-auth-kibo: admin auth failed error", e);
			throw new RpcException("kibomdm login failed", e);
		}

		Map<String, Object> m = new HashMap<String, Object>();

		// MDM 확인 요청
		Map<String, Object> requestData = new HashMap<String, Object>();
		requestData.put("DEVICE_ID", deviceId);

		// api 요청 - 단말 인증
		try {
			String result = service(url.toString(), true, "mdmdi01", "checkRegisteredDevice", requestData, authCookie);
			JSONObject json = new JSONObject(result);
			m = parse(json);
		} catch (Exception e) {
			m.put("success", false);
			logger.error("frodo-auth-kibo: json to map parse failed error", e);
		}

		return m;

	}

	@Override
	public Map<String, Object> verifyOtp(String deviceId, String otpValue) {

		URL url = config.getUrl();
		String adminAccount = config.getAdminAccount();
		String adminPassword = config.getAdminPassword();

		// 관리자 계정으로 로그인 인증
		String authCookie = null;
		try {
			authCookie = getAuthCookie(url.toString(), true, adminAccount, adminPassword);
		} catch (Exception e) {
			logger.error("frodo-auth-kibo: admin auth failed error", e);
		}

		Map<String, Object> m = new HashMap<String, Object>();

		// OTP 확인 요청
		Map<String, Object> requestData = new HashMap<String, Object>();
		requestData.put("OTP_VALUE", otpValue);
		requestData.put("DEVICE_ID", deviceId);

		try {
			String result = service(url.toString(), true, "mumum01", "getOTPValidate", requestData, authCookie);
			JSONObject json = new JSONObject(result);
			m = parse(json);
		} catch (Exception e) {
			m.put("success", false);
			logger.error("frodo-auth-kibo: json to map parse failed error", e);
		}

		return m;

	}

	private ResultBean connectionHttp(String comUrl, String requestData, String cookieData) throws Exception {

		URL url = new URL(comUrl);
		HttpURLConnection cnx = (HttpURLConnection) url.openConnection();
		cnx.setDoOutput(true);
		cnx.setRequestProperty("user-agent", USER_AGENT);
		cnx.setRequestMethod("POST");

		if (cookieData != null) {
			// 쿠키값 세팅
			cnx.setRequestProperty("Cookie", cookieData);
		}
		cnx.setRequestProperty("Content-Type", "application/json");

		OutputStream os = cnx.getOutputStream();

		// 전송할 데이터 입력(String)
		os.write(requestData.getBytes("utf-8"));
		os.flush();

		BufferedReader rd = null;
		String line = null;
		StringBuffer resp = new StringBuffer();
		try {
			rd = new BufferedReader(new InputStreamReader(cnx.getInputStream(), "utf-8"));
			while ((line = rd.readLine()) != null) {
				resp.append(line);
			}
		} catch (Exception e) {
			logger.error("frodo-auth-kibo: cannot connect to " + comUrl, e);
			throw new RpcException("MDM connect failed", e);
		} finally {
			if (os != null)
				os.close();

			if (rd != null)
				rd.close();
		}

		logger.debug("frodo-auth-kibo: connectionHttp reponsData [{}]", resp);

		if (cnx.getResponseCode() != 200) {
			throw new RpcException("MDM connect failure " + cnx.getResponseCode());
		}

		ResultBean resultBean = new ResultBean();
		resultBean.setMsg(resp.toString());
		resultBean.setHttpURLConnection(cnx);
		return resultBean;

	}

	@SuppressWarnings("unused")
	private static void trustAllHosts() {
		// Create a trust manager that does not validate certificate chains
		TrustManager[] trustAllCerts = new TrustManager[] { new X509TrustManager() {
			public java.security.cert.X509Certificate[] getAcceptedIssuers() {
				return new java.security.cert.X509Certificate[] {};
			}

			@Override
			public void checkClientTrusted(java.security.cert.X509Certificate[] chain, String authType)
					throws java.security.cert.CertificateException {

			}

			@Override
			public void checkServerTrusted(java.security.cert.X509Certificate[] chain, String authType)
					throws java.security.cert.CertificateException {

			}
		} };

		// Install the all-trusting trust manager
		try {
			SSLContext sc = SSLContext.getInstance("TLS");
			sc.init(null, trustAllCerts, new java.security.SecureRandom());
			HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private ResultBean connectionHttps(String comUrl, String requestData, String cookieData) throws Exception {

		URL url = new URL(comUrl);

		// trustAllHosts();

		HttpsURLConnection cnx = (HttpsURLConnection) url.openConnection();
		cnx.setDoOutput(true);
		cnx.setRequestProperty("user-agent", USER_AGENT);
		cnx.setRequestMethod("POST");
		// cnx.setHostnameVerifier(DO_NOT_VERIFY);

		if (cookieData != null) {
			cnx.setRequestProperty("Cookie", cookieData);
		}
		cnx.setRequestProperty("Content-Type", "application/json");

		OutputStream os = cnx.getOutputStream();
		os.write(requestData.getBytes("utf-8"));
		os.flush();

		BufferedReader rd = null;
		String line = null;
		StringBuffer resp = new StringBuffer();
		try {
			rd = new BufferedReader(new InputStreamReader(cnx.getInputStream(), "utf-8"));
			while ((line = rd.readLine()) != null) {
				resp.append(line);
			}
		} catch (Exception e) {
			logger.error("frodo-auth-kibo: cannot connect to " + comUrl, e);
			throw new RpcException("MDM connect failed", e);
		} finally {
			if (os != null)
				os.close();

			if (rd != null)
				rd.close();
		}

		logger.debug("frodo-auth-kibo: connectionHttps reponse data [{}]", resp);

		if (cnx.getResponseCode() != 200) {
			throw new RpcException("Response code = " + cnx.getResponseCode() + ", msg = " + cnx.getResponseMessage());
		}

		ResultBean resultBean = new ResultBean();
		resultBean.setMsg(resp.toString());
		resultBean.setHttpURLConnection(cnx);

		return resultBean;

	}

	/**
	 * 인증 요청
	 * 
	 * @param doamin
	 * @param isSsl
	 *            - https로 연결 = true, http로 연결 = false
	 * @param userId
	 * @param password
	 * @return -- 로그인 성공시 쿠키값 반환
	 * @throws Exception
	 */
	public String getAuthCookie(String domain, boolean isSsl, String userId, String password) throws Exception {

		String url = domain + loginUrl;

		Map<String, Object> body = new HashMap<String, Object>();
		body.put("userid", userId);
		body.put("password", password);

		String requestData = genHeaderDataMap("login", "login", body);

		ResultBean resultBean;

		if (isSsl) {
			resultBean = connectionHttps(url, requestData, null);
		} else {
			resultBean = connectionHttp(url, requestData, null);
		}

		JSONObject jsonObject = new JSONObject(resultBean.getMsg());

		JSONObject headerData = jsonObject.getJSONObject("head");
		boolean isError = headerData.getBoolean("error");
		String responseCode = headerData.getString("responseCode");
		String responseMsg = headerData.getString("responseMsg");

		if (isError) {
			throw new RpcException("Login Fail -- :responseCode:" + responseCode + "   message:" + responseMsg);
		} else {
			HttpURLConnection cnx = resultBean.getHttpURLConnection();

			List<String> cookieList = cnx.getHeaderFields().get("Set-Cookie");
			String cookieString = "";
			for (String data : cookieList) {
				cookieString = cookieString + data + ";";
			}

			logger.debug("frodo-auth-kibo: login authCookie [{}]", cookieString);
			return cookieString;
		}

	}

	/**
	 * API 요청
	 * 
	 * @param domain
	 * @param isSsl
	 *            - https로 연결 = true, http로 연결 = false
	 * @param serviceName
	 * @param commandName
	 * @param requestData
	 * @param cookieData
	 * @return - 처리 결과 JSON 형태 입니다.
	 * @throws Exception
	 */
	public String service(String domain, boolean isSsl, String serviceName, String commandName, Map<String, Object> requestData,
			String cookieData) throws Exception {
		String url = domain + serviceUrl;

		String requestDataString = genHeaderDataMap(serviceName, commandName, requestData);
		ResultBean resultBean;

		if (isSsl) {
			resultBean = connectionHttps(url, requestDataString, cookieData);
		} else {
			resultBean = connectionHttp(url, requestDataString, cookieData);
		}

		JSONObject jsonObject = new JSONObject(resultBean.getMsg());

		JSONObject headerData = jsonObject.getJSONObject("head");
		boolean isError = headerData.getBoolean("error");
		String responseCode = headerData.getString("responseCode");
		String responseMsg = headerData.getString("responseMsg");

		if (isError) {
			throw new Exception("Service Fail -- :servie:" + serviceName + "   commandName:" + commandName + "   responseCode:"
					+ responseCode + "   message:" + responseMsg);
		} else {
			if (jsonObject.get("body") != null) {
				return jsonObject.getString("body");
			} else {
				return "";
			}
		}
	}

	/**
	 * 기본 헤더 데이터 생성
	 * 
	 * @param service
	 * @param command
	 * @param body
	 * @return
	 */
	private String genHeaderDataMap(String service, String command, Map<String, Object> body) {
		Map<String, Object> header = new HashMap<String, Object>();
		header.put("responseCode", "");
		header.put("error", false);
		header.put("errorType", "");
		header.put("requestTime", "");
		header.put("command", command);
		header.put("service", service);
		header.put("responseCode", "");
		header.put("responseMsg", "");
		header.put("responseTime", "");

		Map<String, Object> msg = new HashMap<String, Object>();
		msg.put("head", header);
		if (body != null)
			msg.put("body", body);

		String data = new JSONObject(msg).toString();
		logger.debug("frodo-auth-kibo: Send Message [{}]", data);

		return data;
	}

	private Map<String, Object> parse(JSONObject jsonObject) throws IOException {
		Map<String, Object> m = new HashMap<String, Object>();
		String[] names = JSONObject.getNames(jsonObject);
		if (names == null)
			return m;

		for (String key : names) {
			try {
				Object value = jsonObject.get(key);
				if (value == JSONObject.NULL)
					value = null;
				else if (value instanceof JSONArray)
					value = parse((JSONArray) value);
				else if (value instanceof JSONObject)
					value = parse((JSONObject) value);

				m.put(key, value);
			} catch (JSONException e) {
				logger.error("frodo-auth-kibo: cannot parse json", e);
				throw new IOException(e);
			}
		}

		return m;
	}

	private Object parse(JSONArray jsonarray) throws IOException {
		List<Object> list = new ArrayList<Object>();
		for (int i = 0; i < jsonarray.length(); i++) {
			try {
				Object o = jsonarray.get(i);
				if (o == JSONObject.NULL)
					list.add(null);
				else if (o instanceof JSONArray)
					list.add(parse((JSONArray) o));
				else if (o instanceof JSONObject)
					list.add(parse((JSONObject) o));
				else
					list.add(o);
			} catch (JSONException e) {
				logger.error("frodo-auth-kibo: cannot parse json", e);
				throw new IOException(e);
			}
		}
		return list;
	}

	@Override
	public String getSubjectDn(String loginName) {
		throw new UnsupportedOperationException("subject_dn not supported");
	}

	@Override
	public boolean isPasswordChangeSupported() {
		return false;
	}

	@Override
	public void changePassword(String account, String newPassword) {
		throw new UnsupportedOperationException("cannot change password");
	}
}
