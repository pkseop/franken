package kr.co.future.sslvpn.auth.sm.impl;

import kr.co.future.sslvpn.auth.sm.SMAuthApi;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.apache.felix.ipojo.annotations.ServiceProperty;

import kr.co.future.api.Script;
import kr.co.future.api.ScriptFactory;
import kr.co.future.confdb.ConfigService;

@Component(name = "sm-auth-script-factory")
@Provides
public class SMAuthScriptFactory implements ScriptFactory {
	@SuppressWarnings("unused")
	@ServiceProperty(name = "alias", value = "sm")
	private String alias;

	@Requires
	private ConfigService conf;

    @Requires
    private SMAuthApi smAuthApi;

	@Override
	public Script createScript() {
		return new SMAuthScript(conf, smAuthApi);
	}
}
