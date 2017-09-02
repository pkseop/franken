package kr.co.future.sslvpn.auth.pdas.impl;

import kr.co.future.sslvpn.auth.pdas.PdasAuthApi;
import kr.co.future.sslvpn.auth.pdas.PdasConfig;
import kr.co.future.api.Script;
import kr.co.future.api.ScriptArgument;
import kr.co.future.api.ScriptContext;
import kr.co.future.api.ScriptUsage;

public class PdasAuthScript implements Script {
	private PdasAuthApi pdasApi;
	private ScriptContext context;

	public PdasAuthScript(PdasAuthApi pdasApi) {
		this.pdasApi = pdasApi;
	}

	@Override
	public void setScriptContext(ScriptContext context) {
		this.context = context;
	}

	public void config(String[] args) {
		PdasConfig config = pdasApi.getConfig();
		if (config == null) {
			context.println("pdas config not set");
			return;
		}

		context.println(config);
	}

	@ScriptUsage(description = "set db connection", arguments = {
			@ScriptArgument(name = "db host", type = "string", description = "database host name (e.g. localhost)"),
			@ScriptArgument(name = "db name", type = "string", description = "database name"),
			@ScriptArgument(name = "db account", type = "string", description = "database account"),
			@ScriptArgument(name = "db password", type = "string", description = "database password"),
			@ScriptArgument(name = "user table", type = "string", description = "user table name") })
	public void set(String[] args) {
		PdasConfig config = new PdasConfig();
		config.setDbHost(args[0]);
		config.setDbName(args[1]);
		config.setDbAccount(args[2]);
		config.setDbPassword(args[3]);
		config.setTableName(args[4]);

		pdasApi.setConfig(config);
		context.println("set");
	}

	@ScriptUsage(description = "test login", arguments = {
			@ScriptArgument(name = "account", type = "string", description = "login name"),
			@ScriptArgument(name = "password", type = "string", description = "password") })
	public void verify(String[] args) {
		if (pdasApi.verifyPassword(args[0], args[1]))
			context.println("login success");
		else
			context.println("password fail");
	}
}
