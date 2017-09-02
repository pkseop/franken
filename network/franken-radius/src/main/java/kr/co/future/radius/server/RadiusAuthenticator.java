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
package kr.co.future.radius.server;

import java.util.List;

import kr.co.future.radius.protocol.AccessRequest;
import kr.co.future.radius.protocol.RadiusResponse;

public abstract class RadiusAuthenticator extends RadiusInstance {
	public RadiusAuthenticator(String name, RadiusFactory<?> factory) {
		super(name, factory);
	}

	public abstract RadiusResponse authenticate(RadiusProfile profile, AccessRequest req,
			List<RadiusUserDatabase> userDatabases);
}
