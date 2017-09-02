package kr.co.future.sslvpn.model;

public class RegistryCheck {
	private String key;

	private RegistryType type;

	private String value;

	private boolean checkValue = true;

	public String getKey() {
		return key;
	}

	public void setKey(String key) {
		this.key = key;
	}

	public RegistryType getType() {
		return type;
	}

	public void setType(RegistryType type) {
		this.type = type;
	}

	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}

	public boolean isCheckValue() {
		return checkValue;
	}

	public void setCheckValue(boolean checkValue) {
		this.checkValue = checkValue;
	}

	@Override
	public String toString() {
		return "[key= " + key + ", type= " + type + ", value= " + value + ", check_value=" + checkValue + "]";
	}

}
