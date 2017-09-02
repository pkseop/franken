/*
 * Copyright 2011 Future Systems, Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package kr.co.future.dom.api.impl;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Random;
import java.util.Set;

import kr.co.future.dom.api.AdminApi;
import kr.co.future.dom.api.ConfigManager;
import kr.co.future.dom.api.DOMException;
import kr.co.future.dom.api.DefaultEntityEventListener;
import kr.co.future.dom.api.EntityEventListener;
import kr.co.future.dom.api.LoginCallback;
import kr.co.future.dom.api.MySQLConnectionService;
import kr.co.future.dom.api.OrganizationApi;
import kr.co.future.dom.api.OtpApi;
import kr.co.future.dom.api.ProgramApi;
import kr.co.future.dom.api.Transaction;
import kr.co.future.dom.api.UserApi;
import kr.co.future.dom.model.Admin;
import kr.co.future.dom.model.Permission;
import kr.co.future.dom.model.ProgramProfile;
import kr.co.future.dom.model.User;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Invalidate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.apache.felix.ipojo.annotations.Validate;

import kr.co.future.api.PrimitiveConverter;
import kr.co.future.confdb.ConfigTransaction;
import kr.co.future.confdb.Predicate;
import kr.co.future.confdb.Predicates;
import kr.co.future.msgbus.PushApi;
import kr.co.future.msgbus.Session;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(name = "dom-admin-api")
@Provides
public class AdminApiImpl implements AdminApi {
	private static final Class<User> cls = User.class;
	private static final String NOT_FOUND = "admin-not-found";
	private static final String LOCKED_ADMIN = "locked-admin";
	private final Logger logger = LoggerFactory.getLogger(AdminApiImpl.class.getName());

//	private EntityEventListener<User> userEventListener = new DefaultEntityEventListener<User>() {
//		@Override
//		public void entityRemoving(String domain, User obj, ConfigTransaction xact, Object state) {
//			Admin admin = getAdmin(domain, obj);
//			if (admin != null && admin.getRole().getName().equals("master"))
//				throw new DOMException("cannot-master-unset");
//		}
//	};
//	private EntityEventListener<ProgramProfile> programProfileEventListener = new DefaultEntityEventListener<ProgramProfile>() {
//		@Override
//		public void entityRemoving(String domain, ProgramProfile obj, ConfigTransaction xact, Object state) {
//			List<User> users = new ArrayList<User>();
//			List<Predicate> preds = new ArrayList<Predicate>();
//			for (Admin admin : getAdmins(domain)) {
//				if (admin.getProfile().getName().equals(obj.getName())) {
//					admin.setProfile(null);
//					users.add(admin.getUser());
//					preds.add(Predicates.field("loginName", admin.getUser().getLoginName()));
//				}
//			}
//
//			Transaction x = Transaction.getInstance(xact);
//			cfg.updates(x, User.class, preds, users, null);
//		}
//	};

//	@Requires
//	private ConfigManager cfg;

	@Requires
	private OrganizationApi orgApi;

	@Requires
	private ProgramApi programApi;

	@Requires
	private UserApi userApi;

	@Requires
	private PushApi pushApi;

	@Requires(optional = true, nullable = false)
	private OtpApi otpApi;
	
	@Requires
    private MySQLConnectionService connectionService;

	private Set<LoginCallback> callbacks = new HashSet<LoginCallback>();
	private PriorityQueue<LoggedInAdmin> loggedIn = new PriorityQueue<LoggedInAdmin>(11, new LoggedInAdminComparator());

	@Validate
	public void validate() {
//		userApi.addEntityEventListener(userEventListener);
//		programApi.getProgramProfileEventProvider().addEntityEventListener(programProfileEventListener);
		
		//retrieve master's login name.
		User user = userApi.getMasterUser();
		if(user != null) {
			ADMIN_LOGIN_NAME = user.getLoginName();
		}
	}

	@Invalidate
	public void invalidate() {
//		if (userApi != null)
//			userApi.removeEntityEventListener(userEventListener);
//		if (programApi != null)
//			programApi.getProgramProfileEventProvider().removeEntityEventListener(programProfileEventListener);
	}

	@Override
	public String getExtensionName() {
		return "admin";
	}

//	private Predicate getPred() {
//		return Predicates.not(Predicates.field("ext/admin", null));
//	}
//
//	private Predicate getPred(String loginName) {
//		return Predicates.and(getPred(), Predicates.field("loginName", loginName));
//	}

	@Override
	public Collection<Admin> getAdmins(String domain) {
		String query = "SELECT loginName FROM AdminExtension";

        Connection con = null;
        PreparedStatement psmt = null;
        ResultSet rs = null;
        
        Collection<User> adminUsers = new ArrayList<User>();
        try {
            con = connectionService.getConnection();

            if (con == null) {
                logger.error("Can't connect to database. check database status.");
                throw new Exception("database connect fail");
            }

            psmt = con.prepareStatement(query);
            rs = psmt.executeQuery();

            while (rs.next()) {
            	String loginName = rs.getString("loginName");
            	adminUsers.add(userApi.findUser(domain, loginName));
            }
        } catch (Exception e) {
            logger.error("getUsers: ", e);
        } finally {
        	if(rs != null){
                try {
                    rs.close();
                } catch (SQLException e) {
                    logger.error("error occurred during closing ResultSet", e);
                }
            }
            if (psmt != null) {
                try {
                    psmt.close();
                } catch (SQLException e) {
                	logger.error("error occurred during closing PreparedStatement", e);
                }
            }
            if (con != null) {
                try {
                    con.close();
                } catch (SQLException e) {
                	logger.error("error occurred during closing Connection", e);
                }
            }
        }

        Collection<Admin> admins = new ArrayList<Admin>();
		for (User user : adminUsers)
			admins.add(parseAdmin(domain, user));
		return admins;
	}

	@Override
	public Admin findAdmin(String domain, String loginName) {
		//return parseAdmin(domain, cfg.find(domain, cls, getPred(loginName)));
        return parseAdmin(domain, userApi.findUser(domain, loginName));
	}

	@Override
	public Admin getAdmin(String domain, String loginName) {
		Admin admin = findAdmin(domain, loginName);
		if (admin == null)
			throw new DOMException(NOT_FOUND);
		return admin;
	}

	@Override
	public Admin getAdmin(String domain, User user) {
		Object o = user.getExt().get(getExtensionName());
		if (o == null)
			return null;

		if (o instanceof Admin)
			return (Admin) o;
		else if (o instanceof Map)
			return parseAdmin(domain, user);

		return null;
	}

	private Admin parseAdmin(String domain, User user) {
		if (user == null)
			return null;

        if (user.getExt().get(getExtensionName()) == null) {
            logger.info("parseAdmin: admin extension is null");
            return null;
        }
        
        Admin admin = PrimitiveConverter.parse(Admin.class, user.getExt().get(getExtensionName()));

		admin.setUser(user);
		return admin;
	}

	@Override
	public void setAdmin(String domain, String requestAdminLoginName, String targetUserLoginName, Admin admin) {
		checkPermissionLevel(domain, requestAdminLoginName, admin, "set-admin-permission-denied");

		prepare(admin);

		User target = userApi.getUser(domain, targetUserLoginName);
		if (target.getExt().get(getExtensionName()) != null) {
			Admin oldAdmin = parseAdmin(domain, target);
			if (oldAdmin != null && !oldAdmin.isEnabled() && admin.isEnabled())
				admin.setLoginFailures(0);
		}

		target.getExt().put(getExtensionName(), admin);
		userApi.updateUser(domain, target, false);
	}

	@Override
	public String updateOtpSeed(String domain, String requestAdminLoginName, String targetUserLoginName) {
		Admin admin = getAdmin(domain, targetUserLoginName);
		String newSeed = createOtpSeed();
		admin.setOtpSeed(newSeed);
		setAdmin(domain, requestAdminLoginName, targetUserLoginName, admin);
		return newSeed;
	}

	private void prepare(Admin admin) {
		if (admin.getLang() == null)
			admin.setLang("en");
		if (admin.getOtpSeed() == null)
			admin.setOtpSeed(createOtpSeed());
	}

	private String createOtpSeed() {
		Random random = new Random();
		StringBuilder sb = new StringBuilder();
		char[] c = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789".toCharArray();
		for (int i = 0; i < 10; i++)
			sb.append(c[random.nextInt(c.length)]);
		return sb.toString();
	}

	@Override
	public void unsetAdmin(String domain, String requestAdminLoginName, String targetUserLoginName) {
		// login name is immutable, only one master account per domain, and
		// default name is "admin"
		if (targetUserLoginName.equals("admin"))
			throw new DOMException("cannot-master-unset");

		if (targetUserLoginName.equals(requestAdminLoginName))
			throw new DOMException("cannot-remove-requesting-admin");

		Admin target = getAdmin(domain, targetUserLoginName);
		if (requestAdminLoginName != null)
			checkPermissionLevel(domain, requestAdminLoginName, target, "unset-admin-permission-denied");

		userApi.deleteAdminExtension(targetUserLoginName);
	}

	private void checkPermissionLevel(String domain, String requestAdminLoginName, Admin admin, String exceptionMessage) {
		Admin request = findAdmin(domain, requestAdminLoginName);
		if (request == null)
			throw new DOMException("request-admin-not-found");

		if (request.getRole().getLevel() < admin.getRole().getLevel())
			throw new DOMException(exceptionMessage);
	}

	@Override
	public Admin login(Session session, String loginName, String hash, boolean force) {
		String domain = session.getOrgDomain();
		Admin admin = getAdmin(domain, loginName);

		try {
			checkAcl(session, admin);

			if (!admin.isEnabled()) {
				int lockTime = 300;
				Object param = orgApi.getOrganizationParameter(domain, "login_lock_time");
				if (param != null) {
					try {
						lockTime = (Integer) param;
					} catch (NumberFormatException e) {
					}
				}
				Calendar c = Calendar.getInstance();
				c.add(Calendar.SECOND, -lockTime);

				Date failed = admin.getLastLoginFailedDateTime();
				if (failed == null || failed.after(c.getTime()))
					throw new DOMException(LOCKED_ADMIN);
			}

			String password = null;
			if (otpApi != null && admin.isUseOtp())
				password = Sha256.hash(otpApi.getOtpValue(admin.getOtpSeed()));
			else
				password = admin.getUser().getPassword();

			if (password == null)
				throw new DOMException("invalid-password");

			if (!hash.equals(Sha256.hash(password + session.getString("nonce")))) {
				if (admin.isUseOtp())
					throw new DOMException("invalid-otp-password");
				else
					throw new DOMException("invalid-password");
			}

			enforcePasswordChange(domain, admin);
			
			String disableAdminDupCheck = orgApi.getOrganizationParameter(domain, "disable_admin_dup_check", String.class);
			if(disableAdminDupCheck == null || disableAdminDupCheck.equals("false")) {
				//이미 접속되어 있는 사용자는 중복 접속이 되지 않게 한다.
				for(LoggedInAdmin loggedInAdmin : loggedIn) {
					if(loggedInAdmin.loginName.equals(loginName)){
						throw new DOMException("duplicate-login");
					}
				}
			}

			Integer maxSession = orgApi.getOrganizationParameter(domain, "max_sessions", Integer.class);
			if (maxSession != null) {
				if (maxSession > 0) {
					if (force) {
						while (loggedIn.size() >= maxSession) {
							if (loggedIn.peek().level > admin.getRole().getLevel())
								throw new DOMException("max-session");

							LoggedInAdmin ban = loggedIn.poll();
							Map<String, Object> m = new HashMap<String, Object>();
							m.put("type", "terminate");
							m.put("kick_by", admin.getUser().getLoginName());
							pushApi.push(ban.session, "kraken-system-event", m);

							long wait = System.currentTimeMillis() + 5000;
							while (loggedIn.contains(ban) && System.currentTimeMillis() < wait) {
								try {
									Thread.sleep(100);
								} catch (InterruptedException e) {
								}
							}
							ban.session.close();
						}
					} else if (loggedIn.size() >= maxSession) {
						LoggedInAdmin peek = loggedIn.peek();
						if (peek.level > admin.getRole().getLevel())
							throw new DOMException("max-session");

						Map<String, Object> m = new HashMap<String, Object>();
						m.put("login_name", peek.loginName);
						m.put("session_id", peek.session.getGuid());
						m.put("ip", peek.session.getRemoteAddress().getHostAddress());
						throw new DOMException("max-session", m);
					}
				}
			}

			updateLoginFailures(domain, admin, true);
			loggedIn.add(new LoggedInAdmin(admin.getRole().getLevel(), new Date(), session, admin.getUser().getLoginName()));

			for (LoginCallback callback : callbacks)
				callback.onLoginSuccess(admin, session);

			return admin;
		} catch (DOMException e) {
			if (!e.getErrorCode().equals("max-session")) {
				updateLoginFailures(domain, admin, false);
				for (LoginCallback callback : callbacks) {
					if (e.getErrorCode().equals(LOCKED_ADMIN))
						callback.onLoginLocked(admin, session);
					else
						callback.onLoginFailed(admin, session, e);
				}
			}
			throw e;
		}
	}

	private void enforcePasswordChange(String domain, Admin admin) {
		logger.trace("kraken dom: last password change [{}]", admin.getUser().getLastPasswordChange());
		long interval = 0;
		if (admin.getUser().getLastPasswordChange() != null)
			interval = new Date().getTime() - admin.getUser().getLastPasswordChange().getTime();

		Integer passwordExpiry = (Integer) orgApi.getOrganizationParameter(domain, "dom.user.password_expiry");
		if (passwordExpiry == null)
			passwordExpiry = 365;

		long baseline = passwordExpiry * 86400 * 1000L;
		logger.trace("kraken dom: checking expired password interval [{}], last change [{}]", interval, baseline);

		if (interval > baseline)
			throw new DOMException("expired-password");
	}
	
	// Peter H. Nahm - 2013/08/05 
	public static long ipToLong(String ipAddress) {
		long result = 0;
		String[] atoms = ipAddress.split("\\.");
		
		for (int i = 3; i >= 0; i--) {
			result |= (Long.parseLong(atoms[3 - i]) << (i * 8));
		}
		
		return result & 0xFFFFFFFF;
	} 

	private void checkAcl(Session session, Admin admin) {
		String remote = session.getRemoteAddress().getHostAddress();
		boolean failed = false;
		
		// Peter H. Nahm - 2013/08/05
		long longRemote = ipToLong(remote);
		User user = admin.getUser();
		
		Map<String, Object> ext = user.getExt();
		if (ext != null) {
			if (ext.containsKey("frodo")) {
				Map<String, Object> frodo = (Map<String, Object>) ext.get("frodo");
				
				if(frodo != null) {
                    if (frodo.get("allow_ip_ranges") != null) {
                        Object[] array = null;

                        try {
                            array = (Object[]) frodo.get("allow_ip_ranges");
                        } catch (ClassCastException ex) {
                            array = null;
                        }

                        if (array != null) {
                            for (int i = 0; i < array.length; i++) {
                                Map<String, String> range = (Map<String, String>) array[i];
                                long longFromIP = ipToLong(range.get("ip_from"));
                                long longToIP = ipToLong(range.get("ip_to"));
                                if (longRemote >= longFromIP && longRemote <= longToIP) {
                                    return;
                                }
                            }
                        }
                    }
				}
			}
		} 

		String hosts = orgApi.getOrganizationParameter(session.getOrgDomain(), "dom.admin.trust_hosts", String.class);
		if (hosts != null && !hosts.isEmpty()) {
			String[] tokens = hosts.split(",");
			for (String token : tokens) {
				token = token.trim();
				if (token.equals(remote))
					return;
			}

			// check admin specific config (give change)
			failed = true;
		}

		// check per admin
		if (!admin.isUseAcl()) {
			if (failed)
				throw new DOMException("not-trust-host");
			return;
		}

		for (String host : admin.getTrustHosts()) {
			if (host != null && host.equals(remote))
				return;
		}
		throw new DOMException("not-trust-host");
	}

	private void updateLoginFailures(String domain, Admin admin, boolean success) {
		if (success) {
			admin.setLastLoginDateTime(new Date());
			admin.setLastLoginFailedDateTime(null);
			admin.setLoginFailures(0);
			admin.setEnabled(true);
		} else {
			if (admin.isEnabled())
				admin.setLastLoginFailedDateTime(new Date());
			admin.setLoginFailures(admin.getLoginFailures() + 1);
			if (admin.isUseLoginLock() && admin.getLoginFailures() >= admin.getLoginLockCount())
				admin.setEnabled(false);
		}
		String loginName = admin.getUser().getLoginName();
		setAdmin(domain, loginName, loginName, admin);
	}

	@Override
	public void logout(Session session) {
		if (session.getOrgDomain() != null && session.getAdminLoginName() != null) {
			logger.trace("kraken dom: logout [domain: {}, login: {}]", session.getOrgDomain(), session.getAdminLoginName());
			Admin admin = getAdmin(session.getOrgDomain(), session.getAdminLoginName());
			loggedIn.remove(new LoggedInAdmin(session));
			for (LoginCallback callback : callbacks)
				callback.onLogout(admin, session);
		}
	}

	private class LoggedInAdmin {
		private int level;
		private Date loginTime;
		private Session session;
		private String loginName;

		private LoggedInAdmin(Session session) {
			this.session = session;
		}

		private LoggedInAdmin(int level, Date loginTime, Session session, String loginName) {
			this.level = level;
			this.loginTime = loginTime;
			this.session = session;
			this.loginName = loginName;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + getOuterType().hashCode();
			result = prime * result + ((session == null) ? 0 : session.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			LoggedInAdmin other = (LoggedInAdmin) obj;
			if (!getOuterType().equals(other.getOuterType()))
				return false;
			if (session == null) {
				if (other.session != null)
					return false;
			} else if (!session.equals(other.session))
				return false;
			return true;
		}

		private AdminApiImpl getOuterType() {
			return AdminApiImpl.this;
		}
	}

	private class LoggedInAdminComparator implements Comparator<LoggedInAdmin> {
		@Override
		public int compare(LoggedInAdmin o1, LoggedInAdmin o2) {
			if (o1.level != o2.level)
				return o1.level - o2.level;
			else
				return o1.loginTime.compareTo(o2.loginTime);
		}
	}

	@Override
	public void registerLoginCallback(LoginCallback callback) {
		callbacks.add(callback);
	}

	@Override
	public void unregisterLoginCallback(LoginCallback callback) {
		callbacks.remove(callback);
	}

	@Override
	public boolean canManage(String domain, Admin admin, User user) {
		String loginName = user.getLoginName();
		if (!admin.getRole().getPermissions().contains(new Permission("dom", "user_edit")))
			return false;

		Object ext = user.getExt().get("admin");
		if (ext == null)
			return true;
		
		Admin targetAdmin = PrimitiveConverter.parse(Admin.class, ext);
		if (targetAdmin != null && !loginName.equals(admin.getUser().getLoginName())
				&& targetAdmin.getRole().getLevel() >= admin.getRole().getLevel()) {
			return false;
		}
		return true;
	}
	
	public static final String DEFAULT_LOGIN_NAME = "admin";
	public static final String DEFAULT_PASSWORD = Sha256.hash("future_01");
	private static String ADMIN_LOGIN_NAME = DEFAULT_LOGIN_NAME;
	
	@Override
	public boolean isFirstLogin(Session session, String loginName) {
		if(!loginName.equals(DEFAULT_LOGIN_NAME))
			return false;
		
		String domain = session.getOrgDomain();
		Admin admin = getAdmin(domain, loginName);
		
		//To use changed admin login name later.
		ADMIN_LOGIN_NAME = admin.getUser().getLoginName();
		if(!ADMIN_LOGIN_NAME.equals(DEFAULT_LOGIN_NAME))
			return false;
		
		String password = null;
		if (otpApi != null && admin.isUseOtp())
			password = Sha256.hash(otpApi.getOtpValue(admin.getOtpSeed()));
		else
			password = admin.getUser().getPassword();
		
		if(password.equals(DEFAULT_PASSWORD))
			return true;
		
		return false;
	}
	
	@Override
	public void modifyAdminLoginInfo(Session session, String loginName, String password, String trustHosts) {
		String domain = session.getOrgDomain();
		Admin admin = getAdmin(domain, DEFAULT_LOGIN_NAME);
		User target = userApi.getUser(domain, DEFAULT_LOGIN_NAME);
		if (target.getExt().get(getExtensionName()) != null) {
			Admin oldAdmin = parseAdmin(domain, target);
			if (oldAdmin != null && !oldAdmin.isEnabled() && admin.isEnabled())
				admin.setLoginFailures(0);
		}
		target.setLoginName(loginName);
		target.setPassword(password);
		//update admin info.
		userApi.updateAdminUser(domain, target, DEFAULT_LOGIN_NAME);
		//set trust hosts.
		orgApi.setOrganizationParameter(domain, "dom.admin.trust_hosts", trustHosts);
		//To use changed admin login name later.
		ADMIN_LOGIN_NAME = loginName;
	}
	
}
