package kr.co.future.sslvpn.core.msgbus;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import kr.co.future.sslvpn.model.AccessGateway.DeviceAuthMode;
import kr.co.future.sslvpn.model.AccessProfile;
import kr.co.future.sslvpn.model.AuthorizedDevice;
import kr.co.future.sslvpn.model.ClientApp;
import kr.co.future.sslvpn.model.ClientIpRange;
import kr.co.future.sslvpn.model.IpLeaseRange;
import kr.co.future.sslvpn.model.Server;
import kr.co.future.sslvpn.model.SplitRoutingEntry;
import kr.co.future.sslvpn.model.api.AccessProfileApi;
import kr.co.future.sslvpn.model.api.AuthorizedDeviceApi;
import kr.co.future.sslvpn.model.api.ClientAppApi;
import kr.co.future.sslvpn.model.api.ClientCheckProfileApi;
import kr.co.future.sslvpn.model.api.ServerApi;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;

import kr.co.future.api.PrimitiveConverter;
import kr.co.future.msgbus.MsgbusException;
import kr.co.future.msgbus.Request;
import kr.co.future.msgbus.Response;
import kr.co.future.msgbus.handler.MsgbusMethod;
import kr.co.future.msgbus.handler.MsgbusPlugin;
import kr.co.future.sslvpn.core.ImportCsv;
import kr.co.future.sslvpn.core.servlet.csv.ExportObject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(name = "frodo-import-csv")
@Provides
@MsgbusPlugin
public class ImportCsvPlugin implements ImportCsv{
	
	private Logger logger = LoggerFactory.getLogger(ImportCsvPlugin.class);
	
	@Requires
	private ServerApi serverApi;
	
	@Requires
	private AccessProfileApi profileApi;
	
	@Requires
	private AuthorizedDeviceApi authDeviceApi;
	
	@Requires
	private ClientCheckProfileApi clientCheckProfileApi;
	
	@Requires
	private ClientAppApi clientAppApi;
	
	@MsgbusMethod
	public void importAuthorizedDevices(Request req, Response resp) {
		List<Map<String, String>> datas = null;
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");
		
		try {
			datas = parseCsv(req.getString("csv"), req.getString("charset"));
		} catch (IOException e) {
			throw new MsgbusException("frodo", "invalid csv");
		}
		List<AuthorizedDevice> createDevices = new ArrayList<AuthorizedDevice>();
		List<AuthorizedDevice> updateDevices = new ArrayList<AuthorizedDevice>();
		for(Map<String, String> data : datas) {
			Map<String, Object> m = new HashMap<String, Object>();
			String guid = data.get("guid");
			
			m.put("guid", guid);
			m.put("device_key", data.get("device_key"));
			if(data.get("type") != null)
				m.put("type", Integer.parseInt(data.get("type")));
			m.put("host_name", data.get("host_name"));
			m.put("description", data.get("description"));
			m.put("owner", data.get("owner"));
			m.put("remote_ip", data.get("remote_ip"));
			if(data.get("is_blocked") != null)
				m.put("is_blocked", Boolean.parseBoolean(data.get("is_blocked")));
			m.put("expiration", data.get("expiration") == null ? null : dateParse(data.get("expiration"), sdf));
			m.put("login_name", data.get("login_name"));
			m.put("hdd_serial", data.get("hdd_serial"));
			m.put("mac_address", data.get("mac_address"));
			m.put("remote_client_ip", data.get("remote_client_ip"));
			if(data.get("is_authorized") != null)
				m.put("is_authorized", Boolean.parseBoolean(data.get("is_authorized")));
			
			m.put("org_unit_name", data.get("org_unit_name"));
			
			AuthorizedDevice authDevice = authDeviceApi.getDevice(guid);
			if(authDevice == null) {
				authDevice = (AuthorizedDevice)PrimitiveConverter.overwrite(new AuthorizedDevice(), m);
				createDevices.add(authDevice);
			} else {
				authDevice = (AuthorizedDevice) PrimitiveConverter.overwrite(authDevice, m);
				updateDevices.add(authDevice);
			}
			
		}
		if(createDevices.size() > 0)
			authDeviceApi.registerDevices(createDevices);
		if(updateDevices.size() > 0)
			authDeviceApi.updateDevices(updateDevices);
	}
	
	@MsgbusMethod	
	public void importIPV4Filtering(Request req, Response resp) {
		executePHP(req, resp, "mode=1");
	}
	
	@MsgbusMethod	
	public void importIPV4NAT(Request req, Response resp) {
		executePHP(req, resp, "mode=2");
	}
	
	
	private void executePHP(Request req, Response resp, String param){
		String file = "";
		String php = "/var/www/webadmin/firewall_java_ins.php";
		try {
			if (param == "mode=1"){
				file =  "/utm/log/tmp/firewall_policy_ins_temp.csv";
			} else if (param == "mode=2"){
				file =  "/utm/log/tmp/firewall_nat_ins_temp.csv";
			}
			String decoded = new String(decodeBase64(req.getString("csv")), Charset.forName(req.getString("charset")));
			PrintWriter out = new PrintWriter(file, "euc-kr");
			out.print(decoded);
			out.close();
			logger.info("frodo core: csv tmp file save complete.");
			
			File phpFile = new File("/var/www/webadmin/firewall_java_ins.php");
			if (!phpFile.exists())
				throw new MsgbusException("frodo", "firewall_java_ins.php not exist.");
			ProcessBuilder builder = new ProcessBuilder("/usr/bin/php", "-q", php, param, "file="+file);
			Process p = builder.start();
			logger.info("frodo core: running command {}, param {}", php, param);
			int waitFor = p.waitFor();
			InputStream is = p.getInputStream();
			BufferedReader br = new BufferedReader(new InputStreamReader(is, "euc-kr"));
			String line = br.readLine();
			clear(p.getErrorStream());
			StringBuilder sb = new StringBuilder();
			try {
				while (line != null){
					sb.append(line).append(System.lineSeparator());
					line = br.readLine();
				}
			} catch (IOException e) {
			} finally {
				if (is != null) {
					try {
						is.close();
					} catch (IOException e) {
					}
				}
			}
			logger.info("frodo core: import csv result => {}",sb.toString());
		} catch (Exception e) {
			logger.error("import csv failed", e);
			throw new MsgbusException("frodo", "invalid csv");
		} finally{
			File f = new File(file);
			f.delete();
			logger.info("frodo core: import csv tmp file delete complete.");
		}
	}
	
	private void clear(InputStream is) {
		StringBuilder sb = new StringBuilder();
		try {
			byte[] b = new byte[4096];
			while (true) {
				int read = is.read(b);
				if (read <= 0)
					break;

				sb.append(new String(b, 0, read));
			}
		} catch (IOException e) {
		} finally {
			if (is != null) {
				try {
					is.close();
				} catch (IOException e) {
				}
			}

			logger.info("frodo core: php error stream output [{}]", sb.toString());
		}
	}
	
	@MsgbusMethod	
	public void importAccessProfiles(Request req, Response resp) {
		List<Map<String, String>> datas = null;
		
		try {
			datas = parseCsv(req.getString("csv"), req.getString("charset"));
			importAccessProfiles(datas);
		} catch (Exception e) {
			logger.error("import csv failed", e);
			throw new MsgbusException("frodo", "invalid csv");
		}
	}
	
	public void importAccessProfiles(List<Map<String, String>> datas) throws Exception {
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");
		
		for(Map<String, String> data : datas) {
			Map<String, Object> m = new HashMap<String, Object>();
			String guid = data.get("guid");
			
			m.put("id", Integer.parseInt(data.get("id")));
			m.put("guid", guid);
			m.put("name", data.get("name"));
			m.put("description", data.get("description"));
			m.put("allow_time_id", data.get("allow_time_id"));
			m.put("verify_client_ip", data.get("verify_client_ip") == null || data.get("verify_client_ip").equals("") ? false : Boolean.parseBoolean(data.get("verify_client_ip")));
			m.put("use_nac", data.get("use_nac") == null ? false : Boolean.parseBoolean(data.get("use_nac")));
			m.put("ip_from", data.get("ip_from"));
			m.put("ip_to", data.get("ip_to"));
			m.put("ip_lease_ranges", data.get("ip_lease_ranges") == null || data.get("ip_lease_ranges").equals("") ? new ArrayList<IpLeaseRange>() : getIpLeaseRanges(data.get("ip_lease_ranges")));
			m.put("use_client_timeout", data.get("use_client_timeout") == null ? false : Boolean.parseBoolean(data.get("use_client_timeout")));
			if(data.get("max_client_timeout") != null)
				m.put("max_client_timeout", Integer.parseInt(data.get("max_client_timeout")));
			m.put("client_timeout", data.get("client_timeout") == null ? 3600 : Integer.parseInt(data.get("client_timeout")));
			m.put("use_fail_limit", data.get("use_fail_limit") == null ? false : Boolean.parseBoolean(data.get("use_fail_limit")));
			m.put("fail_limit", data.get("fail_limit") == null ? 5 : Integer.parseInt(data.get("fail_limit")));
			m.put("password_expiry", data.get("password_expiry") == null ? 0 : Integer.parseInt(data.get("password_expiry")));
			m.put("client_check_profile", data.get("client_check_profile") == null ? null : clientCheckProfileApi.getClientCheckProfile(data.get("client_check_profile")));
			m.put("created_at", dateParse(data.get("created_at"), sdf));
			m.put("updated_at", dateParse(data.get("updated_at"), sdf));
			if(data.get("login_method") != null)
				m.put("login_method", Integer.parseInt(data.get("login_method")));
			m.put("encryptions", data.get("encryptions") == null ? null : getEncryptions(data.get("encryptions")));
			if(data.get("password_change_alert") != null)
				m.put("password_change_alert", Integer.parseInt(data.get("password_change_alert")));
			if(data.get("account_expiry_alert") == null)
				m.put("account_expiry_alert", Long.parseLong(data.get("account_expiry_alert")));
			m.put("device_auth_mode", data.get("device_auth_mode") == null ? null : DeviceAuthMode.valueOf(data.get("device_auth_mode")));
			m.put("client_ip_ranges", data.get("client_ip_ranges") == null ? new ArrayList<ClientIpRange>() : getClientIpRanges(data.get("client_ip_ranges")));
			if(data.get("use_split_routing") != null)
				m.put("use_split_routing", Boolean.parseBoolean(data.get("use_split_routing")));
			m.put("split_routing_entries", data.get("split_routing_entries") == null ? new ArrayList<SplitRoutingEntry>() : getSplitRoutingEntries(data.get("split_routing_entries")));
			if(data.get("user_unlock_time") != null)
				m.put("user_unlock_time", Integer.parseInt(data.get("user_unlock_time")));
			m.put("popup_url", data.get("popup_url"));
			if(data.get("ldap_attributes") != null)
				m.put("ldap_attributes",  new HashMap<String, Object>().put("use_password_change", Boolean.parseBoolean(data.get("ldap_attributes"))));			
			if(data.get("use_auto_reconnect") != null)
				m.put("use_auto_reconnect", Boolean.parseBoolean(data.get("use_auto_reconnect")));
			if(data.get("use_client_auto_uninstall") != null)
				m.put("use_client_auto_uninstall", Boolean.parseBoolean(data.get("use_client_auto_uninstall")));
			if(data.get("use_proxy") != null)
				m.put("use_proxy", Boolean.parseBoolean(data.get("use_proxy")));
			
			boolean isExist = false;
			AccessProfile profile = profileApi.getAccessProfile(guid);
			if(profile == null) {
				profile = (AccessProfile)PrimitiveConverter.overwrite(new AccessProfile(), m);
			} else {
				profile = (AccessProfile) PrimitiveConverter.overwrite(profile, m);
				isExist = true;
			}
			
			if(data.containsKey("normal_acl")) {				
				List<Server> list = getNormalAcl(data.get("normal_acl"));
				if(list.size() > 0) {
					profile.setNormalAcl(list);
				}
			}
			
			if(data.containsKey("quarantine_acl")) {
				List<Server> list = getNormalAcl(data.get("quarantine_acl"));
				if(list.size() > 0) {
					profile.setQuarantineAcl(list);					
				}
			}
			
			if(data.containsKey("client_app")) {
				List<ClientApp> list = getClientApp(data.get("client_app"));
				if(list.size() > 0) {
					profile.setClientApps(list);
				}
			}
			
			if(isExist)
				profileApi.updateAccessProfile(profile);
			else
				profileApi.createAccessProfile(profile);
		}
		
	}
	
	private List<Map<String, Object>> getSplitRoutingEntries(String splitRoutingEntriesStr) throws Exception {
		List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();
		
		String[] splitRoutingEntries = splitRoutingEntriesStr.split(ExportObject.DELIM_1ST);
		if(splitRoutingEntries.length == 1 && !splitRoutingEntries[0].contains(ExportObject.DELIM_2ND))
			oldWay_getSplitRoutingEntries(splitRoutingEntriesStr, list);
		else {
			for(String splitRoutingEntry : splitRoutingEntries) {
				String[] clientIpRangeDatas = splitRoutingEntry.split(ExportObject.DELIM_2ND);
				Map<String, Object> m = new HashMap<String, Object>();
				m.put("ip", clientIpRangeDatas[0]);
				m.put("cidr", Integer.parseInt(clientIpRangeDatas[1]));
				if(clientIpRangeDatas.length == 3 && !clientIpRangeDatas[2].equals("null"))
					m.put("gateway", clientIpRangeDatas[2]);
				else
					m.put("gateway", null); 
				
				list.add(m);
			}
		}
		
		return list;
	}
	
	private void oldWay_getSplitRoutingEntries(String splitRoutingEntriesStr, List<Map<String, Object>> list) throws Exception {
		String[] splitRoutingEntries = splitRoutingEntriesStr.split("&");
		for(String splitRoutingEntry : splitRoutingEntries) {
			String[] clientIpRangeDatas = splitRoutingEntry.split("\\|");
			Map<String, Object> m = new HashMap<String, Object>();
			m.put("ip", clientIpRangeDatas[0]);
			m.put("cidr", Integer.parseInt(clientIpRangeDatas[1]));
			if(clientIpRangeDatas.length == 3 && !clientIpRangeDatas[2].equals("null"))
				m.put("gateway", clientIpRangeDatas[2]);
			else
				m.put("gateway", null); 
			
			list.add(m);
		}
	}
	
	private List<String> getEncryptions(String encryptionsStr) throws Exception {
		List<String> list = new ArrayList<String>();
		
		String[] encryptions = encryptionsStr.split(ExportObject.DELIM_2ND);
		if(encryptions.length == 1)
			oldWay_getEncryptions(encryptionsStr, list);
		else {
			for(String encryption : encryptions) {
				list.add(encryption);
			}
		}
		
		return list;
	}
	
	private void oldWay_getEncryptions(String encryptionsStr, List<String> list) throws Exception {
		String[] encryptions = encryptionsStr.split("\\|");
		for(String encryption : encryptions) {
			list.add(encryption);
		}
	}
	
	private List<Map<String, Object>> getClientIpRanges(String clientIpRangesStr) throws Exception {
		List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();
		
		String[] clientIpRanges = clientIpRangesStr.split(ExportObject.DELIM_1ST);
		if(clientIpRanges.length == 1 && !clientIpRanges[0].contains(ExportObject.DELIM_2ND))
			oldWay_getClientIpRanges(clientIpRangesStr, list);
		else {
			for(String clientIpRange : clientIpRanges) {
				String[] clientIpRangeDatas = clientIpRange.split(ExportObject.DELIM_2ND);
				Map<String, Object> m = new HashMap<String, Object>();
				m.put("ip_from", clientIpRangeDatas[0]);
				m.put("ip_to", clientIpRangeDatas[1]);
				
				list.add(m);
			}
		}
		
		return list;
	}
	
	private void oldWay_getClientIpRanges(String clientIpRangesStr, List<Map<String, Object>> list) throws Exception {
		String[] clientIpRanges = clientIpRangesStr.split("&");
		for(String clientIpRange : clientIpRanges) {
			String[] clientIpRangeDatas = clientIpRange.split("\\|");
			Map<String, Object> m = new HashMap<String, Object>();
			m.put("ip_from", clientIpRangeDatas[0]);
			m.put("ip_to", clientIpRangeDatas[1]);
			
			list.add(m);
		}
	}
	
	private List<Map<String, Object>> getIpLeaseRanges(String ipLeaseRangesStr) throws Exception {
		List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();
		
		String[] ipLeaseRanges = ipLeaseRangesStr.split(ExportObject.DELIM_1ST);
		if(ipLeaseRanges.length == 1 && !ipLeaseRanges[0].contains(ExportObject.DELIM_2ND))
			oldWay_getIpLeaseRanges(ipLeaseRangesStr, list);
		else {
			for(String ipLeaseRange : ipLeaseRanges) {
				String[] ipLeaseRangesDatas = ipLeaseRange.split(ExportObject.DELIM_2ND);
				Map<String, Object> m = new HashMap<String, Object>();
				m.put("ip_from", ipLeaseRangesDatas[0]);
				m.put("ip_to", ipLeaseRangesDatas[1]);
				
				list.add(m);
			}
		}
		
		return list;
	}
	
	private void oldWay_getIpLeaseRanges(String ipLeaseRangesStr, List<Map<String, Object>> list) throws Exception {
		String[] ipLeaseRanges = ipLeaseRangesStr.split("&");
		for(String ipLeaseRange : ipLeaseRanges) {
			String[] ipLeaseRangesDatas = ipLeaseRange.split("\\|");
			Map<String, Object> m = new HashMap<String, Object>();
			m.put("ip_from", ipLeaseRangesDatas[0]);
			m.put("ip_to", ipLeaseRangesDatas[1]);
			
			list.add(m);
		}
	}
	
	private List<ClientApp> getClientApp(String clientAppStr) throws Exception {
		List<ClientApp> list = new ArrayList<ClientApp>();
		
		String[] clientApps = clientAppStr.split(ExportObject.DELIM_1ST);
		if(clientApps.length == 1 && !clientApps[0].contains(ExportObject.DELIM_2ND)) 
			oldWay_getClientApp(clientAppStr, list);
		else {
			for(String clientApp : clientApps) {
				String[] clientAppDatas = clientApp.split(ExportObject.DELIM_2ND);
				ClientApp ca = clientAppApi.getClientApp(clientAppDatas[1]);
				list.add(ca);
			}
		}
		
		return list;
	}
	
	private void oldWay_getClientApp(String clientAppStr, List<ClientApp> list) throws Exception {
		String[] clientApps = clientAppStr.split("&");
		for(String clientApp : clientApps) {
			String[] clientAppDatas = clientApp.split("\\|");
			ClientApp ca = clientAppApi.getClientApp(clientAppDatas[1]);
			list.add(ca);
		}
	}
	
	private List<Server> getNormalAcl(String normalAclStr) throws Exception {
		List<Server> list = new ArrayList<Server>();
		
		String[] normalAcls = normalAclStr.split(ExportObject.DELIM_1ST);
		if(normalAcls.length == 1 && !normalAcls[0].contains(ExportObject.DELIM_2ND))
			oldWay_getNormalAcl(normalAclStr, list);
		else {
			for(String normalAcl : normalAcls) {
				String[] normalAclDatas = normalAcl.split(ExportObject.DELIM_2ND);
				Server server = serverApi.getServer(normalAclDatas[1]);
				list.add(server);
			}
		}
		
		return list;
	}
	
	private void oldWay_getNormalAcl(String normalAclStr, List<Server> list) throws Exception {
		String[] normalAcls = normalAclStr.split("&");
		for(String normalAcl : normalAcls) {
			String[] normalAclDatas = normalAcl.split("\\|");
			Server server = serverApi.getServer(normalAclDatas[1]);
			list.add(server);
		}
	}
	
	@MsgbusMethod
	public void importServers(Request req, Response resp) throws Exception {
		List<Map<String, String>> datas = null;
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");
		
		try {
			datas = parseCsv(req.getString("csv"), req.getString("charset"));
		} catch (IOException e) {
			throw new MsgbusException("frodo", "invalid csv");
		}
		
		for(Map<String, String> data : datas) {
			Map<String, Object> m = new HashMap<String, Object>();
			String guid = data.get("guid");
			
			m.put("guid", guid);
			m.put("name", data.get("name"));
			m.put("description", data.get("description"));
			m.put("operator", data.get("operator"));
			m.put("phone", data.get("phone"));
			
			if(data.containsKey("created_at")) {
				Date created_at = dateParse(data.get("created_at"), sdf);
				m.put("created_at", created_at);
			}
			
			if(data.containsKey("updated_at")) {
				Date updated_at = dateParse(data.get("updated_at"), sdf);
				m.put("updated_at", updated_at);
			}
			
			m.put("endpoints", getEndPoint(data.get("endpoints")));
			
			Server server = serverApi.getServer(guid);
			if(server == null) {
				server = (Server) PrimitiveConverter.overwrite(new Server(), m);				
				serverApi.createServer(server);
			} else {
				server = (Server) PrimitiveConverter.overwrite(server, m);
				serverApi.updateServer(server);
			}
		}
		
	}
	
	private List<Map<String, Object>> getEndPoint(String endpointsStr) throws Exception {
		List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();
		
		String[] endpoints = endpointsStr.split(ExportObject.DELIM_1ST);
		if(endpoints.length == 1 && !endpoints[0].contains(ExportObject.DELIM_2ND))
			oldWay_getEndPoint(endpointsStr, list);
		else {
			for(String endpoint : endpoints) {
				String[] endpointDatas = endpoint.split(ExportObject.DELIM_2ND);
				Map<String, Object> m = new HashMap<String, Object>();
				m.put("ip4_addr", endpointDatas[0]);
				m.put("ip4_mask", Integer.parseInt(endpointDatas[1]));
				m.put("ip6_addr", endpointDatas[2]);
				m.put("ip6_mask", Integer.parseInt(endpointDatas[3]));
				m.put("description", endpointDatas[4]);
				m.put("port_ranges", getProtRange(endpointDatas[5]));
				
				list.add(m);
			}
		}
		
		return list;
	}
	
	private void oldWay_getEndPoint(String endpointsStr, List<Map<String, Object>> list) throws Exception {
		String[] endpoints = endpointsStr.split("&");
		for(String endpoint : endpoints) {
			String[] endpointDatas = endpoint.split("\\|");
			Map<String, Object> m = new HashMap<String, Object>();
			m.put("ip4_addr", endpointDatas[0]);
			m.put("ip4_mask", Integer.parseInt(endpointDatas[1]));
			m.put("ip6_addr", endpointDatas[2]);
			m.put("ip6_mask", Integer.parseInt(endpointDatas[3]));
			m.put("description", endpointDatas[4]);
			m.put("port_ranges", getProtRange(endpointDatas[5]));
			
			list.add(m);
		}
	}
	
	private List<Map<String, Object>> getProtRange(String portRangeStr) throws Exception {
		List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();
		
		String[] portRanges = portRangeStr.split(ExportObject.DELIM_3RD);
		if(portRanges.length == 1 &&  !portRanges[0].contains(ExportObject.DELIM_4TH))
			oldWay_getProtRange(portRangeStr, list);
		else {
			for(String portRange : portRanges) {
				String[] portRangeDatas = portRange.split(ExportObject.DELIM_4TH);
				Map<String, Object> m = new HashMap<String, Object>();
				m.put("protocol", portRangeDatas[0]);
				m.put("port_from", Integer.parseInt(portRangeDatas[1]));
				m.put("port_to", Integer.parseInt(portRangeDatas[2]));
				
				list.add(m);
			}
		}
		
		return list;
	}
	
	private void oldWay_getProtRange(String portRangeStr, List<Map<String, Object>> list) throws Exception {
		String[] portRanges = portRangeStr.split("@");
		for(String portRange : portRanges) {
			String[] portRangeDatas = portRange.split("#");
			Map<String, Object> m = new HashMap<String, Object>();
			m.put("protocol", portRangeDatas[0]);
			m.put("port_from", Integer.parseInt(portRangeDatas[1]));
			m.put("port_to", Integer.parseInt(portRangeDatas[2]));
			
			list.add(m);
		}
	}
	
	private Date dateParse(String source, SimpleDateFormat sdf) {
		try {
			return sdf.parse(source);
		} catch (ParseException e) {
			return null;
		}
	}
	
	private static byte[] decodeMap = new byte[128];
	static {
		int i = 0;
		byte b = 'A';
		for (; i < 26; i++)
			decodeMap[b++] = (byte) i;
		b = 'a';
		for (; i < 52; i++)
			decodeMap[b++] = (byte) i;
		b = '0';
		for (; i < 62; i++)
			decodeMap[b++] = (byte) i;
		decodeMap['+'] = 62;
		decodeMap['/'] = 63;
	}
	
	private byte[] decodeBase64(String src) {
		char[] ch = src.toCharArray();

		int len = ch.length / 4 * 3;
		if (ch[len - 1] == '=')
			len--;
		if (ch[len - 2] == '=')
			len--;

		byte[] result = new byte[len];
		for (int i = 0; i < ch.length / 4; i++) {
			byte a = decodeMap[ch[i * 4]];
			byte b = decodeMap[ch[i * 4 + 1]];
			byte c = decodeMap[ch[i * 4 + 2]];
			byte d = decodeMap[ch[i * 4 + 3]];

			long l = (a & 0x3F) << 18 | (b & 0x3F) << 12 | (c & 0x3F) << 6 | (d & 0x3F);
			for (int j = 2; j >= 0; j--) {
				if (i * 3 + j < result.length)
					result[i * 3 + j] = (byte) (l & 0xFF);
				l >>= 8;
			}
		}
		return result;
	}
	
	private List<Map<String, String>> parseCsv(String[] header, String decoded, List<Map<String, String>> result) throws IOException {
		logger.info(decoded);
		for (String line : decoded.split("\n")) {
			line = line.replace("\r", "");
			if (line.trim().isEmpty())
				continue;
			String[] values = parseCsvLine(line);
			if (header == null)
				header = values;
			else {
				try {
					Map<String, String> m = new HashMap<String, String>();
					for (int i = 0; i < header.length; i++) {
						if (!values[i].trim().isEmpty())
							m.put(header[i], values[i].trim());
					}
					result.add(m);
				} catch (Exception e) {
					logger.error("frodo core: user import failed [{}]", line);
				}
			}
		}
		return result;
	}
	
	public List<Map<String, String>> parseCsvNoDecode(byte[] csvBytes, String charset) throws IOException {
		List<Map<String, String>> result = new ArrayList<Map<String, String>>();
		String[] header = null;
		String decoded = new String(csvBytes, Charset.forName(charset));
		parseCsv(header, decoded, result);
		return result;
	}
	
	private List<Map<String, String>> parseCsv(String csv, String charset) throws IOException {
		List<Map<String, String>> result = new ArrayList<Map<String, String>>();
		String[] header = null;
		String decoded = new String(decodeBase64(csv), Charset.forName(charset));
		parseCsv(header, decoded, result);
		return result;
	}

	private static int lookup(char[] c, int index) {
		try {
			while (true) {
				if (c[index] != ' ')
					return index;
				index++;
			}
		} catch (IndexOutOfBoundsException e) {
			return -1;
		}
	}
	
	private static String[] parseCsvLine(String str) throws IOException {
		List<String> result = new ArrayList<String>();
		StringBuilder sb = new StringBuilder();
		char[] c = str.toCharArray();
		boolean quote = false;
		boolean begin = false;
		for (int i = 0; i < c.length; i++) {
			if ((quote && c[i] == '"' && (lookup(c, i + 1) == -1 || c[lookup(c, i + 1)] == ',')) || (!quote && c[i] == ',')) {
				if (quote) {
					i = lookup(c, i + 1);
					if (i == -1)
						i = c.length;
				}
				result.add(sb.toString());
				sb = new StringBuilder();
				begin = false;
				quote = false;
				continue;
			}

			if (begin) {
				if (c[i] == '"') {
					if (c[i + 1] == '"') {
						sb.append('"');
						i++;
					} else {
						if (!quote || c[lookup(c, i + 1)] != ',')
							throw new IOException("invalid format, index " + i);
					}
				} else
					sb.append(c[i]);
			} else {
				begin = true;
				if (c[lookup(c, i)] == '"') {
					i = lookup(c, i);
					quote = true;
				} else
					sb.append(c[i]);
			}
		}
		if (begin)
			result.add(sb.toString());
		if (str.endsWith(","))
			result.add("");
		return result.toArray(new String[0]);
	}
}
