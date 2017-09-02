package kr.co.future.httpd;

import java.io.File;
import java.io.FilenameFilter;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public class LogCleaner implements Runnable {
	private static final int PURGE_CYCLE = 3600 * 1000;
	private static final int KEEP_LOG_DAYS = 7;
	private static final String LOG_FILENAME = "http-access.log.";

	@Override
	public void run() {
		try {
			while (true) {
				File logDir = new File(System.getProperty("kraken.log.dir"));
				File[] logFiles = logDir.listFiles(new FilenameFilter() {
					@Override
					public boolean accept(File dir, String name) {
						return name.startsWith(LOG_FILENAME);
					}
				});

				// delete older than 1 weeks
				Calendar cal = Calendar.getInstance();
				cal.add(Calendar.DAY_OF_MONTH, -KEEP_LOG_DAYS);
				Date baseline = cal.getTime();

				SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
				for (File file : logFiles) {
					try {
						Date date = dateFormat.parse(file.getName().substring(LOG_FILENAME.length()));
						if (date.before(baseline))
							file.delete();

					} catch (ParseException e) {
					}
				}

				Thread.sleep(PURGE_CYCLE); // 1 hour
			}
		} catch (InterruptedException e) {
		}
	}
}