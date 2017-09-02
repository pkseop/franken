package kr.co.future.sslvpn.auth.pdas.impl;

import kr.co.future.sslvpn.auth.pdas.PdasAuthApi;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.apache.felix.ipojo.annotations.ServiceProperty;

import kr.co.future.api.Script;
import kr.co.future.api.ScriptFactory;

@Component(name = "pdas-auth-script-factory")
@Provides
public class PdasAuthScriptFactory implements ScriptFactory {
	@SuppressWarnings("unused")
	@ServiceProperty(name = "alias", value = "pdas")
	private String alias;

	@Requires
	private PdasAuthApi pdasApi;

	@Override
	public Script createScript() {
		return new PdasAuthScript(pdasApi);
	}

}
