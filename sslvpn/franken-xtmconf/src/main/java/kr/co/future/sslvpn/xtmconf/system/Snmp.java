package kr.co.future.sslvpn.xtmconf.system;

import java.util.HashMap;
import java.util.Map;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import kr.co.future.sslvpn.xtmconf.AttributeBuilder;
import kr.co.future.sslvpn.xtmconf.NodeWrapper;
import kr.co.future.sslvpn.xtmconf.XtmConfig;

public class Snmp extends XtmConfig {
	private boolean settingUse;
	private boolean settingAuthtrap; // TRAP주소 체크박스
	private String community; // 커뮤니티:TRAP주소
	private String location; // 기타정보

	@Override
	public String getXmlFilename() {
		return "system_snmp.xml";
	}

	@Override
	public String getRootTagName() {
		return "system";
	}

	public static Snmp parse(NodeWrapper nw) {
		if (!nw.isName("snmp"))
			return null;

		Snmp snmp = new Snmp();

		for (NodeWrapper c : nw.children()) {
			if (c.isName("setting")) {
				snmp.settingUse = c.boolAttr("chk_use");
				snmp.settingAuthtrap = c.boolAttr("chk_authtrap");
			} else if (c.isName("community"))
				snmp.community = c.value();
			else if (c.isName("location"))
				snmp.location = c.value();
		}

		return snmp;
	}

	public boolean isSettingUse() {
		return settingUse;
	}

	public void setSettingUse(boolean settingUse) {
		this.settingUse = settingUse;
	}

	public boolean isSettingAuthtrap() {
		return settingAuthtrap;
	}

	public void setSettingAuthtrap(boolean settingAuthtrap) {
		this.settingAuthtrap = settingAuthtrap;
	}

	public String getCommunity() {
		return community;
	}

	public void setCommunity(String community) {
		this.community = community;
	}

	public String getLocation() {
		return location;
	}

	public void setLocation(String location) {
		this.location = location;
	}

	@Override
	protected Element convertToElement(Document doc) {
		Element e = doc.createElement("snmp");

		appendChild(doc, e, "setting", null,
				new AttributeBuilder("chk_use", settingUse).put("chk_authtrap", settingAuthtrap));
		appendChild(doc, e, "community", community);
		appendChild(doc, e, "location", location);

		return e;
	}

	@Override
	public Map<String, Object> marshal() {
		Map<String, Object> m = new HashMap<String, Object>();
		m.put("use", settingUse);
		m.put("authtrap", settingAuthtrap);
		m.put("community", community);
		m.put("location", location);

		return m;
	}

	@Override
	public String toString() {
		return "Snmp [settingUse=" + settingUse + ", settingAuthtrap=" + settingAuthtrap + ", community=" + community
				+ ", location=" + location + "]";
	}
}
