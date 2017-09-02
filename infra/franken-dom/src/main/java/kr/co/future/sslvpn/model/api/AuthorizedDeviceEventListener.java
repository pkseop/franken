package kr.co.future.sslvpn.model.api;

import kr.co.future.sslvpn.model.AuthorizedDevice;

public interface AuthorizedDeviceEventListener {
	void onRegister(AuthorizedDevice device);

	void onUnregister(AuthorizedDevice device);

	void onUpdate(AuthorizedDevice device);
}
