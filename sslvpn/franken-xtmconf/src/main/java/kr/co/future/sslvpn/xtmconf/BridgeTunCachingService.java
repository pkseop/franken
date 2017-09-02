package kr.co.future.sslvpn.xtmconf;

public interface BridgeTunCachingService {
	void reload();

	String getBridgedTunIp();
	
	String getBridgredTunNetmask();
}
