package kr.co.future.sslvpn.core.xenics.script;

import kr.co.future.sslvpn.core.xenics.XenicsConfig;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.apache.felix.ipojo.annotations.ServiceProperty;

import kr.co.future.api.Script;
import kr.co.future.api.ScriptFactory;

@Component(name = "xenics-config-script-factory")
@Provides
public class XenicsScriptFactory implements ScriptFactory {

	@ServiceProperty(name = "alias", value = "xenics")
	private String alias;
	
	@Requires
	private XenicsConfig xenicsConfig;
	
	@Override
	public Script createScript() {
		return new XenicsScript(xenicsConfig);
	}
	
}
