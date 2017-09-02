package kr.co.future.sslvpn.core.login;

import java.util.Date;

import kr.co.future.sslvpn.core.AuthCode;
import kr.co.future.sslvpn.model.AccessProfile;
import kr.co.future.sslvpn.model.UserExtension;
import kr.co.future.sslvpn.core.pipeline.BaseError;
import kr.co.future.sslvpn.core.pipeline.PipelineContext;
import kr.co.future.sslvpn.core.pipeline.Stage;
import kr.co.future.sslvpn.core.servlet.AuthServiceServlet;
import kr.co.future.dom.model.User;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CheckAccountLockStage extends Stage {

	private final Logger logger = LoggerFactory.getLogger(AuthServiceServlet.class.getName());
	
	@Override
   public void doExecute(PipelineContext context) {
		LoginContext loginContext = (LoginContext)context;
		LoginUtil loginUtil = loginContext.loginUtil;
		
		User user = loginContext.getUser();
		String loginName = loginContext.getLoginName();
		UserExtension ext = loginContext.getUserExt();
		AccessProfile profile = loginContext.getProfile();
		
		try{
			// check policy
			if (ext == null)
				ext = loginUtil.createUserExtension(user, null);
			
			// check account lock
			if (ext.isLocked()) {
				if(ext.isAutoLocked() != null && ext.isAutoLocked()) {
					loginContext.setResult(loginUtil.fail(AuthCode.AutoLocked, profile));
					return;
				}
				if (profile.getUserUnlockTime() != null && ext.getLastPasswordFailTime() != null) {
					logger.debug("frodo core: use unlock user, login_name [{}], last fail time [{}]", loginName,
							ext.getLastPasswordFailTime());
					long now = new Date().getTime();
					//Change hours -> minutes
					//long interval = profile.getUserUnlockTime() * 60 * 60 * 1000;
					long interval = profile.getUserUnlockTime() * 60 * 1000;
					if (now - ext.getLastPasswordFailTime().getTime() > interval) {
						ext.setLocked(false);
						ext.setLoginFailures(0);
						ext.setLastPasswordFailTime(null);
					}
					else{	/* Bug Fix */
						loginContext.setResult(loginUtil.fail(AuthCode.Locked, profile));
						return;
					}
				} else {
					loginContext.setResult(loginUtil.fail(AuthCode.Locked, profile));
					return;
				}
			}
			
			loginContext.setUserExt(ext);
		} catch (Exception e) {
			BaseError error = new BaseError("login fail", "cannot login", e);
			loginContext.addError(error);
		}
	   
   }
}
