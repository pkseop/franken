package kr.co.future.sslvpn.model;

import java.net.InetAddress;
import java.net.UnknownHostException;
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
import kr.co.future.msgbus.Marshaler;

@CollectionName("access_gateways")
public class AccessGateway implements Marshalable {
	// 공인인증서 본인확인 방법 (주민등록번호 혹은 DN 매칭)
	public enum IdentificationMode {
		Idn, SubjectDn
	}

	public enum DeviceAuthMode {
		// AccessGateway에서는 단말인증을 사용하지만 특정 Accessprofile에서는 사용하지 않을 때 NONE을 사용
		OneToMany, OneToOne, None;
	}

	public enum LdapMode {
		NONE, LDAP_SYNC, LDAP_SYNC_EXTEND
	}

	// "tcp" or "udp" for ssl tunneling
	@FieldOption(length = 4)
	private String protocol;

	@FieldOption(nullable = false)
	private boolean useDeviceAuth;

	@FieldOption(nullable = false)
	private boolean useRadiusAuth;

	@FieldOption(nullable = true)
	private String radiusOrgUnit;

	@FieldOption(nullable = false)
	private boolean useLdapAuth;

	@FieldOption(name = "ldap_mode", nullable = true)
	private LdapMode ldapMode = null;

	// 1 (pw), 2 (cert), 3 (pw+cert), 4(npki), 5(pw+npki)
	@FieldOption(name = "login_method")
	private int loginMethod;

	@FieldOption(name = "use_split", nullable = false)
	private boolean useSplitRouting;

//	@FieldOption(name = "dns_postfix", length = 60)
//	private String dnsPostfix;

	@FieldOption(name = "created_at", nullable = false)
	private Date createDateTime;

	@FieldOption(name = "updated_at", nullable = false)
	private Date updateDateTime;

	@FieldOption(name = "notice", nullable = true)
	private String notice;

	@FieldOption(name = "lnk_name", nullable = true)
	private String lnkName;

	@CollectionTypeHint(SplitRoutingEntry.class)
	private List<SplitRoutingEntry> splitRoutingEntries = new ArrayList<SplitRoutingEntry>();

	@FieldOption(name = "use_sql_auth", nullable = false)
	private boolean useSqlAuth;

	@FieldOption(name = "user_sql", nullable = true)
	private String userSql;

	@FieldOption(name = "password_expiry_sql", nullable = true)
	private String passwordExpirySql;

	@FieldOption(name = "auth_sql", nullable = true)
	private String authSql;

	@FieldOption(name = "idn_sql", nullable = true)
	private String idnSql;

	@FieldOption(name = "subject_dn_sql", nullable = true)
	private String subjectDnSql;

	@FieldOption(name = "db_account", nullable = true)
	private String dbAccount;

	@FieldOption(name = "db_password", nullable = true)
	private String dbPassword;

	@FieldOption(name = "db_connstring", nullable = true)
	private String dbConnectionString;

	@FieldOption(name = "password_hash_type", nullable = true)
	private String passwordHashType;

	// 1 (front), 2 (rear)
	@FieldOption(name = "salt_location", nullable = true)
	private Integer saltLocation;

	@FieldOption(name = "salt_sql", nullable = true)
	private String saltSql;

	@FieldOption(name = "device_auth_mode", nullable = true)
	private DeviceAuthMode deviceAuthMode = DeviceAuthMode.OneToMany;

	@CollectionTypeHint(String.class)
	@FieldOption(name = "device_key_type", nullable = true)
	private List<String> deviceKeyType;

	@FieldOption(name = "password_reset_msg", nullable = true)
	private String passwordResetMessage;

	@FieldOption(name = "id_label", nullable = true)
	private String idLabel;

	@FieldOption(name = "identification_mode", nullable = true)
	private IdentificationMode identificationMode = IdentificationMode.Idn;

	@FieldOption(name = "top_image", nullable = true)
	private String topImage;

	@FieldOption(name = "notice_image", nullable = true)
	private String noticeImage;

	@FieldOption(name = "cert_dialog_image", nullable = true)
	private String certDialogImage; // base64 encoded

	@FieldOption(name = "subject_dn_hash_type", nullable = true)
	private String subjectDnHashType; // for sql auth only

	@FieldOption(name = "password_encoding", nullable = true)
	private String passwordEncoding;// hex or base64

	@FieldOption(name = "auto_user_lock_date", nullable = true)
	private Integer autoUSerLockDate;

	// 기초과학연구원 subject_dn 인코딩용.
	@FieldOption(name = "subject_dn_charset", nullable = true)
	private String subjectDnCharset = "utf-8";

	@FieldOption(name = "subject_dn_encoding", nullable = true)
	private String subjectDnEncoding = "hex";

	@FieldOption(name = "page_title", nullable = true)
	private String pageTitle;

	@FieldOption(name = "admin_servlet_name", nullable = true)
	private String adminServletName;

	@CollectionTypeHint(ExternalVpn.class)
	@FieldOption(name = "external_vpn_", nullable = true)
	private ExternalVpn externalVpn;

	@FieldOption(name = "device_expire_msg", nullable = true)
	private String DeviceExpireMsg;

	@FieldOption(name = "use_auto_reconnect", nullable = true)
	private Boolean useAutoReconnect;

	@FieldOption(name = "use_client_auto_uninstall", nullable = true)
	private Boolean useClientAutoUninstall;

	@FieldOption(name = "use_web_proxy", nullable = true)
	private Boolean useWebProxy = false;

	@FieldOption(name = "web_proxy_cache_size", nullable = true)
	private Integer webProxyCacheSize = 100;

	@FieldOption(name = "web_proxy_port", nullable = true)
	private Integer webProxyPort = 8080;

	@FieldOption(name = "disaster_recovery_list", nullable = true)
	private List<String> disasterRecoveryList;

	@FieldOption(name = "use_client_type", nullable = true)
	private List<String> useClientType;

	@FieldOption(name = "use_integrated_management", nullable = true)
	private Boolean useIntegratedManagement;

	@FieldOption(name = "parent_node", nullable = true)
	private String parentNode;

	@FieldOption(name = "use_password_reset", nullable = true)
	private Boolean usePasswordReset = true;

	@FieldOption(name = "use_auth_center", nullable = true)
	private Boolean useAuthCenter = true;

	@FieldOption(name = "client_comment", nullable = true)
	private String clientComment;
	
	public Boolean getUsePasswordReset() {
		if (usePasswordReset == null)
			return true;
		return usePasswordReset;
	}

	public void setUsePasswordReset(Boolean usePasswordReset) {
		if (usePasswordReset == null)
			this.usePasswordReset = false;
		this.usePasswordReset = usePasswordReset;
	}

	public Boolean getUseAuthCenter() {
		if (useAuthCenter == null)
			return true;
		return useAuthCenter;
	}

	public void setUseAuthCenter(Boolean useAuthCenter) {
		if (useAuthCenter == null)
			this.useAuthCenter = false;
		this.useAuthCenter = useAuthCenter;
	}

	public String getClientComment() {
		return clientComment;
	}

	public void setClientComment(String clientComment) {
		this.clientComment = clientComment;
	}

	public Boolean getUseIntegratedManagement() {
		if (useIntegratedManagement == null)
			return false;
		return useIntegratedManagement;
	}

	public void setUseIntegratedManagement(Boolean useIntegratedManagement) {
		if (useIntegratedManagement == null)
			this.useIntegratedManagement = false;
		this.useIntegratedManagement = useIntegratedManagement;
	}

	public String getParentNode() {
		return parentNode;
	}

	public void setParentNode(String parentNode) {
		if (parentNode != null) {
			try {
				InetAddress.getByName(parentNode);
			} catch (UnknownHostException e) {
				throw new IllegalArgumentException("invalid parent node address");
			}
		}
		this.parentNode = parentNode;
	}

	public List<String> getDisasterRecoveryList() {
		return disasterRecoveryList;
	}

	public void setDisasterRecoveryList(List<String> disasterRecoveryList) {
		this.disasterRecoveryList = disasterRecoveryList;
	}

	public List<String> getUseClientType() {
		if (useClientType == null) {
			List<String> l = new ArrayList<String>();
			l.add("active-x");
			l.add("pc-client");
			return l;
		}
		return useClientType;
	}

	public void setUseClientType(List<String> useClientType) {
		this.useClientType = useClientType;
	}

	public boolean getUseWebProxy() {
		if (useWebProxy == null)
			return false;
		return useWebProxy;
	}

	public void setUseWebProxy(Boolean useWebProxy) {
		this.useWebProxy = useWebProxy;
	}

	public int getWebProxyCacheSize() {
		if (useWebProxy == null)
			return 100;
		return webProxyCacheSize;
	}

	public void setWebProxyCacheSize(Integer webProxyCacheSize) {
		this.webProxyCacheSize = webProxyCacheSize;
	}

	public int getWebProxyPort() {
		if (webProxyPort == null)
			return 8080;
		return webProxyPort;
	}

	public void setWebProxyPort(Integer webProxyPort) {
		this.webProxyPort = webProxyPort;
	}

	public Boolean getUseClientAutoUninstall() {
		if (useClientAutoUninstall == null)
			return false;
		return useClientAutoUninstall;
	}

	public void setUseClientAutoUninstall(Boolean useClientAutoUninstall) {
		if (useClientAutoUninstall == null)
			useClientAutoUninstall = false;
		this.useClientAutoUninstall = useClientAutoUninstall;
	}

	public Boolean getUseAutoReconnect() {
		if (this.useAutoReconnect == null)
			return false;
		return useAutoReconnect;
	}

	public void setUseAutoReconnect(Boolean useAutoReconnect) {
		if (useAutoReconnect == null)
			useAutoReconnect = false;
		this.useAutoReconnect = useAutoReconnect;
	}

	public LdapMode getLdapMode() {
		if (ldapMode == null) {
			if (useLdapAuth)
				ldapMode = LdapMode.LDAP_SYNC;
			else
				ldapMode = LdapMode.NONE;
			this.useLdapAuth = false;
		}

		return ldapMode;
	}

	public void setLdapMode(LdapMode ldapMode) {
		this.ldapMode = ldapMode;
	}

	public String getDeviceExpireMsg() {
		return DeviceExpireMsg;
	}

	public void setDeviceExpireMsg(String deviceExpireMsg) {
		DeviceExpireMsg = deviceExpireMsg;
	}

	public ExternalVpn getExternalVpn() {
		return externalVpn;
	}

	public void setExternalVpn(ExternalVpn externalVpn) {
		this.externalVpn = externalVpn;
	}

	public String getPageTitle() {
		return pageTitle;
	}

	public void setPageTitle(String pageTitle) {
		this.pageTitle = pageTitle;
	}

	public String getSubjectDnCharset() {
		return subjectDnCharset == null ? "utf-8" : subjectDnCharset;
	}

	public void setSubjectDnCharset(String subjectDnCharset) {
		if (subjectDnCharset == null)
			this.subjectDnCharset = "utf-8";
		else
			this.subjectDnCharset = subjectDnCharset;
	}

	public String getSubjectDnEncoding() {
		return subjectDnEncoding == null ? "hex" : subjectDnEncoding;
	}

	public void setSubjectDnEncoding(String subjectDnEncoding) {
		this.subjectDnEncoding = subjectDnEncoding;
	}

	public String getPasswordEncoding() {
		return passwordEncoding == null ? "hex" : passwordEncoding;
	}

	public void setPasswordEncoding(String passwordEncoding) {
		if (passwordEncoding == null)
			this.passwordEncoding = "hex";
		else
			this.passwordEncoding = passwordEncoding;
	}

	public String getPasswordExpirySql() {
		return passwordExpirySql;
	}

	public void setPasswordExpirySql(String passwordExpirySql) {
		this.passwordExpirySql = passwordExpirySql;
	}

	public String getIdLabel() {
		return idLabel;
	}

	public void setIdLabel(String idLabel) {
		this.idLabel = idLabel;
	}

	public String getPasswordResetMessage() {
		return passwordResetMessage;
	}

	public void setPasswordResetMessage(String passwordResetMessage) {
		this.passwordResetMessage = passwordResetMessage;
	}

	public String getDbAccount() {
		return dbAccount;
	}

	public void setDbAccount(String dbAccount) {
		this.dbAccount = dbAccount;
	}

	public String getDbPassword() {
		return dbPassword;
	}

	public void setDbPassword(String dbPassword) {
		this.dbPassword = dbPassword;
	}

	public String getDbConnectionString() {
		return dbConnectionString;
	}

	public void setDbConnectionString(String dbConnectionString) {
		this.dbConnectionString = dbConnectionString;
	}

	public String getUserSql() {
		return userSql;
	}

	public void setUserSql(String userSql) {
		this.userSql = userSql;
	}

	public String getAuthSql() {
		return authSql;
	}

	public void setAuthSql(String authSql) {
		this.authSql = authSql;
	}

	public String getIdnSql() {
		return idnSql;
	}

	public void setIdnSql(String idnSql) {
		this.idnSql = idnSql;
	}

	public String getSubjectDnSql() {
		return subjectDnSql;
	}

	public void setSubjectDnSql(String subjectDnSql) {
		this.subjectDnSql = subjectDnSql;
	}

	public boolean isUseSqlAuth() {
		return useSqlAuth;
	}

	public void setUseSqlAuth(boolean useSqlAuth) {
		this.useSqlAuth = useSqlAuth;
	}

	public String getProtocol() {
		return protocol;
	}

	public void setProtocol(String protocol) {
		this.protocol = protocol;
	}

	public String getNotice() {
		return notice;
	}

	public void setNotice(String notice) {
		this.notice = notice;
	}

	public boolean isUseDeviceAuth() {
		return useDeviceAuth;
	}

	public void setUseDeviceAuth(boolean useDeviceAuth) {
		this.useDeviceAuth = useDeviceAuth;
	}

	public boolean isUseRadiusAuth() {
		return useRadiusAuth;
	}

	public void setUseRadiusAuth(boolean useRadiusAuth) {
		this.useRadiusAuth = useRadiusAuth;
	}

	public String getRadiusOrgUnitId() {
		return radiusOrgUnit;
	}

	public void setRadiusOrgUnitId(String radiusOrgUnit) {
		this.radiusOrgUnit = radiusOrgUnit;
	}

	public boolean isUseLdapAuth() {
		return useLdapAuth;
	}

	public void setUseLdapAuth(boolean useLdapAuth) {
		this.useLdapAuth = useLdapAuth;
	}

	public int getLoginMethod() {
		return loginMethod;
	}

	public void setLoginMethod(Integer loginMethod) {
		this.loginMethod = loginMethod;
	}

	public boolean isUseSplitRouting() {
		return useSplitRouting;
	}

	public void setUseSplitRouting(boolean useSplitRouting) {
		this.useSplitRouting = useSplitRouting;
	}

//	public String getDnsPostfix() {
//		return dnsPostfix;
//	}
//
//	public void setDnsPostfix(String dnsPostfix) {
//		this.dnsPostfix = dnsPostfix;
//	}

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

	public String getLnkName() {
		return lnkName;
	}

	public void setLnkName(String lnkName) {
		this.lnkName = lnkName;
	}

	public List<SplitRoutingEntry> getSplitRoutingEntries() {
		return splitRoutingEntries;
	}

	public void setSplitRoutingEntries(List<SplitRoutingEntry> splitRoutingEntries) {
		this.splitRoutingEntries = splitRoutingEntries;
	}

	public String getPasswordHashType() {
		return passwordHashType;
	}

	public void setPasswordHashType(String passwordHashType) {
		this.passwordHashType = passwordHashType;
	}

	public Integer getSaltLocation() {
		return saltLocation;
	}

	public void setSaltLocation(Integer saltLocation) {
		this.saltLocation = saltLocation;
	}

	public String getSaltSql() {
		return saltSql;
	}

	public void setSaltSql(String saltSql) {
		this.saltSql = saltSql;
	}

	public DeviceAuthMode getDeviceAuthMode() {
		if (deviceAuthMode == null)
			return DeviceAuthMode.OneToMany;
		return deviceAuthMode;
	}

	public void setDeviceAuthMode(DeviceAuthMode deviceAuthMode) {
		this.deviceAuthMode = deviceAuthMode;
	}

	public IdentificationMode getIdentificationMode() {
		if (identificationMode == null)
			return IdentificationMode.Idn;
		return identificationMode;
	}

	public void setIdentificationMode(IdentificationMode identificationMode) {
		this.identificationMode = identificationMode;
	}

	public List<String> getDeviceKeyType() {
		if (deviceKeyType == null) {
			List<String> l = new ArrayList<String>();
			l.add("HDD");
			return l;
		}
		return deviceKeyType;
	}

	public void setDeviceKeyType(List<String> deviceKeyType) {
		this.deviceKeyType = deviceKeyType;
	}

	public String getTopImage() {
		return topImage;
	}

	public void setTopImage(String topImage) {
		this.topImage = topImage;
	}

	public String getNoticeImage() {
		return noticeImage;
	}

	public void setNoticeImage(String noticeImage) {
		this.noticeImage = noticeImage;
	}

	public String getCertDialogImage() {
		return certDialogImage;
	}

	public void setCertDialogImage(String certDialogImage) {
		this.certDialogImage = certDialogImage;
	}

	public String getSubjectDnHashType() {
		return subjectDnHashType;
	}

	public void setSubjectDnHashType(String subjectDnHashType) {
		this.subjectDnHashType = subjectDnHashType;
	}

	public Integer getAutoUSerLockDate() {
		return autoUSerLockDate;
	}

	public void setAutoUSerLockDate(Integer autoUSerLockDate) {
		this.autoUSerLockDate = autoUSerLockDate;
	}

	public String getAdminConsolePath() {
		if (adminServletName == null)
			return "/admin";
		return adminServletName;
	}

	public void setAdminServletName(String adminServletName) {
		this.adminServletName = adminServletName;
	}

	@Override
	public Map<String, Object> marshal() {
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ssZ");
		Map<String, Object> m = new HashMap<String, Object>();
		m.put("protocol", protocol);
		m.put("login_method", loginMethod);
		m.put("use_device_auth", useDeviceAuth);
		m.put("notice", notice);
		m.put("radius_use", useRadiusAuth);
		m.put("radius_org_unit_id", radiusOrgUnit);
		m.put("ldap_use", useLdapAuth);
		m.put("use_split_routing", useSplitRouting);
		m.put("split_routing_entries", Marshaler.marshal(splitRoutingEntries));
//		m.put("dns_postfix", dnsPostfix);
		m.put("lnk_name", lnkName);
		m.put("use_sql_auth", useSqlAuth);
		m.put("user_sql", userSql);
		m.put("auth_sql", authSql);
		m.put("idn_sql", idnSql);
		m.put("subject_dn_sql", subjectDnSql);
		m.put("db_account", dbAccount);
		m.put("db_password", dbPassword);
		m.put("db_connstring", dbConnectionString);
		m.put("created_at", dateFormat.format(createDateTime));
		m.put("updated_at", dateFormat.format(updateDateTime));
		m.put("password_hash_type", passwordHashType);
		m.put("salt_location", saltLocation);
		m.put("salt_sql", saltSql);
		m.put("device_auth_mode", deviceAuthMode);
		m.put("device_key_type", deviceKeyType);
		m.put("password_reset_msg", passwordResetMessage);
		m.put("id_label", idLabel);
		m.put("identification_mode", getIdentificationMode().toString());
		m.put("top_image", topImage);
		m.put("notice_image", noticeImage);
		m.put("cert_dialog_image", certDialogImage);
		m.put("subject_dn_hash_type", subjectDnHashType);
		m.put("password_expiry_sql", passwordExpirySql);
		m.put("password_encoding", getPasswordEncoding());
		m.put("subject_dn_charset", subjectDnCharset);
		m.put("subject_dn_encoding", subjectDnEncoding);
		m.put("auto_user_lock_date", autoUSerLockDate);
		m.put("page_title", pageTitle);
		m.put("admin_servlet_name", adminServletName);
		m.put("external_vpn", externalVpn == null ? null : externalVpn.marshal());
		m.put("device_expire_msg", DeviceExpireMsg);
		m.put("ldap_mode", getLdapMode());
		m.put("use_auto_reconnect", getUseAutoReconnect());
		m.put("use_client_auto_uninstall", getUseClientAutoUninstall());
		m.put("use_web_proxy", getUseWebProxy());
		m.put("web_proxy_cache_size", getWebProxyCacheSize());
		m.put("web_proxy_port", getWebProxyPort());
		m.put("disaster_recovery_list", getDisasterRecoveryList());
		m.put("use_client_type", getUseClientType());
		m.put("use_integrated_management", getUseIntegratedManagement());
		m.put("parent_node", getParentNode());
		m.put("use_password_reset", getUsePasswordReset());
		m.put("use_auth_center", getUseAuthCenter());
		m.put("client_comment", getClientComment());

		return m;
	}
}
