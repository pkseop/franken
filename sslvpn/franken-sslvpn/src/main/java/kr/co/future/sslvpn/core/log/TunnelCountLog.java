package kr.co.future.sslvpn.core.log;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import kr.co.future.logstorage.Log;

public class TunnelCountLog {
	private Date date;

	int tunnelCount;

	public int getTunnelCount() {
		return tunnelCount;
	}

	public void setTunnelCount(int tunnelCount) {
		this.tunnelCount = tunnelCount;
	}

	public Date getDate() {
		return date;
	}

	public void setDate(Date date) {
		this.date = date;
	}

	public Log toLog() {
		Map<String, Object> m = new HashMap<String, Object>();
		m.put("tunnel_count", tunnelCount);

		return new Log("ssl-tunnelcount", date, m);
	}
}
