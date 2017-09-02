package kr.co.future.sslvpn.core.login;

import java.io.IOException;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import kr.co.future.confdb.Predicates;
import kr.co.future.dom.api.OrganizationUnitApi;
import kr.co.future.dom.api.TimetableApi;
import kr.co.future.dom.model.OrganizationUnit;
import kr.co.future.dom.model.User;
import kr.co.future.ldap.LdapProfile;
import kr.co.future.ldap.LdapProfile.CertificateType;
import kr.co.future.ldap.LdapServerType;
import kr.co.future.ldap.LdapService;
import kr.co.future.ldap.LdapUser;
import kr.co.future.radius.client.RadiusClient;
import kr.co.future.radius.client.auth.Authenticator;
import kr.co.future.radius.client.auth.ChapAuthenticator;
import kr.co.future.radius.client.auth.PapAuthenticator;
import kr.co.future.radius.protocol.AccessAccept;
import kr.co.future.radius.protocol.RadiusPacket;
import kr.co.future.rpc.RpcException;
import kr.co.future.sslvpn.core.AuthCode;
import kr.co.future.sslvpn.core.CrlValidator;
import kr.co.future.sslvpn.core.DupLoginCheck;
import kr.co.future.sslvpn.core.ExternalAuthService;
import kr.co.future.sslvpn.core.OsType;
import kr.co.future.sslvpn.core.SqlAuthResult;
import kr.co.future.sslvpn.core.SqlAuthService;
import kr.co.future.sslvpn.core.cluster.ClusterService;
import kr.co.future.sslvpn.core.impl.CrlConnectionErrorException;
import kr.co.future.sslvpn.core.impl.InternalIp;
import kr.co.future.sslvpn.core.impl.SaltGenerator;
import kr.co.future.sslvpn.core.servlet.AuthServiceServlet;
import kr.co.future.sslvpn.core.xenics.XenicsService;
import kr.co.future.sslvpn.model.AccessGateway;
import kr.co.future.sslvpn.model.AccessGateway.DeviceAuthMode;
import kr.co.future.sslvpn.model.AccessGateway.IdentificationMode;
import kr.co.future.sslvpn.model.AccessGateway.LdapMode;
import kr.co.future.sslvpn.model.AccessProfile;
import kr.co.future.sslvpn.model.AuthorizedDevice;
import kr.co.future.sslvpn.model.UserExtension;
import kr.co.future.sslvpn.model.api.AccessGatewayApi;
import kr.co.future.sslvpn.model.api.AccessProfileApi;
import kr.co.future.sslvpn.model.api.AuthorizedDeviceApi;
import kr.co.future.sslvpn.model.api.IpLeaseApi;
import kr.co.future.sslvpn.model.api.UserApi;
import kr.co.future.sslvpn.xtmconf.BridgeTunCachingService;
import kr.co.future.sslvpn.xtmconf.XtmConfig;
import kr.co.future.sslvpn.xtmconf.network.Radius;
import kr.co.future.sslvpn.xtmconf.network.Radius.AuthMethod;
import kr.co.future.sslvpn.xtmconf.network.Radius.Fileserver;

import org.bouncycastle.asn1.ASN1Encodable;
import org.bouncycastle.asn1.DERBitString;
import org.bouncycastle.asn1.DERPrintableString;
import org.bouncycastle.asn1.DERSequence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LoginUtil {
	private final Logger logger = LoggerFactory.getLogger(AuthServiceServlet.class);
	public final Logger internalLogger = LoggerFactory.getLogger(InternalIp.class.getName());
	
	public static final int LoginMethod_PWONLY = 1;
	public static final int LoginMethod_PRIVCERT = 2;
	public static final int LoginMethod_PW_PRIVCERT = 3;
	public static final int LoginMethod_NPKI = 4;
	public static final int LoginMethod_PW_NPKI = 5;
	public static final int LoginMethod_PW_OTP = 6;
	
	public AccessGatewayApi gatewayApi;
	public kr.co.future.dom.api.UserApi domUserApi;
	public AccessProfileApi profileApi;
	public UserApi userApi;
	public OrganizationUnitApi orgUnitApi;	
	public ExternalAuthService externalAuth;	
	public AuthorizedDeviceApi authDeviceApi;
	public SqlAuthService sqlAuth;
	public ClusterService cluster;
	public BridgeTunCachingService bridgeCache;
	public IpLeaseApi ipLeaseApi;
	public LdapService ldap;
	public TimetableApi timetableApi;
	public DupLoginCheck dupLoginCheck;
	public XenicsService xenicsService;
	
//	private ConcurrentMap<String, UpdateRequest> transientUsers = new ConcurrentHashMap<String, UpdateRequest>();
//	private BlockingQueue<UpdateRequest> updateQueue = new ArrayBlockingQueue<UpdateRequest>(1000, true);
//	private Thread updateThread;
//	private BatchUpdater updater = new BatchUpdater();
	
	public LoginUtil(AccessGatewayApi gatewayApi, kr.co.future.dom.api.UserApi domUserApi, 
			AccessProfileApi profileApi, UserApi userApi2, OrganizationUnitApi orgUnitApi, ExternalAuthService externalAuth, 
			AuthorizedDeviceApi authDeviceApi, ClusterService cluster, BridgeTunCachingService bridgeCache,
			IpLeaseApi ipLeaseApi, LdapService ldap, TimetableApi timetableApi, SqlAuthService sqlAuth,
			DupLoginCheck dupLoginCheck, XenicsService xenicsService) {
		this.gatewayApi = gatewayApi;
		this.domUserApi = domUserApi;
		this.profileApi = profileApi;
		this.userApi = userApi2;
		this.orgUnitApi = orgUnitApi;
		this.externalAuth = externalAuth;
		this.authDeviceApi = authDeviceApi;
		this.cluster = cluster;
		this.bridgeCache = bridgeCache;
		this.ipLeaseApi = ipLeaseApi;
		this.ldap = ldap;
		this.timetableApi = timetableApi;
		this.sqlAuth = sqlAuth;
		this.dupLoginCheck = dupLoginCheck;
		this.xenicsService = xenicsService;
		
		start();
	}
	
	public void start() {
//		updateThread = new Thread(updater, "Frodo Login Sync");
//		updateThread.start();
	}
	
	public void stop() {
//		updater.stop();
//		if (updateThread != null)
//			updateThread.interrupt();

		logger.info("frodo core: stopping auth rpc service, clearing lease2tunnel table");
		//listeners.clear();
		
	}
	
//	private class UpdateRequest {
//		private User user;
//
//		public UpdateRequest(User user) {
//			this.user = user;
//		}
//	}
	
	public User findUser(String loginName) {
//		UpdateRequest req = transientUsers.get(loginName);
//		if (req != null)
//			return req.user;

		return domUserApi.findUser("localhost", loginName);
	}
	
	public UserExtension createUserExtension(User user, String leaseIp) {
		UserExtension ext;
		ext = new UserExtension();
		ext.setUser(user);
		ext.setCreateDateTime(new Date());
		ext.setUpdateDateTime(new Date());
		ext.setLastIp(leaseIp);
		user.getExt().put("frodo", ext);
		return ext;
	}
	
	public void submitUpdate(User user) {
//		UpdateRequest req = new UpdateRequest(user);
//		synchronized (transientUsers) {
//			transientUsers.put(user.getLoginName(), req);
//		}
//
//		try {
//			updateQueue.put(req);
//		} catch (InterruptedException e) {
//			throw new IllegalStateException("auth update queue interrupted");
//		}
		domUserApi.updateUser("localhost", user, false);
	}
	
//	private class BatchUpdater implements Runnable {
//		private volatile boolean doStop;
//
//		@Override
//		public void run() {
//			doStop = false;
//
//			try {
//				while (!doStop) {
//					try {
//						sync();
//					} catch (InterruptedException e) {
//						logger.trace("frodo core: login sync interrupted");
//					} catch (Throwable t) {
//						logger.error("frodo core: cannot sync users", t);
//					}
//				}
//			} finally {
//				logger.info("frodo core: login sync thread stopped");
//			}
//		}
//
//		public void stop() {
//			doStop = true;
//		}
//
//		private void sync() throws InterruptedException {
//			int sleepTime = 5000;		//pks. 2014-11-04. changed from 500 to 5000.
//			ArrayList<UpdateRequest> round = new ArrayList<UpdateRequest>();
//			updateQueue.drainTo(round);
//
//			// should not put new request while removing in progress
//			Map<String, Config> configMap = findUsers(round);
//
//			if (configMap == null) {
//				Thread.sleep(sleepTime);
//				return;
//			}
//
//			ArrayList<ConfigUpdateRequest<User>> users = new ArrayList<ConfigUpdateRequest<User>>();
//			synchronized (transientUsers) {
//				for (UpdateRequest req : round) {
//					User user = req.user;
//					users.add(new ConfigUpdateRequest<User>(configMap.get(user.getLoginName()), user));
//
//					logger.trace("frodo core: batch updating user [{}], ext [{}]", user.getLoginName(), user.getExt()
//							.get("frodo"));
//
//					UpdateRequest r = transientUsers.get(user.getLoginName());
//					if (r == req)
//						transientUsers.remove(user.getLoginName());
//				}
//			}
//
//			if (users.size() > 0)
//				domUserApi.updateUsers("localhost", users);
//
//			Thread.sleep(sleepTime);
//		}
//
//		private Map<String, Config> findUsers(ArrayList<UpdateRequest> round) {
//			try {
//				HashSet<String> loginNames = new HashSet<String>();
//				if (round.size() == 0)
//					return null;
//
//				for (UpdateRequest req : round)
//					loginNames.add(req.user.getLoginName());
//
//				List<Config> configs = cfg.matches("localhost", User.class, Predicates.in("login_name", loginNames), 0,
//						Integer.MAX_VALUE);
//
//				Map<String, Config> m = new HashMap<String, Config>();
//				for (Config c : configs) {
//					@SuppressWarnings("unchecked")
//					Map<String, Object> userMap = (Map<String, Object>) c.getDocument();
//					m.put((String) userMap.get("login_name"), c);
//				}
//
//				return m;
//			} catch (Exception e) {
//				logger.trace("frodo core: kraken-dom-localhost database does not exist", e);
//				return null;
//			}
//		}
//	}
	
	public Map<String, Object> success(InetAddress ip, AccessProfile profile) {
		Map<String, Object> m = new HashMap<String, Object>();
		m.put("auth_code", 0);
		m.put("ip", ip.getHostAddress());
		m.put("profile_id", profile.getId());
		return m;
	}
	
	public Map<String, Object> fail(AuthCode code, AccessProfile profile, int failCount) {
		if (code.getCode() == 0)
			logger.error("frodo core: fail code should not be 0");

		Map<String, Object> m = new HashMap<String, Object>();
		if (code == AuthCode.PasswordFail || code == AuthCode.OtpFail || code == AuthCode.ResidentNumberFail) {
			int authCode = code.getCode() & 0xFFF;
			failCount = (failCount & 0x0F) << 12;
			m.put("auth_code", authCode | failCount);
		} else {
			m.put("auth_code", code.getCode());
		}
		if (profile != null)
			m.put("profile_id", profile.getId());

		return m;
	}

	public Map<String, Object> fail(AuthCode code, AccessProfile profile) {
		return fail(code, profile, 0);
	}
	
	public void removeEmptyR(Map<String, Object> props) {
		// empty r check for android
		byte[] r = (byte[]) props.get("r");
		if (r != null) {
			boolean isEmpty = true;
			for (byte b : r) {
				if (b != 0) {
					isEmpty = false;
					break;
				}
			}

			if (isEmpty)
				props.remove("r");
		}
	}
	
	public void traceAuth(Map<String, Object> props) {
		StringBuilder sb = new StringBuilder();
		int i = 0;
		Map<String, Object> arrays = new HashMap<String, Object>();
		for (String key : props.keySet()) {
			if (key.equals("pw")) {
				Logger l = LoggerFactory.getLogger("kr.co.future.frodo.core.impl.AuthRpcService.password");
				l.debug("frodo core: password [{}]", props.get("pw"));
				continue;
			}
			
			if (key.equals("idn")) {
				continue;
			}

			if (i++ != 0)
				sb.append(", ");

			sb.append(key);
			sb.append('=');

			Object value = props.get(key);
			if (value != null) {
				String string = value.toString();
				sb.append(string);
				if (string.startsWith("[B")) { // byte array
					arrays.put(key, value);
				}
			}
		}

		logger.info("frodo core: try auth [{}]", sb.toString());
		if (logger.isDebugEnabled()) {
			for (Map.Entry<String, Object> entry : arrays.entrySet()) {
				logger.debug("{}=[{}]", entry.getKey(), toHex((byte[]) entry.getValue()));
			}
		}
	}
	
	public String toHex(byte[] b) {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < b.length; i++)
			sb.append(String.format("%02x", b[i]));

		return sb.toString();
	}
	
	public Integer getLoginMethod(AccessGateway gw, String loginName, User user) {
		Integer loginMethod = gw.getLoginMethod();
		AccessProfile profile = determineProfile(loginName, user);
		if (profile != null && profile.getLoginMethod() != null)
			loginMethod = profile.getLoginMethod();

		return loginMethod;
	}
	
	public boolean isLoginMethodWithPassword(AccessGateway gw, User user, String loginName) {
		Integer loginMethod = getLoginMethod(gw, loginName, user);
		return loginMethod == LoginMethod_PWONLY || loginMethod == LoginMethod_PW_PRIVCERT || loginMethod == LoginMethod_PW_NPKI || loginMethod == LoginMethod_PW_OTP;
	}
	
	public AccessProfile determineProfile(String loginName) {
		User user = domUserApi.findUser("localhost", loginName);
		return determineProfile(loginName, user);
	}
	
	public AccessProfile determineProfile(String loginName, User user) {
		boolean found = false;
		String orgUnitName = null;
		String userName = null;

		Map<String, Object> ext = null;
		AccessGateway gw = gatewayApi.getCurrentAccessGateway();

		if (user == null) {
			String sourceType = null;
			if (gw.isUseRadiusAuth()) {
				//여기서 사용자가 생성되지 않도록 함. 사용자 인증 시에 생성되도록 함.
				String radiusOrgUnitId = gw.getRadiusOrgUnitId();
				if (radiusOrgUnitId != null) {
					OrganizationUnit orgUnit = orgUnitApi.getOrganizationUnit("localhost", radiusOrgUnitId);
					orgUnitName = orgUnit.getName();
					logger.debug("frodo core: finding auth method, check radius auth, org unit [{}]", orgUnitName);
					return profileApi.determineOrgUnitProfile(orgUnitName);
				} else {
					return profileApi.getDefaultProfile();
				}
			}

			if (!found && gw.getLdapMode() == LdapMode.LDAP_SYNC) {
				LdapProfile profile = getLdapProfile();
				if (profile != null) {
					LdapUser ldapUser = ldap.findUser(profile, loginName);

					if (ldapUser != null) {
						found = true;
						orgUnitName = ldapUser.getOrganizationUnitName();
						userName = ldapUser.getDisplayName();
						sourceType = "ldap";
						logger.debug("frodo core: finding auth method, check ldap auth, org unit [{}]", orgUnitName);
					} else {
						logger.debug("frodo core: finding auth method, check ldap auth, login name not found [{}]", loginName);
					}
				}
			}

			if (!found && gw.getLdapMode() == LdapMode.LDAP_SYNC_EXTEND) {
				List<LdapProfile> profiles = new ArrayList<LdapProfile>();
				LdapProfile defaultProfile = getLdapProfile();
				if (defaultProfile != null)
					profiles.add(defaultProfile);
				profiles.addAll(ldap.getProfiles());

				for (LdapProfile profile : profiles) {
					if (profile.getServerType() != LdapServerType.ActiveDirectory)
						continue;

					LdapUser ldapUser = ldap.findUser(profile, loginName);
					if (ldapUser == null)
						continue;

					found = true;
					orgUnitName = ldapUser.getOrganizationUnitName();
					userName = ldapUser.getDisplayName();
					sourceType = "ldap";
					break;
				}
			}

			if (!found && sqlAuth.isEnabled()) {
				SqlAuthResult result = sqlAuth.verifyUser(loginName);
				if (result.isSuccess()) {
					found = true;
					orgUnitName = result.getOrgUnitName();
					userName = result.getName();
					sourceType = "sql";
					logger.debug("frodo core: finding auth method, check sql auth, login name found [{}]", loginName);
				} else {
					logger.debug("frodo core: finding auth method, check sql auth, login name not found [{}]", loginName);
				}
			}

			if (!found && externalAuth.isEnabled()) {
				Map<String, Object> result = externalAuth.verifyUser(loginName);
				boolean isSuccess = (Boolean) result.get("result");

				if (isSuccess) {
					found = true;
					orgUnitName = (String) result.get("org_unit_name");
					userName = (String) result.get("name");
					if (userName == null || userName.isEmpty())
						userName = loginName;
					sourceType = "external";
					logger.debug("frodo core: finding auth method, check external auth, login name found [{}]", loginName);
					ext = result;
				} else {
					logger.debug("frodo core: finding auth method, check external auth, login name not found [{}]", loginName);
				}
			}

			if (found) {
				createLocalUser(loginName, userName, orgUnitName, sourceType, ext, user);
				return profileApi.determineOrgUnitProfile(orgUnitName);
			}
			return null;
		} else {
			return profileApi.determineProfile(user);
		}
	}
	
	private User createLocalUser(String loginName, String userName, String orgUnitName, String sourceType,
			Map<String, Object> ext, User user) {
		logger.info("frodo core: create user => login name [{}], source type [{}]", new Object[]{loginName, sourceType});
		
		OrganizationUnit unit = null;
		if (orgUnitName != null) {
			unit = orgUnitApi.findOrganizationUnitByName("localhost", orgUnitName);
			logger.trace("frodo core: org unit [{}] for login [{}]", unit, loginName);
			if (unit == null) {
				unit = new OrganizationUnit();
				unit.setName(orgUnitName);
				orgUnitApi.createOrganizationUnit("localhost", unit);

			}
		}

		boolean isUpdate = true;

		if (user == null) {
			user = new User();
			isUpdate = false;
		}
		if(unit != null)
			user.setOrgUnit(unit);
		user.setLoginName(loginName);
		if (userName != null)
			user.setName(userName);
		user.setSourceType(sourceType);

		UserExtension userExt = null;
		if (ext != null) {
			if (isUpdate)
				userExt = userApi.getUserExtension(user);

			if (userExt == null)
				userExt = new UserExtension();

			if (!isUpdate) {
				if (ext.containsKey("device_count"))
					userExt.setDeviceKeyCountSetting((Integer) ext.get("device_count"));
			}

			if (ext.containsKey("static_ip")) {
				userExt.setStaticIp4((String) ext.get("static_ip"));
			}
			if (ext.containsKey("idn")) {
				userExt.setSalt(SaltGenerator.createSalt(20));
				String idnHash = domUserApi.hashPassword(userExt.getSalt(), ext.get("idn").toString().replace("-", ""));
				userExt.setIdnHash(idnHash);
			}
			if (ext.containsKey("source_type")) {
				userExt.setSourceType((String) ext.get("source_type"));
			}

			userExt.setUser(user);
			userExt.setUpdateDateTime(new Date());
			user.getExt().put("frodo", userExt);
		}

		if (isUpdate) {
			domUserApi.updateUser("localhost", user, false);
		} else {
			if (domUserApi.findUser("localhost", loginName) == null) {
				domUserApi.createUser("localhost", user);
			}
		}
		return user;
	}
	
	public LdapProfile getLdapProfile() {
		List<Radius> configs = XtmConfig.readConfig(Radius.class);
		Radius ldapConfig = null;

		for (Radius config : configs)
			if (config.getType().equals(Radius.Type.Domain))
				ldapConfig = config;

		if (ldapConfig == null || !ldapConfig.isLdapUse())
			return null;

		LdapProfile profile = new LdapProfile();
		profile.setName("frodo");
		profile.setDc(ldapConfig.getLdapAddress());
		profile.setAccount(ldapConfig.getLdapAccount());
		profile.setPassword(ldapConfig.getLdapPassword());
		profile.setBaseDn(ldapConfig.getLdapBaseDn());
		if (ldapConfig.getLdapType() == Fileserver.Active_Directory)
			profile.setServerType(LdapServerType.ActiveDirectory);
		else
			profile.setServerType(LdapServerType.SunOneDirectory);

		try {
			if (ldapConfig.isLdapUseTrustStore())
				profile.setTrustStore(CertificateType.X509, ldapConfig.getLdapTrustStore());
			return profile;
		} catch (GeneralSecurityException e) {
			logger.error("frodo core: cannot ldap login, trust store load fail", e);
			throw new RpcException("illegal ldaps crt: " + e.getMessage());
		} catch (IOException e) {
			logger.error("frodo core: cannot ldap login, trust store load fail", e);
			throw new RpcException("ldaps crt io error: " + e.getMessage());
		} catch (ParseException e) {
			logger.error("frodo core: cannot ldap login, trust store load fail", e);
			throw new RpcException("ldaps crt io error: " + e.getMessage());
      }
	}
	
	public AuthCode checkPasswordLogin(Map<String, Object> props) {
		return checkPasswordLogin(props, findUser((String) props.get("id")));
	}
	
	public AuthCode checkPasswordLogin(Map<String, Object> props, User user) {
		String loginName = (String) props.get("id");
		String password = (String) props.get("pw");
		String otp = (String) props.get("client_otp");
		String rrn = (String) props.get("rrn");			//주민등록번호 뒤 7자리
		Integer loginMethod = 0;
		int iOTP = 0;
		AuthCode radiusAuthCode = AuthCode.PasswordFail;
		
		//동부화재는 사죵자를 장비마다 분산시켜 놓았다. ldap 인증 시에 다른 존의 사용자가 접속하지 못하도록 장비에 사용자 정보가 없으면 리턴하도록 한다.
		if(user == null)
			return AuthCode.UserNotFound;
		

		AccessGateway gw = gatewayApi.getCurrentAccessGateway();
		if (gw != null)
			loginMethod = getLoginMethod(gw, loginName, user);
		
		Collection<AccessProfile> profiles = profileApi.getAccessProfiles();
		for (AccessProfile p : profiles) {
			if (p != null) {
				if (p.getLoginMethod() != null && p.getLoginMethod() == LoginMethod_PW_OTP)
					iOTP = 1;
			}
		}
		
		// check external auth
		AuthCode externalCode = checkExternalAuth(props, user);
		if (externalCode != null)
			return externalCode;
		
		if (loginMethod != LoginMethod_PW_OTP) {
			// check radius
			radiusAuthCode = checkRadiusLogin(loginName, password, user);
			logger.trace("frodo core: radius login result for [{}] -> {}", loginName, radiusAuthCode);
			if (radiusAuthCode == AuthCode.RadiusSuccess)
				return radiusAuthCode;
		}
		else {
			// check otp
			if (otp != null && !otp.equals("password_change")) {	// Skip in case of password change
				radiusAuthCode = checkRadiusLogin(loginName, otp, user);
				logger.trace("frodo core: otp login result for [{}] -> {}", loginName, radiusAuthCode);
				if (radiusAuthCode != AuthCode.RadiusSuccess)
					return radiusAuthCode;
			}
		}

		// check ldap
		AuthCode ldapAuthCode = checkLdapLogin(loginName, password);
		logger.trace("frodo core: ldap login result for [{}] -> {}", loginName, ldapAuthCode);
		if (ldapAuthCode == AuthCode.LdapSuccess)
			return ldapAuthCode;

		AuthCode ldapAttributesCode = checkLdapAttributesLogin(loginName, password);
		logger.trace("frodo core: ldap attributes login result for [{}] -> {}", loginName, ldapAttributesCode);
		if (ldapAttributesCode == AuthCode.LdapSuccess)
			return ldapAttributesCode;		
		
		// check local database
		AuthCode localAuthCode = checkLocalLogin(loginName, password, user);
		logger.trace("frodo core: local login result for [{}] -> {}", loginName, localAuthCode);
		if (localAuthCode == AuthCode.LocalSuccess)
			return localAuthCode;

		// check sql auth
		AuthCode sqlCode = checkSqlAuth(props, user);
		if (sqlCode != null)
			return sqlCode;

		if (ldapAuthCode != AuthCode.LdapNoUse)
			return ldapAuthCode;

		if (ldapAttributesCode != AuthCode.LdapNoUse)
			return ldapAttributesCode;

		if (iOTP == 0 && radiusAuthCode != AuthCode.RadiusNoUse)
			return radiusAuthCode;
		
		// At first login of LoginMethod_PW_OTP users the password provided will be saved.
		if (loginMethod == LoginMethod_PW_OTP) {
			UserExtension ext = userApi.getUserExtension(user);
			if (ext == null)
				return AuthCode.RadiusSuccess;
		}
		
		return localAuthCode;
	}
	
	private AuthCode checkRadiusLogin(String loginName, String password, User user) {
		AccessGateway gw = gatewayApi.getCurrentAccessGateway();
		if (gw == null || !gw.isUseRadiusAuth())
			return AuthCode.RadiusNoUse;

		List<Radius> configs = XtmConfig.readConfig(Radius.class);
		Radius radius = null;

		for (Radius config : configs)
			if (config.getType().equals(Radius.Type.Radius))
				radius = config;

		if (radius == null || !radius.isRadiusUse())
			return AuthCode.RadiusNoUse;

		if (password == null || password.isEmpty())
			return AuthCode.RadiusReject;

		try {
			InetAddress addr = InetAddress.getByName(radius.getRadiusIp());
			if (addr.isLoopbackAddress() || addr.isLinkLocalAddress() || addr.isLinkLocalAddress())
				return AuthCode.RadiusReject;
			int port = radius.getAuthPort();
			RadiusClient client = new RadiusClient(addr, port, radius.getRadiusPassword());

			Authenticator auth = null;
			if (radius.getAuthMethod() != null && radius.getAuthMethod().equals(AuthMethod.CHAP))
				auth = new ChapAuthenticator(client, loginName, password);
			else
				auth = new PapAuthenticator(client, loginName, password);

			RadiusPacket response = client.authenticate(auth);
			if (response instanceof AccessAccept) {
				if (user == null) {
					logger.info("frodo core: create new user for radius login [{}]", loginName);
					AccessGateway gateway = gatewayApi.getCurrentAccessGateway();
					if (gateway == null) {
						logger.error("frodo core: access gateway not found");
						return null;
					}

					OrganizationUnit orgUnit = null;
					if (gateway.getRadiusOrgUnitId() != null)
						orgUnit = orgUnitApi.getOrganizationUnit("localhost", gateway.getRadiusOrgUnitId());

					// create new user
					user = new User();
					user.setOrgUnit(orgUnit);
					user.setLoginName(loginName);
					user.setName(loginName);
					user.setCreated(new Date());
					user.setUpdated(new Date());
					user.setSourceType("radius");

					Map<String, Object> m = new HashMap<String, Object>();
					m.put("ip", radius.getRadiusIp());
					m.put("port", radius.getAuthPort());
					m.put("auth_method", radius.getAuthMethod().toString());
					user.getExt().put("radius", m);

					domUserApi.createUser("localhost", user);
				}

				return AuthCode.RadiusSuccess;
			} else
				return AuthCode.RadiusReject;
		} catch (UnknownHostException e) {
			return AuthCode.RadiusTimeout;
		} catch (IOException e) {
			return AuthCode.RadiusTimeout;
		}
	}
	
	private AuthCode checkLdapLogin(String loginName, String password) {
		AccessGateway gw = gatewayApi.getCurrentAccessGateway();
		if (gw == null || gw.getLdapMode() != LdapMode.LDAP_SYNC)
			return AuthCode.LdapNoUse;

		LdapProfile profile = getLdapProfile();

		if (profile == null)
			return AuthCode.LdapNoUse;

		if (!ldap.verifyPassword(profile, loginName, password, 5000))
			return AuthCode.LdapReject;

		return AuthCode.LdapSuccess;
	}
	
	private AuthCode checkLdapAttributesLogin(String loginName, String password) {
		AccessGateway gw = gatewayApi.getCurrentAccessGateway();
		if (gw == null || (gw.getLdapMode() != LdapMode.LDAP_SYNC_EXTEND))
			return AuthCode.LdapNoUse;
		List<LdapProfile> profiles = new ArrayList<LdapProfile>();

		LdapProfile defaultProfile = getLdapProfile();
		if (defaultProfile != null)
			profiles.add(defaultProfile);
		profiles.addAll(ldap.getProfiles());

		for (LdapProfile profile : profiles) {
			if (profile.getServerType() != LdapServerType.ActiveDirectory)
				continue;

			if (ldap.verifyPassword(profile, loginName, password))
				return AuthCode.LdapSuccess;
		}

		return AuthCode.LdapReject;
	}
	
	private AuthCode checkExternalAuth(Map<String, Object> props, User user) {
		String loginName = (String) props.get("id");
		// check external auth
		if (externalAuth.isEnabled()) {
			Map<String, Object> m = externalAuth.login(props);
			AuthCode code = AuthCode.parse((Integer) m.get("auth_code"));

			// create user if login success & account not found
			if (code.getCode() == 0) {
				String userName = (String) m.get("name");
				String orgUnitName = (String) m.get("org_unit_name");

				createLocalUser(loginName, userName, orgUnitName, "external", m, user);
			}

			logger.trace("frodo core: external auth result for [{}] -> {}", loginName, code);
			return code;
		}

		return null;
	}
	
	private AuthCode checkSqlAuth(Map<String, Object> props, User user) {
		String loginName = (String) props.get("id");
		if (sqlAuth.isEnabled()) {
			//토요타,렉서스 : 사용자 검증 쿼리에 Boolean Flag가 있는 경우, Y -> N으로 변경되면 "암호가 틀리다"는 메시지 표시되는 문제 해결
			SqlAuthResult result = sqlAuth.verifyUser(loginName);
			if (result.isSuccess()) {
				logger.debug("frodo core: finding sql user - login name found [{}]", loginName);
			} else {
				if(user != null) {
					String sourceType = user.getSourceType();
					if(sourceType != null && sourceType.equals("sql")) {
						logger.debug("frodo core: finding sql user - login name not found [{}] - delete local user", loginName);
						domUserApi.removeUser("localhost", loginName);
					}
				}
				return null;
			}
			
			Map<String, Object> m = sqlAuth.login(props);

			AuthCode code = AuthCode.PasswordFail;
			
			Integer auth_code = (Integer) m.get("auth_code");
			if (auth_code == 2)
				code = AuthCode.parse((Integer) m.get("auth_code"));
			else if (auth_code == 0)
				code = AuthCode.SqlSuccess;

			// create user if login success & account not found
			if (code.getCode() == 0) {
				String userName = (String) m.get("name");
				String orgUnitName = (String) m.get("org_unit_name");
				handleUserInfo(loginName, userName, orgUnitName, "sql", user);
			}

			logger.trace("frodo core: sql auth result for [{}] -> {}", loginName, code);
			return code;
		}

		return null;
	}
	
	private AuthCode checkLocalLogin(String loginName, String pw, User user) {
		AuthCode authCode = AuthCode.LocalSuccess;
		if (user == null)
			return AuthCode.UserNotFound;

		if (!domUserApi.verifyPassword("localhost", loginName, pw))
			return AuthCode.PasswordFail;

		return authCode;
	}
	
	public User handleUserInfo(String loginName, String userName, String orgUnitName, String sourceType, User user) {
		if (user == null)
			user = createLocalUser(loginName, userName, orgUnitName, sourceType, null, user);

		return user;
	}
	
	public LdapUser findLdapUser(String loginName) {
		List<LdapProfile> profiles = new ArrayList<LdapProfile>();
		LdapProfile ldapProfile = getLdapProfile();
		if (ldapProfile != null)
			profiles.add(ldapProfile);

		profiles.addAll(ldap.getProfiles());
		for (LdapProfile profile : profiles) {
			if (profile.getServerType() != LdapServerType.ActiveDirectory)
				continue;

			try {
				LdapUser ldapUser = ldap.findUser(profile, loginName);
				if (ldapUser == null)
					continue;

				return ldapUser;
			} catch (IllegalStateException e) {
				logger.error("frodo core: cannot fetch ldap user, ldap profile [{}]. try next ldap profile", profile);
			}
		}
		return null;
	}
	
	public AuthCode checkCertLogin(Integer loginMethod, BigInteger serial, String crl, String loginName, String subjectCN) {

		logger.trace("frodo core: subject_cn [{}], login_name [{}]", subjectCN, loginName);

		if (loginMethod == LoginMethod_PRIVCERT || loginMethod == LoginMethod_PW_PRIVCERT) {
			if (subjectCN == null || !subjectCN.equals(loginName)) {
				logger.error("frodo core: certificate mismatch, subject [{}], login_name [{}]", subjectCN, loginName);
				return AuthCode.CertVerifyFail;
			}
		}

		if (crl == null || crl.length() < 5)
			return AuthCode.CertSuccess;

		try {
			if (CrlValidator.isRevoked(crl, serial))
				return AuthCode.CertRevoked;

		} catch (CrlConnectionErrorException e) {
			logger.error("frodo core: crl connection [" + crl + "], serial [" + serial + "] error", e);
			return AuthCode.CrlConnectFail;
		}

		return AuthCode.CertSuccess;
	}
	
	public boolean isLoginMethodWithNPKI(AccessGateway gw, User user, String loginName) {
		Integer loginMethod = getLoginMethod(gw, loginName, user);
		return loginMethod == LoginMethod_NPKI || loginMethod == LoginMethod_PW_NPKI;
	}
	
	public AuthCode checkNpki(AccessGateway gw, User user, byte[] vid, String subjectDn, AuthCode pwAuthCode) {
		String loginName = user.getLoginName();

		// 기존에 장비에 저장되어 있던 VID 값
		String vidp = null;

		// NPKI but VID not found
		UserExtension ext = userApi.getUserExtension(user);
		if (ext == null && gw.getIdentificationMode() == IdentificationMode.Idn) {
			logger.debug("frodo core: npki vid check for [{}] -> user ext not found", loginName);
			return AuthCode.NpkiNotFound;
		}

		if (ext != null)
			vidp = ext.getVid();

		if (vidp == null) {
			if (gw.getIdentificationMode() == IdentificationMode.Idn) {
				logger.debug("frodo core: npki vid check for [{}] -> vid not registered", loginName);
				return AuthCode.NpkiNotFound;
			} else {
				// 기존 VID가 없으면 일단 이 부분에서는 판정하지 말고 리턴하고 하단에서 처리
				return AuthCode.NpkiSuccess;
			}
		}

		String vidHex = toHex(vid);
		logger.debug("frodo core: checking npki vid [{}] with vid' [{}]", vidHex, vidp);

		if (vidHex.equalsIgnoreCase(vidp)) {
			int loginMethod = getLoginMethod(gw, loginName, user);
			if (loginMethod == LoginMethod_NPKI)
				return AuthCode.NpkiSuccess;
			else
				return pwAuthCode;
		} else if (gw.getIdentificationMode() == IdentificationMode.Idn) {
			return AuthCode.NpkiNotFound;
		} else {
			return AuthCode.NpkiFail;
		}
	}
	
	public boolean checkVidIntegrity(byte[] vid, String idn, byte[] r, String hashOid) {
		String hashType = getHashName(hashOid);
		
		logger.debug("frodo core: hashType [{}]", hashType);

		try {
			DERSequence derSequence = new DERSequence(new ASN1Encodable[] {new DERPrintableString(idn.getBytes()), new DERBitString(r)});
			
			byte[] content = derSequence.getDEREncoded();
			
			MessageDigest md2 = MessageDigest.getInstance(hashType);
			md2.update(content);
			byte[] h2 = md2.digest();
			md2.reset();
			md2.update(h2);
			byte[] vidp2 = md2.digest();

			logger.debug("frodo core: idn [{}]", idn);
			logger.debug("frodo core: compare vid [{}] with vid' [{}]", toHex(vid), toHex(vidp2));
		
			return Arrays.equals(vid, vidp2);
		} catch (NoSuchAlgorithmException e) {
			logger.error("frodo core: cannot support hash algorithm [{}]", hashOid);
			return false;
		}
	}
	
	private String getHashName(String oid) {
		if (oid.equals("1.3.14.3.2.26"))
			return "SHA1";
		else if (oid.equals("1.2.840.113549.2.5"))
			return "MD5";
		else if (oid.equals("2.16.840.1.101.3.4.2.1"))
			return "SHA-256";
		else if (oid.equals("2.16.840.1.101.3.4.2.2"))
			return "SHA-384";
		else if (oid.equals("2.16.840.1.101.3.4.2.3"))
			return "SHA-512";
		else {
			Logger logger = LoggerFactory.getLogger(AuthServiceServlet.class.getName());
			logger.warn("frodo core: unsupported hash type for oid [{}]", oid);
			return null;
		}
	}
	
	public void registerUnauthorizedDevice(User user, AccessProfile profile, String deviceKey, InetAddress remoteIp,
			String macAddress, String hddSerial, String remoteClientIp, OsType osType) {

		if (osType != OsType.Android
				&& (macAddress == null || macAddress.isEmpty() || hddSerial == null || hddSerial.isEmpty()
						|| remoteClientIp == null || remoteClientIp.isEmpty())) {
			logger.trace(
					"frodo core: cannot register unauthorized device, mac_address [{}], hdd_serial [{}], remote_client_ip [{}]",
					new Object[] { macAddress, hddSerial, remoteClientIp });
			return;
		}

		AccessGateway gw = gatewayApi.getCurrentAccessGateway();
		DeviceAuthMode deviceAuthMode = gw.getDeviceAuthMode();
		if (profile != null && profile.getDeviceAuthMode() != null)
			deviceAuthMode = profile.getDeviceAuthMode();

		Map<String, Object> field = new HashMap<String, Object>();
		field.put("device_key", deviceKey);
		field.put("is_authorized", false);

		if (deviceAuthMode == DeviceAuthMode.OneToOne)
			field.put("login_name", user.getLoginName());

		if (authDeviceApi.getDevice(Predicates.field(field)) != null) {
			logger.trace("frodo core: duplicate device key, cannot register unauthorized device, login_name [{}]",
					user.getLoginName());
			return;
		}

		AuthorizedDevice authDevice = new AuthorizedDevice();
		authDevice.setType(osType.getCode());
		authDevice.setDeviceKey(deviceKey);
		authDevice.setHostName(null);
		authDevice.setDescription(null);
		authDevice.setOwner(user.getName());
		authDevice.setMacAddress(macAddress);
		authDevice.setHddSerial(hddSerial);
		authDevice.setRemoteClientip(remoteClientIp);
		authDevice.setRemoteIp(remoteIp.getHostAddress());
		authDevice.setBlocked(false);
		authDevice.setExpiration(null);
		authDevice.setLoginName(user.getLoginName());
		authDevice.setIsAuthorized(false);
		if(user.getOrgUnit() != null)
			authDevice.setOrgUnitName(user.getOrgUnit().getName());

		authDeviceApi.registerDevice(authDevice);
	}
}
