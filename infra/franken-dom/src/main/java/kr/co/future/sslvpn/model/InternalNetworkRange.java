package kr.co.future.sslvpn.model;

import java.util.HashMap;
import java.util.Map;

import kr.co.future.api.FieldOption;
import kr.co.future.msgbus.Marshalable;

public class InternalNetworkRange implements Marshalable {
	@FieldOption(name = "ip", nullable = false)
	private String ip;

	@FieldOption(name = "cidr", nullable = false)
	private int cidr;

	public String getIp() {
		return ip;
	}

	public void setIp(String ip) {
		this.ip = ip;
	}

	public int getCidr() {
		return cidr;
	}

	public void setCidr(int cidr) {
		this.cidr = cidr;
	}

	@Override
	public Map<String, Object> marshal() {
		Map<String, Object> m = new HashMap<String, Object>();
		m.put("ip", ip);
		m.put("cidr", cidr);
		return m;
	}

}
