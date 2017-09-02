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

public class ClassAttribute extends RadiusAttribute {

	private byte[] data;
	
	public ClassAttribute(byte[] data) {
		this.data = data;
	}
	
	public ClassAttribute(byte[] encoded, int offset, int length) {
		if (encoded[offset] != getType())
			throw new IllegalArgumentException("binary is not class attribute");
		
		this.data = decodeString(encoded, offset, length);
	}
	
	@Override
	public int getType() {
		return 25;
	}

	public byte[] getData() {
		return data;
	}
	
	@Override
	public byte[] getBytes() {
		return encodeString(getType(), data);
	}
}
