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

@CollectionName("vpn_server_config")
public class VpnServerConfig implements Marshalable {
	
	@FieldOption(name = "vpn_ip", nullable = false)
	private String vpnIp;

	@FieldOption(name = "vpn_netmask", nullable = false)
	private String vpnNetmask;

	// port number for tunnel
	@FieldOption(nullable = false)
	private Integer sslPort = 4886;
	
	@FieldOption(name = "use_obfuscation_key", nullable = false)
	private Boolean useObfuscationKey = false;
	
	@FieldOption(name = "obfuscation_key", nullable = true)
	private String obfuscationKey = null;
	
	@CollectionTypeHint(String.class)
	private List<String> encryptions = new ArrayList<String>();
	
	@FieldOption(name = "dns_addr1", nullable = true)
	private String dnsAddr1;

	@FieldOption(name = "dns_addr2", nullable = true)
	private String dnsAddr2;
	
	@FieldOption(name = "use_remote_db", nullable = false)
	private Boolean useRemoteDb = false;
	
	@FieldOption(name = "remote_kill_ip", nullable = true)
	private String remoteKillIp;
	
	@FieldOption(name = "remote_db_host_name", nullable = true)
	private String remoteDbHostName;
	
	@FieldOption(name = "remote_db_login_name", nullable = true)
	private String remoteDbLoginName;
	
	@FieldOption(name = "remote_db_password", nullable = true)
	private String remoteDbPassword;
	
	@FieldOption(name = "remote_db_name", nullable = true)
	private String remoteDbName;
	
	@FieldOption(name = "remote_db_table_name", nullable = true)
	private String remoteDbTableName;
	
	@FieldOption(name = "remote_db_port", nullable = true)
	private String remoteDbPort;
	
	@FieldOption(name = "remote_db_socket", nullable = true)
	private String remoteDbSocket;
	
	@FieldOption(name = "proxy_port", nullable = false)
	private Integer proxyPort = 24886;
	
	@FieldOption(name = "use_tcp_acceleration", nullable = false)
	private Boolean useTcpAcceleration = false;
	
	@FieldOption(name = "use_packet_compress", nullable = false)
	private Boolean usePacketCompress = false;
	
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
	
	public boolean isUseObfuscationKey() {
		return useObfuscationKey;
	}

	public void setUseObfuscationKey(boolean useObfuscationKey) {
		this.useObfuscationKey = useObfuscationKey;
	}
	
	public String getObfuscationKey() {
		return obfuscationKey;
	}

	public void setObfuscationKey(String obfuscationKey) {
		this.obfuscationKey = obfuscationKey;
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
	
	public Integer getProxyPort() {
		return proxyPort;
	}

	public void setProxyPort(Integer proxyPort) {
		this.proxyPort = proxyPort;
	}
	
	public Boolean getUseTcpAcceleration() {
		if (useTcpAcceleration == null)
			return false;
		return useTcpAcceleration;
	}

	public void setUseTcpAcceleration(Boolean useTcpAcceleration) {
		this.useTcpAcceleration = useTcpAcceleration;
	}
	
	public Boolean getUsePacketCompress() {
		if (usePacketCompress == null)
			return false;

		return usePacketCompress;
	}

	public void setUsePacketCompress(Boolean usePacketCompress) {
		if (usePacketCompress == null)
			this.usePacketCompress = false;
		else
			this.usePacketCompress = usePacketCompress;
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
		m.put("use_obfuscation_key", useObfuscationKey);
		m.put("obfuscation_key", obfuscationKey);
		m.put("use_remote_db", useRemoteDb);
		m.put("remote_kill_ip", remoteKillIp);
		m.put("remote_db_host_name", remoteDbHostName);
		m.put("remote_db_login_name", remoteDbLoginName);
		m.put("remote_db_password", remoteDbPassword);
		m.put("remote_db_name", remoteDbName);
		m.put("remote_db_table_name", remoteDbTableName);
		m.put("remote_db_port", remoteDbPort);
		m.put("remote_db_socket", remoteDbSocket);
		m.put("use_tcp_acceleration", getUseTcpAcceleration());
		m.put("use_packet_compress", getUsePacketCompress());
		m.put("proxy_port", proxyPort);
		m.put("created_at", dateFormat.format(createDateTime));
		m.put("updated_at", dateFormat.format(updateDateTime));
		
		return m;
	}
}
