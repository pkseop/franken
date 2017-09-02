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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Collection;

import kr.co.future.ldap.LdapConfig;
import kr.co.future.ldap.LdapOrgUnit;
import kr.co.future.ldap.LdapProfile;
import kr.co.future.ldap.LdapServerType;
import kr.co.future.ldap.LdapService;
import kr.co.future.ldap.LdapSyncService;
import kr.co.future.ldap.LdapUser;

import org.apache.felix.ipojo.annotations.Requires;
import kr.co.future.api.Script;
import kr.co.future.api.ScriptArgument;
import kr.co.future.api.ScriptContext;
import kr.co.future.api.ScriptUsage;
import kr.co.future.confdb.Config;
import kr.co.future.confdb.ConfigDatabase;
import kr.co.future.confdb.ConfigService;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.unboundid.ldap.sdk.LDAPException;

public class LdapScript implements Script {
	private final Logger logger = LoggerFactory.getLogger(LdapScript.class.getName());
	private BundleContext bc;
	private LdapService ldap;
	private ScriptContext context;
	private ConfigService conf;

	public LdapScript(BundleContext bc, LdapService ldap, ConfigService conf) {
		this.bc = bc;
		this.ldap = ldap;
		this.conf = conf;
	}

	@Override
	public void setScriptContext(ScriptContext context) {
		this.context = context;
	}

	public void profiles(String[] args) {
		context.println("LDAP Profiles");
		context.println("-----------------");
		for (LdapProfile profile : ldap.getProfiles()) {
			context.println(profile.toString());
		}
	}

	@ScriptUsage(description = "create ldap profile", arguments = {
			@ScriptArgument(name = "name", type = "string", description = "ldap profile name"),
			@ScriptArgument(name = "address", type = "string", description = "ip address or domain name of ldap server"),
			@ScriptArgument(name = "port", type = "int", description = "port number of ldap server"),
			@ScriptArgument(name = "account", type = "string", description = "admin account name for simple bind (e.g. OFFICE\\xeraph"),
			@ScriptArgument(name = "password", type = "string", description = "admin password"),
			@ScriptArgument(name = "server type", type = "string", description = "ActiveDirectory or SunOneDirectory", optional = true),
			@ScriptArgument(name = "base dn", type = "string", description = "LDAP base DN", optional = true),
			@ScriptArgument(name = "truststore path", type = "string", description = "truststore file path", optional = true) })
	public void createProfile(String[] args) {
		FileInputStream is = null;
		try {
			LdapProfile profile = new LdapProfile();
			profile.setName(args[0]);
			profile.setDc(args[1]);
			profile.setPort(Integer.valueOf(args[2]));
			profile.setAccount(args[3]);
			profile.setPassword(args[4]);
			if (args.length > 5)
				profile.setServerType(LdapServerType.valueOf(args[5]));
			if (args.length > 6)
				profile.setBaseDn(args[6]);
			if (args.length > 7) {
				File file = new File(args[7]);
				if (!file.exists())
					throw new IllegalArgumentException("file not found");

				is = new FileInputStream(file);
				profile.setX509Certificate(is);
			}

			ldap.createProfile(profile);
			context.println("created");
		} catch (Exception e) {
			context.println(e.getMessage());
		} finally {
			if (is != null)
				try {
					is.close();
				} catch (IOException e) {
				}
		}
	}

	@ScriptUsage(description = "remove ldap profile", arguments = { @ScriptArgument(name = "name", type = "string", description = "ldap profile name") })
	public void removeProfile(String[] args) {
		String name = args[0];
		ldap.removeProfile(name);
		context.println("removed");
	}

	@ScriptUsage(description = "print all users", arguments = { @ScriptArgument(name = "profile name", type = "string", description = "profile name") })
	public void users(String[] args) {
		String profileName = args[0];
		LdapProfile profile = ldap.getProfile(profileName);
		if (profile == null) {
			context.println("profile not found.");
			return;
		}

		Collection<LdapUser> accounts = ldap.getUsers(profile);
		if (accounts == null) {
			context.println("domain users not found");
			return;
		}

		context.println("Users");
		context.println("-------");
		for (LdapUser account : accounts)
			context.println(account.toString());

		context.println("total " + accounts.size() + " users");
	}

	@ScriptUsage(description = "search by user", arguments = {
			@ScriptArgument(name = "profile name", type = "string", description = "profile name"),
			@ScriptArgument(name = "account", type = "string", description = "account name") })
	public void searchUser(String[] args) {
		String profileName = args[0];
		LdapProfile profile = ldap.getProfile(profileName);
		if (profile == null) {
			context.println("profile not found.");
			return;
		}

		LdapUser account = ldap.findUser(profile, args[1]);
		if (account == null) {
			context.println("account not found");
		} else {
			context.println(account);
		}
	}

	@ScriptUsage(description = "print all organization units", arguments = { @ScriptArgument(name = "profile name", type = "string", description = "profile name") })
	public void orgUnits(String[] args) {
		String profileName = args[0];
		LdapProfile profile = ldap.getProfile(profileName);
		if (profile == null) {
			context.println("profile not found.");
			return;
		}

		Collection<LdapOrgUnit> ous = ldap.getOrgUnits(profile);
		if (ous == null) {
			context.println("organization units not found");
			return;
		}

		context.println("Organization Units");
		context.println("--------------------");
		for (LdapOrgUnit ou : ous)
			context.println(ou);
	}

	@ScriptUsage(description = "print all domain users", arguments = {
			@ScriptArgument(name = "profile name", type = "string", description = "profile name"),
			@ScriptArgument(name = "account", type = "string", description = "account name without domain prefix (e.g. xeraph)"),
			@ScriptArgument(name = "password", type = "string", description = "test password") })
	public void verifyPassword(String[] args) {
		String profileName = args[0];
		String account = args[1];
		String password = args[2];

		LdapProfile profile = ldap.getProfile(profileName);
		if (profile == null) {
			context.println("profile not found.");
			return;
		}

		boolean success = ldap.verifyPassword(profile, account, password);
		if (success)
			context.println("valid password");
		else
			context.println("invalid password");
	}

	@ScriptUsage(description = "change password", arguments = {
			@ScriptArgument(name = "profile name", type = "string", description = "profile name"),
			@ScriptArgument(name = "account", type = "string", description = "account name without domain prefix (e.g. xeraph)"),
			@ScriptArgument(name = "password", type = "string", description = "new password") })
	public void changePassword(String[] args) {
		String profileName = args[0];
		String account = args[1];
		String newPassword = args[2];

		LdapProfile profile = ldap.getProfile(profileName);

		if (profile == null) {
			context.println("profile not found.");
			return;
		}

		try {
			ldap.changePassword(profile, account, newPassword);
			context.println("password changed");
		} catch (Throwable t) {
			context.println("cannot change password, " + t.getMessage());
			logger.error("cannot change ldap password", t);
		}
	}

	@ScriptUsage(description = "sync all organization units with kraken-dom", arguments = { @ScriptArgument(name = "profile name", type = "string", description = "profile name") })
	public void sync(String[] args) {
		LdapSyncService ldapSync = getSyncService();
		if (ldapSync == null) {
			context.println("kraken-dom not found");
			return;
		}

		String profileName = args[0];
		LdapProfile profile = ldap.getProfile(profileName);

		if (profile == null) {
			context.println("ldap profile not found");
			return;
		}

		ldapSync.sync(profile);
		context.println("sync success");
	}

	@ScriptUsage(description = "unsync all organization units with kraken-dom", arguments = { @ScriptArgument(name = "profile name", type = "string", description = "profile name") })
	public void unsync(String[] args) {
		LdapSyncService ldapSync = getSyncService();
		if (ldapSync == null) {
			context.println("kraken-dom not found");
			return;
		}

		String profileName = args[0];
		LdapProfile profile = ldap.getProfile(profileName);

		if (profile == null) {
			context.println("ldap profile not found");
			return;
		}

		ldapSync.unsync(profile);
		context.println("unsync success");
	}

	@ScriptUsage(description = "unsync all organization units with kraken-dom")
	public void unsyncAll(String[] args) {
		LdapSyncService ldapSync = getSyncService();
		if (ldapSync == null) {
			context.println("kraken-dom not found");
			return;
		}

		ldapSync.unsyncAll();
		context.println("unsync success");
	}

	@ScriptUsage(description = "set id attribute name. if id attribute name is not passed, it will be unset", arguments = {
			@ScriptArgument(name = "profile name", type = "string", description = "profile name"),
			@ScriptArgument(name = "id attribute name", type = "string", description = "id attribute name (e.g. uid)", optional = true) })
	public void setIdAttr(String[] args) {
		LdapProfile p = ldap.getProfile(args[0]);
		if (p == null) {
			context.println("profile not found");
			return;
		}

		if (args.length > 1)
			p.setIdAttr(args[1]);
		else
			p.setIdAttr(null);

		ldap.updateProfile(p);
		context.println("set");
	}

	@ScriptUsage(description = "set sync time", arguments = {
			@ScriptArgument(name = "profile name", type = "string", description = "profile name"),
			@ScriptArgument(name = "sync time", type = "string", description = "sync time like 22:10 and 10 minute unit") })
	public void setSyncTime(String[] args) {
		LdapProfile p = ldap.getProfile(args[0]);
		if (p == null) {
			context.println("profile not found");
			return;
		}

		try {
			String[] syncTime = args[1].split(":");
			if(syncTime.length != 2) {
				context.println("the sync time format should be like this => '22:20'");
				return;
			}
			int hour = Integer.valueOf(syncTime[0]);
			if(hour < 0 || hour > 23) {
				context.println("the hour should be between 0 - 23");
				return;
			}
			int minute = Integer.valueOf(syncTime[1]);
			if(minute < 0 || minute > 50 || minute % 10 != 0) {
				context.println("the minute should be 0, 10, 20, 30, 40 or 50");
				return;
			}
			
			p.setSyncTime(args[1]);
			ldap.updateProfile(p);
			context.println("set");
		} catch (NumberFormatException e) {
			context.println("invalid number format");
		}
	}

	private LdapSyncService getSyncService() {
		ServiceReference ref = bc.getServiceReference(LdapSyncService.class.getName());
		if (ref == null)
			throw new IllegalStateException("kraken-dom not found");

		return (LdapSyncService) bc.getService(ref);
	}
	
	@ScriptUsage(description = "set page size", arguments = {
			@ScriptArgument(name = "page size", type = "int", description = "page size to retrieve data at one go", optional = true) })
	public void setPageSize(String[] args) {
		ConfigDatabase db = conf.ensureDatabase("kraken-ldap");
		Config c = db.findOne(LdapConfig.class, null);
		LdapConfig ldapConfig = c.getDocument(LdapConfig.class);
		
		if(args.length == 0) {
			context.println("page size: " + ldapConfig.getPageSize());
		}
		
		if (args.length != 1) {
			context.println("input one integer argument");
			return;
		}
		
		try{
			int pageSize = Integer.parseInt(args[0]);
			
			ldapConfig.setPageSize(pageSize);
			db.update(c, ldapConfig);
			
			context.println("set");
		} catch (NumberFormatException e) {
			context.println("invalid number format");
		}
	}

    @ScriptUsage(description = "initialize ldap connection pool", arguments = {})
    public void initializeLdapConnectionPool(String[] args) {
        ldap.initializeLdapConnectionPool();
        context.println("initialize ldap connection pool done.");
    }

    @ScriptUsage(description = "set ldap connection pool size", arguments = {
            @ScriptArgument(name = "initial pool size", type = "int", description = "initial ldap connection pool size"),
            @ScriptArgument(name = "max pool size", type = "int", description = "max ldap connection pool size")})
    public void setLdapConnectionPoolSize(String[] args) {
        if(args.length == 0) {
            context.println("initial pool size: " + ldap.getInitialConnectionsSize() + " , max pool size: " + ldap.getMaxConnectionsSize());
        }

        if (args.length != 2) {
            context.println("input initial pool size and max pool size together");
            return;
        }

        ldap.setInitialConnectionsSize(Integer.parseInt(args[0]));
        ldap.setMaxConnectionsSize(Integer.parseInt(args[1]));

        context.println("set initial pool size, max pool size set done.");

        ldap.initializeLdapConnectionPool();
        context.println("initialize ldap connection pool done.");
    }

    //pks. 2014-11-10. frodo-ldap-extend's functions moved to here
    
    @ScriptUsage(description = "check user account expire", arguments = {
			@ScriptArgument(name = "ldap profile", type = "string", description = "ldap profile name", optional = false),
			@ScriptArgument(name = "user account", type = "string", description = "user account", optional = false) })
	public void isAccountExpired(String[] args) {
		String profileName = args[0];
		LdapProfile profile = ldap.getProfile(profileName);

		if (profile == null) {
			context.println("profile not found");
			return;
		}

		try {
			if (ldap.isAccountExpired(profile, args[1]))
				context.println("account [" + args[1] + "] is expired");
			else
				context.println("account [" + args[1] + "] is not expired");
		} catch (IllegalStateException e) {
			context.println("account [" + args[1] + "] does not exist");
		}
	}

	@ScriptUsage(description = "check user password expire", arguments = {
			@ScriptArgument(name = "ldap profile", type = "string", description = "ldap profile name", optional = false),
			@ScriptArgument(name = "user account", type = "string", description = "user account", optional = false) })
	public void isPasswordExpired(String[] args) {
		String profileName = args[0];
		LdapProfile profile = ldap.getProfile(profileName);

		if (profile == null) {
			context.println("profile not found");
			return;
		}

		try {
			if (ldap.isPasswordExpired(profile, args[1]))
				context.println("account [" + args[1] + "] is expired");
			else
				context.println("account [" + args[1] + "] is not expired");
		} catch (IllegalStateException e) {
			context.println("account [" + args[1] + "] does not exist");
		} catch (LDAPException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
