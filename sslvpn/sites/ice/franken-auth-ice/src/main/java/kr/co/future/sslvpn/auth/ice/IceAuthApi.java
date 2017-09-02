package kr.co.future.sslvpn.auth.ice;

import java.util.Map;

import kr.co.future.ldap.LdapProfile;
import kr.co.future.ldap.LdapUser;

public interface IceAuthApi {
	String getSubjectDn(String loginName);

	Map<String, Object> verifyUser(String loginName);
	
	IceConfig getConfig();
	
	void setConfig(IceConfig config);
	
	public boolean verifyPassword(LdapProfile profile, String uid, String password, int timeout);
	
	public boolean isAccountExpired(String loginName);
	
	public LdapUser findUser(LdapProfile profile, String uid);
	
	public String getAttribute(String loginName, String attributeName);
}
