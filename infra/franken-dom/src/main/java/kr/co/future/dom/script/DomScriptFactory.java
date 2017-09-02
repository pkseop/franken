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
package kr.co.future.dom.script;

import kr.co.future.api.Script;
import kr.co.future.api.ScriptFactory;
import kr.co.future.confdb.ConfigService;
import kr.co.future.dom.api.ApplicationApi;
import kr.co.future.dom.api.AreaApi;
import kr.co.future.dom.api.GlobalConfigApi;
import kr.co.future.dom.api.HostApi;
import kr.co.future.dom.api.HostUpdateApi;
import kr.co.future.dom.api.OrganizationApi;
import kr.co.future.dom.api.OrganizationUnitApi;
import kr.co.future.dom.api.ProgramApi;
import kr.co.future.dom.api.RoleApi;
import kr.co.future.dom.api.UserApi;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.apache.felix.ipojo.annotations.ServiceProperty;

@Component(name = "dom-script-factory")
@Provides
public class DomScriptFactory implements ScriptFactory {
	@SuppressWarnings("unused")
	@ServiceProperty(name = "alias", value = "dom")
	private String alias;

	@Requires
	private GlobalConfigApi globalConfigApi;

	@Requires
	private OrganizationApi orgApi;

	@Requires
	private OrganizationUnitApi orgUnitApi;

	@Requires
	private UserApi userApi;

	@Requires
	private RoleApi roleApi;

	@Requires
	private ProgramApi programApi;

	@Requires
	private AreaApi areaApi;

	@Requires
	private HostApi hostApi;

	@Requires
	private ApplicationApi appApi;

	@Requires
	private HostUpdateApi updateApi;

	@Requires
	private ConfigService conf;

	@Override
	public Script createScript() {
		return new DomScript(globalConfigApi, orgApi, orgUnitApi, userApi, roleApi, programApi, areaApi, hostApi, appApi,
				updateApi, conf);
	}
}
