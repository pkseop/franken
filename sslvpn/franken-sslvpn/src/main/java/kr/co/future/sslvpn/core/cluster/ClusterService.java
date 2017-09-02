package kr.co.future.sslvpn.core.cluster;

import java.util.Collection;

import kr.co.future.sslvpn.core.cluster.ClusterConfig;
import kr.co.future.sslvpn.core.cluster.ClusterEventListener;
import kr.co.future.sslvpn.core.cluster.ClusterMessageListener;
import kr.co.future.sslvpn.core.cluster.ClusterNode;
import kr.co.future.sslvpn.core.cluster.ClusterNodeConfig;
import kr.co.future.sslvpn.core.cluster.ClusterOperation;

public interface ClusterService {
	void start();

	void stop();

	long getBootTimestamp();

	ClusterConfig getConfig();

	void setConfig(ClusterConfig mode);

	Collection<ClusterNode> getClusterNodes();

	/**
	 * @return the current cluster master, null if current node is master
	 */
	ClusterNode getClusterMaster();

//	void updateClusterMaster();

	ClusterNode getClusterNode(int id);

	void addNode(ClusterNodeConfig nodeConfig);

	void removeNode(int id);

	void addMessageListener(ClusterMessageListener listener);

	void removeMessageListener(ClusterMessageListener listener);

	void addEventListener(ClusterEventListener listener);

	void removeEventListener(ClusterEventListener listener);

	int leaseIp(String loginName, String profileName, int poolSize);

	void submitUpdate(ClusterOperation op);
	
	void updateClusterTunnelCount();
//pks. 2015-01-23. 중복 로그인 체크 방법의 변경으로 사용하지 않음.		
//	boolean isDuplicatedLogin(String loginName);	
//	boolean killTunnelByDupLogin(String loginName);
	
	void setConfigWithNoStart(ClusterConfig newConfig);
	
	public void setConfigWithRegisterListenersBySyncUserDataOnly(ClusterConfig newConfig);
	
	public void setConfigWithExcludeUserData(ClusterConfig newConfig);
}
