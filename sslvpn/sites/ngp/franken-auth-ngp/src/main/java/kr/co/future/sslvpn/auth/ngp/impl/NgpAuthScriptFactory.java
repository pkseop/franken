package kr.co.future.sslvpn.auth.ngp.impl;

import kr.co.future.sslvpn.auth.ngp.NgpAuthApi;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.apache.felix.ipojo.annotations.ServiceProperty;

import kr.co.future.api.Script;
import kr.co.future.api.ScriptFactory;

@Component(name = "ngp-auth-script-factory")
@Provides
public class NgpAuthScriptFactory implements ScriptFactory {
	@SuppressWarnings("unused")
	@ServiceProperty(name = "alias", value = "ngp")
	private String alias;

	@Requires
	private NgpAuthApi ngpApi;

	@Override
	public Script createScript() {
		return new NgpAuthScript(ngpApi);
	}

}
