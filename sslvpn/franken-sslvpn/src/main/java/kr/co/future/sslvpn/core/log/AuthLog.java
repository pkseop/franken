package kr.co.future.sslvpn.core.log;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import kr.co.future.logstorage.Log;

public class AuthLog {
	private Date date;

	// login or logout
	private String type;

	// success or fail code
	private String code;

	// login name
	private String login;

	private String profile;

	private String remoteIp;

	private int remotePort;

	private int tunnel;

	private String natIp;

	private Integer osType;

	private String deviceKey;
	
	private Integer connectionDurationTime;
	
	private String closeCondition;
	
	private String userName;
	
	public String getCloseCondition() {
		return closeCondition;
	}

	public void setCloseCondition(int closeCondition) {
		switch (closeCondition) {
		case -1:
			this.closeCondition = "not close";
			break;
		case 0:	
			this.closeCondition = "normal(user)";
			break;
		case 1:
			this.closeCondition = "timeout";
			break;
		case 2:
			this.closeCondition = "normal(admin)";
			break;
		case 3:
			this.closeCondition = "abnormal(internal)";
			break;
		default:
			this.closeCondition = "abnormal(internal)";
			break;
		}
	}

	public String getUserName() {
		return userName;
	}

	public void setUserName(String userName) {
		this.userName = userName;
	}

	public Integer getConnectionDurationTime() {		
		return connectionDurationTime;
	}

	public void setConnectionDurationTime(Integer connectionDurationTime) {
		this.connectionDurationTime = connectionDurationTime;
	}

	public Integer getOsType() {
		return osType;
	}

	public void setOsType(Integer osType) {
		this.osType = osType;
	}

	public String getDeviceKey() {
		return deviceKey;
	}

	public void setDeviceKey(String deviceKey) {
		this.deviceKey = deviceKey;
	}

	public Date getDate() {
		return date;
	}

	public void setDate(Date date) {
		this.date = date;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getCode() {
		return code;
	}

	public void setCode(String code) {
		this.code = code;
	}

	public String getLogin() {
		return login;
	}

	public void setLogin(String login) {
		this.login = login;
	}

	public String getProfile() {
		return profile;
	}

	public void setProfile(String profile) {
		this.profile = profile;
	}

	public String getRemoteIp() {
		return remoteIp;
	}

	public void setRemoteIp(String remoteIp) {
		this.remoteIp = remoteIp;
	}

	public int getRemotePort() {
		return remotePort;
	}

	public void setRemotePort(int remotePort) {
		this.remotePort = remotePort;
	}

	public long getTunnel() {
		return tunnel;
	}

	public void setTunnel(int tunnel) {
		this.tunnel = tunnel;
	}

	public String getNatIp() {
		return natIp;
	}

	public void setNatIp(String natIp) {
		this.natIp = natIp;
	}

	public Log toLog() {
		Map<String, Object> m = new HashMap<String, Object>();
		m.put("type", type);
		m.put("code", code);
		m.put("login", login);
		m.put("profile", profile);
		m.put("remote_ip", remoteIp);
		m.put("remote_port", remotePort);
		m.put("tunnel", tunnel);
		m.put("nat_ip", natIp);
		m.put("os_type", osType);
		m.put("device_key", deviceKey);

		return new Log("ssl-auth", date, m);
	}
	
	@Override
	public String toString() {
		StringBuilder result = new StringBuilder();
	    String NEW_LINE = System.getProperty("line.separator");

	    result.append(this.getClass().getName() + " Object {" + NEW_LINE);
	    result.append(" type: " + type + NEW_LINE);
	    result.append(" code: " + code + NEW_LINE);
	    result.append(" login: " + login + NEW_LINE );
	    result.append(" profile: " + profile + NEW_LINE);
	    result.append(" remote_ip: " + remoteIp + NEW_LINE);
	    result.append(" remote_port: " + remotePort + NEW_LINE);
	    result.append(" tunnel: " + tunnel + NEW_LINE);
	    result.append(" nat_ip: " + natIp + NEW_LINE);
	    result.append(" os_type: " + osType + NEW_LINE);
	    result.append(" device_key: " + deviceKey + NEW_LINE);
	    result.append("}");
	    
	    return result.toString();
	}
}
