package kr.co.future.sslvpn.core.script;

import kr.co.future.sslvpn.core.DupLoginCheck;
import kr.co.future.sslvpn.core.GlobalConfigApi;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.apache.felix.ipojo.annotations.ServiceProperty;
import kr.co.future.api.Script;
import kr.co.future.api.ScriptFactory;

@Component(name = "dup-login-script-factory")
@Provides
public class DupLoginScriptFactory implements ScriptFactory {
	@ServiceProperty(name = "alias", value = "dup-login")
	private String alias;
	
	@Requires
	private GlobalConfigApi configApi;
	
	@Requires
	private DupLoginCheck dupLoginCheck;
	
	@Override
	public Script createScript() {
		return new DupLoginScript(configApi, dupLoginCheck);
	}

}
