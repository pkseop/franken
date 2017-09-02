/*
 * Copyright 2011 Future Systems, Inc
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
package kr.co.future.msgbus.impl;

import kr.co.future.msgbus.MessageBus;
import kr.co.future.msgbus.ResourceApi;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.apache.felix.ipojo.annotations.ServiceProperty;
import kr.co.future.api.Script;
import kr.co.future.api.ScriptFactory;

@Component(name = "msgbus-script-factory")
@Provides
public class MsgbusScriptFactory implements ScriptFactory {
	@SuppressWarnings("unused")
	@ServiceProperty(name = "alias", value = "msgbus")
	private String alias;

	@Requires
	private MessageBus msgbus;
	
	@Requires
	private ResourceApi resourceApi;

	@Override
	public Script createScript() {
		return new MsgbusScript(msgbus, resourceApi);
	}

}
