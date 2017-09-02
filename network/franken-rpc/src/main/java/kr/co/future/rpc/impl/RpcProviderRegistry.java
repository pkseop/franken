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
package kr.co.future.rpc.impl;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import kr.co.future.rpc.RpcService;

public class RpcProviderRegistry {
	private Map<String, RpcService> providerMap;

	public RpcProviderRegistry() {
		providerMap = new ConcurrentHashMap<String, RpcService>();
	}
	
	public RpcService find(String name) {
		return providerMap.get(name);
	}
	
	public void register(String name, RpcService provider) {
		if (providerMap.containsKey(name))
			throw new IllegalStateException("duplicated session name");
		
		providerMap.put(name, provider);
	}
	
	public void unregister(String name) {
		providerMap.remove(name);
	}
}
