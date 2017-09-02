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
package kr.co.future.rpc;

import java.util.Collection;

public interface RpcAgent {
	String getGuid();

	Collection<RpcBindingProperties> getBindings();

	void open(RpcBindingProperties props);

	void close(RpcBindingProperties props);

	RpcConnection connect(RpcConnectionProperties props);

	RpcConnection connectSsl(RpcConnectionProperties props);

	Collection<RpcConnection> getConnections();

	RpcConnection findConnection(int id);

	RpcPeerRegistry getPeerRegistry();

	void addConnectionListener(RpcConnectionEventListener listener);

	void removeConnectionListener(RpcConnectionEventListener listener);
}
