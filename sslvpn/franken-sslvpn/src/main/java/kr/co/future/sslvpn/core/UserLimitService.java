package kr.co.future.sslvpn.core;


public interface UserLimitService {
	int getUserLimit();
	
	void refreshUserCount();
}
