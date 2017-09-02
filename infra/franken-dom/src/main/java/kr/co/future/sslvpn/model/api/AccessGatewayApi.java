package kr.co.future.sslvpn.model.api;

import kr.co.future.dom.api.EntityEventProvider;
import kr.co.future.sslvpn.model.AccessGateway;
import kr.co.future.sslvpn.model.SSLConfig;

public interface AccessGatewayApi extends EntityEventProvider<AccessGateway> {
	void updatePolicySyncSchedule(String s);

	void setSSLConfig(SSLConfig c);
	
	AccessGateway getCurrentAccessGateway();

//	AccessGateway createAccessGateway(String name, String description, String license, int sslPort);

//	void updateAccessGateway(String name, String description, String license, int sslPort);

	void updateAccessGateway(AccessGateway gw);
}
