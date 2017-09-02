package kr.co.future.sslvpn.authui.impl;

import kr.co.future.sslvpn.authui.PasswordResetService;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.apache.felix.ipojo.annotations.ServiceProperty;

import kr.co.future.api.Script;
import kr.co.future.api.ScriptFactory;

@Component(name = "frodo-reset-script-factory")
@Provides
public class PasswordResetScriptFactory implements ScriptFactory {

	@SuppressWarnings("unused")
	@ServiceProperty(name = "alias", value = "frodo-reset")
	private String alias;

	@Requires
	private PasswordResetService reset;

	@Override
	public Script createScript() {
		return new PasswordResetScript(reset);
	}

}
