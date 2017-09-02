package kr.co.future.sslvpn.model.api;

import kr.co.future.sslvpn.model.LogSetting;

public interface LogSettingApi {
	public static final String DB_NAME = "frodo-config";
	
	public LogSetting getLogSetting();
	
	public void setLogSetting(LogSetting config);
	
//	public void updateFtpConfig();
}
