package kr.co.future.sslvpn.core.msgbus;

import kr.co.future.sslvpn.model.api.LogSettingApi;
import kr.co.future.sslvpn.xtmconf.XtmConfig;
import kr.co.future.sslvpn.xtmconf.manage.LogSetting;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Requires;

import kr.co.future.logstorage.DiskLackAction;
import kr.co.future.logstorage.DiskSpaceType;
import kr.co.future.logstorage.LogStorageMonitor;
import kr.co.future.msgbus.Request;
import kr.co.future.msgbus.Response;
import kr.co.future.msgbus.handler.MsgbusMethod;
import kr.co.future.msgbus.handler.MsgbusPlugin;
import kr.co.future.sslvpn.core.backup.BackupFileCleaner;
import kr.co.future.sslvpn.core.backup.LogBackup;

@Component(name = "log-setting-plugin")
@MsgbusPlugin
public class LogSettingPlugin {
	
	@Requires
	private LogStorageMonitor logStorageMonitor;
	
	@Requires
	private LogSettingApi logSettingApi;
	
	@Requires
	private LogBackup logBackup;
	
	@Requires
	private BackupFileCleaner backupFileCleaner;
	
	@MsgbusMethod
	public void updateLogSetting(Request req, Response resp) {
		updateLogStorageSetting();
		updateBackup();
	}
	
	public void updateLogStorageSetting() {
		for (LogSetting ls : XtmConfig.readConfig(LogSetting.class)) {
			if (ls.getType() == LogSetting.Type.Setting) {
				if (ls.getFreeze() == LogSetting.FreezeType.Overwrite)
					logStorageMonitor.setDiskLackAction(DiskLackAction.RemoveOldLog);
				else if (ls.getFreeze() == LogSetting.FreezeType.Stop)
					logStorageMonitor.setDiskLackAction(DiskLackAction.StopLogging);

				if (ls.getFreezeValue() != null)
					logStorageMonitor.setMinFreeSpace(100 - ls.getFreezeValue(), DiskSpaceType.Percentage);
			}
		}
	}

	public void updateBackup() {
		kr.co.future.sslvpn.model.LogSetting logSetting = logSettingApi.getLogSetting();
		if (logSetting.getBackupUse() == null){
			logSetting.setBackupUse(false);
		}
		
		if(logSetting.getBackupUse()) {
			logBackup.unregisterSchedule();
			logBackup.registerSchedule(logSetting.getBackupSchedule());
		} else {
			logBackup.unregisterSchedule();
		}
		
		if (logSetting.getFtpUse() == null){
			logSetting.setFtpUse(false);
		}
		
		if(logSetting.getFtpUse()) {
			backupFileCleaner.unregisterSchedule();
			backupFileCleaner.registerSchedule();
		} else {
			backupFileCleaner.unregisterSchedule();
		}
	}
}
