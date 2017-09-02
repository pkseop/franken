package kr.co.future.sslvpn.xtmconf.system;

import java.util.HashMap;
import java.util.Map;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import kr.co.future.sslvpn.xtmconf.AttributeBuilder;
import kr.co.future.sslvpn.xtmconf.MarshalValue;
import kr.co.future.sslvpn.xtmconf.NodeWrapper;
import kr.co.future.sslvpn.xtmconf.XtmConfig;

public class UserAdmin extends XtmConfig {
	private int num;
	private String cid;
	private boolean settingConfig; // 접근권한-설정
	private boolean settingMonitor; // 접근권한-로그,모니터
	private boolean settingOtp; // 연락처-OTP 사용
	private String id; // 아이디
	private String salt; // 맛소금
	private String password; // 비밀번호 (sha1 encrypt)
	private String mail; // 받는 Email
	// private String sendMail; // 보낼 Email
	private String phone; // 연락처
	private String host; // Trusted host

	@Override
	public String getXmlFilename() {
		return "system_user_admin.xml";
	}

	@Override
	public String getRootTagName() {
		return "system";
	}

	public static UserAdmin parse(NodeWrapper nw) {
		if (!nw.isName("user"))
			return null;

		UserAdmin ua = new UserAdmin();
		ua.num = nw.intAttr("num");
		ua.cid = nw.attr("cid");

		for (NodeWrapper c : nw.children()) {
			if (c.isName("setting")) {
				ua.settingConfig = c.boolAttr("chk_config");
				ua.settingMonitor = c.boolAttr("chk_monitor");
				ua.settingOtp = c.boolAttr("chk_otp");
			} else if (c.isName("id"))
				ua.id = c.value();
			else if (c.isName("password"))
				ua.password = c.value();
			else if (c.isName("salt"))
				ua.salt = c.value();
			else if (c.isName("mail"))
				ua.mail = c.value();
			else if (c.isName("phone"))
				ua.phone = c.value();
			else if (c.isName("host"))
				ua.host = c.value();
		}

		return ua;
	}

	public int getNum() {
		return num;
	}

	public void setNum(int num) {
		this.num = num;
	}

	public String getCid() {
		return cid;
	}

	public void setCid(String cid) {
		this.cid = cid;
	}

	public boolean isSettingConfig() {
		return settingConfig;
	}

	public void setSettingConfig(boolean settingConfig) {
		this.settingConfig = settingConfig;
	}

	public boolean isSettingMonitor() {
		return settingMonitor;
	}

	public void setSettingMonitor(boolean settingMonitor) {
		this.settingMonitor = settingMonitor;
	}

	public boolean isSettingOtp() {
		return settingOtp;
	}

	public void setSettingOtp(boolean settingOtp) {
		this.settingOtp = settingOtp;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public String getSalt() {
		return salt;
	}

	public void setSalt(String salt) {
		this.salt = salt;
	}

	public String getMail() {
		return mail;
	}

	public void setMail(String mail) {
		this.mail = mail;
	}

	public String getPhone() {
		return phone;
	}

	public void setPhone(String phone) {
		this.phone = phone;
	}

	public String getHost() {
		return host;
	}

	public void setHost(String host) {
		this.host = host;
	}

	@Override
	protected Element convertToElement(Document doc) {
		Element e = doc.createElement("user");

		e.setAttribute("num", String.valueOf(num));
		e.setAttribute("cid", cid);
		AttributeBuilder settingAttr = new AttributeBuilder("chk_config", settingConfig).put("chk_monitor",
				settingMonitor).put("chk_otp", settingOtp);
		appendChild(doc, e, "setting", null, settingAttr);
		appendChild(doc, e, "id", id);
		appendChild(doc, e, "password", password);
		appendChild(doc, e, "salt", salt);
		appendChild(doc, e, "mail", mail);
		appendChild(doc, e, "send_mail", mail);
		appendChild(doc, e, "phone", phone);
		appendChild(doc, e, "host", host);

		return e;
	}

	@Override
	public Map<String, Object> marshal() {
		Map<String, Object> m = new HashMap<String, Object>();
		m.put("num", num);
		m.put("cid", cid);
		m.put("setting", new MarshalValue("config", settingConfig).put("monitor", settingMonitor)
				.put("otp", settingOtp).get());
		m.put("id", id);
		m.put("mail", mail);
		m.put("phone", phone);
		m.put("host", host);

		return m;
	}

	@Override
	public String toString() {
		return "UserAdmin [num=" + num + ", cid=" + cid + ", settingConfig=" + settingConfig + ", settingMonitor="
				+ settingMonitor + ", settingOtp=" + settingOtp + ", id=" + id + ", mail=" + mail + ", phone=" + phone
				+ ", host=" + host + "]";
	}
}
