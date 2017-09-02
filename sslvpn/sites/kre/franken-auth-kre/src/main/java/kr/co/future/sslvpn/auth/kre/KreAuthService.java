package kr.co.future.sslvpn.auth.kre;

import java.util.Map;

public interface KreAuthService {
	Map<String, Object> verifyUser(String loginName);

	boolean verifySso(String loginName, String clientIp);

	boolean dummyVerifyUser(String loginName);

	void setSsoConfig(KreAuthConfig config);

	KreAuthConfig getSsoConfig();
}
