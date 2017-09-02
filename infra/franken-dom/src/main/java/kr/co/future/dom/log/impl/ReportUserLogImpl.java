package kr.co.future.dom.log.impl;


import java.util.Collection;
import java.util.Date;

import kr.co.future.dom.log.ReportUserLog;
import kr.co.future.dom.log.UserLog;
import kr.co.future.dom.model.User;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.apache.felix.ipojo.annotations.Validate;
import kr.co.future.logstorage.Log;
import kr.co.future.logstorage.LogStorage;
import kr.co.future.logstorage.LogStorageStatus;

@Component(name = "report-user-log")
@Provides
public class ReportUserLogImpl implements ReportUserLog {
	
	@Requires
	private LogStorage storage;
	
	@Validate
	public void start() {
		ensureTable(UserLog.TABLE_NAME);
	}
	
	private void ensureTable(String tableName) {
		try {
			storage.createTable(tableName);
		} catch (IllegalStateException e) {
		}
	}
	
	@Override
	public void writeUserLog(User user, String state) {
		UserLog log = new UserLog();
		log.setDate(new Date());
		log.setLoginName(user.getLoginName());
		log.setName(user.getName());
		log.setSourceType(user.getSourceType());
		log.setState(state);
		
		Log l = log.toLog();

		if (storage.getStatus() == LogStorageStatus.Open)
			storage.write(l);
	}
	
	@Override
	public void writeUsersLog(Collection<User> userList, String state) {
		for(User user: userList) {
			writeUserLog(user, state);
		}
	}
	
	@Override 
	public void writeUsersLog(String loginName, String name, String sourceType, String state) {
		UserLog log = new UserLog();
		log.setDate(new Date());
		log.setLoginName(loginName);
		log.setName(name);
		log.setSourceType(sourceType);
		log.setState(state);
		
		Log l = log.toLog();
		
		if (storage.getStatus() == LogStorageStatus.Open)
			storage.write(l);
	}
}
