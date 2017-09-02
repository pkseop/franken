package kr.co.future.sslvpn.auth.hl.impl;

import kr.co.future.sslvpn.auth.hl.HlAuthApi;
import kr.co.future.sslvpn.auth.hl.HlConfig;
import kr.co.future.sslvpn.auth.hl.HlLdapUserCacheRemoveEnforcerApi;
import kr.co.future.sslvpn.auth.hl.HlUserRemoveEnforcerApi;
import kr.co.future.api.Script;
import kr.co.future.api.ScriptArgument;
import kr.co.future.api.ScriptContext;
import kr.co.future.api.ScriptUsage;
import kr.co.future.confdb.Config;
import kr.co.future.confdb.ConfigDatabase;
import kr.co.future.confdb.ConfigService;

public class HlAuthScript implements Script {
	private ScriptContext context;

	private HlUserRemoveEnforcerApi removeApi;
	private HlLdapUserCacheRemoveEnforcerApi removeCacheApi;
    private HlAuthApi hlAuthApi;
	private ConfigService conf;

	public HlAuthScript(ConfigService conf, HlUserRemoveEnforcerApi removeApi, HlLdapUserCacheRemoveEnforcerApi removeCacheApi, HlAuthApi hlAuthApi) {
		this.removeApi = removeApi;
		this.removeCacheApi = removeCacheApi;
		this.conf = conf;
        this.hlAuthApi = hlAuthApi;
	}

	@ScriptUsage(description = "set user remove hour", arguments = { @ScriptArgument(name = "hour", type = "int", description = "user remove hour", optional = false) })
	public void setUserRemoveHour(String[] args) {
		int hour = Integer.parseInt(args[0]);

		HlConfig config = new HlConfig();
		config.setRemoveHour(hour);
		ConfigDatabase db = conf.ensureDatabase("hl");
		Config c = db.findOne(HlConfig.class, null);
		if (c != null) {
			db.update(c, config);
		} else {
			db.add(config);
		}

		removeApi.registerSchedule(hour);
		context.println("register user remove schedule, cron run " + hour + "H");
	}
	
	@ScriptUsage(description = "set ldapuser cache remove hour", arguments = { @ScriptArgument(name = "hour", type = "int", description = "ldapuser cache list remove hour", optional = false) })
	public void setLdapuserCacheListRemoveHour(String[] args) {
		int hour = Integer.parseInt(args[0]);

		HlConfig config = new HlConfig();
		config.setRemoveCacheHour(hour);
		ConfigDatabase db = conf.ensureDatabase("hl");
		Config c = db.findOne(HlConfig.class, null);
		if (c != null) {
			db.update(c, config);
		} else {
			db.add(config);
		}

		removeCacheApi.registerSchedule(hour);
		context.println("register ldapuser cache list remove schedule, cron run " + hour + "H");
	}

    @ScriptUsage(description = "remove ldapuser cache data")
    public void removeLdapuserCacheData(String[] args) {
        hlAuthApi.removeCacheList();

        context.println("remove ldapuser cache data done");
    }

    @ScriptUsage(description = "enable user password reset button", arguments = @ScriptArgument(name = "enable password reset button", type = "boolean", optional = true))
    public void setUserPasswordResetButton(String[] args) {
        if(args.length == 1) {
            if(args[0].equals("true") || args[0].equals("false")){
                HlConfig config = new HlConfig();
                config.setEnable(Boolean.valueOf(args[0]));

                ConfigDatabase db = conf.ensureDatabase("hl");
                Config c = db.findOne(HlConfig.class, null);
                if (c != null) {
                    db.update(c, config);
                } else {
                    db.add(config);
                }

                return;
            } else {
                context.print("enter true or false");
                return;
            }
        } else if(args.length == 0) {
            ConfigDatabase db = conf.ensureDatabase("hl");
            Config c = db.findOne(HlConfig.class, null);
            HlConfig hlConfig = c.getDocument(HlConfig.class);
            context.println("enable: " + hlConfig.isEnabled());
        } else {
            context.println("enter true or false");
            return;
        }

    }

	@Override
	public void setScriptContext(ScriptContext context) {
		this.context = context;
	}

}
