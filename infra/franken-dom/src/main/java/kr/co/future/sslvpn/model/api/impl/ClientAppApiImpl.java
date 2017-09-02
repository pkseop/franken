package kr.co.future.sslvpn.model.api.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.atomic.AtomicInteger;

import kr.co.future.sslvpn.model.AccessProfile;
import kr.co.future.sslvpn.model.ClientApp;
import kr.co.future.sslvpn.model.api.AccessProfileApi;
import kr.co.future.sslvpn.model.api.ClientAppApi;
import kr.co.future.sslvpn.model.api.ClientAppEventListener;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Invalidate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.apache.felix.ipojo.annotations.Validate;

import kr.co.future.confdb.Config;
import kr.co.future.confdb.ConfigDatabase;
import kr.co.future.confdb.ConfigIterator;
import kr.co.future.confdb.ConfigService;
import kr.co.future.confdb.ConfigTransaction;
import kr.co.future.confdb.Predicates;
import kr.co.future.dom.api.ApplicationApi;
import kr.co.future.dom.api.DefaultEntityEventListener;
import kr.co.future.dom.api.EntityEventListener;
import kr.co.future.dom.model.Application;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(name = "frodo-client-app-api")
@Provides
public class ClientAppApiImpl extends DefaultEntityEventListener<Application> implements ClientAppApi,
		EntityEventListener<Application> {

	private final Logger logger = LoggerFactory.getLogger(ClientAppApiImpl.class.getName());

	@Requires
	private ConfigService conf;

	@Requires
	private ApplicationApi appApi;

	@Requires
	private AccessProfileApi profileApi;

	private CopyOnWriteArraySet<ClientAppEventListener> listeners;

	private AtomicInteger counter;

	@Validate
	public void start() {
		listeners = new CopyOnWriteArraySet<ClientAppEventListener>();
		int max = 0;
		appApi.addEntityEventListener(this);

		for (ClientApp a : getClientApps()) {
			if (a.getId() > max)
				max = a.getId();
		}

		counter = new AtomicInteger(max);

		for (ClientApp a : getClientApps()) {
			if (a.getId() == 0) {
				a.setId(counter.incrementAndGet());
				updateClientApp(a);
			}
		}
	}

	@Invalidate
	public void stop() {
		if (appApi != null)
			appApi.removeEntityEventListener(this);
	}

	@Override
	public List<ClientApp> getClientApps() {
		ConfigDatabase db = conf.ensureDatabase("frodo");
		ConfigIterator it = db.findAll(ClientApp.class);
		return (List<ClientApp>) it.getDocuments(ClientApp.class);
	}

	@Override
	public ClientApp getClientApp(String id) {
		ConfigDatabase db = conf.ensureDatabase("frodo");
		Config c = db.findOne(ClientApp.class, Predicates.field("guid", id));
		if (c == null)
			return null;

		return c.getDocument(ClientApp.class);
	}

	@Override
	public void createClientApp(ClientApp client) {
		ConfigDatabase db = conf.ensureDatabase("frodo");
		client.setId(counter.incrementAndGet());
		db.add(client);

		for (ClientAppEventListener listener : listeners) {
			try {
				listener.onCreated(client);
			} catch (Throwable t) {
				logger.error("frodo model: client app callback should not throw any exception", t);
			}
		}
	}

	@Override
	public void updateClientApp(ClientApp client) {
		ConfigDatabase db = conf.ensureDatabase("frodo");
		Config c = db.findOne(ClientApp.class, Predicates.field("guid", client.getGuid()));
		if (c == null)
			throw new IllegalStateException("app not found: " + client.getGuid());

		ClientApp old = c.getDocument(ClientApp.class);
		old.setId(client.getId());
		old.setPlatform(client.getPlatform());
		old.setName(client.getName());
		old.setOperator(client.getOperator());
		old.setPhone(client.getPhone());
		old.setIcon(client.getIcon());
		old.setMetadata(client.getMetadata());

		db.update(c, old, true);

		for (ClientAppEventListener listener : listeners) {
			try {
				listener.onUpdated(old);
			} catch (Throwable t) {
				logger.error("frodo model: client app callback should not throw any exception", t);
			}
		}
	}

	@Override
	public void removeClientApp(String id) {
		removeClientApps(Arrays.asList(new String[] { id }));
	}

	@Override
	public void removeClientApps(List<String> ids) {
		ConfigDatabase db = conf.ensureDatabase("frodo");
		ConfigIterator it = db.find(ClientApp.class, Predicates.in("guid", ids));

		while (it.hasNext()) {
			Config c = it.next();
			ClientApp app = c.getDocument(ClientApp.class);

			List<AccessProfile> profiles = filt(profileApi.getAccessProfiles(), ids);

			if (!profiles.isEmpty()) {
				String names = "";
				int i = 0;
				for (AccessProfile p : profiles) {
					if (i++ != 0)
						names += ", ";

					names += p.getName();
				}
				throw new IllegalStateException("application used by: " + names);
			}

			if (app != null) {
				db.remove(c);

				for (ClientAppEventListener listener : listeners) {
					try {
						listener.onRemoved(app);
					} catch (Throwable t) {
						logger.error("frodo model: client app callback should not throw any exception", t);
					}
				}
			}
		}
	}

	private List<AccessProfile> filt(List<AccessProfile> list, List<String> ids) {
		List<AccessProfile> r = new ArrayList<AccessProfile>();
		for (AccessProfile p : list)
			if (hasClientApp(p, ids))
				r.add(p);
		return r;
	}

	private boolean hasClientApp(AccessProfile p, List<String> ids) {
		for (ClientApp d : p.getClientApps()) {
			if (ids.contains(d.getGuid()))
				return true;
		}
		return false;
	}

	@Override
	public List<ClientApp> getClientApps(String appGuid) {
		ConfigDatabase db = conf.ensureDatabase("frodo");
		ConfigIterator it = db.find(ClientApp.class, Predicates.field("appGuid", appGuid));
		return (List<ClientApp>) it.getDocuments(ClientApp.class);
	}

	@Override
	public void entityRemoving(String domain, Application obj, ConfigTransaction xact, Object state) {
		List<ClientApp> apps = getClientApps(obj.getGuid());
		if (apps.size() > 0)
			throw new IllegalStateException("used by: " + apps.get(0).getName());
	}

	@Override
	public void migrateClientApps() {
		ConfigDatabase db = conf.ensureDatabase("frodo");

		List<ClientApp> clientAppList = getClientApps();
		for (ClientApp c : clientAppList) {
			// 기존 application 의 metadata를 clientApp의 metadata로 이식
			if (c.getAppGuid() == null) {
				logger.warn("frodo-model: skip migration, app guid is null, client guid [{}]", c.getGuid());
				continue;
			}

			Application app = appApi.findApplication("localhost", c.getAppGuid());
			if (app == null) {
				logger.warn("frodo-model: skip migration, app not found, client guid [{}]", c.getGuid());
				continue;
			}

			logger.trace("frodo-model: migrating client app, app_name [{}], metadatas [{}]", app.getName(), app.getMetadatas());

			Map<String, Object> metadata = new HashMap<String, Object>();

			if (c.getPlatform().equals("web"))
				metadata.put("url", c.getUrl());
			else if (c.getPlatform().equals("windows"))
				metadata.put("exe_path", app.getMetadatas().get("exe_path"));
			else if (c.getPlatform().equals("android")) {
				metadata.put("pkg_name", app.getMetadatas().get("pkg_name"));
				metadata.put("pkg_download_url", app.getMetadatas().get("pkg_download_url"));

			}

			// 기존 confDb의 clientApp 불러오기
			Config old = db.findOne(ClientApp.class, Predicates.field("guid", c.getGuid()));
			if (old == null)
				throw new IllegalStateException("app not found: " + c.getGuid());

			ClientApp clientApp = old.getDocument(ClientApp.class);
			clientApp.setId(c.getId());
			clientApp.setGuid(c.getGuid());
			clientApp.setName(c.getName());
			clientApp.setOperator(c.getOperator());
			clientApp.setPhone(c.getPhone());
			clientApp.setIcon(c.getIcon());
			clientApp.setDescription(c.getDescription());
			clientApp.setMobile(c.getMobile());
			clientApp.setEmail(c.getEmail());
			clientApp.setMetadata(metadata);

			logger.trace("frodo-model: migrated client app to [{}]", clientApp.marshal());

			db.update(old, clientApp);

		}

	}

	@Override
	public void addListener(ClientAppEventListener listener) {
		if (listener == null)
			throw new IllegalStateException("client app event listener should be not null");
		listeners.add(listener);
	}

	@Override
	public void removeListener(ClientAppEventListener listener) {
		if (listener == null)
			throw new IllegalStateException("client app event listener should be not null");
		listeners.remove(listener);
	}
}
