package kr.co.future.sslvpn.model.api;

import java.util.List;

import kr.co.future.sslvpn.model.ClientApp;

public interface ClientAppApi {
	List<ClientApp> getClientApps();

	List<ClientApp> getClientApps(String appGuid);

	ClientApp getClientApp(String id);

	void createClientApp(ClientApp client);

	void updateClientApp(ClientApp client);

	void removeClientApp(String id);

	void removeClientApps(List<String> ids);

	void migrateClientApps();

	void addListener(ClientAppEventListener listener);

	void removeListener(ClientAppEventListener listener);
}
