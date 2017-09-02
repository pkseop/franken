package kr.co.future.sslvpn.core.login;

import java.util.Calendar;
import java.util.Date;

import kr.co.future.sslvpn.core.AuthCode;
import kr.co.future.sslvpn.model.AccessProfile;
import kr.co.future.sslvpn.model.UserExtension;
import kr.co.future.sslvpn.core.pipeline.BaseError;
import kr.co.future.sslvpn.core.pipeline.PipelineContext;
import kr.co.future.sslvpn.core.pipeline.Stage;
import kr.co.future.sslvpn.core.servlet.AuthServiceServlet;
import kr.co.future.dom.model.Schedule;
import kr.co.future.dom.model.Timetable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CheckScheduleStage extends Stage {
	private final Logger logger = LoggerFactory.getLogger(AuthServiceServlet.class);

	@Override
   public void doExecute(PipelineContext context) {
		LoginContext loginContext = (LoginContext)context;
		LoginUtil loginUtil = loginContext.loginUtil;
		
		AccessProfile profile = loginContext.getProfile();
		UserExtension ext = loginContext.getUserExt();
		
		try{
			if (!checkSchedule(loginUtil, ext, profile)) {
				loginContext.setResult(loginUtil.fail(AuthCode.Schedule, profile));
				return;
			}
		} catch (Exception e) {
			BaseError error = new BaseError("login fail", "cannot login", e);
			loginContext.addError(error);
		}
   }
	
	private boolean checkSchedule(LoginUtil loginUtil, String timeTableId) {
		Timetable table = loginUtil.timetableApi.getTimetable("localhost", timeTableId);
		if (table == null) {
			logger.warn("frodo core: timetable [{}] not found", timeTableId);
			return true;
		}

		Calendar c = Calendar.getInstance();
		Date now = c.getTime();
		Date base = getBeginOfDay();
		int seconds = (int) ((now.getTime() - base.getTime()) / 1000);

		for (Schedule schedule : table.getSchedules()) {
			if (schedule.getDayOfWeek() != (c.get(Calendar.DAY_OF_WEEK) - 1))
				continue;

			if (schedule.getBeginSecond() <= seconds && seconds <= schedule.getEndSecond())
				return true;
		}

		return false;
	}

	private boolean checkSchedule(LoginUtil loginUtil, UserExtension ext, AccessProfile profile) {
		String profileTimeTableId = profile.getAllowTimeId();
		String userTimeTableId = ext.getAllowTimeTableId();
		
		//user policy has higher priority.
		if(userTimeTableId != null)
			return checkSchedule(loginUtil, userTimeTableId);
		else if(profileTimeTableId != null)
			return checkSchedule(loginUtil, profileTimeTableId);
		else
			return true;
	}
	
	private Date getBeginOfDay() {
		Calendar c = Calendar.getInstance();
		c.set(Calendar.HOUR_OF_DAY, 0);
		c.set(Calendar.MINUTE, 0);
		c.set(Calendar.SECOND, 0);
		c.set(Calendar.MILLISECOND, 0);
		return c.getTime();
	}

}
