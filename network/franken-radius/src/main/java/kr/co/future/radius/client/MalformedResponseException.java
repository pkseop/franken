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
package kr.co.future.radius.client;

import kr.co.future.radius.protocol.RadiusPacket;
import kr.co.future.radius.protocol.RadiusResponse;

public class MalformedResponseException extends RuntimeException {
	private static final long serialVersionUID = 1L;

	private RadiusPacket req;
	private RadiusResponse resp;

	public MalformedResponseException(RadiusPacket req, RadiusResponse resp) {
		this.req = req;
		this.resp = resp;
	}

	public RadiusPacket getRequest() {
		return req;
	}

	public RadiusResponse getResponse() {
		return resp;
	}
}
