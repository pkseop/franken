package kr.co.future.sslvpn.model;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import kr.co.future.api.CollectionTypeHint;
import kr.co.future.api.FieldOption;
import kr.co.future.api.PrimitiveConverter;
import kr.co.future.api.ReferenceKey;
import kr.co.future.confdb.CollectionName;
import kr.co.future.msgbus.Marshalable;
import kr.co.future.msgbus.Marshaler;
import kr.co.future.sslvpn.model.AccessGateway.DeviceAuthMode;

@CollectionName("access_profiles")
public class AccessProfile implements Marshalable {
	private String guid = UUID.randomUUID().toString();

	// for marlin compatibility
	private int id;

	@FieldOption(nullable = false, length = 60)
	private String name;

	@FieldOption(length = 250)
	private String description;

	@FieldOption(name = "allow_time_id")
	private String allowTimeId;

	@FieldOption(name = "verify_client_ip", nullable = false)
	private boolean verifyClientIp;

	@FieldOption(name = "use_nac", nullable = false)
	private boolean useNac;

	@FieldOption(name = "ip_from", length = 20)
	private String ipFrom;

	@FieldOption(name = "ip_to", length = 20)
	private String ipTo;

	// 임대대역 다중화를 위한 ipLeaseRange 리스트화
	@CollectionTypeHint(IpLeaseRange.class)
	private List<IpLeaseRange> ipLeaseRanges = new ArrayList<IpLeaseRange>();

	@FieldOption(name = "use_client_timeout", nullable = false)
	private boolean useClientTimeout;

	@FieldOption(skip = true)
	private Integer maxClientTimeout;

	// in seconds
	@FieldOption(name = "client_timeout", nullable = false)
	private int clientTimeout;

	@FieldOption(name = "use_fail_limit", nullable = false)
	private boolean useFailLimit;

	@FieldOption(name = "fail_limit", nullable = false)
	private int failLimitCount;

	// in days
	@FieldOption(name = "password_expiry", nullable = false)
	private int passwordExpiry = 365;

	@FieldOption(name = "created_at", nullable = false)
	private Date createDateTime;

	@FieldOption(name = "updated_at", nullable = false)
	private Date updateDateTime;

	@FieldOption(name = "login_method", nullable = true)
	private Integer loginMethod;

	@FieldOption(name = "encryptions", nullable = true)
	private List<String> encryptions;

	// 일 단위 반환
	@FieldOption(name = "password_change_alert", nullable = true)
	private int passwordChangeAlert;

	@FieldOption(name = "account_expiry_alert", nullable = true)
	private Long accountExpiryAlert = 0l;

	@CollectionTypeHint(Server.class)
	@ReferenceKey("guid")
	private List<Server> normalAcl = new ArrayList<Server>();

	@CollectionTypeHint(Server.class)
	@ReferenceKey("guid")
	private List<Server> quarantineAcl = new ArrayList<Server>();

	@CollectionTypeHint(ClientApp.class)
	@ReferenceKey("guid")
	private List<ClientApp> clientApps = new ArrayList<ClientApp>();

	@ReferenceKey("guid")
	@FieldOption(name = "client_check_profile", nullable = true)
	private ClientCheckProfile clientCheckProfile;

	@CollectionTypeHint(InternalNetworkRange.class)
	private List<InternalNetworkRange> internalNetworkRanges = new ArrayList<InternalNetworkRange>();

	// null일 경우 AccessGateway에서 상속
	@FieldOption(name = "device_auth_mode", nullable = true)
	private DeviceAuthMode deviceAuthMode;

	@CollectionTypeHint(ClientIpRange.class)
	private List<ClientIpRange> clientIpRanges = new ArrayList<ClientIpRange>();

	@FieldOption(name = "use_split_routing", nullable = true)
	private Boolean useSplitRouting;

	@CollectionTypeHint(SplitRoutingEntry.class)
	private List<SplitRoutingEntry> splitRoutingEntries = new ArrayList<SplitRoutingEntry>();

	// 사용자가 잠긴 후 일정시간이 지나면 풀리도록 설정
	@FieldOption(name = "user_unlock_time", nullable = true)
	private Integer userUnlockTime;

	@FieldOption(name = "popup_url", nullable = true)
	private String popupUrl;

	@CollectionTypeHint(LdapAttribute.class)
	@FieldOption(name = "ldap_attributes", nullable = true)
	private LdapAttribute ldapAttributes;

	@FieldOption(name = "use_auto_reconnect", nullable = true)
	private Boolean useAutoReconnect;

	@FieldOption(name = "use_client_auto_uninstall", nullable = true)
	private Boolean useClientAutoUninstall;
	
	@FieldOption(name = "use_proxy", nullable = true)
	private Boolean useProxy;
	
	// expire date of account
	@FieldOption(name = "expire_valid_date", nullable = true)
	private Date expireValidDate;
	
	@FieldOption(name = "start_valid_date", nullable = true)
	private Date startValidDate;
	
	@FieldOption(name = "access_dir", nullable = true)
	private String accessDir;
	
	@FieldOption(name = "access_dir_type", nullable = true)
	private String accessDirType;
	
	@FieldOption(name = "use_ios", nullable = true)
	private Boolean useIOS;

	public Date getExpireValidDate() {
		return expireValidDate;
	}
	
	public void setExpireValidDate(Date expireValidDate) {
		this.expireValidDate = expireValidDate;
	}
	
	public Date getStartValidDate() {
		return startValidDate;
	}
	
	public void setStartValidDate(Date startValidDate) {
		this.startValidDate = startValidDate;
	}
	
	public Boolean getUseProxy() {
		return useProxy;
	}

	public void setUseProxy(Boolean useProxy) {
		this.useProxy = useProxy;
	}

	public Boolean getUseClientAutoUninstall() {
		return useClientAutoUninstall;
	}

	public void setUseClientAutoUninstall(Boolean useClientAutoUninstall) {
		this.useClientAutoUninstall = useClientAutoUninstall;
	}

	public Boolean getUseAutoReconnect() {
		return useAutoReconnect;
	}

	public void setUseAutoReconnect(Boolean useAutoReconnect) {
		this.useAutoReconnect = useAutoReconnect;
	}

	public LdapAttribute getLdapAttributes() {
		return ldapAttributes;
	}

	public void setLdapAttributes(LdapAttribute ldapAttributes) {
		this.ldapAttributes = ldapAttributes;
	}

	public String getPopupUrl() {
		return popupUrl;
	}

	public void setPopupUrl(String popupUrl) {
		this.popupUrl = popupUrl;
	}

	public Integer getUserUnlockTime() {
		return userUnlockTime;
	}

	public void setUserUnlockTime(Integer userUnlockTime) {
		this.userUnlockTime = userUnlockTime;
	}

	public Boolean getUseSplitRouting() {
		return useSplitRouting;
	}

	public Boolean isUseSplitRouting() {
		return useSplitRouting;
	}

	public void setUseSplitRouting(Boolean useSplitRouting) {
		this.useSplitRouting = useSplitRouting;
	}

	public List<SplitRoutingEntry> getSplitRoutingEntries() {
		return splitRoutingEntries;
	}

	public void setSplitRoutingEntries(List<SplitRoutingEntry> splitRoutingEntries) {
		this.splitRoutingEntries = splitRoutingEntries;
	}

	public List<ClientIpRange> getClientIpRanges() {
		return clientIpRanges;
	}

	public void setClientIpRanges(List<ClientIpRange> clientIpRanges) {
		this.clientIpRanges = clientIpRanges;
	}

	public DeviceAuthMode getDeviceAuthMode() {
		return deviceAuthMode;
	}

	public void setDeviceAuthMode(DeviceAuthMode deviceAuthMode) {
		this.deviceAuthMode = deviceAuthMode;
	}

	public void setIpFrom(String ipFrom) {
		this.ipFrom = ipFrom;
	}

	public void setIpTo(String ipTo) {
		this.ipTo = ipTo;
	}

	public String getGuid() {
		return guid;
	}

	public void setGuid(String guid) {
		this.guid = guid;
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getAllowTimeId() {
		return allowTimeId;
	}

	public void setAllowTimeId(String allowTimeId) {
		this.allowTimeId = allowTimeId;
	}

	public boolean isVerifyClientIp() {
		return verifyClientIp;
	}

	public void setVerifyClientIp(boolean verifyClientIp) {
		this.verifyClientIp = verifyClientIp;
	}

	public boolean isUseNac() {
		return useNac;
	}

	public void setUseNac(boolean useNac) {
		this.useNac = useNac;
	}

	public String getIpFrom() {
		return ipFrom;
	}

	public String getIpTo() {
		return ipTo;
	}

	public List<IpLeaseRange> getIpLeaseRanges() {
		ArrayList<IpLeaseRange> ranges = new ArrayList<IpLeaseRange>(ipLeaseRanges);
		// migration ip lease ranges
		if (ipFrom != null && ipTo != null) {
			IpLeaseRange i = new IpLeaseRange();
			i.setIpFrom(ipFrom);
			i.setIpTo(ipTo);
			ranges.add(i);
		}
		return ranges;
	}

	public void setIpLeaseRanges(List<IpLeaseRange> ipLeaseRange) {
		this.ipFrom = null;
		this.ipTo = null;
		this.ipLeaseRanges = ipLeaseRange;
	}

	public boolean isUseClientTimeout() {
		return useClientTimeout;
	}

	public void setUseClientTimeout(boolean useClientTimeout) {
		this.useClientTimeout = useClientTimeout;
	}

	public Integer getMaxClientTimeout() {
		return maxClientTimeout;
	}

	public void setMaxClientTimeout(Integer maxClientTimeout) {
		this.maxClientTimeout = maxClientTimeout;
	}

	public int getClientTimeout() {
		return clientTimeout;
	}

	public void setClientTimeout(int clientTimeout) {
		this.clientTimeout = clientTimeout;
	}

	public boolean isUseFailLimit() {
		return useFailLimit;
	}

	public void setUseFailLimit(boolean useFailLimit) {
		this.useFailLimit = useFailLimit;
	}

	public int getFailLimitCount() {
		return failLimitCount;
	}

	public void setFailLimitCount(int failLimitCount) {
		this.failLimitCount = failLimitCount;
	}

	public int getPasswordExpiry() {
		return passwordExpiry;
	}

	public void setPasswordExpiry(int passwordExpiry) {
		this.passwordExpiry = passwordExpiry;
	}

	public Date getCreateDateTime() {
		return createDateTime;
	}

	public void setCreateDateTime(Date createDateTime) {
		this.createDateTime = createDateTime;
	}

	public Date getUpdateDateTime() {
		return updateDateTime;
	}

	public Integer getLoginMethod() {
		return loginMethod;
	}

	public void setLoginMethod(Integer loginMethod) {
		this.loginMethod = loginMethod;
	}

	public List<String> getEncryptions() {
		return encryptions;
	}

	public void setEncryptions(List<String> encryptions) {
		this.encryptions = encryptions;
	}

	public void setUpdateDateTime(Date updateDateTime) {
		this.updateDateTime = updateDateTime;
	}

	public List<Server> getNormalAcl() {
		return normalAcl;
	}

	public void setNormalAcl(List<Server> normalAcl) {
		this.normalAcl = normalAcl;
	}

	public List<Server> getQuarantineAcl() {
		return quarantineAcl;
	}

	public void setQuarantineAcl(List<Server> quarantineAcl) {
		this.quarantineAcl = quarantineAcl;
	}

	public List<ClientApp> getClientApps() {
		return clientApps;
	}

	public void setClientApps(List<ClientApp> clientApps) {
		this.clientApps = clientApps;
	}

	public List<InternalNetworkRange> getInternalNetworkRanges() {
		return internalNetworkRanges;
	}

	public void setInternalNetworkRanges(List<InternalNetworkRange> internalNetworkRanges) {
		this.internalNetworkRanges = internalNetworkRanges;
	}

	public ClientCheckProfile getClientCheckProfile() {
		return clientCheckProfile;
	}

	public void setClientCheckProfile(ClientCheckProfile clientCheckProfile) {
		this.clientCheckProfile = clientCheckProfile;
	}

	public int getPasswordChangeAlert() {
		return passwordChangeAlert;
	}

	public void setPasswordChangeAlert(int passwordChangeAlert) {
		this.passwordChangeAlert = passwordChangeAlert;
	}

	public Long getAccountExpiryAlert() {
		if (this.accountExpiryAlert == null)
			this.accountExpiryAlert = 0l;
		return accountExpiryAlert;
	}

	public void setAccountExpiryAlert(Long accountExpiryAlert) {
		if (accountExpiryAlert == null)
			accountExpiryAlert = 0l;
		this.accountExpiryAlert = accountExpiryAlert;
	}
	
	public String getAccessDir() {
		return accessDir;
	}

	public void setAccessDir(String accessDir) {
		this.accessDir = accessDir;
	}

	public String getAccessDirType() {
		return accessDirType;
	}

	public void setAccessDirType(String accessDirType) {
		this.accessDirType = accessDirType;
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
		m.put("id", id);
		m.put("guid", guid);
		m.put("name", name);
		m.put("description", description);
		m.put("allow_time_id", allowTimeId);
		m.put("verify_client_ip", verifyClientIp);
		m.put("use_nac", useNac);
		m.put("ip_from", ipFrom);
		m.put("ip_to", ipTo);
		m.put("ip_lease_ranges", ipLeaseRanges != null ? Marshaler.marshal(getIpLeaseRanges()) : null);
		m.put("use_client_timeout", useClientTimeout);
		m.put("max_client_timeout", maxClientTimeout);
		m.put("client_timeout", clientTimeout);
		m.put("use_fail_limit", useFailLimit);
		m.put("fail_limit", failLimitCount);
		m.put("password_expiry", passwordExpiry);
		if (clientCheckProfile == null)
			m.put("client_check_profile", null);
		m.put("created_at", dateFormat.format(createDateTime));
		m.put("updated_at", dateFormat.format(updateDateTime));
		m.put("login_method", loginMethod);
		m.put("encryptions", encryptions);
		m.put("password_change_alert", passwordChangeAlert);
		m.put("account_expiry_alert", getAccountExpiryAlert());
		m.put("nac_name", clientCheckProfile == null ? null : clientCheckProfile.getName());
		m.put("device_auth_mode", deviceAuthMode);
		m.put("client_ip_ranges", PrimitiveConverter.serialize(clientIpRanges));
		m.put("use_split_routing", useSplitRouting);
		m.put("split_routing_entries", Marshaler.marshal(splitRoutingEntries));
		m.put("user_unlock_time", userUnlockTime);
		m.put("popup_url", popupUrl);
		m.put("ldap_attributes", ldapAttributes);
		m.put("use_auto_reconnect", useAutoReconnect);
		m.put("use_client_auto_uninstall", useClientAutoUninstall);
		m.put("use_proxy", useProxy);
		m.put("expire_valid_date", expireValidDate != null ? dateFormat.format(expireValidDate) : null);
		m.put("start_valid_date", startValidDate != null ? dateFormat.format(startValidDate) : null);
		m.put("access_dir", accessDir);
		m.put("access_dir_type", accessDirType);
		m.put("use_ios", useIOS);
		
		return m;
	}

	public String checkDuplicate() {
		String enc = null;
		if (encryptions != null && encryptions != null && !encryptions.isEmpty())
			enc = encryptions.get(0);
		String ccpGuid = clientCheckProfile == null ? null : clientCheckProfile.getGuid();
		return "AccessProfile [loginMethod=" + loginMethod + ", encryptions=" + enc + ", clientCheckProfile=" + ccpGuid
				+ ", deviceAuthMode=" + deviceAuthMode + "]";
	}

}
