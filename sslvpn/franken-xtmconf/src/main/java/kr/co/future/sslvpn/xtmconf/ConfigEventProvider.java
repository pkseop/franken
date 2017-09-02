package kr.co.future.sslvpn.xtmconf;

public interface ConfigEventProvider {
	void addListener(ConfigEventListener listener);
	void removeListener(ConfigEventListener listener);
}
