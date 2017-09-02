package kr.co.future.sslvpn.core;

import java.util.HashMap;
import java.util.Map;
import kr.co.future.api.FieldOption;
import kr.co.future.confdb.CollectionName;
import kr.co.future.msgbus.Marshalable;

@CollectionName("global_config")
public class GlobalConfig implements Marshalable {

	@FieldOption(name = "id", nullable = false)
	private int id;

	@FieldOption(name = "admin_console_path", nullable = true)
	private String adminConsolePath;

	@FieldOption(name = "user_ui_path", nullable = true)
	private String userUiPath;

	@FieldOption(name = "show_windows_client_download")
	private boolean showWindowsClientDownload = true;

	@FieldOption(name = "show_linux_client_download")
	private boolean showLinuxClientDownload = true;

	@FieldOption(name = "show_ios_client_download")
	private boolean showIosClientDownload = true;

	@FieldOption(name = "show_android_client_download")
	private boolean showAndroidClientDownload = true;

	@FieldOption(name = "show_manual_download")
	private boolean showManualDownload = true;

	@FieldOption(name = "device_key_expire_time", nullable = true)
	private Integer deviceKeyExpiryDuration;

	@FieldOption(name = "show_windows_client_icon")
	private boolean showWindowsClientIcon = true;

	@FieldOption(name = "show_windows_client_connection_add_icon")
	private boolean showWindowsClientConnectionIcon = true;
	
	@FieldOption(name = "do_not_show_admin", nullable = true)
	private Boolean doNotShowAdmin;
	
	@FieldOption(name = "encrypt_vpninfo", nullable = true)
	private Boolean encryptVpninfo;
	
	@FieldOption(name = "sql_conn_pool_config", nullable = true)
	private String sqlConnPoolConfig;
	
	@FieldOption(name = "enable_conn_pool", nullable = true)
	private Boolean enableConnPool;
	
	@FieldOption(name = "admin_console_context", nullable = false)
	private String adminConsoleContext = "frodo";
	
	@FieldOption(name = "use_dup_login_check", nullable = true)
	private Boolean useDupLoginCheck;
	
	@FieldOption(name = "dup_login_check_nodes", nullable = true)
	private String dupLoginCheckNodes;
	
	@FieldOption(name = "default_dns_resolver_provider", nullable = true)
	private String defaultDnsResolverProvider;
	
	@FieldOption(name = "enable_dns_resolver", nullable = true)
	private Boolean enableDnsResolver;
	
	@FieldOption(name = "block_dup_login_check", nullable = true)
	private Boolean blockDupLoginCheck;
	
	public Boolean getBlockDupLoginCheck() {
		return blockDupLoginCheck;
	}

	public void setBlockDupLoginCheck(Boolean blockDupLoginCheck) {
		this.blockDupLoginCheck = blockDupLoginCheck;
	}
		
	public void setUseDupLoginCheck(Boolean useDupLoginCheck) {
		this.useDupLoginCheck = useDupLoginCheck;
	}

	public String getDupLoginCheckNodes() {
		return dupLoginCheckNodes;
	}

	public void setDupLoginCheckNodes(String dupLoginCheckNodes) {
		this.dupLoginCheckNodes = dupLoginCheckNodes;
	}
	
	public Boolean getUseDupLoginCheck() {
		return useDupLoginCheck;
	}

	public String getAdminConsoleContext() {
		return adminConsoleContext;
	}

	public void setAdminConsoleContext(String adminConsoleContext) {
		this.adminConsoleContext = adminConsoleContext;
	}

	public boolean isShowWindowsClientIcon() {
		return showWindowsClientIcon;
	}

	public void setShowWindowsClientIcon(boolean showWindowsClientIcon) {
		this.showWindowsClientIcon = showWindowsClientIcon;
	}

	public boolean isShowWindowsClientConnectionIcon() {
		return showWindowsClientConnectionIcon;
	}

	public void setShowWindowsClientConnectionIcon(boolean showActiveXClientIcon) {
		this.showWindowsClientConnectionIcon = showActiveXClientIcon;
	}

	public Integer getDeviceKeyExpiryDuration() {
		return deviceKeyExpiryDuration;
	}

	public void setDeviceKeyExpiryDuration(Integer deviceKeyExpiryDuration) {
		this.deviceKeyExpiryDuration = deviceKeyExpiryDuration;
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public String getAdminConsolePath() {
		return adminConsolePath;
	}

	public void setAdminConsolePath(String adminConsolePath) {
		this.adminConsolePath = adminConsolePath;
	}

	public String getUserUiPath() {
		return userUiPath;
	}

	public void setUserUiPath(String userUiPath) {
		this.userUiPath = userUiPath;
	}

	public boolean isShowWindowsClientDownload() {
		return showWindowsClientDownload;
	}

	public void setShowWindowsClientDownload(boolean showWindowsClientDownload) {
		this.showWindowsClientDownload = showWindowsClientDownload;
	}

	public boolean isShowLinuxClientDownload() {
		return showLinuxClientDownload;
	}

	public void setShowLinuxClientDownload(boolean showLinuxClientDownload) {
		this.showLinuxClientDownload = showLinuxClientDownload;
	}

	public boolean isShowIosClientDownload() {
		return showIosClientDownload;
	}

	public void setShowIosClientDownload(boolean showIosClientDownload) {
		this.showIosClientDownload = showIosClientDownload;
	}

	public boolean isShowAndroidClientDownload() {
		return showAndroidClientDownload;
	}

	public void setShowAndroidClientDownload(boolean showAndroidClientDownload) {
		this.showAndroidClientDownload = showAndroidClientDownload;
	}

	public boolean isShowManualDownload() {
		return showManualDownload;
	}

	public void setShowManualDownload(boolean showManualDownload) {
		this.showManualDownload = showManualDownload;
	}
	
	public Boolean isDoNotShowAdmin() {
		return doNotShowAdmin;
	}

	public void setDoNotShowAdmin(Boolean doNotShowAdmin) {
		this.doNotShowAdmin = doNotShowAdmin;
	}
	
	public Boolean isEncryptVpninfo() {
		return encryptVpninfo;
	}
	
	public void setEncryptVpninfo(Boolean encryptVpninfo) {
		this.encryptVpninfo = encryptVpninfo;
	}	
	
	public String getSqlConnPoolConfig() {
		return sqlConnPoolConfig;
	}
	
	public void setSqlConnPoolConfig(String sqlConnPoolConfig) {
		this.sqlConnPoolConfig = sqlConnPoolConfig;
	}
	
	public Boolean isConnPoolEnabled() {
		return enableConnPool;
	}
	
	public void enableConnPool(Boolean enableConnPool) {
		this.enableConnPool = enableConnPool;
	}
	
	@Override
	public String toString() {
		return "GlobalConfig [id=" + id + ", admin_console_path=" + adminConsolePath + ", user_ui_path=" + userUiPath
				+ ", show_windows_client_download=" + showWindowsClientDownload + ", show_linux_client_download="
				+ showLinuxClientDownload + ", show_ios_client_download=" + showIosClientDownload
				+ ", show_android_client_download=" + showAndroidClientDownload + ", show_manual_download=" + showManualDownload
				+ ", device_key_expire_time=" + deviceKeyExpiryDuration + ", show_windows_client_icon=" + showWindowsClientIcon
				+ ", show_windows_client_connection_add_icon=" + showWindowsClientConnectionIcon
				+ ", do_not_show_admin=" + doNotShowAdmin + ", encrypt_vpninfo=" +  encryptVpninfo
				+ ", sql_conn_pool_config=" + sqlConnPoolConfig + ", enable_conn_pool=" + enableConnPool
				+ ", admin_console_context=" + adminConsoleContext + ", use_dup_login_check=" + useDupLoginCheck
				+ ", dup_login_check_nodes=" + dupLoginCheckNodes + ", default_dns_resolver_provider=" + defaultDnsResolverProvider
				+ ", enable_dns_resolver=" + enableDnsResolver
				+ ", block_dup_login_check=" + blockDupLoginCheck
				+ "]";
	}

	@Override
	public Map<String, Object> marshal() {
		Map<String, Object> m = new HashMap<String, Object>();
		m.put("id", id);
		m.put("admin_console_path", adminConsolePath);
		m.put("user_ui_path", userUiPath);
		m.put("show_windows_client_download", showWindowsClientDownload);
		m.put("show_linux_client_download", showLinuxClientDownload);
		m.put("show_ios_client_download", showIosClientDownload);
		m.put("show_android_client_download", showAndroidClientDownload);
		m.put("show_manual_download", showManualDownload);
		m.put("device_key_expire_time", deviceKeyExpiryDuration);
		m.put("show_windows_client_icon", showWindowsClientIcon);
		m.put("show_windows_client_connection_add_icon", showWindowsClientConnectionIcon);
		m.put("do_not_show_admin", doNotShowAdmin);
		if(encryptVpninfo != null)
			m.put("encrypt_vpninfo", encryptVpninfo);
		if(sqlConnPoolConfig != null)
			m.put("sql_conn_pool_config", sqlConnPoolConfig);
		if(enableConnPool != null)
			m.put("enable_conn_pool", enableConnPool);
		m.put("admin_console_context", adminConsoleContext);
		m.put("use_dup_login_check", useDupLoginCheck);
		m.put("dup_login_check_nodes", dupLoginCheckNodes);
		m.put("default_dns_resolver_provider", defaultDnsResolverProvider);
		m.put("enable_dns_resolver", enableDnsResolver);
		m.put("block_dup_login_check", blockDupLoginCheck);
		
		return m;
	}
	
	public String getDefaultDnsResolverProvider() {
		return defaultDnsResolverProvider;
	}

	public void setDefaultDnsResolverProvider(String defaultDnsResolverProvider) {
		this.defaultDnsResolverProvider = defaultDnsResolverProvider;
	}

	public Boolean getEnableDnsResolver() {
		return enableDnsResolver;
	}

	public void setEnableDnsResolver(Boolean enableDnsResolver) {
		this.enableDnsResolver = enableDnsResolver;
	}
}
