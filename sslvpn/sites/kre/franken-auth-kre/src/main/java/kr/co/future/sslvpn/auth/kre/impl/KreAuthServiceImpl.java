package kr.co.future.sslvpn.auth.kre.impl;

import java.util.HashMap;
import java.util.Map;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.apache.felix.ipojo.annotations.Validate;

import kr.co.future.confdb.Config;
import kr.co.future.confdb.ConfigDatabase;
import kr.co.future.confdb.ConfigService;
import kr.co.future.dom.api.UserApi;
import kr.co.future.dom.model.User;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import WiseAccess.SSO;
import WiseAccess.SsoAuthInfo;
import WiseAccess.SsoParser;
import kr.co.future.sslvpn.auth.BaseExternalAuthApi;
import kr.co.future.sslvpn.auth.kre.KreAuthConfig;
import kr.co.future.sslvpn.auth.kre.KreAuthService;

@Component(name = "kre-auth-service")
@Provides
public class KreAuthServiceImpl extends BaseExternalAuthApi implements KreAuthService {
	private final Logger logger = LoggerFactory.getLogger(KreAuthServiceImpl.class.getName());

	@Requires
	private ConfigService conf;

	@Requires
	private UserApi domUserApi;
	
	private SSO sso;
	private KreAuthConfig config;

	private Map<String, String> tokenMap;

	@Validate
	public void start() {
		ConfigDatabase db = conf.ensureDatabase("frodo");
		Config c = db.findOne(KreAuthConfig.class, null);
		if (c == null) {
			config = null;
			sso = null;
		} else {
			config = c.getDocument(KreAuthConfig.class);
			sso = new SSO(config.getSsoApiKey());
		}
		tokenMap = new HashMap<String, String>();
	}

	@Override
	public Object login(Map<String, Object> props) {
		String loginName = (String) props.get("id");
		String password = (String) props.get("pw");

		logger.info("loginName : " + loginName + ", password : " + password);
		
		Map<String, Object> result = new HashMap<String, Object>();
		User user = domUserApi.findUser("localhost", loginName);
		//logger.info("user.getLoginName() : " + user.getLoginName() + ", user.getPassword() : " + user.getPassword());
		
		if (user == null) {
			result.put("auth_code", 1);
			logger.info("user null");
			return result;
		}

		if (!domUserApi.verifyPassword("localhost", loginName, password)) {
			result.put("auth_code", 2);
			logger.info("user password not match");
			return result;
		}

		result.put("org_unit_name", user.getOrgUnit() == null ? null : user.getOrgUnit().getName());
		result.put("name", user.getName());
		result.put("auth_code", 0);
		return result;
	}

	@Override
	public Map<String, Object> verifyUser(String loginName) {
		Map<String, Object> result = new HashMap<String, Object>();
		User user = domUserApi.findUser("localhost", loginName);
		if (user == null)
			result.put("result", false);
		else
			result.put("result", true);

		return result;
	}
	
	private final static String LOGIN_REGULAR_EXPRESSION = "^[_a-z0-9-]+(.[_a-z0-9-]+)*@[a-z0-9-]+(.[a-z0-9-]+)*$";

	@Override
	public boolean verifySso(String loginName, String clientIp) {
		Map<String, Object> result = new HashMap<String, Object>();

		if (!loginName.matches(LOGIN_REGULAR_EXPRESSION)) {
			return true;
		}
		
		if (sso == null) {
			logger.error("frodo auth kre: cannot found sso config.");
			return false;
		}

		if (config.isUseDummy())
			return dummyVerifyUser(loginName);
		
		SsoAuthInfo ssoInfo = null;
		for (String remoteIp : config.getSsoRemoteIps()) {
			sso.setHostName(remoteIp);
			ssoInfo = sso.authDN(loginName, true, clientIp);
			int resultCode = sso.getLastError();

			if (resultCode < 0) {
				if (resultCode == -729) {
					logger.debug("frodo auth kre: cannot connect sso server [{}]", remoteIp);
					continue;
				} else {
					logger.error("frodo auth kre: verify fail. login_name [{}], result code [{}], result msg [{}]", new Object[] {
							loginName, resultCode, sso.getLastErrorMsg() });
					return false;
				}
			}
			String token = makeSsoToken(ssoInfo, clientIp);//pks: 2013-08-16. sso token setting changed. //ssoInfo.getToken();
			tokenMap.put(loginName, token);

			return true;
		}

		logger.debug("frodo auth kre: cannot connect all sso server");
		return false;
	}

	@Override
	public boolean dummyVerifyUser(String loginName) {
		Map<String, Object> result = new HashMap<String, Object>();
		if (sso == null) {
			logger.error("frodo auth kre: cannot found sso config.");
			return false;
		}

		for (String remoteIp : config.getSsoRemoteIps()) {
			sso.setHostName(remoteIp);
			logger.error("frodo auth kre: try verify user. login_name [{}], host_name [{}], api_key [{}]", new Object[] {
					loginName, remoteIp, config.getSsoApiKey() });

			int resultCode = -1;
			resultCode = sso.regUserSession(loginName, remoteIp, true);

			if (resultCode < 0) {
				if (resultCode == -729) {
					logger.debug("frodo auth kre: cannot connect sso server [{}]", remoteIp);
					continue;
				} else {
					logger.error("frodo auth kre: verify fail. login_name [{}], result code [{}], result msg [{}]", new Object[] {
							loginName, resultCode, resultCode });
					return false;
				}
			}
		}

		result.put("token", sso.getToken());
		tokenMap.put(loginName, sso.getToken());
		return true;
	}

	@Override
	public boolean useSso() {
		return true;
	}

	@Override
	public String getSsoToken(String loginName) {
		if (!loginName.matches(LOGIN_REGULAR_EXPRESSION))
			return null;
		return tokenMap.get(loginName);
	}

	@Override
	public void setSsoConfig(KreAuthConfig config) {
		ConfigDatabase db = conf.ensureDatabase("frodo");
		Config c = db.findOne(KreAuthConfig.class, null);
		if (c == null)
			db.add(config);
		else
			db.update(c, config);

		this.config = config;
		sso = new SSO(config.getSsoApiKey());
	}

	@Override
	public KreAuthConfig getSsoConfig() {
		return config;
	}
	
	//pks: 2013-08-16. sso token setting changed.
	private String makeSsoToken(SsoAuthInfo ssoInfo, String sRemoteAddr) {
		String sessionToken = ssoInfo.getToken() ;
		String suid = ssoInfo.getUserId();
		String suname = ssoInfo.getUserName();
		String sProfile = ssoInfo.getProfile();

		// 사용자 PROFILE에 있는 정보를 가져온다.
		SsoParser parser = new SsoParser();
		parser.setQuery(sProfile);
		// 필요한 값만 PUTVALUE한다. ----------------------------
		sso.putValue("EMP_NO",suid); // 사번
		sso.putValue("EMP_NM",suname); // 이름
		sso.putValue("JBLV_CD",parser.search("JBLV_CD")); // 직급코드
		sso.putValue("DEPT_CD",parser.search("DEPT_CD")); // 부서코드
		sso.putValue("DEPT_NM",parser.search("DEPT_NM")); // 부서명
		sso.putValue("JOB_CD",parser.search("JOB_CD")); // 직무코드
		sso.putValue("JOB_NM",parser.search("JOB_NM")); // 직무명
		sso.putValue("POST_CD",parser.search("POST_CD")); // 직위코드
		sso.putValue("POST_NM",parser.search("POST_NM")); // 직위명
		sso.putValue("DUTY_CD",parser.search("DUTY_CD")); // 직책코드
		sso.putValue("DUTY_NM",parser.search("DUTY_NM")); // 직책명
		sso.putValue("FRST_EMP_NO",parser.search("FRST_EMP_NO")); // 직책명
		sso.putValue("EMP_DV_CD",parser.search("EMP_DV_CD")); //사원구분코드(외부직원)
	
		String token = sso.makeToken(3, sessionToken, "GID_DEMO1",	sRemoteAddr);
		return token;
	}
	
	@Override
	public boolean isPasswordChangeSupported() {
		return true;
	}
	
	@Override
	public void changePassword(String account, String newPassword) {
		User user = domUserApi.getUser("localhost", account);
		
		Map<String, Object> ext = user.getExt();
		@SuppressWarnings("unchecked")
		Map<String, Object> frodo = (Map<String, Object>) ext.get("frodo");
		frodo.put("force_password_change", false);
		ext.put("frodo", frodo);
		user.setExt(ext);
		user.setPassword(newPassword);
		
		domUserApi.updateUser("localhost", user, true);
	}
}
