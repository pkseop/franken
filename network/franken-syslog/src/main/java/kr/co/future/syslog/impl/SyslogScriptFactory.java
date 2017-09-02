/*
 * Copyright 2009 NCHOVY
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
package kr.co.future.syslog.impl;

import kr.co.future.syslog.SyslogServerRegistry;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.apache.felix.ipojo.annotations.ServiceProperty;
import kr.co.future.api.Script;
import kr.co.future.api.ScriptFactory;

@Component(name = "syslog-script-factory")
@Provides
public class SyslogScriptFactory implements ScriptFactory {

	@SuppressWarnings("unused")
	@ServiceProperty(name = "alias", value = "syslog")
	private String alias;

	@Requires
	private SyslogServerRegistry syslogRegistry;

	@Override
	public Script createScript() {
		return new SyslogScript(syslogRegistry);
	}

}
