package kr.co.future.sslvpn.core.impl;

import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import kr.co.future.sslvpn.core.AuthService;
import kr.co.future.sslvpn.core.Tunnel;
import kr.co.future.sslvpn.model.AccessProfile;
import kr.co.future.sslvpn.model.api.AccessProfileApi;
import kr.co.future.sslvpn.core.impl.ScheduleEnforcer;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Invalidate;
import org.apache.felix.ipojo.annotations.Requires;
import org.apache.felix.ipojo.annotations.Validate;

import kr.co.future.dom.api.TimetableApi;
import kr.co.future.dom.model.Schedule;
import kr.co.future.dom.model.Timetable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(name = "frodo-schedule-enforcer")
public class ScheduleEnforcer implements Runnable {
	private final Logger logger = LoggerFactory.getLogger(ScheduleEnforcer.class.getName());

	@Requires
	private AuthService auth;

	@Requires
	private TimetableApi timetableApi;

	@Requires
	private AccessProfileApi profileApi;

	private Thread t;

	private volatile boolean doStop;

	@Validate
	public void start() {
		doStop = false;
		t = new Thread(this, "Frodo Schedule Enforcer");
		t.start();
	}

	@Invalidate
	public void stop() {
		doStop = true;
		t.interrupt();
	}

	@Override
	public void run() {
		try {
			while (!doStop) {
				logger.trace("frodo-core: schedule enforcer started");
//				check();
				Thread.sleep(60000);
			}
		} catch (InterruptedException e) {
			logger.trace("frodo-core: interrupted schedule enforcer");
		} catch (Exception e) {
			logger.error("frodo-core: schedule error", e);
		} finally {
			logger.info("frodo-core: schedule enforcer stopped");
		}
	}
	
//pks. 2015-01-30. 불필요한 기능으로 사용하지 않도록 함. 본 인스턴스 자체가 생성되지 않게 metadata.xml 파일에서 주석처리함.
//	private void check() {
//		Date now = new Date();
//		Map<String, Timetable> timetables = getTimetables();
//
//		for (Tunnel t : auth.getTunnels()) {
//			if (!t.getType().equals("sslvpn"))
//				continue;
//
//			AccessProfile p = profileApi.determineProfile(t.getLoginName());
//			if (p == null)
//				continue;
//
//			String tableId = p.getAllowTimeId();
//			if (tableId == null)
//				continue;
//
//			Timetable table = timetables.get(tableId);
//			boolean ok = checkTimetable(now, table);
//
//			if (!ok) {
//				logger.info("frodo core: kill tunnel [{}], login name [{}] by profile [{}] schedule",
//						new Object[] { t.getId(), t.getLoginName(), t.getProfileId() });
//				try {
//					auth.killTunnel(t.getId());
//				} catch (Exception e) {
//					logger.error("frodo core: kill tunnel failed", e);
//				}
//			}
//		}
//	}

	private boolean checkTimetable(Date d, Timetable t) {
		Calendar c = Calendar.getInstance();
		c.setTime(d);
		int day = c.get(Calendar.DAY_OF_WEEK) - 1;

		int seconds = (int) (((d.getTime() + c.getTimeZone().getRawOffset()) / 1000) % 86400);

		for (Schedule s : t.getSchedules()) {
			if (s.getDayOfWeek() != day)
				continue;

			if (s.getBeginSecond() <= seconds && seconds <= s.getEndSecond()) {
				logger.debug("frodo core: allowed schedule [{}]", s);
				return true;
			}
		}

		logger.debug("frodo core: forbidden time for timetable [{}], current seconds [{}]", t.getGuid(), seconds);

		return false;
	}

	private Map<String, Timetable> getTimetables() {
		Map<String, Timetable> m = new HashMap<String, Timetable>();
		for (Timetable t : timetableApi.getTimetables("localhost"))
			m.put(t.getGuid(), t);
		return m;
	}
}
