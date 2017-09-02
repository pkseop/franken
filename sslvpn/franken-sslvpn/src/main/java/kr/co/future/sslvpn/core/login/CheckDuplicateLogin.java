package kr.co.future.sslvpn.core.login;

import kr.co.future.sslvpn.core.AuthCode;
import kr.co.future.sslvpn.core.DupLoginCheck;
import kr.co.future.sslvpn.core.xenics.XenicsService;
import kr.co.future.sslvpn.model.AccessProfile;

import kr.co.future.sslvpn.core.pipeline.BaseError;
import kr.co.future.sslvpn.core.pipeline.PipelineContext;
import kr.co.future.sslvpn.core.pipeline.Stage;

public class CheckDuplicateLogin extends Stage {

	@Override
	public void doExecute(PipelineContext context) {
		LoginContext loginContext = (LoginContext)context;
		LoginUtil loginUtil = loginContext.loginUtil;
		
		final String loginName = loginContext.getLoginName();
		AccessProfile profile = loginContext.getProfile();
		final DupLoginCheck dupLoginCheck = loginUtil.dupLoginCheck;
		final XenicsService xenicsService = loginUtil.xenicsService;
		
		try{
			if(dupLoginCheck.isBlock()) {		//로그인된 사용자로 또 로그인 시도를 할 때에 로그인할 수 없게 한다.
				if(dupLoginCheck.useDuplicateLoginCheck()) {
					if(dupLoginCheck.isDuplicateLogin(loginName)) {
						loginContext.setResult(loginUtil.fail(AuthCode.DuplicatedLogin, profile));
					}
					dupLoginCheck.sendLoginInfo(loginName);
				} else {
					if(xenicsService.isAlreadyLoggedin(loginName)) {
						loginContext.setResult(loginUtil.fail(AuthCode.DuplicatedLogin, profile));
					}
				}
			} else {
				//check for duplicate login
				Thread t = new Thread(){
					public void run(){
						if(dupLoginCheck.useDuplicateLoginCheck()) {
							if(dupLoginCheck.isDuplicateLogin(loginName)) {
								dupLoginCheck.killDuplicateLoginTunnel(loginName);
							}
							dupLoginCheck.sendLoginInfo(loginName);
						} else {
							if(xenicsService.isAlreadyLoggedin(loginName)) {
								xenicsService.killDuploginTunnel(loginName);
							}
						}
				    }
				};
				t.start();
			}
		} catch (Exception e) {
			BaseError error = new BaseError("login fail", "cannot login", e);
			loginContext.addError(error);
		}
	}
}
