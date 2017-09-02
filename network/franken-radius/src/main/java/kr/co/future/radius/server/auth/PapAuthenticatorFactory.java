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
package kr.co.future.radius.server.auth;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import kr.co.future.radius.server.RadiusAuthType;
import kr.co.future.radius.server.RadiusAuthenticator;
import kr.co.future.radius.server.RadiusAuthenticatorFactory;
import kr.co.future.radius.server.RadiusConfigMetadata;
import kr.co.future.radius.server.RadiusInstanceConfig;
import kr.co.future.radius.server.RadiusModuleType;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Provides;

@Component(name = "radius-papauth-factory")
@Provides
public class PapAuthenticatorFactory implements RadiusAuthenticatorFactory {

	@Override
	public String getName() {
		return "pap";
	}

	@Override
	public RadiusModuleType getModuleType() {
		return RadiusModuleType.Authenticator;
	}

	@Override
	public Set<RadiusAuthType> getSupportedAuthTypes() {
		return new TreeSet<RadiusAuthType>(Arrays.asList(RadiusAuthType.PAP));
	}

	@Override
	public List<RadiusConfigMetadata> getConfigMetadatas() {
		return new ArrayList<RadiusConfigMetadata>();
	}

	@Override
	public RadiusAuthenticator newInstance(RadiusInstanceConfig config) {
		return new PapAuthenticator(config.getName(), this);
	}

	@Override
	public String toString() {
		return "PAP authenticatior factory";
	}
}
