package kr.co.future.dom.log;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import kr.co.future.logstorage.Log;

public class UserLog {
	
	public static final String TABLE_NAME = "user";
	
	private Date date;	

	private String loginName;	

	private String name;
	
	private String sourceType;
	
	private String state;
	
	public Date getDate() {
		return date;
	}

	public void setDate(Date date) {
		this.date = date;
	}
	
	public String getLoginName() {
		return loginName;
	}

	public void setLoginName(String loginName) {
		this.loginName = loginName;
	}
	
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getSourceType() {
		return sourceType;
	}

	public void setSourceType(String sourceType) {
		this.sourceType = sourceType;
	}

	public String getState() {
		return state;
	}

	public void setState(String state) {
		this.state = state;
	}
	
	public Log toLog() {
		Map<String, Object> m = new HashMap<String, Object>();
		m.put("login_name", loginName);
		m.put("name", name);
		m.put("source_type", sourceType);
		m.put("state", state);
		
		return new Log(TABLE_NAME, date, m);
	}
}
