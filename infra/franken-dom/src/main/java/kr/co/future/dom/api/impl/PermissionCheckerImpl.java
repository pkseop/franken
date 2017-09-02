/*
 * Copyright 2011 Future Systems, Inc.
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
package kr.co.future.dom.api.impl;

import kr.co.future.dom.api.RoleApi;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import kr.co.future.dom.api.impl.PermissionCheckerImpl;
import kr.co.future.msgbus.PermissionChecker;
import kr.co.future.msgbus.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(name = "dom-permission-checker")
@Provides
public class PermissionCheckerImpl implements PermissionChecker {
	private final Logger logger = LoggerFactory.getLogger(PermissionCheckerImpl.class);

	@Requires
	private RoleApi roleApi;

	@Override
	public boolean check(Session session, String group, String permission) {
		logger.trace("kraken dom: checking permission, session [{}], group [{}], code [{}]", new Object[] { session, group, permission });
		return roleApi.hasPermission(session.getOrgDomain(), session.getAdminLoginName(), group, permission);
	}
}
