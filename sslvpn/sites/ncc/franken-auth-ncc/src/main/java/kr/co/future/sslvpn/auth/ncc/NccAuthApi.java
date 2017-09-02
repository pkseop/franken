package kr.co.future.sslvpn.auth.ncc;

import kr.co.future.sslvpn.auth.ExternalAuthApi;

public interface NccAuthApi extends ExternalAuthApi {
	void setConfig(NccConfig config);

	NccConfig getConfig();
}
