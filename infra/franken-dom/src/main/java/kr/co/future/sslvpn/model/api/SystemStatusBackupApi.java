package kr.co.future.sslvpn.model.api;

import kr.co.future.sslvpn.model.SystemStatusBackup;

public interface SystemStatusBackupApi {
	public static final String DB_NAME = "frodo-config";
	
	public SystemStatusBackup getSystemStatusBackup();
	
	public void setSystemStatusBackup(SystemStatusBackup config);
}
