package kr.co.future.sslvpn.xtmconf;

import java.util.Date;
import java.util.Map;

public interface LastHitService extends Runnable {
	/**
	 * @return rule uid to last date mappings
	 */
	Map<String, Date> getLastHits();

	Date getLastHit(String uid);

	void hit(String uid);	
}
