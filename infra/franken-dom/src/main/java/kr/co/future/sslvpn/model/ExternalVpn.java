package kr.co.future.sslvpn.model;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import kr.co.future.api.FieldOption;
import kr.co.future.api.MapTypeHint;
import kr.co.future.msgbus.Marshalable;

public class ExternalVpn implements Marshalable {
	@FieldOption(name = "type", nullable = true)
	private String type;

	@MapTypeHint({ String.class, Object.class })
	@FieldOption(name = "address", nullable = true)
	private List<Map<String, Object>> address;

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public List<Map<String, Object>> getAddress() {
		return address;
	}

	public void setAddress(List<Map<String, Object>> address) {
		this.address = address;
	}

	@Override
	public Map<String, Object> marshal() {
		Map<String, Object> m = new HashMap<String, Object>();
		m.put("type", type);
		m.put("address", address);
		return m;
	}
}
