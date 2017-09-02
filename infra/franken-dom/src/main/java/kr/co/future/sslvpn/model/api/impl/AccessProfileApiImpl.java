package kr.co.future.sslvpn.model.api.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.felix.ipojo.annotations.Bind;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Invalidate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.apache.felix.ipojo.annotations.Unbind;
import org.apache.felix.ipojo.annotations.Validate;

import kr.co.future.api.PrimitiveParseCallback;
import kr.co.future.confdb.BaseConfigDatabaseListener;
import kr.co.future.confdb.BaseConfigServiceListener;
import kr.co.future.confdb.Config;
import kr.co.future.confdb.ConfigDatabase;
import kr.co.future.confdb.ConfigIterator;
import kr.co.future.confdb.ConfigService;
import kr.co.future.confdb.ConfigTransaction;
import kr.co.future.confdb.Predicates;
import kr.co.future.confdb.RollbackException;
import kr.co.future.dom.api.DOMException;
import kr.co.future.dom.api.DefaultEntityEventListener;
import kr.co.future.dom.api.DefaultEntityEventProvider;
import kr.co.future.dom.api.OrganizationApi;
import kr.co.future.dom.api.OrganizationUnitApi;
import kr.co.future.dom.model.OrganizationUnit;
import kr.co.future.dom.model.User;
import kr.co.future.msgbus.Marshaler;
import kr.co.future.sslvpn.model.AccessProfile;
import kr.co.future.sslvpn.model.ClientApp;
import kr.co.future.sslvpn.model.ClientCheckProfile;
import kr.co.future.sslvpn.model.IpEndpoint;
import kr.co.future.sslvpn.model.OrgUnitExtension;
import kr.co.future.sslvpn.model.ReferenceException;
import kr.co.future.sslvpn.model.Server;
import kr.co.future.sslvpn.model.UserExtension;
import kr.co.future.sslvpn.model.api.AccessGatewayApi;
import kr.co.future.sslvpn.model.api.AccessProfileApi;
import kr.co.future.sslvpn.model.api.ClientAppApi;
import kr.co.future.sslvpn.model.api.ClientAppEventListener;
import kr.co.future.sslvpn.model.api.ClientCheckProfileApi;
import kr.co.future.sslvpn.model.api.ClientCheckProfileEventListener;
import kr.co.future.sslvpn.model.api.UserApi;
import kr.co.future.sslvpn.model.api.VpnServerConfigApi;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(name = "frodo-access-profile-api")
@Provides
public class AccessProfileApiImpl extends DefaultEntityEventProvider<AccessProfile> implements AccessProfileApi {
	private final Logger logger = LoggerFactory.getLogger(AccessProfileApiImpl.class.getName());

	@Requires
	private kr.co.future.dom.api.UserApi domUserApi;

	@Requires
	private OrganizationUnitApi orgUnitApi;

	@Requires
	private OrganizationApi orgApi;

	@Requires
	private UserApi userApi;

	@Requires
	private ConfigService conf;

	@Requires
	private AccessGatewayApi gwApi;
	
	@Requires
	private VpnServerConfigApi vpnServerConfigApi;

	private AtomicInteger counter;

	// org unit cache
	private Map<String, OrganizationUnit> orgUnits = new HashMap<String, OrganizationUnit>();

	// frodo org unit extension cache
	private Map<String, OrgUnitExtension> ouexts = new HashMap<String, OrgUnitExtension>();

	// access profile cache
	private Map<String, AccessProfile> profs = new HashMap<String, AccessProfile>();

	private OrgUnitEntityEventListener orgUnitEntityEventListener = new OrgUnitEntityEventListener();

	private ClientCheckProfileApi ccProfileApi;
	private ClientCheckProfileUpdater ccpUpdater;

	private ClientAppApi appApi;
	private ClientAppUpdater appUpdater;

	private ConfDbListener dbListener;
	private ConfServiceListener serviceListener;

	public AccessProfileApiImpl() {
		serviceListener = new ConfServiceListener();
		dbListener = new ConfDbListener();
	}

	@Bind(optional = true)
	public void bindClientCheckProfileApi(ClientCheckProfileApi ccProfileApi) {
		ccpUpdater = new ClientCheckProfileUpdater();
		this.ccProfileApi = ccProfileApi;
		ccProfileApi.addListener(ccpUpdater);
	}

	// unbind의 경우 메소드의 이름을 보고 unbind 할 대상을 정한다.
	@Unbind
	public void unbindClientCheckProfileApi() {
		logger.debug("frodo  model: unbind client check profile api");
		this.ccProfileApi.removeListener(ccpUpdater);
		this.ccProfileApi = null;
	}

	@Bind(optional = true)
	public void bindClientAppApi(ClientAppApi appApi) {
		appUpdater = new ClientAppUpdater();
		this.appApi = appApi;
		appApi.addListener(appUpdater);
	}

	@Unbind
	public void unbindClientAppApi() {
		logger.debug("frodo  model: unbind client app api");
		this.appApi.removeListener(appUpdater);
		this.appApi = null;
	}

	@Validate
	public void start() {
		int max = 0;

		for (AccessProfile p : getAccessProfiles())
			if (max < p.getId())
				max = p.getId();

		counter = new AtomicInteger(max);

		orgUnitApi.addEntityEventListener(orgUnitEntityEventListener);

        logger.info("AccessProfileApiImpl initializeCache() start...");
		initializeCache();
        logger.info("AccessProfileApiImpl initializeCache() end...");

		conf.addListener(serviceListener);
		ConfigDatabase db = conf.ensureDatabase("frodo");
		db.addListener(dbListener);
	}

	@Invalidate
	public void stop() {
		if (orgUnitApi != null)
			orgUnitApi.removeEntityEventListener(orgUnitEntityEventListener);

		if (conf != null) {
			conf.removeListener(serviceListener);
			ConfigDatabase db = conf.ensureDatabase("frodo");
			db.removeListener(dbListener);
		}
	}

	private class OrgUnitEntityEventListener extends DefaultEntityEventListener<OrganizationUnit> {
		@Override
		public void entityAdded(String domain, OrganizationUnit obj, Object state) {
			orgUnits.put(obj.getGuid(), obj);
		}

		@Override
		public void entityUpdated(String domain, OrganizationUnit obj, Object state) {
			orgUnits.put(obj.getGuid(), obj);
		}

		@Override
		public void entityRemoved(String domain, OrganizationUnit obj, Object state) {
			orgUnits.remove(obj.getGuid());
			removeOrgUnitExtension(obj.getGuid());
		}
	}

	private void removeOrgUnitExtension(String ourUnitGuid) {
		ConfigDatabase db = conf.ensureDatabase("frodo");
		ConfigIterator it = null;
		try {
			it = db.find(OrgUnitExtension.class, Predicates.field("org_unit_id", ourUnitGuid));
			while (it.hasNext()) {
				Config c = it.next();
				db.remove(c, true);
			}
		} finally {
			if (it != null)
				it.close();
		}
	}

	private void initializeCache() {
		if (orgApi.findOrganization("localhost") == null)
			return;

		ConfigIterator it = null;
		try {
			for (OrganizationUnit orgUnit : orgUnitApi.getOrganizationUnits("localhost"))
				orgUnits.put(orgUnit.getGuid(), orgUnit);

			ConfigDatabase db = conf.ensureDatabase("frodo");
			it = db.findAll(OrgUnitExtension.class);
			while (it.hasNext()) {
				OrgUnitExtension ouext = it.next().getDocument(OrgUnitExtension.class, newParseCallback());
				ouexts.put((ouext.getOrgUnitId() != null) ? ouext.getOrgUnitId() : "", ouext);
			}
			it.close();

			it = db.findAll(AccessProfile.class);
			while (it.hasNext()) {
				AccessProfile profile = it.next().getDocument(AccessProfile.class, newParseCallback());
				profs.put(profile.getGuid(), profile);
			}
			it.close();
		} catch (DOMException e) {
			logger.warn("frodo model: cannot cache org units", e);
		} finally {
			if (it != null)
				it.close();
		}
	}

	@Override
	public AccessProfile getProfile(int id) {
		for (AccessProfile p : profs.values())
			if (p.getId() == id)
				return p;

		return null;
	}

	@Override
	public List<AccessProfile> getAccessProfiles() {
		// 순서 때문에 일단 DB에서 검색하는 것으로 처리
		ConfigDatabase db = conf.ensureDatabase("frodo");
		ConfigIterator it = db.findAll(AccessProfile.class);
		return (List<AccessProfile>) it.getDocuments(AccessProfile.class, newParseCallback());
	}

	@Override
	public AccessProfile determineOrgUnitProfile(String orgUnitName) {
		OrganizationUnit orgUnit = orgUnitApi.findOrganizationUnitByName("localhost", orgUnitName);
		return determineOrgUnitProfile(orgUnit);
	}

	@Override
	public AccessProfile determineProfile(String loginName) {
		User user = domUserApi.findUser("localhost", loginName);
		if(user == null)
			return null;
		return determineProfile(user);
	}

	@Override
	public AccessProfile determineProfile(User user) {
		UserExtension ext = userApi.getUserExtension(user);
		if (ext != null && ext.getProfile() != null)
			return ext.getProfile();

		OrganizationUnit unit = user.getOrgUnit();
		return determineOrgUnitProfile(unit);
	}

	private AccessProfile determineOrgUnitProfile(OrganizationUnit unit) {
		while (unit != null) {
			OrgUnitExtension ouext = ouexts.get(unit.getGuid());
			if (ouext != null && ouext.getProfile() != null)
				ouext.setProfile(profs.get(ouext.getProfile().getGuid()));
			if (ouext != null && ouext.getProfile() != null)
				return ouext.getProfile();

			String parent = unit.getParent();
			if (parent == null)
				break;

			unit = orgUnits.get(parent);
		}

		// get default access profile
		return getDefaultProfile();
	}

	@Override
	public AccessProfile getAccessProfile(String guid) {
		ConfigDatabase db = conf.ensureDatabase("frodo");
		Config c = db.findOne(AccessProfile.class, Predicates.field("guid", guid));
		if (c == null)
			return null;

		return c.getDocument(AccessProfile.class, newParseCallback());
	}

	@Override
	public AccessProfile getDefaultProfile() {
		// check root profile
		OrgUnitExtension o = ouexts.get("");
		if (o != null && o.getProfile() != null)
			o.setProfile(profs.get(o.getProfile().getGuid()));
		if (o != null)
			return o.getProfile();

		ConfigDatabase db = conf.ensureDatabase("frodo");
		Config c = db.findOne(AccessProfile.class, null);
		return c.getDocument(AccessProfile.class, newParseCallback());
	}

	@Override
	public String createAccessProfile(AccessProfile p) {
		//Peter H. Nahm - Move to IOT version
		//checkDuplicate(p, true);

		ConfigDatabase db = conf.ensureDatabase("frodo");
		p.setId(counter.incrementAndGet());
		p.setCreateDateTime(new Date());
		p.setUpdateDateTime(new Date());
		db.add(p);
		profs.put(p.getGuid(), p);

		fireEntityAdded("localhost", p);
		return p.getGuid();
	}

	@Override
	public void updateAccessProfile(AccessProfile p) {
		ConfigDatabase db = conf.ensureDatabase("frodo");
		Config c = db.findOne(AccessProfile.class, Predicates.field("guid", p.getGuid()));
		if (c == null)
			throw new IllegalStateException("profile not found: " + p.getGuid());

		//Peter H. Nahm - Move to IOT version
		//checkDuplicate(p, false);

		AccessProfile old = c.getDocument(AccessProfile.class, newParseCallback());
		p.setId(old.getId());
		p.setCreateDateTime(old.getCreateDateTime());
		p.setUpdateDateTime(new Date());
		db.update(c, p, true);
		profs.put(p.getGuid(), p);
		fireEntityUpdated("localhost", p);
	}

	@Override
	public void removeAccessProfile(String guid) {
		checkRemovable(Arrays.asList(guid));

		ConfigDatabase db = conf.ensureDatabase("frodo");
		Config c = db.findOne(AccessProfile.class, Predicates.field("guid", guid));
		if (c == null)
			return;

		AccessProfile p = c.getDocument(AccessProfile.class);
		db.remove(c, true);
		profs.remove(p.getGuid());
		fireEntityRemoved("localhost", p);
	}

	@Override
	public void removeAccessProfiles(List<String> guids) {
		checkRemovable(guids);

		ConfigDatabase db = conf.ensureDatabase("frodo");

		ConfigIterator it = db.find(AccessProfile.class, Predicates.in("guid", guids));
		if (!it.hasNext()) {
			it.close();
			return;
		}

		ConfigTransaction xact = db.beginTransaction();
		List<AccessProfile> profiles = new ArrayList<AccessProfile>();
		try {
			while (it.hasNext()) {
				Config c = it.next();
				AccessProfile prof = c.getDocument(AccessProfile.class);
				profiles.add(prof);
				db.remove(xact, c, true);
			}
			xact.commit("frodo-model", "remove access profile " + guids.toString());
		} catch (Throwable t) {
			xact.rollback();
			throw new RollbackException(t);
		} finally {
			if (it != null)
				it.close();
		}

		for (AccessProfile profile : profiles)
			fireEntityRemoved("localhost", profile);
	}

	private void checkDuplicate(AccessProfile p, boolean isCreate) {
		if (isCreate) {
			ConfigDatabase db = conf.ensureDatabase("frodo");
			Config c = db.findOne(AccessProfile.class, Predicates.field("guid", p.getGuid()));
			if (c != null)
				throw new IllegalStateException("duplicated guid: " + p.getGuid());

			c = db.findOne(AccessProfile.class, Predicates.field("name", p.getName()));
			if (c != null)
				throw new IllegalStateException("duplicated name: " + p.getGuid());
		}

		for (AccessProfile profile : getAccessProfiles()) {
			if (!isCreate && p.getGuid().equals(profile.getGuid()))
				continue;
			if (p.checkDuplicate().equals(profile.checkDuplicate()))
				throw new IllegalStateException("duplicated profile");
		}

	}

	private void checkRemovable(List<String> guids) {
		ConfigDatabase db = conf.ensureDatabase("frodo");
		ConfigIterator profilesIt = null;
		ConfigIterator orgUnitExtIt = null;
		try {
			profilesIt = db.findAll(AccessProfile.class);
			if (profilesIt.getDocuments(AccessProfile.class).size() - guids.size() < 1)
				throw new IllegalStateException("cannot remove profile guids=" + guids);

			orgUnitExtIt = db.find(OrgUnitExtension.class, Predicates.in("profile/guid", guids));
			while (orgUnitExtIt.hasNext()) {
				Config c = orgUnitExtIt.next();
				String orgUnitGuid = c.getDocument(OrgUnitExtension.class).getOrgUnitId();
				OrganizationUnit orgUnit = orgUnitApi.findOrganizationUnit("localhost", orgUnitGuid);
				if (orgUnitGuid == null || orgUnit != null)
					throw new ReferenceException(getReferencedValues(guids));
			}

			for(String guid : guids) {
				List<String> loginNames = domUserApi.getLoginNamesByAccessProfileGuid(guid);
				if(loginNames != null && loginNames.size() > 0 )
					throw new ReferenceException(getReferencedValues(guids));
			}
		} finally {
			if (profilesIt != null)
				profilesIt.close();
			if (orgUnitExtIt != null)
				orgUnitExtIt.close();
		}
	}

	public Map<String, Object> getReferencedValues(List<String> guids) {
		ConfigDatabase db = conf.ensureDatabase("frodo");
		List<String> orgUnitNames = new ArrayList<String>();
		List<String> userNames = new ArrayList<String>();
		Map<String, Object> m = new HashMap<String, Object>();

		ConfigIterator orgUnitIt = null;
		ConfigIterator userIt = null;
		try {
			orgUnitIt = db.find(OrgUnitExtension.class, Predicates.in("profile/guid", guids));
			m.put("org", false);
			while (orgUnitIt.hasNext()) {
				Config c = orgUnitIt.next();
				OrgUnitExtension ext = c.getDocument(OrgUnitExtension.class);
				if (ext.getOrgUnitId() != null) {
					OrganizationUnit orgUnit = orgUnitApi.findOrganizationUnit("localhost", ext.getOrgUnitId());
					if (orgUnit == null) {
						removeOrgUnitExtension(ext.getOrgUnitId());
						continue;
					}
					String name = orgUnit.getName();
					orgUnitNames.add(name);
				} else
					m.put("org", true);
			}

			for(String guid : guids) {
				List<String> loginNames = domUserApi.getLoginNamesByAccessProfileGuid(guid);
				userNames.addAll(loginNames);
			}			
		} finally {
			if (orgUnitIt != null)
				orgUnitIt.close();
			if (userIt != null)
				userIt.close();
		}

		m.put("org_unit_names", orgUnitNames);
		m.put("user_names", userNames);
		return m;
	}

	@Override
	public OrgUnitExtension findOrgUnitExtension(String orgUnitId) {
		ConfigDatabase db = conf.ensureDatabase("frodo");
		Config c = db.findOne(OrgUnitExtension.class, Predicates.field("orgUnitId", orgUnitId));
		if (c == null)
			return null;
		return c.getDocument(OrgUnitExtension.class, newParseCallback());
	}

	private PrimitiveParseCallback newParseCallback() {
		return new PrimitiveParseCallback() {
			@Override
			public <T> T onParse(Class<T> clazz, Map<String, Object> referenceKey) {
				ConfigDatabase db = conf.ensureDatabase("frodo");
				Config c = db.findOne(clazz, Predicates.field(referenceKey));
				if (c == null)
					return null;
				return c.getDocument(clazz, newParseCallback());
			}
		};
	}

	@Override
	public void setOrgUnitExtension(String orgUnitId, String profileId, boolean setChildren) {
		ConfigDatabase db = conf.ensureDatabase("frodo");

		AccessProfile profile = null;
		if (profileId != null) {
			Config c = db.findOne(AccessProfile.class, Predicates.field("guid", profileId));
			if (c != null)
				profile = c.getDocument(AccessProfile.class);
		}

		OrganizationUnit orgUnit = orgUnitApi.findOrganizationUnit("localhost", orgUnitId);
		if (orgUnit != null || orgUnitId == null)
			setProfile(db, orgUnit, profile, setChildren);
	}

	private void setProfile(ConfigDatabase db, OrganizationUnit orgUnit, AccessProfile profile, boolean setChildren) {
		String guid = (orgUnit != null) ? orgUnit.getGuid() : null;
		Config c = db.findOne(OrgUnitExtension.class, Predicates.field("orgUnitId", guid));
		if (c == null) {
			OrgUnitExtension o = new OrgUnitExtension();
			o.setOrgUnitId(guid);
			o.setProfile(profile);
			o.setCreateDateTime(new Date());
			o.setUpdateDateTime(new Date());
			ouexts.put(o.getOrgUnitId() != null ? o.getOrgUnitId() : "", o);
			db.add(o);
		} else {
			OrgUnitExtension old = c.getDocument(OrgUnitExtension.class);
			old.setProfile(profile);
			old.setUpdateDateTime(new Date());
			ouexts.put(old.getOrgUnitId() != null ? old.getOrgUnitId() : "", old);
			db.update(c, old, true);
		}

		if (setChildren) {
			Collection<OrganizationUnit> childs = null;
			if (orgUnit != null)
				childs = orgUnit.getChildren();
			else
				childs = orgUnitApi.getOrganizationUnits("localhost");

			for (OrganizationUnit child : childs)
				setProfile(db, child, profile, setChildren);
		}
	}

	@Override
	public List<Object> serialize() {
		List<Object> l = new ArrayList<Object>();

		for (AccessProfile profile : getAccessProfiles()) {
			Map<String, Object> m = profile.marshal();
			m.put("normal_acl", serializeEndpoints(profile.getNormalAcl()));
			m.put("abnormal_acl", serializeEndpoints(profile.getQuarantineAcl()));
			m.put("internal", Marshaler.marshal(profile.getInternalNetworkRanges()));
			l.add(m);
		}

		return l;
	}

	@Override
	public boolean isAllAuthMethodSame() {
		List<AccessProfile> profiles = getAccessProfiles();
		int gwMethod = gwApi.getCurrentAccessGateway().getLoginMethod();
		for (AccessProfile profile : profiles) {
			if (profile.getLoginMethod() != null && profile.getLoginMethod() != gwMethod)
				return false;
		}

		return true;
	}

	@Override
	public boolean isAllEncryptionsSame() {
		List<AccessProfile> profiles = getAccessProfiles();
		String gwEncryptions = vpnServerConfigApi.getCurrentVpnServerConfig().getEncryptions().get(0);
		for (AccessProfile profile : profiles) {
			String profileEncryption = null;
			if (profile.getEncryptions() == null)
				profileEncryption = vpnServerConfigApi.getCurrentVpnServerConfig().getEncryptions().get(0);
			else
				profileEncryption = profile.getEncryptions().get(0);

			if ((profileEncryption != null) && !(profileEncryption.equals(gwEncryptions)))
				return false;
		}

		return true;
	}

	private List<Object> serializeEndpoints(List<Server> servers) {
		List<Object> l = new ArrayList<Object>();

		for (Server server : servers)
			for (IpEndpoint e : server.getEndpoints())
				l.add(e.marshal());

		return l;
	}

	private class ClientCheckProfileUpdater implements ClientCheckProfileEventListener {
		@Override
		public void onUpdated(ClientCheckProfile ccp) {
			for (AccessProfile p : profs.values()) {
				ClientCheckProfile clientCheckProfile = p.getClientCheckProfile();
				if (clientCheckProfile != null && clientCheckProfile.getGuid().equals(ccp.getGuid()))
					p.setClientCheckProfile(ccp);
			}
		}

		@Override
		public void onCreated(ClientCheckProfile p) {
		}

		@Override
		public void onRemoved(ClientCheckProfile p) {
		}
	}

	private class ClientAppUpdater implements ClientAppEventListener {
		@Override
		public void onUpdated(ClientApp newApp) {
			String guid = newApp.getGuid();

			for (AccessProfile p : profs.values()) {
				for (ClientApp oldApp : p.getClientApps()) {
					String oldGuid = oldApp.getGuid();
					if (oldGuid.equals(guid)) {
						p.getClientApps().remove(oldApp);
						p.getClientApps().add(newApp);
						break;
					}
				}
			}
		}

		@Override
		public void onCreated(ClientApp app) {
		}

		@Override
		public void onRemoved(ClientApp app) {
		}
	}

	private class ConfDbListener extends BaseConfigDatabaseListener {
		@Override
		public void onImport(ConfigDatabase db) {
			if (!db.getName().equals("frodo"))
				return;
			logger.debug("frodo model: receive import event, initialize cache");
			initializeCache();
		}
	}

	private class ConfServiceListener extends BaseConfigServiceListener {
		@Override
		public void onCreateDatabase(ConfigDatabase db) {
			if (!db.getName().equals("frodo"))
				return;
			db.addListener(dbListener);
		}
	}
}
