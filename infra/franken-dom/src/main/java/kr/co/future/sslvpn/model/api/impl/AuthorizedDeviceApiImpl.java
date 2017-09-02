package kr.co.future.sslvpn.model.api.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import kr.co.future.sslvpn.model.AccessGateway;
import kr.co.future.sslvpn.model.AccessProfile;
import kr.co.future.sslvpn.model.AuthorizedDevice;
import kr.co.future.sslvpn.model.AccessGateway.DeviceAuthMode;
import kr.co.future.sslvpn.model.api.AccessGatewayApi;
import kr.co.future.sslvpn.model.api.AuthorizedDeviceApi;
import kr.co.future.sslvpn.model.api.AuthorizedDeviceEventListener;

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
import kr.co.future.confdb.Predicate;
import kr.co.future.confdb.Predicates;
import kr.co.future.confdb.RollbackException;
import kr.co.future.dom.api.DefaultEntityEventListener;
import kr.co.future.dom.api.EntityEventListener;
import kr.co.future.dom.api.EntityState;
import kr.co.future.dom.api.UserApi;
import kr.co.future.dom.model.User;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(name = "frodo-auth-device-api")
@Provides
public class AuthorizedDeviceApiImpl implements AuthorizedDeviceApi {
	private final Logger logger = LoggerFactory.getLogger(AuthorizedDeviceApiImpl.class.getName());

	@Requires
	private ConfigService conf;

	@Requires
	private AccessGatewayApi gwApi;

	@Requires
	private UserApi domUserApi;

	private CopyOnWriteArraySet<AuthorizedDeviceEventListener> listeners;

	private EntityEventListener<User> userEventListner = new DefaultEntityEventListener<User>() {
		@Override
		public void entityUpdated(String domain, User user, Object state) {
			ConfigIterator it = null;
			try {
				ConfigDatabase db = conf.ensureDatabase("frodo");
				it = db.find(AuthorizedDevice.class, Predicates.field("login_name", user.getLoginName()));
				List<AuthorizedDevice> authDevices = (List<AuthorizedDevice>) it.getDocuments(AuthorizedDevice.class);
				List<AuthorizedDevice> updateDevices = new ArrayList<AuthorizedDevice>();
				for (AuthorizedDevice authDevice : authDevices) {
					if (!authDevice.getOwner().equals(user.getName())) {
						authDevice.setOwner(user.getName());
						updateDevices.add(authDevice);
					}
				}
				if (updateDevices.size() > 0)
					updateDevices(updateDevices);
			} finally {
				if (it != null)
					it.close();
			}
		}

		@Override
		public void entitiesUpdated(String domain, Collection<EntityState> users) {
			Map<String, User> userMap = new HashMap<String, User>();
			for (EntityState entity : users) {
				User user = (User) entity.entity;
				userMap.put(user.getLoginName(), user);
			}

			ConfigDatabase db = conf.ensureDatabase("frodo");
			Collection<AuthorizedDevice> authDevices = db.find(AuthorizedDevice.class,
					Predicates.in("login_name", userMap.keySet())).getDocuments(AuthorizedDevice.class);

			List<AuthorizedDevice> updateDevices = new ArrayList<AuthorizedDevice>();
			for (AuthorizedDevice authDevice : authDevices) {
				User user = userMap.get(authDevice.getLoginName());
				if (user == null)
					continue;

				if (!authDevice.getOwner().equals(user.getName())) {
					authDevice.setOwner(user.getName());
					updateDevices.add(authDevice);
				}

			}

			if (updateDevices.size() > 0)
				updateDevices(updateDevices);
		}

		@Override
		public void entitiesRemoved(String domain, Collection<EntityState> objs) {
			HashSet<String> loginNames = new HashSet<String>();
			for (EntityState e : objs)
				loginNames.add(((User) e.entity).getLoginName());

			ConfigDatabase db = conf.ensureDatabase("frodo");
			Collection<AuthorizedDevice> devices = db.find(AuthorizedDevice.class, Predicates.in("login_name", loginNames))
					.getDocuments(AuthorizedDevice.class);
			if (devices.isEmpty())
				return;

			HashSet<String> guids = new HashSet<String>();
			for (AuthorizedDevice device : devices)
				guids.add(device.getGuid());

			unregisterDevices(guids);
		}

		@Override
		public void entityRemoved(String domain, User obj, Object state) {
			ConfigIterator it = null;
			try {
				ConfigDatabase db = conf.ensureDatabase("frodo");
				it = db.find(AuthorizedDevice.class, Predicates.field("login_name", obj.getLoginName()));
				List<AuthorizedDevice> devices = (List<AuthorizedDevice>) it.getDocuments(AuthorizedDevice.class);
				if (devices.isEmpty())
					return;

				Set<String> guids = new HashSet<String>();
				for (AuthorizedDevice device : devices) {
					guids.add(device.getGuid());
				}

				unregisterDevices(guids);
			} finally {
				if (it != null)
					it.close();
			}
		}
	};

	public AuthorizedDeviceApiImpl() {
		listeners = new CopyOnWriteArraySet<AuthorizedDeviceEventListener>();
	}

	@Validate
	public void start() {
		domUserApi.addEntityEventListener(userEventListner);
	}

	@Invalidate
	public void stop() {
		if (domUserApi != null)
			domUserApi.removeEntityEventListener(userEventListner);
	}

	@Override
	public List<AuthorizedDevice> getDevices() {
		return getDevices(0, Integer.MAX_VALUE);
	}

	@Override
	public List<AuthorizedDevice> getDevices(int offset, int limit) {
		return getDevices(offset, limit, null);
	}

	@Override
	public List<AuthorizedDevice> getDevices(Set<String> guids) {
		if (guids == null || guids.isEmpty())
			return new ArrayList<AuthorizedDevice>();

		return getDevices(0, Integer.MAX_VALUE, Predicates.in("guid", guids));
	}

	@Override
	public List<AuthorizedDevice> getDevices(int offset, int limit, Predicate pred) {

		ConfigDatabase db = conf.ensureDatabase("frodo");
		ConfigIterator it = null;

		try {
			it = db.find(AuthorizedDevice.class, pred);
			return (List<AuthorizedDevice>) it.getDocuments(AuthorizedDevice.class, null, offset, limit);
		} finally {
			if (it != null)
				it.close();
		}
	}

	@Override
	public int countDevices(Predicate pred) {
		ConfigIterator it = null;
		try {
			ConfigDatabase db = conf.ensureDatabase("frodo");
			it = db.find(AuthorizedDevice.class, pred);
			return it.count();
		} finally {
			if (it != null)
				it.close();
		}
	}

	@Override
	public void registerDevice(AuthorizedDevice device) {
		ConfigDatabase db = conf.ensureDatabase("frodo");
		db.add(device);

		for (AuthorizedDeviceEventListener listener : listeners) {
			try {
				listener.onRegister(device);
			} catch (Throwable t) {
				logger.error("frodo model: auth device callback should not throw any exception", t);
			}
		}
	}

	@Override
	public void registerDevices(List<AuthorizedDevice> devices) {
		ConfigDatabase db = conf.ensureDatabase("frodo");
		ConfigTransaction xact = db.beginTransaction();

		try {
			for (AuthorizedDevice device : devices) {
				db.add(xact, device);
			}
			xact.commit("frodo-model", "register devices");
		} catch (Throwable e) {
			xact.rollback();
			throw new RollbackException(e);
		}

		for (AuthorizedDevice device : devices) {
			for (AuthorizedDeviceEventListener listener : listeners) {
				try {
					listener.onRegister(device);
				} catch (Throwable t) {
					logger.error("frodo model: auth device callback should not throw any exception", t);
				}
			}
		}
	}

	@Override
	public void unregisterDevices(Set<String> guids) {
		if (guids.isEmpty())
			return;

		ConfigDatabase db = conf.ensureDatabase("frodo");
		ConfigIterator it = db.find(AuthorizedDevice.class, Predicates.in("guid", guids));

		if (!it.hasNext()) {
			it.close();
			return;
		}

		ConfigTransaction xact = db.beginTransaction();
		List<AuthorizedDevice> devices = new ArrayList<AuthorizedDevice>();
		try {
			while (it.hasNext()) {
				Config c = it.next();
				devices.add(c.getDocument(AuthorizedDevice.class));
				db.remove(xact, c, true);
			}
			xact.commit("frodo-model", "remove device " + guids.toString());
		} catch (Throwable e) {
			xact.rollback();
			throw new RollbackException(e);
		} finally {
			if (it != null)
				it.close();
		}

		for (AuthorizedDevice device : devices) {
			for (AuthorizedDeviceEventListener listener : listeners) {
				try {
					listener.onUnregister(device);
				} catch (Throwable t) {
					logger.error("frodo model: auth device callback should not throw any exception", t);
				}
			}
		}

	}

	@Override
	public void unregisterDevice(String guid) {
		ConfigDatabase db = conf.ensureDatabase("frodo");
		Config c = db.findOne(AuthorizedDevice.class, Predicates.field("guid", guid));
		if (c != null) {
			AuthorizedDevice device = c.getDocument(AuthorizedDevice.class);
			db.remove(c, true);

			for (AuthorizedDeviceEventListener listener : listeners) {
				try {
					listener.onUnregister(device);
				} catch (Throwable t) {
					logger.error("frodo model: auth device callback should not throw any exception", t);
				}
			}
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public void updateDevices(List<AuthorizedDevice> devices) {
		Map<String, AuthorizedDevice> newDeviceMap = toMap(devices);

		ConfigDatabase db = conf.ensureDatabase("frodo");
		ConfigTransaction xact = db.beginTransaction();

		ConfigIterator it = db.find(AuthorizedDevice.class, Predicates.in("guid", getGuids(devices)));
		List<AuthorizedDevice> updatedDevices = new ArrayList<AuthorizedDevice>();
		try {
			while (it.hasNext()) {
				Config c = it.next();
				Map<String, Object> old = (Map<String, Object>) c.getDocument();
				AuthorizedDevice newDevice = newDeviceMap.get(old.get("guid"));
				db.update(xact, c, newDevice, false);
				updatedDevices.add(newDevice);
			}
			xact.commit("frodo-model", "updated authorized devices");
		} catch (Throwable e) {
			xact.rollback();
			throw new RollbackException(e);
		} finally {
			if (it != null)
				it.close();
		}

		for (AuthorizedDevice device : updatedDevices) {
			for (AuthorizedDeviceEventListener listener : listeners) {
				try {
					listener.onUpdate(device);
				} catch (Throwable t) {
					logger.error("frodo model: auth device callback should not throw any exception", t);
				}
			}
		}
	}

	private Map<String, AuthorizedDevice> toMap(List<AuthorizedDevice> devices) {
		Map<String, AuthorizedDevice> m = new HashMap<String, AuthorizedDevice>();
		for (AuthorizedDevice device : devices)
			m.put(device.getGuid(), device);
		return m;
	}

	private Set<String> getGuids(List<AuthorizedDevice> devices) {
		Set<String> s = new HashSet<String>();
		for (AuthorizedDevice device : devices)
			s.add(device.getGuid());
		return s;
	}

	@Override
	public void blockDevice(String guid, boolean block) {
		ConfigDatabase db = conf.ensureDatabase("frodo");
		Config c = db.findOne(AuthorizedDevice.class, Predicates.field("guid", guid));
		if (c != null) {
			AuthorizedDevice device = c.getDocument(AuthorizedDevice.class);
			device.setBlocked(block);
			db.update(c, device, true);

			for (AuthorizedDeviceEventListener listener : listeners) {
				try {
					listener.onUpdate(device);
				} catch (Throwable t) {
					logger.error("frodo model: auth device callback should not throw any exception", t);
				}
			}
		}
	}

	@Override
	public AuthorizedDevice getDevice(String guid) {
		return getDevice(Predicates.field("guid", guid));
	}

	@Override
	public AuthorizedDevice getDevice(Predicate pred) {
		ConfigDatabase db = conf.ensureDatabase("frodo");
		Config c = db.findOne(AuthorizedDevice.class, pred);
		if (c != null)
			return c.getDocument(AuthorizedDevice.class);
		return null;
	}

	@Override
	public boolean isAuthorized(String deviceKey, String loginName, AccessProfile profile) {
		DeviceAuthMode deviceAuthMode = gwApi.getCurrentAccessGateway().getDeviceAuthMode();
		if (profile != null && profile.getDeviceAuthMode() != null)
			deviceAuthMode = profile.getDeviceAuthMode();

		Map<String, Object> filter = new HashMap<String, Object>();
		filter.put("device_key", deviceKey);
		//Peter H. Nahm - 사용자 독립적 단말 인증 구조 개선
		//if (deviceAuthMode == DeviceAuthMode.OneToOne)
			filter.put("login_name", loginName);

		for (AuthorizedDevice device : getDevices(0, Integer.MAX_VALUE, Predicates.field(filter))) {
			if (!device.getIsAuthorized())
				return false;
		}
		return true;
	}

	@Override
	public AuthorizedDevice findDeviceByKey(String deviceKey, String loginName, int osType, AccessProfile profile) {
		AccessGateway gw = gwApi.getCurrentAccessGateway();
		DeviceAuthMode deviceAuthMode = gw.getDeviceAuthMode();
		if (profile != null && profile.getDeviceAuthMode() != null) {
			if (profile.getDeviceAuthMode() == DeviceAuthMode.None)
				return null;

			deviceAuthMode = profile.getDeviceAuthMode();
		}

		Map<String, Object> filter = new HashMap<String, Object>();
		filter.put("deviceKey", deviceKey);
		filter.put("type", osType);
		if (deviceAuthMode != DeviceAuthMode.OneToOne)
			filter.put("login_name", loginName);

		return getDevice(Predicates.field(filter));
	}

	@Override
	public void setExpiration(String guid, Date expiration) {
		ConfigDatabase db = conf.ensureDatabase("frodo");
		Config c = db.findOne(AuthorizedDevice.class, Predicates.field("guid", guid));

		if (c != null) {
			AuthorizedDevice device = c.getDocument(AuthorizedDevice.class);
			device.setExpiration(expiration);
			db.update(c, device, true);

			for (AuthorizedDeviceEventListener listener : listeners) {
				try {
					listener.onUpdate(device);
				} catch (Throwable t) {
					logger.error("frodo model: auth device callback should not throw any exception", t);
				}
			}
		}
	}

	@Override
	public void addListener(AuthorizedDeviceEventListener listener) {
		listeners.add(listener);
	}

	@Override
	public void removeListener(AuthorizedDeviceEventListener listener) {
		listeners.remove(listener);
	}
}
