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

public class MoveToCode extends AnsiEscapeCode {
	private int rowNumber;
	private int columnNumber;

	public MoveToCode(int columnNumber) {
		this.columnNumber = columnNumber;
	}

	public MoveToCode(int rowNumber, int columnNumber) {
		this.rowNumber = rowNumber;
		this.columnNumber = columnNumber;
	}

	@Override
	public byte[] toByteArray() {
		if (rowNumber != 0) {
			return wrapCSI(columnNumber + ";" + rowNumber + "H");
		} else {
			return wrapCSI(columnNumber + "G");
		}
	}
}
