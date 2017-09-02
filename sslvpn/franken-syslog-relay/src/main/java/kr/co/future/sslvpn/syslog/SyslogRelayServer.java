package kr.co.future.sslvpn.syslog;

import java.util.List;

public interface SyslogRelayServer {
	List<SyslogRelayConfig> getConfigs();

	void addConfig(SyslogRelayConfig c);

	void removeConfig(SyslogRelayConfig c);
}
