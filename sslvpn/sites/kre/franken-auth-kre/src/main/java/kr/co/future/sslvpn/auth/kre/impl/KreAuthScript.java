package kr.co.future.sslvpn.auth.kre.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import kr.co.future.api.Script;
import kr.co.future.api.ScriptArgument;
import kr.co.future.api.ScriptContext;
import kr.co.future.api.ScriptUsage;
import kr.co.future.sslvpn.auth.kre.KreAuthConfig;
import kr.co.future.sslvpn.auth.kre.KreAuthService;

public class KreAuthScript implements Script {
	private KreAuthService kre;
	private ScriptContext context;

	public KreAuthScript(KreAuthService kre) {
		this.kre = kre;
	}

	@Override
	public void setScriptContext(ScriptContext context) {
		this.context = context;
	}

	@ScriptUsage(description = "verify user", arguments = {
			@ScriptArgument(name = "login name", type = "string", description = "login name"),
			@ScriptArgument(name = "use dummy", type = "string", description = "true or false") })
	public void verifySso(String[] args) {
		boolean result = false;
		boolean useDummy = Boolean.parseBoolean(args[2]);
		if (useDummy)
			result = kre.dummyVerifyUser(args[0]);
		else
			result = kre.verifySso(args[0], args[1]);
		context.println(result);
	}

	@ScriptUsage(description = "set sso config", arguments = {
			@ScriptArgument(name = "api key", type = "string", description = "sso api key"),
			@ScriptArgument(name = "remote ip1", type = "string", description = "sso remote ip1"),
			@ScriptArgument(name = "remote ip2", type = "string", description = "sso remote ip2"),
			@ScriptArgument(name = "use dummy", type = "string", description = "true or false", optional = true) })
	public void setSsoConfig(String[] args) {
		KreAuthConfig config = new KreAuthConfig();
		config.setSsoApiKey(args[0]);
		List<String> remoteIps = new ArrayList<String>();
		remoteIps.add(args[1]);
		remoteIps.add(args[2]);
		config.setSsoRemoteIps(remoteIps);

		if (args.length > 3)
			config.setUseDummy(Boolean.parseBoolean(args[2]));

		kre.setSsoConfig(config);
		context.println("sso config set");
	}

	@ScriptUsage(description = "get sso config")
	public void getSsoConfig(String[] args) {
		context.println(kre.getSsoConfig().toString());
	}
}
