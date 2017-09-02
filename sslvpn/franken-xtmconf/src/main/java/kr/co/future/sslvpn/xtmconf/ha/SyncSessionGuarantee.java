package kr.co.future.sslvpn.xtmconf.ha;

import java.util.HashMap;
import java.util.Map;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import kr.co.future.sslvpn.xtmconf.AttributeBuilder;
import kr.co.future.sslvpn.xtmconf.NodeWrapper;
import kr.co.future.sslvpn.xtmconf.Utils;
import kr.co.future.sslvpn.xtmconf.XtmConfig;

public class SyncSessionGuarantee extends XtmConfig {
	private boolean use; // 세션보장 사용 (Packet_Relay)
	private ActMode mode; // 선택 모드
	private String iface; // 인터페이스
	private String sgmac; // 세션보장할 장비의 특정 인터페이스 MAC주소

	@Override
	public String getXmlFilename() {
		return "sync_session_guarantee.xml";
	}

	@Override
	public String getRootTagName() {
		return "sync";
	}

	public static SyncSessionGuarantee parse(NodeWrapper nw) {
		if (!nw.isName("guarantee"))
			return null;

		SyncSessionGuarantee s = new SyncSessionGuarantee();
		s.use = nw.boolAttr("chk_use");

		for (NodeWrapper c : nw.children()) {
			if (c.isName("act"))
				s.mode = ActMode.get(c.attr("mode"));
			else if (c.isName("interface"))
				s.iface = c.value();
			else if (c.isName("sgmac"))
				s.sgmac = c.value();
		}

		return s;
	}

	public boolean isUse() {
		return use;
	}

	public void setUse(boolean use) {
		this.use = use;
	}

	public ActMode getAct() {
		return mode;
	}

	public void setAct(ActMode act) {
		this.mode = act;
	}

	public String getIface() {
		return iface;
	}

	public void setIface(String iface) {
		this.iface = iface;
	}

	public String getSgmac() {
		return sgmac;
	}

	public void setSgmac(String sgmac) {
		this.sgmac = sgmac;
	}

	@Override
	protected Element convertToElement(Document doc) {
		Element e = doc.createElement("guarantee");

		e.setAttribute("chk_use", Utils.bool(use));
		appendChild(doc, e, "act", null, new AttributeBuilder("mode", mode));
		appendChild(doc, e, "interface", iface);
		appendChild(doc, e, "sgmac", sgmac);

		return e;
	}

	@Override
	public Map<String, Object> marshal() {
		Map<String, Object> m = new HashMap<String, Object>();
		m.put("use", use);
		m.put("mode", mode);
		m.put("interface", iface);
		m.put("sgmac", sgmac);

		return m;
	}

	@Override
	public String toString() {
		return "SyncSessionGuarantee [use=" + use + ", act=" + mode + ", iface=" + iface + ", sgmac=" + sgmac + "]";
	}
}
