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
package kr.co.future.radius.protocol;

public class NasPortAttribute extends RadiusAttribute {
	private int port;

	public NasPortAttribute(int port) {
		this.port = port;
	}

	public NasPortAttribute(byte[] encoded, int offset, int length) {
		if (encoded[offset] != getType())
			throw new IllegalArgumentException("binary is not nas port attribute");

		this.port = decodeInt(encoded, offset, length);
	}

	@Override
	public int getType() {
		return 5;
	}

	public int getPort() {
		return port;
	}

	@Override
	public byte[] getBytes() {
		return encodeInt(getType(), port);
	}
}
