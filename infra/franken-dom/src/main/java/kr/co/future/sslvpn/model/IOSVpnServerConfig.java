package kr.co.future.sslvpn.model;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import kr.co.future.api.CollectionTypeHint;
import kr.co.future.api.FieldOption;
import kr.co.future.confdb.CollectionName;
import kr.co.future.msgbus.Marshalable;

@CollectionName("ios_vpn_server_config")
public class IOSVpnServerConfig implements Marshalable {
	
	@FieldOption(name = "vpn_ip", nullable = false)
	private String vpnIp;

	@FieldOption(name = "vpn_netmask", nullable = false)
	private String vpnNetmask;
	
	@FieldOption(name = "use_ios", nullable = false)
	private Boolean useIOS = false;

	// port number for tunnel
	@FieldOption(nullable = false)
	private Integer sslPort = 4885;
	
	@CollectionTypeHint(String.class)
	private List<String> encryptions = new ArrayList<String>();
	
	@FieldOption(name = "dns_addr1", nullable = true)
	private String dnsAddr1;

	@FieldOption(name = "dns_addr2", nullable = true)
	private String dnsAddr2;
	
	@FieldOption(name = "created_at", nullable = false)
	private Date createDateTime;

	@FieldOption(name = "updated_at", nullable = false)
	private Date updateDateTime;
	
	public Date getCreateDateTime() {
		return createDateTime;
	}

	public void setCreateDateTime(Date createDateTime) {
		this.createDateTime = createDateTime;
	}

	public Date getUpdateDateTime() {
		return updateDateTime;
	}

	public void setUpdateDateTime(Date updateDateTime) {
		this.updateDateTime = updateDateTime;
	}
	
	public String getVpnNetmask() {
		return vpnNetmask;
	}

	public void setVpnNetmask(String vpnNetmask) {
		this.vpnNetmask = vpnNetmask;
	}
	
	public String getVpnIp() {
		return vpnIp;
	}

	public void setVpnIp(String vpnIp) {
		this.vpnIp = vpnIp;
	}
	
	public int getSslPort() {
		return sslPort;
	}
	
	public void setSslPort(int sslPort) {
		this.sslPort = sslPort;
	}
	
	public List<String> getEncryptions() {
		return encryptions;
	}

	public void setEncryptions(List<String> encryptions) {
		this.encryptions = encryptions;
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

	public Boolean getUseIOS() {
		return useIOS;
	}

	public void setUseIOS(Boolean useIOS) {
		this.useIOS = useIOS;
	}
	
	@Override
	public Map<String, Object> marshal() {
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ssZ");
		Map<String, Object> m = new HashMap<String, Object>();
		
		m.put("vpn_ip", vpnIp);
		m.put("vpn_netmask", vpnNetmask);
		m.put("ssl_port", sslPort);
		m.put("encryptions", encryptions);
		m.put("dns_addr1", dnsAddr1);
		m.put("dns_addr2", dnsAddr2);
		m.put("use_ios", useIOS);
		m.put("created_at", dateFormat.format(createDateTime));
		m.put("updated_at", dateFormat.format(updateDateTime));
		
		return m;
	}
}
