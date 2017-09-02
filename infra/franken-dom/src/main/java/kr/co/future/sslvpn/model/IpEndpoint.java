package kr.co.future.sslvpn.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import kr.co.future.api.CollectionTypeHint;
import kr.co.future.api.FieldOption;
import kr.co.future.msgbus.Marshalable;
import kr.co.future.msgbus.Marshaler;

public class IpEndpoint implements Marshalable {
	@FieldOption(name = "ip4_addr", length = 20)
	private String ip4Address;

	@FieldOption(name = "ip4_mask", nullable = false)
	private int ip4mask = 32;

	@FieldOption(name = "ip6_addr", length = 40)
	private String ip6Address;

	@FieldOption(name = "ip6_mask", nullable = false)
	private int ip6mask = 128;
	
	@FieldOption(name = "description", length = 250)
	private String description;

	@CollectionTypeHint(PortRange.class)
	private List<PortRange> portRanges = new ArrayList<PortRange>();

	public String getIp4Address() {
		return ip4Address;
	}

	public void setIp4Address(String ip4Address) {
		this.ip4Address = ip4Address;
	}

	public String getIp6Address() {
		return ip6Address;
	}

	public void setIp6Address(String ip6Address) {
		this.ip6Address = ip6Address;
	}

	public int getIp4Mask() {
		return ip4mask;
	}

	public void setIp4Mask(int ip4mask) {
		this.ip4mask = ip4mask;
	}

	public int getIp6Mask() {
		return ip6mask;
	}

	public void setIp6Mask(int ip6mask) {
		this.ip6mask = ip6mask;
	}

	public List<PortRange> getPortRanges() {
		return portRanges;
	}

	public void setPortRanges(List<PortRange> portRanges) {
		this.portRanges = portRanges;
	}
	
	public String getDescription() {
		return description;
	}
	
	public void setDescription(String description) {
		this.description = description;
	}

	@Override
	public Map<String, Object> marshal() {
		Map<String, Object> m = new HashMap<String, Object>();
		m.put("ip4_addr", ip4Address);
		m.put("ip4_mask", ip4mask);
		m.put("ip6_addr", ip6Address);
		m.put("ip6_mask", ip6mask);
		m.put("description", description);
		m.put("port_ranges", Marshaler.marshal(portRanges));
		return m;
	}
}
