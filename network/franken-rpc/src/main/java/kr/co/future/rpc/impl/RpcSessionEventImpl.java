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
package kr.co.future.rpc.impl;

import java.util.Properties;

import kr.co.future.rpc.RpcSession;
import kr.co.future.rpc.RpcSessionEvent;

public class RpcSessionEventImpl implements RpcSessionEvent {
	private int type;
	private RpcSession session;
	private Properties props;

	public RpcSessionEventImpl(int type, RpcSession session) {
		this(type, session, new Properties());
	}

	public RpcSessionEventImpl(int type, RpcSession session, Properties props) {
		this.session = session;
		this.props = props;
	}

	@Override
	public int getType() {
		return type;
	}

	@Override
	public RpcSession getSession() {
		return session;
	}

	@Override
	public Object getParameter(String key) {
		return props.get(key);
	}

}
