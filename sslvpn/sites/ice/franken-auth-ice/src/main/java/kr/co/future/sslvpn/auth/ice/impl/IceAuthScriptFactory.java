package kr.co.future.sslvpn.auth.ice.impl;

import kr.co.future.sslvpn.auth.ice.IceAuthApi;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.apache.felix.ipojo.annotations.ServiceProperty;

import kr.co.future.api.Script;
import kr.co.future.api.ScriptFactory;

@Component(name = "ice-auth-script-factory")
@Provides
public class IceAuthScriptFactory implements ScriptFactory {
	@ServiceProperty(name = "alias", value = "ice")
	private String alias;

	@Requires
	private IceAuthApi iceApi;;

	@Override
	public Script createScript() {
		return new IceAuthScript(iceApi);
	}
}