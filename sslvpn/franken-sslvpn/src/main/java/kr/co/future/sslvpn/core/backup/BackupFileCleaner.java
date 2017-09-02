package kr.co.future.sslvpn.core.backup;

public interface BackupFileCleaner {
	public void registerSchedule();
	
	public void unregisterSchedule();
}
