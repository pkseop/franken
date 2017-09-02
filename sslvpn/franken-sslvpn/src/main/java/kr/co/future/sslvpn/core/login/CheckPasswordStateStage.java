package kr.co.future.sslvpn.core.login;

import java.security.MessageDigest;
import java.util.Date;

import kr.co.future.sslvpn.core.AuthCode;
import kr.co.future.sslvpn.model.AccessGateway;
import kr.co.future.sslvpn.model.AccessProfile;
import kr.co.future.sslvpn.model.UserExtension;
import kr.co.future.sslvpn.model.AccessGateway.IdentificationMode;
import kr.co.future.sslvpn.core.impl.SaltGenerator;
import kr.co.future.sslvpn.core.pipeline.BaseError;
import kr.co.future.sslvpn.core.pipeline.PipelineContext;
import kr.co.future.sslvpn.core.pipeline.Stage;
import kr.co.future.sslvpn.core.servlet.AuthServiceServlet;
import kr.co.future.dom.model.User;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CheckPasswordStateStage extends Stage{

	private final Logger logger = LoggerFactory.getLogger(AuthServiceServlet.class.getName());
	
	@Override
	public void doExecute(PipelineContext context) {
		LoginContext loginContext = (LoginContext)context;
		LoginUtil loginUtil = loginContext.loginUtil;
		
		String loginName = loginContext.getLoginName();
		AccessGateway gw = loginContext.getGw();
		User user = loginContext.getUser();
		UserExtension ext = loginContext.getUserExt();
		AuthCode authCode = loginContext.getAuthCode();
		String pw = loginContext.getPw();
		AccessProfile profile = loginContext.getProfile();
		
		try{
			// enforce password expiry
			if (loginUtil.isLoginMethodWithPassword(gw, user, loginName) && !authCode.getStatus().equals("password-fail")) {
				if (ext.getSalt() == null)
					ext.setSalt(SaltGenerator.createSalt(20));

				MessageDigest md = MessageDigest.getInstance("SHA-256");
				md.update(ext.getSalt().getBytes("utf-8"));
				md.update(pw.getBytes());
				String hash = loginUtil.toHex(md.digest());

				if (ext.getLastPasswordChange() == null || ext.getLastPasswordHash() == null
						|| !ext.getLastPasswordHash().equals(hash)) {
					ext.setLastPasswordChange(new Date());
					ext.setLastPasswordHash(hash);
				}

				String sourceType = user.getSourceType();
				boolean isUseAuth = false;
				if (sourceType != null) {
					if (sourceType.equals("external") && loginUtil.externalAuth.isEnabled() && loginUtil.externalAuth.isPasswordExpirySupported()) {
						isUseAuth = true;
						if (loginUtil.externalAuth.getPasswordExpiry(loginName) <= 0) {
							loginContext.setResult(loginUtil.fail(AuthCode.PasswordExpired, profile));
							return;
						}
					} else if (sourceType.equals("sql") && loginUtil.sqlAuth.isEnabled()) {
//						updateUserWithSqlAuth(loginUtil, user, ext);
						if(loginUtil.sqlAuth.isPasswordExpirySupported()) {
							isUseAuth = true;
							if (loginUtil.sqlAuth.getPasswordExpiry(loginName) <= 0) {
								loginContext.setResult(loginUtil.fail(AuthCode.PasswordExpired, profile));
								return;
							}
						}
					}
				}

				// skip if password expiry is 0
				if (profile.getPasswordExpiry() != 0 && !isUseAuth) {
					long interval = new Date().getTime() - ext.getLastPasswordChange().getTime();
					long expiry = profile.getPasswordExpiry();
					if (interval > expiry * 86400 * 1000) {
						Object[] args = new Object[] { loginName, expiry, ext.getLastPasswordChange() };
						logger.debug("frodo core: password expired for login [{}], expiry [{}], last change [{}]", args);
						// source type 이 external이고 지원하고 되면
						loginContext.setResult(loginUtil.fail(AuthCode.PasswordExpired, profile));
						return;
					}
				}
			}
			
			// Save the password provided by users using LoginMethod_PW_OTP
			Integer loginMethod = loginUtil.getLoginMethod(gw, loginName, user);
			if (loginMethod == LoginUtil.LoginMethod_PW_OTP && (user.getPassword() == null || user.getPassword() == "")) {
				user.setPassword(pw);
				loginUtil.domUserApi.updateUser("localhost", user, true);
			}
			
			String statusCode = authCode.getStatus();
			logger.debug("frodo core: auth code [{}, {}]", authCode, statusCode);
			if (!statusCode.equals("success")) {
				// update login failure (only if user failure)
				if (statusCode.equals("password-fail") || statusCode.equals("otp-fail")) {
					ext.setLoginFailures(ext.getLoginFailures() + 1);
					ext.setLastPasswordFailTime(new Date());

					logger.debug("frodo core: passwd fail count {}/{}", ext.getLoginFailures(), profile.getFailLimitCount());
					if (profile.isUseFailLimit() && profile.getFailLimitCount() <= ext.getLoginFailures()) {
						logger.debug("frodo core: user {} locked", loginName);
						ext.setLocked(true);
					}

					loginUtil.submitUpdate(ext.getUser());

					loginContext.setResult(loginUtil.fail(authCode, profile, ext.getLoginFailures()));
					return;
				}

				// 만약 기존과 다른 공인인증서로 로그인한 경우 npki-fail이 날텐데 이 부분에서 교체 등록할 기회를 준다.
				if (authCode != AuthCode.NpkiFail || gw.getIdentificationMode() != IdentificationMode.SubjectDn) {
					loginContext.setResult(loginUtil.fail(authCode, profile));
					return;
				}
			}
		} catch (Exception e) {
			BaseError error = new BaseError("login fail", "cannot login", e);
			loginContext.addError(error);
		}
	   
   }
	
	//pks: 2013-12-10. To update idn info when using sql authentication.
	//pks: 2015-06-25. do not need to save idn info.
//	private void updateUserWithSqlAuth(LoginUtil loginUtil, User user, UserExtension ext) {
//		try{
//			String loginName = user.getLoginName();
//			String remoteIdn = loginUtil.sqlAuth.getIdn(loginName);
//			if(remoteIdn != null) {
//				if (ext.getSalt() == null) {
//					ext.setSalt(loginName);
//				}
//				remoteIdn = remoteIdn.replace("-", "");
//				String idnHash = loginUtil.domUserApi.hashPassword(ext.getSalt(), remoteIdn);
//				
//				ext.setCertType("personal");
//				ext.setIdnHash(idnHash);
//				ext.setUser(user);
//				ext.setUpdateDateTime(new Date());
//				user.getExt().put("frodo", ext);	
//				loginUtil.domUserApi.updateUser("localhost", user, false);
//			}
//		} catch (Exception e) {
//			logger.info("frodo core: can not update idn info by sql auth.");			
//		}
//	}

}
