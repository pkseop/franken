package kr.co.future.sslvpn.model.api.impl;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.apache.felix.ipojo.annotations.Validate;

import kr.co.future.confdb.Config;
import kr.co.future.confdb.ConfigDatabase;
import kr.co.future.confdb.ConfigService;
import kr.co.future.confdb.Predicates;
import kr.co.future.sslvpn.model.LogSetting;
import kr.co.future.sslvpn.model.api.LogSettingApi;

@Component(name = "log-setting-api")
@Provides
public class LogSettingApiImpl implements LogSettingApi {
	
	@Requires
	private ConfigService conf;
	
	private LogSetting cachedConfig = null;
	
	@Validate
	public void start() {
		ConfigDatabase db = conf.ensureDatabase(DB_NAME);
		Config c = db.findOne(LogSetting.class, Predicates.field("id", 1));

		if (c != null)
			cachedConfig = c.getDocument(LogSetting.class);
		else {
			LogSetting config = new LogSetting();
			config.setId(1);
			config.setFtpUse(false);
			config.setBackupUse(false);
			config.setFtpType(false);
			config.setBackupMaintainDays(30);
			db.add(config);
			cachedConfig = config;
		}
	}

	@Override
   public LogSetting getLogSetting() {
	   return cachedConfig;
   }
	
	@Override
	public void setLogSetting(LogSetting config) {
		ConfigDatabase db = conf.ensureDatabase(DB_NAME);
		Config c = db.findOne(LogSetting.class, Predicates.field("id", 1));

		if (c == null)
			db.add(config);
		else
			db.update(c, config, true);

		cachedConfig = config;
	}
}
