package kr.co.future.sslvpn.core.cluster.impl;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.LinkedBlockingDeque;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.TrustManagerFactory;

import kr.co.future.sslvpn.core.CertCenterApi;
import kr.co.future.sslvpn.core.InstallerApi;
import kr.co.future.sslvpn.core.cluster.ClusterConfig;
import kr.co.future.sslvpn.core.cluster.ClusterContext;
import kr.co.future.sslvpn.core.cluster.ClusterEventListener;
import kr.co.future.sslvpn.core.cluster.ClusterMessageListener;
import kr.co.future.sslvpn.core.cluster.ClusterNode;
import kr.co.future.sslvpn.core.cluster.ClusterNodeConfig;
import kr.co.future.sslvpn.core.cluster.ClusterOperation;
import kr.co.future.sslvpn.core.cluster.ClusterService;
import kr.co.future.sslvpn.model.AccessGateway;
import kr.co.future.sslvpn.model.AccessProfile;
import kr.co.future.sslvpn.model.AuthorizedDevice;
import kr.co.future.sslvpn.model.ClientApp;
import kr.co.future.sslvpn.model.ClientCheckProfile;
import kr.co.future.sslvpn.model.DnsZone;
import kr.co.future.sslvpn.model.Server;
import kr.co.future.sslvpn.model.VpnServerConfig;
import kr.co.future.sslvpn.model.api.AccessGatewayApi;
import kr.co.future.sslvpn.model.api.AccessProfileApi;
import kr.co.future.sslvpn.model.api.AuthorizedDeviceApi;
import kr.co.future.sslvpn.model.api.AuthorizedDeviceEventListener;
import kr.co.future.sslvpn.model.api.ClientAppApi;
import kr.co.future.sslvpn.model.api.ClientAppEventListener;
import kr.co.future.sslvpn.model.api.ClientCheckProfileApi;
import kr.co.future.sslvpn.model.api.ClientCheckProfileEventListener;
import kr.co.future.sslvpn.model.api.ClusteredIpLeaseApi;
import kr.co.future.sslvpn.model.api.DnsProxyApi;
import kr.co.future.sslvpn.model.api.ServerApi;
import kr.co.future.sslvpn.model.api.VpnServerConfigApi;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Invalidate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.apache.felix.ipojo.annotations.Validate;

import kr.co.future.api.KeyStoreManager;
import kr.co.future.api.PrimitiveConverter;
import kr.co.future.api.PrimitiveParseCallback;
import kr.co.future.api.PrimitiveSerializeCallback;
import kr.co.future.ca.BaseCertificateAuthorityListener;
import kr.co.future.ca.CertificateAuthority;
import kr.co.future.ca.CertificateAuthorityService;
import kr.co.future.ca.CertificateMetadata;
import kr.co.future.ca.RevocationReason;
import kr.co.future.confdb.Config;
import kr.co.future.confdb.ConfigDatabase;
import kr.co.future.confdb.ConfigService;
import kr.co.future.confdb.ConfigTransaction;
import kr.co.future.confdb.Predicates;
import kr.co.future.dom.api.ConfigManager;
import kr.co.future.dom.api.DefaultEntityEventListener;
import kr.co.future.dom.api.OrganizationUnitApi;
import kr.co.future.dom.api.TimetableApi;
import kr.co.future.dom.api.UserApi;
import kr.co.future.dom.model.OrganizationUnit;
import kr.co.future.dom.model.Timetable;
import kr.co.future.dom.model.User;
import kr.co.future.rpc.RpcAgent;
import kr.co.future.rpc.RpcClient;
import kr.co.future.rpc.RpcConnection;
import kr.co.future.rpc.RpcConnectionEventListener;
import kr.co.future.rpc.RpcConnectionProperties;
import kr.co.future.rpc.RpcSession;
import kr.co.future.sslvpn.core.xenics.XenicsService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;

@Component(name = "frodo-cluster")
@Provides
public class ClusterServiceImpl implements ClusterService {
	private final Logger logger = LoggerFactory.getLogger(ClusterServiceImpl.class.getName());

	@Requires
	private RpcAgent agent;

	@Requires
	private KeyStoreManager keyStoreManager;

	@Requires
	private ConfigService conf;

	@Requires
	private ClusteredIpLeaseApi leaseApi;

	@Requires
	private OrganizationUnitApi orgUnitApi;

	@Requires
	private UserApi userApi;

	@Requires
	private AuthorizedDeviceApi deviceApi;

	@Requires
	private ConfigManager domConf;

	@Requires
	private AccessProfileApi profileApi;

	@Requires
	private ClientAppApi appApi;

	@Requires
	private CertificateAuthorityService ca;

	@Requires
	private AccessGatewayApi gwApi;

	@Requires
	private ClientCheckProfileApi ccpApi;

	@Requires
	private ServerApi serverApi;

	@Requires
	private InstallerApi installer;

	@Requires
	private CertCenterApi certApi;

	@Requires
	private DnsProxyApi dnsApi;
	
	@Requires
	private XenicsService xenicsService;
	
	@Requires
	private TimetableApi timetableApi;
	
	@Requires
	private VpnServerConfigApi vpnServerConfigApi;

	private BatchUpdater batchUpdater;

	private LinkedBlockingDeque<ClusterOperation> replications;

	private long bootTimestamp;

	private ClusterConfig config;

	private ClusterNode master;

	private ClusterWatcher watcher;

	private AuthoritySynchronizer authoritySync;

	private ClientAppSynchronizer appSync;

	private ProfileSynchronizer profileSync;

	private AccessGatewaySynchronizer gwSync;
	
//	private VpnServerConfigSynchronizer vpnServerConfigSync;

	private ClientCheckProfileSynchronizer ccpSync;

	private ServerSynchronizer serverSync;

	private OrgUnitSynchronizer orgUnitSync;

	private UserSynchronizer userSync;

	private AuthDeviceSynchronizer deviceSync;

	private DnsZoneSynchronizer dnsSync;

	private ConcurrentMap<Integer, ClusterNode> nodes;

	private CopyOnWriteArraySet<ClusterEventListener> eventListeners;

	private CopyOnWriteArraySet<ClusterMessageListener> messageListeners;
	
	private TimetableSynchronizer timetableSync;
	
	public ClusterServiceImpl() {
		bootTimestamp = new Date().getTime();
		config = new ClusterConfig();
		nodes = new ConcurrentHashMap<Integer, ClusterNode>();
		eventListeners = new CopyOnWriteArraySet<ClusterEventListener>();
		messageListeners = new CopyOnWriteArraySet<ClusterMessageListener>();
		watcher = new ClusterWatcher();
		batchUpdater = new BatchUpdater();
		replications = new LinkedBlockingDeque<ClusterOperation>();

		appSync = new ClientAppSynchronizer();
		profileSync = new ProfileSynchronizer();
		gwSync = new AccessGatewaySynchronizer();
		orgUnitSync = new OrgUnitSynchronizer();
		userSync = new UserSynchronizer();
		deviceSync = new AuthDeviceSynchronizer();
		ccpSync = new ClientCheckProfileSynchronizer();
		serverSync = new ServerSynchronizer();
		authoritySync = new AuthoritySynchronizer();
		dnsSync = new DnsZoneSynchronizer();
		timetableSync = new TimetableSynchronizer();
//		vpnServerConfigSync = new VpnServerConfigSynchronizer();
	}

	@Validate
	@Override
	public void start() {
		// load config
		ConfigDatabase db = conf.ensureDatabase("frodo-cluster");
		Config c = db.findOne(ClusterConfig.class, null);
		if (c != null)
			this.config = c.getDocument(ClusterConfig.class);
		else
			this.config = new ClusterConfig();

		registerListeners();
		
		if (!config.isEnabled())
			return;

		for (ClusterNodeConfig nc : config.getNodes()) {
			try {
				nodes.put(nc.getNodeId(), new ClusterNode(nc.getNodeId(), new InetSocketAddress(nc.getIp(), nc.getPort()),
						InetAddress.getByName(nc.getPublicIp())));
			} catch (UnknownHostException e) {
				logger.error("frodo-cluster: error occured during nodes loading", e);
			}
		}
		
		if(config.isMaster())
			master = null;
				

		watcher.start();
		batchUpdater.start();
	}

	@Invalidate
	@Override
	public void stop() {
		unregisterListeners();

		batchUpdater.stop();
		watcher.stop();

		// now, current node is master
		master = null;
	}
	
	private void registerListeners() {
		registerUserDataListeners();
		registerListenersWhichNoUserData();
	}
	
	private void registerUserDataListeners() {
		if(config.getSyncExcludeUserData() == null || config.getSyncExcludeUserData() == false) {
			orgUnitApi.addEntityEventListener(orgUnitSync);
			userApi.addEntityEventListener(userSync);
		}
	}
	
	private void registerListenersWhichNoUserData() {
		if(config.getSyncUserDataOnly() == null || config.getSyncUserDataOnly() == false) {
			appApi.addListener(appSync);
			profileApi.addEntityEventListener(profileSync);		
			deviceApi.addListener(deviceSync);
			gwApi.addEntityEventListener(gwSync);
			ccpApi.addListener(ccpSync);
			serverApi.addEntityEventListener(serverSync);
			ca.addListener(authoritySync);
			dnsApi.addEntityEventListener(dnsSync);
			timetableApi.addEntityEventListener(timetableSync);
//			vpnServerConfigApi.addEntityEventListener(vpnServerConfigSync);
		}
	}
	
	private void unregisterListeners() {
		unregisterUserDataListeners();
		unregisterListenersWhichNoUserData();
	}
	
	private void unregisterUserDataListeners() {
		if (orgUnitApi != null)
			orgUnitApi.removeEntityEventListener(orgUnitSync);

		if (userApi != null)
			userApi.removeEntityEventListener(userSync);
	}
	
	private void unregisterListenersWhichNoUserData() {
		if (appApi != null)
			appApi.removeListener(appSync);

		if (profileApi != null)
			profileApi.removeEntityEventListener(profileSync);

		if (deviceApi != null)
			deviceApi.removeListener(deviceSync);

		if (gwApi != null)
			gwApi.removeEntityEventListener(gwSync);

		if (ccpApi != null)
			ccpApi.removeListener(ccpSync);

		if (serverApi != null)
			serverApi.removeEntityEventListener(serverSync);

		if (ca != null)
			ca.addListener(authoritySync);

		if (dnsApi != null)
			dnsApi.removeEntityEventListener(dnsSync);
		
		if (timetableApi != null)
			timetableApi.removeEntityEventListener(timetableSync);
		
//		if (vpnServerConfigApi != null)
//			vpnServerConfigApi.removeEntityEventListener(vpnServerConfigSync);
	}

	@Override
	public long getBootTimestamp() {
		return bootTimestamp;
	}

	@Override
	public ClusterConfig getConfig() {
		return ClusterConfig.duplicate(config);
	}

	@Override
	public void setConfig(ClusterConfig newConfig) {
		ClusterConfig old = this.config;
		this.config = newConfig;

		ConfigDatabase db = conf.ensureDatabase("frodo-cluster");
		Config c = db.findOne(ClusterConfig.class, null);
		if (c == null) {
			db.add(newConfig);
		} else {
			db.update(c, newConfig);
		}

		// apply new status change
		logger.info("frodo core: cluster config change, old [{}], new [{}]", old.isEnabled(), newConfig.isEnabled());

		if (!old.isEnabled() && newConfig.isEnabled()){
			start();
		}
		else if (old.isEnabled() && !newConfig.isEnabled())
			stop();

	}
	
	@Override
    public void setConfigWithNoStart(ClusterConfig newConfig) {
    	this.config = newConfig;
    }
	
	@Override
	public void setConfigWithRegisterListenersBySyncUserDataOnly(ClusterConfig newConfig) {
		this.config = newConfig;
		if(config.isEnabled()) {
			if(config.getSyncUserDataOnly() != null && config.getSyncUserDataOnly())
				unregisterListenersWhichNoUserData();
	    	else {
	    		unregisterListenersWhichNoUserData();
	    		registerListenersWhichNoUserData();
	    	}
		}
	}
	
	@Override
	public void setConfigWithExcludeUserData(ClusterConfig newConfig) {
		this.config = newConfig;
		if(config.isEnabled()) {
			if(config.getSyncExcludeUserData() != null && config.getSyncExcludeUserData()) 
				unregisterUserDataListeners();
			else {
				unregisterUserDataListeners();
				registerUserDataListeners();
			}
		}
	}

	@Override
	public Collection<ClusterNode> getClusterNodes() {
		return nodes.values();
	}

	@Override
	public ClusterNode getClusterNode(int id) {
		return nodes.get(id);
	}

	@Override
	public ClusterNode getClusterMaster() {
		return master;
	}

	private void ensureConnections() {
		for (ClusterNode node : getClusterNodes())
			ensureConnection(node);
	}

	private void ensureConnection(ClusterNode node) {
		if (node.getClient() != null)
			return;

		RpcClient client = null;
		try {
			KeyManagerFactory kmf = keyStoreManager.getKeyManagerFactory("rpc-agent", "SunX509");
			TrustManagerFactory tmf = keyStoreManager.getTrustManagerFactory("rpc-ca", "SunX509");
			client = new RpcClient(agent.getGuid());

			RpcConnectionProperties p = new RpcConnectionProperties(node.getAddress(), kmf, tmf);
			RpcConnection conn = client.connectSsl(p);
			conn.bind("frodo-cluster-slave", new ClusterSlaveRpcService(this));
			RpcSession session = conn.createSession("frodo-cluster-master");
			session.post("connect", config.getNodeId());
			node.setAvailable(true);
			node.setClient(client);
			node.setSession(session);
			conn.addListener(new NodeDisconnector(node));
			logger.info("frodo core: connected to node [" + node + "]");
		} catch (Throwable t) {
			logger.error("frodo core: cannot connect to node [" + node + "]", t);
			if (client != null)
				client.close();
		}
	}

	@Override
	public void updateClusterTunnelCount() {
		// try update
		for (ClusterNode node : getClusterNodes()) {
			updateTunnelCount(node);
		}
		logger.trace("frodo core: updated cluster node count");
	}

	public void updateClusterMaster() throws Exception {
		ensureConnections();
		
		int retryCnt = 3;
		
		for (ClusterNode node : getClusterNodes()){
			RpcSession session = node.getSession();
			if (session == null) {
				logger.trace("frodo core: cluster node [{}]'s session not found", node.getId());
				return;
			}
			boolean isMaster = false;
			for (int i = 0; i < retryCnt; i++) {
				logger.trace("frodo core: get cluster node [{}]'s session try [{}]", node.getId(), i);
				boolean connectResutl = false;
				try {
					isMaster = (Boolean) session.call("isMaster", new Object[] {}, 3000);
					logger.trace("frodo core: cluster node [{}] boot [{}] tunnel [{}]", node.getAddress(), node.getTimestamp());

					connectResutl = true;
				} catch (Throwable e) {
					logger.trace("frodo core: get cluster node [{}]'s session try [{}] fail", node.getId(), i);
					continue;
				}

				if (connectResutl == true) {
					break;
				}
			}
			
			if(isMaster) {
				//request master data when master changed to other node.
				if(this.master == null || !master.getPublicIp().equals(node.getPublicIp())){
					logger.trace("frodo core: found master [{}]", node);
					notifyMasterChange(node);
					this.master = node;
				}
				return;
			}
		}

    }
	
//	@Override
//	public void updateClusterMaster() {
//		ClusterNode newMaster = null;
//		long lastTimestamp = getBootTimestamp();
//
//		ensureConnections();
//
//		// try update
//		for (ClusterNode node : getClusterNodes()) {
//			updateBootTimestamp(node);
//		}
//
//		// find node which has minimum boot timestamp
//		for (ClusterNode node : getClusterNodes()) {
//			if (node.isAvailable() && node.getTimestamp().getTime() < lastTimestamp) {
//				lastTimestamp = node.getTimestamp().getTime();
//				newMaster = node;
//			}
//		}
//
//		if (this.master == null && newMaster != null)
//			notifyMasterChange(newMaster);
//		else if (this.master != null && newMaster == null)
//			notifyMasterChange(null);
//		else if (this.master != null && newMaster != null && !this.master.equals(newMaster))
//			notifyMasterChange(newMaster);
//
//		this.master = newMaster;
//		logger.trace("frodo core: updated cluster master node [{}]", master);
//	}

	private void notifyMasterChange(ClusterNode master) {
		logger.info("frodo core: cluster master is changed [{}]", master);

		for (ClusterEventListener listener : eventListeners) {
			try {
				listener.onMasterChange(master);
			} catch (Throwable t) {
				logger.error("frodo core: cluster event listener should not throw any exception", t);
			}
		}
	}

//	private void updateBootTimestamp(ClusterNode node) {
//		int retryCnt = 3;
//		
//		RpcSession session = node.getSession();
//		
//		if (session == null) {
//			logger.trace("frodo core: cluster node [{}]'s session not found", node.getId());
//			return;
//		}
//
//		long timestamp = 0L;
//		boolean connectResutl = false;
//		
//		for (int i = 0; i < retryCnt; i++) {
//			logger.trace("frodo core: get cluster node [{}]'s session try [{}]", node.getId(), i);
//			
//			try {
//				timestamp = (Long) session.call("getBootTimestamp", new Object[] {}, 3000);
//				
//				node.setAvailable(true);
//				node.setTimestamp(new Date(timestamp));
//				logger.trace("frodo core: cluster node [{}] boot [{}] tunnel [{}]", node.getAddress(), node.getTimestamp());
//				
//				connectResutl = true;
//			} catch (Throwable e) {
//				logger.trace("frodo core: get cluster node [{}]'s session try [{}] fail", node.getId(), i);
//				continue;
//			}
//			
//			if (connectResutl == true) {
//				break;
//			}
//		}
//		
//		if (connectResutl == false) {
//			node.setAvailable(false);
//			node.setTimestamp(null);
//			logger.error("frodo core: cannot update cluster node [" + node + "] boot timestamp");
//
//			if (node.getClient() != null) {
//				node.getClient().close();
//				node.setClient(null);
//				node.setSession(null);
//			}
//		}
//
//		return;
//	}

	private void updateTunnelCount(ClusterNode node) {
		try {
			RpcSession session = node.getSession();
			if (session == null) {
				logger.trace("frodo core: cluster node [{}]'s session not found", node.getId());
				return;
			}

			int tunnelCount = (Integer) session.call("getTunnelCount", new Object[] {}, 5000);
			node.setTunnelCount(tunnelCount);
			logger.trace("frodo core: cluster node [{}] tunnel [{}]", node.getAddress(), node.getTunnelCount());
		} catch (Throwable e) {
			node.setTunnelCount(0);
			logger.error("frodo core: cannot update cluster node [" + node + "] tunnel count", e);

			if (node.getClient() != null) {
				node.getClient().close();
				node.setClient(null);
				node.setSession(null);
			}
		}
	}

	@Override
	public void addNode(ClusterNodeConfig nodeConfig) {
		if (nodes.containsKey(nodeConfig.getNodeId()))
			throw new IllegalStateException("duplicated node id: " + nodeConfig.getNodeId());

		ClusterNode node = null;
		try {
			node = new ClusterNode(nodeConfig.getNodeId(), new InetSocketAddress(nodeConfig.getIp(), nodeConfig.getPort()),
					InetAddress.getByName(nodeConfig.getPublicIp()));
		} catch (UnknownHostException e) {
			throw new IllegalArgumentException("invalid public ip address");
		}
		nodes.put(node.getId(), node);

		// update confdb
		ConfigDatabase db = conf.ensureDatabase("frodo-cluster");
		ClusterConfig cc = getConfig();
		cc.getNodes().add(nodeConfig);

		Config c = db.findOne(ClusterConfig.class, null);
		if (c != null) {
			db.update(c, cc);
		} else {
			db.add(cc);
		}

		this.config = cc;
	}

	@Override
	public void removeNode(int id) {
		ClusterNode node = nodes.remove(id);
		if (node == null)
			return;
		
		removeNode(node);

		// update confdb
		ConfigDatabase db = conf.ensureDatabase("frodo-cluster");
		ClusterConfig cc = getConfig();
		cc.getNodes().remove(new ClusterNodeConfig(id));

		Config c = db.findOne(ClusterConfig.class, null);
		if (c != null) {
			db.update(c, cc);
		} else {
			db.add(cc);
		}

		this.config = cc;
	}
	
	//to remove rpc connection.
	private void removeNode(ClusterNode node) {
		node.setAvailable(false);
		node.setTimestamp(null);
		logger.trace("frodo core: cluster node [" + node + "] has removed.");

		if (node.getClient() != null) {
			node.getClient().close();
			node.setClient(null);
			node.setSession(null);
		}
	}

	@Override
	public void addEventListener(ClusterEventListener listener) {
		eventListeners.add(listener);
	}

	@Override
	public void removeEventListener(ClusterEventListener listener) {
		eventListeners.remove(listener);
	}

	@Override
	public void addMessageListener(ClusterMessageListener listener) {
		messageListeners.add(listener);
	}

	@Override
	public void removeMessageListener(ClusterMessageListener listener) {
		messageListeners.remove(listener);
	}

	@Override
	public int leaseIp(String loginName, String profileName, int poolSize) {
		ClusterNode currentMaster = getClusterMaster();
		if (currentMaster == null)
			return leaseApi.lease(loginName, profileName, poolSize);

		RpcClient client = null;
		try {
			KeyManagerFactory kmf = keyStoreManager.getKeyManagerFactory("rpc-agent", "SunX509");
			TrustManagerFactory tmf = keyStoreManager.getTrustManagerFactory("rpc-ca", "SunX509");
			client = new RpcClient(agent.getGuid());

			RpcConnectionProperties p = new RpcConnectionProperties(currentMaster.getAddress(), kmf, tmf);
			RpcConnection conn = client.connectSsl(p);
			RpcSession session = conn.createSession("frodo-cluster-master");
			int offset = (Integer) session.call("lease", new Object[] { loginName, profileName, poolSize }, 2000);
			leaseApi.record(loginName, profileName, poolSize, offset);

			if (logger.isTraceEnabled())
				logger.trace("frodo core: cluster lease offset [{}] for login [{}], profile [{}], pool size [{}] from [{}]",
						new Object[] { offset, loginName, profileName, poolSize, currentMaster });

			return offset;
		} catch (Throwable e) {
			logger.error("frodo core: cluster lease fail, master node [" + currentMaster + "], login [" + loginName + "]", e);
		} finally {
			if (client != null)
				client.close();
		}

		return -1;
	}

	private void replicate(ClusterOperation op) {
		if (!config.isEnabled())
			return;

		logger.debug("frodo core: replicating [{}]", op);

		for (ClusterMessageListener listener : messageListeners) {
			try {
				logger.debug("frodo core: replicate to [{}]", listener.getNodeId());
				listener.onMessage(op);
			} catch (Throwable t) {
				logger.error("frodo core: cannot replicate cluster event - " + op, t);
			}
		}
	}

	@Override
	public void submitUpdate(ClusterOperation op) {
		replications.add(op);
	}

	private static class NodeDisconnector implements RpcConnectionEventListener {
		private ClusterNode node;

		public NodeDisconnector(ClusterNode node) {
			this.node = node;
		}

		@Override
		public void connectionOpened(RpcConnection connection) {
		}

		@Override
		public void connectionClosed(RpcConnection connection) {
			if (node.getClient() != null)
				node.getClient().close();

			node.setAvailable(false);
			node.setClient(null);
			node.setSession(null);
		}

	}
	
	private class BatchUpdater implements Runnable {
		private Thread thread;
		private boolean doStop;

		public void start() {
			if (thread != null)
				return;

			thread = new Thread(this, "Frodo Cluster Batch Updater");
			thread.start();
		}

		public void stop() {
			if (thread == null)
				return;

			doStop = true;
			thread.interrupt();
			thread = null;
		}

		@Override
		public void run() {
			doStop = false;
			logger.info("frodo core: cluster batch updater started, thread [{}]", Thread.currentThread().getId());

			try {
				while (!doStop) {
					try {
						updateClusterMaster();
						
						batchUpdate();
						//changed from 2 to 20 seconds because of performance.
						Thread.sleep(20000);
					} catch (InterruptedException e) {
					} catch (Throwable t) {
						logger.error("frodo core: cluster watcher error", t);
					}
				}

			} finally {
				logger.info("frodo core: cluster batch updater stopped, thread [{}]", Thread.currentThread().getId());
			}
		}

		private void batchUpdate() {
			Date begin = new Date();
			ArrayList<ClusterOperation> ops = new ArrayList<ClusterOperation>(replications.size() * 2);
			replications.drainTo(ops);

			if (ops.size() == 0)
				return;

			//use map or set to prevent duplicated creation, update and remove.
			Map<String, OrganizationUnit> createOrgUnits = new HashMap<String, OrganizationUnit>();
			Map<String, OrganizationUnit> updateOrgUnits = new HashMap<String, OrganizationUnit>();
			Set<String> removeOrgUnits = new HashSet<String>();

			Map<String, AuthorizedDevice> createDevices = new HashMap<String, AuthorizedDevice>();
			Map<String, AuthorizedDevice> updateDevices = new HashMap<String, AuthorizedDevice>();
			Set<String> removeDevices = new HashSet<String>();

			List<ClusterOperation> jsonUsers = new ArrayList<ClusterOperation>();

			Map<String, User> createUsers = new HashMap<String, User>();
			Map<String, User> updateUsers = new HashMap<String, User>();
			Set<String> removeUsers = new HashSet<String>();

			List<CertificateMetadata> issueCerts = new ArrayList<CertificateMetadata>();
			List<Object> revokeCerts = new ArrayList<Object>();

			AccessGateway updateGw = null;

			Map<String, DnsZone> createDnsZones = new HashMap<String, DnsZone>();
			Map<String, DnsZone> updateDnsZones = new HashMap<String, DnsZone>();
			Set<String> removeDnsZones = new HashSet<String>();
         
			Map<String, Timetable> createTimetable = new HashMap<String, Timetable>();
            Map<String, Timetable> updateTimetable = new HashMap<String, Timetable>();
            Set<String> removeTimetable = new HashSet<String>();
            
            Map<String, AccessProfile> createAccessProfiles = new HashMap<String, AccessProfile>();
            Map<String, AccessProfile> updateAccessProfiles = new HashMap<String, AccessProfile>();
            Set<String> removeAccessProfiles = new HashSet<String>();
            
            Map<String, ClientApp> createClientApps = new HashMap<String, ClientApp>();
            Map<String, ClientApp> updateClientApps = new HashMap<String, ClientApp>();
            Set<String> removeClientApps = new HashSet<String>();
            
            Map<String, ClientCheckProfile> createClientCheckProfiles = new HashMap<String, ClientCheckProfile>();
            Map<String, ClientCheckProfile> updateClientCheckProfiles = new HashMap<String, ClientCheckProfile>();
            Set<String> removeClientCheckProfiles = new HashSet<String>();
            
            Map<String, Server> createServers = new HashMap<String, Server>();
            Map<String, Server> updateServers = new HashMap<String, Server>();
            Set<String> removeServers = new HashSet<String>();
            
//            VpnServerConfig updateVpnServerConfig = null;

			Map<String, OrganizationUnit> currentOrgUnits = new HashMap<String, OrganizationUnit>();
			for (OrganizationUnit orgUnit : orgUnitApi.getOrganizationUnits("localhost"))
				currentOrgUnits.put(orgUnit.getGuid(), orgUnit);

			try {
				ClusterContext.replicated.set(true);
				for (ClusterOperation op : ops) {
					String category = op.getCategory();
					String operation = op.getOperation();
					Object obj = op.getObj();

					logger.trace("frodo core: recv category [{}], operation [{}]", category, operation);

					if (category.equals("user")) {
						@SuppressWarnings("unchecked")
						Map<String, Object> m = (Map<String, Object>) obj;
						String loginName = (String) m.get("login_name");

						// CAUTION: parse after organization unit commit
						if (operation.equals("create") || operation.equals("update")) {
							jsonUsers.add(op);
						} else if (operation.equals("remove")) {
							removeUsers.add(loginName);
						}

					} else if (category.equals("auth_device")) {
						AuthorizedDevice device = PrimitiveConverter.parse(AuthorizedDevice.class, obj);
						if (operation.equals("register")) {
							createDevices.put(device.getGuid(), device);
						} else if (operation.equals("unregister")) {
							removeDevices.add(device.getGuid());
						} else if (operation.equals("update")) {
							updateDevices.put(device.getGuid(), device);
						}
					} else if (category.equals("org_unit")) {
						OrganizationUnit orgUnit = PrimitiveConverter.parse(OrganizationUnit.class, obj);

						if (operation.equals("create")) {
							logger.trace("frodo core: recv create org unit [{}]", orgUnit);
							if (!currentOrgUnits.containsKey(orgUnit.getGuid()))
								createOrgUnits.put(orgUnit.getGuid(), orgUnit);
							else
								logger.warn("frodo core: recv duplicated create org unit [{}]", orgUnit);
						} else if (operation.equals("update")) {
							logger.trace("frodo core: recv update org unit [{}]", orgUnit);
							updateOrgUnits.put(orgUnit.getGuid(), orgUnit);
						} else if (operation.equals("remove")) {
							logger.trace("frodo core: recv remove org unit [{}]", orgUnit);
							removeOrgUnits.add(orgUnit.getGuid());
						}
					} else if (category.equals("access_profile")) {
						AccessProfile profile = PrimitiveConverter.parse(AccessProfile.class, obj, accessProfileParseCallback());
						if (operation.equals("create")) {
							logger.trace("frodo core: recv create access profile [{}]", profile);
							createAccessProfiles.put(profile.getGuid(), profile);
						} else if (operation.equals("update")) {
							logger.trace("frodo core: recv update access profile [{}]", profile);
							updateAccessProfiles.put(profile.getGuid(), profile);
						} else if (operation.equals("remove")) {
							logger.trace("frodo core: recv remove access profile [{}]", profile);
							removeAccessProfiles.add(profile.getGuid());
						}
					} else if (category.equals("client_app")) {
						ClientApp app = PrimitiveConverter.parse(ClientApp.class, obj);
						if (operation.equals("create")) {
							logger.trace("frodo core: recv create client app [{}]", app);
							createClientApps.put(app.getGuid(), app);
						} else if (operation.equals("update")) {
							logger.trace("frodo core: recv update client app [{}]", app);
							updateClientApps.put(app.getGuid(), app);
						} else if (operation.equals("remove")) {
							logger.trace("frodo core: recv remove client app [{}]", app);
							removeClientApps.add(app.getGuid());
						}
					} else if (category.equals("cert")) {
						if (operation.equals("issue")) {
							CertificateMetadata req = PrimitiveConverter.parse(CertificateMetadata.class, obj);
							issueCerts.add(req);
						} else if (operation.equals("revoke")) {
							revokeCerts.add(obj);
						}
					} else if (category.equals("access_gateway")) {
						AccessGateway gw = PrimitiveConverter.parse(AccessGateway.class, obj);
						updateGw = gw;
					} else if (category.equals("client_check_profile")) {
						ClientCheckProfile profile = PrimitiveConverter.parse(ClientCheckProfile.class, obj);
						if (operation.equals("create")) {
							logger.trace("frodo core: recv create client check profile [{}]", obj);
							createClientCheckProfiles.put(profile.getGuid(), profile);
						} else if (operation.equals("update")) {
							logger.trace("frodo core: recv update client check profile [{}]", obj);
							updateClientCheckProfiles.put(profile.getGuid(), profile);
						} else if (operation.equals("remove")) {
							logger.trace("frodo core: recv remove client check profile [{}]", obj);
							removeClientCheckProfiles.add(profile.getGuid());
						}
					} else if (category.equals("server")) {
						Server server = PrimitiveConverter.parse(Server.class, obj);
						if (operation.equals("create")) {
							logger.trace("frodo core: recv create server [{}]", obj);
							createServers.put(server.getGuid(), server);
						} else if (operation.equals("update")) {
							logger.trace("frodo core: recv update server [{}]", obj);
							updateServers.put(server.getGuid(), server);
						} else if (operation.equals("remove")) {
							logger.trace("frodo core: recv remove server [{}]", obj);
							removeServers.add(server.getGuid());
						}
					} else if (category.equals("dns_zone")) {
						DnsZone zone = PrimitiveConverter.parse(DnsZone.class, obj);
						if (operation.equals("create")) {
							try {
								createDnsZones.put(zone.getDomain(), zone);
							} catch (Throwable t) {
								logger.error("frodo core: create dns zone failed", t);
							}
						} else if (operation.equals("update")) {
							try {
								updateDnsZones.put(zone.getDomain(), zone);
							} catch (Throwable t) {
								logger.error("frodo core: update dns zone failed", t);
							}
						} else if (operation.equals("remove")) {
							try {
								removeDnsZones.add(zone.getDomain());
							} catch (Throwable t) {
								logger.error("frodo core: remove dns zone failed", t);
							}
						}
					} else if (category.equals("timetable")) {
						Timetable timetable = PrimitiveConverter.parse(Timetable.class, obj);
						if(operation.equals("create")) {
							try {
								createTimetable.put(timetable.getGuid(), timetable);
							} catch (Throwable t) {
								logger.error("frodo core: create timetable failed", t);
							}
						} else if (operation.equals("update")) {
							try {
								updateTimetable.put(timetable.getGuid(), timetable);
							} catch (Throwable t) {
								logger.error("frodo core: update timetable failed", t);
							}
						} else if (operation.equals("remove")) {
							try {
								removeTimetable.add(timetable.getGuid());
							} catch (Throwable t) {
								logger.error("frodo core: remove timetable failed", t);
							}
						}
					} 
//					else if (category.equals("vpn_server_config")) {
//						VpnServerConfig vpnServerConfig = PrimitiveConverter.parse(VpnServerConfig.class, obj);
//						updateVpnServerConfig = vpnServerConfig;
//					}
				}

				// replicate org units
				handleOrgUnitBatchUpdate(createOrgUnits, updateOrgUnits, removeOrgUnits);
				
				// replicate access profiles
				handleAccessProfileBatchUpdate(createAccessProfiles, updateAccessProfiles, removeAccessProfiles);
				
				// replicate client apps
				handleClientAppBatchUpdate(createClientApps, updateClientApps, removeClientApps);
				
				// replicate client check profiles
				handleClientCheckProfileBatchUpdate(createClientCheckProfiles, updateClientCheckProfiles, removeClientCheckProfiles);
				
				// replicate servers
				handleServerBatchUpdate(createServers, updateServers, removeServers);
			
				// replicate auth devices
				handleDeviceBatchUpdate(createDevices, updateDevices, removeDevices);
				
				//replicate timetables
				handleTimetableBatchUpdate(createTimetable, updateTimetable, removeTimetable);

				// replicate users
				handleUserBatchUpdate(jsonUsers, createUsers, updateUsers, removeUsers);

				//replicate certificates
				handleCertificateBatchUpdate(issueCerts, revokeCerts);
				
				//replicate dns zones
				handleDnsZoneBatchUpdate(createDnsZones, updateDnsZones, removeDnsZones);

				if (updateGw != null) {
					try {
						gwApi.updateAccessGateway(updateGw);
						logger.trace("frodo core: update [{}] acccess gateway", updateGw);
					} catch (Throwable t) {
						logger.error("frodo core: cluster batch update failed", t);
					}
				}
				
//				if (updateVpnServerConfig != null) {
//					try {
//						vpnServerConfigApi.updateVpnServerConfig(updateVpnServerConfig);
//						logger.trace("frodo core: update [{}] vpn server config", updateGw);
//					} catch (Throwable t) {
//						logger.error("frodo core: cluster batch update failed", t);
//					}
//				}
			} finally {
				ClusterContext.replicated.set(false);

				long elapsed = new Date().getTime() - begin.getTime();
				logger.trace("frodo core: [{}]ms elapsed for cluster batch update", elapsed);
			}
		}
		
		private void handleDeviceBatchUpdate(Map<String, AuthorizedDevice> createDevices, Map<String, AuthorizedDevice> updateDevices, Set<String> removeDevices) {
        	if (createDevices.size() > 0) {
                try {
                    deviceApi.registerDevices(new ArrayList<AuthorizedDevice>(createDevices.values()));
                    logger.trace("frodo core: registered [{}] auth devices", createDevices.size());
                } catch (Throwable t) {
                    logger.error("frodo core: cluster batch update failed", t);
                }
            }

            if (updateDevices.size() > 0) {
                try {
                    deviceApi.updateDevices(new ArrayList<AuthorizedDevice>(updateDevices.values()));
                    logger.trace("frodo core: updated [{}] auth devices", updateDevices.size());
                } catch (Throwable t) {
                    logger.error("frodo core: cluster batch update failed", t);
                }
            }

            if (removeDevices.size() > 0) {
                try {
                    deviceApi.unregisterDevices(removeDevices);
                    logger.trace("frodo core: unregistered [{}] auth devices", removeDevices.size());
                } catch (Throwable t) {
                    logger.error("frodo core: cluster batch update failed", t);
                }
            }
        }
		
		private void handleAccessProfileBatchUpdate(Map<String, AccessProfile> createAccessProfiles, Map<String, AccessProfile> updateAccessProfiles, Set<String> removeAccessProfiles) {
			if(createAccessProfiles.size() > 0) {
				try {
					for(AccessProfile profile : createAccessProfiles.values()) 
						profileApi.createAccessProfile(profile);
				} catch (Throwable t) {
					logger.error("frodo core: create access profile failed", t);
				}
			}
			
			if(updateAccessProfiles.size() > 0) {
				try {
					for(AccessProfile profile : updateAccessProfiles.values()) 
						profileApi.updateAccessProfile(profile);
				} catch (Throwable t) {
					logger.error("frodo core: update access profile failed", t);
				}
			}
			
			if(removeAccessProfiles.size() > 0) {
				try {
					for(String guid : removeAccessProfiles)
						profileApi.removeAccessProfile(guid);
				} catch (Throwable t) {
					logger.error("frodo core: remove access profile failed", t);
				}
			}
		}
		
		private void handleClientAppBatchUpdate(Map<String, ClientApp> createClientApps, Map<String, ClientApp> updateClientApps, Set<String> removeClientApps) {
			if(createClientApps.size() > 0) {
				try {
					for(ClientApp app : createClientApps.values())
						appApi.createClientApp(app);
				} catch (Throwable t) {
					logger.error("frodo core: create client app failed", t);
				}
			}
			
			if(updateClientApps.size() > 0) {
				try {
					for(ClientApp app : updateClientApps.values())
						appApi.updateClientApp(app);
				} catch (Throwable t) {
					logger.error("frodo core: update client app failed", t);
				}
			}
			
			if(removeClientApps.size() > 0) {
				try {
					for(String guid : removeClientApps)
						appApi.removeClientApp(guid);
				} catch (Throwable t) {
					logger.error("frodo core: remove client app failed", t);
				}
			}
		}
		
		private void handleClientCheckProfileBatchUpdate(Map<String, ClientCheckProfile> createClientCheckProfiles, Map<String, ClientCheckProfile> updateClientCheckProfiles, Set<String> removeClientCheckProfiles) {
			if(createClientCheckProfiles.size() > 0) {
				try {
					for(ClientCheckProfile profile : createClientCheckProfiles.values())
						ccpApi.createClientCheckProfile(profile);
				} catch (Throwable t) {
					logger.error("frodo core: create client check profile failed", t);
				}
			}
			
			if(updateClientCheckProfiles.size() > 0) {
				try {
					for(ClientCheckProfile profile : updateClientCheckProfiles.values())
						ccpApi.updateClientCheckProfile(profile);
				} catch (Throwable t) {
					logger.error("frodo core: update client check profile failed", t);
				}
			}
			
			if(removeClientCheckProfiles.size() > 0) {
				try {
					for(String guid : removeClientCheckProfiles)
						ccpApi.removeClientCheckProfile(guid);
				} catch (Throwable t) {
					logger.error("frodo core: remove client check profile failed", t);
				}
			}
		}
		
		private void handleServerBatchUpdate(Map<String, Server> createServers, Map<String, Server> updateServers, Set<String> removeServers) {
			if(createServers.size() > 0) {
				try {
					for(Server server : createServers.values())
						serverApi.createServer(server);
				} catch (Throwable t) {
					logger.error("frodo core: create server failed", t);
				}
			}
		
			if(updateServers.size() > 0) {
				try {
					for(Server server : updateServers.values())
						serverApi.updateServer(server);
				} catch (Throwable t) {
					logger.error("frodo core: update server failed", t);
				}
			}
			
			if(removeServers.size() > 0) {
				try {
					for(String guid : removeServers)
						serverApi.removeServer(guid);
				} catch (Throwable t) {
					logger.error("frodo core: remove server failed", t);
				}
			}
		}
		
		private void handleTimetableBatchUpdate(Map<String, Timetable> createTimetable, Map<String, Timetable> updateTimetable, Set<String> removeTimetable) {
			if (createTimetable.size() > 0) {
				try {
					timetableApi.createTimetables("localhost", createTimetable.values());
					logger.trace("frodo core: created [{}] timetables", createTimetable.size());
				} catch (Throwable t) {
					logger.error("frodo core: cluster batch update failed", t);
				}
			}
			
			if (updateTimetable.size() > 0) {
				try {
					timetableApi.updateTimetables("localhost", updateTimetable.values());
					logger.trace("frodo core: updated [{}] timetables", updateTimetable.size());
				} catch (Throwable t) {
					logger.error("frodo core: cluster batch update failed", t);
				}
			}
			
			if (removeTimetable.size() > 0) {
				try {
					timetableApi.removeTimetables("localhost", removeTimetable);
					logger.trace("frodo core: updated [{}] timetables", updateTimetable.size());
				} catch (Throwable t) {
					logger.error("frodo core: cluster batch update failed", t);
				}
			}
		}
		
		private void handleOrgUnitBatchUpdate(Map<String, OrganizationUnit> createOrgUnits, Map<String, OrganizationUnit> updateOrgUnits, Set<String> removeOrgUnits) {
        	if (createOrgUnits.size() > 0) {
                try {
                    orgUnitApi.createOrganizationUnits("localhost", createOrgUnits.values());
                    logger.trace("frodo core: created [{}] org units", createOrgUnits.size());
                } catch (Throwable t) {
                    logger.error("frodo core: cluster batch update failed", t);
                }
            }

            if (updateOrgUnits.size() > 0) {
                try {
                    orgUnitApi.updateOrganizationUnits("localhost", updateOrgUnits.values());
                    logger.trace("frodo core: updated [{}] org units", updateOrgUnits.size());
                } catch (Throwable t) {
                    logger.error("frodo core: cluster batch update failed", t);
                }
            }

            if (removeOrgUnits.size() > 0) {
                try {
                    orgUnitApi.removeOrganizationUnits("localhost", removeOrgUnits);
                    logger.trace("frodo core: removed [{}] org units", removeOrgUnits.size());
                } catch (Throwable t) {
                    logger.error("frodo core: cluster batch update failed", t);
                }
            }
        }
        
        private void handleUserBatchUpdate(List<ClusterOperation> jsonUsers, Map<String, User> createUsers, Map<String, User> updateUsers, Set<String> removeUsers) {
            // use map to prevent duplicated user creation
            for (ClusterOperation op : jsonUsers) {
                User user = PrimitiveConverter.parse(User.class, op.getObj());
                String loginName = user.getLoginName();
                
                String name = userApi.getUserNameByLoginName(loginName);

                if (Strings.isNullOrEmpty(name)) {
                	createUsers.put(loginName, user);
                }
                else {
                	updateUsers.put(loginName, user);
                }
            }

            if (createUsers.size() > 0) {
                try {
                    userApi.createUsers("localhost", createUsers.values(), true);
                    logger.trace("frodo core: created [{}] users", createUsers.size());
                } catch (Throwable t) {
                    logger.error("frodo core: cluster batch update failed", t);
                }
            }

            if (updateUsers.size() > 0) {
                try {                	
                    userApi.updateUsers("localhost", updateUsers.values(), false);
                    logger.trace("frodo core: updated [{}] users", updateUsers.size());
                } catch (Throwable t) {
                    logger.error("frodo core: cluster batch update failed", t);
                }
            }

            if (removeUsers.size() > 0) {
                try {
                    userApi.removeUsers("localhost", removeUsers);
                    logger.trace("frodo core: removed [{}] users", removeUsers.size());
                } catch (Throwable t) {
                    logger.error("frodo core: cluster batch update failed", t);
                }
            }
        }

		  private void handleDnsZoneBatchUpdate(Map<String, DnsZone> createDnsZones, Map<String, DnsZone> updateDnsZones,
		        Set<String> removeDnsZones) {
			  if (createDnsZones.size() > 0) {
				  try {
					  for (DnsZone zone : createDnsZones.values()){
						  dnsApi.createDnsZone(zone);
					  }
					  logger.trace("frodo core: create [{}] dns zones", createDnsZones.size());
				  } catch (Throwable t) {
					  logger.error("frodo core: cluster batch update failed", t);
				  }
			  }
			  if (updateDnsZones.size() > 0) {
				  try {
					  for (DnsZone zone : updateDnsZones.values())
						  dnsApi.updateDnsZone(zone);
					  logger.trace("frodo core: update [{}] dns zones", updateDnsZones.size());
				  } catch (Throwable t) {
					  logger.error("frodo core: cluster batch update failed", t);
					  }
				  }
			  
			  if (removeDnsZones.size() > 0) {
				  try {
					  dnsApi.removeDnsZones(new ArrayList<String>(removeDnsZones));
					  logger.trace("frodo core: remove [{}] dns zones", removeDnsZones.size());
				  } catch (Throwable t) {
					  logger.error("frodo core: cluster batch update failed", t);
				  }
			  }
		  }

		private void handleCertificateBatchUpdate(List<CertificateMetadata> issueCerts, List<Object> revokeCerts) {
			CertificateAuthority authority = ca.getAuthority(kr.co.future.sslvpn.core.Config.Cert.caCommonName);
			if (authority == null) {
				logger.error("frodo core: cannot sync certificates, authority not found");
				return;
			}

			if (issueCerts.size() > 0) {
				try {
					for (CertificateMetadata cm : issueCerts)
						authority.importCertificate(cm);
					logger.trace("frodo core: issue [{}] certs", issueCerts.size());
				} catch (Throwable t) {
					logger.error("frodo core: cluster batch update failed", t);
				}
			}

			if (revokeCerts.size() > 0) {
				try {
					for (Object obj : revokeCerts) {
						Object objs[] = (Object[]) obj;
						CertificateMetadata cm = PrimitiveConverter.parse(CertificateMetadata.class, objs[0]);
						RevocationReason reason = RevocationReason.valueOf((String) objs[1]);
						authority.revoke(cm, reason);
					}
					logger.trace("frodo core: revoke [{}] certs", revokeCerts.size());
				} catch (Throwable t) {
					logger.error("frodo core: cluster batch update failed", t);
				}
			}
		}

		private PrimitiveParseCallback accessProfileParseCallback() {
			return new PrimitiveParseCallback() {
				@Override
				public <T> T onParse(Class<T> clazz, Map<String, Object> referenceKey) {
					ConfigDatabase db = conf.ensureDatabase("frodo");
					Config c = db.findOne(clazz, Predicates.field(referenceKey));
					if (c == null)
						return null;
					return c.getDocument(clazz, accessProfileParseCallback());
				}
			};
		}

	}

	private class ClusterWatcher implements Runnable {
		private Thread thread;
		private boolean doStop;
		private boolean first;

		public void start() {
			if (thread != null)
				return;

			first = true;
			thread = new Thread(this, "Frodo Cluster Watcher");
			thread.start();
		}

		public void stop() {
			if (thread == null)
				return;

			doStop = true;
			thread.interrupt();
			thread = null;
		}

		@Override
		public void run() {
			doStop = false;
			logger.info("frodo core: cluster watcher started, thread [{}]", Thread.currentThread().getId());
			
			int failCount = 0;
         long sleepTime = 10 * 1000;
			
			try {
				while (!doStop) {
					try {
						logger.info("cluster watcher started!!!");
						updateClusterMaster();
						logger.info("cluster db sync finished!!!");

						failCount = 0;				//initialize when connection recovered
						if(first) {
							first = false;
							sleepTime = 30 * 60 * 1000;	//after first sync sleep 30minutes.
							logger.info("cluster first sync finished!!!");
						}
						
						logger.info("cluster watcher hold for sometime!!!");
						//체크 시간을 3분으로
						Thread.sleep(sleepTime);
					} catch (InterruptedException e) {
						logger.debug("frodo core: cluster watcher interrupted");
						logger.error("frodo core: cluster watcher connect false count: " + failCount);
                  failCount++;
                  
                  try {
                  	Thread.sleep(sleepTime);
                  } catch (InterruptedException e1) {
                  	e1.printStackTrace();
                  }
					} catch (Throwable t) {
						logger.error("frodo core: cluster watcher error", t);
						logger.error("frodo core: cluster watcher connect false count: " + failCount);
                  failCount++;
                  
                  try {
                     Thread.sleep(sleepTime);
                  } catch (InterruptedException e) {
                  	e.printStackTrace();
                  }
					}
					
					if (failCount == 5) {
						logger.info("frodo core: cluster watcher connect fail 5 times in a row. change interval time to 10 minutes");
              } else if(failCount == 10) {	//if cluster node couldn't be recovered in 65 minutes then stop watcher.
            	  logger.error("frodo core: cluster watcher connect fail, connection cancel. failCount: " + failCount);
            	  doStop = true;
              }
				}
			} finally {
				doStop = false;
				disconnectAll();

				logger.info("frodo core: cluster watcher stopped, thread [{}]", Thread.currentThread().getId());
			}
		}

		private void disconnectAll() {
			for (ClusterNode node : getClusterNodes()) {
				if (node.getClient() != null) {
					node.getClient().close();
					node.setClient(null);
				}
				node.setAvailable(false);
			}
		}
	}

	private class AuthoritySynchronizer extends BaseCertificateAuthorityListener {
		@Override
		public void onRevokeCert(CertificateAuthority authority, CertificateMetadata cm, RevocationReason reason) {
			if (!authority.getName().equals("local"))
				return;

			if (!config.isEnabled())
				return;

			Boolean replicated = ClusterContext.replicated.get();
			if (replicated != null && replicated)
				return;

			replicate(new ClusterOperation("cert", "revoke", PrimitiveConverter.serialize(Arrays.asList(cm, reason.toString()))));
		}

		@Override
		public void onIssueCert(CertificateAuthority authority, CertificateMetadata cm) {
			if (!authority.getName().equals("local"))
				return;

			if (!config.isEnabled())
				return;

			Boolean replicated = ClusterContext.replicated.get();
			if (replicated != null && replicated)
				return;

			replicate(new ClusterOperation("cert", "issue", PrimitiveConverter.serialize(cm, new EmptySerializer())));
		}
	}

	private class ClientAppSynchronizer implements ClientAppEventListener {
		@Override
		public void onCreated(ClientApp app) {
			if (!config.isEnabled())
				return;

			Boolean replicated = ClusterContext.replicated.get();
			if (replicated != null && replicated)
				return;

			replicate(new ClusterOperation("client_app", "create", PrimitiveConverter.serialize(app, new EmptySerializer())));
		}

		@Override
		public void onUpdated(ClientApp app) {
			if (!config.isEnabled())
				return;

			Boolean replicated = ClusterContext.replicated.get();
			if (replicated != null && replicated)
				return;

			replicate(new ClusterOperation("client_app", "update", PrimitiveConverter.serialize(app, new EmptySerializer())));
		}

		@Override
		public void onRemoved(ClientApp app) {
			if (!config.isEnabled())
				return;

			Boolean replicated = ClusterContext.replicated.get();
			if (replicated != null && replicated)
				return;

			replicate(new ClusterOperation("client_app", "remove", PrimitiveConverter.serialize(app, new EmptySerializer())));
		}

	}

	private class ProfileSynchronizer extends DefaultEntityEventListener<AccessProfile> {

		@Override
		public void entityAdded(String domain, AccessProfile obj, Object state) {
			if (!config.isEnabled())
				return;

			Boolean replicated = ClusterContext.replicated.get();
			if (replicated != null && replicated)
				return;

			replicate(new ClusterOperation("access_profile", "create", PrimitiveConverter.serialize(obj, new EmptySerializer())));
		}

		@Override
		public void entityUpdated(String domain, AccessProfile obj, Object state) {
			if (!config.isEnabled())
				return;

			Boolean replicated = ClusterContext.replicated.get();
			if (replicated != null && replicated)
				return;

			replicate(new ClusterOperation("access_profile", "update", PrimitiveConverter.serialize(obj, new EmptySerializer())));
		}

		@Override
		public void entityRemoved(String domain, AccessProfile obj, Object state) {
			if (!config.isEnabled())
				return;

			Boolean replicated = ClusterContext.replicated.get();
			if (replicated != null && replicated)
				return;

			replicate(new ClusterOperation("access_profile", "remove", PrimitiveConverter.serialize(obj, new EmptySerializer())));
		}

	}

	private class OrgUnitSynchronizer extends DefaultEntityEventListener<OrganizationUnit> {

		@Override
		public void entityAdded(String domain, OrganizationUnit obj, Object state) {
			if (!config.isEnabled())
				return;

			Boolean replicated = ClusterContext.replicated.get();
			if (replicated != null && replicated)
				return;

			replicate(new ClusterOperation("org_unit", "create", PrimitiveConverter.serialize(obj, new EmptySerializer())));
		}

		@Override
		public void entityUpdated(String domain, OrganizationUnit obj, Object state) {
			if (!config.isEnabled())
				return;

			Boolean replicated = ClusterContext.replicated.get();
			if (replicated != null && replicated)
				return;

			replicate(new ClusterOperation("org_unit", "update", PrimitiveConverter.serialize(obj, new EmptySerializer())));
		}

		@Override
		public void entityRemoving(String domain, OrganizationUnit obj, ConfigTransaction xact, Object state) {
		}

		@Override
		public void entityRemoved(String domain, OrganizationUnit obj, Object state) {
			if (!config.isEnabled())
				return;

			Boolean replicated = ClusterContext.replicated.get();
			if (replicated != null && replicated)
				return;

			replicate(new ClusterOperation("org_unit", "remove", PrimitiveConverter.serialize(obj, new EmptySerializer())));
		}
	}

	private class UserSynchronizer extends DefaultEntityEventListener<User> {

		@Override
		public void entityAdded(String domain, User obj, Object state) {
			if (!config.isEnabled())
				return;

			Boolean replicated = ClusterContext.replicated.get();
			if (replicated != null && replicated)
				return;

			replicate(new ClusterOperation("user", "create", PrimitiveConverter.serialize(obj, new EmptySerializer())));
		}

		@Override
		public void entityUpdated(String domain, User obj, Object state) {
			if (!config.isEnabled())
				return;

			Boolean replicated = ClusterContext.replicated.get();
			if (replicated != null && replicated)
				return;

			replicate(new ClusterOperation("user", "update", PrimitiveConverter.serialize(obj, new EmptySerializer())));
		}

		@Override
		public void entityRemoving(String domain, User obj, ConfigTransaction xact, Object state) {
		}

		@Override
		public void entityRemoved(String domain, User obj, Object state) {
			if (!config.isEnabled())
				return;

			Boolean replicated = ClusterContext.replicated.get();
			if (replicated != null && replicated)
				return;

			replicate(new ClusterOperation("user", "remove", PrimitiveConverter.serialize(obj, new EmptySerializer())));
		}
	}

	private class AuthDeviceSynchronizer implements AuthorizedDeviceEventListener {

		@Override
		public void onRegister(AuthorizedDevice device) {
			Boolean replicated = ClusterContext.replicated.get();
			if (replicated != null && replicated)
				return;

			replicate(new ClusterOperation("auth_device", "register", PrimitiveConverter.serialize(device, new EmptySerializer())));
		}

		@Override
		public void onUnregister(AuthorizedDevice device) {
			Boolean replicated = ClusterContext.replicated.get();
			if (replicated != null && replicated)
				return;

			replicate(new ClusterOperation("auth_device", "unregister", PrimitiveConverter.serialize(device,
					new EmptySerializer())));
		}

		@Override
		public void onUpdate(AuthorizedDevice device) {
			Boolean replicated = ClusterContext.replicated.get();
			if (replicated != null && replicated)
				return;

			replicate(new ClusterOperation("auth_device", "update", PrimitiveConverter.serialize(device, new EmptySerializer())));
		}
	}

	private class AccessGatewaySynchronizer extends DefaultEntityEventListener<AccessGateway> {
		@Override
		public void entityUpdated(String domain, AccessGateway gw, Object state) {
			Boolean replicated = ClusterContext.replicated.get();
			if (replicated != null && replicated)
				return;

			replicate(new ClusterOperation("access_gateway", "update", PrimitiveConverter.serialize(gw, new EmptySerializer())));
		}
	}
	
//	private class VpnServerConfigSynchronizer extends DefaultEntityEventListener<VpnServerConfig> {
//		@Override
//		public void entityUpdated(String domain, VpnServerConfig config, Object state) {
//			Boolean replicated = ClusterContext.replicated.get();
//			if (replicated != null && replicated)
//				return;
//
//			replicate(new ClusterOperation("vpn_server_config", "update", PrimitiveConverter.serialize(config, new EmptySerializer())));
//		}
//	}

	private class ClientCheckProfileSynchronizer implements ClientCheckProfileEventListener {
		@Override
		public void onCreated(ClientCheckProfile p) {
			Boolean replicated = ClusterContext.replicated.get();
			if (replicated != null && replicated)
				return;

			replicate(new ClusterOperation("client_check_profile", "create", PrimitiveConverter.serialize(p,
					new EmptySerializer())));
		}

		@Override
		public void onUpdated(ClientCheckProfile p) {
			Boolean replicated = ClusterContext.replicated.get();
			if (replicated != null && replicated)
				return;

			replicate(new ClusterOperation("client_check_profile", "update", PrimitiveConverter.serialize(p,
					new EmptySerializer())));
		}

		@Override
		public void onRemoved(ClientCheckProfile p) {
			Boolean replicated = ClusterContext.replicated.get();
			if (replicated != null && replicated)
				return;

			replicate(new ClusterOperation("client_check_profile", "remove", PrimitiveConverter.serialize(p,
					new EmptySerializer())));
		}
	}

	private class ServerSynchronizer extends DefaultEntityEventListener<Server> {
		@Override
		public void entityAdded(String domain, Server server, Object state) {
			Boolean replicated = ClusterContext.replicated.get();
			if (replicated != null && replicated)
				return;

			replicate(new ClusterOperation("server", "create", PrimitiveConverter.serialize(server, new EmptySerializer())));
		}

		@Override
		public void entityUpdated(String domain, Server server, Object state) {
			Boolean replicated = ClusterContext.replicated.get();
			if (replicated != null && replicated)
				return;

			replicate(new ClusterOperation("server", "update", PrimitiveConverter.serialize(server, new EmptySerializer())));
		}

		@Override
		public void entityRemoved(String domain, Server server, Object state) {
			Boolean replicated = ClusterContext.replicated.get();
			if (replicated != null && replicated)
				return;

			replicate(new ClusterOperation("server", "remove", PrimitiveConverter.serialize(server, new EmptySerializer())));
		}

	}

	private class DnsZoneSynchronizer extends DefaultEntityEventListener<DnsZone> {
		@Override
		public void entityAdded(String domain, DnsZone dns, Object state) {
			Boolean replicated = ClusterContext.replicated.get();
			if (replicated != null && replicated)
				return;

			replicate(new ClusterOperation("dns_zone", "create", PrimitiveConverter.serialize(dns, new EmptySerializer())));
		}

		@Override
		public void entityUpdated(String domain, DnsZone dns, Object state) {
			Boolean replicated = ClusterContext.replicated.get();
			if (replicated != null && replicated)
				return;

			replicate(new ClusterOperation("dns_zone", "update", PrimitiveConverter.serialize(dns, new EmptySerializer())));
		}

		@Override
		public void entityRemoved(String domain, DnsZone dns, Object state) {
			Boolean replicated = ClusterContext.replicated.get();
			if (replicated != null && replicated)
				return;

			replicate(new ClusterOperation("dns_zone", "remove", PrimitiveConverter.serialize(dns, new EmptySerializer())));
		}
	}
	
	private class TimetableSynchronizer extends DefaultEntityEventListener<Timetable> {
		@Override
		public void entityAdded(String domain, Timetable timetable, Object state) {
			Boolean replicated = ClusterContext.replicated.get();
			if (replicated != null && replicated)
				return;

			replicate(new ClusterOperation("timetable", "create", PrimitiveConverter.serialize(timetable, new EmptySerializer())));
		}

		@Override
		public void entityUpdated(String domain, Timetable timetable, Object state) {
			Boolean replicated = ClusterContext.replicated.get();
			if (replicated != null && replicated)
				return;

			replicate(new ClusterOperation("timetable", "update", PrimitiveConverter.serialize(timetable, new EmptySerializer())));
		}

		@Override
		public void entityRemoved(String domain, Timetable timetable, Object state) {
			Boolean replicated = ClusterContext.replicated.get();
			if (replicated != null && replicated)
				return;

			replicate(new ClusterOperation("timetable", "remove", PrimitiveConverter.serialize(timetable, new EmptySerializer())));
		}
	}

	private class EmptySerializer implements PrimitiveSerializeCallback {
		@Override
		public void onSerialize(Object root, Class<?> cls, Object obj, Map<String, Object> referenceKeys) {
		}
	}

}
