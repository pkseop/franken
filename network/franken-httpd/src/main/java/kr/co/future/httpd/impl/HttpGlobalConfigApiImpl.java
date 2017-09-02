package kr.co.future.httpd.impl;

import kr.co.future.httpd.HttpGlobalConfig;
import kr.co.future.httpd.HttpGlobalConfigApi;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.apache.felix.ipojo.annotations.Validate;
import kr.co.future.confdb.Config;
import kr.co.future.confdb.ConfigDatabase;
import kr.co.future.confdb.ConfigService;
import kr.co.future.confdb.Predicates;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(name = "http-global-config-api")
@Provides
public class HttpGlobalConfigApiImpl implements HttpGlobalConfigApi {
	private final Logger logger = LoggerFactory.getLogger(HttpGlobalConfigApiImpl.class.getName());

	@Requires
	private ConfigService conf;
	
	private HttpGlobalConfig cachedConfig = null;
	
	@Validate
	public void start() {
		try{
			ConfigDatabase db = conf.ensureDatabase("kraken-httpd");
			Config c = db.findOne(HttpGlobalConfig.class, Predicates.field("id", 1));
	
			if (c != null)
				cachedConfig = c.getDocument(HttpGlobalConfig.class);
		}catch(Exception e){
			logger.error("error occurred during read globa config", e);
		}
	}
	
	@Override
	public HttpGlobalConfig getHttpGlobalConfig() {
		return cachedConfig;
	}

	@Override
	public void setHttpGlobalConfig(HttpGlobalConfig config) {
		ConfigDatabase db = conf.ensureDatabase("kraken-httpd");
		Config c = db.findOne(HttpGlobalConfig.class, Predicates.field("id", 1));

		if (c == null)
			db.add(config);
		else
			db.update(c, config, true);

		cachedConfig = config;
	}
	
}
