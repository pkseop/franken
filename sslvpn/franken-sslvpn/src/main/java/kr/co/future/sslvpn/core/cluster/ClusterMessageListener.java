package kr.co.future.sslvpn.core.cluster;

import kr.co.future.sslvpn.core.cluster.ClusterOperation;

public interface ClusterMessageListener {
	int getNodeId();

	void onMessage(ClusterOperation op);
}
