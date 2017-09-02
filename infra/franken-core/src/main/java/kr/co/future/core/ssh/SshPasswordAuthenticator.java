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
package kr.co.future.core.ssh;

import org.apache.sshd.server.PasswordAuthenticator;
import org.apache.sshd.server.session.ServerSession;

import kr.co.future.api.AccountManager;
import kr.co.future.core.main.Kraken;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

public class SshPasswordAuthenticator implements PasswordAuthenticator {
	@Override
	public boolean authenticate(String username, String password, ServerSession session) {
		BundleContext bc = Kraken.getContext();
		ServiceReference ref = bc.getServiceReference(AccountManager.class.getName());
		AccountManager manager = (AccountManager) bc.getService(ref);
		return manager.verifyPassword(username, password);
	}
}
