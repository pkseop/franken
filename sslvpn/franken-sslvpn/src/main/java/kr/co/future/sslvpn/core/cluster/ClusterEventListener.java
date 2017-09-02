package kr.co.future.sslvpn.core.cluster;

import kr.co.future.sslvpn.core.cluster.ClusterNode;

public interface ClusterEventListener {
	void onMasterChange(ClusterNode master);
}
