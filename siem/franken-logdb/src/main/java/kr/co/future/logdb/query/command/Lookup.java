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
package kr.co.future.logdb.query.command;

import kr.co.future.logdb.LogQueryCommand;
import kr.co.future.logdb.LookupHandler;
import kr.co.future.logdb.LookupHandlerRegistry;

public class Lookup extends LogQueryCommand {
	private LookupHandlerRegistry registry;
	private String handlerName;
	private String srcField;
	private String localSrcField;
	private String dstField;
	private String localDstField;

	public Lookup(String handlerName, String srcField, String dstField) {
		this(handlerName, srcField, srcField, dstField, dstField);
	}

	public Lookup(String handlerName, String localSrcField, String srcField, String dstField, String localDstField) {
		this.handlerName = handlerName;
		this.srcField = srcField;
		this.localSrcField = localSrcField;
		this.dstField = dstField;
		this.localDstField = localDstField;
	}

	public void setLogQueryService(LookupHandlerRegistry registry) {
		this.registry = registry;
	}

	@Override
	public void push(LogMap m) {
		Object value = m.get(localSrcField);
		LookupHandler handler = registry.getLookupHandler(handlerName);
		if (handler != null)
			m.put(localDstField, handler.lookup(srcField, dstField, value));
		write(m);
	}

	@Override
	public boolean isReducer() {
		return false;
	}
}
