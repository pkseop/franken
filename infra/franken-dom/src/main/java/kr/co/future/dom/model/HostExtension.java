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
package kr.co.future.dom.model;

import java.util.HashMap;
import java.util.Map;

import kr.co.future.api.FieldOption;

public class HostExtension {
	@FieldOption(nullable = false, length = 255)
	private String type;

	private Map<String, Object> configs = new HashMap<String, Object>();
	private int ord;

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public Map<String, Object> getConfigs() {
		return configs;
	}

	public void setConfigs(Map<String, Object> configs) {
		this.configs = configs;
	}

	public int getOrd() {
		return ord;
	}

	public void setOrd(int ord) {
		this.ord = ord;
	}

	@Override
	public String toString() {
		return "[" + ord + "] " + type;
	}

}
