package kr.co.future.sslvpn.syslog;

import kr.co.future.api.FieldOption;
import kr.co.future.confdb.CollectionName;

@CollectionName("relay_configs")
public class SyslogRelayConfig {
	private String ip;

	@FieldOption(nullable = false)
	private int port;
	
	public SyslogRelayConfig() {
	}
	
	public SyslogRelayConfig(String ip, int port) {
		this.ip = ip;
		this.port = port;
	}

	public String getIp() {
		return ip;
	}

	public void setIp(String ip) {
		this.ip = ip;
	}

	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}

	@Override
	public String toString() {
		return ip + ":" + port;
	}
}
