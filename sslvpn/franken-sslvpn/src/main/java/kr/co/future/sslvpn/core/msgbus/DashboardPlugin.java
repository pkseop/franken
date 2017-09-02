package kr.co.future.sslvpn.core.msgbus;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import kr.co.future.sslvpn.core.AlertCategory;
import kr.co.future.sslvpn.core.AuthService;
import kr.co.future.sslvpn.core.DashboardService;
import kr.co.future.sslvpn.core.LicenseStatus;
import kr.co.future.sslvpn.core.LiveStatus;
import kr.co.future.sslvpn.core.NetworkService;
import kr.co.future.sslvpn.core.TrendGraphPoint;
import kr.co.future.sslvpn.core.TrendGraphType;
import kr.co.future.sslvpn.core.Tunnel;
import kr.co.future.sslvpn.core.TunnelEventListener;
import kr.co.future.sslvpn.core.msgbus.DashboardPlugin;
import kr.co.future.sslvpn.model.IpEndpoint;
import kr.co.future.sslvpn.model.Server;
import kr.co.future.sslvpn.model.api.ServerApi;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Invalidate;
import org.apache.felix.ipojo.annotations.Requires;
import org.apache.felix.ipojo.annotations.Validate;

import kr.co.future.confdb.BaseConfigDatabaseListener;
import kr.co.future.confdb.BaseConfigServiceListener;
import kr.co.future.confdb.ConfigDatabase;
import kr.co.future.confdb.ConfigService;
import kr.co.future.confdb.ConfigTransaction;
import kr.co.future.dom.api.AdminApi;
import kr.co.future.dom.api.DOMException;
import kr.co.future.dom.api.DefaultEntityEventListener;
import kr.co.future.dom.api.EntityEventListener;
import kr.co.future.dom.api.LoginCallback;
import kr.co.future.dom.api.OrganizationApi;
import kr.co.future.dom.api.OrganizationUnitApi;
import kr.co.future.dom.api.UserApi;
import kr.co.future.dom.model.Admin;
import kr.co.future.linux.api.CpuStat;
import kr.co.future.linux.api.MemoryStat;
import kr.co.future.logdb.LogQuery;
import kr.co.future.logdb.LogQueryService;
import kr.co.future.logdb.LogResultSet;
import kr.co.future.logdb.LookupHandler;
import kr.co.future.logdb.LookupHandlerRegistry;
import kr.co.future.msgbus.Marshaler;
import kr.co.future.msgbus.PushApi;
import kr.co.future.msgbus.Request;
import kr.co.future.msgbus.Response;
import kr.co.future.msgbus.Session;
import kr.co.future.msgbus.handler.MsgbusMethod;
import kr.co.future.msgbus.handler.MsgbusPlugin;
import kr.co.future.sslvpn.core.PerformanceMonitor;
import kr.co.future.sslvpn.core.impl.SerialKey;
import kr.co.future.sslvpn.core.xenics.XenicsService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(name = "frodo-dashboard-plugin")
@MsgbusPlugin
public class DashboardPlugin implements TunnelEventListener {
	private final Logger logger = LoggerFactory.getLogger(DashboardPlugin.class);
	@Requires
	private LogQueryService queryService;

	@Requires
	private DashboardService dash;

	@Requires
	private PerformanceMonitor perf;

	@Requires
	private AuthService auth;

	@Requires
	private UserApi userApi;

	@Requires
	private OrganizationUnitApi orgUnitApi;

	@Requires
	private ServerApi serverApi;

	@Requires
	private PushApi pushApi;

	@Requires
	private LookupHandlerRegistry lookup;

	@Requires
	private OrganizationApi orgApi;

	@Requires
	private ConfigService conf;

	@Requires
	private AdminApi adminApi;
	
	@Requires
	private XenicsService xenicsService;

	private int maxTunnel;
	private ConfDbListener dbListener;
	private ConfServiceListener serviceListener;

//	private Map<String, String> userIdToName = new HashMap<String, String>();
//	private Map<String, String> userIdToOrgUnitId = new HashMap<String, String>();
//	private Map<String, String> orgUnitIdToOrgName = new HashMap<String, String>();
	private Set<Integer> masks = new TreeSet<Integer>(Collections.reverseOrder());
	private Map<Integer, Set<ServerIp>> serverIp = new HashMap<Integer, Set<ServerIp>>();
	private Map<ServerIp, String> serverIpToServerName = new HashMap<ServerIp, String>();
//	private EntityEventListener<User> userEventListener = new UserEventListener();
//	private EntityEventListener<OrganizationUnit> orgUnitEvenetListener = new OrgUnitEventListener();
	private EntityEventListener<Server> serverEventListener = new ServerEventListener();
	private LookupHandler lookupHandler = new SSLplusLookupHandler();
//	private TunnelList tunnelList;
	private LogOutCallBack logoutCallBack;

	//pks: 2013-08-06. Variables to check power status.
	private Thread powerStatusThread = null;
	private byte powerStatusFlag = 0;
		
	public DashboardPlugin() {
		serviceListener = new ConfServiceListener();
		dbListener = new ConfDbListener();
		logoutCallBack = new LogOutCallBack();
	}

	@Validate
	public void start() {
//		tunnelList = new TunnelList();
		auth.addTunnelEventListener(this);
		maxTunnel = SerialKey.getMaxTunnel();

		conf.addListener(serviceListener);
		ConfigDatabase db = conf.ensureDatabase("frodo");
		db.addListener(dbListener);

		Thread cacheLoader = new Thread(new Runnable() {
			@Override
			public void run() {
				logger.info("DashboardPlugin initializeCache() start...");
				initializeCache();
				logger.info("DashboardPlugin initializeCache() end...");
			}
		});
		cacheLoader.setName("Frodo Dashboard Cache Loader");
		cacheLoader.start();

//		userApi.addEntityEventListener(userEventListener);
//		orgUnitApi.addEntityEventListener(orgUnitEvenetListener);
		serverApi.addEntityEventListener(serverEventListener);
		lookup.addLookupHandler("sslplus", lookupHandler);
		adminApi.registerLoginCallback(logoutCallBack);
		
		// pks: 2013-08-06. check power status if device is 3000 or 5000.
		checkPowerStatus();
	}

	private void initializeCache() {
		// ignore if org not found
		if (orgApi.findOrganization("localhost") == null)
			return;

//		userIdToOrgUnitId.clear();
//		userIdToName.clear();
//		for (User user : userApi.getUsers("localhost")) {
//			userIdToName.put(user.getLoginName(), user.getName());
//			if (user.getOrgUnit() != null)
//				userIdToOrgUnitId.put(user.getLoginName(), user.getOrgUnit().getGuid());
//		}
//
//		orgUnitIdToOrgName.clear();
//		for (OrganizationUnit orgUnit : orgUnitApi.getOrganizationUnits("localhost")) {
//			orgUnitIdToOrgName.put(orgUnit.getGuid(), orgUnit.getName());
//		}
//
		serverIp.clear();
		masks.clear();
		serverIpToServerName.clear();
		for (Server server : serverApi.getServers()) {
			for (IpEndpoint endpoint : server.getEndpoints()) {
				if (endpoint.getIp4Address() != null) {
					try {
						ServerIp v4 = new ServerIp(InetAddress.getByName(endpoint.getIp4Address()), endpoint.getIp4Mask());
						if (!serverIp.containsKey(endpoint.getIp4Mask()))
							serverIp.put(endpoint.getIp4Mask(), new HashSet<ServerIp>());
						serverIp.get(endpoint.getIp4Mask()).add(v4);
						masks.add(endpoint.getIp4Mask());
						serverIpToServerName.put(v4, server.getName());
					} catch (UnknownHostException e) {
					}
				}

				if (endpoint.getIp6Address() != null) {
					try {
						ServerIp v6 = new ServerIp(InetAddress.getByName(endpoint.getIp6Address()), endpoint.getIp6Mask());
						if (!serverIp.containsKey(endpoint.getIp6Mask()))
							serverIp.put(endpoint.getIp6Mask(), new HashSet<ServerIp>());
						serverIp.get(endpoint.getIp6Mask()).add(v6);
						masks.add(endpoint.getIp6Mask());
						serverIpToServerName.put(v6, server.getName());
					} catch (UnknownHostException e) {
					}
				}
			}
		}
	}

	@Invalidate
	public void stop() {
//		if (userApi != null)
//			userApi.removeEntityEventListener(userEventListener);
//
//		if (orgUnitApi != null)
//			orgUnitApi.removeEntityEventListener(orgUnitEvenetListener);

		if (serverApi != null)
			serverApi.removeEntityEventListener(serverEventListener);

		if (lookup != null)
			lookup.removeLookupHandler("sslplus");

		if (auth != null)
			auth.removeTunnelEventListener(this);

		if (conf != null) {
			conf.removeListener(serviceListener);
			ConfigDatabase db = conf.ensureDatabase("frodo");
			db.removeListener(dbListener);
		}

		if (adminApi != null)
			adminApi.unregisterLoginCallback(logoutCallBack);

//		if (tunnelList != null)
//			tunnelList.stop();
	}

	@MsgbusMethod
	public void getTrendGraph(Request req, Response resp) {
		TrendGraphType type = TrendGraphType.parse(req.getString("type"));
		if (type != null) {
			resp.put("type", type.name().toLowerCase());
			List<TrendGraphPoint> graph = perf.getGraph(type);
			resp.put("graph", (graph != null) ? Marshaler.marshal(graph) : null);
		}
	}

	@MsgbusMethod
	public void getTunnels(Request req, Response resp) {
		Integer offset = req.getInteger("offset");
		Integer limit = req.getInteger("limit");
		String processId = req.getString("process_id");
		String filterField = req.getString("filter_field");
		String filterValue = req.getString("filter_value");

		logger.info("frodo-core: get tunnels");
		
		int total = 0;
		if(filterField != null && filterValue != null) {
			total = xenicsService.getTotalNumOfTunnels(filterField, filterValue);
		} else
			total = xenicsService.getTotalNumOfTunnels();
		
		if(offset == null)
			offset = 0;
		if(limit == null || limit > total)
			limit = total;
		
		resp.put("tunnels", xenicsService.getTunnels(limit, offset, filterField, filterValue));
		resp.put("total", total);
	}

	@MsgbusMethod
	public void removeProcessId(Request req, Response resp) {
//		String processId = req.getString("process_id");
//		if (tunnelList == null)
//			return;
//
//		tunnelList.removeProcessId(processId);
	}

	@MsgbusMethod
	public void killTunnel(Request req, Response resp) {
		auth.killTunnel(req.getInteger("tunnel_id"));
	}

	@MsgbusMethod
	public void getMetrics(Request req, Response resp) throws InterruptedException, IOException {
		LiveStatus radiusStatus = dash.getServiceStatus(NetworkService.Radius);
		LiveStatus ldapStatus = dash.getServiceStatus(NetworkService.Ldap);
		LiveStatus sslStatus = dash.getServiceStatus(NetworkService.SslEngine);
		LicenseStatus license = dash.getLicenseStatus();

		MemoryStat mem = MemoryStat.getMemoryStat();
		int cpuUsage = CpuStat.getCpuUsage().getUsage();
		int memUsage = (int) ((mem.getMemTotal() - mem.getMemFree() - mem.getCached()) * 100 / mem.getMemTotal());

		File f = new File("/utm/log");
		long totalUsed = f.getTotalSpace() - f.getFreeSpace();
		long total = f.getTotalSpace();

		// performance
		resp.put("cpu", cpuUsage);
		resp.put("memory", memUsage);
		resp.put("memory_use", (int) ((mem.getMemTotal() - mem.getMemFree() - mem.getCached())));
		resp.put("memory_total", (int) (mem.getMemTotal()));
		resp.put("disk", (int) (totalUsed * 100 / total));

		// connectivity
		resp.put("radius", radiusStatus.toString().toLowerCase());
		resp.put("ldap", ldapStatus.toString().toLowerCase());
		resp.put("ssl", sslStatus.toString().toLowerCase());

		// license
		resp.put("license", license.name().toLowerCase());

		// tx, rx
		List<TrendGraphPoint> rxPoints = perf.getGraph(TrendGraphType.RxBytes);
		List<TrendGraphPoint> txPoints = perf.getGraph(TrendGraphType.TxBytes);

		resp.put("rx",  getAvg(rxPoints)/*(long) rxPoints.get(rxPoints.size() - 2).value*/);
		resp.put("tx",  getAvg(txPoints)/*(long) txPoints.get(txPoints.size() - 2).value*/);

		// tunnel
		resp.put("max_tunnel", maxTunnel);
		resp.put("tunnel", xenicsService.getTotalNumOfTunnels()/*auth.getTunnels().size()*/);

		// alerts
		for (AlertCategory category : AlertCategory.values())
			resp.put(category.getCode(), dash.getAlert(category).marshal());
		
		// pks: 2013-08-06. if this device is 3000 or 5000 then checkPowerStatus
		// is not null.
		if (powerStatusThread != null) {
			putPowerStatusInfo(resp);
		}
	}
	
	private long getAvg(List<TrendGraphPoint> points) {
		if(points == null)
			return 0;
		int size = points.size();
		if(size == 0)
			return 0;
		
		double total = 0;
		for(TrendGraphPoint point : points) {
			total += point.value;
		}
		return (long)(total / size); 
	}

	@MsgbusMethod
	public void getTopLoginByUser(Request req, Response resp) {
		String duration = req.getString("duration");
		int limit = req.getInteger("limit");
		String qs = String.format("table duration=%s ssl-auth | search type == login | search code == success | "
				+ "stats count(_time) as success by login | sort limit=%d -success | "
				+ "lookup sslplus login as login_name output name", duration, limit);
		query(qs, resp);
	}

	@MsgbusMethod
	public void getTopLoginByOrgUnit(Request req, Response resp) {
		String duration = req.getString("duration");
		int limit = req.getInteger("limit");
		String qs = String.format("table duration=%s ssl-auth | search type == login | search code == success | "
				+ "lookup sslplus login as login_name output org_unit_name | "
				+ "stats count(_time) as success by org_unit_name | sort limit=%d -success", duration, limit);
		query(qs, resp);
	}

	@MsgbusMethod
	public void getTopLoginFailedByUser(Request req, Response resp) {
		String duration = req.getString("duration");
		int limit = req.getInteger("limit");
		String qs = String.format("table duration=%s ssl-auth | search type == login | search code != success | "
				+ "stats count(_time) as failed by login | sort limit=%d -failed | "
				+ "lookup sslplus login as login_name output name", duration, limit);
		query(qs, resp);
	}

	@MsgbusMethod
	public void getTopLoginFailedByCode(Request req, Response resp) {
		String duration = req.getString("duration");
		String qs = String.format("table duration=%s ssl-auth | search type == login | search code != success | "
				+ "stats count(_time) as failed by code | sort -failed", duration);
		query(qs, resp);
	}

	@MsgbusMethod
	public void getTopDeniedAccessByUser(Request req, Response resp) {
		String duration = req.getString("duration");
		int limit = req.getInteger("limit");
		String qs = String.format("table duration=%s ssl-access | search action != allow | "
				+ "stats count(_time) as failed by login | lookup sslplus login as login_name output name | "
				+ "sort limit=%d -failed", duration, limit);
		query(qs, resp);
	}

	@MsgbusMethod
	public void getTopDeniedAccessByOrgUnit(Request req, Response resp) {
		String duration = req.getString("duration");
		int limit = req.getInteger("limit");
		String qs = String.format("table duration=%s ssl-access | search action != allow | "
				+ "lookup sslplus login as login_name output org_unit_name | "
				+ "stats count(_time) as failed by org_unit_name | sort limit=%d -failed", duration, limit);
		query(qs, resp);
	}

	@MsgbusMethod
	public void getTopDeniedAccessByServer(Request req, Response resp) {
		String duration = req.getString("duration");
		int limit = req.getInteger("limit");
		String qs = String.format("table duration=%s ssl-access | search action != allow | "
				+ "lookup sslplus server_ip output server_name | stats count(_time) as failed "
				+ "by server_ip, server_name | sort limit=%d -failed", duration, limit);
		query(qs, resp);
	}

	@MsgbusMethod
	public void getTopTrafficByUser(Request req, Response resp) {
		String duration = req.getString("duration");
		int limit = req.getInteger("limit");
		String qs = String.format("table duration=%s ssl-flow | stats sum(eval(tx_bytes+rx_bytes)) as traffic"
				+ " by login | lookup sslplus login as login_name output name | sort limit=%d -traffic", duration, limit);
		query(qs, resp);
	}

	@MsgbusMethod
	public void getTopTrafficByOrgUnit(Request req, Response resp) {
		String duration = req.getString("duration");
		int limit = req.getInteger("limit");
		String qs = String.format("table duration=%s ssl-flow | lookup sslplus login as login_name output "
				+ "org_unit_name | stats sum(eval(tx_bytes+rx_bytes)) as traffic by org_unit_name | " + "sort limit=%d -traffic",
				duration, limit);
		query(qs, resp);
	}

	@MsgbusMethod
	public void getTopTrafficByServer(Request req, Response resp) {
		String duration = req.getString("duration");
		int limit = req.getInteger("limit");
		String qs = String.format("table duration=%s ssl-flow | lookup sslplus server_ip output server_name | stats "
				+ "sum(eval(tx_bytes+rx_bytes)) as traffic by server_ip, server_name | sort limit=%d -traffic", duration, limit);
		query(qs, resp);
	}

	private void query(String qs, Response resp) {
		LogQuery lq = queryService.createQuery(qs);
		lq.run();
		List<Object> result = new ArrayList<Object>();
		LogResultSet rs = null;
		try {
			rs = lq.getResult();
			while (rs.hasNext())
				result.add(rs.next());
			resp.put("result", result);
		} catch (IOException e) {
			throw new IllegalStateException("dashboard query failed " + lq.getQueryString(), e);
		} finally {
			if (rs != null)
				rs.close();
			queryService.removeQuery(lq.getId());
		}
	}

	@Override
	public void onOpen(Tunnel t) {
//		tunnelList.put(t.getId());
//		for (String processId : tunnelList.getProcessIds()) {
//			if (tunnelList.validate(processId, t.getId())) {
//				Map<String, Object> m = serialize(t);
//				m.put("status", "opened");
//				pushApi.push("localhost", "frodo-tunnel", m);
//			}
//		}
	}

	@Override
	public void onClose(Tunnel t) {
//		for (String processId : tunnelList.getProcessIds()) {
//			if (tunnelList.validate(processId, t.getId())) {
//				Map<String, Object> m = serialize(t);
//				m.put("status", "closed");
//				pushApi.push("localhost", "frodo-tunnel", m);
//			}
//		}
//		tunnelList.remove(t.getId());
	}

	@Override
	public void onAllClose() {
//		if (tunnelList != null)
//			tunnelList.initiallize();
//		else
//			tunnelList = new TunnelList();
	}

//	private Map<String, Object> serialize(Tunnel t) {
//		Map<String, Object> m = t.marshal();
//		m.put("name", userIdToName.get(t.getLoginName()));
//		return m;
//	}

//	private class UserEventListener extends DefaultEntityEventListener<User> {
//		@Override
//		public void entityAdded(String domain, User obj, Object state) {
//			userIdToName.put(obj.getLoginName(), obj.getName());
//			if (obj.getOrgUnit() != null)
//				userIdToOrgUnitId.put(obj.getLoginName(), obj.getOrgUnit().getGuid());
//		}
//
//		@Override
//		public void entityUpdated(String domain, User obj, Object state) {
//			userIdToName.put(obj.getLoginName(), obj.getName());
//			if (obj.getOrgUnit() != null)
//				userIdToOrgUnitId.put(obj.getLoginName(), obj.getOrgUnit().getGuid());
//		}
//
//		@Override
//		public void entityRemoving(String domain, User obj, ConfigTransaction xact, Object state) {
//		}
//
//		@Override
//		public void entityRemoved(String domain, User obj, Object state) {
//			userIdToName.remove(obj.getLoginName());
//			userIdToOrgUnitId.remove(obj.getLoginName());
//		}
//	}
//
//	private class OrgUnitEventListener extends DefaultEntityEventListener<OrganizationUnit> {
//		@Override
//		public void entityAdded(String domain, OrganizationUnit obj, Object state) {
//			orgUnitIdToOrgName.put(obj.getGuid(), obj.getName());
//		}
//
//		@Override
//		public void entityUpdated(String domain, OrganizationUnit obj, Object state) {
//			orgUnitIdToOrgName.put(obj.getGuid(), obj.getName());
//		}
//
//		@Override
//		public void entityRemoving(String domain, OrganizationUnit obj, ConfigTransaction xact, Object state) {
//		}
//
//		@Override
//		public void entityRemoved(String domain, OrganizationUnit obj, Object state) {
//			orgUnitIdToOrgName.remove(obj.getGuid());
//		}
//	}
//
	private class ServerEventListener extends DefaultEntityEventListener<Server> {
		@Override
		public void entityAdded(String domain, Server obj, Object state) {
			for (IpEndpoint endpoint : obj.getEndpoints()) {
				if (endpoint.getIp4Address() != null) {
					try {
						ServerIp v4 = new ServerIp(InetAddress.getByName(endpoint.getIp4Address()), endpoint.getIp4Mask());
						if (!serverIp.containsKey(endpoint.getIp4Mask()))
							serverIp.put(endpoint.getIp4Mask(), new HashSet<ServerIp>());
						serverIp.get(endpoint.getIp4Mask()).add(v4);
						masks.add(endpoint.getIp4Mask());
						serverIpToServerName.put(v4, obj.getName());
					} catch (UnknownHostException e) {
					}
				}

				if (endpoint.getIp6Address() != null) {
					try {
						ServerIp v6 = new ServerIp(InetAddress.getByName(endpoint.getIp6Address()), endpoint.getIp6Mask());
						if (!serverIp.containsKey(endpoint.getIp6Mask()))
							serverIp.put(endpoint.getIp6Mask(), new HashSet<ServerIp>());
						serverIp.get(endpoint.getIp6Mask()).add(v6);
						masks.add(endpoint.getIp6Mask());
						serverIpToServerName.put(v6, obj.getName());
					} catch (UnknownHostException e) {
					}
				}
			}
		}

		@Override
		public void entityUpdated(String domain, Server obj, Object state) {
			for (IpEndpoint endpoint : obj.getEndpoints()) {
				if (endpoint.getIp4Address() != null) {
					try {
						ServerIp v4 = new ServerIp(InetAddress.getByName(endpoint.getIp4Address()), endpoint.getIp4Mask());
						if (!serverIp.containsKey(endpoint.getIp4Mask()))
							serverIp.put(endpoint.getIp4Mask(), new HashSet<ServerIp>());
						serverIp.get(endpoint.getIp4Mask()).add(v4);
						masks.add(endpoint.getIp4Mask());
						serverIpToServerName.put(v4, obj.getName());
					} catch (UnknownHostException e) {
					}
				}

				if (endpoint.getIp6Address() != null) {
					try {
						ServerIp v6 = new ServerIp(InetAddress.getByName(endpoint.getIp6Address()), endpoint.getIp6Mask());
						if (!serverIp.containsKey(endpoint.getIp6Mask()))
							serverIp.put(endpoint.getIp6Mask(), new HashSet<ServerIp>());
						serverIp.get(endpoint.getIp6Mask()).add(v6);
						masks.add(endpoint.getIp6Mask());
						serverIpToServerName.put(v6, obj.getName());
					} catch (UnknownHostException e) {
					}
				}
			}
		}

		@Override
		public void entityRemoving(String domain, Server obj, ConfigTransaction xact, Object state) {
		}

		@Override
		public void entityRemoved(String domain, Server obj, Object state) {
			for (IpEndpoint endpoint : obj.getEndpoints()) {
				if (endpoint.getIp4Address() != null) {
					try {
						ServerIp v4 = new ServerIp(InetAddress.getByName(endpoint.getIp4Address()), endpoint.getIp4Mask());
						serverIp.get(endpoint.getIp4Mask()).remove(v4);
						serverIpToServerName.remove(v4);
					} catch (UnknownHostException e) {
					}
				}

				if (endpoint.getIp6Address() != null) {
					try {
						ServerIp v6 = new ServerIp(InetAddress.getByName(endpoint.getIp6Address()), endpoint.getIp6Mask());
						serverIp.get(endpoint.getIp6Mask()).remove(v6);
						serverIpToServerName.remove(v6);
					} catch (UnknownHostException e) {
					}
				}
			}
		}
	}

	private class ConfDbListener extends BaseConfigDatabaseListener {
		@Override
		public void onImport(ConfigDatabase db) {
			if (!db.getName().equals("frodo"))
				return;

			Thread cacheLoader = new Thread(new Runnable() {
				@Override
				public void run() {
					initializeCache();
				}
			});
			cacheLoader.setName("Frodo Dashboard Cache Loader");
			cacheLoader.start();
		}
	}

	private class ConfServiceListener extends BaseConfigServiceListener {
		@Override
		public void onCreateDatabase(ConfigDatabase db) {
			if (!db.getName().equals("frodo"))
				return;
			db.addListener(dbListener);
		}
	}

	private class LogOutCallBack implements LoginCallback {
		@Override
		public void onLoginSuccess(Admin admin, Session session) {
		}

		@Override
		public void onLoginFailed(Admin admin, Session session, DOMException e) {
		}

		@Override
		public void onLoginLocked(Admin admin, Session session) {
		}

		@Override
		public void onLogout(Admin admin, Session session) {
//			if (tunnelList != null)
//				tunnelList.removeSession(session);
		}
	}

	private class SSLplusLookupHandler implements LookupHandler {
		private Logger logger = LoggerFactory.getLogger(SSLplusLookupHandler.class);

		@Override
		public Object lookup(String srcField, String dstField, Object value) {
			if (srcField.equals("login_name") && dstField.equals("name")) {
				if (value != null && !value.toString().isEmpty()) {
					String name = userApi.getUserNameByLoginName((String)value);
					return (name != null && !name.isEmpty()) ? name : "알 수 없음";
				} else
					return "알 수 없음";
			} else if (srcField.equals("login_name") && dstField.equals("org_unit_name")) {
				if (value != null) {
					String orgUnitId = userApi.getOrgUnitIdByLoginName((String)value);
					if (orgUnitId != null)
						return orgUnitApi.getOrgUnitNameByGuid(orgUnitId);
					else
						return "기타";
				} else
					return "기타";
			} else if (srcField.equals("server_ip") && dstField.equals("server_name")) {
				try {
					if (logger.isDebugEnabled())
						logger.debug("frodo core: lookup handler value [{}]", value.toString());
					if (value != null) {
						byte[] ip = InetAddress.getByName(value.toString()).getAddress();
						for (Integer mask : masks) {
							ServerIp serverIp = new ServerIp(masking(ip, mask), mask);
							String name = serverIpToServerName.get(serverIp);
							if (name != null)
								return name;
							else
								return "기타";
						}
					} else {
						return "기타";
					}
				} catch (UnknownHostException e) {
				}
			} else if (srcField.equals("logtype") && dstField.equals("hex_logtype")) {
				return String.format("0x%x", value);
			} else if (srcField.equals("code") && dstField.equals("auth_code_desc")) {
				if (value == null)
					return null;
				return getAuthCodeDesc(value.toString());
			}

			return null;
		}

		private byte[] masking(byte[] ip, int mask) {
			byte[] result = new byte[ip.length];
			for (int i = ip.length - 1; i >= 0; i--) {
				if (mask-- > 0)
					result[i] = 0;
				else
					result[i] = ip[i];
			}
			return result;
		}

		private String getAuthCodeDesc(String value) {
			if (value.equals("user-not-found"))
				return "사용자 ID 없음";
			else if (value.equals("password-fail"))
				return "암호 입력 실패";
			else if (value.equals("crl-connect-fail"))
				return "CRL 서버 접속 실패";
			else if (value.equals("ocsp-connect-fail"))
				return "OCSP 서버 접속 실패";
			else if (value.equals("cert-revoked"))
				return "파기된 인증서";
			else if (value.equals("policy-locked"))
				return "사용자 계정 잠김";
			else if (value.equals("policy-expired"))
				return "계정 유효기간 만료";
			else if (value.equals("policy-client-iprange"))
				return "접속 불가 클라이언트 IP 대역";
			else if (value.equals("policy-schedule"))
				return "접속 불가 시간대";
			else if (value.equals("radius-no-use"))
				return "RADIUS 비활성화";
			else if (value.equals("radius-reject"))
				return "RADIUS 인증 실패";
			else if (value.equals("radius-timeout"))
				return "RADIUS 서버 타임아웃";
			else if (value.equals("ldap-no-use"))
				return "LDAP 비활성화";
			else if (value.equals("rpc-error"))
				return "RPC 오류 발생";
			else if (value.equals("ldap-connect-fail"))
				return "LDAP 서버 접속 실패";
			else if (value.equals("ldap-reject"))
				return "LDAP 인증 실패";
			else if (value.equals("npki-fail"))
				return "공인인증서 불일치";
			else if (value.equals("ip-lease-fail"))
				return "IP 할당 실패";
			else if (value.equals("npki-idn-fail"))
				return "주민등록번호에 의한 공인인증서 본인확인 검증 실패";
			else if (value.equals("npki-not-found"))
				return "공인인증서 미등록 (본인확인 필요)";
			else if (value.equals("policy-mismatch"))
				return "잘못된 로그인 시도";
			else if (value.equals("tunnel-limit"))
				return "동시접속자 초과";
			else if (value.equals("device-fail"))
				return "비인가 단말";
			else if (value.equals("cert-expired"))
				return "인증서 기한 만료";
			else if (value.equals("cert-verify-fail"))
				return "신뢰할 수 없는 인증서";
			else if (value.equals("profile-not-found"))
				return "액세스 정책 없음";
			else if (value.equals("password-expired"))
				return "암호 기한 만료";
			else if (value.equals("otp-fail"))
				return "OTP 암호 입력 실패";
			else if (value.equals("idn-not-found"))
				return "주민번호 미등록";
			else if (value.equals("device-expired"))
				return "단말 유효기한 만료";
			else if (value.equals("device-locked"))
				return "단말 잠김";
			else if (value.equals("subject-dn-not-found"))
				return "인증서 주체 DN 미등록";
			else if (value.equals("duplicated-login"))
				return "중복 로그인";
			else if (value.equals("resident-number-fail"))
				return "주민번호 인증 실패";
			else if (value.equals("auto-locked"))
				return "휴면 계정 잠김";
			else if (value.equals("success")) {
				return "성공";
			}

			return "알 수 없음";
		}
	}

	private class ServerIp {
		private byte[] ip;
		private int mask;

		private ServerIp(InetAddress ip, int mask) {
			this(ip.getAddress(), mask);
		}

		private ServerIp(byte[] ip, int mask) {
			this.ip = ip;
			this.mask = mask;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + getOuterType().hashCode();
			result = prime * result + Arrays.hashCode(ip);
			result = prime * result + mask;
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			ServerIp other = (ServerIp) obj;
			if (!getOuterType().equals(other.getOuterType()))
				return false;
			if (!Arrays.equals(ip, other.ip))
				return false;
			if (mask != other.mask)
				return false;
			return true;
		}

		private DashboardPlugin getOuterType() {
			return DashboardPlugin.this;
		}
	}

//pks. 2015-01-29. 사용하지 않는 코드라 주석처리함.	
//	private class TunnelList {
//		private Logger logger = LoggerFactory.getLogger(TunnelList.class);
//		// session id to proccess ids
//		private Map<String, List<String>> sessionMap;
//		// process id to offset,limit
//		private Map<String, Map<String, Integer>> processMap;
//		private int[] viewIndexTable;
//		private int[] tunnelIndexTable;
//		private AtomicInteger viewCount;
//		private AtomicInteger tunnelCount;
//
//		public TunnelList() {
//			initiallize();
//
//			for (Tunnel t : auth.getTunnels()) {
//				put(t.getId());
//			}
//		}
//
//		public void stop() {
//			initiallize();
//		}
//
//		private void initiallize() {
//			viewCount = new AtomicInteger(0);
//			tunnelCount = new AtomicInteger(0);
//			if (sessionMap == null)
//				sessionMap = new HashMap<String, List<String>>();
//			if (processMap == null)
//				processMap = new HashMap<String, Map<String, Integer>>();
//			sessionMap.clear();
//			processMap.clear();
//
//			viewIndexTable = new int[65535];
//			tunnelIndexTable = new int[65535];
//			for (int index = 0; index < viewIndexTable.length; index++)
//				viewIndexTable[index] = tunnelIndexTable[index] = -1;
//		}
//
//		public List<Object> getTunnels(int offset, int limit) {
//			synchronized (viewIndexTable) {
//				List<Object> tunnels = new ArrayList<Object>();
//				if (offset < 0)
//					offset = 0;
//				int boundary = offset + limit;
//				if (boundary > viewCount.get())
//					boundary = viewCount.get();
//
//				for (int index = offset; index < boundary; index++) {
//					Tunnel tunnel = auth.getTunnel(viewIndexTable[index]);
//					if (tunnel == null)
//						continue;
//					tunnels.add(serialize(tunnel));
//				}
//				logger.trace("frodo core: tunnel list current offset[{}], limit[{}], view count [{}], send size [{}]",
//						new Object[] { offset, limit, viewCount, tunnels.size() });
//				return tunnels;
//			}
//		}
//
//		public Set<String> getProcessIds() {
//			return processMap.keySet();
//		}
//
//		public void removeProcessId(String processId) {
//			processMap.remove(processId);
//		}
//
//		public void addSession(String sessionId, String processId, int offset, int limit) {
//			logger.trace("frodo core: request get tunnels session_id[{}], process_id[{}]", sessionId, processId);
//			Map<String, Integer> m = new HashMap<String, Integer>();
//			m.put("offset", offset);
//			m.put("limit", limit);
//			processMap.put(processId, m);
//
//			List<String> ids = sessionMap.get(sessionId);
//			if (ids == null)
//				ids = new ArrayList<String>();
//			ids.add(processId);
//			sessionMap.put(sessionId, ids);
//		}
//
//		public void put(int tunnelId) {
//			synchronized (viewIndexTable) {
//				if (tunnelIndexTable[tunnelId] != -1)
//					return;
//
//				logger.trace("frodo core: insert tunnel id[{}], view count[{}]", tunnelId, this.viewCount);
//				viewIndexTable[this.viewCount.get()] = tunnelId;
//				tunnelIndexTable[tunnelId] = this.viewCount.get();
//				this.viewCount.incrementAndGet();
//				this.tunnelCount.incrementAndGet();
//			}
//		}
//
//		public void remove(int tunnelId) {
//			synchronized (viewIndexTable) {
//				AtomicInteger viewIndex = new AtomicInteger(tunnelIndexTable[tunnelId]);
//				if (viewIndex.get() < 0)
//					return;
//				AtomicInteger index = new AtomicInteger(viewIndex.get() + 1);
//				synchronized (index) {
//					tunnelIndexTable[tunnelId] = -1;
//
//					for (; index.get() < this.viewCount.get(); index.incrementAndGet()) {
//						int currentTunnelId = viewIndexTable[index.get()];
//						// shift view table
//						viewIndexTable[index.get() - 1] = currentTunnelId;
//						// replace view table location in tunnel table
//						tunnelIndexTable[currentTunnelId] = index.get() - 1;
//					}
//
//					this.viewIndexTable[viewCount.getAndDecrement()] = -1;
//					if (this.viewCount.get() < 0)
//						this.viewCount.set(0);
//					this.tunnelCount.decrementAndGet();
//				}
//			}
//		}
//
//		public boolean validate(String processId, int tunnelId) {
//			synchronized (viewIndexTable) {
//				Map<String, Integer> m = processMap.get(processId);
//				if (m == null)
//					return false;
//				Integer offset = m.get("offset");
//				Integer limit = m.get("limit");
//
//				if (offset == null || limit == null || limit > this.viewCount.get())
//					return false;
//
//				int viewIndex = tunnelIndexTable[tunnelId];
//				if (viewIndex >= offset && viewIndex < offset + limit)
//					return true;
//
//				return false;
//			}
//		}
//
//		public void removeSession(Session session) {
//			logger.trace("frodo core: remove session guid [{}], is exist [{}]", session.getGuid(),
//					sessionMap.containsKey(session.getGuid()));
//			List<String> ids = sessionMap.get(session.getGuid());
//			sessionMap.remove(session.getGuid());
//			if (ids == null)
//				return;
//
//			for (String id : ids)
//				processMap.remove(id);
//		}
//	}
	
	/*********************** pks: 2013-08-06. Power Status Check Code start. ***********************/
	public static final int POWER_STATUS_1 = 1;
	public static final int POWER_STATUS_2 = 2;
	
	private void checkPowerStatus() {
		if (checkSerialForPowerStatus()) {
			powerStatusThread = new Thread(new Runnable() {
				@Override
				public void run() {
					while (true) {
						try {
							readPowerStatus();
							Thread.sleep(60000); // wait 1 min
						} catch (InterruptedException e) {
							logger.info("frodo core: interrupted the thread which checking power status");
						}
					}
				}
			});
			powerStatusThread.setName("Power status check");
			powerStatusThread.start();
		}
	}
	
	private void nullifyPowerStatusThread() {
		logger.error("frodo core: cannot read power_info file");
		if(powerStatusThread != null) {
			powerStatusThread.interrupt();
			powerStatusThread = null;
		}
	}

	private void readPowerStatus() {
		powerStatusFlag = 0;		//initialize
		BufferedReader br = null;
		String line = null;

		try {
			br = getBufferedReaderToReadFile("/proc/utm/conf/power_info");
			if (br != null)
				line = br.readLine();
			else { //it means cannot read power_info file
				nullifyPowerStatusThread();
				return;
			}
			
			if (line != null) {
				// parse string "Power Monitor is running/stopped" to check the last word is 'running' or 'stopped'.
				int index = line.lastIndexOf(' ') + 1; 
				String subStr = line.substring(index);
				if (subStr.equals("running")) {
					byte temp = 0;
					for (int i = 0; i < 2; i++) {
						// parse string "Power #1 : on/off" to check the last word is 'on' or 'off'.
						line = br.readLine();
						index = line.lastIndexOf(' ') + 1;
						subStr = line.substring(index);
						
						if (subStr.equals("on")) { 
							if (i == 0)
								temp |= POWER_STATUS_1; // power_status_1 is on.
							else
								temp |= POWER_STATUS_2; // power_status_2 is on.
						}
					}
					powerStatusFlag = temp;
				} else if(subStr.equals("stopped")) {
					powerStatusFlag = -1; //doesn't support power management.
				}
			}
		} catch (IOException e) {
			logger.error("frodo core: cannot read power_info file");
		} finally {
			try {
				if (br != null)
					br.close();
			} catch (IOException e) {
			}
		}
	}

	private boolean checkSerialForPowerStatus() {
		BufferedReader br = null;
		String serial = null;

		try {
			br = getBufferedReaderToReadFile("/proc/utm/serial");
			if (br != null)
				serial = br.readLine();
		} catch (IOException e) {
			logger.error("frodo core: cannot read serial file");
		} finally {
			try {
				if (br != null)
					br.close();
			} catch (IOException e) {
			}
		}

		// pks: 2013-08-06. if only device is 3000 or 5000 then check power status.
		if (serial != null) {
			if (serial.startsWith("WSS301") || serial.startsWith("WSS501"))
				return true;
		}
		return false;
	}

	private BufferedReader getBufferedReaderToReadFile(String fileName) {
		File f = new File(fileName);

		if (f.exists()) {
			BufferedReader br = null;
			try {
				br = new BufferedReader(new InputStreamReader(new FileInputStream(f)));
			} catch (IOException e) {
				logger.error("frodo core: cannot read " + fileName + " file");
			}
			return br;
		}
		return null;
	}
	
	// pks: 2013-08-06. put power status information to response.
	private void putPowerStatusInfo(Response resp) {
		if(powerStatusFlag == -1)	//if power management wasn't supported then the related UI wouldn't be show up.
			return;
		
		if ((powerStatusFlag & POWER_STATUS_1) == 1)
			resp.put("power_status_1", true);
		else
			resp.put("power_status_1", false);

		if ((powerStatusFlag & POWER_STATUS_2) == 2)
			resp.put("power_status_2", true);
		else
			resp.put("power_status_2", false);
	}
	
	/*********************** pks: 2013-08-06. Power Status Check Code end. ***********************/	
}