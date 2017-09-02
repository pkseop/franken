package kr.co.future.sslvpn.core.impl;

import java.util.Arrays;
import java.util.Date;

import kr.co.future.confdb.Config;
import kr.co.future.confdb.ConfigDatabase;
import kr.co.future.confdb.ConfigService;
import kr.co.future.confdb.Predicates;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import kr.co.future.sslvpn.model.AccessGateway;
import kr.co.future.sslvpn.model.AccessProfile;
import kr.co.future.sslvpn.model.IOSVpnServerConfig;
import kr.co.future.sslvpn.model.OrgUnitExtension;
import kr.co.future.sslvpn.model.VpnServerConfig;
import kr.co.future.sslvpn.core.impl.InitialFrodoSchema;

public class InitialFrodoSchema {
	private static final Logger logger = LoggerFactory.getLogger(InitialFrodoSchema.class.getName());

	public static void generate(ConfigService conf) {
		try {
			ConfigDatabase db = conf.ensureDatabase("frodo");
			createAccessGateway(db);
			createVpnServerConfig(conf);
			createIOSVpnServerConfig(conf);
			createDefaultProfile(db);
		} catch (Exception e) {
			logger.error("kraken dom: schema init failed", e);
			throw new RuntimeException("cannot create schema", e);
		}
	}

	private static void createAccessGateway(ConfigDatabase db) {
		Config c = db.findOne(AccessGateway.class, null);
		if (c != null)
			return;

		AccessGateway gw = new AccessGateway();
		gw.setLoginMethod(1);
		gw.setProtocol("tcp");
		gw.setUseDeviceAuth(true);
		gw.setUseLdapAuth(false);
		gw.setUseRadiusAuth(false);
		gw.setRadiusOrgUnitId(null);
		gw.setCreateDateTime(new Date());
		gw.setUpdateDateTime(new Date());

		db.add(gw);
	}
	
	private static void createVpnServerConfig(ConfigService conf) {
		ConfigDatabase db = conf.ensureDatabase("frodo-config");
		Config c = db.findOne(VpnServerConfig.class, null);
		if (c != null)
			return;

		VpnServerConfig config = new VpnServerConfig();
		config.setVpnIp("10.0.0.0");
		config.setVpnNetmask("255.255.0.0");
		config.setSslPort(4886);
		config.setEncryptions(Arrays.asList(new String[] { "SEED" }));
		config.setUseObfuscationKey(false);
		config.setUseRemoteDb(false);
		config.setRemoteKillIp("127.0.0.1");
		config.setRemoteDbHostName("127.0.0.1");
		config.setRemoteDbLoginName("root");
		config.setRemoteDbPassword("WeGuardia_01");
		config.setRemoteDbName("xenics");
		config.setRemoteDbTableName("con_info");
		config.setRemoteDbPort("3306");
		config.setRemoteDbSocket("/tmp/mysql.sock");
		config.setProxyPort(24886);
		config.setUseTcpAcceleration(false);
		config.setUsePacketCompress(false);
		config.setCreateDateTime(new Date());
		config.setUpdateDateTime(new Date());

		db.add(config);
	}
	
	private static void createIOSVpnServerConfig(ConfigService conf) {
		ConfigDatabase db = conf.ensureDatabase("frodo-config");
		Config c = db.findOne(IOSVpnServerConfig.class, null);
		if (c != null)
			return;

		IOSVpnServerConfig config = new IOSVpnServerConfig();
		config.setVpnIp("10.0.1.0");
		config.setUseIOS(false);
		config.setVpnNetmask("255.255.0.0");
		config.setSslPort(4885);
		config.setEncryptions(Arrays.asList(new String[] { "AES128" }));
		config.setCreateDateTime(new Date());
		config.setUpdateDateTime(new Date());

		db.add(config);
	}

	private static void createDefaultProfile(ConfigDatabase db) {
		Config c = db.findOne(AccessProfile.class, null);
		if (c != null)
			return;

		// create default access profile
		AccessProfile p = new AccessProfile();
		p.setId(1);
		p.setName("default");
		p.setUseClientTimeout(true);
		p.setClientTimeout(600); // 3600 -> 600
		p.setUseFailLimit(true);
		p.setFailLimitCount(5);
		p.setPasswordExpiry(365); // 1year
		p.setCreateDateTime(new Date());
		p.setUpdateDateTime(new Date());

		db.add(p);

		// apply profile as a default policy
		c = db.findOne(OrgUnitExtension.class, Predicates.field("orgUnitId", null));
		if (c != null)
			return;

		OrgUnitExtension ext = new OrgUnitExtension();
		ext.setOrgUnitId(null);
		ext.setProfile(p);
		ext.setCreateDateTime(new Date());
		ext.setUpdateDateTime(new Date());
		db.add(ext);
	}
}
