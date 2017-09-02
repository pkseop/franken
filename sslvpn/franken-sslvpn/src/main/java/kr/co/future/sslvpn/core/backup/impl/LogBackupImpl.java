package kr.co.future.sslvpn.core.backup.impl;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import kr.co.future.sslvpn.model.LogSetting;
import kr.co.future.sslvpn.model.api.LogSettingApi;
import kr.co.future.sslvpn.core.backup.BackupCode;
import kr.co.future.sslvpn.core.backup.BackupType;
import kr.co.future.sslvpn.core.backup.LogBackup;
import kr.co.future.sslvpn.core.ftp.FtpCode;
import kr.co.future.sslvpn.core.ftp.FtpService;
import kr.co.future.sslvpn.core.ftp.SftpService;
import kr.co.future.sslvpn.core.xenics.XenicsService;

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

@Component(name = "log-backup")
@Provides
public class LogBackupImpl implements LogBackup, Runnable {
	
	private Logger logger = LoggerFactory.getLogger(LogBackupImpl.class);
	
	@ServiceProperty(name = "instance.name")
	private String instanceName;
	
	@Requires
	private CronService cronService;
	
	@Requires
	private LogSettingApi logSettingApi;
	
	@Requires
	private XenicsService xenicsService;
	
	@Requires
	private ConfigService conf;
	
	private kr.co.future.sslvpn.model.LogSetting logSetting;
	
	private ArrayList<File> backupFileList;
	
	private Date latestDate, oldestDate;
	
	@Validate
	public void start() {
		//LogSettingApi를 써서 값을 가져오면 처음 번들이 올라올 때에 로딩이 다 안되어 있는 상태라 값을 못가져와 스케줄을 등록하지 못할 수 있다. 
		ConfigDatabase db = conf.ensureDatabase(LogSettingApi.DB_NAME);
		Config c = db.findOne(LogSetting.class, Predicates.field("id", 1));
		if (c != null) {
			logSetting = c.getDocument(LogSetting.class);
			if(logSetting.getBackupUse() && logSetting.getBackupSchedule() != null) {
				registerSchedule(logSetting.getBackupSchedule());
			}
		}
		
		//create folder for backup file.
		File dir = new File(COMPRESS_LOG_DIR);
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
   public void registerSchedule(String value) {
		logSetting = logSettingApi.getLogSetting();
		Integer cronJobId = getCronJobId();
		if (cronJobId == null) {
			try {
				Schedule schedule = new Schedule.Builder(instanceName).build(value);
				cronService.registerSchedule(schedule);
				logger.info("schedule register success for log backup");
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
			logger.info("schedule unregister success for log backup");
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
		try {
			backupStarted();
			//설정에 따라 로그 데이터 파일을 임시 디렉토리에 복사함
			if(!copyLogFilesToBackupDir()) {
				return;
			}
			//임시 디렉토리를 압축함.			
			String fileName = compressFiles();
			if(fileName == null) {	//압축  실패			
				return;
			} else {
				removeBackupedFiles();				//압축 성공. 백업된 파일들을 실제 디렉토리에서 삭제
			}
			//(s)ftp를 사용하면 압축 파일을 전송함.
			if(logSetting.getFtpUse()) {
				FtpCode code = uploadFile(fileName);
				//결과 db에 업데이트
				insertDbBackupInfo(fileName, code.getCode());
				if(code == FtpCode.Success) {		//ftp로 파일전송이 완료되었으면 압축 파일을 삭제한다.
					File file = new File(fileName);
					Files.move(file, new File(PRESERVE_DIR + file.getName())); //전송된 파일은 디렉토리를 옮겨 7일 정도 보관되게 한다.
				}				
			} else {				//ftp를 사용하지 않을 경우 장비에 압축파일을 그대로 보관한다.
				//결과 db에 업데이트
				insertDbBackupInfo(fileName, BackupCode.LocalSuccess.getCode());
			}
		} catch (Exception e) {
			logger.error("error occured during backup.", e);
		} finally {
			backupFinished();
		}
   }
	
	private void backupStarted() throws IOException, ParseException {
		logger.info("backup started");
		
		oldestDate = new Date();		//current date.
		latestDate = new Date(0);		//old date. 1970-01-01.		
		backupFileList = new ArrayList<File>();
		
		removeRollbackedFiles();
	}
	
	private void backupFinished() {
		//업로드가 안 된 백업파일들을 다시 전송한다.
		uploadUntrasferredCompFiles();
		
		oldestDate = null;
		latestDate = null;
		backupFileList = null;
		
		removeTempDirForBackup();
		
		logger.info("backup finished");
	}
	
	//remove rollbacked files to prevent duplicated backup
	private void removeRollbackedFiles() throws IOException, ParseException {
		List<String> fileNames = xenicsService.fetchFtpBackupFileNames(BackupType.Log.getType(), BackupCode.Rollbacked.getCode());
		if(fileNames.size() == 0)
			return;
		
		for(String fileName : fileNames) {
			removeRollbackedFiles(fileName);
		}
	}
	
	@Override
	public void removeRollbackedFiles(String fileName) throws ParseException, IOException {
		//parse file name. ex) 20150111-20150222.tar.gz
		String[] arr = fileName.split("\\.");
		String[] dates = arr[0].split("-");
		SimpleDateFormat format = new SimpleDateFormat("yyyyMMdd");
		Date first = format.parse(dates[0]);
		Date last = format.parse(dates[1]);
		
		subDirListRemoveRollbackedFiles(LOG_STORAGE_DIR, first, last);
		// /utm/log/sslplus/backup/comp 폴더에 파일이 있으면 ftp 전송이 아직 안된 것으로 상태를  LocalSuccess로 변경함
		if(new File(COMPRESS_LOG_DIR+fileName).exists())
			xenicsService.updateBackupState(fileName, BackupCode.LocalSuccess.getCode());
		else	//ftp 전송이 된 것임
			xenicsService.updateBackupState(fileName, BackupCode.Success.getCode());
	}
	
	private void subDirListRemoveRollbackedFiles(String source, Date first, Date last) throws IOException {
		File dir = new File(source); 
		File[] fileList = dir.listFiles();
		
		for(int i = 0 ; i < fileList.length ; i++) {
			File file = fileList[i];
			
			if(file.isFile()) {
				if(isRollBackedFile(file, first, last)) {
					file.delete();
					logger.info("remove rollbacked file [{}] ", file.getAbsolutePath());
				}
			}	else if(file.isDirectory()) {				
				subDirListRemoveRollbackedFiles(file.getCanonicalPath().toString(), first, last); 
			}
		}
	}
	
	private boolean isRollBackedFile(File file, Date first, Date last) {
		String name = file.getName();
		String[] str = name.split("\\.");
		
		SimpleDateFormat format = new SimpleDateFormat(LOG_FILE_DATE_FORMAT);
		Date fileDate = null;
		try {
	      fileDate = format.parse(str[0]);	      
      } catch (ParseException e) {
	      logger.error("file name parsing error", e);
	      return false;
      }
		
		if((first.before(fileDate) || first.equals(fileDate)) && 
				(last.after(fileDate) || last.equals(fileDate))) {
			return true;
		}
		return false;
	}
	
	private void removeTempDirForBackup() {
		try{
			String command = "rm -rf " + TEMP_LOG_BACKUP_DIR;
			Process p = Runtime.getRuntime().exec(command);
			p.waitFor();
			
			logger.info("temporary directory for backup removed");
		} catch(Exception e) {
			logger.error("remove backup dir failed", e);
		}
	}
	
	private void removeBackupedFiles() {
		for(File file : backupFileList) {
			file.delete();
		}
		logger.info("backuped files are removed");
	}
	
	
	private boolean copyLogFilesToBackupDir() {
		try {			
			subDirList(LOG_STORAGE_DIR);
      } catch (IOException e) {
	      logger.error("move files to backup dir failed", e);
	      return false;
      }
		
		if(backupFileList.size() == 0)
			return false;
		
		return true;
	}
	
	private void subDirList(String source) throws IOException {
		File dir = new File(source); 
		File[] fileList = dir.listFiles();
		
		for(int i = 0 ; i < fileList.length ; i++){
			File file = fileList[i];
			
			if(file.isFile()) {					
				if(isExceededDateFile(file)) {
					copyToBackupDir(file);
					backupFileList.add(file);
					logger.info("file [{}] copied for backup", file.getAbsolutePath());
				}
			}	else if(file.isDirectory()) {
				subDirList(file.getCanonicalPath().toString()); 
			}
		}
		return;
	}
	
	private void findLatestOldestDate(Date date) {
		if(latestDate.before(date)) {
			latestDate = date;
		}
		
		if(oldestDate.after(date)) {
			oldestDate = date;
		}
	}
	
	private boolean isExceededDateFile(File file) {
		String name = file.getName();
		String[] str = name.split("\\.");
		
		Date now = new Date();
		SimpleDateFormat format = new SimpleDateFormat(LOG_FILE_DATE_FORMAT);
		Date fileDate = null;
		try {
	      fileDate = format.parse(str[0]);	      
      } catch (ParseException e) {
	      logger.error("file name parsing error", e);
	      return false;
      }
		
		long diff = now.getTime() - fileDate.getTime();
		long diffDays = diff / A_DAY;
		
		if(diffDays > logSetting.getBackupMaintainDays()) {
			findLatestOldestDate(fileDate);
			return true;
		}
		return false;
	}
	
	private void copyToBackupDir(File file) throws IOException {
		String path = file.getAbsolutePath();
		String path2 = path.substring(LOG_STORAGE_DIR.length());
		String backupPath =  TEMP_LOG_BACKUP_DIR + path2;
		File backupFile = new File(backupPath);
		File parent = backupFile.getParentFile();
		if(!parent.exists())
			parent.mkdirs();
		
		Files.copy(file, backupFile);
	}
	
	private String compressFiles() {
		SimpleDateFormat format = new SimpleDateFormat("yyyyMMdd");
		String latest = format.format(latestDate);
		String oldest = format.format(oldestDate);
		String fileName = TEMP_LOG_BACKUP_DIR + oldest + "-" + latest + ".tar.gz";
		
		try{
			String command = "tar czf " + fileName + " " + TEMP_LOG_BACKUP_DIR;
			Process p = Runtime.getRuntime().exec(command);
			p.waitFor();
			
			File file = new File(fileName);
			if(file.exists()) {
				command = "mv " + fileName + " " + COMPRESS_LOG_DIR;
				p = Runtime.getRuntime().exec(command);
				p.waitFor();
				
				logger.info("backup file compression successed");
				
				fileName = COMPRESS_LOG_DIR + file.getName();
				return fileName;
			}
		} catch(Exception e) {
			logger.error("compress error", e);
			return null;
		}
		return null;		
	}
	
	private void uploadUntrasferredCompFiles() {
		if(!logSetting.getFtpUse()) 
			return;
		
		File dir = new File(COMPRESS_LOG_DIR); 
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
	
	private FtpCode uploadFile(String fileName) {
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
		xenicsService.insertBackupInfo(BackupType.Log.getType(), file.getName(), getFileSize(file), stateCode);
	}
	
	private void updateDbBackupState(String fileName, int stateCode) {
		xenicsService.updateBackupState(fileName, stateCode);
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
}
