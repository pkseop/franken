package kr.co.future.sslvpn.userui.impl;

import kr.co.future.sslvpn.userui.ClientDownloadConfig;
import kr.co.future.sslvpn.userui.ClientDownloadConfigApi;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.apache.felix.ipojo.annotations.Validate;

import kr.co.future.confdb.Config;
import kr.co.future.confdb.ConfigDatabase;
import kr.co.future.confdb.ConfigService;
import kr.co.future.confdb.Predicates;

@Component(name = "client-download-config-api")
@Provides
public class ClientDownloadConfigApiImpl implements ClientDownloadConfigApi{
	@Requires
	private ConfigService conf;
	
	private ClientDownloadConfig cachedConfig = null;
	
	@Validate
	public void start() {
		ConfigDatabase db = conf.ensureDatabase(DB_NAME);
		Config c = db.findOne(ClientDownloadConfig.class, Predicates.field("id", 1));

		if (c != null)
			cachedConfig = c.getDocument(ClientDownloadConfig.class);
		else {
			ClientDownloadConfig config = new ClientDownloadConfig();
			config.setId(1);
			config.setEnable(true);	//default
			config.setRedirectURL("");
			config.setEnableLocalDownload(true); //default
			db.add(config);
			cachedConfig = config;
		}
	}
	
	@Override
	public ClientDownloadConfig getClientDownloadConfig() {
		return cachedConfig;
	}
	
	@Override
	public void setClientDownloadConfig(ClientDownloadConfig config) {
		ConfigDatabase db = conf.ensureDatabase(DB_NAME);
		Config c = db.findOne(ClientDownloadConfig.class, Predicates.field("id", 1));

		if (c == null)
			db.add(config);
		else
			db.update(c, config, true);

		cachedConfig = config;
	}
	
	@Override
	public Boolean isEnabled() {
		if(cachedConfig != null) {
			return cachedConfig.isEnabled();
		}
		return true;
	}

	@Override
	public String getRedirectURL() {
		if(cachedConfig != null) {
			return cachedConfig.getRedirectURL();
		}
		return "";
	}

	@Override
	public Boolean isEnableLocalDownload() {
		if(cachedConfig != null) {
			return cachedConfig.isEnableLocalDownload();
		}
		return true;
	}

    @Override
    public Boolean isEnableSpecificDownload() {
        if(cachedConfig != null) {
            return cachedConfig.isEnableSpecificDownload();
        }
        return true;
    }
    
    @Override
    public Boolean useHttpRedirectForSetupFile() {
    	if(cachedConfig != null) {
            return cachedConfig.useHttpRedirectForSetupFile();
        }
        return false;
    }
}
