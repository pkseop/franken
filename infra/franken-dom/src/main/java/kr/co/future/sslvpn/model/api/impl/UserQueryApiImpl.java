package kr.co.future.sslvpn.model.api.impl;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import kr.co.future.sslvpn.model.UserQuery;
import kr.co.future.sslvpn.model.api.UserApi;
import kr.co.future.sslvpn.model.api.UserQueryApi;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;

import kr.co.future.confdb.Config;
import kr.co.future.confdb.ConfigDatabase;
import kr.co.future.confdb.ConfigIterator;
import kr.co.future.confdb.ConfigService;
import kr.co.future.confdb.ConfigTransaction;
import kr.co.future.confdb.Predicates;
import kr.co.future.confdb.RollbackException;

@Component(name = "frodo-user-query-api")
@Provides
public class UserQueryApiImpl implements UserQueryApi {

	@Requires
	private ConfigService conf;
	
	@Requires
	private UserApi userApi;

	@Override
	public String createUserQuery(UserQuery userQuery) {
		ConfigDatabase db = conf.ensureDatabase("frodo");
		Map<String, Object> pred = new HashMap<String, Object>();
		pred.put("owner", userQuery.getOwner());
		pred.put("title", userQuery.getTitle());
		Config c = db.findOne(UserQuery.class, Predicates.field(pred));

		if (c != null)
			throw new IllegalStateException("duplicated user query : owner=" + userQuery.getOwner() + ", title="
					+ userQuery.getTitle());

		userQuery.setCreated(new Date());
		userQuery.setUpdated(new Date());
		db.add(userQuery);
		
		// save users_count
		userApi.setDomUserCount(1, true);
		
		return userQuery.getGuid();
	}

	@Override
	public void updateUserQuery(UserQuery userQuery) {
		ConfigDatabase db = conf.ensureDatabase("frodo");
		String guid = userQuery.getGuid();
		Config config = db.findOne(UserQuery.class, Predicates.field("guid", guid));

		if (config == null)
			throw new IllegalStateException("does not exist user query : " + guid);

		userQuery.setUpdated(new Date());
		db.update(config, userQuery, true);
	}

	@Override
	public List<String> removeUserQueries(String owner, List<String> guids) {
		ConfigDatabase db = conf.ensureDatabase("frodo");

		ConfigIterator it = db.find(UserQuery.class,
				Predicates.and(Predicates.field("owner", owner), Predicates.in("guid", guids)));
		ConfigTransaction xact = db.beginTransaction();

		List<String> removedGuids = new ArrayList<String>();
		try {
			while (it.hasNext()) {
				Config c = it.next();
				db.remove(xact, c, true);
				removedGuids.add(((UserQuery) c.getDocument(UserQuery.class)).getGuid());
			}
			xact.commit("frodo", "remove user queries, owner=" + owner);
			
			// save users_count
			userApi.setDomUserCount(removedGuids.size(), false);
		} catch (Throwable e) {
			xact.rollback();
			throw new RollbackException(e);
		} finally {
			if (it != null)
				it.close();
		}

		return removedGuids;
	}

	@Override
	public List<UserQuery> getUserQueries(String owner) {
		ConfigDatabase db = conf.ensureDatabase("frodo");
		ConfigIterator it = db.find(UserQuery.class, Predicates.field("owner", owner));
		return (List<UserQuery>) it.getDocuments(UserQuery.class);
	}

}
