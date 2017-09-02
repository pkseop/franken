package kr.co.future.sslvpn.auth.ice.impl;

import java.net.MalformedURLException;
import java.util.Map;

import kr.co.future.sslvpn.auth.ice.IceAuthApi;
import kr.co.future.sslvpn.auth.ice.IceConfig;
import kr.co.future.api.Script;
import kr.co.future.api.ScriptArgument;
import kr.co.future.api.ScriptContext;
import kr.co.future.api.ScriptUsage;

public class IceAuthScript implements Script {

	private IceAuthApi iceApi;
	private ScriptContext context;

	public IceAuthScript(IceAuthApi iceApi) {
		this.iceApi = iceApi;
	}

	@Override
	public void setScriptContext(ScriptContext context) {
		this.context = context;
	}

	public void getConfig(String args[]) {
		IceConfig config = iceApi.getConfig();

		if (config == null) {
			context.println("ice config not set");
			return;
		}

		context.println(config);

	}

	@ScriptUsage(description = "set Information", arguments = { 
			@ScriptArgument(name = "Idattribute", type = "string", description = "uid or cn"),
			@ScriptArgument(name = "ldap profile name", type = "string", description = "to use ldap")
			})
	public void setConfig(String[] args) throws MalformedURLException {
		IceConfig config = iceApi.getConfig();
		if(config == null)
			config = new IceConfig();
		
		config.setIdAttr(args[0]);
		config.setLdapProfileName(args[1]);

		iceApi.setConfig(config);
		context.println("set");
	}

	public void verifyUser(String[] args) {
		Map<String, Object> m = iceApi.verifyUser(args[0]);
		context.println(m.toString());
	}
	
	@ScriptUsage(description = "get attribute value from ldap", arguments = { 
			@ScriptArgument(name = "login name", type = "string", description = "uid or cn"),
			@ScriptArgument(name = "attribute name", type = "string", description = "attribute from ldap")
			})
	public void getLdapAttrVal(String[] args) {
		String ret = iceApi.getAttribute(args[0], args[1]);
		context.println("result: user [" + args[0] + "]'s attribute [" + args[1] + "]" + " value is [" + ret + "]");
	}
}
