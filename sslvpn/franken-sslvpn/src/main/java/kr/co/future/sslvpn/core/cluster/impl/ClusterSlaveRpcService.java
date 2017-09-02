package kr.co.future.sslvpn.core.cluster.impl;

import java.util.Map;

import kr.co.future.sslvpn.core.cluster.ClusterOperation;
import kr.co.future.sslvpn.core.cluster.ClusterService;

import kr.co.future.rpc.RpcMethod;
import kr.co.future.rpc.SimpleRpcService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ClusterSlaveRpcService extends SimpleRpcService {
	private final Logger logger = LoggerFactory.getLogger(ClusterSlaveRpcService.class.getName());

	private ClusterService cluster;

	public ClusterSlaveRpcService(ClusterService cluster) {
		this.cluster = cluster;
	}

	@RpcMethod(name = "replicate")
	public void replicate(Map<String, Object> params) {
		String category = (String) params.get("category");
		String operation = (String) params.get("operation");
		Object obj = params.get("obj");

		if (logger.isTraceEnabled())
			logger.trace("frodo core: recv replication, category [{}], operation [{}], obj [{}]", new Object[] { category,
					operation, obj });

		ClusterOperation op = new ClusterOperation(category, operation, obj);
		cluster.submitUpdate(op);
	}
}
