package kr.co.future.sslvpn.core;

import kr.co.future.sslvpn.core.Tunnel;

public interface TunnelEventListener {
	void onOpen(Tunnel t);

	void onClose(Tunnel t);

	void onAllClose();
}
