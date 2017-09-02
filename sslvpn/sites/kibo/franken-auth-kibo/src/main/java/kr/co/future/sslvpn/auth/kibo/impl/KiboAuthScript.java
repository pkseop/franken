package kr.co.future.sslvpn.auth.kibo.impl;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;

import kr.co.future.sslvpn.auth.kibo.KiboAuthApi;
import kr.co.future.sslvpn.auth.kibo.KiboConfig;
import kr.co.future.api.Script;
import kr.co.future.api.ScriptArgument;
import kr.co.future.api.ScriptContext;
import kr.co.future.api.ScriptUsage;

public class KiboAuthScript implements Script {

	private KiboAuthApi kiboApi;
	private ScriptContext context;

	public KiboAuthScript(KiboAuthApi kiboApi) {
		this.kiboApi = kiboApi;
	}

	@Override
	public void setScriptContext(ScriptContext context) {
		this.context = context;
	}

	public void getConfig(String[] args) {
		KiboConfig config = kiboApi.getConfig();
		if (config == null) {
			context.println("kibo config not set");
			return;
		}
		context.println(config);
	}

	@ScriptUsage(description = "set Information", arguments = {
			@ScriptArgument(name = "url", type = "string", description = "host url"),
			@ScriptArgument(name = "adminAccount", type = "String", description = "Admin Account Id"),
			@ScriptArgument(name = "adminPassword", type = "String", description = "Admin Account Password"),
			@ScriptArgument(name = "ldapAdminAccount", type = "String", description = "ldap admin account"),
			@ScriptArgument(name = "ldapAdminPassword", type = "String", description = "ldap admin password"),
			@ScriptArgument(name = "secureKey", type = "String", description = "decrypt secureKey") })
	public void setConfig(String[] args) throws MalformedURLException {
		KiboConfig config = new KiboConfig();
		URL url = new URL(args[0]);

		config.setUrl(url);
		config.setAdminAccount(args[1]);
		config.setAdminPassword(args[2]);
		config.setLdapAdminAccount(args[3]);
		config.setLdapAdminPassword(args[4]);
		config.setSecureKey(args[5]);

		kiboApi.setConfig(config);
		context.println("set");
	}

	@ScriptUsage(description = "decryption keypad value", arguments = { @ScriptArgument(name = "data", type = "string", description = "Encryption data") })
	public void decrypt(String args[]) {
		String data = kiboApi.decryptKey(args[0]);
		context.println("decrypt data=[" + data + "]");
	}

	@ScriptUsage(description = "verify Device", arguments = { @ScriptArgument(name = "deviceId", type = "string", description = "device Id") })
	public void verifyDevice(String[] args) {
		String deviceKey = args[0];

		Map<String, Object> verify = kiboApi.verifyDevice(deviceKey);
		context.println("verify=" + verify);
		String result = verify.get("success").toString();

		context.println("result");
		context.println("==========");
		if (result.equals("true"))
			context.println("verify MDM success");
		else
			context.println("verify MDM failed");

	}

	@ScriptUsage(description = "verify OTP", arguments = {
			@ScriptArgument(name = "deviceId", type = "string", description = "device Id"),
			@ScriptArgument(name = "OtpValue", type = "string", description = "otp Value") })
	public void verifyOtp(String[] args) {
		String deviceKey = args[0];
		String otpValue = args[1];

		Map<String, Object> verify = kiboApi.verifyOtp(deviceKey, otpValue);
		context.println("verify=" + verify);
		String result = verify.get("success").toString();

		context.println("result");
		context.println("==========");
		if (result.equals("true"))
			context.println("verify OTP success");
		else
			context.println("verify OTP failed");
	}
}
