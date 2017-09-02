package kr.co.future.sslvpn.core.login;

import java.util.Date;

import kr.co.future.sslvpn.core.AuthCode;
import kr.co.future.sslvpn.core.pipeline.BaseError;
import kr.co.future.sslvpn.core.pipeline.PipelineContext;
import kr.co.future.sslvpn.core.pipeline.Stage;
import kr.co.future.sslvpn.model.AccessProfile;
import kr.co.future.sslvpn.model.UserExtension;

public class CheckValidDateStage extends Stage {
	
	@Override
	public void doExecute(PipelineContext context) {
		LoginContext loginContext = (LoginContext)context;
		LoginUtil loginUtil = loginContext.loginUtil;
		
		UserExtension ext = loginContext.getUserExt();
		AccessProfile profile = loginContext.getProfile();
		
		try{
			if(!checkValidDate(ext, profile)) {
				loginContext.setResult(loginUtil.fail(AuthCode.Expired, profile));
				return;
			}
		} catch (Exception e) {
			BaseError error = new BaseError("login fail", "cannot login", e);
			loginContext.addError(error);
		}
	}

	private boolean checkValidDate(Date startDate, Date expireDate) {
		Date now = new Date();
		if((startDate != null && now.before(startDate)) || (expireDate != null && now.after(expireDate)))
			return false;
		return true;
	}
	
	private boolean checkValidDate(UserExtension ext, AccessProfile profile) {
		Date userStartDate = ext.getStartDateTime();
		Date userExpireDate = ext.getExpireDateTime();
		
		Date profileStartDate = profile.getStartValidDate();
		Date profileExpireDate = profile.getExpireValidDate();
		
		//user policy has higher priority.
		if(userStartDate != null || userExpireDate != null)
			return checkValidDate(userStartDate, userExpireDate);
		else if(profileStartDate != null || profileExpireDate != null)
			return checkValidDate(profileStartDate, profileExpireDate);
		else
			return true;
	}
	
}
