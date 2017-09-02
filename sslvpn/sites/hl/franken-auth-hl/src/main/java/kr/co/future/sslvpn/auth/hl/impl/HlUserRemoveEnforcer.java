package kr.co.future.sslvpn.auth.hl.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Invalidate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.apache.felix.ipojo.annotations.ServiceProperty;
import org.apache.felix.ipojo.annotations.Validate;

import kr.co.future.confdb.Config;
import kr.co.future.confdb.ConfigDatabase;
import kr.co.future.confdb.ConfigService;
import kr.co.future.confdb.Predicates;
import kr.co.future.cron.CronService;
import kr.co.future.cron.Schedule;
import kr.co.future.dom.api.OrganizationUnitApi;
import kr.co.future.dom.api.UserApi;
import kr.co.future.dom.model.OrganizationUnit;
import kr.co.future.ldap.LdapProfile;
import kr.co.future.ldap.LdapService;
import kr.co.future.ldap.LdapUser;
import kr.co.future.sslvpn.auth.hl.HlAuthApi;
import kr.co.future.sslvpn.auth.hl.HlConfig;
import kr.co.future.sslvpn.auth.hl.HlUserRemoveEnforcerApi;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(name = "hl-user-remove-enforcer")
@Provides
public class HlUserRemoveEnforcer implements Runnable, HlUserRemoveEnforcerApi {
	private Logger logger = LoggerFactory.getLogger(HlUserRemoveEnforcer.class);

	@ServiceProperty(name = "instance.name")
	private String instanceName;

	@Requires
	private LdapService ldap;

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
			hour = c.getDocument(HlConfig.class).getRemoveHour();

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
		Collection<LdapProfile> profiles = hlAuthApi.getLdapProfiles();
		Collection<String> targetLoginNames = getTargetLoginNames();
		List<String> removeLoginNames = new ArrayList<String>();

		for (String loginName : targetLoginNames) {
			boolean removable = true;
			for (LdapProfile profile : profiles) {
				try {
					LdapUser user = ldap.findUser(profile, loginName);
					if (user != null)
						removable = false;
				} catch (Exception e) {
					logger.error("frodo hl auth: cannot find user. login_name [{}]", loginName);
				}
			}
			if (removable)
				removeLoginNames.add(loginName);
		}

		logger.info("frodo hl auth: remove user " + removeLoginNames.size() + " list start.");
		domUserApi.removeUsers("localhost", removeLoginNames);
		logger.info("frodo hl auth: remove user list success.");
	}

	private Collection<String> getTargetLoginNames() {
		List<String> orgUnitGuids = new ArrayList<String>();
		for (OrganizationUnit orgUnit : orgUnitApi.getOrganizationUnits("localhost")) {
			if (orgUnit.getName().matches("\\d{5}"))
				orgUnitGuids.add(orgUnit.getGuid());
		}

		return domUserApi.getLoginNames("localhost", null, true, Predicates.in("org_unit/guid", orgUnitGuids), 0,
				Integer.MAX_VALUE);
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
			logger.error("frodo hl auth: remove user cron register failed");
		}
	}
}
