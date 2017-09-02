package kr.co.future.sslvpn.model;

import java.util.Map;

import kr.co.future.api.FieldOption;

public class DnsRecord {

	@FieldOption(name = "type")
	private int type;

	@FieldOption(name = "data")
	private Map<String, Object> data;

	public int getType() {
		return type;
	}

	public void setType(int type) {
		this.type = type;
	}

	public Map<String, Object> getData() {
		return data;
	}

	public void setData(Map<String, Object> data) {
		this.data = data;
	}

	@Override
	public String toString() {
		return "type=" + type + ", ip=" + data.get("ip");
	}

}