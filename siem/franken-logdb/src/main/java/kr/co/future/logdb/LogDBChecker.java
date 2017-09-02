package kr.co.future.logdb;

import java.util.Date;

import kr.co.future.cron.PeriodicJob;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(name = "log-query-checker")
@Provides
@PeriodicJob("*/10 * * * *")
public class LogDBChecker implements Runnable {
	private final Logger logger = LoggerFactory.getLogger(LogDBChecker.class.getName());
	
	@Requires
	private LogQueryService qs;
	
	@Override
	public void run() {
		logger.trace("start remove long time log query.");
		for (LogQuery q : qs.getQueries()) {
			int id = q.getId();
			Date startDate = q.getLastStarted();
			Date now = new Date();
			if ((now.getTime() - startDate.getTime()) / (60 * 60 * 1000) >= 1){
				qs.removeQuery(id);
				logger.trace("query id {} removed.", id);
			}
		}
		logger.trace("end remove long time log query.");
	}

}
