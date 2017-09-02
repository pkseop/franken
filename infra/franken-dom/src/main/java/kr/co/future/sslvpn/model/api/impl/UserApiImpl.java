package kr.co.future.sslvpn.model.api.impl;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import kr.co.future.sslvpn.model.AccessProfile;
import kr.co.future.sslvpn.model.ClientIpRange;
import kr.co.future.sslvpn.model.UserExtension;
import kr.co.future.sslvpn.model.api.AccessProfileApi;
import kr.co.future.sslvpn.model.api.UserApi;

import org.apache.felix.ipojo.annotations.Bind;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Invalidate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.apache.felix.ipojo.annotations.Unbind;
import org.apache.felix.ipojo.annotations.Validate;

import kr.co.future.api.PrimitiveConverter;
import kr.co.future.api.PrimitiveParseCallback;
import kr.co.future.confdb.Config;
import kr.co.future.confdb.ConfigDatabase;
import kr.co.future.confdb.ConfigService;
import kr.co.future.confdb.Predicate;
import kr.co.future.confdb.Predicates;
import kr.co.future.dom.api.ConfigManager;
import kr.co.future.dom.api.DefaultEntityEventListener;
import kr.co.future.dom.api.EntityEventListener;
import kr.co.future.dom.api.OrganizationApi;
import kr.co.future.dom.model.Organization;
import kr.co.future.dom.model.User;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(name = "frodo-user-api")
@Provides
public class UserApiImpl implements UserApi {
    private static final String LOCALHOST = "localhost";
    private static final Class<User> cls = User.class;
    private static final String NOT_FOUND = "user-not-found";

    private Logger logger = LoggerFactory.getLogger(UserApiImpl.class);

    private EntityEventListener<AccessProfile> profileEventListener = new DefaultEntityEventListener<AccessProfile>() {
        @Override
        public void entityUpdated(String domain, AccessProfile obj, Object state) {
            profiles.put(obj.getGuid(), obj);
        }

        @Override
        public void entityRemoved(String domain, AccessProfile obj, Object state) {
            profiles.remove(obj.getGuid());
        }

        @Override
        public void entityAdded(String domain, AccessProfile obj, Object state) {
            profiles.put(obj.getGuid(), obj);
        }
    };

    @Requires
    private ConfigManager cfg;

    @Requires
    private ConfigService conf;

    @Requires
    private kr.co.future.dom.api.UserApi domUserApi;

    @Requires
    private OrganizationApi orgApi;

    private AccessProfileApi profileApi;

    @Bind(optional = true)
    public void bindAccessProfileApi(AccessProfileApi profileApi) {
        this.profileApi = profileApi;
        profileApi.addEntityEventListener(profileEventListener);
    }

    @Unbind
    public void unbindAccessProfileApi() {
        this.profileApi.removeEntityEventListener(profileEventListener);
        this.profileApi = null;
    }

    private ConcurrentHashMap<String, AccessProfile> profiles = new ConcurrentHashMap<String, AccessProfile>();

    @Validate
    public void validate() {
        if (profileApi != null)
            profileApi.addEntityEventListener(profileEventListener);
    }

    @Invalidate
    public void invalidate() {
        if (profileApi != null)
            profileApi.removeEntityEventListener(profileEventListener);
    }

    @Override
    public String getExtensionName() {
        return "frodo";
    }

    private Predicate getPred() {
        return Predicates.not(Predicates.field("ext/frodo", null));
    }

    private Predicate byLoginName(String loginName) {
        return Predicates.and(getPred(), Predicates.field("loginName", loginName));
    }

    private Predicate byLoginNames(Set<String> loginName) {
        return Predicates.and(getPred(), Predicates.in("loginName", loginName));
    }

    private Predicate byDeviceAuthKey(String deviceKey) {
        return Predicates.and(getPred(), Predicates.field("ext/frodo/deviceAuthKey", deviceKey));
    }

    private Predicate byReservedIp(InetAddress reservedIp) {
        return Predicates.and(getPred(), Predicates.field("ext/frodo/staticIp4", reservedIp));
    }

    private Predicate forOtherReservation(String loginName, InetAddress reservedIp) {
        return Predicates.and(byLoginName(loginName), Predicates.field("ext/frodo/staticIp4", reservedIp));
    }

    private UserExtension parseUser(String domain, User user) {
        return parseUser(domain, user, newParseCallback());
    }

    private UserExtension parseUser(String domain, User user, PrimitiveParseCallback parseCallback) {
        if (user == null || user.getExt() == null) {
            return null;
        }

        if (user.getExt().get("frodo") == null) {
            return null;
        }

        UserExtension userExtension = null;//PrimitiveConverter.parse(UserExtension.class, user.getExt().get(getExtensionName()), parseCallback);

        if (user.getExt().get("frodo") instanceof UserExtension) {
            userExtension = (UserExtension) user.getExt().get("frodo");
        } else {
            if (user.getExt().get("frodo") instanceof Map) {
                userExtension = PrimitiveConverter.parse(UserExtension.class, user.getExt().get("frodo"));
            }
        }

        // at first, old extension can be null
        if (userExtension == null) {
            return null;
        }

        user.getExt().put(getExtensionName(), userExtension);
        userExtension.setUser(user);
        return userExtension;
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
    public UserExtension checkAuthKey(String authKey) {
        User user = domUserApi.getUserWithDeviceAuthKey(LOCALHOST, authKey);
        if (user == null)
            return null;
        return parseUser(LOCALHOST, user);
    }

    @Override
    public List<UserExtension> getUserExtensions() {
        List<UserExtension> users = new ArrayList<UserExtension>();
        PrimitiveParseCallback parseCallback = newParseCallback();

        for (User user : domUserApi.getUsers(LOCALHOST)) {
            UserExtension ext = parseUser(LOCALHOST, user, parseCallback);
            if (ext != null)
                users.add(ext);
        }
        return users;
    }

    @Override
    public List<UserExtension> getUserExtensions(Set<String> loginNames) {
        List<UserExtension> users = new ArrayList<UserExtension>();
        PrimitiveParseCallback parseCallback = newParseCallback();

        for (User user : domUserApi.getUsers(LOCALHOST, loginNames)) {
            UserExtension ext = parseUser(LOCALHOST, user, parseCallback);
            if (ext != null)
                users.add(ext);
        }

        return users;
    }

    @Override
    public List<UserExtension> getUserExtensions(Collection<User> users) {
        List<UserExtension> exts = new ArrayList<UserExtension>();
        PrimitiveParseCallback parseCallback = newParseCallback();

        for (User user : users) {
            UserExtension ext = parseUser(LOCALHOST, user, parseCallback);
            if (ext != null)
                exts.add(ext);
        }

        return exts;
    }
    
    @Override
    public List<UserExtension> getUserExtsWithStaticIp4() {
    	List<UserExtension> exts = new ArrayList<UserExtension>();
    	for(User user : domUserApi.getUsersWithCondOnUserExt("staticIp4 is not null")) {
    		UserExtension ext = parseUser(LOCALHOST, user, null);
            if (ext != null)
                exts.add(ext);
    	}
    	return exts;
    }

    @Override
    public UserExtension getUserExtension(User user) {
        return parseUser(LOCALHOST, user);
    }

    @Override
    public UserExtension findUserExtension(String loginName) {
        User user = domUserApi.findUser(LOCALHOST, loginName);
        if (user == null)
            return null;
        return parseUser(LOCALHOST, user);
    }

    @Override
    public UserExtension findUserExtension(InetAddress reservedIp) {
        if (reservedIp == null)
            return null;

        User user = domUserApi.getUserWithReservedIp(LOCALHOST, reservedIp.getHostAddress());
        if (user == null)
            return null;

        return parseUser(LOCALHOST, user);
    }

    @Override
    public UserExtension findUserExtension(String exceptLoginName, InetAddress reservedIp) {
        if (reservedIp == null)
            return null;

        User user = domUserApi.findOtherUserUseReservedIp(LOCALHOST, exceptLoginName, reservedIp.getHostAddress());
        if (user == null)
            return null;

        return parseUser(LOCALHOST, user);
    }

    @Override
    public void setUserExtension(UserExtension ext) {
        User user = domUserApi.getUser(LOCALHOST, ext.getUser().getLoginName());
        ext.setUpdateDateTime(new Date());
        user.getExt().put(getExtensionName(), ext);
        domUserApi.updateUser(LOCALHOST, user, false);
    }

    @Override
    public void removeUserExtension(String loginName) {
        User user = domUserApi.getUser(LOCALHOST, loginName);
        user.getExt().remove(getExtensionName());
        domUserApi.updateUser(LOCALHOST, user, false);
    }

    @Override
    public int getDomUserCount() {
        return domUserApi.countUsers("localhost", null, true, null, false);
    }

    @Override
    public void setDomUserCount(int count, Boolean flag) {
//		ConfigDatabase db = conf.ensureDatabase("kraken-dom-localhost");
//		Config c = db.findOne(Organization.class, null);
//
//		if (orgApi.findOrganization("localhost") != null) {
//			Organization org = orgApi.getOrganization("localhost");
//
//			if (org != null) {
//				int cnt = 0;
//				int users_count = Integer.parseInt((String) org.getParameters().get("dom.admin.users_count"));
//
//				if (flag == null)
//					cnt = count;
//				else {
//					if (flag)
//						cnt = users_count + count;
//					else
//						cnt = users_count - count;
//				}
//
//				String strCount= new Integer(cnt).toString();
//				org.getParameters().put("dom.admin.users_count", strCount);
//				db.update(c, org);
//			}
//		}
    }
}
