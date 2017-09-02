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

import java.util.UUID;

import kr.co.future.dom.api.AdminApi;
import kr.co.future.dom.api.DOMException;
import kr.co.future.dom.api.GlobalConfigApi;
import kr.co.future.dom.api.impl.AdminApiImpl;
import kr.co.future.dom.api.impl.Sha256;
import kr.co.future.dom.model.Admin;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Requires;

import kr.co.future.dom.msgbus.LoginPlugin;
import kr.co.future.msgbus.Request;
import kr.co.future.msgbus.Response;
import kr.co.future.msgbus.Session;
import kr.co.future.msgbus.handler.AllowGuestAccess;
import kr.co.future.msgbus.handler.CallbackType;
import kr.co.future.msgbus.handler.MsgbusMethod;
import kr.co.future.msgbus.handler.MsgbusPlugin;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(name = "dom-login-plugin")
@MsgbusPlugin
public class LoginPlugin {
	private final Logger logger = LoggerFactory.getLogger(LoginPlugin.class);

	@Requires
	private GlobalConfigApi globalConfigApi;

	@Requires
	private AdminApi adminApi;

	@AllowGuestAccess
	@MsgbusMethod
	public void hello(Request req, Response resp) {
		String nonce = UUID.randomUUID().toString();
		String lang = req.getString("lang");
		if (lang == null)
			lang = "en";

		req.getSession().setProperty("nonce", nonce);
		req.getSession().setProperty("lang", lang);

		resp.put("nonce", nonce);
		resp.put("session_id", req.getSession().getGuid());
		resp.put("message", "login please.");
		resp.putAll(globalConfigApi.getConfigs(false));
	}

	@AllowGuestAccess
	@MsgbusMethod
	public void login(Request req, Response resp) {
		Session session = req.getSession();
		session.setProperty("org_domain", "localhost");
		String nick = req.getString("nick");
		String hash = req.getString("hash");
		String nonce = session.getString("nonce");
		boolean force = req.has("force") ? req.getBoolean("force") : false;

		logger.trace("kraken dom: login attempt nick [{}] hash [{}] nonce [{}]", new Object[] { nick, hash, nonce });
//		if(adminApi.isFirstLogin(session, nick))			//슈퍼관리자 최초 접속인지를 체크. CC에서 추가되었던 기능으로 현재는 사용하지 않음.
//			resp.put("result", "admin-first-login");
//		else 
		{
			Admin admin = adminApi.login(session, nick, hash, force);
			resp.put("result", "success");
			resp.put("use_idle_timeout", admin.isUseIdleTimeout());
			if (admin.isUseIdleTimeout())
				resp.put("idle_timeout", admin.getIdleTimeout());
	
			session.unsetProperty("nonce");
			session.setProperty("admin_login_name", admin.getUser().getLoginName());
			session.setProperty("locale", admin.getLang());
		}
	}

	@MsgbusMethod(type = CallbackType.SessionClosed)
	public void logout(Session session) {
		adminApi.logout(session);
	}
	
	@AllowGuestAccess
	@MsgbusMethod
	public void setFirstLoginAttr(Request req, Response resp) {
		String loginName = req.getString("login_name");
		String password = req.getString("password");
		String trustHosts = req.getString("trust_hosts");
		
		if(loginName.equals(AdminApiImpl.DEFAULT_LOGIN_NAME))
			throw new DOMException("invalid-login-name");
		else if(Sha256.hash(password).equals(AdminApiImpl.DEFAULT_PASSWORD))
			throw new DOMException("invalid-password");
		else {
			Session session = req.getSession();             
			adminApi.modifyAdminLoginInfo(session, loginName, password, trustHosts);
			resp.put("result", "success");
		}
	}
}
