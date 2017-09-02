package kr.co.future.sslvpn.core;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import kr.co.future.msgbus.Marshalable;

public class AlertStatus implements Marshalable {
	private Date date;
	private boolean alert;
	
	public AlertStatus() {
	}
	
	public AlertStatus(Date date, boolean alert) {
		this.date = date;
		this.alert = alert;
	}

	@Override
	public Map<String, Object> marshal() {
		Map<String, Object> m = new HashMap<String, Object>();
		m.put("date", date);
		m.put("alert", alert);
		return m;
	}

}
