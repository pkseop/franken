/*
 * Copyright 2011 Future Systems, Inc
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
package kr.co.future.msgbus;

import java.net.InetAddress;
import java.util.Date;
import java.util.Locale;

public interface Session {
	@Deprecated
	int getId();

	String getGuid();

	String getOrgDomain();

	String getAdminLoginName();

	InetAddress getLocalAddress();

	InetAddress getRemoteAddress();

	Locale getLocale();

	boolean has(String key);

	Object get(String key);

	String getString(String key);

	Integer getInt(String key);

	void setProperty(String key, Object value);

	void unsetProperty(String key);

	void send(Message msg);

	void close();
	
	Date getLastAccessTime();
	
	void setLastAccessTime();
}
