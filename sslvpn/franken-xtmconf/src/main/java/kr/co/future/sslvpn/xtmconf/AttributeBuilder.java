package kr.co.future.sslvpn.xtmconf;

import java.util.HashMap;
import java.util.Map;

public class AttributeBuilder {
	private Map<String, String> attr = new HashMap<String, String>();

	public AttributeBuilder() {
	}

	public AttributeBuilder(String key, boolean value) {
		put(key, value);
	}

	public AttributeBuilder(String key, Boolean value) {
		if (value != null)
			put(key, value.booleanValue());
		else
			put(key, value);
	}

	public AttributeBuilder(String key, Object value) {
		put(key, value);
	}

	public AttributeBuilder put(String key, boolean value) {
		this.attr.put(key, value ? "on" : "off");
		return this;
	}

	public AttributeBuilder put(String key, int value) {
		this.attr.put(key, String.valueOf(value));
		return this;
	}

	public AttributeBuilder put(String key, Object value) {
		this.attr.put(key, (value != null) ? value.toString() : "");
		return this;
	}

	public Map<String, String> get() {
		return attr;
	}
}
