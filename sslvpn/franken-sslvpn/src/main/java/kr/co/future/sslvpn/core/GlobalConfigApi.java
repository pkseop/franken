package kr.co.future.sslvpn.core;

import kr.co.future.dom.api.EntityEventProvider;
import kr.co.future.sslvpn.core.GlobalConfig;

public interface GlobalConfigApi extends EntityEventProvider<GlobalConfig>{
	public static final String DB_NAME = "frodo-config";
	
	GlobalConfig getGlobalConfig();

	void setGlobalConfig(GlobalConfig config);
}
