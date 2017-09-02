package kr.co.future.sslvpn.model;

import java.util.HashMap;
import java.util.Map;

import kr.co.future.api.FieldOption;
import kr.co.future.msgbus.Marshalable;

public class PortRange implements Marshalable {
	@FieldOption(name = "protocol", length = 6, nullable = false)
	private String protocol;

	@FieldOption(name = "port_from", nullable = false)
	private int portFrom;

	@FieldOption(name = "port_to", nullable = false)
	private int portTo;

	public int getPortFrom() {
		return portFrom;
	}

	public void setPortFrom(int portFrom) {
		this.portFrom = portFrom;
	}

	public int getPortTo() {
		return portTo;
	}

	public void setPortTo(int portTo) {
		this.portTo = portTo;
	}

	public String getProtocol() {
		return protocol;
	}

	public void setProtocol(String protocol) {
		if (protocol == null)
			throw new IllegalArgumentException("protocol can not be null");

		if (!protocol.equalsIgnoreCase("tcp") && !protocol.equalsIgnoreCase("udp"))
			throw new IllegalArgumentException("protocol should be tcp or udp");

		this.protocol = protocol.toLowerCase();
	}

	@Override
	public Map<String, Object> marshal() {
		Map<String, Object> m = new HashMap<String, Object>();
		m.put("protocol", protocol);
		m.put("port_from", portFrom);
		m.put("port_to", portTo);
		return m;
	}

	@Override
	public String toString() {
		return "protocol=" + protocol + ", port_from=" + portFrom + ", port_to=" + portTo;
	}
}
