package kr.co.future.sslvpn.core;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import kr.co.future.sslvpn.core.LogCapacityLogService;
import kr.co.future.sslvpn.core.log.LogCapacityLog;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;

import kr.co.future.cron.PeriodicJob;
import kr.co.future.logdb.LogQuery;
import kr.co.future.logdb.LogQueryService;
import kr.co.future.logstorage.Log;
import kr.co.future.logstorage.LogStorage;
import kr.co.future.logstorage.LogTableRegistry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@PeriodicJob("0 * * * *")
@Provides
@Component(name = "frodo-capacity-log-service")
public class LogCapacityLogService implements Runnable {
	private final Logger logger = LoggerFactory.getLogger(LogCapacityLogService.class.getName());

	@Requires
	private LogStorage logStorage;

	@Requires
	private LogTableRegistry tableRegistry;

	@Requires
	private LogQueryService qs;

	@Override
	public void run() {
		writeTableCapacityLog();
		writeLogDirCapacityLog();
	}

	private void writeLogDirCapacityLog() {
		logStorage.write(getLog("log-dir", logStorage.getDirectory()));
	}

	private void writeTableCapacityLog() {
		List<Log> logs = new ArrayList<Log>();
		for (String tableName : tableRegistry.getTableNames())
			logs.add(getLog(tableName, new File(logStorage.getDirectory(), Integer.toString(tableRegistry.getTableId(tableName)))));

		logStorage.write(logs);
	}

	private Log getLog(String tableName, File logDir) {
		LogCapacityLog currentLog = new LogCapacityLog();
		currentLog.setDate(new Date());
		currentLog.setTableName(tableName);
		currentLog.setTotal(getCapacity(logDir));
		logger.trace("frodo core: write log capacity table_name [{}], log [{}]", tableName, currentLog.toString());

		Map<String, Object> lastedLog = getLastedLog(tableName);
		if (lastedLog != null) {
			currentLog.setBeforeCapacity((Long) lastedLog.get("total"));
			currentLog.setIncrementCapacity(currentLog.getTotal() - currentLog.getBeforeCapacity());
		}

		logger.trace("frodo core: write log capacity table_name [{}], log [{}]", tableName, currentLog.toString());
		return currentLog.toLog();
	}

	private Map<String, Object> getLastedLog(String tableName) {
		Map<String, Object> result = null;

		String query = "table log-capacity | search table_name == " + tableName
				+ " | fields _time, table_name, before_capacity, increment_capacity, total";

		LogQuery lq = qs.createQuery(query);

		try {
			qs.startQuery(lq.getId());
			do {
				try {
					Thread.sleep(100);
				} catch (InterruptedException e) {
					logger.error("frodo core: log query interrupted", e);
				}
			} while (!lq.isEnd());

			Long resultCount = lq.getResultCount();
			if (resultCount == null || resultCount == 0)
				return null;

			result = lq.getResultAsList(0, 1).get(0);
		} catch (IOException e) {
			logger.error("frodo core: cannot obtain lasted capacity log", e);
		} finally {
			qs.removeQuery(lq.getId());
		}

		return result;
	}

	private long getCapacity(File parent) {
		long capacity = 0;

		if (parent.isDirectory()) {
			for (File child : parent.listFiles()) {
				capacity += getCapacity(child);
			}

			return capacity;
		}

		return parent.length();
	}
}
