package kr.co.future.sslvpn.model;

import java.util.HashMap;
import java.util.Map;

import kr.co.future.api.FieldOption;
import kr.co.future.msgbus.Marshalable;

public class SplitRoutingEntry implements Marshalable {
	@FieldOption(name = "ip", length = 20, nullable = false)
	private String ip;

	@FieldOption(name = "cidr", nullable = false)
	private int cidr;

	@FieldOption(name = "gateway", length = 20)
	private String gateway;

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

	public String getGateway() {
		return gateway;
	}

	public void setGateway(String gateway) {
		this.gateway = gateway;
	}

	@Override
	public Map<String, Object> marshal() {
		Map<String, Object> m = new HashMap<String, Object>();
		m.put("ip", ip);
		m.put("cidr", cidr);
		m.put("gateway", gateway);
		return m;
	}
}
