package kr.co.future.sslvpn.core.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import kr.co.future.sslvpn.core.SqlAuthResult;
import kr.co.future.sslvpn.core.SqlAuthService;
import kr.co.future.sslvpn.core.impl.SqlUserAutoRemover;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;

import kr.co.future.cron.PeriodicJob;
import kr.co.future.dom.api.UserApi;
import kr.co.future.dom.model.User;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@PeriodicJob("0 0 * * *")
@Provides
@Component(name = "sql-user-auto-remover")
public class SqlUserAutoRemover implements Runnable {
	private final Logger logger = LoggerFactory.getLogger(SqlUserAutoRemover.class);

	@Requires
	private SqlAuthService sqlAuth;

	@Requires
	private UserApi domUserApi;

	@Override
	public void run() {
		remover();
	}

	private void remover() {
		if (!sqlAuth.isEnabled())
			return;

		Collection<User> users = domUserApi.getUsers("localhost");
		List<String> removeUsers = new ArrayList<String>();
		for (User user : users) {
			if (user.getSourceType() != null && user.getSourceType().equals("sql")) {
				SqlAuthResult result = sqlAuth.verifyUser(user.getLoginName());
				if (!result.isSuccess())
					removeUsers.add(user.getLoginName());
			}
		}

		if (!removeUsers.isEmpty()) {
			logger.info("[{}] users does not exist sql server.", removeUsers.size());
			domUserApi.removeUsers("localhost", removeUsers);
		}
	}
}
