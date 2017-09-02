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
package kr.co.future.dom.api;

import java.util.Map;

import kr.co.future.msgbus.MsgbusException;

public class DOMException extends MsgbusException {
	private static final long serialVersionUID = 1L;

	public DOMException(String errorCode) {
		this(errorCode, null);
	}

	public DOMException(String errorCode, Map<String, Object> parameters) {
		super("dom", errorCode, parameters);
	}
}
