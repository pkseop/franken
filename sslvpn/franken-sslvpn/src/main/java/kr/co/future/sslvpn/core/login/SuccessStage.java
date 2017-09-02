package kr.co.future.sslvpn.core.login;

import java.net.InetAddress;

import kr.co.future.sslvpn.core.pipeline.BaseError;
import kr.co.future.sslvpn.core.pipeline.PipelineContext;
import kr.co.future.sslvpn.core.pipeline.Stage;
import kr.co.future.sslvpn.core.servlet.AuthServiceServlet;
import kr.co.future.sslvpn.model.AccessProfile;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SuccessStage extends Stage {
	
	private final Logger logger = LoggerFactory.getLogger(AuthServiceServlet.class.getName());

	@Override
	public void doExecute(PipelineContext context) {
		LoginContext loginContext = (LoginContext)context;
		LoginUtil loginUtil = loginContext.loginUtil;
	   
		final String loginName = loginContext.getLoginName();
		String os_type = loginContext.getOs_type();
		String deviceKey = loginContext.getDeviceKey();
		InetAddress leaseIp = loginContext.getLeaseIp();
		AccessProfile profile = loginContext.getProfile();
		
		try{
			// return success
			logger.info("frodo core: login success for user [{}], os type [{}], device key [{}]", new Object[] { loginName,
					os_type, deviceKey });
			loginContext.setResult(loginUtil.success(leaseIp, profile));
		} catch (Exception e) {
			BaseError error = new BaseError("login fail", "cannot login", e);
			loginContext.addError(error);
		}
   }

}
