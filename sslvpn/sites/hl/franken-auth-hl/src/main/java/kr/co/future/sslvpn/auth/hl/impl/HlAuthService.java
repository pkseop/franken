package kr.co.future.sslvpn.auth.hl.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.StringTokenizer;

import kr.co.future.sslvpn.model.UserExtension;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;

import kr.co.future.dom.api.OrganizationUnitApi;
import kr.co.future.dom.api.UserApi;
import kr.co.future.dom.model.OrganizationUnit;
import kr.co.future.dom.model.User;
import kr.co.future.ldap.LdapProfile;
import kr.co.future.ldap.LdapServerType;
import kr.co.future.ldap.LdapService;
import kr.co.future.ldap.LdapUser;
import kr.co.future.sslvpn.auth.BaseExternalAuthApi;
import kr.co.future.sslvpn.auth.hl.HlAuthApi;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.unboundid.ldap.sdk.Filter;
import com.unboundid.ldap.sdk.LDAPConnection;
import com.unboundid.ldap.sdk.SearchRequest;
import com.unboundid.ldap.sdk.SearchResult;
import com.unboundid.ldap.sdk.SearchResultEntry;
import com.unboundid.ldap.sdk.SearchScope;

@Component(name = "hl-auth-service")
@Provides
public class HlAuthService extends BaseExternalAuthApi implements HlAuthApi {
	private static final String LOCALHOST = "localhost";
    private static final String DEFAULT_PASSWORD = "hlihli1!";
    private static final String HANHWA_LIFE_BASEDN = "hanwhalife";
    private static final String KOREA_LIFE_BASEDN = "korealife";

	private final Logger logger = LoggerFactory.getLogger(HlAuthService.class.getName());

	@Requires
	private UserApi domUserApi;

	@Requires
	private kr.co.future.sslvpn.model.api.UserApi userApi;

	@Requires
	private OrganizationUnitApi orgUnitApi;

	@Requires
	private LdapService ldap;
	
//	private HashMap<String, LdapUser> ldapUserList = new HashMap<String, LdapUser>();
//	private HashMap<String, Long> ldapUserPwExList = new HashMap<String, Long>();

	@Override
	public Object login(Map<String, Object> props) {
		String loginName = (String) props.get("id");
		String password = (String) props.get("pw");

		long startTime = System.nanoTime();
		
		logger.debug("##### Start " + loginName + " login process #####");
		
		LdapUser ldapUser = null;
		LdapProfile currentProfile = null;
		
		for (LdapProfile profile : getLdapProfiles()) {
            if (profile.getBaseDn().contains(HANHWA_LIFE_BASEDN)) {
                logger.debug("frodo hl auth: connect to ldap server => " + profile.getTargetDomain());
                ldapUser = ldap.findUser(profile, loginName);

                if (ldapUser != null) {
                    currentProfile = profile;
                    break;
                }
            }
		}
		
		Map<String, Object> result = verifyUser(ldapUser, loginName);
		
		if (!(Boolean) result.get("result")) {
			result.put("auth_code", 13);
			
			logger.debug("##### End " + loginName + " login process fail. ldap user verify fail. elapsed time " + ((System.nanoTime() - startTime) / 1000000.0) + " milliseconds #####");
			
			return result;
		}

		result.put("auth_code", getAuthCode(currentProfile, ldapUser, loginName, password));
		
		logger.debug("##### End " + loginName + " login process success. elapsed time " + ((System.nanoTime() - startTime) / 1000000.0) + " milliseconds #####");
		
		return result;
	}
	
	@Override
	public Map<String, Object> verifyUser(String loginName) {
		logger.debug("frodo hl auth: verifyUser(String loginName) Called.");
		
		LdapUser ldapUser = null;
		
		for (LdapProfile profile : getLdapProfiles()) {
            if (profile.getBaseDn().contains(HANHWA_LIFE_BASEDN)) {
                logger.debug("frodo hl auth: connect to ldap server => " + profile.getTargetDomain());
                ldapUser = ldap.findUser(profile, loginName);
            }

            if (ldapUser != null) {
                break;
            }
		}
		
		return verifyUser(ldapUser, loginName);
	}

	public Map<String, Object> verifyUser(LdapUser ldapUser, String loginName) {
		Map<String, Object> result = new HashMap<String, Object>();

		User user = domUserApi.findUser(LOCALHOST, loginName);
		UserExtension ext = userApi.getUserExtension(user);
		
		if (ldapUser == null) {
			logger.trace("frodo hl auth: cannot found ldap user, login_name[{}]", loginName);
			removeUser(user, ext);
			result.put("result", false);
			return result;
		}

		String departmentNumber = ldapUser.getDepartmentNumber();
		if (user == null || (ext != null && ext.getSourceType() != null && ext.getSourceType().equals("department"))) {
			logger.trace("frodo hl auth: trying check department number. login_name [{}], department_number [{}]", loginName, departmentNumber);
			if (departmentNumber == null || departmentNumber.isEmpty()) {
				logger.trace("frodo hl auth: cannot found department number, login_name [{}], department_number [{}]", loginName, departmentNumber);
				removeUser(user, ext);
				result.put("result", false);
				return result;
			}

			Map<String, OrganizationUnit> orgUnits = new HashMap<String, OrganizationUnit>();
			for (OrganizationUnit ou : orgUnitApi.getOrganizationUnits(LOCALHOST))
				orgUnits.put(ou.getName(), ou);

			if (!orgUnits.containsKey(departmentNumber)) {
				logger.trace("frodo hl auth: connot found organizaion unit, department name[{}]", departmentNumber);
				removeUser(user, ext);
				result.put("result", false);
				return result;
			}

			result.put("source_type", "department");
			result.put("device_count", 1);
			result.put("org_unit_name", departmentNumber);
		} else if (user != null) {
			result.put("org_unit_name", user.getOrgUnit() == null ? null : user.getOrgUnit().getName());
			logger.debug("frodo hl auth: organization unit [{}]", result.get("org_unit_name"));
		}

		result.put("result", true);
		result.put("login_name", loginName);
		result.put("name", ldapUser.getDisplayName() == null ? loginName : ldapUser.getDisplayName());
		return result;
	}

	@Override
	public long getAccountExpiry(String loginName) {
		Collection<LdapProfile> profiles = getLdapProfiles();
		for (LdapProfile profile : profiles) {
			if (profile.getServerType() != LdapServerType.ActiveDirectory)
				continue;

            if (profile.getBaseDn().contains(HANHWA_LIFE_BASEDN)) {
                try {
                    LdapUser ldapUser = null;
                    ldapUser = ldap.findUser(profile, loginName);

                    if (ldapUser != null) {
                        if (ldapUser.getAccountExpires() == null)
                            return Long.MAX_VALUE;

                        return (ldapUser.getAccountExpires().getTime() - new Date().getTime())/ 1000;
                    }
                } catch (Exception e) {
                    logger.error("frodo hl auth: getAccountExpiry cannot connect ldap, profile name=[{}]", profile.getName(), e);
                    continue;
                }
            }
		}

		return Long.MAX_VALUE;
	}

	@Override
	public boolean isAccountExpirySupported() {
		return true;
	}

	@Override
	public boolean isPasswordExpirySupported() {
		return true;
	}

	@Override
	public boolean isPasswordChangeSupported() {
		for (LdapProfile profile : getLdapsProfiles()) {
			try {
				if (profile.getBaseDn().contains(HANHWA_LIFE_BASEDN) && profile.getX509Certificate() != null) {
                    return true;
                }
			} catch (Exception e) {
				logger.error("frodo hl auth: cannot obtain trust store", profile.getName(), e);
				continue;
			}
		}
		
		return false;
	}

	@Override
	public void changePassword(String account, String newPassword) {
		Collection<LdapProfile> profiles = getLdapsProfiles();
		for (LdapProfile profile : profiles) {
			if (profile.getServerType() != LdapServerType.ActiveDirectory)
				continue;

            if (profile.getBaseDn().contains(HANHWA_LIFE_BASEDN)) {
                try {
                    long startTime = System.nanoTime();
                    ldap.changePassword(profile, account, newPassword);

                    logger.debug("frodo hl auth: ldap.changePassword() run time => " + ((System.nanoTime() - startTime) / 1000000.0) + " milliseconds");
                    logger.debug("frodo hl auth: change password, profile name=[{}], login_name=[{}]", profile.getName(), account);
                } catch (Exception e) {
                    logger.error("frodo hl auth: cannot change password, profile name=[{}] error [{}]", new Object[]{profile.getName(), e});
                    continue;
                }
            }
		}
	}

	@Override
	public long getPasswordExpiry(String loginName) {
		Collection<LdapProfile> profiles = getLdapProfiles();
		for (LdapProfile profile : profiles) {
			if (profile.getServerType() != LdapServerType.ActiveDirectory)
				continue;

            if (profile.getBaseDn().contains(HANHWA_LIFE_BASEDN)) {
                try {
                    LdapUser ldapUser = null;

                    long startTime = System.nanoTime();
                    ldapUser = ldap.findUser(profile, loginName);

                    logger.debug("frodo hl auth: ldap.findUser() run time => " + ((System.nanoTime() - startTime) / 1000000.0) + " milliseconds");

                    if (ldapUser != null) {
                        return getPasswordExpiry(profile, ldapUser);
                    }
                } catch (Exception e) {
                    logger.error("frodo hl auth: getPasswordExpiry1 cannot connect ldap, profile name=[{}]", profile.getName(),e);
                    continue;
                }
            }
		}

		return Long.MAX_VALUE;
	}

	private void removeUser(User user, UserExtension ext) {
		if (user == null || !(ext != null && ext.getSourceType() != null && ext.getSourceType().equals("department")))
			return;

		logger.trace("frodo auth hl: invalid user. remove user login_name [{}]", user.getLoginName());
		domUserApi.removeUser(LOCALHOST, user.getLoginName());
	}



	private long getPasswordExpiry(LdapProfile profile, LdapUser ldapUser) {
        try {
            if (ldapUser.getPwdLastSet() == null) {
                return Long.MAX_VALUE;
            }
        } catch (Exception ex) {
            logger.error("Error: " , ex);
        }

        return getPasswordExpiryReal(profile, ldapUser);
	}

    private long getPasswordExpiryReal(LdapProfile profile, LdapUser ldapUser) {
        long startTime = System.nanoTime();
        LDAPConnection connection = null;

        try {
            connection = new LDAPConnection(profile.getDc(), profile.getPort());
            connection.bind(profile.getAccount(), profile.getPassword());

            SearchRequest searchRequest = new SearchRequest(buildBaseDN(profile), SearchScope.SUB, Filter.createEqualityFilter("objectClass", "domain"));
            SearchResult searchResult = connection.search(searchRequest);

            if(searchResult.getEntryCount() == 1) {
                SearchResultEntry entry = searchResult.getSearchEntries().get(0);
                long maxPwdAge = Math.abs(entry.getAttributeValueAsLong("maxPwdAge")) / 10000;
                long pwdLastSet = ldapUser.getPwdLastSet().getTime();

                logger.info("maxPwdAge: " + maxPwdAge);
                logger.info("pwdLastSet: " + pwdLastSet);

                if (pwdLastSet < 0) {
                    pwdLastSet = 0L;
                }

                long remainTime = (pwdLastSet + maxPwdAge) - new Date().getTime();
                logger.info("remainTime: " + remainTime);
                long temp = remainTime < 0 ? 0L : remainTime / 1000;

                logger.info("temp: " + temp);
                logger.debug("frodo hl auth: getPasswordExpiry() run time => " + ((System.nanoTime() - startTime) / 1000000.0) + " milliseconds");

                return temp;
            } else {
                return Long.MAX_VALUE;
            }
        } catch (Exception e) {
            logger.error("frodo hl auth: getPasswordExpiry2 cannot connect ldap, profile name=[{}], error [{}]", new Object[]{profile.getName(), e});
            return Long.MAX_VALUE;
        } finally {
            if (connection != null && connection.isConnected())
                connection.close();
        }
    }

	private int getAuthCode(LdapProfile profile, LdapUser user, String loginName, String password) {
		try {
			if (getPasswordExpiry(profile, user) < 0) {
				logger.trace("frodo hl auth: user password expired, login_name[{}]", loginName);
				return 24;
			}
		} catch (Exception e) {
			logger.error("frodo hl auth: cannot verify use passwordExpiryr, profile=[{}], login_name=[{}]", profile.getName(), loginName);
			return 12;
		}

		long startTime = System.nanoTime();
		if (!ldap.verifyPassword(profile, loginName, password)) {
			logger.debug("frodo hl auth: ldap.verifyPassword() run time => " + ((System.nanoTime() - startTime) / 1000000.0) + " milliseconds");
			logger.trace("frodo hl auth: user password verify fail, login_name[{}]", loginName);

            return 2;
		}
		logger.debug("frodo hl auth: ldap.verifyPassword() run time => " + ((System.nanoTime() - startTime) / 1000000.0) + " milliseconds");

		return 0;
	}

	public Collection<LdapProfile> getLdapProfiles() {
		Collection<LdapProfile> profiles = new ArrayList<LdapProfile>();
	
		profiles.addAll(ldap.getProfiles());
		
		List<LdapProfile> tempList = new ArrayList<LdapProfile>();
		
		for (LdapProfile ldapProfile : profiles) {
			try {
				if (ldapProfile.getX509Certificate() == null && ldapProfile.getBaseDn().contains(HANHWA_LIFE_BASEDN))
					tempList.add(ldapProfile);
			} catch (Exception e) {
				logger.error("frodo hl auth: getLdapProfiles() : " + e);
			}
		}
		
		Collections.shuffle(tempList, new Random());
		
		return tempList;
	}

    public Collection<LdapProfile> getAllLdapProfiles() {
        Collection<LdapProfile> profiles = new ArrayList<LdapProfile>();

        profiles.addAll(ldap.getProfiles());

        List<LdapProfile> tempList = new ArrayList<LdapProfile>();

        for (LdapProfile ldapProfile : profiles) {
            try {
                if (ldapProfile.getX509Certificate() == null)
                    tempList.add(ldapProfile);
            } catch (Exception e) {
                logger.error("frodo hl auth: getLdapProfiles() : " + e);
            }
        }

        Collections.shuffle(tempList, new Random());

        return tempList;
    }

	public Collection<LdapProfile> getLdapsProfiles() {
		Collection<LdapProfile> profiles = new ArrayList<LdapProfile>();
		
		profiles.addAll(ldap.getProfiles());
		
		List<LdapProfile> tempList = new ArrayList<LdapProfile>();
		
		for (LdapProfile ldapProfile : profiles) {
			try {
				if (ldapProfile.getX509Certificate() != null && ldapProfile.getBaseDn().contains(HANHWA_LIFE_BASEDN))
					tempList.add(ldapProfile);
			} catch (Exception e) {
				logger.error("frodo hl auth: getLdapProfiles() : " + e);
			}
		}
		
		Collections.shuffle(tempList, new Random());
		
		return tempList;
	}

    public Collection<LdapProfile> getAllLdapsProfiles() {
        Collection<LdapProfile> profiles = new ArrayList<LdapProfile>();

        profiles.addAll(ldap.getProfiles());

        List<LdapProfile> tempList = new ArrayList<LdapProfile>();

        for (LdapProfile ldapProfile : profiles) {
            try {
                if (ldapProfile.getX509Certificate() != null)
                    tempList.add(ldapProfile);
            } catch (Exception e) {
                logger.error("frodo hl auth: getLdapProfiles() : " + e);
            }
        }

        Collections.shuffle(tempList, new Random());

        return tempList;
    }

	private String buildBaseDN(LdapProfile profile) {
		if (profile.getBaseDn() != null)
			return profile.getBaseDn();

		String domain = profile.getDc();
		StringTokenizer t = new StringTokenizer(domain, ".");
		String dn = "";
		int i = 0;
		while (t.hasMoreTokens()) {
			if (i++ != 0)
				dn += ",";

			dn += "dc=" + t.nextToken();
		}

		return dn;
	}	

	@Override
	public void removeCacheList() {

	}

    @Override
    public boolean searchUserNameWithLoginName(String loginName, String name) {
        Collection<LdapProfile> profiles = getAllLdapProfiles();

        int foundCnt = 0;

        logger.info("Profiles size: " + profiles.size());

        for (LdapProfile profile : profiles) {
            if (profile.getServerType() != LdapServerType.ActiveDirectory)
                continue;

            logger.info("Profile basedn: " + profile.getBaseDn());
            if (profile.getBaseDn().contains(KOREA_LIFE_BASEDN)) {
                LdapUser ldapUser = ldap.findUser(profile, loginName);

                if (ldapUser != null && ldapUser.getDisplayName().equals(name)) {
                    logger.info("Korealife ldap: found user => " + ldapUser.getDisplayName());
                    foundCnt++;
                } else {
                    logger.info("korealife ldap: not found user => " + name);
                }

                continue;
            }

            if (profile.getBaseDn().contains(HANHWA_LIFE_BASEDN)) {
                LdapUser ldapUser = ldap.findUser(profile, loginName);

                if (ldapUser != null && ldapUser.getDisplayName().equals(name)) {
                    logger.info("hanwhalife ldap: found user => " + ldapUser.getDisplayName());
                    foundCnt++;
                } else {
                    logger.info("hanwhalife ldap: not found user => " + name);
                }
            }

        }

        if (foundCnt == 2) {
            return true;
        } else {
            return false;
        }
    }

    @Override
    public boolean resetUserPassword(String loginName) {
        if (resetPassword(loginName, DEFAULT_PASSWORD) == 0) {
            return true;
        } else {
            return false;
        }
    }

    private int resetPassword(String account, String newPassword) {
        Collection<LdapProfile> profiles = getAllLdapsProfiles();

        int errorCnt = 0;

        for (LdapProfile profile : profiles) {
            if (profile.getServerType() != LdapServerType.ActiveDirectory)
                continue;

            if (profile.getBaseDn().contains(KOREA_LIFE_BASEDN)) {
                try {
                    ldap.changePassword(profile, account, newPassword);
                    logger.debug("frodo hl auth: change password, profile name=[{}], login_name=[{}]", profile.getName(), account);
                } catch (Exception e) {
                    logger.error("frodo hl auth: cannot change password, profile name=[{}] error [{}]", new Object[]{profile.getName(), e});
                    errorCnt++;

                    continue;
                }

                continue;
            }

            if (profile.getBaseDn().contains(HANHWA_LIFE_BASEDN)) {
                try {
                    ldap.changePassword(profile, account, newPassword);
                    logger.debug("frodo hl auth: change password, profile name=[{}], login_name=[{}]", profile.getName(), account);
                } catch (Exception e) {
                    logger.error("frodo hl auth: cannot change password, profile name=[{}] error [{}]", new Object[]{profile.getName(), e});
                    errorCnt++;

                    continue;
                }
            }

        }

        return errorCnt;
    }
}
