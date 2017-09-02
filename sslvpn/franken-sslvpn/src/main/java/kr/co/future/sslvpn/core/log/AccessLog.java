package kr.co.future.sslvpn.core.log;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import kr.co.future.logstorage.Log;

public class AccessLog {
	private Date date;

	private String login;

	private long tunnel;

	private long session;

	// allow or deny
	private String action;

	private String clientIp;

	private int clientPort;

	private String serverIp;

	private int serverPort;

	private String protocol;
	
	public Date getDate() {
		return date;
	}

	public void setDate(Date date) {
		this.date = date;
	}

	public String getLogin() {
		return login;
	}

	public void setLogin(String login) {
		this.login = login;
	}

	public long getTunnel() {
		return tunnel;
	}

	public void setTunnel(long tunnel) {
		this.tunnel = tunnel;
	}

	public long getSession() {
		return session;
	}

	public void setSession(long session) {
		this.session = session;
	}

	public String getAction() {
		return action;
	}

	public void setAction(String action) {
		this.action = action;
	}

	public String getClientIp() {
		return clientIp;
	}

	public void setClientIp(String clientIp) {
		this.clientIp = clientIp;
	}

	public int getClientPort() {
		return clientPort;
	}

	public void setClientPort(int clientPort) {
		this.clientPort = clientPort;
	}

	public String getServerIp() {
		return serverIp;
	}

	public void setServerIp(String serverIp) {
		this.serverIp = serverIp;
	}

	public int getServerPort() {
		return serverPort;
	}

	public void setServerPort(int serverPort) {
		this.serverPort = serverPort;
	}

	public String getProtocol() {
		return protocol;
	}

	public void setProtocol(String protocol) {
		this.protocol = protocol;
	}

	public Log toLog() {
		Map<String, Object> m = new HashMap<String, Object>();
		m.put("login", login);
		m.put("tunnel", tunnel);
//		m.put("session", session);
		m.put("action", action);
		m.put("client_ip", clientIp);
		m.put("client_port", clientPort);
		m.put("server_ip", serverIp);
		m.put("server_port", serverPort);
		m.put("protocol", protocol);
		return new Log("ssl-access", date, m);
	}
}
