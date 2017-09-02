package kr.co.future.sslvpn.xtmconf.etc;

import java.util.HashMap;
import java.util.Map;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import kr.co.future.sslvpn.xtmconf.NodeWrapper;
import kr.co.future.sslvpn.xtmconf.Utils;
import kr.co.future.sslvpn.xtmconf.XtmConfig;

public class FwScript extends XtmConfig {
	public static enum Type {
		McForwarding("mc_forwarding"), IDSPort("ids_port"), SIP("sip");

		private String name;

		private Type(String name) {
			this.name = name;
		}

		public static Type get(String str) {
			for (Type t : Type.values()) {
				if (t.toString().equals(str))
					return t;
			}
			return null;
		}

		@Override
		public String toString() {
			return name;
		}
	}

	private Type type;
	private boolean mcForwarding; // L3 모드에서 Multicast...
	private String idsIface; // IDS 연동
	private boolean sip; // 주소변환시 SIP패킷의...

	@Override
	public String getXmlFilename() {
		return "fw_script.xml";
	}

	@Override
	public String getRootTagName() {
		return "fw_script";
	}

	public static FwScript parse(NodeWrapper nw) {
		if (Type.get(nw.name()) == null)
			return null;

		FwScript fs = new FwScript();
		fs.type = Type.get(nw.name());

		if (fs.type == Type.McForwarding)
			fs.mcForwarding = nw.boolAttr("chk_use");
		else if (fs.type == Type.IDSPort) {
			for (NodeWrapper c : nw.children()) {
				if (c.isName("interface"))
					fs.idsIface = c.value();
			}
		} else if (fs.type == Type.SIP)
			fs.sip = nw.boolAttr("chk_use");

		return fs;
	}

	public Type getType() {
		return type;
	}

	public void setType(Type type) {
		this.type = type;
	}

	public boolean isMcForwarding() {
		return mcForwarding;
	}

	public void setMcForwarding(boolean mcForwarding) {
		this.mcForwarding = mcForwarding;
	}

	public String getIdsIface() {
		return idsIface;
	}

	public void setIdsIface(String idsIface) {
		this.idsIface = idsIface;
	}

	public boolean isSip() {
		return sip;
	}

	public void setSip(boolean sip) {
		this.sip = sip;
	}

	@Override
	protected Element convertToElement(Document doc) {
		Element e = doc.createElement(type.toString());

		if (type == Type.McForwarding)
			e.setAttribute("chk_use", Utils.bool(mcForwarding));
		else if (type == Type.IDSPort)
			appendChild(doc, e, "interface", idsIface);
		else if (type == Type.SIP)
			e.setAttribute("chk_use", Utils.bool(sip));

		return e;
	}

	@Override
	public Map<String, Object> marshal() {
		Map<String, Object> m = new HashMap<String, Object>();
		m.put("type", type);
		if (type == Type.McForwarding)
			m.put("mc_forwarding", mcForwarding);
		else if (type == Type.IDSPort)
			m.put("interface", idsIface);
		else if (type == Type.SIP)
			m.put("sip", sip);

		return m;
	}

	@Override
	public String toString() {
		return "FwScript [type=" + type + ", mcForwarding=" + mcForwarding + ", idsIface=" + idsIface + ", sip=" + sip
				+ "]";
	}
}
