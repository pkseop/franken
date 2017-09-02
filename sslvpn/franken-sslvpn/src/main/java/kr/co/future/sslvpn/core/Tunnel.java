package kr.co.future.sslvpn.core;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import kr.co.future.msgbus.Marshalable;

public class Tunnel implements Marshalable {
	private int id;

	// one of sslvpn, l2tp, xauth
	private String type;
	private String loginName;
	private int profileId;
	private String profileName;
	private long txBytes;
	private long rxBytes;
	private long txPackets;
	private long rxPackets;
	private Date loginDateTime;
	private InetAddress leaseIp;
	private InetSocketAddress remoteAddress;
	private String hostGuid;
	private String subjectDn;

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public int getProfileId() {
		return profileId;
	}

	public void setProfileId(int profileId) {
		this.profileId = profileId;
	}

	public String getLoginName() {
		return loginName;
	}

	public void setLoginName(String loginName) {
		this.loginName = loginName;
	}

	public String getProfileName() {
		return profileName;
	}

	public void setProfileName(String profileName) {
		this.profileName = profileName;
	}

	public long getTxBytes() {
		return txBytes;
	}

	public void setTxBytes(long txBytes) {
		this.txBytes = txBytes;
	}

	public long getRxBytes() {
		return rxBytes;
	}

	public void setRxBytes(long rxBytes) {
		this.rxBytes = rxBytes;
	}

	public long getTxPackets() {
		return txPackets;
	}

	public void setTxPackets(long txPackets) {
		this.txPackets = txPackets;
	}

	public long getRxPackets() {
		return rxPackets;
	}

	public void setRxPackets(long rxPackets) {
		this.rxPackets = rxPackets;
	}

	public Date getLoginDateTime() {
		return loginDateTime;
	}

	public void setLoginDateTime(Date loginDateTime) {
		this.loginDateTime = loginDateTime;
	}

	public InetAddress getLeaseIp() {
		return leaseIp;
	}

	public void setLeaseIp(InetAddress leaseIp) {
		this.leaseIp = leaseIp;
	}

	public InetSocketAddress getRemoteAddress() {
		return remoteAddress;
	}

	public void setRemoteAddress(InetSocketAddress remoteAddress) {
		this.remoteAddress = remoteAddress;
	}

	public String getHostGuid() {
		return hostGuid;
	}

	public void setHostGuid(String hostGuid) {
		this.hostGuid = hostGuid;
	}

	public String getSubjectDn() {
		return subjectDn;
	}

	public void setSubjectDn(String subjectDn) {
		this.subjectDn = subjectDn;
	}

	@Override
	public String toString() {
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ssZ");
		String message = "tunnel id=" + id + ", login=" + loginName + ", login_at=" + dateFormat.format(loginDateTime)
				+ ", lease_ip=" + leaseIp + ", remote=" + remoteAddress + ", profile=" + profileId + ", rx_bytes=" + rxBytes
				+ ", tx_bytes=" + txBytes + ", rx_pkts=" + rxPackets + ", tx_pkts=" + txPackets + ",subject_dn=" + subjectDn;

		return message;
	}

	@Override
	public Map<String, Object> marshal() {
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ssZ");
		Map<String, Object> m = new HashMap<String, Object>();
		m.put("id", id);
		m.put("type", type);
		m.put("login_name", loginName);
		m.put("profile_id", profileId);
		m.put("profile_name", profileName);
		m.put("remote_addr", remoteAddress.getAddress().getHostAddress() + ":" + remoteAddress.getPort());
		m.put("lease_ip", leaseIp.getHostAddress());
		m.put("login_at", dateFormat.format(loginDateTime));
		m.put("rx_bytes", rxBytes);
		m.put("tx_bytes", txBytes);
		m.put("rx_pkts", rxPackets);
		m.put("tx_pkts", txPackets);
		return m;
	}
}
