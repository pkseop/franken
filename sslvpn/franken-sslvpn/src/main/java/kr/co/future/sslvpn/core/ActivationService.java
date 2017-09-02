package kr.co.future.sslvpn.core;

import kr.co.future.sslvpn.core.ActivationListener;

public interface ActivationService {
	void activate();
	
	void addListener(ActivationListener listener);

	void removeListener(ActivationListener listener);
}
