package kr.co.future.sslvpn.core.backup;

public interface Constants {
	public static final String LOG_STORAGE_DIR = System.getProperty("kraken.data.dir") + "/kraken-logstorage/log/";
	
	public static final String BACKUP_DIR = "/utm/log/sslplus/backup/";				//백업 디렉토리
	
	public static final String COMPRESS_LOG_DIR = BACKUP_DIR + "comp/log/";			//ftp로 전송되지 않은 백업 파일을 보관하는 디렉터리
	
	public static final String COMPRESS_POLICY_DIR = BACKUP_DIR + "comp/policy/";	//ftp로 전송되지 않은 정책 백업 파일을 보관하는 디렉터리
	
	public static final String PRESERVE_DIR = BACKUP_DIR + "preserve/";				//업로드한 백업 파일 또는 롤백하기 위해 다운로드한 파일을 7일간 보관하는 디렉터리
	
	public static final String TEMP_DIR = BACKUP_DIR + "temp/";
	
	public static final String TEMP_LOG_BACKUP_DIR = TEMP_DIR + "log/";			//백업할 로그 파일을 임시적으로 복사해놓기 위한 디렉터리
	
	public static final String TEMP_LOG_ROLLBACK_DIR = TEMP_DIR + "rollback/log/";	//롤백할 로그 파일을 모아두는 임시 디렉터리
	
	public static final String TEMP_POLICY_ROLLBACK_DIR = TEMP_DIR + "rollback/policy/";	//롤백할 로그 파일을 모아두는 임시 디렉터리
	
	public static final String TEMP_DOWNLOAD_DIR = TEMP_DIR + "download/";
	
	public static final String TEMP_UPLOAD_DIR = TEMP_DIR + "upload/";
	
	public static final long A_DAY = 24 * 60 * 60 * 1000;
	
	public static final String[] BYTE_UNIT = {"Bytes", "KB", "MB", "GB", "TB"};
	
	public static final String LOG_FILE_DATE_FORMAT = "yyyy-MM-dd";
	
	public static final String SIMPLE_DATE_FORMAT = "yyyyMMdd_HHmmss";
	
	public static final String PREFIX_LOG_FILES = "logs";
	
	public static final String PREFIX_POLICY_FILES = "policies";
}
