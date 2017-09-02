package kr.co.future.sslvpn.core.xenics.impl;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import kr.co.future.sslvpn.model.AccessProfile;

public class Users {
	public int tunnel_id;
	public int client_id;	
	public String user_id;
	public String rent_ip;
	public String trust_ip;
	public Timestamp login_date;
	public String tun_name;
	public int using_flag;
	
	public Map<String, Object> mappingToTunnelInfo(AccessProfile profile) {
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ssZ");
		Map<String, Object> m = new HashMap<String, Object>();
		m.put("id", tunnel_id);
		m.put("type", null);
		m.put("login_name", user_id);
		m.put("profile_name", profile != null ? profile.getName() : null);
		m.put("lease_ip", rent_ip);
		m.put("remote_addr", trust_ip);
		m.put("login_at", dateFormat.format(new Date(login_date.getTime())));
		m.put("tx_bytes", 0);
		m.put("rx_bytes", 0);
		m.put("tx_pkts", 0);
		m.put("rx_pkts", 0);
		return m;
	}
	
	@Override
	public String toString() {
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ssZ");
		
		StringBuilder sb = new StringBuilder();
		sb.append("tunnel_id=").append(tunnel_id).append(", client_id=").append(client_id).append(", user_id=").append(user_id)
		.append(", rent_ip=").append(rent_ip).append(", trust_ip=").append(trust_ip).append(", login_date=").append(dateFormat.format(new Date(login_date.getTime())));
		
		return sb.toString();
	}
}
