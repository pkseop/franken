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

import java.util.Map;

import kr.co.future.dom.api.ConfigManager;
import kr.co.future.dom.api.OrganizationApi;
import kr.co.future.dom.model.Organization;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Requires;
import kr.co.future.api.PrimitiveConverter;
import kr.co.future.msgbus.Request;
import kr.co.future.msgbus.Response;
import kr.co.future.msgbus.handler.MsgbusMethod;
import kr.co.future.msgbus.handler.MsgbusPermission;
import kr.co.future.msgbus.handler.MsgbusPlugin;

@Component(name = "dom-org-plugin")
@MsgbusPlugin
public class OrganizationPlugin {
	@Requires
	private ConfigManager conf;

	@Requires
	private OrganizationApi orgApi;

	@MsgbusMethod
	public void getOrganization(Request req, Response response) {
		String domain = req.has("domain") ? req.getString("domain") : req.getOrgDomain();
		Organization organization = orgApi.getOrganization(domain);
		response.put("result", PrimitiveConverter.serialize(organization));
	}

	@MsgbusMethod
	@MsgbusPermission(group = "dom", code = "manage")
	public void createOrganization(Request req, Response resp) {
		Organization organization = (Organization) PrimitiveConverter.overwrite(new Organization(), req.getParams(),
				conf.getParseCallback(req.getOrgDomain()));
		orgApi.createOrganization(organization);
	}

	@MsgbusMethod
	@MsgbusPermission(group = "dom", code = "manage")
	public void removeOrganization(Request req, Response resp) {
		String domain = req.getString("domain");
		orgApi.removeOrganization(domain);
	}

	/**
	 * update own organization domain information
	 */
	@MsgbusMethod
	@MsgbusPermission(group = "dom", code = "config_edit")
	public void updateOrganization(Request req, Response resp) {
		Organization before = orgApi.getOrganization(req.getOrgDomain());
		Organization organization = (Organization) PrimitiveConverter.overwrite(before, req.getParams(),
				conf.getParseCallback(req.getOrgDomain()));
		orgApi.updateOrganization(organization);
	}

	@MsgbusMethod
	@MsgbusPermission(group = "dom", code = "config_view")
	public void getOrganizationParameters(Request req, Response resp) {
		String domain = req.getOrgDomain();
		Map<String, Object> params = orgApi.getOrganizationParameters(domain);
		resp.put("result", PrimitiveConverter.serialize(params));
	}

	@MsgbusMethod
	@MsgbusPermission(group = "dom", code = "config_view")
	public void getOrganizationParameter(Request req, Response resp) {
		String domain = req.getOrgDomain();
		String key = req.getString("key");
		Object param = orgApi.getOrganizationParameter(domain, key);
		resp.put("result", PrimitiveConverter.serialize(param));
	}

	@MsgbusMethod
	@MsgbusPermission(group = "dom", code = "config_edit")
	public void setOrganizationParameter(Request req, Response resp) {
		String domain = req.getOrgDomain();
		String key = req.getString("key");
		Object value = req.get("value");
		orgApi.setOrganizationParameter(domain, key, value);
	}

	@MsgbusMethod
	@MsgbusPermission(group = "dom", code = "config_edit")
	public void unsetOrganizationParameter(Request req, Response resp) {
		String domain = req.getOrgDomain();
		String key = req.getString("key");
		orgApi.unsetOrganizationParameter(domain, key);
	}
}
