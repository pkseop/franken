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
package kr.co.future.ldap;

import com.unboundid.ldap.sdk.LDAPConnection;
import com.unboundid.ldap.sdk.LDAPException;

import java.util.Collection;

public interface LdapService {
	Collection<LdapProfile> getProfiles();

	LdapProfile getProfile(String name);

	void createProfile(LdapProfile profile);

	void updateProfile(LdapProfile profile);

	void removeProfile(String name);

	Collection<LdapUser> getUsers(LdapProfile profile);

	LdapUser findUser(LdapProfile profile, String uid);

	Collection<LdapOrgUnit> getOrgUnits(LdapProfile profile);

	boolean verifyPassword(LdapProfile profile, String uid, String password);

	boolean verifyPassword(LdapProfile profile, String uid, String password, int timeout);

	void testLdapConnection(LdapProfile profile, Integer timeout);

	void changePassword(LdapProfile profile, String uid, String newPassword);

	void changePassword(LdapProfile profile, String uid, String newPassword, int timeout);
	
	LDAPConnection openLdapConnection(LdapProfile profile) throws LDAPException;

//	LDAPConnection getLdapConnection() throws LDAPException;
	
	LDAPConnection getLdapConnection(LdapProfile profile) throws LDAPException;

//    LDAPConnection getLdapsConnection() throws LDAPException;
    
    LDAPConnection getLdapsConnection(LdapProfile profile) throws LDAPException;

    void initializeLdapConnectionPool();

    void setInitialConnectionsSize(int initialConnectionPoolSize);

    void setMaxConnectionsSize(int maxConnectionPoolSize);

    int getInitialConnectionsSize();

    int getMaxConnectionsSize();
    
    //pks. 2014-11-10. frodo-ldap-extend's functions moved to here
    
    boolean isPasswordExpired(LdapProfile profile, String loginName) throws LDAPException, Exception;

	boolean isPasswordExpired(LdapProfile profile, LdapUser ldapUser) throws LDAPException, Exception;

	boolean isAccountExpired(LdapProfile profile, String loginName);

	boolean isAccountExpired(LdapProfile profile, LdapUser ldapUser);
}
