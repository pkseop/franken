package kr.co.future.sslvpn.kctech.backup;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;



import kr.co.future.sslvpn.model.api.LogSettingApi;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Invalidate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.apache.felix.ipojo.annotations.ServiceProperty;
import org.apache.felix.ipojo.annotations.Validate;
import kr.co.future.confdb.ConfigService;
import kr.co.future.cron.CronService;
import kr.co.future.cron.Schedule;
import kr.co.future.logdb.LogQuery;
import kr.co.future.logdb.LogQueryCallback;
import kr.co.future.logdb.LogQueryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import au.com.bytecode.opencsv.CSVWriter;


@Component(name = "log-backup-csv")
@Provides
public class CSVLogBackupImpl implements Runnable{
	
	private Logger logger = LoggerFactory.getLogger(CSVLogBackupImpl.class);
	
	private static final String CSV_LOG_DIR = "/utm/log/kraken/home/root/daily_backup/";			//csv 로그를 저장하는 디렉토리
	
	@ServiceProperty(name = "instance.name")
	private String instanceName;
	
	@Requires
	private CronService cronService;
	
	@Requires
	private LogSettingApi logSettingApi;
	
	@Requires
	private ConfigService conf;
	
	@Requires
	private LogQueryService service;
	
	private String backupDate;
	
	
	@Validate
	public void start() {
		registerSchedule();
		
		//create folder for backup file.
		File dir = new File(CSV_LOG_DIR);
		if(!dir.exists())
			dir.mkdirs();

	}
	
	@Invalidate
	public void stop() {
		unregisterSchedule();
	}

   public void registerSchedule() {
		try {
			Schedule schedule = new Schedule.Builder(instanceName).build("5 0 * * *");
			cronService.registerSchedule(schedule);
			logger.info("schedule register success for csv log backup");
		} catch (Exception e) {
			logger.error("log backup: cron register failed", e);
		}
   }

   public void unregisterSchedule() {
		Integer cronJobId = getCronJobId();
		if (cronJobId != null) {
			cronService.unregisterSchedule(cronJobId);
			logger.info("schedule unregister success for csv log backup");
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
		Date yesterday = new Date();
		yesterday.setTime(yesterday.getTime() - ((long) 1000 * 60 * 60 * 24 ));
		SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
		backupDate = sdf.format(yesterday);
		logger.info("backupdate is {}", backupDate);
		deleteOldLog();
		startBackup();
   }
	
	/**
	 * log 디렉토리에서 90일이 지난 파일들은 삭제 처리한다.
	 */
	private void deleteOldLog(){
		File[] listFile = new File(CSV_LOG_DIR).listFiles(); 
		try{
			if(listFile.length > 0){
				String fileName = "";
				String strFileDate = "";
				for(int i = 0 ; i < listFile.length ; i++){
					if(listFile[i].isFile()){
						logger.info("{} file detected.", listFile[i].getName());
						fileName = listFile[i].getName();
						strFileDate = fileName.split("-")[1].split("\\.")[0];
						if (dateDiff(strFileDate) > 90){
							listFile[i].delete();
							logger.info("{} file deleted.", listFile[i].getName());
						}
					}
				}
			}
		}catch(Exception e){
			logger.error("delete file fail.", e);
		}
	}
	
	/**
	 * yyyyMMdd로 받은 문자열과 오늘 날자와의 일수 차이를 계산해서 돌려준다.
	 * @param fileDate
	 * @return int diff day
	 * @throws ParseException
	 */
	private int dateDiff(String fileDate) throws ParseException{
		SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
		Date date = sdf.parse(fileDate);
		return Math.round(((new Date()).getTime() - date.getTime()) / (long) 1000 * 60 * 60 * 24);
	}
	
	private void startBackup(){
		logger.info("csv log backup started");
		// csv 로그파일 저장
		saveAuthLog();
		saveAccessLog();
		saveFlowLog();
		saveTunnelCountLog();
	}
	
	private void saveAuthLog(){
		String query = "table from=#date000000 to=#date235959 ssl-auth | fields _time, type, login, remote_ip, profile, tunnel, os_type, device_key, code, auth_code_desc | lookup sslplus code output auth_code_desc";
		query = query.replaceAll("#date", backupDate);
		logger.info(query);
		executeQuery(query, "auth");
		
		logger.info("saveAuthLog backup end");
	}

	private void saveAccessLog(){
		String query = "table from=#date000000 to=#date235959 ssl-access | fields _time, login, tunnel, session, client_ip, client_port, server_ip, server_port, protocol, action";
		query = query.replaceAll("#date", backupDate);
		logger.info(query);
		executeQuery(query, "access");
		
		logger.info("saveAccessLog backup end");
	}
	
	private void saveFlowLog(){
		String query = "table from=#date000000 to=#date235959 ssl-flow | replace login is null \"알 수 없음\" | fields _time, login, tunnel, session, client_ip, client_port, server_ip, server_port, protocol, rx_bytes, tx_bytes, rx_packets, tx_packets, eos";
		query = query.replaceAll("#date", backupDate);
		logger.info(query);
		executeQuery(query, "flow");
		
		logger.info("saveFlowLog backup end");
	}
	
	private void saveTunnelCountLog(){
		String query = "table from=#date000000 to=#date235959 ssl-tunnelcount | fields _time, tunnel_count";
		query = query.replaceAll("#date", backupDate);
		logger.info(query);
		executeQuery(query, "tunnelcount");
		
		logger.info("saveTunnelCountLog backup end");
	}
	
	private void executeQuery(String query, String logType){
		LogQuery logQuery = service.createQuery(query);
		LogQueryCallback callback = new CVSLogBackupQueryCallback(logQuery, logType);
		logQuery.registerQueryCallback(callback);
		// start query
		service.startQuery(logQuery.getId());
	}
	
	private class CVSLogBackupQueryCallback implements LogQueryCallback {
		private LogQuery query;
		private String type;

		private CVSLogBackupQueryCallback(LogQuery query, String type) {
			this.query = query;
			this.type = type;
		}

		@Override
		public int offset() {
			return 0;
		}

		@Override
		public int limit() {
			return 0;
		}

		@Override
		public void onQueryStatusChange() {
		}

		@Override
		public void onPageLoaded() {
		}

		@Override
		public void onEof() {
			try {
				List<Map<String, Object>> result = query.getResultAsList();
				if (result == null)
					return;
				logger.debug("{} result count : {}", type, result.size());
				saveLogCVS(result, type);
			} catch (IOException e) {
				logger.error("getResult fail.", e);
			} 
			query.unregisterQueryCallback(this);
			service.removeQuery(query.getId());
		}
	}
	
	private void saveLogCVS(List<Map<String, Object>> result, String type){
		Charset charset = Charset.forName("utf-8");
		CSVWriter writer = null;
		List<String> keySet = getKeyList(type);
		
		String logFilename = String.format("%s%s-%s.csv", CSV_LOG_DIR, type, backupDate);
		logger.info("saveLogCVS save to {} file.", logFilename);
		OutputStreamWriter out = null;
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ssZ");
		try {
			out = new OutputStreamWriter(new FileOutputStream(logFilename), charset);
			writer = new CSVWriter(out,',', '"');
			int count = 0;
			for (Map<String, Object> m : result) {
				int size = m.keySet().size();
				if (count == 0) {
					writer.writeNext(keySet.toArray(new String[keySet.size()]));
				}

				List<String> values = new ArrayList<String>();
				String value = null;
				
				for (String key : keySet) {
					if ("_time".equals(key))
						value = dateFormat.format((Date)m.get(key));
					else
						value = m.get(key) == null ? "" : m.get(key).toString();
					logger.debug("item = {} : {}",key, value);
					values.add(value);
				}
				writer.writeNext(values.toArray(new String[size]));

				if (count % 1000 == 0)
					writer.flush();

				count++;
			}

		} catch (FileNotFoundException e) {
			logger.error("saveLogCVS file not found.", e);
		} catch (IOException e) {
			logger.error("saveLogCVS io exception.", e);
		} finally {
			if (writer != null)
				try {
					writer.close();
				} catch (IOException e) {
				}
		}
	}
	
	private List<String> getKeyList(String type){
		List<String> keySet = new ArrayList<String>();
		// i need key list.
		if ("auth".equals(type)){
			// _time, type, login, remote_ip, profile, tunnel, os_type, device_key, code, auth_code_desc
			keySet.add("_time");
			keySet.add("type");
			keySet.add("login");
			keySet.add("remote_ip");
			keySet.add("profile");
			keySet.add("tunnel");
			keySet.add("os_type");
			keySet.add("device_key");
			keySet.add("code");
			keySet.add("auth_code_desc");
		} else if ("access".equals(type)){
			// _time, login, tunnel, session, client_ip, client_port, server_ip, server_port, protocol, action
			keySet.add("_time");
			keySet.add("login");
			keySet.add("tunnel");
			keySet.add("session");
			keySet.add("client_ip");
			keySet.add("client_port");
			keySet.add("server_ip");
			keySet.add("server_port");
			keySet.add("protocol");
			keySet.add("action");
		} else if ("flow".equals(type)){
			// _time, login, tunnel, session, client_ip, client_port, server_ip, server_port, protocol, rx_bytes, tx_bytes, rx_packets, tx_packets, eos
			keySet.add("_time");
			keySet.add("login");
			keySet.add("tunnel");
			keySet.add("session");
			keySet.add("client_ip");
			keySet.add("client_port");
			keySet.add("server_ip");
			keySet.add("server_port");
			keySet.add("protocol");
			keySet.add("rx_bytes");
			keySet.add("tx_bytes");
			keySet.add("rx_packets");
			keySet.add("tx_packets");
			keySet.add("eos");
		} else if ("tunnelcount".equals(type)){
			// _time, tunnel_count
			keySet.add("_time");
			keySet.add("tunnel_count");
		}
		return keySet;
	}
}
