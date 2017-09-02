package kr.co.future.sslvpn.core.impl;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.apache.felix.ipojo.annotations.Validate;
import kr.co.future.confdb.Config;
import kr.co.future.confdb.ConfigDatabase;
import kr.co.future.confdb.ConfigService;
import kr.co.future.confdb.Predicates;
import kr.co.future.dom.api.DefaultEntityEventProvider;

import kr.co.future.sslvpn.core.GlobalConfig;
import kr.co.future.sslvpn.core.GlobalConfigApi;

@Component(name = "frodo-global-config-api")
@Provides
public class GlobalConfigApiImpl extends DefaultEntityEventProvider<GlobalConfig> implements GlobalConfigApi {
	@Requires
	private ConfigService conf;

	private GlobalConfig cachedConfig = null;

	@Validate
	public void start() {
		ConfigDatabase db = conf.ensureDatabase(DB_NAME);
		Config c = db.findOne(GlobalConfig.class, Predicates.field("id", 1));

		if (c != null)
			cachedConfig = c.getDocument(GlobalConfig.class);
		else {
			GlobalConfig config = new GlobalConfig();
			config.setId(1);
			db.add(config);
			cachedConfig = config;
		}
	}

	@Override
	public GlobalConfig getGlobalConfig() {
		return cachedConfig;
	}

	@Override
	public void setGlobalConfig(GlobalConfig config) {
		ConfigDatabase db = conf.ensureDatabase(DB_NAME);
		Config c = db.findOne(GlobalConfig.class, Predicates.field("id", 1));

		if (c == null)
			db.add(config);
		else
			db.update(c, config, true);

		cachedConfig = config;
		
		fireEntityUpdated("localhost", config);
	}
}
