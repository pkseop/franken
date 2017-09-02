package kr.co.future.sslvpn.core.cluster;

import kr.co.future.sslvpn.core.cluster.ClusterScript;
import kr.co.future.sslvpn.core.cluster.ClusterService;
import kr.co.future.sslvpn.model.api.AuthorizedDeviceApi;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.apache.felix.ipojo.annotations.ServiceProperty;

import kr.co.future.api.Script;
import kr.co.future.api.ScriptFactory;
import kr.co.future.confdb.ConfigService;

@Component(name = "frodo-cluster-script-factory")
@Provides
public class ClusterScriptFactory implements ScriptFactory {

	@SuppressWarnings("unused")
	@ServiceProperty(name = "alias", value = "cluster")
	private String alias;

	@Requires
	private ClusterService cluster;

	@Requires
	private AuthorizedDeviceApi deviceApi;
	
	@Requires
	private ClusterSync clusterSync;
	
	@Requires
   private ConfigService conf;

	@Override
	public Script createScript() {
		return new ClusterScript(cluster, deviceApi, clusterSync, conf);
	}

}
