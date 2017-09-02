package kr.co.future.sslvpn.auth.pdas;

public interface PdasAuthApi {
	boolean verifyPassword(String account, String password);
	
	void setConfig(PdasConfig config);

	PdasConfig getConfig();
}
