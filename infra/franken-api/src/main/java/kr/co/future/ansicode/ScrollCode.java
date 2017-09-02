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
package kr.co.future.ansicode;

public class ScrollCode extends AnsiEscapeCode {
	private boolean up;
	private int lines;

	public ScrollCode(boolean up) {
		this(up, 1);
	}

	public ScrollCode(boolean up, int lines) {
		this.up = up;
		this.lines = lines;
	}

	@Override
	public byte[] toByteArray() {
		if (up)
			return wrapCSI(lines + "S");
		else
			return wrapCSI(lines + "T");
	}
}
