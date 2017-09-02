package kr.co.future.sslvpn.core;

public interface DownloadService {
	void addDownloadToken(String token);
	
	void removeDownloadToken(String token);
}
