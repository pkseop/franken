package kr.co.future.sslvpn.core.backup;

import java.io.IOException;
import java.text.ParseException;

public interface LogBackup extends Constants{
	public void registerSchedule(String schedule);
	
	public void unregisterSchedule();
	
	public void removeRollbackedFiles(String fileName) throws ParseException, IOException;
}
