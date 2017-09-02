/*
 * Copyright 2013 Future Systems
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
package kr.co.future.logstorage.engine;

import java.nio.ByteBuffer;
import java.util.Map;

import kr.co.future.logstorage.Log;
import kr.co.future.logstorage.file.LogRecord;

import kr.co.future.codec.EncodingRule;

/**
 * @since 0.9
 * @author xeraph
 */
class LogMarshaler {
	private LogMarshaler() {
	}

	public static Log convert(String tableName, LogRecord logdata) {
		ByteBuffer bb = ByteBuffer.wrap(logdata.getData().array());
		bb.position(logdata.getData().position());
		bb.limit(logdata.getData().limit());
		Map<String, Object> m = EncodingRule.decodeMap(bb);
		return new Log(tableName, logdata.getDate(), logdata.getId(), m);
	}
}
