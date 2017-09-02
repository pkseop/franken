package kr.co.future.sslvpn.auth.hl;

import kr.co.future.api.FieldOption;

public class HlConfig {
	@FieldOption(nullable = false)
	private int removeHour;
	
	@FieldOption(nullable = false)
	private int removeCacheHour;

    @FieldOption(name = "is_enabled", nullable = false)
    private Boolean isEnabled = true;

    public Boolean isEnabled() {
        return isEnabled;
    }

    public void setEnable(Boolean isEnabled){
        this.isEnabled = isEnabled;
    }

	public int getRemoveHour() {
		return removeHour;
	}

	public void setRemoveHour(int removeHour) {
		this.removeHour = removeHour;
	}

	public int getRemoveCacheHour() {
		return removeCacheHour;
	}

	public void setRemoveCacheHour(int removeCacheHour) {
		this.removeCacheHour = removeCacheHour;
	}


}
