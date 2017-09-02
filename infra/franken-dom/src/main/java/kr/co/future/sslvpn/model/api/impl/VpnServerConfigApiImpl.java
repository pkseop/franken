package kr.co.future.sslvpn.model.api.impl;

import java.util.UUID;

import kr.co.future.sslvpn.model.IOSVpnServerConfig;
import kr.co.future.sslvpn.model.IOSVpnServerConfigParams;
import kr.co.future.sslvpn.model.VpnServerConfig;
import kr.co.future.sslvpn.model.VpnServerConfigParams;
import kr.co.future.sslvpn.model.api.VpnServerConfigApi;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Invalidate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.apache.felix.ipojo.annotations.Validate;

import kr.co.future.confdb.BaseConfigDatabaseListener;
import kr.co.future.confdb.BaseConfigServiceListener;
import kr.co.future.confdb.Config;
import kr.co.future.confdb.ConfigDatabase;
import kr.co.future.confdb.ConfigService;
//import kr.co.future.dom.api.DefaultEntityEventProvider;



import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(name = "vpn-server-config-api")
@Provides
public class VpnServerConfigApiImpl //extends DefaultEntityEventProvider<VpnServerConfig> 
				implements VpnServerConfigApi{
	
	private final Logger logger = LoggerFactory.getLogger(VpnServerConfigApiImpl.class.getName());
	
	@Requires
	private ConfigService conf;
	
	private VpnServerConfig current;
	
	private IOSVpnServerConfig currentIOS;
	
	private ConfDbListener dbListener;
	private ConfServiceListener serviceListener;
	
	public VpnServerConfigApiImpl() {
		serviceListener = new ConfServiceListener();
		dbListener = new ConfDbListener();
	}
	
	@Validate
	public void start() {
		conf.addListener(serviceListener);
		ConfigDatabase db = conf.ensureDatabase("frodo-config");
		db.addListener(dbListener);

        logger.info("VpnServerConfigApiImpl initializeCache() start...");
		initializeCache();
        logger.info("VpnServerConfigApiImpl initializeCache() end...");
	}
	
	@Invalidate
	public void stop() {
		if (conf != null) {
			conf.removeListener(serviceListener);
			ConfigDatabase db = conf.ensureDatabase("frodo-config");
			db.removeListener(dbListener);
		}
	}
	
	@Override
	public void setVpnServerConfig(VpnServerConfigParams c) {
		ConfigDatabase db = conf.ensureDatabase("frodo-config");
		Config cf = db.findOne(VpnServerConfig.class, null);
		VpnServerConfig config = cf.getDocument(VpnServerConfig.class);
		// must exists
		if (config == null)
			return;
		
		config.setVpnIp(c.getVpnIp());
		config.setVpnNetmask(c.getVpnNetmask());
		config.setSslPort(c.getSslPort());	
		config.setDnsAddr1(c.getDnsAddr1());
		config.setDnsAddr2(c.getDnsAddr2());
		config.setEncryptions(c.getEncryptions());
		
		config.setUseRemoteDb(c.getUseRemoteDb());
		if(c.getUseRemoteDb()) {
			config.setRemoteKillIp(c.getRemoteKillIp());
			config.setRemoteDbHostName(c.getRemoteDbHostName());
			config.setRemoteDbLoginName(c.getRemoteDbLoginName());
			config.setRemoteDbPassword(c.getRemoteDbPassword());
			config.setRemoteDbName(c.getRemoteDbName());
			config.setRemoteDbTableName(c.getRemoteDbTableName());
			config.setRemoteDbPort(c.getRemoteDbPort());
			config.setRemoteDbSocket(c.getRemoteDbSocket());
		}
		
		config.setUseObfuscationKey(c.isUseObfuscationKey());

		if (c.isUseObfuscationKey()) {
			UUID obfucationKey = UUID.randomUUID();
			long low = obfucationKey.getLeastSignificantBits();
			long high = obfucationKey.getMostSignificantBits();
			String stringToHexObfucationKey = String.format("%016x%016x", low, high);
			config.setObfuscationKey(stringToHexObfucationKey);
		}
		
		config.setUseTcpAcceleration(c.getUseTcpAcceleration());
		config.setUsePacketCompress(c.getUsePacketCompress());
		config.setProxyPort(c.getProxyPort());
		
		logger.debug("frodo model: vpn server config [{}]", config.marshal());

		db.update(cf, config, true);

		current = config;
//		fireEntityUpdated("localhost", config);
	}
	
	@Override
	public VpnServerConfig getCurrentVpnServerConfig() {
		if (current != null)
			return current;

		ConfigDatabase db = conf.ensureDatabase("frodo-config");
		Config c = db.findOne(VpnServerConfig.class, null);
		if (c != null) {
			current = c.getDocument(VpnServerConfig.class);
			return current;
		} else
			return null;
	}
	
	@Override
	public IOSVpnServerConfig getCurrentIOSVpnServerConfig() {
		if (currentIOS != null)
			return currentIOS;

		ConfigDatabase db = conf.ensureDatabase("frodo-config");
		Config c = db.findOne(IOSVpnServerConfig.class, null);
		if (c != null) {
			currentIOS = c.getDocument(IOSVpnServerConfig.class);
			return currentIOS;
		} else
			return null;
	}
	
	private void initializeCache() {
		ConfigDatabase db = conf.ensureDatabase("frodo-config");
		Config cf = db.findOne(VpnServerConfig.class, null);
		if (cf == null)
			return;
		VpnServerConfig gw = cf.getDocument(VpnServerConfig.class);

		this.current = gw;
	}

	private class ConfDbListener extends BaseConfigDatabaseListener {
		@Override
		public void onImport(ConfigDatabase db) {
			if (!db.getName().equals("frodo-config"))
				return;
			initializeCache();
		}
	}

	private class ConfServiceListener extends BaseConfigServiceListener {
		@Override
		public void onCreateDatabase(ConfigDatabase db) {
			if (!db.getName().equals("frodo-config"))
				return;
			db.addListener(dbListener);
		}
	}

	@Override
	public void updateVpnServerConfig(VpnServerConfig config) {
		ConfigDatabase db = conf.ensureDatabase("frodo-config");
		Config c = db.findOne(VpnServerConfig.class, null);

		if (c == null)
			throw new IllegalStateException("vpn server config does not exist");

		db.update(c, config, true);
		current = config;
//		fireEntityUpdated("localhost", config);
	}

	@Override
	public void setIOSVpnServerConfig(IOSVpnServerConfigParams c) {
		ConfigDatabase db = conf.ensureDatabase("frodo-config");
		Config cf = db.findOne(IOSVpnServerConfig.class, null);
		IOSVpnServerConfig config = cf.getDocument(IOSVpnServerConfig.class);
		// must exists
		if (config == null)
			return;
		
		config.setVpnIp(c.getVpnIp());
		config.setVpnNetmask(c.getVpnNetmask());
		config.setSslPort(c.getSslPort());	
		config.setDnsAddr1(c.getDnsAddr1());
		config.setDnsAddr2(c.getDnsAddr2());
		config.setEncryptions(c.getEncryptions());
		
		config.setUseIOS(c.getUseIOS());
		
		logger.debug("frodo model: vpn server config [{}]", config.marshal());

		db.update(cf, config, true);

		currentIOS = config;
		
	}

	@Override
	public void updateIOSVpnServerConfig(IOSVpnServerConfig config) {
		ConfigDatabase db = conf.ensureDatabase("frodo-config");
		Config c = db.findOne(IOSVpnServerConfig.class, null);

		if (c == null)
			throw new IllegalStateException("vpn server config does not exist");

		db.update(c, config, true);
		currentIOS = config;
		
	}
}
