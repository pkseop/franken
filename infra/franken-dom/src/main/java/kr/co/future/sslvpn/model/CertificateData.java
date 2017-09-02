package kr.co.future.sslvpn.model;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import kr.co.future.msgbus.Marshalable;

public class CertificateData implements Marshalable {
	private String loginName;
	private String serial;
	private String orgUnitName;
	private String orgUnitGuid;
	private Date notBefore;
	private Date notAfter;
	private Date issuedDate;
	private String userName;
	private String reason;

	public String getOrgUnitGuid() {
		return orgUnitGuid;
	}

	public void setOrgUnitGuid(String orgUnitGuid) {
		this.orgUnitGuid = orgUnitGuid;
	}

	public String getReason() {
		return reason;
	}

	public void setReason(String reason) {
		this.reason = reason;
	}

	public String getUserName() {
		return userName;
	}

	public void setUserName(String userName) {
		this.userName = userName;
	}

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

	public String getOrgUnitName() {
		return orgUnitName;
	}

	public void setOrgUnitName(String orgUnitName) {
		this.orgUnitName = orgUnitName;
	}

	public Date getNotBefore() {
		return notBefore;
	}

	public void setNotBefore(Date notBefore) {
		this.notBefore = notBefore;
	}

	public Date getNotAfter() {
		return notAfter;
	}

	public void setNotAfter(Date notAfter) {
		this.notAfter = notAfter;
	}

	public Date getIssuedDate() {
		return issuedDate;
	}

	public void setIssuedDate(Date issuedDate) {
		this.issuedDate = issuedDate;
	}

	@Override
	public Map<String, Object> marshal() {
		Map<String, Object> m = new HashMap<String, Object>();
		m.put("login_name", loginName);
		m.put("serial", serial);
		m.put("org_unit_name", orgUnitName);
		m.put("user_name", userName);
		m.put("issued_date", issuedDate);
		m.put("not_before", notBefore);
		m.put("not_after", notAfter);
		m.put("reason", reason);
		m.put("org_unit_guid", orgUnitGuid);

		return m;
	}

}
