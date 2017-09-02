package kr.co.future.sslvpn.core.login;

import kr.co.future.sslvpn.core.AuthCode;
import kr.co.future.sslvpn.model.AccessGateway;
import kr.co.future.sslvpn.core.pipeline.BaseError;
import kr.co.future.sslvpn.core.pipeline.PipelineContext;
import kr.co.future.sslvpn.core.pipeline.Stage;
import kr.co.future.sslvpn.core.servlet.AuthServiceServlet;
import kr.co.future.dom.model.User;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CheckPwLoginStage extends Stage {
	private final Logger logger = LoggerFactory.getLogger(AuthServiceServlet.class.getName());
	
	private final String PasswordCA = "sslplus-ca";
	private final String PrivateCertCN = "local";
	
	@Override
	public void doExecute(PipelineContext context) {
		LoginContext loginContext = (LoginContext)context;
		LoginUtil loginUtil = loginContext.loginUtil;
		
		String loginName = loginContext.getLoginName();
		String issuerCN = loginContext.getIssuerCN();
		String password = loginContext.getPw();
		AccessGateway gw = loginContext.getGw();
		User user = loginContext.getUser();
		
		try{
			Integer loginMethod = loginUtil.getLoginMethod(gw, loginName, user);
			loginContext.setLoginMethod(loginMethod);
			
			if (isPolicyMismatch(issuerCN, loginName, loginMethod, password)) {
				loginContext.setAuthCode(AuthCode.PolicyMismatch);
				loginContext.setPwAuthOk(true);
				return;
			}

			AuthCode pwAuthCode = null;

			if (password != null && loginUtil.isLoginMethodWithPassword(gw, user, loginName)) {
				// Peter H. Nahm - user를 넘겨주지 않아 인증 결과로 업데이트된 사용자 속성들이 DB에서 사라짐.
				//pwAuthCode = checkPasswordLogin(props);
				pwAuthCode = loginUtil.checkPasswordLogin(loginContext.props, user);
				logger.trace("frodo core: password login result for [{}] -> {}", loginName, pwAuthCode);

				if (pwAuthCode.getCode() != 0) {
					loginContext.setAuthCode(pwAuthCode);					
					loginContext.setPwAuthOk(true);
					return;
				}
			} else {
				pwAuthCode = AuthCode.LocalSuccess;
				logger.trace("frodo core: password verification skipped");
			}		
			
			loginContext.setAuthCode(pwAuthCode);
		} catch (Exception e) {
			BaseError error = new BaseError("login fail", "cannot login", e);
			loginContext.addError(error);
		}
   }
	
	private boolean isPolicyMismatch(String issuerCN, String loginName, Integer loginMethod, String password) {
		logger.trace("frodo core: login method [{}], issuer CN [{}]", new Object[] { loginMethod, issuerCN });

		boolean isNullPassword = (password == null || password.trim().isEmpty());

		switch (loginMethod) {
		case LoginUtil.LoginMethod_PWONLY:
			return !issuerCN.equals(PasswordCA);
		case LoginUtil.LoginMethod_NPKI:
			return !isNullPassword || (issuerCN.equals(PasswordCA) || issuerCN.equals(PrivateCertCN));
		case LoginUtil.LoginMethod_PRIVCERT:
			return !isNullPassword || !issuerCN.equals(PrivateCertCN);
		case LoginUtil.LoginMethod_PW_NPKI:
			return (isNullPassword || (issuerCN.equals(PasswordCA) || issuerCN.equals(PrivateCertCN)));
		case LoginUtil.LoginMethod_PW_PRIVCERT:
			return (!issuerCN.equals(PrivateCertCN) || isNullPassword);
		}

		return false;
	}
}
