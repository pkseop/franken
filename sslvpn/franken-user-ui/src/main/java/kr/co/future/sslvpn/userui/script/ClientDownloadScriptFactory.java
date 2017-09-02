package kr.co.future.sslvpn.userui.script;

import kr.co.future.sslvpn.userui.ClientDownloadConfigApi;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.apache.felix.ipojo.annotations.ServiceProperty;

import kr.co.future.api.Script;
import kr.co.future.api.ScriptFactory;

@Component(name = "client-download-script-factory")
@Provides
public class ClientDownloadScriptFactory implements ScriptFactory{
	@ServiceProperty(name = "alias", value = "client-download")
	private String alias;
	
	@Requires
	private ClientDownloadConfigApi clientDownloadConfigApi;

	@Override
	public Script createScript() {
		return new ClientDownloadScript(clientDownloadConfigApi);
	}
	
	
}
