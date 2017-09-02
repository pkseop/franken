package kr.co.future.sslvpn.core.cluster;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;

import kr.co.future.sslvpn.model.AuthorizedDevice;
import kr.co.future.sslvpn.model.api.AuthorizedDeviceApi;
import kr.co.future.api.Script;
import kr.co.future.api.ScriptArgument;
import kr.co.future.api.ScriptContext;
import kr.co.future.api.ScriptUsage;
import kr.co.future.confdb.Config;
import kr.co.future.confdb.ConfigDatabase;
import kr.co.future.confdb.ConfigService;

public class ClusterScript implements Script {
	private ClusterService cluster;
	private ScriptContext context;
	private AuthorizedDeviceApi deviceApi;
	private ClusterSync clusterSync;
	private ConfigService conf;

	public ClusterScript(ClusterService cluster, AuthorizedDeviceApi deviceApi, ClusterSync clusterSync, ConfigService conf) {
		this.cluster = cluster;
		this.deviceApi = deviceApi;
		this.clusterSync = clusterSync;
		this.conf = conf;
	}

	@Override
	public void setScriptContext(ScriptContext context) {
		this.context = context;
	}

//	public void timestamp(String[] args) {
//		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
//		long bootTimestamp = cluster.getBootTimestamp();
//		context.println(bootTimestamp + ", " + dateFormat.format(new Date(bootTimestamp)));
//	}

	@ScriptUsage(description = "set or print cluster config", arguments = {
			@ScriptArgument(name = "enabled", type = "string", description = "enable cluster mode, true or false", optional = true),
			@ScriptArgument(name = "is master", type = "string", description = "set master cluster", optional = true),
			@ScriptArgument(name = "node id", type = "int", description = "cluster node id, should be unique", optional = true),			
			@ScriptArgument(name = "shared key", type = "string", description = "cluster shared key, i.e. password", optional = true)})
	public void config(String[] args) {
		if (args.length >= 4) {
			ClusterConfig cc = cluster.getConfig();
			cc.setEnabled(Boolean.valueOf(args[0]));
			cc.setMaster(Boolean.valueOf(args[1]));
			cc.setNodeId(Integer.valueOf(args[2]));
			cc.setSharedKey(args[3]);

			cluster.setConfig(cc);
			context.println("set cluster config");
		} else {
			context.println(cluster.getConfig());
		}
	}

//	@ScriptUsage(description = "show current master", arguments = {
//			@ScriptArgument(name = "force update", type = "string", description = "force update if true", optional = true),
//			@ScriptArgument(name = "times", type = "int", description = "check loop", optional = true) })
	public void master(String[] args) {
//		if (args.length > 0) {
//			boolean forceUpdate = Boolean.valueOf(args[0]);
//			if (forceUpdate)
//				cluster.updateClusterMaster();
//		}
//
//		int count = 1;
//		if (args.length > 1)
//			count = Integer.valueOf(args[1]);
//
//		for (int i = 0; i < count; i++) {
//			ClusterNode master = cluster.getClusterMaster();
//			if (master == null) {
//				context.println("current node is master");
//			} else {
//				context.println(master);
//			}
//		}
		
		ConfigDatabase db = conf.ensureDatabase("frodo-cluster");
      Config c = db.findOne(ClusterConfig.class, null);
      if (c != null) {
      	ClusterConfig config = c.getDocument(ClusterConfig.class);
          if(config.isMaster())
          	context.println("current node is master");
          else{
          	ClusterNode master = cluster.getClusterMaster();
          	if(master != null) {
          		context.println(master.toString());
          	} else {
          		context.println("can't find master node");
          	}
          }	
      } else {
      	context.println("cluster is not configured.");
      }
      
	}

	public void nodes(String[] args) {
		context.println("Cluster Nodes");
		context.println("---------------");

		for (ClusterNode node : cluster.getClusterNodes()) {
			context.println(node);
		}
	}

	@ScriptUsage(description = "add cluster node", arguments = { @ScriptArgument(name = "id", type = "int", description = "id"),
			@ScriptArgument(name = "public ip", type = "string", description = "public ip (used by ssl client)"),
			@ScriptArgument(name = "internal ip address", type = "string", description = "internal ip address"),
			@ScriptArgument(name = "port", type = "int", description = "rpc port. 7140 by default", optional = true) })
	public void addNode(String[] args) throws UnknownHostException {
		int id = Integer.valueOf(args[0]);
		InetAddress publicIp = InetAddress.getByName(args[1]);
		InetAddress ip = InetAddress.getByName(args[2]);
		int port = 7140;
		if (args.length >= 4)
			port = Integer.valueOf(args[3]);

		cluster.addNode(new ClusterNodeConfig(id, publicIp.getHostAddress(), ip.getHostAddress(), port));
		context.println("added cluster node");
	}

	@ScriptUsage(description = "remove cluster node", arguments = { @ScriptArgument(name = "id", type = "int", description = "node id") })
	public void removeNode(String[] args) {
		int id = Integer.valueOf(args[0]);
		cluster.removeNode(id);
		context.println("removed cluster node");
	}

	public void tail(String[] args) throws InterruptedException {
		context.println("Tracing cluster events, Press ctrl-c to stop..");
		context.println("--------------------------------------------------");

		ClusterConfig mode = cluster.getConfig();
		ClusterEventPrinter printer = new ClusterEventPrinter(mode.getNodeId());
		cluster.addEventListener(printer);
		cluster.addMessageListener(printer);

		try {
			while (true) {
				try {
					context.readLine();
				} catch (InterruptedException e) {
					break;
				}
			}
		} finally {
			cluster.removeEventListener(printer);
			cluster.removeMessageListener(printer);
			context.println("interrupted.");
		}
	}

	@ScriptUsage(description = "create dummy device", arguments = { @ScriptArgument(name = "host name", type = "string", description = "host name") })
	public void createDummyDevice(String[] args) {
		AuthorizedDevice device = new AuthorizedDevice();
		device.setHostName(args[0]);
		device.setType(1);
		device.setDeviceKey(UUID.randomUUID().toString());
		device.setOwner("xeraph");
		deviceApi.registerDevice(device);
		context.println("registered device");
	}
	
	@ScriptUsage(description = "request node data db and paste it", arguments = { @ScriptArgument(name = "host address", type = "string", description = "host address") })
	public void requestNodeData(String[] args) {
		try {
			InetAddress.getByName(args[0]);
		} catch (UnknownHostException e) {
			context.println("Can't access to [" + args[0] + "]");
			return;
		}
		context.println("requesting node data.");
		clusterSync.requestNodeData(args[0]);
		context.println("\n\nrequest node [" + args[0] + "] data finished.");
	}

	private class ClusterEventPrinter implements ClusterMessageListener, ClusterEventListener {

		private int nodeId;

		public ClusterEventPrinter(int nodeId) {
			this.nodeId = nodeId;
		}

		@Override
		public int getNodeId() {
			return nodeId;
		}

		@Override
		public void onMessage(ClusterOperation op) {
			SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
			context.println("sending date: " + dateFormat.format(new Date()) + ", category=" + op.getCategory() + ", obj="
					+ op.getObj());
		}

		@Override
		public void onMasterChange(ClusterNode master) {
			if (master == null)
				context.println("current node is master now");
			else
				context.println("master changed: " + master);
		}

	}
	
	@ScriptUsage(description = "divide tunnel connection by clustering", arguments = { @ScriptArgument(name = "", type = "boolean", description = "true/false", optional = true) })
	public void loadBalancing(String[] args) {
		ConfigDatabase db = conf.ensureDatabase("frodo-cluster");
        Config c = db.findOne(ClusterConfig.class, null);
        ClusterConfig config;
        if (c != null)
            config = c.getDocument(ClusterConfig.class);
        else{
        	context.println("cluster configuration does not exist");
        	return;
        }
        
		if(args.length == 0) {
			context.println("use load balancing: " + config.getUseLoadBalancing());
			return;
		}   
        
		if(!args[0].equals("true") && !args[0].equals("false")){
			context.println("input true or false");
			return;
		}
        
        boolean ret = Boolean.valueOf(args[0]);
        config.setUseLoadBalancing(ret);
        if(cluster.getConfig() != null)
    		cluster.setConfigWithNoStart(config);
    	db.update(c, config);
	}
	
	@ScriptUsage(description = "syn only user data", arguments = { @ScriptArgument(name = "", type = "boolean", description = "true/false", optional = true) })
	public void syncUserDataOnly(String[] args) {
		if(args.length > 1) {
			context.println("too many arguments");
			return;
		}
		
		ConfigDatabase db = conf.ensureDatabase("frodo-cluster");
        Config c = db.findOne(ClusterConfig.class, null);
        ClusterConfig config;
        if (c != null)
            config = c.getDocument(ClusterConfig.class);
        else{
        	config = new ClusterConfig();
        	db.add(config);
        	if(cluster.getConfig() != null)
        		cluster.setConfigWithRegisterListenersBySyncUserDataOnly(config);
        }
		
		if(args.length == 0) {
			context.println("sync user data only: " + config.getSyncUserDataOnly());
			return;
		} else {
			if(args[0].equals("true") || args[0].equals("false")) {
				config.setSyncUserDataOnly(Boolean.parseBoolean(args[0]));
				db.update(c, config);
				cluster.setConfigWithRegisterListenersBySyncUserDataOnly(config);
				context.println("set");
			} else {
				context.println("input true or false");
				return;
			}
		}
			
	}
	
	@ScriptUsage(description = "syn exclude user data", arguments = { @ScriptArgument(name = "", type = "boolean", description = "true/false", optional = true) })
	public void syncExcludeUserData(String[] args) {
		if(args.length > 1) {
			context.println("too many arguments");
			return;
		}
		
		ConfigDatabase db = conf.ensureDatabase("frodo-cluster");
        Config c = db.findOne(ClusterConfig.class, null);
        ClusterConfig config;
        if (c != null)
            config = c.getDocument(ClusterConfig.class);
        else{
        	config = new ClusterConfig();
        	db.add(config);
        	if(cluster.getConfig() != null)
        		cluster.setConfigWithExcludeUserData(config);
        }
		
		if(args.length == 0) {
			context.println("sync exclude user data: " + config.getSyncExcludeUserData());
			return;
		} else {
			if(args[0].equals("true") || args[0].equals("false")) {
				config.setSyncExcludeUserData(Boolean.parseBoolean(args[0]));
				db.update(c, config);
				cluster.setConfigWithExcludeUserData(config);
				context.println("set");
			} else {
				context.println("input true or false");
				return;
			}
		}
	}
}
