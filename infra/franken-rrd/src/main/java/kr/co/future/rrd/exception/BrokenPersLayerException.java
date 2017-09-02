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
package kr.co.future.rrd.exception;

public class BrokenPersLayerException extends RuntimeException {
	private static final long serialVersionUID = -2709984888453852489L;

	public BrokenPersLayerException() {
	}

	public BrokenPersLayerException(Throwable cause) {
		super(cause);
	}
}
