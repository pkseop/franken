package kr.co.future.sslvpn.core;

import kr.co.future.api.FieldOption;
import kr.co.future.confdb.CollectionName;

@CollectionName("external_auth_config")
public class ExternalAuthConfig {
	@FieldOption(nullable = false)
	private boolean enabled;

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	@Override
	public String toString() {
		return "enabled=" + enabled;
	}
}
