package kr.co.future.sslvpn.model;

import java.util.List;

public class VpnServerConfigParams {
	private String vpnIp;
	private String vpnNetmask;
	private Integer sslPort;
	private String dnsAddr1;
	private String dnsAddr2;
	private Boolean useRemoteDb;
	private String remoteKillIp;
	private String remoteDbHostName;
	private String remoteDbLoginName;
	private String remoteDbPassword;
	private String remoteDbName;
	private String remoteDbTableName;
	private String remoteDbPort;
	private String remoteDbSocket;
	private List<String> encryptions;
	private boolean useObfuscationKey;
	private Boolean useTcpAcceleration;
	private Boolean usePacketCompress;
	private Integer proxyPort;
	
	public String getVpnIp() {
		return vpnIp;
	}

	public void setVpnIp(String vpnIp) {
		this.vpnIp = vpnIp;
	}

	public String getVpnNetmask() {
		return vpnNetmask;
	}

	public void setVpnNetmask(String vpnNetmask) {
		this.vpnNetmask = vpnNetmask;
	}

	public Boolean getUseRemoteDb() {
		return useRemoteDb;
	}

	public void setUseRemoteDb(Boolean useRemoteDb) {
		this.useRemoteDb = useRemoteDb;
	}
	
	public String getRemoteKillIp() {
		return remoteKillIp;
	}

	public void setRemoteKillIp(String remoteKillIp) {
		this.remoteKillIp = remoteKillIp;
	}

	public String getRemoteDbHostName() {
		return remoteDbHostName;
	}

	public void setRemoteDbHostName(String remoteDbHostName) {
		this.remoteDbHostName = remoteDbHostName;
	}

	public String getRemoteDbLoginName() {
		return remoteDbLoginName;
	}

	public void setRemoteDbLoginName(String remoteDbLoginName) {
		this.remoteDbLoginName = remoteDbLoginName;
	}

	public String getRemoteDbPassword() {
		return remoteDbPassword;
	}

	public void setRemoteDbPassword(String remoteDbPassword) {
		this.remoteDbPassword = remoteDbPassword;
	}

	public String getRemoteDbName() {
		return remoteDbName;
	}

	public void setRemoteDbName(String remoteDbName) {
		this.remoteDbName = remoteDbName;
	}

	public String getRemoteDbTableName() {
		return remoteDbTableName;
	}

	public void setRemoteDbTableName(String remoteDbTableName) {
		this.remoteDbTableName = remoteDbTableName;
	}

	public String getRemoteDbPort() {
		return remoteDbPort;
	}

	public void setRemoteDbPort(String remoteDbPort) {
		this.remoteDbPort = remoteDbPort;
	}

	public String getRemoteDbSocket() {
		return remoteDbSocket;
	}

	public void setRemoteDbSocket(String remoteDbSocket) {
		this.remoteDbSocket = remoteDbSocket;
	}

	public void setSslPort(Integer sslPort) {
		this.sslPort = sslPort;
	}
	
	public Integer getProxyPort() {
		return proxyPort;
	}

	public void setProxyPort(Integer proxyPort) {
		this.proxyPort = proxyPort;
	}
	
	public Boolean getUsePacketCompress() {
		return usePacketCompress;
	}
	
	public void setUsePacketCompress(Boolean usePacketCompress) {
		this.usePacketCompress = usePacketCompress;
	}
	
	public Boolean getUseTcpAcceleration() {
		return useTcpAcceleration;
	}
	
	public void setUseTcpAcceleration(Boolean useTcpAcceleration) {
		this.useTcpAcceleration = useTcpAcceleration;
	}
	
	public boolean isUseObfuscationKey() {
		return useObfuscationKey;
	}
	
	public void setUseObfuscationKey(boolean useObfuscationKey) {
		this.useObfuscationKey = useObfuscationKey;
	}
	
	public int getSslPort() {
		return sslPort;
	}
	
	public void setSslPort(int sslPort) {
		this.sslPort = sslPort;
	}
	
	public String getDnsAddr1() {
		return dnsAddr1;
	}
	
	public void setDnsAddr1(String dnsAddr1) {
		this.dnsAddr1 = dnsAddr1;
	}
	
	public String getDnsAddr2() {
		return dnsAddr2;
	}
	
	public void setDnsAddr2(String dnsAddr2) {
		this.dnsAddr2 = dnsAddr2;
	}
	
	public List<String> getEncryptions() {
		return encryptions;
	}
	
	public void setEncryptions(List<String> encryptions) {
		this.encryptions = encryptions;
	}
}
