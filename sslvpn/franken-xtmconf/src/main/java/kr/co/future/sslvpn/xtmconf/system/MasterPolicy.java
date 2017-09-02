package kr.co.future.sslvpn.xtmconf.system;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class MasterPolicy {
	private List<String> optionTag;
	private Map<String, Set<String>> attrMap;
	private Map<String, Set<String>> xmlMap;

	public MasterPolicy() {
		optionTag = new ArrayList<String>();
		attrMap = new HashMap<String, Set<String>>();
		xmlMap = new HashMap<String, Set<String>>();

		setOptionTag();
		setAttrs();
		setXmlMap();
	}

	public boolean isKey(String key) {
		return optionTag.contains(key);
	}

	public Set<String> getArrKeys(String key) {
		return attrMap.get(key);
	}

	public Set<String> getXmls(String attr) {
		return xmlMap.get(attr);
	}

	private void setOptionTag() {
		optionTag.add("fw");
		optionTag.add("object");
		optionTag.add("ipsec");
		optionTag.add("log");
		optionTag.add("waf");
		optionTag.add("dbsync");
	}

	private void setAttrs() {
		Set<String> firewall_chk_xml = new HashSet<String>();
		firewall_chk_xml.add("chk_v4spd");
		firewall_chk_xml.add("chk_v4nat");
		firewall_chk_xml.add("chk_v6spd");
		firewall_chk_xml.add("chk_v6nat");
		attrMap.put("fw", firewall_chk_xml);

		Set<String> object_chk_xml = new HashSet<String>();
		object_chk_xml.add("chk_ip");
		object_chk_xml.add("chk_service");
		object_chk_xml.add("chk_flow");
		object_chk_xml.add("chk_schedule");
		object_chk_xml.add("chk_qos");
		object_chk_xml.add("chk_user");
		attrMap.put("object", object_chk_xml);

		Set<String> ipsec_chk_xml = new HashSet<String>();
		ipsec_chk_xml.add("chk_ipsec");
		attrMap.put("ipsec", ipsec_chk_xml);

		Set<String> log_chk_xml = new HashSet<String>();
		log_chk_xml.add("chk_log");
		log_chk_xml.add("chk_syslog");
		log_chk_xml.add("chk_alarm");
		attrMap.put("log", log_chk_xml);

		Set<String> waf_chk_xml = new HashSet<String>();
		waf_chk_xml.add("chk_url");
		attrMap.put("waf", waf_chk_xml);

		Set<String> db_chk_cdb = new HashSet<String>();
		db_chk_cdb.add("chk_domsync");
		db_chk_cdb.add("chk_devicesync");
		attrMap.put("dbsync", db_chk_cdb);
	}

	private void setXmlMap() {
		Set<String> chk_v4spd = new HashSet<String>();
		chk_v4spd.add("firewall_policy.xml");
		xmlMap.put("chk_v4spd", chk_v4spd);

		Set<String> chk_v4nat = new HashSet<String>();
		chk_v4nat.add("firewall_nat.xml");
		xmlMap.put("chk_v4nat", chk_v4nat);

		Set<String> chk_v6spd = new HashSet<String>();
		chk_v6spd.add("firewall_IPv6.xml");
		xmlMap.put("chk_v6spd", chk_v6spd);

		Set<String> chk_v6nat = new HashSet<String>();
		chk_v6nat.add("firewall_IPv6_nat.xml");
		xmlMap.put("chk_v6nat", chk_v6nat);

		Set<String> chk_ip = new HashSet<String>();
		chk_ip.add("object_ip_address.xml");
		chk_ip.add("object_ip_group.xml");
		chk_ip.add("object_ipv6_address.xml");
		chk_ip.add("object_ipv6_group.xml");
		chk_ip.add("object_ipv6_header.xml");
		xmlMap.put("chk_ip", chk_ip);

		Set<String> chk_service = new HashSet<String>();
		chk_service.add("object_service_ftp.xml");
		chk_service.add("object_service_group.xml");
		chk_service.add("object_service_http.xml");
		chk_service.add("object_service_port.xml");
		chk_service.add("object_service_rpc.xml");
		chk_service.add("object_service_smtp.xml");
		xmlMap.put("chk_service", chk_service);

		Set<String> chk_flow = new HashSet<String>();
		chk_flow.add("object_session.xml");
		xmlMap.put("chk_flow", chk_flow);

		Set<String> chk_schedule = new HashSet<String>();
		chk_schedule.add("object_schedule.xml");
		xmlMap.put("chk_schedule", chk_schedule);

		Set<String> chk_qos = new HashSet<String>();
		chk_qos.add("object_qos.xml");
		xmlMap.put("chk_qos", chk_qos);

		Set<String> chk_user = new HashSet<String>();
		chk_user.add("object_user_group.xml");
		chk_user.add("object_user_list.xml");
		xmlMap.put("chk_user", chk_user);

		Set<String> chk_ipsec = new HashSet<String>();
		chk_ipsec.add("vpn_setting.xml");
		chk_ipsec.add("vpn_algorithm.xml");
		chk_ipsec.add("vpn_host_v6.xml");
		chk_ipsec.add("vpn_isakmpsa.xml");
		chk_ipsec.add("vpn_algorithm_v6.xml");
		chk_ipsec.add("vpn_ipsec.xml");
		chk_ipsec.add("vpn_host.xml");
		chk_ipsec.add("vpn_ipsecsa.xml");
		xmlMap.put("chk_ipsec", chk_ipsec);

		Set<String> chk_log = new HashSet<String>();
		chk_log.add("log_setting.xml");
		xmlMap.put("chk_log", chk_log);

		Set<String> chk_syslog = new HashSet<String>();
		chk_syslog.add("syslog_setting.xml");
		xmlMap.put("chk_syslog", chk_syslog);

		Set<String> chk_alarm = new HashSet<String>();
		chk_alarm.add("alarm_setting.xml");
		xmlMap.put("chk_alarm", chk_alarm);

		Set<String> chk_url = new HashSet<String>();
		chk_url.add("waf_basic.xml");
		chk_url.add("waf_injection.xml");
		chk_url.add("waf_dir.xml");
		chk_url.add("waf_outflow.xml");
		chk_url.add("waf_header.xml");
		chk_url.add("waf_webserver.xml");
		chk_url.add("waf_http.xml");
		xmlMap.put("chk_url", chk_url);

		Set<String> chk_domsync = new HashSet<String>();
		chk_domsync.add("kraken-dom-localhost.cdb");
		xmlMap.put("chk_domsync", chk_domsync);

		Set<String> chk_deviceync = new HashSet<String>();
		chk_deviceync.add("devices.tmp");
		xmlMap.put("chk_devicesync", chk_deviceync);
	}
}