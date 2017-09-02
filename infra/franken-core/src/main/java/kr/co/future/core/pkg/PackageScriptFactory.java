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
package kr.co.future.core.pkg;

import kr.co.future.api.PackageManager;
import kr.co.future.api.Script;
import kr.co.future.api.ScriptFactory;
import kr.co.future.core.main.Kraken;

import org.osgi.framework.BundleContext;
import org.osgi.service.prefs.BackingStoreException;

public class PackageScriptFactory implements ScriptFactory {
	private BundleContext bc;
	private PackageManagerService packageManager;

	public PackageScriptFactory() throws BackingStoreException {
		this.bc = Kraken.getContext();
		this.packageManager = new PackageManagerService(Kraken.getContext());
		bc.registerService(PackageManager.class.getName(), packageManager, null);
	}

	@Override
	public Script createScript() {
		return new PackageScript(bc, packageManager);
	}

}
