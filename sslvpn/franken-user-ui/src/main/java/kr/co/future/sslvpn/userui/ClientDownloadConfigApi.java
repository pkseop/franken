package kr.co.future.sslvpn.userui;

public interface ClientDownloadConfigApi {
	public static final String DB_NAME = "client-download-config";
	
	public ClientDownloadConfig getClientDownloadConfig();
	public void setClientDownloadConfig(ClientDownloadConfig config);
	public Boolean isEnabled();
	public String getRedirectURL();
	public Boolean isEnableLocalDownload();
    public Boolean isEnableSpecificDownload();
    public Boolean useHttpRedirectForSetupFile();
}
