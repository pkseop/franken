package kr.co.future.sslvpn.model;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Date;

import kr.co.future.api.FieldOption;
import kr.co.future.confdb.CollectionName;

@CollectionName("ip_leases")
public class IpLease {
	@FieldOption(nullable = false)
	private long ip;

	@FieldOption(name = "login_name", nullable = false)
	private String loginName;

	@FieldOption(name = "tunnel_id", nullable = false)
	private int tunnelId;

	@FieldOption(name = "leased_at", nullable = false)
	private Date leaseDate;

	@FieldOption(name = "expired_at", nullable = false)
	private Date expireDate;

	// for cluster operation, search by profile name and try ip allocation using
	// offset
	private String profileName;

	/**
	 * node id in cluster mode. otherwise 0
	 */
	@FieldOption(name = "node", nullable = false)
	private int node;

	/**
	 * offset from begin address. ignore if offset is 0. use offset for ip
	 * allocation in cluster mode
	 */
	private long ipOffset;

	public long getIp() {
		return ip;
	}

	public void setIp(long ip) {
		this.ip = ip;
	}

	public String getLoginName() {
		return loginName;
	}

	public void setLoginName(String loginName) {
		this.loginName = loginName;
	}

	public int getTunnelId() {
		return tunnelId;
	}

	public void setTunnelId(int tunnelId) {
		this.tunnelId = tunnelId;
	}

	public Date getLeaseDate() {
		return leaseDate;
	}

	public void setLeaseDate(Date leaseDate) {
		this.leaseDate = leaseDate;
	}

	public Date getExpireDate() {
		return expireDate;
	}

	public void setExpireDate(Date expireDate) {
		this.expireDate = expireDate;
	}

	public String getProfileName() {
		return profileName;
	}

	public void setProfileName(String profileName) {
		this.profileName = profileName;
	}

	public int getNode() {
		return node;
	}

	public void setNode(int node) {
		this.node = node;
	}

	public long getIpOffset() {
		return ipOffset;
	}

	public void setIpOffset(long offset) {
		this.ipOffset = offset;
	}

	@Override
	public String toString() {
		return "ip=" + toInetAddress(ip) + ", user=" + loginName + ", tunnel=" + tunnelId + ", lease_at=" + leaseDate
				+ ", expire_at=" + expireDate;
	}

	public static long toLong(String ip) {
		Inet4Address addr = null;
		try {
			addr = (Inet4Address) Inet4Address.getByName(ip);
		} catch (UnknownHostException e) {
		}

		byte[] b = addr.getAddress();
		int l = 0;
		for (int i = 0; i < 4; i++) {
			l <<= 8;
			l |= (b[i] & 0xff);
		}
		return l & 0xffffffffL;
	}

	public static InetAddress toInetAddress(long ipAddr) {
		long copyAddr = ipAddr;
		byte b1 = (byte) ((copyAddr >> 24) & 0xff);
		byte b2 = (byte) ((copyAddr >> 16) & 0xff);
		byte b3 = (byte) ((copyAddr >> 8) & 0xff);
		byte b4 = (byte) (copyAddr & 0xff);
		try {
			return InetAddress.getByAddress(new byte[] { b1, b2, b3, b4 });
		} catch (UnknownHostException e) {
		}
		return null;
	}
}
