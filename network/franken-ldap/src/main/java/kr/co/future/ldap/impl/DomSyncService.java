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
package kr.co.future.ldap.impl;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import kr.co.future.cron.PeriodicJob;
import kr.co.future.dom.api.AdminApi;
import kr.co.future.dom.api.OrganizationUnitApi;
import kr.co.future.dom.api.ProgramApi;
import kr.co.future.dom.api.RoleApi;
import kr.co.future.dom.api.UserApi;
import kr.co.future.dom.model.Admin;
import kr.co.future.dom.model.OrganizationUnit;
import kr.co.future.dom.model.User;
import kr.co.future.ldap.LdapOrgUnit;
import kr.co.future.ldap.LdapProfile;
import kr.co.future.ldap.LdapService;
import kr.co.future.ldap.LdapSyncService;
import kr.co.future.ldap.LdapUser;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import kr.co.future.api.FieldOption;
import kr.co.future.api.PrimitiveConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@PeriodicJob("*/10 * * * *")
@Component(name = "ldap-sync-service")
@Provides
public class DomSyncService implements LdapSyncService, Runnable {

	private static final String EXT_NAME = "ldap";
	private static final String DEFAULT_ROLE_NAME = "admin";
	private static final String DEFAULT_PROFILE_NAME = "all";

	private final Logger logger = LoggerFactory.getLogger(DomSyncService.class);

	@Requires
	private LdapService ldap;

	@Requires
	private UserApi userApi;

	@Requires
	private AdminApi adminApi;

	@Requires
	private OrganizationUnitApi orgUnitApi;

	@Requires
	private RoleApi roleApi;

	@Requires
	private ProgramApi programApi;

	@Override
	public void run() {
		try {
			syncAll();
		} catch (Exception e) {
			logger.error("kraken ldap: sync error", e);
		}
	}
	
	private boolean isSyncTimeNow(String syncTime) {
		Calendar calendar = Calendar.getInstance();
		int currentHour = calendar.get(Calendar.HOUR_OF_DAY), currentMinute = calendar.get(Calendar.MINUTE);
		String[] temp = syncTime.split(":");
		int syncHour = Integer.parseInt(temp[0]), syncMinute = Integer.parseInt(temp[1]);
		if(currentHour == syncHour) {
			int diff = currentMinute - syncMinute;
			if(diff >= 0 && diff < 10)
				return true;
		}
		return false;
	}

	private void syncAll() {
		// NPE at stopping
		if (ldap == null)
			return;

		for (LdapProfile profile : ldap.getProfiles()) {
			String syncTime = profile.getSyncTime();
			if(syncTime == null)
				continue;

			if(isSyncTimeNow(syncTime)) {
				try {
					logger.info("kraken ldap: start scheduled sync using profile [{}]", profile.getName());
					sync(profile);
					logger.info("kraken ldap: scheduled sync using profile [{}] finished!!!", profile.getName());
				} catch (Exception e) {
					logger.error("kraken ldap: periodic sync failed", e);
				}

				ldap.updateProfile(profile);
			}
		}
	}

	@Override
	public void sync(LdapProfile profile) {
		try{
			Collection<LdapOrgUnit> orgUnits = ldap.getOrgUnits(profile);
			Collection<LdapUser> users = ldap.getUsers(profile);
	
			logger.info("ldap info sync to db started!!!");			
			exportDom(orgUnits, users, profile);			
			logger.info("ldap info sync to db finished!!!");
		} catch (Exception e) {
			logger.error("kraken ldap: ldap sync error", e);
		}
	}

	@Override
	public void unsync(LdapProfile profile) {
		Collection<OrganizationUnit> orgUnits = orgUnitApi.getOrganizationUnits("localhost");
		Collection<String> guids = new ArrayList<String>();
		for (OrganizationUnit orgUnit : orgUnits) {
			Map<String, Object> ext = orgUnit.getExt();
			if (ext.containsKey(EXT_NAME)) {
				Map<String, Object> ldap = (Map<String, Object>) ext.get(EXT_NAME);
				if (ldap.get("profile").toString().equals(profile.getName().toString())) {
					logger.debug("kraken-ldap: ext ldap profile name=[{}], ldap profile name=[{}]", ldap.get("profile"),
							profile.getName());
					String guid = orgUnit.getGuid();
					guids.add(guid);
				}
			}
		}
		orgUnitApi.removeOrganizationUnits("localhost", guids);
	}

	@Override
	public void unsyncAll() {
		Collection<OrganizationUnit> orgUnits = orgUnitApi.getOrganizationUnits("localhost");
		Collection<String> guids = new ArrayList<String>();
		for (OrganizationUnit orgUnit : orgUnits) {
			Map<String, Object> ext = orgUnit.getExt();
			if (ext.containsKey(EXT_NAME)) {
				String guid = orgUnit.getGuid();
				guids.add(guid);
			}
		}
		orgUnitApi.removeOrganizationUnits("localhost", guids);
	}

	private void exportDom(Collection<LdapOrgUnit> orgUnits, Collection<LdapUser> users, LdapProfile profile) {
		Map<List<String>, OrganizationUnit> domOrgUnits = exportOrgUnits(profile, orgUnits);
		exportUsers(profile, users, domOrgUnits);
	}

	@SuppressWarnings("unchecked")
	private Map<List<String>, OrganizationUnit> exportOrgUnits(LdapProfile profile, Collection<LdapOrgUnit> orgUnits) {
		String domain = profile.getTargetDomain();
		Map<List<String>, OrganizationUnit> result = new HashMap<List<String>, OrganizationUnit>();

		// names-object map
		Map<List<String>, LdapOrgUnit> orgUnitsMap = new HashMap<List<String>, LdapOrgUnit>();
		for (LdapOrgUnit orgUnit : orgUnits) {
			// only active directory has org unit distinguished name
			String dn = orgUnit.getDistinguishedName();
			if (dn == null)
				continue;
			orgUnitsMap.put(getOUs(dn), orgUnit);
		}

		// sync
		Set<String> removeGuids = new HashSet<String>();
		String profileName = profile.getName();
		if(profileName.length() > 10)	//because db size is 10.
			profileName = profileName.substring(0, 10);
		for (OrganizationUnit ou : orgUnitApi.getOrganizationUnits(domain)) {
			String sourceType = ou.getSourceType();
			if(sourceType != null && sourceType.equals(profileName)) {
				removeGuids.add(ou.getGuid());
			}
		}

		List<OrganizationUnit> create = new ArrayList<OrganizationUnit>();
		Map<String, OrganizationUnit> roots = new HashMap<String, OrganizationUnit>();
		List<OrganizationUnit> update = new ArrayList<OrganizationUnit>();
		for (List<String> names : orgUnitsMap.keySet()) {
			LdapOrgUnit orgUnit = orgUnitsMap.get(names);
			OrganizationUnit domOrgUnit = createOrgUnits(domain, create, roots, names, profileName);
			Map<String, Object> ext = (Map<String, Object>) PrimitiveConverter.serialize(orgUnit);
			ext.put("profile", profile.getName());

			if (!create.contains(domOrgUnit)) {
				domOrgUnit.setSourceType(profileName);
				update.add(domOrgUnit);
			}
			result.put(names, domOrgUnit);
			removeGuids.remove(domOrgUnit.getGuid());
		}

		orgUnitApi.createOrganizationUnits(domain, create);
		orgUnitApi.updateOrganizationUnits(domain, update);
		orgUnitApi.removeOrganizationUnits(domain, removeGuids, true);

		return result;
	}

	private OrganizationUnit createOrgUnits(String domain, List<OrganizationUnit> create, Map<String, OrganizationUnit> roots,
			List<String> names, String profileName) {
		if (names.isEmpty())
			return null;

		ListIterator<String> nameit = names.listIterator(names.size());
		String rootname = nameit.previous();
		OrganizationUnit orgUnit = roots.get(rootname);
		if (orgUnit == null) {
			orgUnit = orgUnitApi.findOrganizationUnitByName(domain, rootname);
			if (orgUnit == null) {
				orgUnit = new OrganizationUnit();
				orgUnit.setName(rootname);
				orgUnit.setSourceType(profileName);
				create.add(orgUnit);
			}
			roots.put(rootname, orgUnit);
		}

		while (nameit.hasPrevious()) {
			String name = nameit.previous();

			OrganizationUnit parent = orgUnit;
			orgUnit = null;
			for (OrganizationUnit child : parent.getChildren()) {
				if (name.equals(child.getName())) {
					orgUnit = child;
					break;
				}
			}

			if (orgUnit == null) {
				orgUnit = new OrganizationUnit();
				orgUnit.setName(name);
				orgUnit.setParent(parent.getGuid());
				orgUnit.setSourceType(profileName);
				create.add(orgUnit);
				parent.getChildren().add(orgUnit);
			}
		}

		return orgUnit;
	}

	private static class DomUserConstraints {
		static public int lenLoginName;
		static public int lenName;
		static public int lenTitle;
		static public int lenEmail;
		static public int lenPhone;
		static {
			try {
				lenLoginName = getLength("loginName");
				lenName = getLength("name");
				lenTitle = getLength("title");
				lenEmail = getLength("email");
				lenPhone = getLength("phone");
			} catch (SecurityException e) {
				LoggerFactory.getLogger(DomSyncService.class).warn("kraken ldap: can't load field length limit", e);
			} catch (NoSuchFieldException e) {
				LoggerFactory.getLogger(DomSyncService.class).warn("kraken ldap: can't load field length limit", e);
			}
		}

		private static int getLength(String string) throws SecurityException, NoSuchFieldException {
			Field declaredField = User.class.getDeclaredField("loginName");
			FieldOption annotation = declaredField.getAnnotation(FieldOption.class);
			if (annotation != null)
				return annotation.length();
			else
				return 0;
		}
	}

	@SuppressWarnings("unchecked")
	private void exportUsers(LdapProfile profile, Collection<LdapUser> users, Map<List<String>, OrganizationUnit> orgUnits) {
		String domain = profile.getTargetDomain();

		// remove removed ldap users
		Set<String> loginNames = new HashSet<String>();
		List<String> remove = new ArrayList<String>();
		Map<String, User> domUsers = new HashMap<String, User>();

		for (LdapUser user : users)
			loginNames.add(user.getAccountName());

		for (User user : userApi.getUsers(domain)) {
			domUsers.put(user.getLoginName(), user);

			Object ext = user.getExt().get("frodo");
			if(ext == null || user.getSourceType() == null || !user.getSourceType().equals("ldap") 
					|| !profile.getName().equals(((Map<String, Object>) ext).get("source_type"))) {
				continue;
			}

			if (!loginNames.contains(user.getLoginName()))
				remove.add(user.getLoginName());
		}

		Admin defaultAdmin = new Admin();
		defaultAdmin.setRole(roleApi.getRole(domain, DEFAULT_ROLE_NAME));
		defaultAdmin.setProfile(programApi.getProgramProfile(domain, DEFAULT_PROFILE_NAME));
		defaultAdmin.setIdleTimeout(3600);
		defaultAdmin.setLoginLockCount(5);
		defaultAdmin.setUseLoginLock(true);
		defaultAdmin.setUseIdleTimeout(true);
		defaultAdmin.setUseOtp(false);
		defaultAdmin.setUseAcl(false);
		defaultAdmin.setEnabled(true);

		// sync
		List<User> create = new ArrayList<User>();
		List<User> update = new ArrayList<User>();
		List<Object[]> failed = new ArrayList<Object[]>();
		for (LdapUser user : users) {
			User domUser = domUsers.get(user.getAccountName());
			boolean exist = (domUser != null);

			Map<String, Object> ext = (Map<String, Object>) PrimitiveConverter.serialize(user);
			ext.put("profile", profile.getName());
			ext.remove("account_name");
			ext.remove("logon_count");
			ext.remove("last_logon");

			boolean basicInfoUpdated = false;
			try {
				if (domUser == null)
					domUser = new User();
//				else if (domUser.getExt() == null || !domUser.getExt().containsKey("frodo")) {
//					logger.trace("kraken ldap: skip local user [{}]", domUser.getLoginName());
//					continue;
//				}
				basicInfoUpdated = updateDomUserFromDomainUser(orgUnits, user, domUser);
			} catch (Exception e) {
				logger.trace("kraken ldap: update failed", e);
				failed.add(new Object[] { user, e });
				continue;
			}

			Map<String, Object> domUserExt = domUser.getExt();
			if (domUserExt == null)
				domUserExt = new HashMap<String, Object>();
			domUserExt.put(EXT_NAME, ext);
			if (user.isDomainAdmin() && !domUserExt.containsKey(adminApi.getExtensionName()))
				domUserExt.put(adminApi.getExtensionName(), defaultAdmin);
			
			boolean userExtInfoUpdated = setLdapProfileNameToUserExt(domUserExt, profile.getName());

			boolean equals = domUserExt.equals(domUser.getExt());

			domUser.setExt(domUserExt);

			if (!exist) {
				create.add(domUser);
				logger.trace("kraken ldap: dom user [{}] will be created", domUser.getLoginName());
			} else {
				if (!equals || basicInfoUpdated || userExtInfoUpdated) {
					update.add(domUser);
					logger.trace("kraken ldap: dom user [{}] will be updated", domUser.getLoginName());
				}
			}
		}

		if (!failed.isEmpty()) {
			int reportId = new Object().hashCode();
			logger.trace(
					"kraken ldap: Importing some accounts failed and ignored while syncing in DomSyncService. failure report identifier: {}",
					reportId);
			for (Object[] f : failed) {
				LdapUser acc = (LdapUser) f[0];
				Exception e = (Exception) f[1];
				logger.trace("kraken ldap: {}: {}", reportId, String.format("%s: %s", acc.getDistinguishedName(), e.toString()));
			}
		}

		userApi.createUsers(domain, create);
		
		for (User updateUser : update) {
			userApi.updateUser(domain, updateUser, false);
		}
		
		userApi.removeUsers(domain, remove);
	}

	private static Pattern p = Pattern.compile("OU=(.*?),");

	private List<String> getOUs(String dn) {
		Matcher m = p.matcher(dn);
		List<String> ous = new ArrayList<String>();
		while (m.find())
			ous.add(m.group(1));
		return ous;
	}

	/**
	 * @return true if user data updated
	 */
	private boolean updateDomUserFromDomainUser(Map<List<String>, OrganizationUnit> orgUnits, LdapUser user, User domUser) {
		boolean updated = false;

		domUser.setSourceType("ldap");
		if (domUser.getLoginName() == null || !domUser.getLoginName().equals(user.getAccountName())) {
			domUser.setLoginName(user.getAccountName());
			updated = true;
		}
		List<String> ou = getOUs(user.getDistinguishedName());

		String ouguid = orgUnits.containsKey(ou) ? orgUnits.get(ou).getGuid() : null;

		if (domUser.getOrgUnit() == null || !domUser.getOrgUnit().getGuid().equals(ouguid)) {
			if (ouguid != null) {

				domUser.setOrgUnit(orgUnits.get(ou));
				updated = true;
			}
		}
		if (domUser.getName() == null || !domUser.getName().equals(user.getDisplayName())) {
			domUser.setName(user.getDisplayName());
			updated = true;
		}
		if (domUser.getTitle() == null || !domUser.getTitle().equals(user.getTitle())) {
			if (user.getTitle() != null) {
				if (DomUserConstraints.lenTitle < user.getTitle().length())
					logger.trace("kraken ldap: title longer than {}: {}", DomUserConstraints.lenTitle, domUser.getTitle());
				else {
					domUser.setTitle(user.getTitle());
					updated = true;
				}
			}
		}
		if (domUser.getEmail() == null || !domUser.getEmail().equals(user.getMail())) {
			if (user.getMail() != null) {
				if (DomUserConstraints.lenEmail < user.getMail().length())
					logger.trace("kraken ldap: email longer than {}: {}", DomUserConstraints.lenEmail, domUser.getEmail());
				else {
					domUser.setEmail(user.getMail());
					updated = true;
				}
			}
		}
		if (domUser.getPhone() == null || !domUser.getPhone().equals(user.getMobile())) {
			if (user.getMobile() != null) {
				if (DomUserConstraints.lenPhone < user.getMobile().length())
					logger.trace("kraken ldap: phone longer than {}: {}", DomUserConstraints.lenPhone, domUser.getPhone());
				else {
					domUser.setPhone(user.getMobile());
					updated = true;
				}
			}
		}

		if (updated) {
			checkConstraints(domUser);
			return true;
		} else {
			return false;
		}
	}

	private void checkConstraints(User domUser) {
		if (domUser.getLoginName() != null && DomUserConstraints.lenLoginName < domUser.getLoginName().length())
			throw new IllegalArgumentException(String.format("getLoginName longer than %d: %s", DomUserConstraints.lenLoginName,
					domUser.getLoginName()));
		if (domUser.getName() != null && DomUserConstraints.lenName < domUser.getName().length())
			throw new IllegalArgumentException(String.format("getName longer than %d: %s", DomUserConstraints.lenName,
					domUser.getName()));
	}
	
	private boolean setLdapProfileNameToUserExt(Map<String, Object> domUserExt, String profileName) {
		//to recognize ldap source from multiple ldap profiles.         
      HashMap<String, Object> frodo = (HashMap<String, Object>)domUserExt.get("frodo");
      if(frodo == null) {
      	frodo = new HashMap<String, Object>();
      	Date now = new Date();
			frodo.put("cid", UUID.randomUUID().toString());
			frodo.put("created_at", now);
			frodo.put("updated_at", now);
      }
      if(!profileName.equals(frodo.get("source_type"))) {
      	frodo.put("source_type", profileName);
      	domUserExt.put("frodo", frodo);
      	return true;
      }
      return false;
	}
}