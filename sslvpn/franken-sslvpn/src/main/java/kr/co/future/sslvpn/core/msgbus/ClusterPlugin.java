package kr.co.future.sslvpn.core.msgbus;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import kr.co.future.sslvpn.core.cluster.ClusterConfig;
import kr.co.future.sslvpn.core.cluster.ClusterNode;
import kr.co.future.sslvpn.core.cluster.ClusterNodeConfig;
import kr.co.future.sslvpn.core.cluster.ClusterService;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Requires;
import kr.co.future.api.PrimitiveConverter;
import kr.co.future.msgbus.Request;
import kr.co.future.msgbus.Response;
import kr.co.future.msgbus.handler.MsgbusMethod;
import kr.co.future.msgbus.handler.MsgbusPlugin;

@MsgbusPlugin
@Component(name = "frodo-cluster-plugin")
public class ClusterPlugin {

	@Requires
	private ClusterService cluster;

	@MsgbusMethod
	public void getClusterConfig(Request req, Response resp) {
		ClusterConfig cc = cluster.getConfig();
		resp.put("config", PrimitiveConverter.serialize(cc));
		resp.put("status", getClusterStatus());
	}

	private List<Object> getClusterStatus() {
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ssZ");
		List<Object> l = new ArrayList<Object>();
		for (ClusterNode node : cluster.getClusterNodes()) {
			Map<String, Object> m = new HashMap<String, Object>();
			m.put("node_id", node.getId());
			m.put("public_ip", node.getPublicIp());
			m.put("timestamp", node.getTimestamp() != null ? dateFormat.format(node.getTimestamp()) : null);
			m.put("available", node.isAvailable());
			l.add(m);
		}
		return l;
	}

	@MsgbusMethod
	public void setClusterConfig(Request req, Response resp) {
		ClusterConfig cc = cluster.getConfig();
		cc.setEnabled(req.getBoolean("enabled"));
		cc.setMaster(req.getBoolean("is_master"));
		cc.setClusterLease(req.getBoolean("cluster_lease"));
		cc.setNodeId(req.getInteger("node_id"));
		cc.setSharedKey(req.getString("shared_key"));

		cluster.setConfig(cc);
	}

	@MsgbusMethod
	public void addClusterNode(Request req, Response resp) {
		ClusterNodeConfig n = new ClusterNodeConfig();
		n.setNodeId(req.getInteger("node_id"));
		n.setPublicIp(req.getString("public_ip"));
		n.setIp(req.getString("ip"));
		n.setPort(req.getInteger("port"));
		cluster.addNode(n);
	}

	@MsgbusMethod
	public void removeClusterNode(Request req, Response resp) {
		cluster.removeNode(req.getInteger("node_id"));
	}
}
