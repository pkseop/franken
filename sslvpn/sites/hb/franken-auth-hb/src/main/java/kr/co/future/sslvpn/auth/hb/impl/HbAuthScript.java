package kr.co.future.sslvpn.auth.hb.impl;

import java.util.HashMap;
import java.util.Map;

import kr.co.future.sslvpn.auth.hb.HbAuthApi;
import kr.co.future.sslvpn.auth.hb.HbConfig;
import kr.co.future.api.Script;
import kr.co.future.api.ScriptArgument;
import kr.co.future.api.ScriptContext;
import kr.co.future.api.ScriptUsage;

public class HbAuthScript implements Script {
	private HbAuthApi hbApi;
	private ScriptContext context;

	public HbAuthScript(HbAuthApi hbApi) {
		this.hbApi = hbApi;
	}

	@Override
	public void setScriptContext(ScriptContext context) {
		this.context = context;
	}

	@ScriptUsage(description = "set db connection", arguments = {
			@ScriptArgument(name = "db host", type = "string", description = "database host name (e.g. localhost)"),
			@ScriptArgument(name = "db name", type = "string", description = "database name"),
			@ScriptArgument(name = "db account", type = "string", description = "database account"),
			@ScriptArgument(name = "db password", type = "string", description = "database password"),
			@ScriptArgument(name = "user table", type = "string", description = "user table name") })
	public void set(String[] args) {
		HbConfig config = new HbConfig();
		config.setDbHost(args[0]);
		config.setDbName(args[1]);
		config.setDbAccount(args[2]);
		config.setDbPassword(args[3]);
		config.setTableName(args[4]);

		hbApi.setConfig(config);
		context.println("set");
	}

	@ScriptUsage(description = "set db connection")
	public void config(String[] args) {
		HbConfig config = hbApi.getConfig();
		if (config == null) {
			context.println("hb config not set");
			return;
		}

		context.println(config);
	}

	@ScriptUsage(description = "verify user login", arguments = {
			@ScriptArgument(name = "login name", type = "string", description = "user login name"),
			@ScriptArgument(name = "password", type = "string", description = "user password"),
			@ScriptArgument(name = "host name", type = "string", description = "user host name") })
	public void verify(String[] args) {
		String loginName = args[0];
		String password = args[1];
		String hostName = args[2];

		Map<String, Object> props = new HashMap<String, Object>();
		props.put("id", loginName);
		props.put("pw", password);
		props.put("host_name", hostName);

		context.println(hbApi.login(props));
	}
}
