package kr.co.future.sslvpn.auth.sm.impl;

import kr.co.future.sslvpn.auth.sm.SMAuthApi;
import kr.co.future.sslvpn.auth.sm.SMAuthUrl;
import kr.co.future.api.Script;
import kr.co.future.api.ScriptArgument;
import kr.co.future.api.ScriptContext;
import kr.co.future.api.ScriptUsage;
import kr.co.future.confdb.Config;
import kr.co.future.confdb.ConfigDatabase;
import kr.co.future.confdb.ConfigService;

public class SMAuthScript implements Script {
	private ScriptContext context;

	private ConfigService conf;
    private SMAuthApi smAuthApi;

	public SMAuthScript(ConfigService conf, SMAuthApi smAuthApi) {
		this.conf = conf;
        this.smAuthApi = smAuthApi;
	}

	@ScriptUsage(description = "set auth url", arguments = { @ScriptArgument(name = "url", type = "string", description = "auth url", optional = false) })
	public void setAuthUrl(String[] args) {
		String url = args[0];

		SMAuthUrl config = new SMAuthUrl();
        config.setUrl(url);

		ConfigDatabase db = conf.ensureDatabase("sm");
		Config c = db.findOne(SMAuthUrl.class, null);
		if (c != null) {
			db.update(c, config);
		} else {
			db.add(config);
		}

        smAuthApi.setAuthUrl(url);
		context.println("auth url register done. url: [" + url + "]");
	}
	
	@Override
	public void setScriptContext(ScriptContext context) {
		this.context = context;
	}

}
