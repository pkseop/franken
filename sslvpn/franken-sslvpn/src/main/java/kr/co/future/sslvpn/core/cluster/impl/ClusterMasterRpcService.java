package kr.co.future.sslvpn.core.cluster.impl;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import kr.co.future.sslvpn.core.AuthService;
import kr.co.future.sslvpn.core.cluster.ClusterConfig;
import kr.co.future.sslvpn.core.cluster.ClusterMessageListener;
import kr.co.future.sslvpn.core.cluster.ClusterOperation;
import kr.co.future.sslvpn.core.cluster.ClusterService;
import kr.co.future.sslvpn.model.api.ClusteredIpLeaseApi;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.apache.felix.ipojo.annotations.ServiceProperty;

import kr.co.future.confdb.Config;
import kr.co.future.confdb.ConfigDatabase;
import kr.co.future.confdb.ConfigService;
import kr.co.future.rpc.RpcConnection;
import kr.co.future.rpc.RpcConnectionEventListener;
import kr.co.future.rpc.RpcContext;
import kr.co.future.rpc.RpcException;
import kr.co.future.rpc.RpcMethod;
import kr.co.future.rpc.RpcSession;
import kr.co.future.rpc.SimpleRpcService;
import kr.co.future.sslvpn.core.xenics.XenicsService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(name = "frodo-cluster-master-rpc")
@Provides
public class ClusterMasterRpcService extends SimpleRpcService {
	private final Logger logger = LoggerFactory.getLogger(ClusterMasterRpcService.class.getName());

	@ServiceProperty(name = "rpc.name", value = "frodo-cluster-master")
	private String name;

	private Date bootTime = new Date();

	@Requires
	private AuthService auth;

	@Requires
	private ClusterService cluster;

	@Requires
	private ClusteredIpLeaseApi leaseApi;
	
	@Requires
	private XenicsService xenicsService;
	
	@Requires
	private ConfigService conf;
	
	@RpcMethod(name="getTunnelCount")
	public int getTunnelCount() {
		return xenicsService.getTotalNumOfTunnels();
	}

	@RpcMethod(name = "getBootTimestamp")
	public long getBootTimestamp() {
		return bootTime.getTime();
	}

	@RpcMethod(name = "connect")
	public void connect(int node) throws RpcException, InterruptedException {
		RpcConnection con = RpcContext.getConnection();
		RpcSession session = con.createSession("frodo-cluster-slave");
		con.addListener(new ClusterDisconnector(node));
		ClusterNotifier notifier = new ClusterNotifier(node, session);
		con.setProperty("notifier", notifier);
		cluster.addMessageListener(notifier);

		logger.info("frodo core: connected slave [{}]", node);
	}
	
//pks. 2015-01-23. 중복 로그인 체크 방법의 변경으로 사용하지 않음.
//	@RpcMethod(name = "isDuplicatedLogin")
//	public boolean isDuplicatedLogin(String loginName) {
//		boolean dupLogin = xenicsService.isAlreadyLoggedin(loginName);
//		return dupLogin;
//	}
//	
//	@RpcMethod(name = "killTunnelByDupLogin")
//	public boolean killTunnelByDupLogin(String loginName) {
//		boolean isKillExecuted = false;
//		boolean dupLogin = xenicsService.isAlreadyLoggedin(loginName);
//		if(dupLogin){
//			xenicsService.killTunnel(loginName);
//			isKillExecuted = true;
//		}
//		return isKillExecuted;
//	}

	@RpcMethod(name = "login")
	public Map<String, Object> login(Map<String, Object> props) throws RpcException {
		return auth.login(props);
	}

	@RpcMethod(name = "lease")
	public int lease(String loginName, String profileName, int poolSize) {
		logger.trace("frodo core: rpc lease request for login [{}], profile [{}], pool size [{}]", new Object[] { loginName,
				profileName, poolSize });

		return leaseApi.lease(loginName, profileName, poolSize);
	}
	
	@RpcMethod(name = "isMaster")
	public boolean isMaster() {
		ConfigDatabase db = conf.ensureDatabase("frodo-cluster");
		Config c = db.findOne(ClusterConfig.class, null);
		if(c == null)
			return false;
		ClusterConfig config = c.getDocument(ClusterConfig.class);
		return config.isMaster();
	}

	private static class ClusterNotifier implements ClusterMessageListener {

		private int nodeId;
		private RpcSession session;

		public ClusterNotifier(int nodeId, RpcSession session) {
			this.nodeId = nodeId;
			this.session = session;
		}

		@Override
		public int getNodeId() {
			return nodeId;
		}

		@Override
		public void onMessage(ClusterOperation op) {
			Map<String, Object> m = new HashMap<String, Object>();
			m.put("timestamp", new Date().getTime());
			m.put("category", op.getCategory());
			m.put("operation", op.getOperation());
			m.put("obj", op.getObj());

			session.post("replicate", m);
		}
	}

	private class ClusterDisconnector implements RpcConnectionEventListener {
		private int node;

		public ClusterDisconnector(int node) {
			this.node = node;
		}

		@Override
		public void connectionOpened(RpcConnection connection) {
		}

		@Override
		public void connectionClosed(RpcConnection connection) {
			ClusterNotifier notifier = (ClusterNotifier) connection.getProperty("notifier");
			if (cluster != null)
				cluster.removeMessageListener(notifier);

			logger.info("frodo core: cluster node [{}] connection closed", node);
		}
	}
}
