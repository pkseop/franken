package kr.co.future.sslvpn.auth;

import java.util.Map;

public interface ExternalAuthApi {

	Object login(Map<String, Object> props);

	Map<String, Object> verifyUser(String loginName);

	String getIdn(String loginName);

	String getSubjectDn(String loginName);

	boolean isPasswordChangeSupported();

	boolean isAccountExpirySupported();

	void changePassword(String account, String newPassword);

	boolean isPasswordExpirySupported();

	long getPasswordExpiry(String loginName);

	long getAccountExpiry(String loginName);

	boolean useSso();

	String getSsoToken(String loginName);

	boolean verifySso(String loginName, String clientIp);
}
