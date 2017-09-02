package kr.co.future.sslvpn.core.impl;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Collection;

import kr.co.future.sslvpn.core.UserLimitException;
import kr.co.future.sslvpn.core.UserLimitService;
import kr.co.future.sslvpn.model.api.UserApi;
import kr.co.future.sslvpn.core.impl.UserLimitServiceImpl;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Invalidate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.apache.felix.ipojo.annotations.Validate;

import kr.co.future.dom.api.DOMException;
import kr.co.future.dom.api.DefaultEntityEventListener;
import kr.co.future.dom.api.EntityState;
import kr.co.future.dom.model.User;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(name = "frodo-user-limit-service")
@Provides
public class UserLimitServiceImpl implements UserLimitService {
	private static final String UserCountFile = "/proc/utm/conf/sslplus_user_count";
	private final Logger logger = LoggerFactory.getLogger(UserLimitServiceImpl.class.getName());
	private int userLimit = 0;
	private int localCount = 0;

	@Requires
	private kr.co.future.dom.api.UserApi domUserApi;
	
	@Requires
	private UserApi userApi;
	
	private DefaultEntityEventListener<User> userEventListner = new DefaultEntityEventListener<User>() {

		@Override
		public void entityAdded(String domain, User obj, Object state) {
			localCount++;
			if (logger.isDebugEnabled())
				logger.debug("frodo core: user limit count [{}] / [{}]", localCount, userLimit);
		}

		@Override
		public void entityAdding(String domain, User obj, Object state) {
			int availableCount = userLimit - localCount;
			if ((availableCount) <= 0)
				throw new UserLimitException();
		}

		@Override
		public void entitiesAdding(String domain, Collection<EntityState> objs) {
			int availableCount = userLimit - localCount;
			if ((availableCount - objs.size()) < 0)
				throw new UserLimitException();
		}

		@Override
		public void entityRemoved(String domain, User obj, Object state) {
			localCount--;
			logger.debug("frodo core: user [{}] removed", obj.getLoginName());
			logger.debug("frodo core: user limit count [{}] / [{}]", localCount, userLimit);
		}

		@Override
		public void entitiesRemoved(String domain, Collection<EntityState> objs) {
			if (localCount - objs.size() < 0)
				localCount = 1;
			else
				localCount = localCount - objs.size();

			logger.debug("frodo core: user limit count [{}] / [{}]", localCount, userLimit);
		}

	};

	@Validate
	public void start() {
		domUserApi.addEntityEventListener(userEventListner);
		userLimit = getUserLimit() * 3;
		 
		/*
		 * Peter H. Nahm
		 * 130K Users make booting slower because of this.
		 * 
		try {
			localCount = domUserApi.countUsers("localhost", null, true, null);
		} catch (DOMException e) {
			logger.error("frodo core: kraken-dom-localhost database not found");
		}
		*/
		localCount = userApi.getDomUserCount();
		logger.info("frodo core: total users = {}", localCount);
	}

	@Invalidate
	public void stop() {
		if (domUserApi != null)
			domUserApi.removeEntityEventListener(userEventListner);
	}

	@Override
	public int getUserLimit() {
		BufferedReader br = null;
		try {
			br = new BufferedReader(new FileReader(UserCountFile));
			// TODO
			// 장비의 시리얼을 받어서 해당하는 유저 제한을 걸며
			// FrodoScript에다가 유저 제한 을 확인할 수 있는 스크립트 추가
			String limit = br.readLine().trim();
			int count = 0;

			// 모바일 등 다른 단말의 갯수 제한 펌웨어에 대한 임시 방편 커밋 하지 말것!!!
			if (limit.contains(":")) {
				for (String s : limit.split(":")) {
					count += Integer.valueOf(s);
				}
			} else
				count = Integer.valueOf(limit);
			// 새 커널 펌웨어에 라이센스가 구버전이면 0이 나올 수 있음
			if (count == 0 || count >= 65535) //65535는 무제한을 의미. 무제한 값을 줄 수는 없으므로 int의 최대값을 리턴함.
				count = Integer.MAX_VALUE;
			logger.debug("frodo core: current user limit [{}]", count);
			return count;
		} catch (FileNotFoundException e) {
			logger.error("frodo core: user count file not found");
			// 기존의 구버전 펌웨어에서는 파일이 존재하지 않으므로 무조건 추가가 되게 한다.
			return Integer.MAX_VALUE;
		} catch (IOException e) {
			logger.error("frodo core: user count read error");
			throw new IllegalStateException("user count read error");
		} finally {
			try {
				if (br != null)
					br.close();
			} catch (IOException e) {
			}
		}
	}

	@Override
	public void refreshUserCount() {
		//localCount = domUserApi.countUsers("localhost", null, true, null);
		localCount = userApi.getDomUserCount();
		logger.info("frodo core: refresh user count = {}", localCount);
	}
}
