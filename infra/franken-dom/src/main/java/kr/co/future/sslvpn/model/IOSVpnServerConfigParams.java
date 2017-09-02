package kr.co.future.sslvpn.model;

import java.util.List;

public class IOSVpnServerConfigParams {
	private String vpnIp;
	private String vpnNetmask;
	private Integer sslPort;
	private String dnsAddr1;
	private String dnsAddr2;
	private Boolean useIOS;
	
	public Boolean getUseIOS() {
		return useIOS;
	}

	public void setUseIOS(Boolean useIOS) {
		this.useIOS = useIOS;
	}

	private List<String> encryptions;
	
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

	public void setSslPort(Integer sslPort) {
		this.sslPort = sslPort;
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
