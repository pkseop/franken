/*
 * Copyright 2011 Future Systems
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
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import kr.co.future.api.PrimitiveConverter;
import kr.co.future.api.PrimitiveParseCallback;
import kr.co.future.confdb.Config;
import kr.co.future.confdb.ConfigDatabase;
import kr.co.future.confdb.ConfigService;
import kr.co.future.confdb.Predicate;
import kr.co.future.confdb.Predicates;
import kr.co.future.dom.api.ConfigUpdateRequest;
import kr.co.future.dom.api.DOMException;
import kr.co.future.dom.api.DefaultEntityEventListener;
import kr.co.future.dom.api.DefaultEntityEventProvider;
import kr.co.future.dom.api.EntityEventListener;
import kr.co.future.dom.api.MySQLConnectionService;
import kr.co.future.dom.api.OrganizationApi;
import kr.co.future.dom.api.OrganizationUnitApi;
import kr.co.future.dom.api.UserApi;
import kr.co.future.dom.api.UserExtensionProvider;
import kr.co.future.dom.log.ReportUserLog;
import kr.co.future.dom.model.Admin;
import kr.co.future.dom.model.OrganizationUnit;
import kr.co.future.dom.model.Permission;
import kr.co.future.dom.model.Program;
import kr.co.future.dom.model.User;
import kr.co.future.sslvpn.model.AccessProfile;
import kr.co.future.sslvpn.model.ClientIpRange;
import kr.co.future.sslvpn.model.UserExtension;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Invalidate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.apache.felix.ipojo.annotations.Validate;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(name = "dom-user-api")
@Provides
public class UserApiImpl extends DefaultEntityEventProvider<User> implements UserApi {
	private static final char[] SALT_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789".toCharArray();
	private final Logger logger = LoggerFactory.getLogger(UserApiImpl.class.getName());
//	private static final Class<User> cls = User.class;
//	private static final String NOT_FOUND = "user-not-found";
	private static final String ALREADY_EXIST = "user-already-exist";
	private static final int DEFAULT_SALT_LENGTH = 10;

	private UserExtensionProviderTracker tracker;
	private ConcurrentMap<String, UserExtensionProvider> userExtensionProviders = new ConcurrentHashMap<String, UserExtensionProvider>();
	
	private EntityEventListener<OrganizationUnit> orgUnitEventListener = new DefaultEntityEventListener<OrganizationUnit>() {
		@Override
		public void entityRemoved(String domain, OrganizationUnit orgUnit, Object state) {
			String query = "SELECT loginName FROM User WHERE guid = ?";
			
			Connection con = null;
            PreparedStatement psmt = null;
            ResultSet rs = null;

			try {
                con = connectionService.getConnection();

                if (con == null) {
                    logger.error("Can't connect to database. check database status.");
                    throw new Exception("database connect fail");
                }

                psmt = con.prepareStatement(query);
                psmt.setString(1, orgUnit.getGuid());
                rs = psmt.executeQuery();
                while(rs.next()) {
                	removeUser(domain, rs.getString("loginName"));
                }
            } catch (SQLException e) {
                logger.error("OrganizationUnit info delete error", e);
            } catch (Exception e) {
                logger.error("OrganizationUnit info delete error", e);
            } finally {
                close(rs, psmt, con);
            }
		}
	};

	@Requires
	private ConfigService conf;

	@Requires
	private OrganizationApi orgApi;

	@Requires
	private OrganizationUnitApi orgUnitApi;
	
	@Requires
	private ReportUserLog reportUserLog;

    @Requires
    private MySQLConnectionService connectionService;
    
    public UserApiImpl(BundleContext bc) {
		this.tracker = new UserExtensionProviderTracker(bc);
	}

	@Validate
	public void validate() {
		orgUnitApi.addEntityEventListener(orgUnitEventListener);
		
		tracker.open();
	}

	@Invalidate
	public void invalidate() {
		if (orgUnitApi != null)
			orgUnitApi.removeEntityEventListener(orgUnitEventListener);
		tracker.close();
	}

	@Override
	public int countUsers(String domain, String orgUnitGuid, boolean includeChildren, String filter, boolean isAdmin) {
		if (orgUnitGuid == null && includeChildren) {
            return countUser(null, filter, isAdmin);
        }

		int total = 0;

		if (includeChildren) {
			Collection<String> guids = retrieveChildrenGuid(domain, orgUnitGuid);
			String orgGuidCond = genSQLInCond(guids);
			total = countUser(orgGuidCond, filter, isAdmin);
		} else {
			String orgGuidCond = "('" + orgUnitGuid + "')";
			total = countUser(orgGuidCond, filter, isAdmin);
		}

		return total;
	}

    private int countUser(String guids, String filter, Boolean isAdmin) {
        int count = 0;

        String query = "";

        if (filter != null && filter.length() > 0) {
            if (isAdmin) {
                if (guids == null) {
                    query = "SELECT COUNT(*) FROM User A, AdminExtension B WHERE A.loginName = B.loginName AND (A.loginName LIKE FILTER OR A.name LIKE FILTER)";
                } else {
                    query = "SELECT COUNT(*) FROM User A, AdminExtension B WHERE A.loginName = B.loginName AND A.guid IN " + guids 
                    		+ " AND (A.loginName LIKE FILTER OR A.name LIKE FILTER)";
                }

                String filterQuery = "'" + filter + "%'";

                query = query.replace("FILTER", filterQuery);
            } else {
                if (guids == null) {
                    query = "SELECT COUNT(*) FROM User WHERE loginName LIKE FILTER OR name LIKE FILTER";
                } else {
                    query = "SELECT COUNT(*) FROM User WHERE guid IN " + guids + " AND (loginName LIKE FILTER OR name LIKE FILTER)";
                }

                String filterQuery = "'" + filter + "%'";

                query = query.replace("FILTER", filterQuery);
            }
        } else {
            if (isAdmin) {
                if (guids == null) {
                    query = "SELECT COUNT(*) FROM User A, AdminExtension B WHERE A.loginName = B.loginName";
                } else {
                    query = "SELECT COUNT(*) FROM User A, AdminExtension B WHERE A.loginName = B.loginName AND A.guid IN " + guids;
                }
            } else {
                if (guids == null) {
                    query = "SELECT COUNT(*) FROM User";
                } else {
                    query = "SELECT COUNT(*) FROM User WHERE guid IN " + guids;
                }
            }
        }

        Connection con = null;
        PreparedStatement psmt = null;
        ResultSet rs = null;

        try {
            con = connectionService.getConnection();

            if (con == null) {
                logger.error("Can't connect to database. check database status.");
                throw new Exception("database connect fail");
            }

            psmt = con.prepareStatement(query);

            rs = psmt.executeQuery();

            // 결과값 반환
            if (rs.next()) {
                count = rs.getInt(1);
            }
        } catch (Exception e) {
            logger.error("countUser: ", e);
        } finally {
            close(rs, psmt, con);
        }

        return count;
    }

	@Override
	public Collection<User> getUsers(String domain) {
        Collection<User> users = new ArrayList<User>();

        String query = "SELECT * from User a left join OrganizationUnit b on a.guid = b.guid" 
        		+ " left join AdminExtension c on a.loginName = c.loginName"
        		+ " left join UserExtension d on a.loginName = d.loginName" 
        		+ " ORDER BY LENGTH(a.loginName), a.loginName";

        Connection con = null;
        PreparedStatement psmt = null;
        ResultSet rs = null;

        try {
            con = connectionService.getConnection();

            if (con == null) {
                logger.error("Can't connect to database. check database status.");
                throw new Exception("database connect fail");
            }

            psmt = con.prepareStatement(query);
            rs = psmt.executeQuery();

            while (rs.next()) {
                users.add(retrieveUser(rs));
            }
        } catch (Exception e) {
            logger.error("getUsers: ", e);
        } finally {
            close(rs, psmt, con);
        }

		return users;
	}
	
	private User retrieveUser(ResultSet rs) throws SQLException {
		User user = new User();
        user.setLoginName(rs.getString("a.loginName"));

        if(rs.getString("a.guid") != null)
        	user.setOrgUnit(retrieveOrgUnit(rs));
        user.setName(rs.getString("a.name"));
        user.setDescription(rs.getString("a.description"));
        user.setPassword(rs.getString("a.password"));
        user.setSalt(rs.getString("a.salt"));
        user.setTitle(rs.getString("a.title"));
        user.setEmail(rs.getString("a.email"));
        user.setPhone(rs.getString("a.phone"));
        user.setLastPasswordChange(toDate(rs.getTimestamp("a.lastPasswordChange")));
        user.setSourceType(rs.getString("a.sourceType"));
        user.setCreated(toDate(rs.getTimestamp("a.created")));
        user.setUpdated(toDate(rs.getTimestamp("a.updated")));
        user.setExt(retriveExt(rs));
        
        return user;
	}
	
	private OrganizationUnit retrieveOrgUnit(ResultSet rs) throws SQLException {
		OrganizationUnit orgUnit = new OrganizationUnit();
        orgUnit.setGuid(rs.getString("b.guid"));
        orgUnit.setName(rs.getString("b.name"));
        orgUnit.setName_desc(rs.getString("b.description"));
        orgUnit.setParent(rs.getString("b.parent"));
        orgUnit.setSourceType(rs.getString("b.source_type"));
        orgUnit.setUpdated(toDate(rs.getTimestamp("b.updated")));
        orgUnit.setCreated(toDate(rs.getTimestamp("b.created")));
        return orgUnit;
	}
	
	private Map<String, Object> retrieveAdminExtension(ResultSet rs) throws SQLException {
		if(rs.getString("c.loginName") == null) {
			return null;
		}
		
		Map<String, Object> adminExtension = new HashMap<String, Object>();
        adminExtension.put("role", getRole(rs.getString("c.roleName")));
        adminExtension.put("profile", getProfileProgram(rs.getString("c.programProfileName")));
        adminExtension.put("lang", rs.getString("c.lang"));
        adminExtension.put("use_login_lock", rs.getBoolean("c.useLoginLock"));
        adminExtension.put("login_lock_count", rs.getInt("c.loginLockCount"));
        adminExtension.put("login_failures", rs.getInt("c.loginFailures"));

        if (rs.getTimestamp("c.lastLoginFailedDateTime") != null)
            adminExtension.put("last_login_failed_date_time", toDate(rs.getTimestamp("c.lastLoginFailedDateTime")));
        else
            adminExtension.put("last_login_failed_date_time", null);

        adminExtension.put("use_idle_timeout", rs.getBoolean("c.useIdleTimeout"));
        adminExtension.put("idle_timeout", rs.getInt("c.idleTimeout"));
        adminExtension.put("last_login_date_time", rs.getTimestamp("c.lastLoginDateTime") == null ? null : toDate(rs.getTimestamp("c.lastLoginDateTime")));
        adminExtension.put("is_enabled", rs.getBoolean("c.isEnabled"));
        adminExtension.put("use_otp", rs.getBoolean("c.useOtp"));
        adminExtension.put("otp_seed", rs.getString("c.otpSeed"));
        adminExtension.put("use_acl", rs.getBoolean("c.useAcl"));
        adminExtension.put("created", toDate(rs.getTimestamp("c.created")));

        return adminExtension;
	}
	
	private Map<String, Object> retrieveUserExtension(ResultSet rs) throws SQLException {
		String loginName = rs.getString("d.loginName");
		if(loginName == null)
			return null;
		
		Map<String, Object> frodoExtension = new HashMap<String, Object>();

        frodoExtension.put("cid", rs.getString("d.cid"));
        frodoExtension.put("static_ip4", rs.getString("d.staticIp4"));
        frodoExtension.put("allow_ip4_from", rs.getString("d.allowIp4From"));
        frodoExtension.put("allow_ip4_to", rs.getString("d.allowIp4To"));
        frodoExtension.put("is_locked", rs.getBoolean("d.isLocked"));
        frodoExtension.put("login_failures", rs.getInt("d.loginFailures"));
        frodoExtension.put("last_ip", rs.getString("d.lastIp"));
        frodoExtension.put("vid", rs.getString("d.vid"));
        frodoExtension.put("salt", rs.getString("d.salt"));
        frodoExtension.put("idn_hash", rs.getString("d.idnHash"));
        frodoExtension.put("subject_dn", rs.getString("d.subjectDn"));
        frodoExtension.put("auth_key", rs.getString("d.deviceAuthKey"));
        frodoExtension.put("device_key_count_setting", rs.getInt("d.deviceKeyCountSetting"));
        frodoExtension.put("expire_at", rs.getTimestamp("d.expireDateTime") == null ? null : toDate(rs.getTimestamp("d.expireDateTime")));
        frodoExtension.put("start_at", rs.getTimestamp("d.startDateTime") == null ? null : toDate(rs.getTimestamp("d.startDateTime")));
        frodoExtension.put("key_expire_at", rs.getTimestamp("d.keyExpireDateTime") == null ? null : toDate(rs.getTimestamp("d.keyExpireDateTime")));
        frodoExtension.put("last_login_at", rs.getTimestamp("d.lastLoginTime") == null ? null : toDate(rs.getTimestamp("d.lastLoginTime")));
        frodoExtension.put("last_logout_at", rs.getTimestamp("d.lastLogoutTime") == null ? null : toDate(rs.getTimestamp("d.lastLogoutTime")));
        frodoExtension.put("last_password_hash", rs.getString("d.lastPasswordHash"));
        frodoExtension.put("last_password_change", rs.getTimestamp("d.lastPasswordChange") == null ? null : toDate(rs.getTimestamp("d.lastPasswordChange")));
        frodoExtension.put("created_at", toDate(rs.getTimestamp("d.createDateTime")));
        frodoExtension.put("updated_at", toDate(rs.getTimestamp("d.updateDateTime")));

        String accessProfileGuid = rs.getString("d.accessProfileGuid");

        if (accessProfileGuid == null || accessProfileGuid.length() == 0) {
            frodoExtension.put("profile", null);
        } else {
        	ConfigDatabase db = conf.ensureDatabase("frodo");
    		Config c = db.findOne(AccessProfile.class, Predicates.field("guid", accessProfileGuid));
    		if (c != null) {
                frodoExtension.put("profile", c.getDocument(AccessProfile.class, newParseCallback()));
    		}
        }

        frodoExtension.put("force_password_change", rs.getBoolean("d.forcePasswordChange"));
        frodoExtension.put("last_password_fail_time", rs.getTimestamp("d.lastPasswordFailTime") == null ? null : toDate(rs.getTimestamp("d.lastPasswordFailTime")));
        frodoExtension.put("source_type", rs.getString("d.sourceType"));
        frodoExtension.put("cert_type", rs.getString("d.certType"));
        frodoExtension.put("is_auto_locked", rs.getBoolean("d.isAutoLocked"));
        frodoExtension.put("auto_lock_released_time", rs.getTimestamp("d.autoLockReleasedTime") == null ? null : toDate(rs.getTimestamp("d.autoLockReleasedTime")));
        frodoExtension.put("twoway_auth_status", rs.getInt("d.twowayAuthStatus"));
        frodoExtension.put("allow_ip_ranges", getUserClientIpRange(loginName));
        frodoExtension.put("allow_time_table_id", rs.getString("d.allowTimeTableId"));
        
        return frodoExtension;
	}
	
	
	
	private Map<String, Object> retriveExt(ResultSet rs) throws SQLException {
        Map<String, Object> userExtension = new HashMap<String, Object>();
        
		Map<String, Object> adminExt = retrieveAdminExtension(rs);
		if(adminExt != null) {
			userExtension.put("admin", adminExt);
		}
		Map<String, Object> userExt = retrieveUserExtension(rs);
		if(userExt != null) {
			userExtension.put("frodo", userExt);
		}
		return userExtension;
	}

	@Override
	public Collection<User> getUsers(String domain, int offset, int limit) {
        Collection<User> users = new ArrayList<User>();

        String query = "SELECT * from User a left join OrganizationUnit b on a.guid = b.guid" 
        		+ " left join AdminExtension c on a.loginName = c.loginName"
        		+ " left join UserExtension d on a.loginName = d.loginName" 
        		+ " LIMIT ? OFFSET ?";

        Connection con = null;
        PreparedStatement psmt = null;
        ResultSet rs = null;

        try {
            con = connectionService.getConnection();

            if (con == null) {
                logger.error("Can't connect to database. check database status.");
                throw new Exception("database connect fail");
            }

            psmt = con.prepareStatement(query);
            psmt.setInt(1, limit);
            psmt.setInt(2, offset);

            rs = psmt.executeQuery();

            while (rs.next()) {
                users.add(retrieveUser(rs));
            }
        } catch (Exception e) {
            logger.error("getUsers: ", e);
        } finally {
            close(rs, psmt, con);
        }

        return users;
	}

    public Collection<User> getUsers(String domain, int offset, int limit, String filter, Boolean isAdmin) {
        Collection<User> users = new ArrayList<User>();

        String query = "";

        if (isAdmin) {
            query = "SELECT * from User a left join OrganizationUnit b on a.guid = b.guid" 
            			+ " inner join AdminExtension c on a.loginName = c.loginName"
            			+ " left join UserExtension d on a.loginName = d.loginName" 
            			+ " WHERE a.loginName LIKE FILTER OR a.name LIKE FILTER ORDER BY LENGTH(a.loginName), a.loginName * 1 LIMIT ? OFFSET ?";
        } else {
            query = "SELECT * from User a left join OrganizationUnit b on a.guid = b.guid" 
            		+ " left join AdminExtension c on a.loginName = c.loginName"
            		+ " left join UserExtension d on a.loginName = d.loginName" 
            		+ " WHERE a.loginName LIKE FILTER OR a.name LIKE FILTER ORDER BY LENGTH(a.loginName), a.loginName LIMIT ? OFFSET ?";
        }

        String filterQuery = "'" + filter + "%'";

        query = query.replace("FILTER", filterQuery);

        Connection con = null;
        PreparedStatement psmt = null;
        ResultSet rs = null;

        try {
            con = connectionService.getConnection();

            if (con == null) {
                logger.error("Can't connect to database. check database status.");
                throw new Exception("database connect fail");
            }

            psmt = con.prepareStatement(query);
            psmt.setInt(1, limit);
            psmt.setInt(2, offset);

            rs = psmt.executeQuery();

            while (rs.next()) {
                users.add(retrieveUser(rs));
            }
        } catch (Exception e) {
            logger.error("getUsers: ", e);
        } finally {
            close(rs, psmt, con);
        }

        return users;
    }

    private Collection<User> getUsers(String domain, String guids, int offset, int limit, String filter, boolean isAdmin) {
        Collection<User> users = new ArrayList<User>();

        String query = "";

        if (isAdmin) {
            query = "SELECT * from User a left join OrganizationUnit b on a.guid = b.guid" 
        			+ " inner join AdminExtension c on a.loginName = c.loginName"
        			+ " left join UserExtension d on a.loginName = d.loginName" 
        			+ " WHERE (a.loginName LIKE FILTER OR a.name LIKE FILTER) AND a.guid IN " + guids
        			+ " ORDER BY LENGTH(a.loginName), a.loginName LIMIT ? OFFSET ?";
        } else {
            query = "SELECT * from User a left join OrganizationUnit b on a.guid = b.guid" 
            		+ " left join AdminExtension c on a.loginName = c.loginName"
            		+ " left join UserExtension d on a.loginName = d.loginName" 
            		+ " WHERE (a.loginName LIKE FILTER OR a.name LIKE FILTER) AND a.guid IN " + guids
            		+ " ORDER BY LENGTH(a.loginName), a.loginName LIMIT ? OFFSET ?";
        }
        
        String filterQuery = "'" + filter + "%'";

        query = query.replace("FILTER", filterQuery);
        
        Connection con = null;
        PreparedStatement psmt = null;
        ResultSet rs = null;

        try {
            con = connectionService.getConnection();

            if (con == null) {
                logger.error("Can't connect to database. check database status.");
                throw new Exception("database connect fail");
            }

            psmt = con.prepareStatement(query);

            psmt.setInt(1, limit);
            psmt.setInt(2, offset);

            rs = psmt.executeQuery();

            while (rs.next()) {
                users.add(retrieveUser(rs));
            }
        } catch (Exception e) {
            logger.error("getUsers: ", e);
        } finally {
            close(rs, psmt, con);
        }

        return users;
    }

	@Override
	public Collection<User> getUsers(String domain, Collection<String> loginNames) {
        Collection<User> users = new ArrayList<User>();
        
        if(loginNames.size() == 0)
        	return users;
        
        StringBuilder sb = new StringBuilder("(");
        for (String loginName : loginNames) {
        	if(sb.length() > 1)
        		sb.append(",");
        	sb.append("'").append(loginName).append("'");
        }
        sb.append(")");

        String query = "SELECT * from User a left join OrganizationUnit b on a.guid = b.guid" 
        		+ " left join AdminExtension c on a.loginName = c.loginName"
        		+ " left join UserExtension d on a.loginName = d.loginName" 
        		+ " WHERE a.loginName IN " + sb.toString();

        Connection con = null;
        PreparedStatement psmt = null;
        ResultSet rs = null;

        try {
            con = connectionService.getConnection();

            if (con == null) {
                logger.error("Can't connect to database. check database status.");
                throw new Exception("database connect fail");
            }

            psmt = con.prepareStatement(query);

            rs = psmt.executeQuery();

            while (rs.next()) {
                users.add(retrieveUser(rs));
            }
        } catch (Exception e) {
            logger.error("getUsers: ", e);
        } finally {
            close(rs, psmt, con);
        }

		return users;
	}

	@Override
	public Collection<User> getUsers(String domain, String orgUnitGuid, boolean includeChildren) {
		return getUsers(domain, orgUnitGuid, includeChildren, null, 0, Integer.MAX_VALUE, false);
	}

//	private int[] getUsers(Collection<User> users, String domain, String orgUnitGuid, boolean includeChildren, String filter, int offset, int limit, boolean isAdmin) {
//		int userCount = countUser(orgUnitGuid, filter, isAdmin);
//		int diff = offset - userCount;
//		
//		logger.trace("offset [{}]  limit [{}]  user count [{}]  diff [{}]", new Object[]{offset, limit, userCount, diff});
//		
//		if(diff == 0)			//offset 만큼의 사용자 정보를 스킴함.
//			offset = 0;
//		else if(diff < 0) {		//사용자  데이터를 가져온다.
//			Collection<User> childUsers  = getUsers(domain, orgUnitGuid, offset, limit, filter, isAdmin);
//			users.addAll(childUsers);
//			offset = 0;
//			limit += diff;
//		} else {				//스킴하는 사용자 수를 offset에서 제해줌. 
//			offset -= userCount;
//		}
//		
//		if(includeChildren && limit > 0) {
//			Collection<OrganizationUnit> children = orgUnitApi.getOrganizationUnitsByParent(domain, orgUnitGuid);
//			for (OrganizationUnit ou : children) {
//				if(limit <= 0)
//					break;
//				//offset과 limit 값은 사용한 만큼 계속 갱신되어야 한다.
//				int[] arr = getUsers(users, domain, ou.getGuid(), includeChildren, filter, offset, limit, isAdmin);
//				offset = arr[0];
//				limit = arr[1];
//			}
//		}
//						
//		return new int[]{offset, limit};
//	}
	
	private void getUsers(Collection<User> users, String domain, String orgUnitGuid, boolean includeChildren, String filter, int offset, int limit, boolean isAdmin) {
		if(includeChildren) {
			Collection<String> guids = retrieveChildrenGuid(domain, orgUnitGuid);
	        Collection<User> children = getUsers(domain, genSQLInCond(guids), offset, limit, filter, isAdmin);
	        users.addAll(children);
		} else {
			String argGuid = "('" + orgUnitGuid + "')";
			Collection<User> children = getUsers(domain, argGuid, offset, limit, filter, isAdmin);
			users.addAll(children);
		}
	}
	
	private String genSQLInCond(Collection<String> col) {
		StringBuilder sb = new StringBuilder("(");
		int count = 0;
        for (String str : col) {
        	if(sb.length() > 1)
        		sb.append(",");
        	sb.append("'").append(str).append("'");
        	count++;
        }
        sb.append(")");
        logger.debug("fetchced data count [{}]", count);
        return sb.toString();
	}
	
	private Collection<String> retrieveChildrenGuid(String domain, String orgUnitGuid) {
		Collection<String> guids = orgUnitApi.getGuidsByParent(domain, orgUnitGuid);
		logger.debug("retrieved children organization unit's guids [{}]", guids);
		return guids;
	}

	@Override
	public Collection<User> getUsers(String domain, String orgUnitGuid, boolean includeChildren, String filter, int offset,	int limit, boolean isAdmin) {
        Collection<User> users = new ArrayList<User>();

		if (orgUnitGuid == null && includeChildren) {
            return getUsers(domain, offset, limit, filter, isAdmin);
        }

		getUsers(users, domain, orgUnitGuid, includeChildren, filter, offset, limit, isAdmin);

		return users;
	}

	@Override
	public Collection<User> getUsers(String domain, String domainController) {
        Collection<User> users = new ArrayList<User>();

        String query = "SELECT * from User a left join OrganizationUnit b on a.guid = b.guid" 
        		+ " left join AdminExtension c on a.loginName = c.loginName"
        		+ " left join UserExtension d on a.loginName = d.loginName" 
        		+ " WHERE a.loginName LIKE FILTER OR a.name LIKE FILTER ORDER BY LENGTH(a.loginName), a.loginName";

        String filter = "'" + domainController + "%'";

        query = query.replace("FILTER", filter);

        Connection con = null;
        PreparedStatement psmt = null;
        ResultSet rs = null;

        try {
            con = connectionService.getConnection();

            if (con == null) {
                logger.error("Can't connect to database. check database status.");
                throw new Exception("database connect fail");
            }

            psmt = con.prepareStatement(query);
            rs = psmt.executeQuery();

            while (rs.next()) {
                users.add(retrieveUser(rs));
            }
        } catch (Exception e) {
            logger.error("getUsers: ", e);
        } finally {
            close(rs, psmt, con);
        }

        return users;
	}

	@Override
	public User findUser(String domain, String loginName) {
		return getUser(domain, loginName);
	}

	@Override
	public Collection<User> getUsers(String domain, Predicate pred) {
        Collection<User> users = new ArrayList<User>();

        String query = "SELECT * from User a left join OrganizationUnit b on a.guid = b.guid" 
        		+ " left join AdminExtension c on a.loginName = c.loginName"
        		+ " left join UserExtension d on a.loginName = d.loginName" 
        		+ " ORDER BY LENGTH(a.loginName), a.loginName";

        Connection con = null;
        PreparedStatement psmt = null;
        ResultSet rs = null;

        try {
            con = connectionService.getConnection();

            if (con == null) {
                logger.error("Can't connect to database. check database status.");
                throw new Exception("database connect fail");
            }

            psmt = con.prepareStatement(query);
            rs = psmt.executeQuery();

            while (rs.next()) {
                users.add(retrieveUser(rs));
            }
        } catch (Exception e) {
            logger.error("getUsers: ", e);
        } finally {
            close(rs, psmt, con);
        }

        return users;
	}

	@Override
	public User getUser(String domain, String loginName) {
		String query = "SELECT * from User a left join OrganizationUnit b on a.guid = b.guid" 
        		+ " left join AdminExtension c on a.loginName = c.loginName"
        		+ " left join UserExtension d on a.loginName = d.loginName"
        		+ " WHERE a.loginName = ?";

        Connection con = null;
        PreparedStatement psmt = null;
        ResultSet rs = null;

        try {
            con = connectionService.getConnection();

            if (con == null) {
                logger.error("Can't connect to database. check database status.");
                throw new Exception("database connect fail");
            }

            psmt = con.prepareStatement(query);
            psmt.setString(1, loginName);

            rs = psmt.executeQuery();

            if (rs.next()) {
                return retrieveUser(rs);
            }
        } catch (Exception e) {
            logger.error("getUser: ", e);
        } finally {
            close(rs, psmt, con);
        }

        return null;
	}

	@Override
	public Map<String, Object> getUserExtension(String loginName, String sourceType) {
    	String userExtensionName = "";

        if (userExtensionProviders.get(loginName) != null)
            userExtensionName = userExtensionProviders.get(loginName).getExtensionName();

        if (userExtensionName.length() == 0) {
            userExtensionName = "frodo";
        }

        Map<String, Object> userExtension = new HashMap<String, Object>();

        String query = "SELECT * FROM AdminExtension WHERE loginName = ?";

        Connection con = null;
        PreparedStatement psmt = null;
        ResultSet rs = null;

        try {
            con = connectionService.getConnection();

            if (con == null) {
                logger.error("Can't connect to database. check database status.");
                throw new Exception("database connect fail");
            }

            psmt = con.prepareStatement(query);
            psmt.setString(1, loginName);
            rs = psmt.executeQuery();

            if (rs.next()) {
                Map<String, Object> adminExtension = new HashMap<String, Object>();
                adminExtension.put("role", getRole(rs.getString("roleName")));
                adminExtension.put("profile", getProfileProgram(rs.getString("programProfileName")));
                adminExtension.put("lang", rs.getString("lang"));
                adminExtension.put("use_login_lock", rs.getBoolean("useLoginLock"));
                adminExtension.put("login_lock_count", rs.getInt("loginLockCount"));
                adminExtension.put("login_failures", rs.getInt("loginFailures"));

                if (rs.getTimestamp("lastLoginFailedDateTime") != null)
                    adminExtension.put("last_login_failed_date_time", toDate(rs.getTimestamp("lastLoginFailedDateTime")));
                else
                    adminExtension.put("last_login_failed_date_time", null);

                adminExtension.put("use_idle_timeout", rs.getBoolean("useIdleTimeout"));
                adminExtension.put("idle_timeout", rs.getInt("idleTimeout"));
                adminExtension.put("last_login_date_time", rs.getTimestamp("lastLoginDateTime") == null ? null : toDate(rs.getTimestamp("lastLoginDateTime")));
                adminExtension.put("is_enabled", rs.getBoolean("isEnabled"));
                adminExtension.put("use_otp", rs.getBoolean("useOtp"));
                adminExtension.put("otp_seed", rs.getString("otpSeed"));
                adminExtension.put("use_acl", rs.getBoolean("useAcl"));
                adminExtension.put("created", toDate(rs.getTimestamp("created")));

                userExtension.put("admin", adminExtension);
            }
        } catch (Exception e) {
            logger.error("getUserExtension: ", e);
        } finally {
            close(rs, psmt, con);
        }

        query = "SELECT * FROM UserExtension WHERE loginName = ?";

        con = null;
        psmt = null;
        rs = null;

        try {
            con = connectionService.getConnection();

            if (con == null) {
                logger.error("Can't connect to database. check database status.");
                throw new Exception("database connect fail");
            }

            psmt = con.prepareStatement(query);
            psmt.setString(1, loginName);
            rs = psmt.executeQuery();

            if (rs.next()) {
                Map<String, Object> frodoExtension = new HashMap<String, Object>();

                frodoExtension.put("cid", rs.getString("cid"));
                frodoExtension.put("static_ip4", rs.getString("staticIp4"));
                frodoExtension.put("allow_ip4_from", rs.getString("allowIp4From"));
                frodoExtension.put("allow_ip4_to", rs.getString("allowIp4To"));
                frodoExtension.put("is_locked", rs.getBoolean("isLocked"));
                frodoExtension.put("login_failures", rs.getInt("loginFailures"));
                frodoExtension.put("last_ip", rs.getString("lastIp"));
                frodoExtension.put("vid", rs.getString("vid"));
                frodoExtension.put("salt", rs.getString("salt"));
                frodoExtension.put("idn_hash", rs.getString("idnHash"));
                frodoExtension.put("subject_dn", rs.getString("subjectDn"));
                frodoExtension.put("auth_key", rs.getString("deviceAuthKey"));
                frodoExtension.put("device_key_count_setting", rs.getInt("deviceKeyCountSetting"));
                frodoExtension.put("expire_at", rs.getTimestamp("expireDateTime") == null ? null : toDate(rs.getTimestamp("expireDateTime")));
                frodoExtension.put("start_at", rs.getTimestamp("startDateTime") == null ? null : toDate(rs.getTimestamp("startDateTime")));
                frodoExtension.put("key_expire_at", rs.getTimestamp("keyExpireDateTime") == null ? null : toDate(rs.getTimestamp("keyExpireDateTime")));
                frodoExtension.put("last_login_at", rs.getTimestamp("lastLoginTime") == null ? null : toDate(rs.getTimestamp("lastLoginTime")));
                frodoExtension.put("last_logout_at", rs.getTimestamp("lastLogoutTime") == null ? null : toDate(rs.getTimestamp("lastLogoutTime")));
                frodoExtension.put("last_password_hash", rs.getString("lastPasswordHash"));
                frodoExtension.put("last_password_change", rs.getTimestamp("lastPasswordChange") == null ? null : toDate(rs.getTimestamp("lastPasswordChange")));
                frodoExtension.put("created_at", toDate(rs.getTimestamp("createDateTime")));
                frodoExtension.put("updated_at", toDate(rs.getTimestamp("updateDateTime")));

                String accessProfileGuid = rs.getString("accessProfileGuid");

                if (accessProfileGuid == null || accessProfileGuid.length() == 0) {
                    frodoExtension.put("profile", null);
                } else {
                	ConfigDatabase db = conf.ensureDatabase("frodo");
            		Config c = db.findOne(AccessProfile.class, Predicates.field("guid", accessProfileGuid));
            		if (c != null) {
	                    frodoExtension.put("profile", c.getDocument(AccessProfile.class, newParseCallback()));
            		}
                }

                frodoExtension.put("force_password_change", rs.getBoolean("forcePasswordChange"));
                frodoExtension.put("last_password_fail_time", rs.getTimestamp("lastPasswordFailTime") == null ? null : toDate(rs.getTimestamp("lastPasswordFailTime")));
                frodoExtension.put("source_type", rs.getString("sourceType"));
                frodoExtension.put("cert_type", rs.getString("certType"));
                frodoExtension.put("is_auto_locked", rs.getBoolean("isAutoLocked"));
                frodoExtension.put("auto_lock_released_time", rs.getTimestamp("autoLockReleasedTime") == null ? null : toDate(rs.getTimestamp("autoLockReleasedTime")));
                frodoExtension.put("twoway_auth_status", rs.getInt("twowayAuthStatus"));
                frodoExtension.put("allow_ip_ranges", getUserClientIpRange(loginName));
                frodoExtension.put("allow_time_table_id", rs.getString("allowTimeTableId"));

//                if (sourceType.equals("ldap")) {
//                    userExtension.put("ldap", frodoExtension);
//                } else {
                    userExtension.put("frodo", frodoExtension);
//                }
//
            }
        } catch (Exception e) {
            logger.error("getUserExtension: ", e);
        } finally {
            close(rs, psmt, con);
        }

        return userExtension;
    }
	
	@Override
	public Collection<User> getUsersWithCondOnUserExt(String cond) {
		String query = "SELECT loginName FROM UserExtension WHERE " + cond;
	               
		Connection con = null;
		PreparedStatement psmt = null;
		ResultSet rs = null;
		
		Collection<User> collections = new ArrayList<User>();
		try {
			con = connectionService.getConnection();
			
			if (con == null) {
				logger.error("Can't connect to database. check database status.");
				throw new Exception("database connect fail");
			}
	
			psmt = con.prepareStatement(query);
			rs = psmt.executeQuery();
	
			while (rs.next()) {
				User user = findUser("localhost", rs.getString("loginName"));
				collections.add(user);
			}
		} catch (Exception e) {
			logger.error("getExtWithCond: ", e);
		} finally {
			close(rs, psmt, con);
		}
		return collections;
	}
	 

    public Collection<String> getUserNames(int offset, int limit) {
        Collection<String> loginNames = new ArrayList<String>();

        String query = "SELECT * FROM User LIMIT ? OFFSET ?";

        Connection con = null;
        PreparedStatement psmt = null;
        ResultSet rs = null;

        try {
            con = connectionService.getConnection();

            if (con == null) {
                logger.error("Can't connect to database. check database status.");
                throw new Exception("database connect fail");
            }

            psmt = con.prepareStatement(query);
            rs = psmt.executeQuery();
            psmt.setInt(1, limit);
            psmt.setInt(2, offset);

            while (rs.next()) {
                loginNames.add(rs.getString("loginName"));
            }
        } catch (Exception e) {
            logger.error("getUserNames: ", e);
        } finally {
            close(rs, psmt, con);
        }

        return loginNames;
    }

    public Collection<String> getUserNames(String guid, int offset, int limit) {
        Collection<String> loginNames = new ArrayList<String>();

        String query = "SELECT * FROM User LIMIT ? OFFSET ?";

        Connection con = null;
        PreparedStatement psmt = null;
        ResultSet rs = null;

        try {
            con = connectionService.getConnection();

            if (con == null) {
                logger.error("Can't connect to database. check database status.");
                throw new Exception("database connect fail");
            }

            psmt = con.prepareStatement(query);
            psmt.setInt(1, limit);
            psmt.setInt(2, offset);

            rs = psmt.executeQuery();

            while (rs.next()) {
                loginNames.add(rs.getString("loginName"));
            }
        } catch (Exception e) {
            logger.error("getUserNames: ", e);
        } finally {
            close(rs, psmt, con);
        }

        return loginNames;
    }

	@Override
	public Collection<String> getLoginNames(String domain, String orgUnitGuid, boolean includeChildren, Predicate pred,
			int offset, int limit) {
		if (orgUnitGuid == null && includeChildren)
			return getUserNames(offset, limit);

		Collection<String> loginNames = getUserNames(orgUnitGuid, offset, limit);

		int dec = Math.min(offset, loginNames.size());
		if (offset == dec) // offset <= users
			limit -= loginNames.size() - offset;
		offset -= dec;

		if (includeChildren) {
			OrganizationUnit parent = orgUnitApi.getOrganizationUnit(domain, orgUnitGuid);
			for (OrganizationUnit ou : parent.getChildren()) {
				if (limit <= 0)
					break;

				Collection<String> childUsers = getLoginNames(domain, ou.getGuid(), includeChildren, pred, offset, limit);
				dec = Math.min(offset, childUsers.size());
				if (offset == dec) // offset <= child users
					limit -= childUsers.size() - offset;
				offset -= dec;

				loginNames.addAll(childUsers);
			}
		}

		return loginNames;
	}

//	private class LoginNameFetcher implements ObjectBuilder<String> {
//		@Override
//		public String build(Config c) {
//			@SuppressWarnings("unchecked")
//			Map<String, Object> m = (Map<String, Object>) c.getDocument();
//			String loginName = (String) m.get("login_name");
//			return loginName;
//		}
//	}

	@Override
	public void createUsers(String domain, Collection<User> users) {
		createUsers(domain, users, false);
	}

	@Override
	public void createUsers(String domain, Collection<User> users, boolean noHash) {
		if (users == null || users.size() == 0)
			return;

		List<User> userList = new ArrayList<User>(users);
		if (!noHash) {
			int saltLength = getSaltLength(domain);
			for (User user : users) {
				user.setSalt(createSalt(saltLength));
				user.setPassword(hashPassword(user.getSalt(), user.getPassword()));
			}
		}

        int createUserCount = 0;

        for (User user : userList) {
            createUserCount += createUser(user);
        }

        logger.trace("[{}] users are created", createUserCount);
        
		setDomAdminUsersCount(userList.size());
		//write log.
		reportUserLog.writeUsersLog(userList, "created");
	}

	@Override
	public void createUser(String domain, User user) {
		createUser(domain, user, false);
	}

	@Override
	public void createUser(String domain, User user, boolean noHash) {
		if (!noHash) {
			user.setSalt(createSalt(domain));
			user.setPassword(hashPassword(user.getSalt(), user.getPassword()));
		}

        createUser(user);

		setDomAdminUsersCount(1);
		//write log.
		reportUserLog.writeUserLog(user, "created");
	}

    private int createUser(User user) {
        String query = "INSERT INTO User (loginName, guid, name, description, password, salt, title, email, phone, lastPasswordChange, sourceType, created, updated) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?)";

        int insertCnt = 0;

        Connection con = null;
        PreparedStatement psmt = null;

        try {
            con = connectionService.getConnection();

            if (con == null) {
                logger.error("Can't connect to database. check database status.");
                throw new Exception("database connect fail");
            }

            psmt = con.prepareStatement(query);
            psmt.setString(1, user.getLoginName());

            if (user.getOrgUnit() != null) {
                psmt.setString(2, user.getOrgUnit().getGuid());
            } else {
                psmt.setString(2, null);
            }

            psmt.setString(3, user.getName());
            psmt.setString(4, user.getDescription());
            psmt.setString(5, user.getPassword());
            psmt.setString(6, user.getSalt());
            psmt.setString(7, user.getTitle());
            psmt.setString(8, user.getEmail());
            psmt.setString(9, user.getPhone());
            psmt.setTimestamp(10, new Timestamp(user.getLastPasswordChange().getTime()));
            psmt.setString(11, user.getSourceType());
            psmt.setTimestamp(12, getCurrentTimeStamp());
            psmt.setTimestamp(13, getCurrentTimeStamp());

            insertCnt = psmt.executeUpdate();
        } catch (SQLException e) {
            if (e.getMessage().contains("Duplicate entry")) {
            	logger.error("User info duplicate insert error", e);
                throw new DOMException(ALREADY_EXIST);
            }else {
            	logger.error("User info insert error", e);
            }
        } catch (Exception e) {
            logger.error("User info insert error", e);
        } finally {
            close(psmt, con);
        }

        if (user.getExt() != null) {
            if (user.getExt().get("admin") != null) {
            	Admin admin = null;
            	if(user.getExt().get("admin") instanceof Admin) {
            		admin = (Admin)user.getExt().get("admin");
            	} else if(user.getExt().get("admin") instanceof Map) {
            		admin = PrimitiveConverter.parse(Admin.class, user.getExt().get("admin"));
            	}

                query = "INSERT INTO AdminExtension (loginName, roleName, programProfileName, lang, useLoginLock, loginLockCount, loginFailures, lastLoginFailedDateTime, useIdleTimeout, idleTimeout, lastLoginDateTime, isEnabled, useOtp, otpSeed, useAcl, created) " +
                        "VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";

                try {
                    con = connectionService.getConnection();

                    if (con == null) {
                        logger.error("Can't connect to database. check database status.");
                        throw new Exception("database connect fail");
                    }

                    psmt = con.prepareStatement(query);
                    psmt.setString(1, user.getLoginName());
                    psmt.setString(2, admin.getRole().getName());
                    psmt.setString(3, admin.getProfile().getName());
                    psmt.setString(4, admin.getLang() == null ? "ko" : admin.getLang());
                    psmt.setBoolean(5, admin.isUseLoginLock());
                    psmt.setInt(6, admin.getLoginLockCount());
                    psmt.setInt(7, admin.getLoginFailures());

                    if (admin.getLastLoginFailedDateTime() != null)
                        psmt.setTimestamp(8, new Timestamp(admin.getLastLoginFailedDateTime().getTime()));
                    else
                        psmt.setTimestamp(8, null);

                    psmt.setBoolean(9, admin.isUseIdleTimeout());
                    psmt.setInt(10, admin.getIdleTimeout());

                    if (admin.getLastLoginDateTime() != null)
                        psmt.setTimestamp(11, new Timestamp(admin.getLastLoginDateTime().getTime()));
                    else
                        psmt.setTimestamp(11, null);

                    psmt.setBoolean(12, admin.isEnabled());
                    psmt.setBoolean(13, admin.isUseOtp());
                    psmt.setString(14, admin.getOtpSeed());
                    psmt.setBoolean(15, admin.isUseAcl());
                    psmt.setTimestamp(16, getCurrentTimeStamp());

                    insertCnt = psmt.executeUpdate();
                } catch (SQLException e) {
                    if (!e.getMessage().contains("Duplicate entry")) {
                        logger.error("AdminExtension info insert error", e);
                    }
                } catch (Exception e) {
                    logger.error("AdminExtension info insert error", e);
                } finally {
                    close(psmt, con);
                }
            } 
            
            if(user.getExt().get("frodo") != null) {
                UserExtension userExtension = new UserExtension();

                if (user.getExt().get("frodo") instanceof UserExtension) {
                    userExtension = (UserExtension) user.getExt().get("frodo");
                } else if (user.getExt().get("frodo") instanceof Map) {
                	userExtension = PrimitiveConverter.parse(UserExtension.class, user.getExt().get("frodo"));
                }

                insertUserExtension(user.getLoginName(), userExtension);
            }
        }

        logger.debug("User info insert success. loginName: [{}]", user.getLoginName());
        
        fireEntityAdded("localhost", user);

        return insertCnt;
    }

	@Override
	public void updateUsers(String domain, List<ConfigUpdateRequest<User>> userUpdates) {
		updateUsers(domain, userUpdates, false);
	}

	@Override
	public void updateUsers(String domain, List<ConfigUpdateRequest<User>> userUpdates, boolean updatePassword) {
		if (userUpdates == null || userUpdates.size() == 0)
			return;

		try {
            int updateUserCnt = 0;

			for (ConfigUpdateRequest<User> update : userUpdates) {
				User user = update.doc;
				user.setUpdated(new Date());
				if (updatePassword)
					user.setPassword(hashPassword(user.getSalt(), user.getPassword()));
				updateUserDB(user);
                updateUserCnt++;
			}

			logger.debug("kraken-dom: updated [{}] users",  updateUserCnt);
		} catch (Throwable e) {
			if (e instanceof DOMException)
				throw (DOMException) e;
			throw new RuntimeException(e);
		}
	}

    private void updateUserDB(User user) {
        String query = "UPDATE User SET guid = ?, name = ?, description = ?, password = ?, salt = ?, title = ?, email = ?, phone = ?, lastPasswordChange = ?, sourceType = ?, updated = ? WHERE loginName = ?";

        Connection con = null;
        PreparedStatement psmt = null;

        try {
            con = connectionService.getConnection();

            if (con == null) {
                logger.error("Can't connect to database. check database status.");
                throw new Exception("database connect fail");
            }

            psmt = con.prepareStatement(query);

            if (user.getOrgUnit() != null) {
                psmt.setString(1, user.getOrgUnit().getGuid());
            } else {
                psmt.setString(1, null);
            }

            psmt.setString(2, user.getName());
            psmt.setString(3, user.getDescription());
            psmt.setString(4, user.getPassword());
            psmt.setString(5, user.getSalt());
            psmt.setString(6, user.getTitle());
            psmt.setString(7, user.getEmail());
            psmt.setString(8, user.getPhone());
            psmt.setTimestamp(9, new Timestamp(user.getLastPasswordChange().getTime()));
            psmt.setString(10, user.getSourceType());
            psmt.setTimestamp(11, getCurrentTimeStamp());
            psmt.setString(12, user.getLoginName());

            psmt.executeUpdate();
        } catch (SQLException e) {
            logger.error("User info update error", e);
        } catch (Exception e) {
            logger.error("User info update error", e);
        } finally {
            close(psmt, con);
        }

        if (user.getExt() != null) {
            if (user.getExt().get("admin") != null) {
                Admin admin = null;

                if (user.getExt().get("admin") instanceof Admin)
                    admin = (Admin) user.getExt().get("admin");
                else if (user.getExt().get("admin") instanceof Map)
                    admin = PrimitiveConverter.parse(Admin.class, user.getExt().get("admin"));

                int insertResult = insertAdminExtension(user.getLoginName(), admin);

                if (insertResult ==0) {
                    updateAdminExtension(user.getLoginName(), admin);
                }
            }

            if (user.getExt().get("frodo") != null) {
                UserExtension userExtension = null;

                if (user.getExt().get("frodo") instanceof UserExtension) {
                    userExtension = (UserExtension) user.getExt().get("frodo");
                } else if (user.getExt().get("frodo") instanceof Map) {
                	userExtension = PrimitiveConverter.parse(UserExtension.class, user.getExt().get("frodo"));
                }

                int insertResult = insertUserExtension(user.getLoginName(), userExtension);
                if (insertResult == 0) {//이미 데이터가 있으므로 업데이트 작업 수행
                    updateUserExtension(user.getLoginName(), userExtension);
                }
            }
        }
        fireEntityUpdated("localhost", user);
    }
    
    @Override
    public boolean updateLoginName(String oldLoginName, String newLoginName) {
    	String query = "UPDATE User SET loginName = ?, updated = ? WHERE loginName = ?";
    	String query2 = "UPDATE UserExtension SET loginName = ?, updateDateTime = ? WHERE loginName = ?";
    	String query3 = "UPDATE AdminExtension SET loginName = ? WHERE loginName = ?";

        Connection con = null;
        PreparedStatement psmt = null;

        boolean result = false;
        try {
            con = connectionService.getConnection();

            if (con == null) {
                logger.error("Can't connect to database. check database status.");
                throw new Exception("database connect fail");
            }
            
            Timestamp ts = getCurrentTimeStamp();

            psmt = con.prepareStatement(query);
            psmt.setString(1, newLoginName);
            psmt.setTimestamp(2, ts);
            psmt.setString(3, oldLoginName);
            int count = psmt.executeUpdate();
            
            if(count > 0) {
            	//modified in User table.
            	result = true;
            	//update UserExtension
            	psmt = con.prepareStatement(query2);
            	psmt.setString(1, newLoginName);
                psmt.setTimestamp(2, ts);
                psmt.setString(3, oldLoginName);
            	psmt.executeUpdate();
            	//update AdminExtension
            	psmt = con.prepareStatement(query3);
            	psmt.setString(1, newLoginName);
                psmt.setString(2, oldLoginName);
            	psmt.executeUpdate();
            } 
        } catch (SQLException e) {
            logger.error("User info update error", e);
        } catch (Exception e) {
            logger.error("User info update error", e);
        } finally {
            close(psmt, con);
        }
        return result;
    }

    private void updateAdminExtension(String loginName, Admin admin) {
        String query = "UPDATE AdminExtension SET roleName = ?, programProfileName = ?, lang = ?, useLoginLock = ?, loginLockCount = ?, " +
                "loginFailures = ?, lastLoginFailedDateTime = ?, useIdleTimeout = ?, idleTimeout = ?, lastLoginDateTime = ?, isEnabled = ?, " +
                "useOtp = ?, otpSeed = ?, useAcl = ? WHERE loginName = ?";

        Connection con = null;
        PreparedStatement psmt = null;

        try {
            con = connectionService.getConnection();

            if (con == null) {
                logger.error("Can't connect to database. check database status.");
                throw new Exception("database connect fail");
            }

            psmt = con.prepareStatement(query);

            psmt.setString(1, admin.getRole().getName());
            psmt.setString(2, admin.getProfile().getName());
            psmt.setString(3, admin.getLang() == null ? "ko" : admin.getLang());
            psmt.setBoolean(4, admin.isUseLoginLock());
            psmt.setInt(5, admin.getLoginLockCount());
            psmt.setInt(6, admin.getLoginFailures());
            psmt.setTimestamp(7, admin.getLastLoginFailedDateTime() == null ? null : new Timestamp(admin.getLastLoginFailedDateTime().getTime()));
            psmt.setBoolean(8, admin.isUseIdleTimeout());
            psmt.setInt(9, admin.getIdleTimeout());
            psmt.setTimestamp(10, admin.getLastLoginDateTime() == null ? null : new Timestamp(admin.getLastLoginDateTime().getTime()));
            psmt.setBoolean(11, admin.isEnabled());
            psmt.setBoolean(12, admin.isUseOtp());
            psmt.setString(13, admin.getOtpSeed());
            psmt.setBoolean(14, admin.isUseAcl());
            psmt.setString(15, loginName);

            psmt.executeUpdate();
        } catch (SQLException e) {
            logger.error("AdminExtension info update error", e);
        } catch (Exception e) {
            logger.error("AdminExtension info update error", e);
        } finally {
            close(psmt, con);
        }
    }

    private int insertAdminExtension(String loginName, Admin admin) {
        String query = "INSERT INTO AdminExtension (loginName, roleName, programProfileName, lang, useLoginLock, loginLockCount, loginFailures, lastLoginFailedDateTime, useIdleTimeout, idleTimeout, lastLoginDateTime, isEnabled, useOtp, otpSeed, useAcl, created) " +
                "VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";

        Connection con = null;
        PreparedStatement psmt = null;

        try {
            con = connectionService.getConnection();

            if (con == null) {
                logger.error("Can't connect to database. check database status.");
                throw new Exception("database connect fail");
            }

            psmt = con.prepareStatement(query);
            psmt.setString(1, loginName);
            psmt.setString(2, admin.getRole().getName());
            psmt.setString(3, admin.getProfile().getName());
            psmt.setString(4, admin.getLang() == null ? "ko" : admin.getLang());
            psmt.setBoolean(5, admin.isUseLoginLock());
            psmt.setInt(6, admin.getLoginLockCount());
            psmt.setInt(7, admin.getLoginFailures());

            if (admin.getLastLoginFailedDateTime() != null)
                psmt.setTimestamp(8, new Timestamp(admin.getLastLoginFailedDateTime().getTime()));
            else
                psmt.setTimestamp(8, null);

            psmt.setBoolean(9, admin.isUseIdleTimeout());
            psmt.setInt(10, admin.getIdleTimeout());

            if (admin.getLastLoginDateTime() != null)
                psmt.setTimestamp(11, new Timestamp(admin.getLastLoginDateTime().getTime()));
            else
                psmt.setTimestamp(11, null);

            psmt.setBoolean(12, admin.isEnabled());
            psmt.setBoolean(13, admin.isUseOtp());
            psmt.setString(14, admin.getOtpSeed());
            psmt.setBoolean(15, admin.isUseAcl());
            psmt.setTimestamp(16, getCurrentTimeStamp());

            psmt.executeUpdate();

            return 1;
        } catch (SQLException e) {
            if (e.getMessage().contains("Duplicate entry")) {
                return 0;
            } else {
                logger.error("AdminExtension info insert error", e);
            }
        } catch (Exception e) {
            logger.error("AdminExtension info insert error", e);
        } finally {
            close(psmt, con);
        }

        return -1;
    }
    
    @Override
    public void deleteAdminExtension(String loginName) {
    	removeAdminExtDB(loginName);
    	fireEntityUpdated("localhost", findUser("localhost", loginName));
    }
    
    public void removeAdminExtDB(String loginName) {
    	String query = "DELETE FROM AdminExtension WHERE loginName = ?";
    	Connection con = null;
        PreparedStatement psmt = null;

        try {
            con = connectionService.getConnection();

            if (con == null) {
                logger.error("Can't connect to database. check database status.");
                throw new Exception("database connect fail");
            }

            psmt = con.prepareStatement(query);
            psmt.setString(1, loginName);

            psmt.executeUpdate();
        } catch (SQLException e) {
            logger.error("User admin ext delete error", e);
        } catch (Exception e) {
            logger.error("User admin ext error", e);
        } finally {
            close(psmt, con);
        }
    }

    private void updateUserExtension(String loginName, UserExtension userExtension) {
        String query = "UPDATE UserExtension SET cid = ?, staticIp4 = ?, allowIp4From = ?, allowIp4To = ?, isLocked = ?, " +
                "loginFailures = ?, lastIp = ?, vid = ?, salt = ?, idnHash = ?, subjectDn = ?, deviceAuthKey = ?," +
                "deviceKeyCountSetting = ?, expireDateTime = ?, startDateTime = ?, keyExpireDateTime = ?, lastLoginTime = ?, " +
                "lastLogoutTime = ?, lastPasswordHash = ?, lastPasswordChange = ?, updateDateTime =?, " +
                "accessProfileGuid = ?, forcePasswordChange =?, lastPasswordFailTime =?, sourceType =?, certType = ?, " +
                "isAutoLocked =?, autoLockReleasedTime = ?, twowayAuthStatus =?, allowTimeTableId = ? WHERE loginName = ?";
        Connection con = null;
        PreparedStatement psmt = null;

        try {
            con = connectionService.getConnection();

            if (con == null) {
                logger.error("Can't connect to database. check database status.");
                throw new Exception("database connect fail");
            }
            psmt = con.prepareStatement(query);

            psmt.setString(1, userExtension.getCid());
            psmt.setString(2, userExtension.getStaticIp4());
            psmt.setString(3, userExtension.getAllowIp4From());
            psmt.setString(4, userExtension.getAllowIp4From());
            psmt.setBoolean(5, userExtension.isLocked());
            psmt.setInt(6, userExtension.getLoginFailures());
            psmt.setString(7, userExtension.getLastIp());
            psmt.setString(8, userExtension.getVid());
            psmt.setString(9, userExtension.getSalt());
            psmt.setString(10, userExtension.getIdnHash());
            psmt.setString(11, userExtension.getSubjectDn());
            psmt.setString(12, userExtension.getDeviceAuthKey());
            psmt.setInt(13, userExtension.getDeviceKeyCountSetting());
            psmt.setTimestamp(14, userExtension.getExpireDateTime() == null ? null : new Timestamp(userExtension.getExpireDateTime().getTime()));
            psmt.setTimestamp(15, userExtension.getStartDateTime() == null ? null : new Timestamp(userExtension.getStartDateTime().getTime()));
            psmt.setTimestamp(16, userExtension.getKeyExpireDateTime() == null ? null : new Timestamp(userExtension.getKeyExpireDateTime().getTime()));
            psmt.setTimestamp(17, userExtension.getLastLoginTime() == null ? null : new Timestamp(userExtension.getLastLoginTime().getTime()));
            psmt.setTimestamp(18, userExtension.getLastLogoutTime() == null ? null : new Timestamp(userExtension.getLastLogoutTime().getTime()));
            psmt.setString(19, userExtension.getLastPasswordHash());
            psmt.setTimestamp(20, userExtension.getLastPasswordChange() == null ? null : new Timestamp(userExtension.getLastPasswordChange().getTime()));
            psmt.setTimestamp(21, userExtension.getUpdateDateTime() == null ? null : new Timestamp(userExtension.getUpdateDateTime().getTime()));
            psmt.setString(22, userExtension.getProfile() == null ? null : userExtension.getProfile().getGuid());
            psmt.setBoolean(23, userExtension.isForcePasswordChange());
            psmt.setTimestamp(24, userExtension.getLastPasswordFailTime() == null ? null : new Timestamp(userExtension.getLastPasswordFailTime().getTime()));
            psmt.setString(25, userExtension.getSourceType());
            psmt.setString(26, userExtension.getCertType());
            psmt.setBoolean(27, userExtension.isAutoLocked() == null ? false : userExtension.isAutoLocked());
            psmt.setTimestamp(28, userExtension.getAutoLockReleasedTime() == null ? null : new Timestamp(userExtension.getAutoLockReleasedTime().getTime()));

            if (userExtension.getTwowayAuthStatus() != null)
                psmt.setInt(29, userExtension.getTwowayAuthStatus());
            else
                psmt.setNull(29, Types.INTEGER);

            psmt.setString(30, userExtension.getAllowTimeTableId());
            psmt.setString(31, loginName);

            List<ClientIpRange> clientIpRanges = userExtension.getAllowIpRanges();

            if (clientIpRanges != null) {
                updateClientIpRangeDB(loginName, clientIpRanges);
            }

            psmt.executeUpdate();
        } catch (SQLException e) {
            logger.error("UserExtension info update error", e);
        } catch (Exception e) {
            logger.error("UserExtension info update error", e);
        } finally {
            close(psmt, con);
        }
    }

    private int insertUserExtension(String loginName, UserExtension userExtension) {
        String query = "INSERT INTO UserExtension (loginName, cid, staticIp4, allowIp4From, allowIp4To, isLocked, loginFailures, lastIp, vid, salt, idnHash, subjectDn, deviceAuthKey, deviceKeyCountSetting, expireDateTime, startDateTime, keyExpireDateTime, lastLoginTime, lastLogoutTime, lastPasswordHash, lastPasswordChange, createDateTime, updateDateTime, accessProfileGuid, forcePasswordChange, lastPasswordFailTime, sourceType, certType, isAutoLocked, autoLockReleasedTime, twowayAuthStatus, allowTimeTableId) " +
                "VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";

        Connection con = null;
        PreparedStatement psmt = null;

        try {
            con = connectionService.getConnection();

            if (con == null) {
                logger.error("Can't connect to database. check database status.");
                throw new Exception("database connect fail");
            }

            psmt = con.prepareStatement(query);

            psmt.setString(1, loginName);
            psmt.setString(2, userExtension.getCid());
            psmt.setString(3, userExtension.getStaticIp4());
            psmt.setString(4, userExtension.getAllowIp4From());
            psmt.setString(5, userExtension.getAllowIp4To());
            psmt.setBoolean(6, userExtension.isLocked());
            psmt.setInt(7, userExtension.getLoginFailures());
            psmt.setString(8, userExtension.getLastIp());
            psmt.setString(9, userExtension.getVid());
            psmt.setString(10, userExtension.getSalt());
            psmt.setString(11, userExtension.getIdnHash());
            psmt.setString(12, userExtension.getSubjectDn());
            psmt.setString(13, userExtension.getDeviceAuthKey());
            psmt.setInt(14, userExtension.getDeviceKeyCountSetting());

            if (userExtension.getExpireDateTime() != null)
                psmt.setTimestamp(15, new Timestamp(userExtension.getExpireDateTime().getTime()));
            else
                psmt.setNull(15, Types.TIMESTAMP);

            if (userExtension.getStartDateTime() != null)
                psmt.setTimestamp(16, new Timestamp(userExtension.getStartDateTime().getTime()));
            else
                psmt.setNull(16, Types.TIMESTAMP);

            if (userExtension.getKeyExpireDateTime() != null)
                psmt.setTimestamp(17, new Timestamp(userExtension.getKeyExpireDateTime().getTime()));
            else
                psmt.setNull(17, Types.TIMESTAMP);

            if (userExtension.getLastLoginTime() != null)
                psmt.setTimestamp(18, new Timestamp(userExtension.getLastLoginTime().getTime()));
            else
                psmt.setNull(18, Types.TIMESTAMP);

            if (userExtension.getLastLogoutTime() != null)
                psmt.setTimestamp(19, new Timestamp(userExtension.getLastLogoutTime().getTime()));
            else
                psmt.setNull(19, Types.TIMESTAMP);

            psmt.setString(20, userExtension.getLastPasswordHash());

            if (userExtension.getLastPasswordChange() != null)
                psmt.setTimestamp(21, new Timestamp(userExtension.getLastPasswordChange().getTime()));
            else
                psmt.setNull(21, Types.TIMESTAMP);

            psmt.setTimestamp(22, getCurrentTimeStamp());
            psmt.setTimestamp(23, getCurrentTimeStamp());

            if (userExtension.getProfile()!= null)
                psmt.setString(24, userExtension.getProfile().getGuid());
            else
                psmt.setNull(24, Types.VARCHAR);

            psmt.setBoolean(25, userExtension.isForcePasswordChange());

            if (userExtension.getLastPasswordFailTime() != null)
                psmt.setTimestamp(26, new Timestamp(userExtension.getLastPasswordFailTime().getTime()));
            else
                psmt.setNull(26, Types.TIMESTAMP);

            psmt.setString(27, userExtension.getSourceType());
            psmt.setString(28, userExtension.getCertType());

            if (userExtension.isAutoLocked() != null)
                psmt.setBoolean(29, userExtension.isAutoLocked());
            else
                psmt.setNull(29, Types.BOOLEAN);

            if (userExtension.getAutoLockReleasedTime() != null)
                psmt.setTimestamp(30, new Timestamp(userExtension.getAutoLockReleasedTime().getTime()));
            else
                psmt.setNull(30, Types.TIMESTAMP);

            if (userExtension.getTwowayAuthStatus() != null)
                psmt.setInt(31, userExtension.getTwowayAuthStatus());
            else
                psmt.setNull(31, Types.INTEGER);

            if (userExtension.getAllowTimeTableId() != null)
                psmt.setString(32, userExtension.getAllowTimeTableId());
            else
                psmt.setNull(32, Types.VARCHAR);

            List<ClientIpRange> clientIpRanges = userExtension.getAllowIpRanges();

            for (ClientIpRange clientIpRange : clientIpRanges) {
                insertClientIpRangeDB(loginName, clientIpRange);
            }

            psmt.executeUpdate();

            return 1; //정상적으로 insert 성공
        } catch (SQLException e) {
            if (e.getMessage().contains("Duplicate entry")) {
                return 0; //이미 데이터가 있어서 insert 실패 하였을 경우
            } else {
                logger.error("UserExtension info insert error", e);
            }
        } catch (Exception e) {
            logger.error("UserExtension info insert error", e);
        } finally {
            close(psmt, con);
        }

        return -1; //에러
    }

    private void insertClientIpRangeDB(String loginName, ClientIpRange clientIpRange) {
        String query = "INSERT INTO ClientIpRange (loginName, ipFrom, ipTo) VALUES (?,?,?)";

        Connection con = null;
        PreparedStatement psmt = null;

        try {
            con = connectionService.getConnection();

            if (con == null) {
                logger.error("Can't connect to database. check database status.");
                throw new Exception("database connect fail");
            }

            psmt = con.prepareStatement(query);
            psmt.setString(1, loginName);
            psmt.setString(2, clientIpRange.getIpFrom());
            psmt.setString(3, clientIpRange.getIpTo());

            psmt.executeUpdate();
        } catch (SQLException e) {
            logger.error("ClientIpRange info insert error", e);
        } catch (Exception e) {
            logger.error("ClientIpRange info insert error", e);
        } finally {
            close(psmt, con);
        }
    }

    private void updateClientIpRangeDB(String loginName, List<ClientIpRange> clientIpRanges) {
        String query = "DELETE FROM ClientIpRange WHERE loginName = ?";

        Connection con = null;
        PreparedStatement psmt = null;

        try {
            con = connectionService.getConnection();

            if (con == null) {
                logger.error("Can't connect to database. check database status.");
                throw new Exception("database connect fail");
            }

            psmt = con.prepareStatement(query);
            psmt.setString(1, loginName);

            psmt.executeUpdate();
        } catch (SQLException e) {
            logger.error("ClientIpRange info delete error", e);
        } catch (Exception e) {
            logger.error("ClientIpRange info delete error", e);
        } finally {
            close(psmt, con);
        }

        for (ClientIpRange clientIpRange : clientIpRanges)
            insertClientIpRangeDB(loginName, clientIpRange);
    }

	@Override
	public void updateUsers(String domain, Collection<User> users, boolean updatePassword) {
		if (users == null || users.size() == 0)
			return;

        int updateUserCnt = 0;

		for (User user : users) {
			user.setUpdated(new Date());
			if (updatePassword)
				user.setPassword(hashPassword(user.getSalt(), user.getPassword()));

            updateUserDB(user);
            updateUserCnt++;
		}

        logger.debug("kraken-dom: updated [{}] users", updateUserCnt);
	}

	@Override
	public void updateUser(String domain, User user, boolean updatePassword) {
		// for backward compatibility
		if (user.getLastPasswordChange() == null)
			user.setLastPasswordChange(new Date());

		user.setUpdated(new Date());
		if (updatePassword)
			user.setPassword(hashPassword(user.getSalt(), user.getPassword()));

        updateUserDB(user);

        logger.debug("kraken-dom: updated success [{}] user", user.getLoginName());
	}

	@Override
	public void removeUsers(String domain, Collection<String> loginNames) {
		if (loginNames == null || loginNames.size() == 0)
			return;

		for (String loginName : loginNames) {
            removeUser(domain, loginName);
        }
	}

	@Override
	public void removeUser(String domain, String loginName) {
User user = getUser(domain, loginName);
		
        String query = "DELETE FROM User WHERE loginName = ?";

        Connection con = null;
        PreparedStatement psmt = null;

        try {
            con = connectionService.getConnection();

            if (con == null) {
                logger.error("Can't connect to database. check database status.");
                throw new Exception("database connect fail");
            }

            psmt = con.prepareStatement(query);
            psmt.setString(1, loginName);

            psmt.executeUpdate();
            
            removeUserExtDB(loginName);
            removeAdminExtDB(loginName);
            
            fireEntityRemoved("localhost", user);
    		setDomAdminUsersCount(-1);
    		//write log.
    		reportUserLog.writeUserLog(user, "removed");
        } catch (SQLException e) {
            logger.error("User info delete error", e);
        } catch (Exception e) {
            logger.error("User info delete error", e);
        } finally {
            close(psmt, con);
        }
	}
	
	private void removeUserExtDB(String loginName) {
        String query = "DELETE FROM UserExtension WHERE loginName = ?";

        Connection con = null;
        PreparedStatement psmt = null;

        try {
            con = connectionService.getConnection();

            if (con == null) {
                logger.error("Can't connect to database. check database status.");
                throw new Exception("database connect fail");
            }

            psmt = con.prepareStatement(query);
            psmt.setString(1, loginName);

            psmt.executeUpdate();
        } catch (SQLException e) {
            logger.error("User info delete error", e);
        } catch (Exception e) {
            logger.error("User info delete error", e);
        } finally {
            close(psmt, con);
        }
    }
   
    @Override
    public User getUserWithDeviceAuthKey(String domain, String deviceAuthKey) {
        String query = "SELECT * FROM UserExtension WHERE deviceAuthKey = ?";

        Connection con = null;
        PreparedStatement psmt = null;
        ResultSet rs = null;

        String loginName = null;

        try {
            con = connectionService.getConnection();

            if (con == null) {
                logger.error("Can't connect to database. check database status.");
                throw new Exception("database connect fail");
            }

            psmt = con.prepareStatement(query);
            psmt.setString(1, deviceAuthKey);

            rs = psmt.executeQuery();

            if (rs.next()) {
                loginName = rs.getString("loginName");
            }
        } catch (Exception e) {
            logger.error("getUserWithDeviceAuthKey: ", e);
        } finally {
            close(rs, psmt, con);
        }

        if (loginName != null) {
            return getUser(domain, loginName);
        }

        return null;
    }

    @Override
    public User getUserWithReservedIp(String domain, String reservedIp) {
        String query = "SELECT * FROM UserExtension WHERE staticIp4 = ?";

        Connection con = null;
        PreparedStatement psmt = null;
        ResultSet rs = null;

        String loginName = null;

        try {
            con = connectionService.getConnection();

            if (con == null) {
                logger.error("Can't connect to database. check database status.");
                throw new Exception("database connect fail");
            }

            psmt = con.prepareStatement(query);
            psmt.setString(1, reservedIp);

            rs = psmt.executeQuery();

            if (rs.next()) {
                loginName = rs.getString("loginName");
            }
        } catch (Exception e) {
            logger.error("getUserWithReservedIp: ", e);
        } finally {
            close(rs, psmt, con);
        }

        if (loginName != null) {
            return getUser(domain, loginName);
        }

        return null;
    }

    @Override
    public User findOtherUserUseReservedIp(String domain, String loginName, String reservedIp) {
        String query = "SELECT * FROM UserExtension WHERE loginName <> ? AND staticIp4 = ?";

        Connection con = null;
        PreparedStatement psmt = null;
        ResultSet rs = null;

        String revLoginName = null;

        try {
            con = connectionService.getConnection();

            if (con == null) {
                logger.error("Can't connect to database. check database status.");
                throw new Exception("database connect fail");
            }

            psmt = con.prepareStatement(query);
            psmt.setString(1, loginName);
            psmt.setString(2, reservedIp);

            rs = psmt.executeQuery();

            if (rs.next()) {
                revLoginName = rs.getString("loginName");
            }
        } catch (Exception e) {
            logger.error("findOtherUserUseReservedIp: ", e);
        } finally {
            close(rs, psmt, con);
        }

        if (revLoginName != null) {
            return getUser(domain, revLoginName);
        }

        return null;
    }

	@Override
	public Collection<UserExtensionProvider> getExtensionProviders() {
		return userExtensionProviders.values();
	}

	@Override
	public UserExtensionProvider getExtensionProvider(String name) {
		return userExtensionProviders.get(name);
	}

	@Override
	public void setSaltLength(String domain, int length) {
		if (length < 0 || length > 20)
			throw new IllegalArgumentException("invalid salt length. (valid: 0~20)");
		orgApi.setOrganizationParameter(domain, "salt_length", length);
	}

	@Override
	public int getSaltLength(String domain) {
		Object length = orgApi.getOrganizationParameter(domain, "salt_length");
		if (length == null || !(length instanceof Integer))
			return DEFAULT_SALT_LENGTH;
		return (Integer) length;
	}

	@Override
	public String createSalt(String domain) {
		int saltLength = getSaltLength(domain);
		logger.trace("kraken dom: salt length [{}]", saltLength);

		return createSalt(saltLength);
	}

	private String createSalt(int saltLength) {
		StringBuilder salt = new StringBuilder(saltLength);
		Random rand = new Random();
		for (int i = 0; i < saltLength; i++)
			salt.append(SALT_CHARS[rand.nextInt(SALT_CHARS.length)]);
		return salt.toString();
	}

	@Override
	public boolean verifyPassword(String domain, String loginName, String password) {
		User user = getUser(domain, loginName);
		String hash = hashPassword(user.getSalt(), password);

		// null check
		if (user.getPassword() == null || hash == null)
			return (password == hash);

		return user.getPassword().equals(hash);
	}

	@Override
	public String hashPassword(String salt, String text) {
		return Sha256.hashPassword(salt, text);
	}
	
	@Override
	public int getAllUserCount() {
		String query = "SELECT count(*) FROM User";

        Connection con = null;
        PreparedStatement psmt = null;
        ResultSet rs = null;

        int result = 0;
        try {
            con = connectionService.getConnection();

            if (con == null) {
                logger.error("Can't connect to database. check database status.");
                throw new Exception("database connect fail");
            }

            psmt = con.prepareStatement(query);
            rs = psmt.executeQuery();

            if (rs.next()) {
                result = rs.getInt(1);
            }
        } catch (Exception e) {
            logger.error("getAllUserCount: ", e);
        } finally {
            close(rs, psmt, con);
        }

        return result;
	}
	
	@Override
	public String getUserNameByLoginName(String loginName) {
		String query = "SELECT name FROM User WHERE loginName = ?";

        Connection con = null;
        PreparedStatement psmt = null;
        ResultSet rs = null;

        String result = null;
        try {
            con = connectionService.getConnection();

            if (con == null) {
                logger.error("Can't connect to database. check database status.");
                throw new Exception("database connect fail");
            }

            psmt = con.prepareStatement(query);
            psmt.setString(1, loginName);
            rs = psmt.executeQuery();

            if (rs.next()) {
                result = rs.getString(1);
            }
        } catch (Exception e) {
            logger.error("getUserNameByLoginName: ", e);
        } finally {
            close(rs, psmt, con);
        }

        return result;
	}
	
	@Override
	public String getOrgUnitIdByLoginName(String loginName) {
		String query = "SELECT guid FROM User WHERE loginName = ?";

        Connection con = null;
        PreparedStatement psmt = null;
        ResultSet rs = null;

        String result = null;
        try {
            con = connectionService.getConnection();

            if (con == null) {
                logger.error("Can't connect to database. check database status.");
                throw new Exception("database connect fail");
            }

            psmt = con.prepareStatement(query);
            psmt.setString(1, loginName);
            rs = psmt.executeQuery();

            if (rs.next()) {
                result = rs.getString(1);
            }
        } catch (Exception e) {
            logger.error("getOrgUnitIdByLoginName: ", e);
        } finally {
            close(rs, psmt, con);
        }

        return result;
	}
	
	@Override
	public List<String> getLoginNamesByAccessProfileGuid(String guid) {
		String query = "SELECT loginName FROM UserExtension WHERE accessProfileGuid = ?";

        Connection con = null;
        PreparedStatement psmt = null;
        ResultSet rs = null;

        List<String> result = new ArrayList<String>();
        try {
            con = connectionService.getConnection();

            if (con == null) {
                logger.error("Can't connect to database. check database status.");
                throw new Exception("database connect fail");
            }

            psmt = con.prepareStatement(query);
            psmt.setString(1, guid);
            rs = psmt.executeQuery();

            while (rs.next()) {
                result.add(rs.getString(1));
            }
        } catch (Exception e) {
            logger.error("getLoginNamesByAccessProfileGuid: ", e);
        } finally {
            close(rs, psmt, con);
        }

        return result;
	}
	
	@Override
	public List<String> searchLoginNameByName(String name, int offset, int limit) {
		String query = "SELECT loginName FROM User WHERE name like '%" + name + "%'";
		if(limit > 0)
			query += " limit " + limit + " offset " + offset;
		
        Connection con = null;
        PreparedStatement psmt = null;
        ResultSet rs = null;

        List<String> result = new ArrayList<String>();
        try {
            con = connectionService.getConnection();

            if (con == null) {
                logger.error("Can't connect to database. check database status.");
                throw new Exception("database connect fail");
            }

            psmt = con.prepareStatement(query);
            rs = psmt.executeQuery();

            while (rs.next()) {
                result.add(rs.getString(1));
            }
        } catch (Exception e) {
            logger.error("searchLoginNameByName: ", e);
        } finally {
            close(rs, psmt, con);
        }

        return result;
	}

	private static class FilterByLoginNames implements Predicate {

		private HashSet<String> loginNames = new HashSet<String>();

		public void addLoginName(String loginName) {
			loginNames.add(loginName);
		}

		@Override
		public boolean eval(Config c) {
			Object doc = c.getDocument();
			if (doc == null)
				return false;

			@SuppressWarnings("unchecked")
			Map<String, Object> m = (Map<String, Object>) doc;
			return loginNames.contains(m.get("login_name"));
		}
	}
	
	private void setDomAdminUsersCount(int count) {
		//lower version bundles don't have "dom.admin.users_count" parameter.
		Object obj = orgApi.getOrganizationParameter("localhost", "dom.admin.users_count");
		if(obj != null) {
			String users_count = (String) orgApi.getOrganizationParameter("localhost", "dom.admin.users_count");
			int value = Integer.parseInt(users_count) + count; 
			String strValue = new Integer(value).toString(); 
			orgApi.setOrganizationParameter("localhost", "dom.admin.users_count", strValue);
		}
	}
	
	private Map<String, Object> getProfileProgram(String programProfileName) {
        Map<String, Object> profileProgram = new HashMap<String, Object>();

        String query = "SELECT * FROM ProgramProfile WHERE name = ?";

        Connection con = null;
        PreparedStatement psmt = null;
        ResultSet rs = null;

        try {
            con = connectionService.getConnection();

            if (con == null) {
                logger.error("Can't connect to database. check database status.");
                return null;
            }

            psmt = con.prepareStatement(query);
            psmt.setString(1, programProfileName);

            rs = psmt.executeQuery();

            if (rs.next()) {
                profileProgram.put("name", rs.getString("name"));
                profileProgram.put("description", rs.getString("description"));
                profileProgram.put("created", toDate(rs.getTimestamp("created")));
                profileProgram.put("updated", toDate(rs.getTimestamp("updated")));
            }
        } catch (Exception e) {
            logger.error("getProfileProgram: ", e);
        } finally {
            close(rs, psmt, con);
        }

        List<String> programName = new ArrayList<String>();

        query = "SELECT * FROM ProgramProfileRel WHERE programProfileName = ?";

        con = null;
        psmt = null;
        rs = null;

        try {
            con = connectionService.getConnection();

            if (con == null) {
                logger.error("Can't connect to database. check database status.");
                return null;
            }

            psmt = con.prepareStatement(query);
            psmt.setString(1, programProfileName);

            rs = psmt.executeQuery();

            while (rs.next()) {
                programName.add(rs.getString("programName"));
            }
        } catch (Exception e) {
            logger.error("getProfileProgram: ", e);
        } finally {
            close(rs, psmt, con);
        }

        List<Program> programs = new ArrayList<Program>();

        query = "SELECT * FROM Program WHERE name = ?";

        for (int i = 0; i < programName.size(); i++) {
            con = null;
            psmt = null;
            rs = null;

            try {
                con = connectionService.getConnection();

                if (con == null) {
                    logger.error("Can't connect to database. check database status.");
                    return null;
                }

                psmt = con.prepareStatement(query);
                psmt.setString(1, programName.get(i));

                rs = psmt.executeQuery();

                if (rs.next()) {
                    Program program = new Program();
                    program.setName(rs.getString("name"));
            		program.setPack(rs.getString("pack"));
            		program.setDescription(rs.getString("description"));
            		program.setPath(rs.getString("path"));
            		program.setVisible(rs.getInt("visible")==1 ? true : false);
            		program.setSeq(rs.getInt("seq"));
            		program.setCreated(toDate(rs.getTimestamp("created")));
            		program.setUpdated(toDate(rs.getTimestamp("updated")));

                    programs.add(program);
                }
            } catch (Exception e) {
                logger.error("getProfileProgram: ", e);
            } finally {
                close(rs, psmt, con);
            }
        }

        profileProgram.put("programs", programs);

        return profileProgram;
    }
	
	private List<ClientIpRange> getUserClientIpRange(String loginName) {
        List<ClientIpRange> clientIpRanges = new ArrayList<ClientIpRange>();

        String query = "SELECT * FROM ClientIpRange WHERE loginName = ?";

        Connection con = null;
        PreparedStatement psmt = null;
        ResultSet rs = null;

        try {
            con = connectionService.getConnection();

            if (con == null) {
                logger.error("Can't connect to database. check database status.");
                return null;
            }

            psmt = con.prepareStatement(query);
            psmt.setString(1, loginName);

            rs = psmt.executeQuery();

            while (rs.next()) {
                ClientIpRange clientIpRange = new ClientIpRange();
                clientIpRange.setIpFrom(rs.getString("ipFrom"));
                clientIpRange.setIpTo(rs.getString("ipTo"));

                clientIpRanges.add(clientIpRange);
            }
        } catch (Exception e) {
            logger.error("getProfileProgram: ", e);
        } finally {
            close(rs, psmt, con);
        }

        return clientIpRanges;
    }
	
	private Map<String, Object> getRole(String roleName) {
        Map<String, Object> role = new HashMap<String, Object>();

        String query = "SELECT * FROM Role WHERE name = ?";

        Connection con = null;
        PreparedStatement psmt = null;
        ResultSet rs = null;

        try {
            con = connectionService.getConnection();

            if (con == null) {
                logger.error("Can't connect to database. check database status.");
                return null;
            }

            psmt = con.prepareStatement(query);
            psmt.setString(1, roleName);

            rs = psmt.executeQuery();

            if (rs.next()) {
                role.put("name", rs.getString("name"));
                role.put("level", rs.getInt("level"));
                role.put("permissions", getPermissions(rs.getString("name")));
                return role;
            }
        } catch (Exception e) {
            logger.error("getRole: ", e);
        } finally {
            close(rs, psmt, con);
        }

        return null;
    }
	
	private List<Permission> getPermissions(String roleName) {
    	List<Permission> permissions = new ArrayList<Permission>();

        String query = "SELECT * FROM RolePermission where roleName = ?";

        Connection con = null;
        PreparedStatement psmt = null;
        ResultSet rs = null;

        try {
            con = connectionService.getConnection();

            if (con == null) {
                logger.error("Can't connect to database. check database status.");
                return null;
            }

            psmt = con.prepareStatement(query);
            psmt.setString(1, roleName);
            rs = psmt.executeQuery();

            while (rs.next()) {
            	Permission perm = new Permission();
            	perm.setGroup(rs.getString("groupName"));
                perm.setPermission(rs.getString("permission"));
                permissions.add(perm);
            }
        } catch (Exception e) {
            logger.error("get permission failed: ", e);
        } finally {
            close(rs, psmt, con);
        }

        return permissions;
    }

    private void close(PreparedStatement psmt, Connection con) {
        logger.trace("dom-user-api: close sql jdbc");
        if (psmt != null)
            try {
                psmt.close();
            } catch (SQLException e) {
            	logger.error("error occurred during closing PreparedStatement", e);
            }
        if (con != null)
            try {
                con.close();
            } catch (SQLException e) {
            	logger.error("error occurred during closing Connection", e);
            }
    }

    private void close(ResultSet rs, PreparedStatement psmt, Connection con) {
        logger.trace("dom-user-api: close sql jdbc");
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
    
    private class UserExtensionProviderTracker extends ServiceTracker {
		public UserExtensionProviderTracker(BundleContext bc) {
			super(bc, UserExtensionProvider.class.getName(), null);
		}

		@Override
		public Object addingService(ServiceReference reference) {
			UserExtensionProvider p = (UserExtensionProvider) super.addingService(reference);
			userExtensionProviders.put(p.getExtensionName(), p);
			return p;
		}

		@Override
		public void removedService(ServiceReference reference, Object service) {
			UserExtensionProvider p = (UserExtensionProvider) service;
			userExtensionProviders.remove(p.getExtensionName());
			super.removedService(reference, service);
		}
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

    public static java.util.Date toDate(java.sql.Timestamp timestamp) {
        long milliseconds = timestamp.getTime() + (timestamp.getNanos() / 1000000);
        return new java.util.Date(milliseconds);
    }

    private static Timestamp getCurrentTimeStamp() {
        Date today = new Date();
        return new Timestamp(today.getTime());
    }
    
    @Override
    public void updateAdminUser(String domain, User user, String oldLoginName) {
    	// for backward compatibility
    	if (user.getLastPasswordChange() == null)
    		user.setLastPasswordChange(new Date());
    	
    	user.setUpdated(new Date());
    	user.setPassword(hashPassword(user.getSalt(), user.getPassword()));
//    	cfg.update(domain, cls, getPred(oldLoginName), user, NOT_FOUND, this);          
    }
    
    @Override
    public User getMasterUser() {
		String query = "SELECT loginName FROM AdminExtension where roleName = ?";

        Connection con = null;
        PreparedStatement psmt = null;
        ResultSet rs = null;
        
        String loginName = null;
        try {
            con = connectionService.getConnection();

            if (con == null) {
                logger.error("Can't connect to database. check database status.");
                throw new Exception("database connect fail");
            }

            psmt = con.prepareStatement(query);
            psmt.setString(1, "master");
            rs = psmt.executeQuery();

            if (rs.next()) {
            	loginName = rs.getString("loginName");
            }
        } catch (Exception e) {
            logger.error("getUsers: ", e);
        } finally {
        	close(rs, psmt, con);
        }

        if(loginName != null)
        	return findUser("localhost", loginName);
        else 
        	return null;
	}
}
