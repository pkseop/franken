package kr.co.future.sslvpn.model;

public class WindowsHardeningCheck {
	private WindowsHardeningCategory category;

	// enforce hardnening
	private boolean enforce;

	// "true", "false", or integer number
	private String baseline;

	public WindowsHardeningCheck() {
	}

	public WindowsHardeningCheck(WindowsHardeningCategory category, boolean enforce, String baseline) {
		this.category = category;
		this.enforce = enforce;
		this.baseline = baseline;
	}

	public WindowsHardeningCategory getCategory() {
		return category;
	}

	public void setCategory(WindowsHardeningCategory category) {
		this.category = category;
	}

	public boolean isEnforce() {
		return enforce;
	}

	public void setEnforce(boolean enforce) {
		this.enforce = enforce;
	}

	public String getBaseline() {
		return baseline;
	}

	public void setBaseline(String baseline) {
		this.baseline = baseline;
	}
}
