package kr.co.future.sslvpn.xtmconf.filtering;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Validate;

import kr.co.future.cron.PeriodicJob;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import kr.co.future.sslvpn.xtmconf.LastHitService;

@Component(name = "frodo-xtmconf-lasthit")
@Provides
@PeriodicJob("* * * * *")
public class LastHitServiceImpl implements LastHitService {

	private static final String lastHitFile = "/utm/log/stat/lasthit.log";
	private final Logger logger = LoggerFactory.getLogger(LastHitService.class.getName());
	private ConcurrentMap<String, Date> ipv4RuleHits = new ConcurrentHashMap<String, Date>();

	@Validate
	public void start() {
		try {
			loadDumpFile();
		} catch (IOException e) {
			logger.error("frodo core: cannot load last hit log file [{}]", e);
		} catch (ParseException e) {
			logger.error("frodo core: last hit parse error [{}]", e);
		}
	}

	public void run() {
		logger.debug("frodo core: dumping last hits");
		dumpFilteringLogs();
	}

	@Override
	public void hit(String uid) {
		logger.debug("frodo core: hit [rule uid {}]", uid);
		ipv4RuleHits.put(uid, new Date());
	}

	@Override
	public Map<String, Date> getLastHits() {
		Map<String, Date> m = new HashMap<String, Date>();

		for (String key : ipv4RuleHits.keySet()) {
			Date value = ipv4RuleHits.get(key);
			m.put(key, value);
		}

		return m;
	}

	@Override
	public Date getLastHit(String uid) {
		logger.debug("frodo core: request last hit uid [{}]", uid);
		return ipv4RuleHits.get(uid);
	}

	private void loadDumpFile() throws IOException, ParseException {
		File file = new File(lastHitFile);

		if (!file.exists())
			return;

		if (!file.isFile()) {
			logger.error("frodo core: path [{}] is not file", file.getAbsolutePath());
			return;
		}

		SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ssZ");
		BufferedReader br = new BufferedReader(new FileReader(lastHitFile));
		String line = "";

		while ((line = br.readLine()) != null) {
			String[] tokens = line.split(";");
			Date date = format.parse(tokens[1]);
			ipv4RuleHits.put(tokens[0], date);
		}
		
		if (br != null){
         try {
             br.close();
         } catch (IOException e) {
         }
     }
	}

	private void dumpFilteringLogs() {
		new File(lastHitFile).getParentFile().mkdirs();
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ssZ");
		BufferedWriter bw = null;
		try {
			bw = new BufferedWriter(new FileWriter(lastHitFile));
			for (String key : ipv4RuleHits.keySet()) {
				bw.write(key);
				bw.write(";");
				bw.write(dateFormat.format(ipv4RuleHits.get(key)));
				bw.newLine();
			}
		} catch (IOException e) {
			logger.error("frodo core: last hit dump error", e);
		} finally {
			try {
				if (bw != null)
					bw.close();
			} catch (IOException e) {
			}
		}
	}

	public ConcurrentMap<String, Date> getIpv4RuleHits() {
		return ipv4RuleHits;
	}

	public void setIpv4RuleHits(ConcurrentMap<String, Date> hits) {
		this.ipv4RuleHits = hits;
	}

}
