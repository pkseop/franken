package kr.co.future.sslvpn.model;

import java.util.HashMap;
import java.util.Map;

import kr.co.future.api.FieldOption;
import kr.co.future.confdb.CollectionName;
import kr.co.future.msgbus.Marshalable;

// 사용자별 현재 사설 인증서 맵핑 
@CollectionName("certs")
public class Cert implements Marshalable {

	@FieldOption(name = "login_name", nullable = false)
	private String loginName;

	@FieldOption(name = "serial", nullable = false)
	private String serial;

	public String getLoginName() {
		return loginName;
	}

	public void setLoginName(String loginName) {
		this.loginName = loginName;
	}

	public String getSerial() {
		return serial;
	}

	public void setSerial(String serial) {
		this.serial = serial;
	}

	@Override
	public String toString() {
		return "Cert [loginName=" + loginName + ", serial=" + serial + "]";
	}

	@Override
	public Map<String, Object> marshal() {
		Map<String, Object> m = new HashMap<String, Object>();
		m.put("login_name", loginName);
		m.put("serial", serial);

		return m;
	}
}
