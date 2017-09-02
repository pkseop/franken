package kr.co.future.sslvpn.core;

import java.util.List;
import java.util.Map;

public interface CertificatesChangeApi {
	public void changeCertificates(List<Map<String, Object>> certFiles, String password);

	public void rollBack();

}
