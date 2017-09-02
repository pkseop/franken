package kr.co.future.sslvpn.auth.ncc.impl;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import kr.co.future.sslvpn.auth.ncc.NccAuthApi;
import kr.co.future.sslvpn.auth.ncc.NccConfig;
import kr.co.future.api.Script;
import kr.co.future.api.ScriptArgument;
import kr.co.future.api.ScriptContext;
import kr.co.future.api.ScriptUsage;

public class NccAuthScript implements Script {
	private NccAuthApi nccApi;
	private ScriptContext context;
	private final static int DEFAULT_TIMEOUT = 5;

	public NccAuthScript(NccAuthApi nncApi) {
		this.nccApi = nncApi;
	}

	@Override
	public void setScriptContext(ScriptContext context) {
		this.context = context;
	}

	public void getConfig(String[] args) {
		NccConfig config = nccApi.getConfig();
		if (config == null) {
			context.println("ncc config not set");
			return;
		}

		context.println(config);
	}

	@ScriptUsage(description = "set vpn confirm url", arguments = {
			@ScriptArgument(name = "url", type = "string", description = "vpn confirm uRL"),
			@ScriptArgument(name = "timeout", type = "int", description = "http timeout in seconds", optional = true) })
	public void setConfig(String[] args) {
		NccConfig config = new NccConfig();
		config.setUrl(args[0]);
		if (args.length >= 2)
			config.setTimeout(Integer.valueOf(args[1]));
		else
			config.setTimeout(DEFAULT_TIMEOUT);

		nccApi.setConfig(config);
		context.println("set");
	}

	@ScriptUsage(description = "verify password", arguments = {
			@ScriptArgument(name = "id", type = "string", description = "id"),
			@ScriptArgument(name = "password", type = "string", description = "password") })
	public void verifyPassword(String[] args) throws IOException {
		Map<String, Object> m = new HashMap<String, Object>();
		m.put("id", args[0]);
		m.put("pw", args[1]);

		context.println("verify password");
		context.println("----------------");
		context.println(nccApi.login(m));

	}

	@ScriptUsage(description = "verify User", arguments = { @ScriptArgument(name = "id", type = "string", description = "id") })
	public void verifyUser(String[] args) throws IOException {
		context.println("verify user");
		context.println("------------");
		context.println(nccApi.verifyUser(args[0]));
	}
}