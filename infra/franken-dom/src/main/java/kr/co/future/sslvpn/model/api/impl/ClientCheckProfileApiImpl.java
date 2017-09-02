package kr.co.future.sslvpn.model.api.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArraySet;

import kr.co.future.sslvpn.model.AccessProfile;
import kr.co.future.sslvpn.model.ClientCheckProfile;
import kr.co.future.sslvpn.model.api.AccessProfileApi;
import kr.co.future.sslvpn.model.api.ClientCheckProfileApi;
import kr.co.future.sslvpn.model.api.ClientCheckProfileEventListener;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.apache.felix.ipojo.annotations.Validate;
import org.json.JSONConverter;
import org.json.JSONException;

import kr.co.future.confdb.Config;
import kr.co.future.confdb.ConfigDatabase;
import kr.co.future.confdb.ConfigIterator;
import kr.co.future.confdb.ConfigService;
import kr.co.future.confdb.ConfigTransaction;
import kr.co.future.confdb.Predicates;
import kr.co.future.confdb.RollbackException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(name = "frodo-client-check-profile-api")
@Provides
public class ClientCheckProfileApiImpl implements ClientCheckProfileApi {

	private final Logger logger = LoggerFactory.getLogger(ClientCheckProfileApiImpl.class.getName());

	@Requires
	private ConfigService conf;

	@Requires
	private AccessProfileApi accessProfileApi;

	private CopyOnWriteArraySet<ClientCheckProfileEventListener> listeners;

	@Validate
	public void start() {
		listeners = new CopyOnWriteArraySet<ClientCheckProfileEventListener>();
	}

	@Override
	public List<ClientCheckProfile> getClientCheckProfiles() {
		ConfigDatabase db = conf.ensureDatabase("frodo");
		ConfigIterator it = db.findAll(ClientCheckProfile.class);
		return new ArrayList<ClientCheckProfile>(it.getDocuments(ClientCheckProfile.class));
	}

	@Override
	public ClientCheckProfile getClientCheckProfile(String guid) {
		ConfigDatabase db = conf.ensureDatabase("frodo");
		Config c = db.findOne(ClientCheckProfile.class, Predicates.field("guid", guid));

		if (c == null)
			return null;

		return c.getDocument(ClientCheckProfile.class);
	}

	@Override
	public String createClientCheckProfile(ClientCheckProfile p) {
		logger.debug("frodo-model: create client check profile [{}]", p);

		ConfigDatabase db = conf.ensureDatabase("frodo");
		Config c = db.findOne(ClientCheckProfile.class, Predicates.field("guid", p.getGuid()));

		if (c != null)
			throw new IllegalStateException("duplicated profile: " + p.getGuid());

		db.add(p);

		for (ClientCheckProfileEventListener listener : listeners) {
			try {
				listener.onCreated(p);
			} catch (Throwable t) {
				logger.error("frodo model: client check profile callback should not throw any exception", t);
			}
		}

		return p.getGuid();
	}

	@Override
	public void updateClientCheckProfile(ClientCheckProfile p) {
		logger.debug("frodo-model: update client check profile [{}]", p);

		ConfigDatabase db = conf.ensureDatabase("frodo");
		Config c = db.findOne(ClientCheckProfile.class, Predicates.field("guid", p.getGuid()));

		if (c == null)
			throw new IllegalStateException("profile not found: " + p.getGuid());

		db.update(c, p, true);

		for (ClientCheckProfileEventListener listener : listeners) {
			try {
				listener.onUpdated(p);
			} catch (Throwable t) {
				logger.error("frodo model: client check profile callback should not throw any exception", t);
			}
		}
	}

	@Override
	public void removeClientCheckProfile(String guid) {
		ConfigDatabase db = conf.ensureDatabase("frodo");
		Config c = db.findOne(ClientCheckProfile.class, Predicates.field("guid", guid));

		if (c == null)
			return;

		for (AccessProfile p : accessProfileApi.getAccessProfiles()) {
			ClientCheckProfile clientCheckProfile = p.getClientCheckProfile();

			if (clientCheckProfile != null && clientCheckProfile.getGuid().equals(guid))
				throw new IllegalStateException("used by access profile: " + p.getName());
		}

		db.remove(c, true);

		for (ClientCheckProfileEventListener listener : listeners) {
			try {
				listener.onRemoved(c.getDocument(ClientCheckProfile.class));
			} catch (Throwable t) {
				logger.error("frodo model: client check profile callback should not throw any exception", t);
			}
		}
	}

	@Override
	public void removeClientCheckProfiles(List<String> guids) {
		List<Map<String, Object>> l = new ArrayList<Map<String, Object>>();
		for (AccessProfile accessProfile : accessProfileApi.getAccessProfiles()) {
			ClientCheckProfile clientCheckProfile = accessProfile.getClientCheckProfile();

			if (clientCheckProfile == null)
				continue;

			for (String clientCheckProfileGuid : guids) {
				if (clientCheckProfileGuid.equals(clientCheckProfile.getGuid())) {
					Map<String, Object> mm = new HashMap<String, Object>();
					mm.put("accrss_profile", accessProfile.getName());
					mm.put("client_check_profile", clientCheckProfile.getName());
					l.add(mm);
				}
			}
		}

		try {
			if (!l.isEmpty())
				throw new IllegalStateException(JSONConverter.jsonize(l));
		} catch (JSONException e) {
			logger.error("frodo model: JSONException", e);
		}

		ConfigDatabase db = conf.ensureDatabase("frodo");
		ConfigIterator it = db.find(ClientCheckProfile.class, Predicates.in("guid", guids));
		ConfigTransaction xact = db.beginTransaction();
		List<ClientCheckProfile> profiles = new ArrayList<ClientCheckProfile>();
		try {
			while (it.hasNext()) {
				Config c = it.next();
				profiles.add(c.getDocument(ClientCheckProfile.class));
				db.remove(xact, c, true);
			}
			xact.commit("frodo-model", "remove client check profile " + guids.toString());
		} catch (Throwable t) {
			xact.rollback();
			throw new RollbackException(t);
		} finally {
			it.close();
		}

		for (ClientCheckProfile profile : profiles) {
			for (ClientCheckProfileEventListener listener : listeners) {
				try {
					listener.onRemoved(profile);
				} catch (Throwable t) {
					logger.error("frodo model: client check profile callback should not throw any exception", t);
				}
			}
		}
	}

	@Override
	public void addListener(ClientCheckProfileEventListener listener) {
		listeners.add(listener);
	}

	@Override
	public void removeListener(ClientCheckProfileEventListener listener) {
		listeners.remove(listener);
	}
}
