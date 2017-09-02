package kr.co.future.sslvpn.xtmconf.system;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class InitFileKey {
	private Map<String, Set<String>> m;
	private Map<String, String> m2;

	public InitFileKey() {
		m = new HashMap<String, Set<String>>();
		m2 = new HashMap<String, String>();

		setKeyMap();
		setCategoryMap();
	}

	public Set<String> getMasterKeys() {
		return m.keySet();
	}
	
	public boolean isMasterKey(String masterKey) { 
		return m.containsKey(masterKey);
	}
	
	public Set<String> getSecondKeys(String masterKey) {
		return m.get(masterKey);
	}

	public String getRootNode(String secondKey) {
		return m2.get(secondKey);
	}

	private void setKeyMap() {
		/* key: master key, value: second key */
		Set<String> fsGateObject = new HashSet<String>();
		fsGateObject.add("center_setup");
		fsGateObject.add("system_basic");
		fsGateObject.add("system_setting_option");
		fsGateObject.add("system_user_admin");
		fsGateObject.add("system_user_access");
		fsGateObject.add("system_reservation");
		fsGateObject.add("system_snmp");
		fsGateObject.add("network_interface");
		fsGateObject.add("network_bridge");
		fsGateObject.add("network_bonding");
		fsGateObject.add("network_vlan");
		fsGateObject.add("network_virtual_ip");
		fsGateObject.add("network_virtual_ipv6");
		fsGateObject.add("network_radius");
		fsGateObject.add("network_dhcp_server");
		fsGateObject.add("network_dhcp_relay");
		fsGateObject.add("network_ddns");
		fsGateObject.add("network_splitDNS_innerDNS");
		fsGateObject.add("network_splitDNS_outerDNS");
		fsGateObject.add("network_tunneling_6in4");
		fsGateObject.add("network_tunneling_6to4");
		fsGateObject.add("network_router_static");
		fsGateObject.add("network_router_policy");
		fsGateObject.add("network_router_vrrp");
		fsGateObject.add("network_router_multicast");
		fsGateObject.add("network_router_checker");
		fsGateObject.add("network_router_script");
		fsGateObject.add("network_anomaly");
		fsGateObject.add("network_anomaly_portscan");
		fsGateObject.add("network_anomaly_spoofing");
		fsGateObject.add("network_ip_manager");
		fsGateObject.add("log_setting");
		fsGateObject.add("log_rtt_setting");
		fsGateObject.add("syslog_setting");
		fsGateObject.add("alarm_setting");
		fsGateObject.add("vpn_setting");
		fsGateObject.add("ssl_setting");
		fsGateObject.add("nac_setting");
		fsGateObject.add("nac_scan");
		fsGateObject.add("nac_arp_spoofing");
		fsGateObject.add("waf_webserver");
		fsGateObject.add("waf_basic");
		fsGateObject.add("waf_dir");
		fsGateObject.add("waf_header");
		fsGateObject.add("waf_http");
		fsGateObject.add("waf_injection");
		fsGateObject.add("waf_outflow");
		fsGateObject.add("anti_virus");
		fsGateObject.add("anti_spam");
		fsGateObject.add("anti_smtp");
		fsGateObject.add("ip_manager");
		fsGateObject.add("ip_scan");
		fsGateObject.add("manage_monitor");
		fsGateObject.add("sync_session_backup");
		fsGateObject.add("sync_session_guarantee");
		fsGateObject.add("fw_script");
		fsGateObject.add("vpn_script");
		fsGateObject.add("link_pack_script");
		fsGateObject.add("ha_head_script");
		fsGateObject.add("ha_branch_script");
		fsGateObject.add("alg_telnet_proxy");
		fsGateObject.add("alg_ftp_proxy");
		fsGateObject.add("certificatelist");
		m.put("FsGateObject", fsGateObject);

		Set<String> fsFireWall = new HashSet<String>();
		fsFireWall.add("firewall_policy");
		fsFireWall.add("firewall_nat");
		fsFireWall.add("firewall_ipv6");
		fsFireWall.add("firewall_vlan");
		fsFireWall.add("vlan_setting");
		fsFireWall.add("firewall_ipv6_nat");
		m.put("FsFirewall", fsFireWall);
		
		Set<String> fsObject = new HashSet<String>();
		fsObject.add("object_ip_address");
		fsObject.add("object_ip_group");
		fsObject.add("object_ipv6_address");
		fsObject.add("object_ipv6_group");
		fsObject.add("object_ipv6_header");
		fsObject.add("object_qos");
		fsObject.add("object_schedule");
		fsObject.add("object_service_ftp");
		fsObject.add("object_service_group");
		fsObject.add("object_service_http");
		fsObject.add("object_service_port");
		fsObject.add("object_service_rpc");
		fsObject.add("object_session");
		fsObject.add("object_user_group");
		fsObject.add("object_user_list");
		m.put("FsObject", fsObject);
		
		Set<String> fsVpn = new HashSet<String>();
		fsVpn.add("vpn_host");
		fsVpn.add("vpn_isakmpsa");
		fsVpn.add("vpn_ipsecsa");
		fsVpn.add("vpn_ipsec");
		fsVpn.add("ssl_access_server");
		fsVpn.add("ssl_access_group");
		fsVpn.add("ssl_access_user");
		m.put("FsVpn", fsVpn);
	}

	private void setCategoryMap() {
		/* key: second key, value: root node */
		m2.put("center_setup", "setting");
		m2.put("system_basic", "system");
		m2.put("system_setting_option", "system");
		m2.put("system_user_admin", "system");
		m2.put("system_user_access", "system");
		m2.put("system_reservation", "system");
		m2.put("system_snmp", "system");
		m2.put("network_interface", "network");
		m2.put("network_bridge", "network");
		m2.put("network_bonding", "network");
		m2.put("network_vlan", "network");
		m2.put("network_virtual_ip", "network");
		m2.put("network_virtual_ipv6", "network");
		m2.put("network_radius", "nac");
		m2.put("network_dhcp_server", "network");
		m2.put("network_dhcp_relay", "network");
		m2.put("network_ddns", "network");
		m2.put("network_splitDNS_innerDNS", "dns");
		m2.put("network_splitDNS_outerDNS", "dns");
		m2.put("network_tunneling_6in4", "network");
		m2.put("network_tunneling_6to4", "network");
		m2.put("network_router_static", "routing");
		m2.put("network_router_policy", "routing");
		m2.put("network_router_vrrp", "routing");
		m2.put("network_router_multicast", "network");
		m2.put("network_router_checker", "routing");
		m2.put("network_router_script", "network_router_script");
		m2.put("network_anomaly", "anomaly");
		m2.put("network_anomaly_portscan", "anomaly");
		m2.put("network_anomaly_spoofing", "anomaly");
		m2.put("network_ip_manager", "network");
		m2.put("log_setting", "log");
		m2.put("log_rtt_setting", "manage");
		m2.put("syslog_setting", "manage");
		m2.put("alarm_setting", "alarm");
		m2.put("vpn_setting", "vpn");
		m2.put("ssl_setting", "ssl");
		m2.put("nac_setting", "nac");
		m2.put("nac_scan", "nac");
		m2.put("nac_arp_spoofing", "spoofing");
		m2.put("waf_webserver", "waf");
		m2.put("waf_basic", "waf");
		m2.put("waf_dir", "waf");
		m2.put("waf_header", "waf");
		m2.put("waf_http", "waf");
		m2.put("waf_injection", "waf");
		m2.put("waf_outflow", "waf");
		m2.put("anti_virus", "virus");
		m2.put("anti_spam", "spam");
		m2.put("anti_smtp", "smtp");
		m2.put("ip_manager", "network");
		m2.put("ip_scan", "network");
		m2.put("manage_monitor", "manage");
		m2.put("sync_session_backup", "sync");
		m2.put("sync_session_guarantee", "sync");
		m2.put("fw_script", "fw_script");
		m2.put("vpn_script", "vpn_script");
		m2.put("link_pack_script", "link_pack_script");
		m2.put("ha_head_script", "ha_head_script");
		m2.put("ha_branch_script", "ha_branch_script");
		m2.put("alg_telnet_proxy", "alg");
		m2.put("alg_ftp_proxy", "alg");
		m2.put("certificatelist", "cert");

		m2.put("firewall_policy", "policy");
		m2.put("firewall_nat", "policy");
		m2.put("firewall_ipv6", "policy");
		m2.put("firewall_vlan", "policy");
		m2.put("vlan_setting", "policy");
		m2.put("firewall_ipv6_nat", "policy");

		m2.put("object_ip_address", "object");
		m2.put("object_ip_group", "object");
		m2.put("object_ipv6_address", "object");
		m2.put("object_ipv6_group", "object");
		m2.put("object_ipv6_header", "object");
		m2.put("object_qos", "object");
		m2.put("object_schedule", "object");
		m2.put("object_service_ftp", "object");
		m2.put("object_service_group", "object");
		m2.put("object_service_http", "object");
		m2.put("object_service_port", "object");
		m2.put("object_service_rpc", "object");
		m2.put("object_session", "object");
		m2.put("object_user_group", "object");
		m2.put("object_user_list", "object");

		m2.put("vpn_host", "vpn");
		m2.put("vpn_isakmpsa", "vpn");
		m2.put("vpn_ipsecsa", "vpn");
		m2.put("vpn_ipsec", "vpn");
		m2.put("ssl_access_server", "ssl");
		m2.put("ssl_access_group", "ssl");
		m2.put("ssl_access_user", "ssl");
	}
}