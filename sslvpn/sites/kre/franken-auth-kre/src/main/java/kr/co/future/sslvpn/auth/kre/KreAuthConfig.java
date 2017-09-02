package kr.co.future.sslvpn.auth.kre;

import java.util.List;

import kr.co.future.api.FieldOption;
import kr.co.future.confdb.CollectionName;

@CollectionName("kre_auth_config")
public class KreAuthConfig {

	@FieldOption(name = "sso_remote_ips")
	private List<String> SsoRemoteIps;

	@FieldOption(name = "sso_api_key")
	private String SsoApiKey;

	@FieldOption(name = "use_dummy")
	private boolean useDummy = false;

	public List<String> getSsoRemoteIps() {
		return SsoRemoteIps;
	}

	public void setSsoRemoteIps(List<String> ssoRemoteIps) {
		SsoRemoteIps = ssoRemoteIps;
	}

	public boolean isUseDummy() {
		return useDummy;
	}

	public void setUseDummy(boolean useDummy) {
		this.useDummy = useDummy;
	}

	public String getSsoApiKey() {
		return SsoApiKey;
	}

	public void setSsoApiKey(String ssoApiKey) {
		SsoApiKey = ssoApiKey;
	}

	@Override
	public String toString() {
		return "KreAuthConfig [SsoRemoteIps=" + SsoRemoteIps.toString() + ", SsoApiKey=" + SsoApiKey + ", useDummy=" + useDummy
				+ "]";
	}
}
