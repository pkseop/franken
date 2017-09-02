package kr.co.future.sslvpn.core.backup.impl;

import java.io.File;
import java.io.IOException;
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.io.Files;

import kr.co.future.sslvpn.model.LogSetting;
import kr.co.future.sslvpn.model.SystemStatusBackup;
import kr.co.future.sslvpn.model.api.LogSettingApi;
import kr.co.future.sslvpn.model.api.SystemStatusBackupApi;
import kr.co.future.sslvpn.xtmconf.system.Backup;
import kr.co.future.sslvpn.core.backup.BackupCode;
import kr.co.future.sslvpn.core.backup.BackupType;
import kr.co.future.sslvpn.core.backup.PolicyBackup;
import kr.co.future.sslvpn.core.ftp.FtpCode;
import kr.co.future.sslvpn.core.ftp.FtpService;
import kr.co.future.sslvpn.core.ftp.SftpService;
import kr.co.future.sslvpn.core.xenics.XenicsService;

@Component(name = "policy-backup")
@Provides
public class PolicyBackupImpl implements PolicyBackup, Runnable {

	private Logger logger = LoggerFactory.getLogger(PolicyBackupImpl.class);
	
	@ServiceProperty(name = "instance.name")
	private String instanceName;
	
	@Requires
	private CronService cronService;
	
	@Requires
	private ConfigService conf;
	
	@Requires
	private SystemStatusBackupApi systemStatusBackupApi;
	
	@Requires
	private LogSettingApi logSettingApi;
	
	@Requires
	private XenicsService xenicsService;
	
	private SystemStatusBackup systemStatusBackup;
	
	@Validate
	public void start() {
		ConfigDatabase db = conf.ensureDatabase(LogSettingApi.DB_NAME);
		Config c = db.findOne(SystemStatusBackup.class, Predicates.field("id", 1));
		if (c != null) {
			SystemStatusBackup config = c.getDocument(SystemStatusBackup.class);
			if(config.getUseBackup()) {
				registerSchedule(config.getSchedule());
			}
		}
		
		//create folder for backup file.
		File dir = new File(COMPRESS_POLICY_DIR);
		if(!dir.exists())
			dir.mkdirs();
		dir = new File(PRESERVE_DIR);
		if(!dir.exists())
			dir.mkdirs();
	}
	
	@Invalidate
	public void stop() {
		unregisterSchedule();
	}
	
	@Override
	public void registerSchedule(String strSchedule) {
		Integer cronJobId = getCronJobId();
		if (cronJobId == null) {
			try {
				Schedule schedule = new Schedule.Builder(instanceName).build(strSchedule);
				cronService.registerSchedule(schedule);
				logger.info("schedule register success for policy backup");
			} catch (Exception e) {
				logger.error("log backup: cron register failed");
			}
		}
	}

	@Override
	public void unregisterSchedule() {
		Integer cronJobId = getCronJobId();
		if (cronJobId != null) {
			cronService.unregisterSchedule(cronJobId);
			logger.info("schedule unregister success for policy backup");
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
		backupStarted();
		
		File file = null;
		try {
			file = Backup.backup(systemStatusBackup.getPassword());
			String fileName = compressFile(file);
			if(fileName != null) {
				//(s)ftp를 사용하면 압축 파일을 전송함.
				if(systemStatusBackup.getUseFtp()) {
					FtpCode code = uploadFile(fileName);
					//결과 db에 업데이트
					insertDbBackupInfo(fileName, code.getCode());
					if(code == FtpCode.Success) {		//ftp로 파일전송이 완료되었으면 압축 파일을 삭제한다.
						File file2 = new File(fileName);
						Files.move(file2, new File(PRESERVE_DIR + file2.getName())); //전송된 파일은 디렉토리를 옮겨 7일 정도 보관되게 한다.
					}
				} else {
					insertDbBackupInfo(fileName, BackupCode.LocalSuccess.getCode());
				}
			}
		} catch (IOException e) {
			logger.error("i/o error occurred during policy backup", e);
			return;
		} catch (InterruptedException e) {
			logger.error("interrupted during policy backup", e);
			return;
		} finally {
			backupFinished();
		}
	}
	
	private void backupStarted() {
		logger.info("policy backup started");
		
		systemStatusBackup = systemStatusBackupApi.getSystemStatusBackup();
	}
	
	private void backupFinished() {
		//업로드가 안 된 백업파일들을 다시 전송한다.
		uploadUntrasferredCompFiles();
		
		logger.info("policy backup finished");
	}
	
	private void uploadUntrasferredCompFiles() {
		if(!systemStatusBackup.getUseFtp()) 
			return;
		
		File dir = new File(COMPRESS_POLICY_DIR); 
		File[] fileList = dir.listFiles();
		logger.info("start upload remaining backup files");
		
		for(File file : fileList) {
			FtpCode code = uploadFile(file.getAbsolutePath());			
			updateDbBackupState(file.getName(), code.getCode());
			if(code == FtpCode.Success) {	//전송 성공 시, 파일을 삭제.
				logger.info("file [{}] uploaded", file.getName());
				try {
					//전송된 파일은 디렉토리를 옮겨 7일 정도 보관되게 한다.
					Files.move(file, new File(PRESERVE_DIR + file.getName()));
				} catch (IOException e) {
					logger.error("error occurred during move file", e);
				} 
			} else {
				logger.info("failed to upload file [{}]", file.getName());
			}
		}
		logger.info("upload remaining backup files finished.");
	}
	
	private String compressFile(File file) {
		String fileName = COMPRESS_POLICY_DIR + file.getName() + ".tar.gz";
		try {
			String command = "tar czf " + fileName + " " + file.getAbsolutePath();
			Process p = Runtime.getRuntime().exec(command);
			p.waitFor();
			
			File f = new File(fileName);
			if(f.exists()) {
				logger.info("backup file compression successed");
				file.delete();
				return fileName;
			}
		} catch(Exception e) {
			logger.error("error occurred during compressing", e);
			return null;
		}
		return null;
	}
	
	private FtpCode uploadFile(String fileName) {
		LogSetting logSetting = logSettingApi.getLogSetting();
		
		if(logSetting.getFtpUse() == false) {
			logger.info("ftp doesn't configured to use");
			return FtpCode.ConnectionFail;
		}
		
		FtpService ftp =null;
		if(logSetting.getFtpType()) {
			ftp = new SftpService(logSetting.getFtpIp(), logSetting.getFtpId(), logSetting.getFtpPw(), logSetting.getFtpPath());			
		} else {
			ftp = new FtpService(logSetting.getFtpIp(), logSetting.getFtpId(), logSetting.getFtpPw(), logSetting.getFtpPath(), true);
		}
		return ftp.uploadFile(fileName);		
	}
	
	private void insertDbBackupInfo(String fileName, int stateCode) {
		File file = new File(fileName);
		xenicsService.insertBackupInfo(BackupType.Policy.getType(), file.getName(), getFileSize(file), stateCode);
	}
	
	//파일 사이즈를 문자열 형태로 전환.
	private String getFileSize(File file) {
		double size = (double)file.length();
		int unit = 0;
      while (size > 1024) {
      	size /= 1024;
         unit++;
      }
      
      String format = "%.2f%s";      
      if(unit == 0) {
          format = "%.0f%s";
      }
      
      return String.format(format, size, BYTE_UNIT[unit]);
	}
	
	private void updateDbBackupState(String fileName, int stateCode) {
		xenicsService.updateBackupState(fileName, stateCode);
	}
}
