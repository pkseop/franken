package kr.co.future.sslvpn.core.cluster;

import java.util.ArrayList;
import java.util.List;

import kr.co.future.api.CollectionTypeHint;
import kr.co.future.api.FieldOption;
import kr.co.future.confdb.CollectionName;
import kr.co.future.sslvpn.core.cluster.ClusterConfig;
import kr.co.future.sslvpn.core.cluster.ClusterNodeConfig;

@CollectionName("config")
public class ClusterConfig {
	@FieldOption(nullable = false)
	private boolean enabled;

	@FieldOption(nullable = false)
	private boolean clusterLease = false;

	@FieldOption(nullable = false)
	private int nodeId;
	
	@FieldOption(nullable = false)
	private boolean isMaster;

	private String sharedKey;
	
	@FieldOption(nullable = true)
	private Boolean useLoadBalancing;
	
	@FieldOption(nullable = true)
	private Boolean syncUserDataOnly;
	
	@FieldOption(nullable = true)
	private Boolean syncExcludeUserData;

	@CollectionTypeHint(ClusterNodeConfig.class)
	private List<ClusterNodeConfig> nodes = new ArrayList<ClusterNodeConfig>();

	public static ClusterConfig duplicate(ClusterConfig config) {
		ClusterConfig c = new ClusterConfig();
		c.enabled = config.enabled;
		c.clusterLease = config.clusterLease;
		c.nodeId = config.nodeId;
		c.sharedKey = config.sharedKey;
		c.nodes = new ArrayList<ClusterNodeConfig>(config.nodes);
		c.isMaster = config.isMaster;
		c.useLoadBalancing = config.useLoadBalancing;
		c.syncUserDataOnly = config.syncUserDataOnly;
		c.syncExcludeUserData = config.syncExcludeUserData;
		return c;
	}

	public ClusterConfig() {
	}

	public ClusterConfig(boolean enabled, int nodeId, String sharedKey, boolean isMaster, Boolean useLoadBalancing, Boolean syncUserDataOnly, Boolean syncExcludeUserData) {
		this.enabled = enabled;
		this.nodeId = nodeId;
		this.sharedKey = sharedKey;
		this.isMaster = isMaster;
		this.useLoadBalancing = useLoadBalancing;
		this.syncUserDataOnly = syncUserDataOnly;
		this.syncExcludeUserData = syncExcludeUserData;
	}
	
	public boolean isMaster() {
		return isMaster;
	}
	
	public void setMaster(boolean isMaster) {
		this.isMaster = isMaster;
	}

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public boolean isClusterLease() {
		return clusterLease;
	}

	public void setClusterLease(boolean clusterLease) {
		this.clusterLease = clusterLease;
	}

	public int getNodeId() {
		return nodeId;
	}

	public void setNodeId(int nodeId) {
		this.nodeId = nodeId;
	}

	public String getSharedKey() {
		return sharedKey;
	}

	public void setSharedKey(String sharedKey) {
		this.sharedKey = sharedKey;
	}

	public List<ClusterNodeConfig> getNodes() {
		return nodes;
	}

	public void setNodes(List<ClusterNodeConfig> nodes) {
		this.nodes = nodes;
	}
	
	public Boolean getUseLoadBalancing() {
		return useLoadBalancing;
	}
	
	public void setUseLoadBalancing(Boolean useLoadBalancing) {
		this.useLoadBalancing = useLoadBalancing;
	}
	
	public Boolean getSyncUserDataOnly() {
		return syncUserDataOnly;
	}

	public void setSyncUserDataOnly(Boolean syncUserDataOnly) {
		this.syncUserDataOnly = syncUserDataOnly;
	}
	
	public Boolean getSyncExcludeUserData() {
		return syncExcludeUserData;
	}

	public void setSyncExcludeUserData(Boolean syncExcludeUserData) {
		this.syncExcludeUserData = syncExcludeUserData;
	}

	@Override
	public String toString() {
		return "enabled=" + enabled + ", is master=" + isMaster +", cluster lease=" + clusterLease + ", node=" + nodeId + ", shared key=" + sharedKey
				+ ", nodes=" + nodes + ", use load balancing=" + useLoadBalancing + ", sync user data only=" + syncUserDataOnly + ", sync exclude user data=" + syncExcludeUserData;
	}
}
