package kr.co.future.sslvpn.auth.hb.impl;

import kr.co.future.sslvpn.auth.hb.HbAuthApi;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.apache.felix.ipojo.annotations.ServiceProperty;

import kr.co.future.api.Script;
import kr.co.future.api.ScriptFactory;

@Component(name = "hb-auth-script-factory")
@Provides
public class HbAuthScriptFactory implements ScriptFactory {
	@SuppressWarnings("unused")
	@ServiceProperty(name = "alias", value = "hb")
	private String alias;

	@Requires
	private HbAuthApi hbApi;

	@Override
	public Script createScript() {
		return new HbAuthScript(hbApi);
	}
}
