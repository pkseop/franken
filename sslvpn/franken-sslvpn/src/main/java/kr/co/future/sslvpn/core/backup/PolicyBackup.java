package kr.co.future.sslvpn.core.backup;

public interface PolicyBackup extends Constants {
	public void registerSchedule(String schedule);
	
	public void unregisterSchedule();
}
