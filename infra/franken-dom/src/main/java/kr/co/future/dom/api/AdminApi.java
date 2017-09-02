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
package kr.co.future.dom.api;

import java.util.Collection;

import kr.co.future.dom.model.Admin;
import kr.co.future.dom.model.User;

import kr.co.future.dom.api.LoginCallback;
import kr.co.future.dom.api.UserExtensionProvider;
import kr.co.future.msgbus.Session;

public interface AdminApi extends UserExtensionProvider {
	Collection<Admin> getAdmins(String domain);

	Admin findAdmin(String domain, String loginName);

	Admin getAdmin(String domain, String loginName);

	Admin getAdmin(String domain, User user);

	boolean canManage(String domain, Admin admin, User user);

	void setAdmin(String domain, String requestAdminLoginName, String targetUserLoginName, Admin admin);

	String updateOtpSeed(String domain, String requestAdminLoginName, String targetUserLoginName);

	void unsetAdmin(String domain, String requestAdminLoginName, String targetUserLoginName);

	Admin login(Session session, String loginName, String hash, boolean force);

	void logout(Session session);

	void registerLoginCallback(LoginCallback callback);

	void unregisterLoginCallback(LoginCallback callback);
	
	boolean isFirstLogin(Session session, String loginName);
	
	void modifyAdminLoginInfo(Session session, String loginName, String password, String trustHosts);
}
