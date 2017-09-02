package kr.co.future.sslvpn.core;

import java.util.Map;

import kr.co.future.sslvpn.core.SqlAuthResult;

public interface SqlAuthService {
	boolean isEnabled();

	Map<String, Object> login(Map<String, Object> props);

	String getIdn(String loginName);

	String getSubjectDn(String loginName);

	/* 범용 모듈화 */
	SqlAuthResult verifyUser(String loginName);

	SqlAuthResult verifyPassword(String loginName, String password);

	boolean isPasswordExpirySupported();

	long getPasswordExpiry(String loginName);
	
	void resetConnectionPool();
	
	void settingChanged();
	
	void enableConnPool(Boolean enableConnPool);
}
