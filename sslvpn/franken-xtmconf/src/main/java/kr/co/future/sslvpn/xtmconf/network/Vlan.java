package kr.co.future.sslvpn.xtmconf.network;

import java.util.HashMap;
import java.util.Map;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import kr.co.future.sslvpn.xtmconf.AttributeBuilder;
import kr.co.future.sslvpn.xtmconf.NodeWrapper;
import kr.co.future.sslvpn.xtmconf.XtmConfig;

public class Vlan extends XtmConfig {
	private int num;
	private String iface; // 인터페이스
	private int id; // VLAN ID
	private boolean reordering; // ?

	@Override
	public String getXmlFilename() {
		return "network_vlan.xml";
	}

	@Override
	public String getRootTagName() {
		return "network";
	}

	public static Vlan parse(NodeWrapper nw) {
		if (!nw.isName("vlan"))
			return null;

		Vlan v = new Vlan();
		v.num = nw.intAttr("num");

		for (NodeWrapper c : nw.children()) {
			if (c.isName("interface"))
				v.iface = c.value();
			else if (c.isName("id"))
				v.id = c.intValue();
			else if (c.isName("setting"))
				v.reordering = c.boolAttr("chk_reordering");
		}

		return v;
	}

	public int getNum() {
		return num;
	}

	public void setNum(int num) {
		this.num = num;
	}

	public String getIface() {
		return iface;
	}

	public void setIface(String iface) {
		this.iface = iface;
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public boolean isReordering() {
		return reordering;
	}

	public void setReordering(boolean reordering) {
		this.reordering = reordering;
	}

	@Override
	protected Element convertToElement(Document doc) {
		Element e = doc.createElement("vlan");

		e.setAttribute("num", String.valueOf(num));
		appendChild(doc, e, "interface", iface);
		appendChild(doc, e, "id", id);
		appendChild(doc, e, "setting", null, new AttributeBuilder("chk_reordering", reordering));

		return e;
	}

	@Override
	public Map<String, Object> marshal() {
		Map<String, Object> m = new HashMap<String, Object>();
		m.put("num", num);
		m.put("interface", iface);
		m.put("id", id);
		m.put("reordering", reordering);

		return m;
	}

	@Override
	public String toString() {
		return "Vlan [num=" + num + ", iface=" + iface + ", id=" + id + ", reodering=" + reordering + "]";
	}
}
