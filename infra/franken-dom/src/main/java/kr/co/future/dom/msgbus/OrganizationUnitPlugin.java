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
import java.util.List;

import kr.co.future.dom.api.ConfigManager;
import kr.co.future.dom.api.OrganizationUnitApi;
import kr.co.future.dom.model.OrganizationUnit;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Requires;
import kr.co.future.api.PrimitiveConverter;
import kr.co.future.msgbus.Request;
import kr.co.future.msgbus.Response;
import kr.co.future.msgbus.handler.MsgbusMethod;
import kr.co.future.msgbus.handler.MsgbusPermission;
import kr.co.future.msgbus.handler.MsgbusPlugin;

@Component(name = "dom-org-unit-plugin")
@MsgbusPlugin
public class OrganizationUnitPlugin {
	@Requires
	private ConfigManager conf;

	@Requires
	private OrganizationUnitApi orgUnitApi;

	@MsgbusMethod
	@MsgbusPermission(group = "dom", code = "user_edit")
	public void removeAllOrganizationUnits(Request req, Response resp) {
		String domain = req.getOrgDomain();
		List<String> guids = new ArrayList<String>();
		for (OrganizationUnit u : orgUnitApi.getOrganizationUnits(domain)) {
			guids.add(u.getGuid());
		}

		orgUnitApi.removeOrganizationUnits(domain, guids, true);
	}

	@MsgbusMethod
	public void getOrganizationUnits(Request req, Response resp) {
		boolean includeChildren = false;
		if (req.has("include_children"))
			includeChildren = req.getBoolean("include_children");
		Collection<OrganizationUnit> orgUnits = orgUnitApi.getOrganizationUnits(req.getOrgDomain(), includeChildren);
		resp.put("org_units", PrimitiveConverter.serialize(orgUnits));
	}

	@MsgbusMethod
	public void getOrganizationUnit(Request req, Response resp) {
		String guid = req.getString("guid");
		OrganizationUnit orgUnit = orgUnitApi.getOrganizationUnit(req.getOrgDomain(), guid);
		resp.put("org_unit", PrimitiveConverter.serialize(orgUnit));
	}

	@MsgbusMethod
	@MsgbusPermission(group = "dom", code = "user_edit")
	public void createOrganizationUnit(Request req, Response resp) {
		OrganizationUnit orgUnit = (OrganizationUnit) PrimitiveConverter.overwrite(new OrganizationUnit(), req.getParams(),
				conf.getParseCallback(req.getOrgDomain()));
		orgUnitApi.createOrganizationUnit(req.getOrgDomain(), orgUnit);
		resp.put("guid", orgUnit.getGuid());
	}

	@MsgbusMethod
	@MsgbusPermission(group = "dom", code = "user_edit")
	public void updateOrganizationUnit(Request req, Response resp) {
		OrganizationUnit orgUnit = (OrganizationUnit) PrimitiveConverter.overwrite(new OrganizationUnit(), req.getParams(),
				conf.getParseCallback(req.getOrgDomain()));
		orgUnitApi.updateOrganizationUnit(req.getOrgDomain(), orgUnit);
	}

	@MsgbusMethod
	@MsgbusPermission(group = "dom", code = "user_edit")
	public void removeOrganizationUnit(Request req, Response resp) {
		String guid = req.getString("guid");
		orgUnitApi.removeOrganizationUnit(req.getOrgDomain(), guid);
	}
}
