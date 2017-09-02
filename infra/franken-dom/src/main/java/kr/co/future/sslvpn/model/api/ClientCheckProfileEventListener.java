package kr.co.future.sslvpn.model.api;

import kr.co.future.sslvpn.model.ClientCheckProfile;

public interface ClientCheckProfileEventListener {
	void onCreated(ClientCheckProfile p);

	void onUpdated(ClientCheckProfile p);

	void onRemoved(ClientCheckProfile p);
}
