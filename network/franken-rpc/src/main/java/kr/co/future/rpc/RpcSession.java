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


public interface RpcSession {
	RpcSessionState getState();

	int getId();

	RpcConnection getConnection();

	String getServiceName();

	Object getProperty(String name);

	void setProperty(String name, Object value);

	RpcAsyncResult call(String method, Object[] params, RpcAsyncCallback callback);

	Object call(String method, Object... params) throws RpcException, InterruptedException;

	Object call(String method, Object[] params, long timeout) throws RpcException, InterruptedException;

	void post(String method, Object... params);

	void close();

	void addListener(RpcSessionEventCallback callback);

	void removeListener(RpcSessionEventCallback callback);
}