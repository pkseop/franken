package kr.co.future.sslvpn.core.login;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import kr.co.future.dom.model.User;
import kr.co.future.sslvpn.core.AuthCode;
import kr.co.future.sslvpn.core.pipeline.PipelineContext;
import kr.co.future.sslvpn.core.pipeline.Stage;
import kr.co.future.sslvpn.core.servlet.AuthServiceServlet;
import kr.co.future.sslvpn.model.AccessGateway;
import kr.co.future.sslvpn.model.AccessProfile;
import kr.co.future.sslvpn.model.UserExtension;

public class InitialStage extends Stage {
	private final Logger logger = LoggerFactory.getLogger(AuthServiceServlet.class.getName());

	@Override
	public void doExecute(PipelineContext context) {
		LoginContext loginContext = (LoginContext)context;
		LoginUtil loginUtil = loginContext.loginUtil;
		
		//retrieve access gateway
		AccessGateway gw = loginContext.loginUtil.gatewayApi.getCurrentAccessGateway();
		loginContext.setGw(gw);
		
		//retrieve user
		String loginName = loginContext.getLoginName();
		User user = loginUtil.findUser(loginName);
		if(user == null) {
			loginContext.setResult(loginUtil.fail(AuthCode.UserNotFound, null));
			return;
		}
		loginContext.setUser(user);
		
		//retrieve user extension
		UserExtension ext = loginUtil.userApi.getUserExtension(user);
		loginContext.setUserExt(ext);
		
		//retrieve access profile
		AccessProfile profile = loginUtil.profileApi.determineProfile(user);
		if (profile == null) {
			loginContext.setResult(loginUtil.fail(AuthCode.ProfileNotFound, profile));
			return;
		}
		logger.trace("frodo core: applying profile [{}:{}] to user [{}]", new Object[] { profile.getId(), profile.getName(),
				loginName });
		loginContext.setProfile(profile);
	}

}
