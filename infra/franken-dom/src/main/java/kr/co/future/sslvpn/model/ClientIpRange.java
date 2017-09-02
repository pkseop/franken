package kr.co.future.sslvpn.model;

import kr.co.future.api.FieldOption;

public class ClientIpRange {

	@FieldOption(name = "ip_from", nullable = false)
	private String ipFrom;

	@FieldOption(name = "ip_to", nullable = false)
	private String ipTo;

	public String getIpFrom() {
		return ipFrom;
	}

	public void setIpFrom(String ipFrom) {
		this.ipFrom = ipFrom;
	}

	public String getIpTo() {
		return ipTo;
	}

	public void setIpTo(String ipTo) {
		this.ipTo = ipTo;
	}

	@Override
	public String toString() {
		return "ClientIpRange [ip_from=" + ipFrom + ", ip_to=" + ipTo + "]";
	}
}
