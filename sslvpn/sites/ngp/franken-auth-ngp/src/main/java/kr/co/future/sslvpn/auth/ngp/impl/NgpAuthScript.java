package kr.co.future.sslvpn.auth.ngp.impl;

import kr.co.future.sslvpn.auth.ngp.NgpAuthApi;
import kr.co.future.sslvpn.auth.ngp.NgpConfig;
import kr.co.future.api.Script;
import kr.co.future.api.ScriptArgument;
import kr.co.future.api.ScriptContext;
import kr.co.future.api.ScriptUsage;

public class NgpAuthScript implements Script {
	
	private NgpAuthApi ngpApi;
	private ScriptContext context;
	
	public NgpAuthScript(NgpAuthApi ngpApi) {
		this.ngpApi = ngpApi;
	}
	
	@Override
	public void setScriptContext(ScriptContext context) {
		this.context = context;
	}
	
	public void getConfig(String[] args) {
		NgpConfig config = ngpApi.getConfig();
		if (config == null) {
			context.println("ngp config not set");
			return;
		}

		context.println(config);
	}
	
	@ScriptUsage(description = "set vpn confirm socket connection url", arguments = {
			@ScriptArgument(name = "url", type = "string", description = "vpn confirm socket connection url"),
			@ScriptArgument(name = "port", type = "int", description = "socket connection port") })
	public void setConfig(String[] args) {
		NgpConfig config = new NgpConfig();
		config.setUrl(args[0]);
		config.setPort(Integer.valueOf(args[1]));
		ngpApi.setConfig(config);
		context.println("set");
	}

}
