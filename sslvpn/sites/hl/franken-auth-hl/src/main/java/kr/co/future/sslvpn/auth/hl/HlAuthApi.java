package kr.co.future.sslvpn.auth.hl;

import java.util.Collection;

import kr.co.future.ldap.LdapProfile;

public interface HlAuthApi {
	Collection<LdapProfile> getLdapProfiles();
	void removeCacheList();
    boolean searchUserNameWithLoginName(String loginName, String name);
    boolean resetUserPassword(String loginName);
}
