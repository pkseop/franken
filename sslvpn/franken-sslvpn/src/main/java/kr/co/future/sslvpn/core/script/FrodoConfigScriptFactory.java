package kr.co.future.sslvpn.core.script;

import kr.co.future.sslvpn.core.GlobalConfigApi;
import kr.co.future.sslvpn.core.SqlAuthService;
import kr.co.future.sslvpn.core.UserMigrationServiceApi;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.apache.felix.ipojo.annotations.ServiceProperty;

import kr.co.future.api.Script;
import kr.co.future.api.ScriptFactory;
import kr.co.future.dns.DnsService;

@Component(name = "frodo-config-script-factory")
@Provides
public class FrodoConfigScriptFactory implements ScriptFactory {
	@ServiceProperty(name = "alias", value = "frodo-config")
	private String alias;
	
	@Requires
	private GlobalConfigApi configApi;
	
	@Requires
	private SqlAuthService sqlAuthService;

    @Requires
    private UserMigrationServiceApi userMigrationServiceApi;
    
    @Requires
	private DnsService dns;
	
	public FrodoConfigScriptFactory() {
		
	}
	
	@Override
	public Script createScript() {
		return new FrodoConfigScript(configApi, sqlAuthService, userMigrationServiceApi, dns);
	}
}
