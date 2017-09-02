package kr.co.future.sslvpn.auth.ncc.impl;

import kr.co.future.sslvpn.auth.ncc.NccAuthApi;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.apache.felix.ipojo.annotations.ServiceProperty;

import kr.co.future.api.Script;
import kr.co.future.api.ScriptFactory;

@Component(name = "ncc-auth-script-factory")
@Provides
public class NccAuthScriptFactory implements ScriptFactory {
	@SuppressWarnings("unused")
	@ServiceProperty(name = "alias", value = "ncc")
	private String alias;

	@Requires
	private NccAuthApi nccApi;

	@Override
	public Script createScript() {
		return new NccAuthScript(nccApi);
	}

}
