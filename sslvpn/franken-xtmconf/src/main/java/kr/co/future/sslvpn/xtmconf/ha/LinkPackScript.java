package kr.co.future.sslvpn.xtmconf.ha;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import kr.co.future.sslvpn.xtmconf.AttributeBuilder;
import kr.co.future.sslvpn.xtmconf.NodeWrapper;
import kr.co.future.sslvpn.xtmconf.Utils;
import kr.co.future.sslvpn.xtmconf.XtmConfig;

public class LinkPackScript extends XtmConfig {
	public static enum PackType {
		Link, Bond;

		public static PackType get(String str) {
			for (PackType p : PackType.values()) {
				if (p.toString().equals(str))
					return p;
			}
			return null;
		}

		@Override
		public String toString() {
			return name().toLowerCase() + "_pack";
		}
	}

	private PackType type; // Link Pack, Bond Pack
	private String name; // ethx
	private Boolean[] chk = new Boolean[Utils.getEthCount()]; // On/Off

	@Override
	public String getXmlFilename() {
		return "link_pack_script.xml";
	}

	@Override
	public String getRootTagName() {
		return "link_pack_script";
	}

	public static LinkPackScript parse(NodeWrapper nw) {
		if (!nw.isName("link_pack") && !nw.isName("bond_pack"))
			return null;

		LinkPackScript lps = new LinkPackScript();
		lps.type = PackType.get(nw.name());

		for (NodeWrapper c : nw.children()) {
			if (c.isName("name"))
				lps.name = c.value();
			else if (c.isName("interface")) {
				for (int i = 0; i < Utils.getEthCount(); i++)
					lps.chk[i] = c.boolAttr("chk_eth" + i);
			}
		}

		return lps;
	}

	public PackType getType() {
		return type;
	}

	public void setType(PackType type) {
		this.type = type;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Boolean[] getChk() {
		return chk;
	}

	public void setChk(Boolean[] chk) {
		this.chk = chk;
	}

	@Override
	protected Element convertToElement(Document doc) {
		Element e = doc.createElement(type.toString());

		appendChild(doc, e, "name", name);
		AttributeBuilder ab = new AttributeBuilder();
		for (int i = 0; i < chk.length; i++)
			ab.put("chk_eth" + i, chk[i] ? "on" : "off");
		appendChild(doc, e, "interface", null, ab);

		return e;
	}

	@Override
	public Map<String, Object> marshal() {
		Map<String, Object> m = new HashMap<String, Object>();
		m.put("type", type);
		m.put("name", name);
		m.put("chk", chk);

		return m;
	}

	@Override
	public String toString() {
		return "LinkPackScript [type=" + type + ", name=" + name + ", chk=" + Arrays.toString(chk) + "]";
	}
}
