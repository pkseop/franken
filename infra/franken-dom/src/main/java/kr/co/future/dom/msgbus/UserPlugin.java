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
package kr.co.future.dom.msgbus;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import kr.co.future.api.PrimitiveConverter;
import kr.co.future.confdb.Config;
import kr.co.future.confdb.Predicate;
import kr.co.future.dom.api.AdminApi;
import kr.co.future.dom.api.ConfigManager;
import kr.co.future.dom.api.DOMException;
import kr.co.future.dom.api.OrganizationUnitApi;
import kr.co.future.dom.api.UserApi;
import kr.co.future.dom.api.UserExtensionProvider;
import kr.co.future.dom.model.Admin;
import kr.co.future.dom.model.OrganizationUnit;
import kr.co.future.dom.model.User;
import kr.co.future.msgbus.MsgbusException;
import kr.co.future.msgbus.Request;
import kr.co.future.msgbus.Response;
import kr.co.future.msgbus.handler.MsgbusMethod;
import kr.co.future.msgbus.handler.MsgbusPermission;
import kr.co.future.msgbus.handler.MsgbusPlugin;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Requires;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(name = "dom-user-plugin")
@MsgbusPlugin
public class UserPlugin {
	private Logger logger = LoggerFactory.getLogger(UserPlugin.class);

	@Requires
	private ConfigManager conf;

	@Requires
	private UserApi userApi;

	@Requires
	private AdminApi adminApi;

	@Requires
	private OrganizationUnitApi orgUnitApi;

	@MsgbusMethod
	@MsgbusPermission(group = "dom", code = "user_edit")
	public void removeAllUsers(Request req, Response resp) {
		long start = new Date().getTime();
		String adminLoginName = req.getAdminLoginName();
		Admin admin = adminApi.findAdmin(req.getOrgDomain(), adminLoginName);
		String domain = req.getOrgDomain();
		Collection<User> users = userApi.getUsers(domain);

		Collection<String> loginNames = new ArrayList<String>();
		Map<String, String> failedList = new HashMap<String, String>();
		for (User u : users) {
			if (adminLoginName.equals(u.getLoginName())) {
				failedList.put(u.getLoginName(), "no-permission");
				continue;
			}
			if (!adminApi.canManage(domain, admin, u)) {
				failedList.put(u.getLoginName(), "no-permission");
				continue;
			}

			loginNames.add(u.getLoginName());
		}

		userApi.removeUsers(domain, loginNames);
		long end = new Date().getTime();
		if (logger.isTraceEnabled())
			logger.trace("kraken dom: remove [{}] users, [{}] milliseconds elapsed", loginNames.size(), end - start);
		resp.put("failed_list", failedList);
	}

	@MsgbusMethod
	@MsgbusPermission(group = "dom", code = "user_edit")
	public void moveUsers(Request req, Response resp) {
		if (!req.has("org_unit_guid"))
			throw new DOMException("null-org-unit");

		if (!req.has("login_names"))
			throw new DOMException("null-login-names");

		String adminLoginName = req.getAdminLoginName();
		Admin admin = adminApi.findAdmin(req.getOrgDomain(), adminLoginName);
		if (admin == null)
			throw new DOMException("admin-not-found");

		String orgUnitGuid = req.getString("org_unit_guid");
		String domain = req.getOrgDomain();

		// org unit guid can be null (for root node)
		OrganizationUnit orgUnit = null;
		if (orgUnitGuid != null)
			orgUnit = orgUnitApi.findOrganizationUnit(domain, orgUnitGuid);

		@SuppressWarnings("unchecked")
		HashSet<String> loginNames = new HashSet<String>((Collection<String>) req.get("login_names"));
		Collection<User> users = userApi.getUsers("localhost", loginNames);

		Map<String, String> failedList = new HashMap<String, String>();
		List<User> updates = new ArrayList<User>();
		for (User u : users) {
			// try to check role
			if (!adminApi.canManage(req.getOrgDomain(), admin, u)) {
				failedList.put(u.getLoginName(), "no-permission");
				continue;
			}
			u.setOrgUnit(orgUnit);
			updates.add(u);
		}

		userApi.updateUsers(domain, updates, false);

		resp.put("failed_list", failedList);
	}

	@MsgbusMethod
	public void getUsers(Request req, Response resp) {
		String orgUnitGuid = req.getString("ou_guid");
        String filter = "";
		Predicate pred = null;
		if (req.has("filter_name") || req.has("filter_login_name")) {
			String name = req.getString("filter_name");
			String loginName = req.getString("filter_login_name");

            if (name.length() > 0)
                filter = name;
            if (loginName.length() > 0)
                filter = loginName;

			pred = new Matched(name, loginName);
		}

		int offset = 0;
		int limit = Integer.MAX_VALUE;

		if (req.has("offset"))
			offset = req.getInteger("offset");
		if (req.has("limit"))
			limit = req.getInteger("limit");

		int total = userApi.countUsers(req.getOrgDomain(), orgUnitGuid, true, filter, false);
		Collection<User> users = userApi.getUsers(req.getOrgDomain(), orgUnitGuid, true, filter, offset, limit, false);

		resp.put("users", PrimitiveConverter.serialize(users));
		resp.put("total", total);
	}

	private static class Matched implements Predicate {
		private String userName;
		private String loginName;

		public Matched(String userName, String loginName) {
			this.userName = userName;
			this.loginName = loginName;
		}

		@Override
		public boolean eval(Config c) {
			@SuppressWarnings("unchecked")
			Map<String, Object> m = (Map<String, Object>) c.getDocument();

			String name = (String) m.get("name");
			String login = (String) m.get("login_name");

			if (name.contains(userName))
				return true;
			if (login.contains(loginName))
				return true;

			return false;
		}
	}

	@MsgbusMethod
	public void getUser(Request req, Response resp) {
		String loginName = req.getString("login_name");
		User user = userApi.getUser(req.getOrgDomain(), loginName);
		resp.put("user", PrimitiveConverter.serialize(user));
	}

	@MsgbusMethod
	@MsgbusPermission(group = "dom", code = "user_edit")
	public void createUser(Request req, Response resp) {
		String domain = req.getOrgDomain();
		User user = (User) PrimitiveConverter.overwrite(new User(), req.getParams());
		@SuppressWarnings("unchecked")
		Map<String, Object> m = (Map<String, Object>) req.get("org_unit");
		if (m != null)
			user.setOrgUnit(orgUnitApi.findOrganizationUnit(domain, (String) m.get("guid")));
		userApi.createUser(req.getOrgDomain(), user);
	}

	@MsgbusMethod
	public void updateUser(Request req, Response resp) {
		String domain = req.getOrgDomain();
		Admin request = adminApi.findAdmin(domain, req.getAdminLoginName());

		if (request == null)
			throw new DOMException("admin-not-found");

		String loginName = req.getString("login_name");
		User old = userApi.getUser(domain, loginName);
		if (request.getRole().getLevel() == 2) {
			if (req.getAdminLoginName().equals(loginName)) {
				old.setName(req.getString("name"));
				old.setDescription(req.getString("description"));
				old.setEmail(req.getString("email"));
				if (req.has("password"))
					old.setPassword(req.getString("password"));
				old.setTitle(req.getString("title"));
				old.setPhone(req.getString("phone"));

				@SuppressWarnings("unchecked")
				Map<String, Object> m = (Map<String, Object>) req.get("org_unit");
				if (m != null)
					old.setOrgUnit(orgUnitApi.findOrganizationUnit(domain, (String) m.get("guid")));
				else
					old.setOrgUnit(null);

				userApi.updateUser(domain, old, req.has("password"));

				return;
			} else if (adminApi.canManage(domain, request, old)) {
				User user = (User) PrimitiveConverter.overwrite(old, req.getParams());
				userApi.updateUser(domain, user, req.has("password"));

				return;
			} else
				throw new DOMException("no-permission");
		} else if (!adminApi.canManage(domain, request, old))
			throw new DOMException("no-permission");

//		User user = (User) PrimitiveConverter.overwrite(old, req.getParams(), conf.getParseCallback(domain));

        Map<String, Object> m = req.getParams();

        Map<String, Object> orgUnit = (Map<String, Object>) req.get("org_unit");

        if (orgUnit != null) {
            old.setOrgUnit(orgUnitApi.findOrganizationUnit(domain, (String) orgUnit.get("guid")));
        } else {
            old.setOrgUnit(null);
        }


        old.setName((String)m.get("name"));
        old.setLoginName((String)m.get("login_name"));
        old.setDescription((String)m.get("description"));
        old.setEmail((String)m.get("email"));
        old.setPhone((String)m.get("phone"));
        old.setTitle((String)m.get("title"));

        if (req.has("password"))
            old.setPassword(req.getString("password"));

		userApi.updateUser(domain, old, req.has("password"));
	}



	@SuppressWarnings("unchecked")
	@MsgbusMethod
	@MsgbusPermission(group = "dom", code = "user_edit")
	public void removeUsers(Request req, Response resp) {
		String adminLoginName = req.getAdminLoginName();
		Admin admin = adminApi.getAdmin(req.getOrgDomain(), adminLoginName);
		if (admin == null)
			throw new MsgbusException("dom", "admin-not-found");

		List<String> loginNames = new ArrayList<String>();
		Map<String, String> failedList = new HashMap<String, String>();

		Collection<User> users = userApi.getUsers(req.getOrgDomain(), (List<String>) req.get("login_names"));
		for (User user : users) {
			if (adminLoginName.equals(user.getLoginName())) {
				failedList.put(user.getLoginName(), "cannot-remove-self");
				continue;
			}
			if (!adminApi.canManage(req.getOrgDomain(), admin, user)) {
				failedList.put(user.getLoginName(), "no-permission");
				continue;
			}

			loginNames.add(user.getLoginName());
		}

		userApi.removeUsers(req.getOrgDomain(), loginNames);

		// return failed users
		resp.put("failed_list", failedList);
	}

	@MsgbusMethod
	@MsgbusPermission(group = "dom", code = "user_edit")
	public void removeUser(Request req, Response resp) {
		String loginName = req.getAdminLoginName();
		String domain = req.getOrgDomain();
		Admin admin = adminApi.getAdmin(req.getOrgDomain(), req.getAdminLoginName());
		if (admin == null)
			throw new MsgbusException("dom", "admin-not-found");

		User target = userApi.findUser(domain, req.getString("login_name"));
		if (target == null)
			throw new MsgbusException("dom", "user-not-found");

		if (loginName.equals(target.getLoginName()))
			throw new MsgbusException("dom", "cannot-remove-self");

		if (!adminApi.canManage(domain, admin, target))
			throw new MsgbusException("dom", "no-permission");

		userApi.removeUser(req.getOrgDomain(), target.getLoginName());
	}

	@MsgbusMethod
	public void getExtensionSchemas(Request req, Response resp) {
		List<String> schemas = new ArrayList<String>();
		for (UserExtensionProvider provider : userApi.getExtensionProviders())
			schemas.add(provider.getExtensionName());
		resp.put("schemas", schemas);
	}
}
