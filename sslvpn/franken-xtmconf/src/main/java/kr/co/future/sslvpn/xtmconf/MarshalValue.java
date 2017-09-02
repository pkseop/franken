package kr.co.future.sslvpn.xtmconf;

import java.util.HashMap;
import java.util.Map;

public class MarshalValue {
	private Map<String, Object> m = new HashMap<String, Object>();

	public MarshalValue() {
	}

	public MarshalValue(String key, Object value) {
		m.put(key, value);
	}

	public MarshalValue put(String key, Object value) {
		m.put(key, value);
		return this;
	}

	public Map<String, Object> get() {
		return m;
	}
}
