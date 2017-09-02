package kr.co.future.sslvpn.auth.hb;

import java.util.Map;

public interface HbAuthApi {
	Object login(Map<String, Object> props);

	HbConfig getConfig();

	void setConfig(HbConfig config);
}
