package kr.co.future.sslvpn.core;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import kr.co.future.msgbus.Marshalable;

public class TrendGraphPoint implements Marshalable {
	public Date date;
	public double value;
	
	public TrendGraphPoint(Date date, double value) {
		this.date = date;
		this.value = value;
		
		if (Double.isNaN(value))
			this.value = 0;
	}

	@Override
	public Map<String, Object> marshal() {
		Map<String, Object> m = new HashMap<String, Object>();
		m.put("date", date);
		m.put("value", value);
		return m;
	}
}
