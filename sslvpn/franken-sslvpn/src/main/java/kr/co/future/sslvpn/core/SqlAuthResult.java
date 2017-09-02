package kr.co.future.sslvpn.core;

public class SqlAuthResult {
	private boolean success;
	private String name;
	private String orgUnitName;

	public boolean isSuccess() {
		return success;
	}

	public void setSuccess(boolean success) {
		this.success = success;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getOrgUnitName() {
		return orgUnitName;
	}

	public void setOrgUnitName(String orgUnitName) {
		this.orgUnitName = orgUnitName;
	}

	@Override
	public String toString() {
		return "SqlAuthResult [success=" + success + ", name=" + name + ", orgUnitName=" + orgUnitName + "]";
	}

}
