package kr.co.future.sslvpn.model;

import java.util.HashMap;
import java.util.Map;

import kr.co.future.api.FieldOption;
import kr.co.future.confdb.CollectionName;
import kr.co.future.msgbus.Marshalable;

@CollectionName("ip_lease_ranges")
public class IpLeaseRange implements Marshalable {
	@FieldOption(name = "ip_from", length = 20)
	private String ipFrom;

	@FieldOption(name = "ip_to", length = 20)
	private String ipTo;

	public String getIpFrom() {
		return ipFrom;
	}

	public void setIpFrom(String ipFrom) {
		this.ipFrom = ipFrom;
	}

	public long getIpFromLong() {
		return IpLease.toLong(ipFrom);
	}

	public String getIpTo() {
		return ipTo;
	}

	public long getIpToLong() {
		return IpLease.toLong(ipTo);
	}

	public void setIpTo(String ipTo) {
		this.ipTo = ipTo;
	}

	public int getPoolSize() {
		return (int) (IpLease.toLong(ipTo) - IpLease.toLong(ipFrom) + 1);
	}

	@Override
	public Map<String, Object> marshal() {
		Map<String, Object> m = new HashMap<String, Object>();
		m.put("ip_from", ipFrom);
		m.put("ip_to", ipTo);

		return m;
	}

	@Override
	public String toString() {
		return "ipFrom=[" + ipFrom + "], ip_to=[" + ipTo + "]";
	}

}
