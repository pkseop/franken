package kr.co.future.sslvpn.model;

import java.util.HashMap;
import java.util.Map;

import kr.co.future.api.FieldOption;
import kr.co.future.msgbus.Marshalable;

public class LdapAttribute implements Marshalable {

	@FieldOption(name = "use_password_change")
	private boolean usePasswordChange = false;

	public boolean isUsePasswordChange() {
		return usePasswordChange;
	}

	public void setUsePasswordChange(boolean usePasswordChange) {
		this.usePasswordChange = usePasswordChange;
	}

	@Override
	public Map<String, Object> marshal() {
		Map<String, Object> m = new HashMap<String, Object>();
		m.put("use_password_change", usePasswordChange);

		return m;
	}
}
