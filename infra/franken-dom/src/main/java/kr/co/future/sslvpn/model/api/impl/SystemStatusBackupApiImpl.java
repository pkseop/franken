package kr.co.future.sslvpn.model.api.impl;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.apache.felix.ipojo.annotations.Validate;

import kr.co.future.confdb.Config;
import kr.co.future.confdb.ConfigDatabase;
import kr.co.future.confdb.ConfigService;
import kr.co.future.confdb.Predicates;
import kr.co.future.sslvpn.model.SystemStatusBackup;
import kr.co.future.sslvpn.model.api.SystemStatusBackupApi;

@Component(name = "system-status-backup-api")
@Provides
public class SystemStatusBackupApiImpl implements SystemStatusBackupApi{

	@Requires
	private ConfigService conf;
	
	private SystemStatusBackup cachedConfig = null;
	
	@Validate
	public void start() {
		ConfigDatabase db = conf.ensureDatabase(DB_NAME);
		Config c = db.findOne(SystemStatusBackup.class, Predicates.field("id", 1));
		
		if (c != null)
			cachedConfig = c.getDocument(SystemStatusBackup.class);
		else {
			SystemStatusBackup config = new SystemStatusBackup();
			config.setId(1);
			config.setPassword(null);
			config.setUseBackup(false);
			config.setSchedule(null);
			config.setUseFtp(false);
			db.add(config);
			cachedConfig = config;
		}
	}
	
	@Override
	public SystemStatusBackup getSystemStatusBackup() {
		return cachedConfig;
	}

	@Override
	public void setSystemStatusBackup(SystemStatusBackup config) {
		ConfigDatabase db = conf.ensureDatabase(DB_NAME);
		Config c = db.findOne(SystemStatusBackup.class, Predicates.field("id", 1));

		if (c == null)
			db.add(config);
		else
			db.update(c, config, true);

		cachedConfig = config;
	}

}
