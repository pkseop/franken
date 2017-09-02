package kr.co.future.sslvpn.xtmconf;

import java.net.InetAddress;

public interface ConfigEventListener {
	void onTapIPChanged(InetAddress inetAddress);
}
