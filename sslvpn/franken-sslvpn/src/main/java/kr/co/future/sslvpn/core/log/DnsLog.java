package kr.co.future.sslvpn.core.log;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import kr.co.future.logstorage.Log;

public class DnsLog {
	private Date date;
	
	private String clientIp;
	
	private String type;
	
	private String msg;
	
	private String domain;
	
	public String getDomain() {
		return domain;
	}

	public void setDomain(String domain) {
		this.domain = domain;
	}

	public Date getDate() {
		return date;
	}

	public void setDate(Date date) {
		this.date = date;
	}

	public String getClientIp() {
		return clientIp;
	}

	public void setClientIp(String clientIp) {
		this.clientIp = clientIp;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getMsg() {
		return msg;
	}

	public void setMsg(String msg) {
		this.msg = msg;
	}
	
	public Log toLog() {
		Map<String, Object> m = new HashMap<String, Object>();
		m.put("client", clientIp);
		m.put("type", type);
		m.put("msg", msg);
		m.put("domain", domain);

		return new Log("dnsproxy", date, m);
	}
	
}