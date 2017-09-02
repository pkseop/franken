package kr.co.future.sslvpn.auth.kre.impl;

import kr.co.future.sslvpn.auth.kre.KreAuthService;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.apache.felix.ipojo.annotations.ServiceProperty;

import kr.co.future.api.Script;
import kr.co.future.api.ScriptFactory;

@Component(name = "kre-auth-script-factory")
@Provides
public class KreAuthScriptFactory implements ScriptFactory {
	@SuppressWarnings("unused")
	@ServiceProperty(name = "alias", value = "kre")
	private String alias;

	@Requires
	private KreAuthService kre;

	@Override
	public Script createScript() {
		return new KreAuthScript(kre);
	}

}
