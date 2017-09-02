package kr.co.future.sslvpn.model.api.impl;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import kr.co.future.sslvpn.model.AccessGateway;
import kr.co.future.sslvpn.model.SSLConfig;
import kr.co.future.sslvpn.model.SplitRoutingEntry;
import kr.co.future.sslvpn.model.AccessGateway.DeviceAuthMode;
import kr.co.future.sslvpn.model.AccessGateway.IdentificationMode;
import kr.co.future.sslvpn.model.AccessGateway.LdapMode;
import kr.co.future.sslvpn.model.api.AccessGatewayApi;

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
import kr.co.future.cron.CronService;
import kr.co.future.cron.Schedule;
import kr.co.future.dom.api.DefaultEntityEventProvider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(name = "frodo-access-gateway-api")
@Provides
public class AccessGatewayApiImpl extends DefaultEntityEventProvider<AccessGateway> implements AccessGatewayApi {
	private static final String POLICY_SYNC_SERVLET = "frodo-policy-sync-servlet-0";
	private final Logger logger = LoggerFactory.getLogger(AccessGatewayApiImpl.class.getName());

	@Requires
	private ConfigService conf;

	@Requires
	private CronService cron;

	private AccessGateway current;

	private ConfDbListener dbListener;
	private ConfServiceListener serviceListener;

	public AccessGatewayApiImpl() {
		serviceListener = new ConfServiceListener();
		dbListener = new ConfDbListener();
	}

	@Validate
	public void start() {
		conf.addListener(serviceListener);
		ConfigDatabase db = conf.ensureDatabase("frodo");
		db.addListener(dbListener);

        logger.info("AccessGatewayApiImpl initializeCache() start...");
		initializeCache();
        logger.info("AccessGatewayApiImpl initializeCache() end...");
	}

	@Invalidate
	public void stop() {
		if (conf != null) {
			conf.removeListener(serviceListener);
			ConfigDatabase db = conf.ensureDatabase("frodo");
			db.removeListener(dbListener);
		}
	}

	@Override
	public void updatePolicySyncSchedule(String schedule) {
		try {
			int target = -1;
			Map<Integer, Schedule> m = cron.getSchedules();
			for (int id : m.keySet()) {
				Schedule s = m.get(id);
				if (s.getTaskName().equals(POLICY_SYNC_SERVLET))
					target = id;
			}

			if (target > 0)
				cron.unregisterSchedule(target);

			if (schedule != null) {
				Schedule s = new Schedule.Builder(POLICY_SYNC_SERVLET).build(schedule);
				cron.registerSchedule(s);
			}
		} catch (Exception e) {
			logger.error("frodo model: update policy sync schedule failed", e);
		}
	}

	public void setSSLConfig(SSLConfig c) {
		ConfigDatabase db = conf.ensureDatabase("frodo");
		Config cf = db.findOne(AccessGateway.class, null);
		AccessGateway gw = cf.getDocument(AccessGateway.class);

		// must exists
		if (gw == null)
			return;

		gw.setProtocol(c.getProtocol());
		gw.setLoginMethod(c.getLoginMethod());
		gw.setUseDeviceAuth(c.isUseDeviceAuth());
		gw.setUseRadiusAuth(c.isUseRadius());
		gw.setUseLdapAuth(c.isUseLdap());
		gw.setRadiusOrgUnitId(c.getRadiusOrgUnitId());
		gw.setUseSplitRouting(c.isUseSplitRouting());
//		gw.setDnsPostfix(c.getDnsPostfix());
		gw.getSplitRoutingEntries().clear();

		for (SplitRoutingEntry e : c.getSplitRoutingEntries()) {
			gw.getSplitRoutingEntries().add(e);
		}

		gw.setNotice(c.getNotice());
		gw.setLnkName(c.getLnkName());

		gw.setUseSqlAuth(c.isUseSqlAuth());
		gw.setUserSql(c.getUserSql());
		gw.setAuthSql(c.getAuthSql());
		gw.setIdnSql(c.getIdnSql());
		gw.setSubjectDnSql(c.getSubjectDnSql());
		gw.setDbAccount(c.getDbAccount());
		gw.setDbPassword(c.getDbPassword());
		gw.setDbConnectionString(c.getDbConnectionString());
		gw.setPasswordHashType(c.getPasswordHashType());

		if (c.getSaltLocation() == null)
			gw.setSaltLocation(0);
		else
			gw.setSaltLocation(c.getSaltLocation());

		gw.setSaltSql(c.getSaltSql());

		if (c.getDeviceAuthMode() == null)
			gw.setDeviceAuthMode(DeviceAuthMode.OneToMany);
		else
			gw.setDeviceAuthMode(DeviceAuthMode.valueOf(c.getDeviceAuthMode()));

		if (c.getDeviceKeyType() == null) {
			List<String> l = new ArrayList<String>();
			l.add("HDD");
			gw.setDeviceKeyType(l);
		} else
			gw.setDeviceKeyType(c.getDeviceKeyType());

		if (c.getIdentificationMode() == null)
			gw.setIdentificationMode(IdentificationMode.Idn);
		else
			gw.setIdentificationMode(IdentificationMode.valueOf(c.getIdentificationMode()));

		gw.setPasswordResetMessage(c.getPasswordResetMessage());
		gw.setIdLabel(c.getIdLabel());
		gw.setTopImage(c.getTopImage());
		gw.setNoticeImage(c.getNoticeImage());
		gw.setCertDialogImage(c.getCertDialogImage());
		gw.setSubjectDnHashType(c.getSubjectDnHashType());
		gw.setPasswordExpirySql(c.getPasswordExpirySql());
		gw.setPasswordEncoding(c.getPasswordEncoding());
		gw.setAutoUSerLockDate(c.getAutoUserLockDate());
		gw.setSubjectDnCharset(c.getSubjectDnCharset());
		gw.setSubjectDnEncoding(c.getSubjectDnEncoding());
		gw.setPageTitle(c.getPageTitle());
		gw.setAdminServletName(c.getAdminServletName());

		gw.setExternalVpn(c.getExtVpn());
		gw.setDeviceExpireMsg(c.getDeviceExpireMsg());
		gw.setLdapMode(LdapMode.valueOf(c.getLdapMode()));
		gw.setUseAutoReconnect(c.getUseAutoReconnect());
		gw.setUseClientAutoUninstall(c.getUseClientAutoUninstall());
		gw.setUseWebProxy(c.getUseWebProxy());
		gw.setWebProxyCacheSize(c.getWebProxyCacheSize());
		gw.setWebProxyPort(c.getWebProxyPort());
		gw.setDisasterRecoveryList(c.getDisasterRecoveryList());
		gw.setUseClientType(c.getUseClientType());
		gw.setUseIntegratedManagement(c.getUseIntegratedManagement());
		gw.setParentNode(c.getParentNode());
		gw.setUsePasswordReset(c.getUsePasswordReset());
		gw.setUseAuthCenter(c.getUseAuthCenter());
		gw.setClientComment(c.getClientComment());

		logger.debug("frodo model: accessgateway [{}]", gw.marshal());

		db.update(cf, gw, true);

		current = gw;
		fireEntityUpdated("localhost", gw);
	}

	@Override
	public AccessGateway getCurrentAccessGateway() {
		if (current != null)
			return current;

		ConfigDatabase db = conf.ensureDatabase("frodo");
		Config c = db.findOne(AccessGateway.class, null);
		if (c != null) {
			current = c.getDocument(AccessGateway.class);
			return current;
		} else
			return null;
	}

//	@Override
//	public AccessGateway createAccessGateway(String name, String description, String license, int sslPort) {
//		ConfigDatabase db = conf.ensureDatabase("frodo");
//
//		// create host
//		AccessGateway gw = new AccessGateway();
//		gw.setSslPort(sslPort);
//		gw.setCreateDateTime(new Date());
//		gw.setUpdateDateTime(new Date());
//
//		db.add(gw);
//
//		current = gw;
//		return gw;
//	}

	@Override
	public void updateAccessGateway(AccessGateway gw) {
		ConfigDatabase db = conf.ensureDatabase("frodo");
		Config c = db.findOne(AccessGateway.class, null);

		if (c == null)
			throw new IllegalStateException("access gateway does not exist");

		db.update(c, gw, true);
		current = gw;
		fireEntityUpdated("localhost", gw);
	}

//	@Override
//	public void updateAccessGateway(String name, String description, String license, int sslPort) {
//		ConfigDatabase db = conf.ensureDatabase("frodo");
//		Config c = db.findOne(AccessGateway.class, null);
//		AccessGateway gw = c.getDocument(AccessGateway.class);
//
//		gw.setSslPort(sslPort);
//		gw.setUpdateDateTime(new Date());
//
//		db.update(c, gw, true);
//
//		current = gw;
//	}

	private void initializeCache() {
		ConfigDatabase db = conf.ensureDatabase("frodo");
		Config cf = db.findOne(AccessGateway.class, null);
		if (cf == null)
			return;
		AccessGateway gw = cf.getDocument(AccessGateway.class);

		this.current = gw;
	}

	private class ConfDbListener extends BaseConfigDatabaseListener {
		@Override
		public void onImport(ConfigDatabase db) {
			if (!db.getName().equals("frodo"))
				return;
			initializeCache();
		}
	}

	private class ConfServiceListener extends BaseConfigServiceListener {
		@Override
		public void onCreateDatabase(ConfigDatabase db) {
			if (!db.getName().equals("frodo"))
				return;
			db.addListener(dbListener);
		}
	}
}
