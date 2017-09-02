package kr.co.future.sslvpn.xtmconf.ipsec;

import java.util.HashMap;
import java.util.Map;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import kr.co.future.sslvpn.xtmconf.AttributeBuilder;
import kr.co.future.sslvpn.xtmconf.NodeWrapper;
import kr.co.future.sslvpn.xtmconf.XtmConfig;

public class VpnSetting extends XtmConfig {
	private String name; // 인터페이스
	private boolean use; // IPSEC 사용

	@Override
	public String getXmlFilename() {
		return "vpn_setting.xml";
	}

	@Override
	public String getRootTagName() {
		return "vpn";
	}

	public static VpnSetting parse(NodeWrapper nw) {
		if (!nw.isName("interface"))
			return null;

		VpnSetting vs = new VpnSetting();

		for (NodeWrapper c : nw.children()) {
			if (c.isName("name"))
				vs.name = c.value();
			else if (c.isName("setting"))
				vs.use = c.boolAttr("chk_ipsec");
		}

		return vs;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public boolean isUse() {
		return use;
	}

	public void setUse(boolean use) {
		this.use = use;
	}

	@Override
	protected Element convertToElement(Document doc) {
		Element e = doc.createElement("interface");

		appendChild(doc, e, "name", name);
		appendChild(doc, e, "setting", null, new AttributeBuilder("chk_ipsec", use));

		return e;
	}

	@Override
	public Map<String, Object> marshal() {
		Map<String, Object> m = new HashMap<String, Object>();
		m.put("name", name);
		m.put("use", use);

		return m;
	}

	@Override
	public String toString() {
		return "VpnSetting [name=" + name + ", use=" + use + "]";
	}
}
