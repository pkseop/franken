package kr.co.future.sslvpn.core.msgbus;

import kr.co.future.sslvpn.model.SystemStatusBackup;
import kr.co.future.sslvpn.model.api.SystemStatusBackupApi;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Requires;

import kr.co.future.msgbus.Request;
import kr.co.future.msgbus.Response;
import kr.co.future.msgbus.handler.MsgbusMethod;
import kr.co.future.msgbus.handler.MsgbusPlugin;
import kr.co.future.sslvpn.core.backup.PolicyBackup;

@Component(name = "system-status-plugin")
@MsgbusPlugin
public class SystemStatusPlugin {
	
	@Requires
	private SystemStatusBackupApi systemStatusBackupApi;
	
	@Requires
	private PolicyBackup policyBackup;
	
	@MsgbusMethod
	public void updateBackup(Request req, Response resp) {
		SystemStatusBackup config = systemStatusBackupApi.getSystemStatusBackup();
		if(config.getUseBackup()) {
			policyBackup.unregisterSchedule();
			policyBackup.registerSchedule(config.getSchedule());
		} else {
			policyBackup.unregisterSchedule();
		}
	}
}
