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
package kr.co.future.dom.api;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import kr.co.future.dom.model.User;
import kr.co.future.confdb.Predicate;
import kr.co.future.dom.api.ConfigUpdateRequest;
import kr.co.future.dom.api.EntityEventProvider;
import kr.co.future.dom.api.UserExtensionProvider;

public interface UserApi extends EntityEventProvider<User> {
	int countUsers(String domain, String orgUnitGuid, boolean includeChildren, String filter, boolean isAdmin);

//	List<Config> getConfigs(String domain, String orgUnitGuid, boolean includeChildren, Predicate pred, int offset, int limit);

	Collection<User> getUsers(String domain);

	Collection<User> getUsers(String domain, int offset, int length);

	Collection<User> getUsers(String domain, Collection<String> loginNames);

	Collection<User> getUsers(String domain, String orgUnitGuid, boolean includeChildren);

	Collection<User> getUsers(String domain, String orgUnitGuid, boolean includeChildren, String filter, int offset, int limit, boolean isAdmin);

	Collection<User> getUsers(String domain, String domainController);

	Collection<User> getUsers(String domain, Predicate pred);
	
	public Map<String, Object> getUserExtension(String loginName, String sourceType);

	Collection<String> getLoginNames(String domain, String orgUnitGuid, boolean includeChildren, Predicate pred, int offset,
			int limit);

	User findUser(String domain, String loginName);

	User getUser(String domain, String loginName);
	
	User getMasterUser();

    User getUserWithDeviceAuthKey(String domain, String deviceAuthKey);

    User getUserWithReservedIp(String domain, String reservedIp);

    User findOtherUserUseReservedIp(String domain, String loginName, String reservedIp);

	void createUsers(String domain, Collection<User> users);

	void createUsers(String domain, Collection<User> users, boolean noHash);

	void createUser(String domain, User user);

	void createUser(String domain, User user, boolean noHash);

	void updateUsers(String domain, List<ConfigUpdateRequest<User>> userUpdates);

	void updateUsers(String domain, List<ConfigUpdateRequest<User>> userUpdates, boolean updatePassword);

	void updateUsers(String domain, Collection<User> users, boolean updatePassword);

	void updateUser(String domain, User user, boolean updatePassword);

	void removeUsers(String domain, Collection<String> loginNames);

	void removeUser(String domain, String loginName);

	Collection<UserExtensionProvider> getExtensionProviders();

	UserExtensionProvider getExtensionProvider(String name);

	void setSaltLength(String domain, int length);

	int getSaltLength(String domain);

	String createSalt(String domain);

	boolean verifyPassword(String domain, String loginName, String password);

	String hashPassword(String salt, String text);
	
	void deleteAdminExtension(String loginName);
	
	public int getAllUserCount();
	
	public String getUserNameByLoginName(String loginName);
	
	public String getOrgUnitIdByLoginName(String loginName);
	
	public List<String> getLoginNamesByAccessProfileGuid(String guid);
	
	public List<String> searchLoginNameByName(String name, int offset, int limit);
	
	void updateAdminUser(String domain, User user, String oldLoginName);
	
	public boolean updateLoginName(String oldLoginName, String newLoginName);
	
	public Collection<User> getUsersWithCondOnUserExt(String cond);
}
