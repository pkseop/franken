package kr.co.future.sslvpn.auth.kibo.impl;

import kr.co.future.sslvpn.auth.kibo.KiboAuthApi;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.apache.felix.ipojo.annotations.ServiceProperty;

import kr.co.future.api.Script;
import kr.co.future.api.ScriptFactory;

@Component(name = "kibo-auth-script-factory")
@Provides
public class KiboAuthScriptFactory implements ScriptFactory {

	@SuppressWarnings("unused")
	@ServiceProperty(name = "alias", value = "kiboauth")
	private String alias;

	@Requires
	private KiboAuthApi kiboApi;

	@Override
	public Script createScript() {
		return new KiboAuthScript(kiboApi);
	}

}
