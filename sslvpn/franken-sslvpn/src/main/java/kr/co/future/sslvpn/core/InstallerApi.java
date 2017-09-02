package kr.co.future.sslvpn.core;

import kr.co.future.sslvpn.core.InstallMonitor;

public interface InstallerApi {
	void install(InstallMonitor monitor) throws Exception;
}
