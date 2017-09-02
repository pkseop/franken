package kr.co.future.sslvpn.xtmconf.nac;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import kr.co.future.sslvpn.xtmconf.NodeWrapper;
import kr.co.future.sslvpn.xtmconf.XtmConfig;

public class Setting extends XtmConfig {
	public static enum Type {
		Setting, Legacy;

		public static Type get(String str) {
			for (Type t : Type.values()) {
				if (t.toString().equals(str))
					return t;
			}
			return null;
		}

		@Override
		public String toString() {
			return name().toLowerCase();
		}
	}

	private Type type;
	private int timeout; // 인증 유효 시간
	private String iface; // 인증 인터페이스
	private List<String> legacy = new ArrayList<String>(); // 인가된 호스트

	@Override
	public String getXmlFilename() {
		return "nac_setting.xml";
	}

	@Override
	public String getRootTagName() {
		return "nac";
	}

	public static Setting parse(NodeWrapper nw) {
		if (Type.get(nw.name()) == null)
			return null;

		Setting s = new Setting();
		s.type = Type.get(nw.name());

		for (NodeWrapper c : nw.children()) {
			if (c.isName("timeout") && s.type == Type.Setting)
				s.timeout = c.intValue();
			else if (c.isName("interface") && s.type == Type.Setting)
				s.iface = c.value();
			else if (c.isName("mac") && s.type == Type.Legacy)
				s.legacy.add(c.value());
		}

		return s;
	}

	public Type getType() {
		return type;
	}

	public void setType(Type type) {
		this.type = type;
	}

	public int getTimeout() {
		return timeout;
	}

	public void setTimeout(int timeout) {
		this.timeout = timeout;
	}

	public String getIface() {
		return iface;
	}

	public void setIface(String iface) {
		this.iface = iface;
	}

	public List<String> getLegacy() {
		return legacy;
	}

	public void setLegacy(List<String> legacy) {
		this.legacy = legacy;
	}

	@Override
	protected Element convertToElement(Document doc) {
		Element e = doc.createElement(type.toString());

		if (type == Type.Setting) {
			appendChild(doc, e, "timeout", timeout);
			appendChild(doc, e, "interface", iface);
		} else if (type == Type.Legacy) {
			e.setAttribute("count", String.valueOf(legacy.size()));
			for (String s : legacy)
				appendChild(doc, e, "mac", s);
		}

		return e;
	}

	@Override
	public Map<String, Object> marshal() {
		Map<String, Object> m = new HashMap<String, Object>();
		m.put("type", type);

		if (type == Type.Setting) {
			m.put("timeout", timeout);
			m.put("interface", iface);
		} else if (type == Type.Legacy)
			m.put("mac", legacy);

		return m;
	}

	@Override
	public String toString() {
		return "Setting [type=" + type + ", timeout=" + timeout + ", iface=" + iface + ", legacy=" + legacy + "]";
	}
}
