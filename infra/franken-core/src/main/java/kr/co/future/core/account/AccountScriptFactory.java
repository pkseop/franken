/*
 * Copyright 2010 NCHOVY
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
package kr.co.future.core.account;

import java.util.Properties;

import kr.co.future.api.AccountManager;
import kr.co.future.api.Script;
import kr.co.future.api.ScriptFactory;
import kr.co.future.auth.api.AuthProvider;
import kr.co.future.confdb.ConfigService;
import org.osgi.framework.BundleContext;

public class AccountScriptFactory implements ScriptFactory {
	private AccountManager manager;

	public AccountScriptFactory(BundleContext bc, ConfigService conf) {
		manager = new AccountManagerImpl(conf);
		bc.registerService(AuthProvider.class.getName(), manager, new Properties());
		bc.registerService(AccountManager.class.getName(), manager, new Properties());
	}

	@Override
	public Script createScript() {
		return new AccountScript(manager);
	}
}
