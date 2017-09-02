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
package kr.co.future.webconsole.plugins;

import kr.co.future.msgbus.Request;
import kr.co.future.msgbus.Response;
import kr.co.future.msgbus.handler.MsgbusMethod;
import kr.co.future.msgbus.handler.MsgbusPermission;
import kr.co.future.msgbus.handler.MsgbusPlugin;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Requires;
import kr.co.future.api.AccountManager;

@Component(name = "webconsole-account-plugin")
@MsgbusPlugin
public class AccountPlugin {

	@Requires
	private AccountManager accountManager;

	@MsgbusMethod
	public void getAccounts(Request req, Response resp) {
		resp.put("accounts", accountManager.getAccounts());
	}

	@MsgbusPermission(group = "account", code = "manage")
	@MsgbusMethod
	public void createAccount(Request req, Response resp) {
		String user = req.getString("user");
		String password = req.getString("password");

		accountManager.createAccount(user, password);
	}

	@MsgbusMethod
	public void changePassword(Request req, Response resp) {
		String user = req.getString("user");
		String oldPassword = req.getString("old_password");
		String newPassword = req.getString("new_password");

		accountManager.changePassword(user, oldPassword, newPassword);
	}

	@MsgbusPermission(group = "account", code = "manage_account")
	@MsgbusMethod
	public void removeAccount(Request req, Response resp) {
		String user = req.getString("user");
		accountManager.removeAccount(user);
	}

}
