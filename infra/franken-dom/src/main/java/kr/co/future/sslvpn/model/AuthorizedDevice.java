package kr.co.future.sslvpn.model;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import kr.co.future.api.FieldOption;
import kr.co.future.confdb.CollectionName;
import kr.co.future.msgbus.Marshalable;

@CollectionName("auth_devices")
public class AuthorizedDevice implements Marshalable {
	private String guid = UUID.randomUUID().toString();

	// volume serial hash for windows, imei hash for android
	@FieldOption(name = "device_key", length = 64, nullable = false)
	private String deviceKey;

	// 1 for Windows, 2 for Linux, 3 for Android, 4 for MacOS, 5 for iOS
	@FieldOption(name = "type", nullable = false)
	private int type;

	// host name
	@FieldOption(name = "host_name", length = 60)
	private String hostName;

	// operating system or anything else
	@FieldOption(name = "description", length = 250)
	private String description;

	// owner name
	@FieldOption(name = "owner", length = 20, nullable = false)
	private String owner;

	@FieldOption(name = "login_name", length = 60)
	private String loginName;
	
	// remote ip at registration phase
	@FieldOption(name = "remote_ip", length = 20)
	private String remoteIp;

	// admin can block authorized device (stolen or other reasons)
	@FieldOption(name = "is_blocked", nullable = false)
	private boolean blocked;

	@FieldOption(name = "expiration", nullable = true)
	private Date expiration;

	@FieldOption(name = "hdd_serial", nullable = true)
	private String hddSerial;

	@FieldOption(name = "mac_address", nullable = true)
	private String macAddress;

	@FieldOption(name = "remote_client_ip", nullable = true)
	private String remoteClientip;

	@FieldOption(name = "is_authorized", nullable = true)
	private Boolean isAuthorized = true;
	
	@FieldOption(name = "org_unit_name", length = 60, nullable = true)
	private String orgUnitName;

	public String getGuid() {
		return guid;
	}

	public void setGuid(String guid) {
		this.guid = guid;
	}

	public String getDeviceKey() {
		return deviceKey;
	}

	public void setDeviceKey(String deviceKey) {
		this.deviceKey = deviceKey;
	}

	public int getType() {
		return type;
	}

	public void setType(int type) {
		this.type = type;
	}

	public String getHostName() {
		return hostName;
	}

	public void setHostName(String hostName) {
		this.hostName = hostName;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getOwner() {
		return owner;
	}

	public void setOwner(String owner) {
		this.owner = owner;
	}

	public String getLoginName() {
		return loginName;
	}

	public void setLoginName(String loginName) {
		this.loginName = loginName;
	}

	public String getRemoteIp() {
		return remoteIp;
	}

	public void setRemoteIp(String remoteIp) {
		this.remoteIp = remoteIp;
	}

	public boolean isBlocked() {
		return blocked;
	}

	public void setBlocked(boolean blocked) {
		this.blocked = blocked;
	}

	public void setExpiration(Date expiration) {
		this.expiration = expiration;
	}

	public Date getExpiration() {
		return expiration;
	}

	public String getHddSerial() {
		return hddSerial;
	}

	public void setHddSerial(String hddSerial) {
		this.hddSerial = hddSerial;
	}

	public String getMacAddress() {
		return macAddress;
	}

	public void setMacAddress(String macAddress) {
		this.macAddress = macAddress;
	}

	public String getRemoteClientip() {
		return remoteClientip;
	}

	public void setRemoteClientip(String remoteClientip) {
		this.remoteClientip = remoteClientip;
	}

	public Boolean getIsAuthorized() {
		return isAuthorized;
	}

	public void setIsAuthorized(Boolean isAuthorized) {
		this.isAuthorized = isAuthorized;
	}
	
	public String getOrgUnitName() {
		return orgUnitName;
	}
	
	public void setOrgUnitName(String orgUnitName) {
		this.orgUnitName = orgUnitName;
	}

	@Override
	public Map<String, Object> marshal() {
		Map<String, Object> m = new HashMap<String, Object>();
		m.put("guid", guid);
		m.put("device_hash", deviceKey);
		m.put("type", type);
		m.put("host_name", hostName);
		m.put("description", description);
		m.put("owner", owner);
		m.put("remote_ip", remoteIp);
		m.put("is_blocked", blocked);
		m.put("expiration", expiration);
		m.put("login_name", loginName);
		m.put("hdd_serial", hddSerial);
		m.put("mac_address", macAddress);
		m.put("remote_client_ip", remoteClientip);
		m.put("is_authorized", isAuthorized);
		m.put("org_unit_name", orgUnitName);
		return m;
	}
}
