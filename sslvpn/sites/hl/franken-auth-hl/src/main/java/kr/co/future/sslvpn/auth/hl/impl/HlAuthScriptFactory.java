package kr.co.future.sslvpn.auth.hl.impl;

import kr.co.future.sslvpn.auth.hl.HlAuthApi;
import kr.co.future.sslvpn.auth.hl.HlLdapUserCacheRemoveEnforcerApi;
import kr.co.future.sslvpn.auth.hl.HlUserRemoveEnforcerApi;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.apache.felix.ipojo.annotations.ServiceProperty;

import kr.co.future.api.Script;
import kr.co.future.api.ScriptFactory;
import kr.co.future.confdb.ConfigService;

@Component(name = "hl-auth-script-factory")
@Provides
public class HlAuthScriptFactory implements ScriptFactory {
	@SuppressWarnings("unused")
	@ServiceProperty(name = "alias", value = "hl")
	private String alias;

	@Requires
	private HlUserRemoveEnforcerApi removeApi;
	
	@Requires
	private HlLdapUserCacheRemoveEnforcerApi removeCacheApi;

	@Requires
	private ConfigService conf;

    @Requires
    private HlAuthApi hlAuthApi;

	@Override
	public Script createScript() {
		return new HlAuthScript(conf, removeApi, removeCacheApi, hlAuthApi);
	}
}
