package kr.co.future.sslvpn.auth.hl.impl;

import java.util.Map;

import kr.co.future.sslvpn.auth.hl.HlAuthApi;
import kr.co.future.sslvpn.auth.hl.HlConfig;
import kr.co.future.sslvpn.auth.hl.HlLdapUserCacheRemoveEnforcerApi;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Invalidate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.apache.felix.ipojo.annotations.ServiceProperty;
import org.apache.felix.ipojo.annotations.Validate;

import kr.co.future.confdb.Config;
import kr.co.future.confdb.ConfigDatabase;
import kr.co.future.confdb.ConfigService;
import kr.co.future.cron.CronService;
import kr.co.future.cron.Schedule;
import kr.co.future.dom.api.OrganizationUnitApi;
import kr.co.future.dom.api.UserApi;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(name = "hl-ldapuser-cache-remove-enforcer")
@Provides
public class HlLdapUserCacheRemoveEnforcer implements Runnable, HlLdapUserCacheRemoveEnforcerApi {
	private Logger logger = LoggerFactory.getLogger(HlLdapUserCacheRemoveEnforcer.class);

	@ServiceProperty(name = "instance.name")
	private String instanceName;

	@Requires
	private HlAuthApi hlAuthApi;

	@Requires
	private OrganizationUnitApi orgUnitApi;

	@Requires
	private UserApi domUserApi;

	@Requires
	private CronService cronService;

	@Requires
	private ConfigService conf;

	@Validate
	public void start() {
		int hour = 0;
		ConfigDatabase db = conf.ensureDatabase("hl");
		Config c = db.findOne(HlConfig.class, null);
		if (c != null)
			hour = c.getDocument(HlConfig.class).getRemoveCacheHour();

		registerSchedule(hour);
	}

	@Invalidate
	public void stop() {
		Integer cronJobId = getCronJobId();
		if (cronJobId != null)
			cronService.unregisterSchedule(cronJobId);
	}

	@Override
	public void run() {
		enforcer();
	}

	private void enforcer() {
		logger.info("frodo hl auth: remove ldapuser cache list start.");
		hlAuthApi.removeCacheList();
		logger.info("frodo hl auth: remove ldapuser cache list success.");
	}

	private Integer getCronJobId() {
		Map<Integer, Schedule> schedules = cronService.getSchedules();
		for (Integer id : schedules.keySet()) {
			Schedule schedule = schedules.get(id);
			if (schedule.getTaskName().equals(instanceName))
				return id;
		}
		return null;
	}

	@Override
	public void registerSchedule(int hour) {
		Integer cronJobId = getCronJobId();

		if (cronJobId != null)
			cronService.unregisterSchedule(cronJobId);

		try {
			Schedule schedule = new Schedule.Builder(instanceName).build("0 " + hour + " * * *");
			cronService.registerSchedule(schedule);
			logger.trace("frodo hl auth: regster cron schedule [{}]", schedule.toString());
		} catch (Exception e) {
			logger.error("frodo hl auth: remove ldapuser cache list cron register failed");
		}
	}
}
