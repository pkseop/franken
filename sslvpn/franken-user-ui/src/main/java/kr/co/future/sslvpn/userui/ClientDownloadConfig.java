package kr.co.future.sslvpn.userui;

import java.util.HashMap;
import java.util.Map;

import kr.co.future.api.FieldOption;
import kr.co.future.confdb.CollectionName;
import kr.co.future.msgbus.Marshalable;

@CollectionName("client-download-config")
public class ClientDownloadConfig implements Marshalable{
	@FieldOption(name = "id", nullable = false)
	private int id;
	
	@FieldOption(name = "is_enabled", nullable = false)
	private Boolean isEnabled = true;
	
	@FieldOption(name = "redirect_url", nullable = false)
	private String redirectURL;
	
	@FieldOption(name = "is_enable_localdownload", nullable = false)
	private Boolean isEnableLocalDownload = true;

    @FieldOption(name = "is_enable_specific_download", nullable = false)
    private Boolean isEnableSpecificDownload = true;
    
    @FieldOption(name = "use_http_redirect_for_setup_file", nullable = false)
    private Boolean useHttpRedirectForSetupFile = false;
	
	public Boolean useHttpRedirectForSetupFile() {
		return useHttpRedirectForSetupFile;
	}

	public void setUseHttpRedirectForSetupFile(Boolean useHttpRedirectForSetupFile) {
		this.useHttpRedirectForSetupFile = useHttpRedirectForSetupFile;
	}

	public Boolean isEnableLocalDownload() {
		return isEnableLocalDownload;
	}

	public void setEnableLocalDownload(Boolean isEnableLocalDownload) {
		this.isEnableLocalDownload = isEnableLocalDownload;
	}

	public int getId() {
		return id;
	}
	
	public void setId(int id) {
		this.id = id;
	}
	
	public Boolean isEnabled() {
		return isEnabled;
	}
	
	public void setEnable(Boolean isEnabled){
		this.isEnabled = isEnabled;
	}
	
	public String getRedirectURL() {
		return redirectURL;
	}
	
	public void setRedirectURL(String redirectURL) {
		this.redirectURL = redirectURL;
	}

    public Boolean isEnableSpecificDownload() {
        return isEnableSpecificDownload;
    }

    public void setEnableSpecificDownload(Boolean isEnableSpecificDownload) {
        this.isEnableSpecificDownload = isEnableSpecificDownload;
    }

	@Override
	public Map<String, Object> marshal() {
		Map<String, Object> m = new HashMap<String, Object>();
		m.put("id", id);
		m.put("is_enabled", isEnabled);
		m.put("is_enable_localdownload", isEnableLocalDownload);
        m.put("is_enable_specific_download", isEnableSpecificDownload);
		m.put("redirect_url", redirectURL);
		m.put("use_http_redirect_for_setup_file", useHttpRedirectForSetupFile);
		
		return m;
	}
}
