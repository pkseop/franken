package kr.co.future.sslvpn.core.log;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import kr.co.future.logstorage.Log;

public class FlowLog {
	private Date date;

	private String login;

	private String userName;

	private long tunnel;

	private long session;

	private String sessionCreateDate;

	private String sessionCloseDate;

	private String clientIp;

	private int clientPort;

	private String serverIp;

	private int serverPort;

	private String protocol;

	private long txBytes;

	private long txPackets;

	private long rxBytes;

	private long rxPackets;

	private boolean eos;

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

	public long getTxBytes() {
		return txBytes;
	}

	public void setTxBytes(long txBytes) {
		this.txBytes = txBytes;
	}

	public long getTxPackets() {
		return txPackets;
	}

	public void setTxPackets(long txPackets) {
		this.txPackets = txPackets;
	}

	public long getRxBytes() {
		return rxBytes;
	}

	public void setRxBytes(long rxBytes) {
		this.rxBytes = rxBytes;
	}

	public long getRxPackets() {
		return rxPackets;
	}

	public void setRxPackets(long rxPackets) {
		this.rxPackets = rxPackets;
	}

	public boolean isEos() {
		return eos;
	}

	public void setEos(boolean eos) {
		this.eos = eos;
	}

	public String getUserName() {
		return userName;
	}

	public void setUserName(String userName) {
		this.userName = userName;
	}

	public String getSessionCreateDate() {
		return sessionCreateDate;
	}

	public void setSessionCreateDate(String sessionCreateDate) {
		this.sessionCreateDate = sessionCreateDate;
	}

	public String getSessionCloseDate() {
		return sessionCloseDate;
	}

	public void setSessionCloseDate(String sessionCloseDate) {
		this.sessionCloseDate = sessionCloseDate;
	}

	public Log toLog() {
		Map<String, Object> m = new HashMap<String, Object>();
		m.put("login", login);
		m.put("tunnel", tunnel);
//		m.put("session", session);
		m.put("client_ip", clientIp);
		m.put("client_port", clientPort);
		m.put("server_ip", serverIp);
		m.put("server_port", serverPort);
		m.put("protocol", protocol);
		m.put("tx_bytes", txBytes);
		m.put("rx_bytes", rxBytes);
		m.put("tx_packets", txPackets);
		m.put("rx_packets", rxPackets);
		m.put("eos", eos);

		return new Log("ssl-flow", date, m);
	}
}
