package kr.co.future.sslvpn.model.api.impl;

import java.util.HashMap;
import java.util.Map;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;

import kr.co.future.confdb.Config;
import kr.co.future.confdb.ConfigDatabase;
import kr.co.future.confdb.ConfigService;
import kr.co.future.confdb.Predicates;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import kr.co.future.sslvpn.model.ClusteredIpLease;
import kr.co.future.sslvpn.model.api.ClusteredIpLeaseApi;

@Component(name = "frodo-clustered-ip-lease-api")
@Provides
public class ClusteredIpLeaseApiImpl implements ClusteredIpLeaseApi {
	private final Logger logger = LoggerFactory.getLogger(ClusteredIpLeaseApiImpl.class.getName());

	@Requires
	private ConfigService conf;

	@Override
	public synchronized int lease(String loginName, String profileName, int poolSize) {
		// find lease history first
		Map<String, Object> pred = new HashMap<String, Object>();
		pred.put("login_name", loginName);
		pred.put("profile_name", profileName);
		pred.put("pool_size", poolSize);

		ConfigDatabase db = conf.ensureDatabase("frodo-cluster");
		Config c = db.findOne(ClusteredIpLease.class, Predicates.field(pred));

		// return already assigned ip
		if (c != null) {
			ClusteredIpLease lease = c.getDocument(ClusteredIpLease.class);
			logger.debug("frodo model: return pre-assigned offset [{}] to login [{}], profile [{}], pool size [{}]",
					new Object[] { lease.getOffset(), loginName, profileName, poolSize });
			return lease.getOffset();
		}

		String key = loginName;

		for (int i = 0; i < 10; i++) {
			int offset = Math.abs(key.hashCode()) % poolSize;
			logger.debug("frodo model: try login name [{}], hashcode [{}], offset [{}]", new Object[] { loginName,
					key.hashCode(), offset });

			// try assign using login name hash code, conflict offset check
			pred = new HashMap<String, Object>();
			pred.put("profile_name", profileName);
			pred.put("pool_size", poolSize);
			pred.put("offset", offset);
			c = db.findOne(ClusteredIpLease.class, Predicates.field(pred));

			// if no offset conflict, assign it
			if (c == null)
				return assign(db, loginName, profileName, poolSize, offset);

			// else, extend key and retry
			key += loginName;
		}

		// return fail code
		logger.error("frodo model: cannot clustered lease for login name [{}]", loginName);
		return -1;
	}

	private int assign(ConfigDatabase db, String loginName, String profileName, int poolSize, int offset) {
		logger.debug("frodo-model: assign new offset [{}] to clustered login [{}], profile [{}], pool size [{}]", new Object[] {
				offset, loginName, profileName, poolSize });

		ClusteredIpLease lease = new ClusteredIpLease();
		lease.setLoginName(loginName);
		lease.setProfileName(profileName);
		lease.setPoolSize(poolSize);
		lease.setOffset(offset);
		db.add(lease, "frodo-model", "assign " + offset + " to clustered login " + loginName);
		return offset;
	}

	@Override
	public synchronized void record(String loginName, String profileName, int poolSize, int offset) {
		logger.debug("frodo model: record clustered lease login [{}], profile [{}], pool size [{}], offset [{}]", new Object[] {
				loginName, profileName, poolSize, offset });

		Map<String, Object> pred = new HashMap<String, Object>();
		pred.put("login_name", loginName);
		pred.put("profile_name", profileName);
		pred.put("pool_size", poolSize);

		ConfigDatabase db = conf.ensureDatabase("frodo-cluster");
		Config c = db.findOne(ClusteredIpLease.class, Predicates.field(pred));
		if (c == null) {
			assign(db, loginName, profileName, poolSize, offset);
		} else {
			ClusteredIpLease lease = c.getDocument(ClusteredIpLease.class);
			lease.setOffset(offset);
			db.update(c, lease);
		}
	}
}
