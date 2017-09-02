package kr.co.future.sslvpn.model;

import java.util.ArrayList;
import java.util.List;

public class SSLConfig {
	private String protocol;
	private int loginMethod;
	private boolean useDeviceAuth;
	private boolean useRadius;
	private String radiusOrgUnitId;
	private boolean useLdap;
	private boolean useSplitRouting;
	private List<SplitRoutingEntry> splitRoutingEntries = new ArrayList<SplitRoutingEntry>();
//	private String dnsPostfix;
	private String notice;
	private String lnkName;
	private boolean useSqlAuth;
	private String userSql;
	private String passwordExpirySql;
	private String authSql;
	private String idnSql;
	private String subjectDnSql;
	private String dbAccount;
	private String dbPassword;
	private String dbConnectionString;
	private String passwordHashType;
	private Integer saltLocation;
	private String saltSql;
	private String deviceAuthMode;
	private List<String> deviceKeyType;
	private String passwordResetMessage;
	private String idLabel;
	private String identificationMode;
	private String topImage;
	private String noticeImage;
	private String certDialogImage;
	private String subjectDnHashType;
	private String passwordEncoding;
	private String subjectDnCharset;
	private String subjectDnEncoding;
	private Integer autoUserLockDate;
	private String pageTitle;
	private String adminServletName;
	private ExternalVpn extVpn;
	private String deviceExpireMsg;
	private String ldapMode;
	private Boolean useAutoReconnect;
	private Boolean useClientAutoUninstall;
	private Boolean useWebProxy;
	private Integer webProxyCacheSize;
	private Integer webProxyPort;
	private List<String> disasterRecoveryList;
	private List<String> useClientType;
	private Boolean useIntegratedManagement;
	private String parentNode;
	private Boolean usePasswordReset;
	private Boolean useAuthCenter;
	private String clientComment;

	public Boolean getUsePasswordReset() {
		return usePasswordReset;
	}

	public void setUsePasswordReset(Boolean usePasswordReset) {
		this.usePasswordReset = usePasswordReset;
	}

	public Boolean getUseAuthCenter() {
		return useAuthCenter;
	}

	public void setUseAuthCenter(Boolean useAuthCenter) {
		this.useAuthCenter = useAuthCenter;
	}

	public String getClientComment() {
		return clientComment;
	}

	public void setClientComment(String clientComment) {
		this.clientComment = clientComment;
	}

	public Boolean getUseIntegratedManagement() {
		return useIntegratedManagement;
	}

	public void setUseIntegratedManagement(Boolean useIntegratedManagement) {
		this.useIntegratedManagement = useIntegratedManagement;
	}

	public String getParentNode() {
		return parentNode;
	}

	public void setParentNode(String parentNode) {
		this.parentNode = parentNode;
	}

	public List<String> getDisasterRecoveryList() {
		return disasterRecoveryList;
	}

	public void setDisasterRecoveryList(List<String> disasterRecoveryList) {
		this.disasterRecoveryList = disasterRecoveryList;
	}

	public List<String> getUseClientType() {
		return useClientType;
	}

	public void setUseClientType(List<String> useClientType) {
		this.useClientType = useClientType;
	}

	public Boolean getUseWebProxy() {
		return useWebProxy;
	}

	public void setUseWebProxy(Boolean useWebProxy) {
		this.useWebProxy = useWebProxy;
	}

	public Integer getWebProxyCacheSize() {
		return webProxyCacheSize;
	}

	public void setWebProxyCacheSize(Integer webProxyCacheSize) {
		this.webProxyCacheSize = webProxyCacheSize;
	}

	public Integer getWebProxyPort() {
		return webProxyPort;
	}

	public void setWebProxyPort(Integer webProxyPort) {
		this.webProxyPort = webProxyPort;
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

	public String getLdapMode() {
		return ldapMode;
	}

	public void setLdapMode(String ldapMode) {
		this.ldapMode = ldapMode;
	}

	public String getDeviceExpireMsg() {
		return deviceExpireMsg;
	}

	public void setDeviceExpireMsg(String deviceExpireMsg) {
		this.deviceExpireMsg = deviceExpireMsg;
	}

	public ExternalVpn getExtVpn() {
		return extVpn;
	}

	public void setExtVpn(ExternalVpn extVpn) {
		this.extVpn = extVpn;
	}

	public List<String> getDeviceKeyType() {
		return deviceKeyType;
	}

	public void setDeviceKeyType(List<String> deviceKeyType) {
		this.deviceKeyType = deviceKeyType;
	}

	public String getAdminServletName() {
		return adminServletName;
	}

	public void setAdminServletName(String adminServletName) {
		this.adminServletName = adminServletName;
	}

	public String getPageTitle() {
		return pageTitle;
	}

	public void setPageTitle(String pageTitle) {
		this.pageTitle = pageTitle;
	}

	public String getSubjectDnCharset() {
		return subjectDnCharset;
	}

	public void setSubjectDnCharset(String subjectDnCharset) {
		this.subjectDnCharset = subjectDnCharset;
	}

	public String getSubjectDnEncoding() {
		return subjectDnEncoding;
	}

	public void setSubjectDnEncoding(String subjectDnEncoding) {
		this.subjectDnEncoding = subjectDnEncoding;
	}

	public String getPasswordEncoding() {
		return passwordEncoding;
	}

	public void setPasswordEncoding(String passwordEncoding) {
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

	public String getDeviceAuthMode() {
		return deviceAuthMode;
	}

	public void setDeviceAuthMode(String deviceAuthMode) {
		this.deviceAuthMode = deviceAuthMode;
	}

	public String getProtocol() {
		return protocol;
	}

	public void setProtocol(String protocol) {
		this.protocol = protocol;
	}

	public int getLoginMethod() {
		return loginMethod;
	}

	public void setLoginMethod(int loginMethod) {
		this.loginMethod = loginMethod;
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

	public boolean isUseRadius() {
		return useRadius;
	}

	public void setUseRadius(boolean useRadius) {
		this.useRadius = useRadius;
	}

	public String getRadiusOrgUnitId() {
		return radiusOrgUnitId;
	}

	public void setRadiusOrgUnitId(String radiusOrgUnitId) {
		this.radiusOrgUnitId = radiusOrgUnitId;
	}

	public boolean isUseLdap() {
		return useLdap;
	}

	public void setUseLdap(boolean useLdap) {
		this.useLdap = useLdap;
	}

	public boolean isUseSplitRouting() {
		return useSplitRouting;
	}

	public void setUseSplitRouting(boolean useSplitRouting) {
		this.useSplitRouting = useSplitRouting;
	}

	public List<SplitRoutingEntry> getSplitRoutingEntries() {
		return splitRoutingEntries;
	}

	public void setSplitRoutingEntries(List<SplitRoutingEntry> splitRoutingEntries) {
		this.splitRoutingEntries = splitRoutingEntries;
	}

//	public String getDnsPostfix() {
//		return dnsPostfix;
//	}
//	
//	public void setDnsPostfix(String dnsPostfix) {
//		this.dnsPostfix = dnsPostfix;
//	}

	public String getLnkName() {
		return lnkName;
	}

	public void setLnkName(String lnkName) {
		this.lnkName = lnkName;
	}

	public boolean isUseSqlAuth() {
		return useSqlAuth;
	}

	public void setUseSqlAuth(boolean useSqlAuth) {
		this.useSqlAuth = useSqlAuth;
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

	public String getIdentificationMode() {
		return identificationMode;
	}

	public void setIdentificationMode(String identificationMode) {
		this.identificationMode = identificationMode;
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

	public Integer getAutoUserLockDate() {
		return autoUserLockDate;
	}

	public void setAutoUserLockDate(Integer autoUserLockDate) {
		this.autoUserLockDate = autoUserLockDate;
	}

}