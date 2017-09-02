package kr.co.future.sslvpn.core;

import java.util.Map;

import kr.co.future.sslvpn.core.ExternalAuthConfig;

public interface ExternalAuthService {
	boolean isEnabled();

	ExternalAuthConfig getConfig();

	void setConfig(ExternalAuthConfig config);

	Map<String, Object> login(Map<String, Object> props);

	// social security number for NPKI
	String getIdn(String loginName);

	// distinguish local user
	Map<String, Object> verifyUser(String loginName);

	String getSubjectDn(String loginName);

	boolean isPasswordChangeSupported();

	void changePassword(String account, String newPassword);

	boolean isPasswordExpirySupported();

	boolean isAccountExpirySupported();

	// 만료까지 남은 기간 초단위
	long getPasswordExpiry(String loginName);

	long getAccountExpiry(String loginName);

	boolean useSso();

	boolean verifySso(String loginName, String clientIp);

	String getSsoToken(String loginName);
}
