package kr.co.future.sslvpn.core.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import kr.co.future.cron.PeriodicJob;
import kr.co.future.dom.api.ConfigManager;
import kr.co.future.dom.model.User;
import kr.co.future.sslvpn.model.AccessGateway;
import kr.co.future.sslvpn.model.api.AccessGatewayApi;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(name = "frodo-auto-user-lock-service")
@Provides
@PeriodicJob("0 0 * * *")public class AutoUserLockServiceImpl implements Runnable {
	private final Logger logger = LoggerFactory.getLogger(AutoUserLockServiceImpl.class.getName());

	@Requires
	private AccessGatewayApi gwApi;

	@Requires
	private kr.co.future.dom.api.UserApi domUserApi;

	@Requires
	private ConfigManager conf;

	@Override
	public void run() {
		AccessGateway gw = gwApi.getCurrentAccessGateway();
		// 사용자 자동 잠금 일수가 없으면 기능 동작하지 않음
		if (gw.getAutoUSerLockDate() == null) {
			return;
		} else {
			logger.debug("frodo core: run auto user lock");
			lockDormantUsers();
		}
	}

	private static class DormantFinder {
		private final Logger logger = LoggerFactory.getLogger(DormantFinder.class.getName());
		private Date currentTime;
		private long duration;

		public DormantFinder(long duration) {
			this.currentTime = new Date();
			this.duration = duration;
		}

		@SuppressWarnings("unchecked")
		public boolean eval(User user) {
			Map<String, Object> ext = user.getExt();
			// 관리자라면 자동잠금은 pass
			if(ext.get("admin") != null)
				return false;
			Map<String, Object> frodo = (Map<String, Object>) ext.get("frodo");

			if (logger.isDebugEnabled())
				logger.debug("frodo core: auto user lock, ext[{}]", ext);

			Date standardDate = null;

			if (frodo == null)
				standardDate = user.getCreated();
			else {
				Date lastLogin = (Date) frodo.get("last_login_at");
				Date lastLogout = (Date) frodo.get("last_logout_at");
				//The data of "last_login_at" is null if the user has never logged in. 
				if(lastLogin == null && lastLogout == null)
					standardDate = (Date) frodo.get("created_at");
				else if(lastLogin == null && lastLogout != null)
					standardDate = lastLogout;
				else if(lastLogin != null && lastLogout == null)
					standardDate = lastLogin;
				else {
					if(lastLogin.after(lastLogout))
						standardDate = lastLogin;
					else 
						standardDate = lastLogout;
				}
				//사용자 계정 신청페이지로 신청한 시작 날짜가 고려되도록 함.
				Date useStartDate = (Date) frodo.get("start_at");
				if(useStartDate != null && useStartDate.after(standardDate))
					standardDate = useStartDate;
				
				Date autoLockReleasedTime = (Date) frodo.get("auto_lock_released_time");
				if(autoLockReleasedTime != null && autoLockReleasedTime.after(standardDate))
					standardDate = autoLockReleasedTime;
			}

			long diff = currentTime.getTime() - standardDate.getTime();

			if (logger.isDebugEnabled())
				logger.debug("frodo core: auto user lock, currentTime [{}], standardTime [{}], diff date [{}]", new Object[] {
						currentTime.getTime(), standardDate.getTime(), diff });

			return diff >= duration;
		}
	}

	@SuppressWarnings("unchecked")
	private void lockDormantUsers() {
		String domain = "localhost";
		AccessGateway gw = gwApi.getCurrentAccessGateway();
		//계산되는 시간을 long 타입으로 변경하기 위해. 그렇지 않으면 int 타입으로 계산되어 오버플로우가 일어나 값이 음수가 된다.
		long days = gw.getAutoUSerLockDate();
		DormantFinder dormantFinder = new DormantFinder(days * 24 * 60 * 60 * 1000);

		logger.debug("frodo core: search dormant users for [{}]days", gw.getAutoUSerLockDate());

		Collection<User> allUsers = domUserApi.getUsers(domain);
		List<User> update = new ArrayList<User>();
		for(User user : allUsers) {
			if(dormantFinder.eval(user)) {
				update.add(user);
			}
		}
		
		if(update.size() == 0){
			return;
		}
		
		for (User user : update) {
			Map<String, Object> ext = user.getExt();
			Map<String, Object> frodo = (Map<String, Object>) ext.get("frodo");
			// frodo 유무에 따른 처리
			if (frodo == null) {
				frodo = new HashMap<String, Object>();
				frodo.put("cid", UUID.randomUUID().toString());
				frodo.put("is_locked", true);
				frodo.put("is_auto_locked", true);
				frodo.put("login_failures", 0);
				frodo.put("created_at", new Date());
				frodo.put("updated_at", new Date());
			} else {
				frodo.put("is_locked", true);
				frodo.put("is_auto_locked", true);
			}
			user.getExt().put("frodo", frodo);

			logger.debug("frodo core: lock dormant user [{}]", user.getLoginName());
		}

		// updateUser 실행
		try {
			domUserApi.updateUsers(domain, update, false);
			logger.info("frodo core: locked [{}] dormant users", update.size());
		} catch (Throwable t) {
			logger.error("frodo core: auto user lock, user update failed", t);
		}
	}
}
