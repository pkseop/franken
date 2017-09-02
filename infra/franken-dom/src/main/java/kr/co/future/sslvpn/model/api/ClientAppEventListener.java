package kr.co.future.sslvpn.model.api;

import kr.co.future.sslvpn.model.ClientApp;

public interface ClientAppEventListener {
	void onCreated(ClientApp app);

	void onUpdated(ClientApp app);

	void onRemoved(ClientApp app);
}
