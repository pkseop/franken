package kr.co.future.sslvpn.core.backup.msgbus;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import kr.co.future.sslvpn.model.LogSetting;
import kr.co.future.sslvpn.model.api.LogSettingApi;
import kr.co.future.sslvpn.xtmconf.XtmConfig;
import kr.co.future.sslvpn.xtmconf.system.Restore;
import kr.co.future.sslvpn.xtmconf.system.SettingOption;

import org.apache.commons.codec.binary.Base64;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Requires;
import org.apache.felix.ipojo.annotations.Validate;

import kr.co.future.confdb.ConfigService;
import kr.co.future.cron.CronService;
import kr.co.future.logstorage.Log;
import kr.co.future.logstorage.LogStorage;
import kr.co.future.msgbus.MsgbusException;
import kr.co.future.msgbus.Request;
import kr.co.future.msgbus.Response;
import kr.co.future.msgbus.Session;
import kr.co.future.msgbus.handler.MsgbusMethod;
import kr.co.future.msgbus.handler.MsgbusPlugin;
import kr.co.future.sslvpn.core.backup.BackupCode;
import kr.co.future.sslvpn.core.backup.BackupType;
import kr.co.future.sslvpn.core.backup.Constants;
import kr.co.future.sslvpn.core.backup.LogBackup;
import kr.co.future.sslvpn.core.ftp.FtpCode;
import kr.co.future.sslvpn.core.ftp.FtpService;
import kr.co.future.sslvpn.core.ftp.SftpService;
import kr.co.future.sslvpn.core.xenics.XenicsService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(name = "backup-plugin")
@MsgbusPlugin
public class BackupPlugin implements Constants{
	private final Logger logger = LoggerFactory.getLogger(BackupPlugin.class);
	
	@Requires
	private XenicsService xenicsService;
	
	@Requires
	private LogSettingApi logSettingApi;
	
	@Requires
	private LogBackup logBackup;
	
	@Requires
	private ConfigService conf;
	
	@Requires
	private CronService cron;
	
	@Requires
	private LogStorage logStorage;
	
	@Validate
	public void start() {
		
	}
	
	@MsgbusMethod
	public void getBackupRecords(Request req, Response resp) {
		Integer offset = req.getInteger("offset");
		Integer limit = req.getInteger("limit");
		Integer type = req.getInteger("type");
		
		int total = xenicsService.getTotalNumOfFtpBackupRecords(type);
		
		if(offset == null)
			offset = 0;
		if(limit == null || limit > total)
			limit = total;
		
		LogSetting logSetting = logSettingApi.getLogSetting();
		
		resp.put("records", xenicsService.getFtpBackupRecords(type, limit, offset));
		resp.put("total", total);
		resp.put("ftp_use", logSetting.getFtpUse());
	}
	
	@MsgbusMethod
	public void rollbackLogs(Request req, Response resp) {
		@SuppressWarnings("unchecked")
		List<String> fileNames = (List<String>) req.get("file_names");
		
		LogSetting logSetting = logSettingApi.getLogSetting();
		
		if(logSetting.getFtpUse()) {
			List<String> downloadFiles = new ArrayList<String>();
			for(String fileName : fileNames) {
				File preserveDirFile = new File(PRESERVE_DIR+fileName);
				File compDirFile = new File(COMPRESS_LOG_DIR+fileName);
				if(!preserveDirFile.exists() && !compDirFile.exists()) {
					downloadFiles.add(fileName);
				}
			}
			if(downloadFiles.size() > 0)
				downloadFiles(downloadFiles, logSetting);
		}
		rollbackLog(fileNames);
	}
	
	private void downloadFiles(List<String> fileNames, LogSetting logSetting) {
		FtpService ftp =null;
		if(logSetting.getFtpType()) {
			ftp = new SftpService(logSetting.getFtpIp(), logSetting.getFtpId(), logSetting.getFtpPw(), logSetting.getFtpPath());			
		} else {
			ftp = new FtpService(logSetting.getFtpIp(), logSetting.getFtpId(), logSetting.getFtpPw(), logSetting.getFtpPath(), true);
		}
		FtpCode code = ftp.downloadFiles(fileNames, PRESERVE_DIR);
		if(code != FtpCode.Success) {
			logger.error("download files for rollback failed");
			throw new MsgbusException("frodo", "rollback-failed");
		}
	}
	
	private void rollbackLog(List<String> fileNames) {
		File dir = new File(TEMP_LOG_ROLLBACK_DIR);
		if(!dir.exists())
			dir.mkdirs();
		
		for(String fileName : fileNames) {
			File preserveDirFile = new File(PRESERVE_DIR+fileName);
			File compDirFile = new File(COMPRESS_LOG_DIR+fileName);
			
			File file = null;
			if(preserveDirFile.exists())
				file = preserveDirFile;
			else if(compDirFile.exists())
				file = compDirFile;
			
			uncompFile(file.getAbsolutePath(), TEMP_LOG_ROLLBACK_DIR);
		}
		moveRollbackFiles();
		
		//update state.
		for(String fileName : fileNames) {
			xenicsService.updateBackupState(fileName, BackupCode.Rollbacked.getCode());
		}
	}
	
	private void uncompFile(String fileName, String dest) {
		try{
			String command = "tar xzf " + fileName + " -C " + dest;
			Process p = Runtime.getRuntime().exec(command);
			p.waitFor();
		} catch(Exception e) {
			logger.error("uncompress error", e);
			throw new MsgbusException("frodo", "rollback-failed");
		}
	}
	
	private void moveRollbackFiles() {
		String tempDir = TEMP_LOG_ROLLBACK_DIR + TEMP_LOG_BACKUP_DIR;
		tempDir = tempDir.replace("//", "/");	// directory /utm/log/sslplus/backup/rollback/utm/log/sslplus/backup/log/
		try{
			moveRollbackLogFiles(tempDir, LOG_STORAGE_DIR);
			
			File file = new File(TEMP_LOG_ROLLBACK_DIR);
			delete(file);
		} catch(Exception e) {
			logger.error("move files error", e);
			throw new MsgbusException("frodo", "rollback-failed");
		}
	}
	
	void moveRollbackLogFiles(String source, String dest) {
		File[] dirs = new File(source).listFiles();
		for(File dir : dirs) {
			File[] files = dir.listFiles();
			for(File file : files) {
				String path = file.getAbsolutePath();
				path = path.replace(source, dest);
				file.renameTo(new File(path));
			}
		}
	}
	
	void delete(File f) throws IOException {
		if (f.isDirectory()) {
			for (File c : f.listFiles())
				delete(c);
		}
		if (!f.delete())
			throw new FileNotFoundException("Failed to delete file: " + f);
	}
	
	@MsgbusMethod
	public void ftpUpload(Request req, Response resp) {
		Integer type = (Integer)req.get("type");
		String dir = null;
		if(type == BackupType.Log.getType())
			dir = COMPRESS_LOG_DIR;
		else if(type == BackupType.Policy.getType())
			dir = COMPRESS_POLICY_DIR;
		
		@SuppressWarnings("unchecked")
		List<String> fileNames = (List<String>) req.get("file_names");
		List<String> filePaths = new ArrayList<String>();
		for(String fileName : fileNames) {
			File untransferredFile = new File(dir+fileName);
			if(untransferredFile.exists() == false) {
				xenicsService.updateBackupState(fileName, BackupCode.UploadFileNotFound.getCode());
				throw new MsgbusException("frodo", "ftp-failed");
			}
			filePaths.add(untransferredFile.getAbsolutePath());
		}
		
		uploadFiles(filePaths, logSettingApi.getLogSetting());
		
		//update state.
		for(String fileName : fileNames) {
			xenicsService.updateBackupState(fileName, BackupCode.Success.getCode());
			File compDirFile = new File(dir, fileName);
			if(compDirFile.isFile()) {
				String cmd = "mv " + compDirFile.getAbsolutePath() + " " + PRESERVE_DIR;
				execCmd(cmd);
			}
		}
	}
	
	private void uploadFiles(List<String> fileNames, LogSetting logSetting) {
		FtpService ftp =null;
		if(logSetting.getFtpType()) {
			ftp = new SftpService(logSetting.getFtpIp(), logSetting.getFtpId(), logSetting.getFtpPw(), logSetting.getFtpPath());			
		} else {
			ftp = new FtpService(logSetting.getFtpIp(), logSetting.getFtpId(), logSetting.getFtpPw(), logSetting.getFtpPath(), true);
		}
		FtpCode code = ftp.uploadFiles(fileNames);
		if(code != FtpCode.Success) {
			logger.error("upload files to ftp server failed");
			throw new MsgbusException("frodo", "ftp-failed");
		}
	}
	
	@MsgbusMethod
	public void removeRollbackedBackup(Request req, Response resp) {
		@SuppressWarnings("unchecked")
		List<String> fileNames = (List<String>) req.get("file_names");
		
		for(String fileName : fileNames) {
			try {
				logBackup.removeRollbackedFiles(fileName);
			} catch (ParseException e) {
				logger.error("parsing error occurred", e);
				throw new MsgbusException("frodo", "remove-failed");
			} catch (IOException e) {
				logger.error("i/o error occurred", e);
				throw new MsgbusException("frodo", "remove-failed");
			}
	
		}
	}
	
	@MsgbusMethod
	public void restorePolicy(Request req, Response resp) {
		logger.info("policy rollback started");
		
		String password = req.getString("password");
		String fileName = req.getString("file_name");
		
		File file = getRestorePolicyFile(fileName);
		if(file == null)
			throw new MsgbusException("frodo", "rollback-failed");
		
		restorePolicy(fileName, password, file);
	}
	
	private File getRestorePolicyFile(String fileName) {
		File dir = new File(TEMP_POLICY_ROLLBACK_DIR);
		if(!dir.exists())
			dir.mkdirs();
		
		File file = new File(PRESERVE_DIR+fileName);
		if(file.exists()) {
			uncompFile(file.getAbsolutePath(), TEMP_POLICY_ROLLBACK_DIR);
			return getPolicyBackupFile();
		}
		
		file = new File(COMPRESS_POLICY_DIR+fileName);
		if(file.exists()) {
			uncompFile(file.getAbsolutePath(), TEMP_POLICY_ROLLBACK_DIR);
			return getPolicyBackupFile();
		}
		
		LogSetting logSetting = logSettingApi.getLogSetting();
		if(logSetting.getFtpUse()) {
			downloadForPolicyRollback(logSetting, fileName);
			uncompFile(PRESERVE_DIR+fileName, TEMP_POLICY_ROLLBACK_DIR);
			return getPolicyBackupFile();
		}
		return null;
	}
	
	private void downloadForPolicyRollback(LogSetting logSetting, String fileName) {
		FtpService ftp =null;
		if(logSetting.getFtpType()) {
			ftp = new SftpService(logSetting.getFtpIp(), logSetting.getFtpId(), logSetting.getFtpPw(), logSetting.getFtpPath());			
		} else {
			ftp = new FtpService(logSetting.getFtpIp(), logSetting.getFtpId(), logSetting.getFtpPw(), logSetting.getFtpPath(), true);
		}
		FtpCode code = ftp.downloadFile(fileName, PRESERVE_DIR);
		if(code != FtpCode.Success) {
			logger.error("download file from ftp server failed");
			throw new MsgbusException("frodo", "rollback-failed");
		} else {
			logger.info("file [{}] downloaded for rollback", fileName);
		}
	}
	
	private File getPolicyBackupFile() {
		File[] list = new File(TEMP_POLICY_ROLLBACK_DIR+"utm/log/policy_back/").listFiles();
		if(list.length != 1) {
			throw new MsgbusException("frodo", "rollback-failed");
		}
		return list[0];
	}
	
	private void restorePolicy(String fileName, String password, File file) {
		try {
			Restore.restore(conf, password, file);
			delete(new File(TEMP_POLICY_ROLLBACK_DIR));
			rollbackPolicyUpdate(fileName);
			logger.info("policy data restored");
			reboot();
		} catch (IOException e) {
			throw new MsgbusException("frodo", e.getMessage());
		}
	}
	
	private void rollbackPolicyUpdate(String fileName) {
		File file = new File(COMPRESS_POLICY_DIR+fileName);
		int stateCode = BackupCode.Success.getCode();
		if(file.exists())
			stateCode = BackupCode.LocalSuccess.getCode();
		
		xenicsService.updatePrevPolicyRollbackState(stateCode);
		xenicsService.updateBackupState(fileName, BackupCode.Rollbacked.getCode());
	}
	
	//AccessGatewayPlugin.java 의 restoreConfig 함수의 소스를 가져옴.
	private void reboot() throws IOException {
		try {
			List<kr.co.future.sslvpn.xtmconf.manage.LogSetting> settings = XtmConfig.readConfig(kr.co.future.sslvpn.xtmconf.manage.LogSetting.class);
			boolean writeLog = false;
			for (kr.co.future.sslvpn.xtmconf.manage.LogSetting setting : settings) {
				if (setting.getType() == kr.co.future.sslvpn.xtmconf.manage.LogSetting.Type.Setting) {
					if (setting.getSystem().ordinal() <= 6 && setting.getSystem().ordinal() != 0)
						writeLog = true;
				}
			}

			if (writeLog) {
				Map<String, Object> data = new HashMap<String, Object>();
				for (String key : new String[] { "nat_sip", "sip", "nat_dip", "dip" })
					data.put(key, "0.0.0.0");
				for (String key : new String[] { "dport", "nat_dport", "sport", "nat_sport", "rule" })
					data.put(key, 0);
				for (String key : new String[] { "dpi_group", "usage", "user" })
					data.put(key, null);
				for (String key : new String[] { "oip", "iface" })
					data.put(key, "");
				data.put("note", "SYSTEM reboot");
				data.put("type", "xtm");
				data.put("proto", "ip");
				SettingOption option = XtmConfig.readConfig(SettingOption.class).get(0);
				data.put("prod", option.getName());
				data.put("category", "system");
				data.put("logtype", 0x11060009);
				data.put("level", 6);
				logStorage.write(new Log("xtm", new Date(), data));
			}
			logStorage.flush();

			try {
				Thread.sleep(3000);
			} catch (InterruptedException e) {
			}

			Runtime.getRuntime().exec("reboot");
		} catch (IOException e) {
			logger.error("frodo core: reboot failed", e);
		}
	}
	
	@MsgbusMethod
	public void fileDownload(Request req, Response resp) {
		Integer type = req.getInteger("type");		//0-log, 1-policy
		@SuppressWarnings("unchecked")
		List<String> fileNames = (List<String>) req.get("file_names");
		
		String tempDir = null;
		try {
			String fileName = collectDownloadLogFiles(type, fileNames);
			tempDir = TEMP_DOWNLOAD_DIR;
			
			File src = new File(tempDir + fileName);
			File dest = new File(XtmConfig.UTM_LOG + "policy_back/" + fileName);
			if (!src.renameTo(dest)) {
				throw new RuntimeException("mv error: " + src.getAbsolutePath() + "->" + dest.getAbsolutePath());
			}
			
			resp.put("file_path", fileName);
	
			logger.trace("backup file download completed");
		} finally {
			if(tempDir != null) {
				removeCmd(tempDir);
			}
		}
	}
	
	private void execCmd(String cmd) {
		try{
			Process p = Runtime.getRuntime().exec(cmd);
			p.waitFor();
			
			logger.debug("cmd [{}] executed", cmd);
		} catch(Exception e) {
			logger.error("error occurred execute command", e);
			throw new MsgbusException("frodo", "exec-cmd-failed");
		}
	}
	
	private void removeCmd(String path) {
		String cmd = "rm -rf " + path;
		execCmd(cmd);
	}
	
	private void compFiles(String dir, String path) {
		StringBuilder sb = new StringBuilder();
		sb.append("tar czf ").append(path).append(" -C ").append(dir);
		
		File[] files = new File(dir).listFiles();
		for(File file : files) {
			sb.append(" ").append(file.getName());
		}
		execCmd(sb.toString());
	}
	
	private void copyFileToDir(File file, String dir) {
		String command = "cp " + file.getAbsolutePath() + " " + dir;
		execCmd(command);
	}
	
	private String collectDownloadLogFiles(Integer type, List<String> fileNames) {
		String localBackupDir = null, prefix = null;
		if(type == BackupType.Log.getType()) {
			localBackupDir = COMPRESS_LOG_DIR;
			prefix = PREFIX_LOG_FILES;
		} else if(type == BackupType.Policy.getType()) {
			localBackupDir = COMPRESS_POLICY_DIR;
			prefix = PREFIX_POLICY_FILES;
		}
		
		//download backup files from (s)ftp server if the file not exists in local
		LogSetting logSetting = logSettingApi.getLogSetting();
		if(logSetting.getFtpUse()) {
			List<String> downloadFiles = new ArrayList<String>();
			for(String fileName : fileNames) {
				File preserveDirFile = new File(PRESERVE_DIR+fileName);
				File compDirFile = new File(localBackupDir+fileName);
				if(!preserveDirFile.exists() && !compDirFile.exists()) {
					downloadFiles.add(fileName);
				}
			}
			if(downloadFiles.size() > 0)
				downloadFiles(downloadFiles, logSetting);
		}
		
		File downloadDir = new File(TEMP_DOWNLOAD_DIR);
		if(!downloadDir.exists())
			downloadDir.mkdirs();
			
		for(String fileName : fileNames) {
			File compDirFile = new File(localBackupDir+fileName);
			File preserveDirFile = new File(PRESERVE_DIR+fileName);
			
			if(compDirFile.isFile()) {
				copyFileToDir(compDirFile, TEMP_DOWNLOAD_DIR);
			} else if(preserveDirFile.isFile()) {
				copyFileToDir(preserveDirFile, TEMP_DOWNLOAD_DIR);
			} else {
				throw new MsgbusException("frodo", "file-does-not-exist");
			}
		}
		
		String downloadFileName = null;
		if(fileNames.size() > 1) {
			SimpleDateFormat formatter = new SimpleDateFormat(SIMPLE_DATE_FORMAT, Locale.KOREA);
			Date date = new Date();
			downloadFileName = prefix + "_" + formatter.format(date) + ".tar.gz";
			compFiles(TEMP_DOWNLOAD_DIR, TEMP_DOWNLOAD_DIR+downloadFileName);
		} else {
			downloadFileName = fileNames.get(0);
		}
		return downloadFileName;
	}

	@SuppressWarnings("unchecked")
	@MsgbusMethod
	public void prepareUpload(Request req, Response resp) throws IOException {
		String fileName = req.getString("filename");
		int fileSize = req.getInteger("filesize");

		Session session = req.getSession();
		Map<String, Object> m = (Map<String, Object>) session.get("upload");
		if (m == null) {
			m = new ConcurrentHashMap<String, Object>();
			session.setProperty("upload", m);
		}
		
		File uploadFile = new File(TEMP_UPLOAD_DIR, fileName);
		uploadFile.getParentFile().mkdirs();

		m.put("data", new UploadMetadata(fileName, fileSize, uploadFile));
	}
	
	private static class UploadMetadata {
		public String fileName;
		public int fileSize;
		public File temp;

		public UploadMetadata(String fileName, int fileSize, File temp) {
			this.fileName = fileName;
			this.fileSize = fileSize;
			this.temp = temp;
		}
	}
	
	private final static String UPLOAD_FILE_KEY = "upload-backup-file";
	
	@MsgbusMethod
	public void uploadPart(Request req, Response resp) throws IOException {
		String data = req.getString("data");
		String flag = req.getString("flag");
		Session session = req.getSession();
		byte[] b = Base64.decodeBase64(data);

		logger.trace("frodo core: flag=[{}]", flag);

		UploadMetadata meta = getUploadMetadata(req);

		FileOutputStream os = (FileOutputStream) session.get(UPLOAD_FILE_KEY);
		if (os == null) {
			os = new FileOutputStream(meta.temp, true);
			session.setProperty(UPLOAD_FILE_KEY, os);
			logger.trace("frodo core: start upload upgrade file");
		}

		if (flag.equals("upload")) {
			os.write(b);
		} else { // flag == close
			if (os != null) {
				try {
					os.close();
					session.unsetProperty(UPLOAD_FILE_KEY);
					logger.trace("frodo core: upgrade file stream is closed");
				} catch (IOException e) {
				}
			}
			updateWithUploadedFile(meta.temp);
		}
	}
	
	@SuppressWarnings("unchecked")
	private UploadMetadata getUploadMetadata(Request req) {
		Session session = req.getSession();

		Map<String, Object> m = (Map<String, Object>) session.get("upload");
		if (m == null)
			throw new MsgbusException("frodo", "upload-data-not-found");

		UploadMetadata meta = (UploadMetadata) m.get("data");
		if (meta == null)
			throw new MsgbusException("frodo", "upload-data-not-found");
		return meta;
	}
	
	private void updateWithUploadedFile(File file) {
		try{
			String fileName = file.getName();
			if(fileName.startsWith(PREFIX_LOG_FILES) || fileName.startsWith(PREFIX_POLICY_FILES)) {
				String path = file.getAbsolutePath();
				String cmd = "tar xzf " + path + " -C " + TEMP_UPLOAD_DIR;
				execCmd(cmd);
				cmd = "rm " + path;
				execCmd(cmd);
			}
			
			File dir = new File(TEMP_UPLOAD_DIR);
			File[] files = dir.listFiles();
			for(File temp : files) {
				if(temp.getName().contains(".gat")) {			//정책 백업파일.
					updateUploadedPolcyFile(temp);
				} else {
					updateUploadedLogFile(temp);
				}
			}
		} finally {
			execCmd("rm -rf " + TEMP_UPLOAD_DIR);
		}
	}
	
	private void updateUploadedLogFile(File file) {
		String fileName = file.getName();
		checkUploadedLogFileName(fileName);
		int stateCode = xenicsService.retrieveFtpBackupStateCode(fileName);
		if(stateCode == -1)	//정보가 없을 경우.
			xenicsService.insertBackupInfo(BackupType.Log.getType(), fileName, getFileSize(file), BackupCode.LocalSuccess.getCode());
		if(stateCode == BackupCode.Success.getCode()) {    //ftp 전송 성공한 파일일 경우.
			String cmd = "mv " + file.getAbsolutePath() + " " + PRESERVE_DIR;
			execCmd(cmd);
		} else {
			File compDirFile = new File(COMPRESS_LOG_DIR, fileName);
			if(compDirFile.isFile() == false) {
				String cmd = "mv " + file.getAbsolutePath() + " " + COMPRESS_LOG_DIR;
				execCmd(cmd);
			}
		}
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
	
	private void checkUploadedLogFileName(String fileName) {
		fileName = fileName.substring(0, fileName.indexOf("."));
		
		String[] strDates = fileName.split("-");
		SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMdd");
        try {
            Date date1 = formatter.parse(strDates[0]);
            Date date2 = formatter.parse(strDates[1]);
            if(date2.before(date1)) {
            	logger.error("file name incorrect. the rear date is before the front date.");
            	throw new MsgbusException("frodo", "upload-file-failed");
            }
        } catch(Exception e) {
        	logger.error("error occurred while checking log backup file name", e);
        	throw new MsgbusException("frodo", "upload-file-failed");
        }
	}
	
	private void updateUploadedPolcyFile(File file) {
		String fileName = file.getName();
		checkUploadedPolicyFileName(fileName);
		int stateCode = xenicsService.retrieveFtpBackupStateCode(fileName);
		if(stateCode == -1)	//정보가 없을 경우.
			xenicsService.insertBackupInfo(BackupType.Policy.getType(), file.getName(), getFileSize(file), BackupCode.LocalSuccess.getCode());
		if(stateCode == BackupCode.Success.getCode()) {    //ftp 전송 성공한 파일일 경우.
			String cmd = "mv " + file.getAbsolutePath() + " " + PRESERVE_DIR;
			execCmd(cmd);
		} else {
			File compDirFile = new File(COMPRESS_POLICY_DIR, fileName);
			if(compDirFile.isFile() == false) {
				String cmd = "mv " + file.getAbsolutePath() + " " + COMPRESS_POLICY_DIR;
				execCmd(cmd);
			}
		}
	}
	
	private void checkUploadedPolicyFileName(String fileName) {
		SimpleDateFormat formatter = new SimpleDateFormat(SIMPLE_DATE_FORMAT);
		try {
			formatter.parse(fileName);
		} catch(Exception e) {
			logger.error("error occurred while checking policy backup file name", e);
			throw new MsgbusException("frodo", "upload-file-failed");
		}
	}
}
