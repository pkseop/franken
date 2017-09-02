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
package kr.co.future.ipojo.impl;

import kr.co.future.ipojo.ComponentFactoryMonitor;
import kr.co.future.ipojo.IPojoScript;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.apache.felix.ipojo.annotations.ServiceProperty;
import kr.co.future.api.Script;
import kr.co.future.api.ScriptFactory;
import org.osgi.framework.BundleContext;

/**
 * Script factory for iPOJO
 * 
 * @author xeraph
 * 
 */
@Component(name = "ipojo-script-factory")
@Provides
public class IPojoScriptFactory implements ScriptFactory {

	private BundleContext bc;

	@SuppressWarnings("unused")
	@ServiceProperty(name = "alias", value = "ipojo")
	private String alias;

	@Requires
	private ComponentFactoryMonitor tracker;

	public IPojoScriptFactory(BundleContext bc) {
		this.bc = bc;
	}

	@Override
	public Script createScript() {
		return new IPojoScript(bc, tracker);
	}

}
