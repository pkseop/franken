package kr.co.future.sslvpn.core.backup.impl;

import java.io.File;
import java.util.Date;
import java.util.Map;

import kr.co.future.sslvpn.model.LogSetting;
import kr.co.future.sslvpn.model.api.LogSettingApi;
import kr.co.future.sslvpn.core.backup.BackupFileCleaner;
import kr.co.future.sslvpn.core.backup.Constants;

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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(name = "backup-file-cleaner")
@Provides
public class BackupFileCleanerImpl implements Runnable, Constants, BackupFileCleaner {
	private Logger logger = LoggerFactory.getLogger(BackupFileCleanerImpl.class);
	
	private static final long KEEP_FILE_DAYS_IN_MILLI_SECONDS = 7 * 24 * 60 * 60 * 1000;
	
	@ServiceProperty(name = "instance.name")
	private String instanceName;
	
	@Requires
	private CronService cronService;
	
	@Requires
	private LogSettingApi logSettingApi;
	
	@Requires
	private ConfigService conf;
	
	@Validate
	public void start() {
		//LogSettingApi를 써서 값을 가져오면 처음 번들이 올라올 때에 로딩이 다 안되어 있는 상태라 값을 못가져와 스케줄을 등록하지 못할 수 있다.
		ConfigDatabase db = conf.ensureDatabase(LogSettingApi.DB_NAME);
		Config c = db.findOne(LogSetting.class, Predicates.field("id", 1));
		if (c != null) {
			LogSetting logSetting = c.getDocument(LogSetting.class);
			if(logSetting.getFtpUse())
				registerSchedule();
		}
	}
	
	@Invalidate
	public void stop() {
		unregisterSchedule();
	}
	
	@Override
	public void registerSchedule() {		
		Integer cronJobId = getCronJobId();
		if (cronJobId == null) {
			try {
				Schedule schedule = new Schedule.Builder(instanceName).build("30 3 * * *");	//매일 3:30에 동작.
				cronService.registerSchedule(schedule);
				logger.info("schedule register success for preserved backup file cleaner");
			} catch (Exception e) {
				logger.error("preserved backup file cleaner: cron register failed");
			}
		}
	}

	@Override
	public void unregisterSchedule() {
		Integer cronJobId = getCronJobId();
		if (cronJobId != null) {
			cronService.unregisterSchedule(cronJobId);
			logger.info("schedule unregister success for preserved backup file cleaner");
		}
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
	public void run() {
		File dir = new File(PRESERVE_DIR);
		File[] fileList = dir.listFiles();
		
		long curTime = new Date().getTime();
		for(File file : fileList) {
			long fileModifiedTime = file.lastModified();
			long diff = curTime - fileModifiedTime;
			if(diff >= KEEP_FILE_DAYS_IN_MILLI_SECONDS) {
				String fileName = file.getName();
				file.delete();
				logger.info("preserved backup file [{}] removed.", fileName);
			}
		}
	}
}