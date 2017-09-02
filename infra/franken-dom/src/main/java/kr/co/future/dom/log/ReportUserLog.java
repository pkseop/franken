package kr.co.future.dom.log;

import java.util.Collection;

import kr.co.future.dom.model.User;

public interface ReportUserLog {
	
	public void writeUserLog(User user, String state);
	
	public void writeUsersLog(Collection<User> userList, String state);
	
	public void writeUsersLog(String loginName, String name, String sourceType, String state);
}
