package kr.co.future.sslvpn.xtmconf.filtering;

import java.util.HashMap;
import java.util.Map;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import kr.co.future.sslvpn.xtmconf.NodeWrapper;
import kr.co.future.sslvpn.xtmconf.XtmConfig;

public class VlanSetting extends XtmConfig {
	private int num;
	private String cid;
	private String name; // 이름
	private int id; // VLAN ID

	@Override
	public String getXmlFilename() {
		return "vlan_setting.xml";
	}

	@Override
	public String getRootTagName() {
		return "policy";
	}

	public static VlanSetting parse(NodeWrapper nw) {
		if (!nw.isName("vlan"))
			return null;

		VlanSetting vs = new VlanSetting();
		vs.num = nw.intAttr("num");
		vs.cid = nw.attr("cid");

		for (NodeWrapper c : nw.children()) {
			if (c.isName("name"))
				vs.name = c.value();
			else if (c.isName("id"))
				vs.id = c.intValue();
		}

		return vs;
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

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	@Override
	protected Element convertToElement(Document doc) {
		Element e = doc.createElement("vlan");

		e.setAttribute("num", String.valueOf(num));
		e.setAttribute("cid", cid);
		appendChild(doc, e, "name", name);
		appendChild(doc, e, "id", id);

		return e;
	}

	@Override
	public Map<String, Object> marshal() {
		Map<String, Object> m = new HashMap<String, Object>();
		m.put("num", num);
		m.put("cid", cid);
		m.put("name", name);
		m.put("id", id);

		return m;
	}

	@Override
	public String toString() {
		return "VlanSetting [num=" + num + ", cid=" + cid + ", name=" + name + ", id=" + id + "]";
	}
}
