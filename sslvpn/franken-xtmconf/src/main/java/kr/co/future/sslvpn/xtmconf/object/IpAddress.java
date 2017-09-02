package kr.co.future.sslvpn.xtmconf.object;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import kr.co.future.sslvpn.xtmconf.AttributeBuilder;
import kr.co.future.sslvpn.xtmconf.MarshalValue;
import kr.co.future.sslvpn.xtmconf.NodeWrapper;
import kr.co.future.sslvpn.xtmconf.Utils;
import kr.co.future.sslvpn.xtmconf.XtmConfig;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class IpAddress extends XtmConfig {
	private int num;
	private String cid;
	private String name; // 객체명
	private String desc; // 설명
	private List<String> ip = new ArrayList<String>(); // IPv4 주소

	@Override
	public String getXmlFilename() {
		return "object_ip_address.xml";
	}

	@Override
	public String getRootTagName() {
		return "object";
	}

	public static IpAddress parse(NodeWrapper nw) {
		if (!nw.isName("address"))
			return null;

		IpAddress addr = new IpAddress();
		addr.num = nw.intAttr("num");
		addr.cid = nw.attr("cid");

		for (NodeWrapper c : nw.children()) {
			if (c.isName("name"))
				addr.name = c.value();
			else if (c.isName("desc"))
				addr.desc = c.value();
			else if (c.isName("ip"))
				addr.ip.add(c.value());
		}

		return addr;
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

	public String getDesc() {
		return desc;
	}

	public void setDesc(String desc) {
		this.desc = desc;
	}

	public List<String> getIp() {
		return ip;
	}

	public void setIp(List<String> ip) {
		this.ip = ip;
	}

	@Override
	protected Element convertToElement(Document doc) {
		Element e = doc.createElement("address");

		e.setAttribute("num", String.valueOf(num));
		e.setAttribute("cid", cid);
		e.setAttribute("count", String.valueOf(ip.size()));
		appendChild(doc, e, "name", name);
		appendChild(doc, e, "desc", desc);

		for (String i : ip)
			appendChild(doc, e, "ip", i, new AttributeBuilder("type", Utils.ipType(i)));

		return e;
	}

	@Override
	public Map<String, Object> marshal() {
		Map<String, Object> m = new HashMap<String, Object>();
		m.put("num", num);
		m.put("cid", cid);
		m.put("name", name);
		m.put("desc", desc);
		List<Object> l = new ArrayList<Object>();
		for (String s : ip)
			l.add(new MarshalValue("value", s).put("type", Utils.ipType(s)).get());
		m.put("ip", l);

		return m;
	}

	@Override
	public String toString() {
		return "IpAddress [num=" + num + ", cid=" + cid + ", name=" + name + ", desc=" + desc + ", ip=" + ip + "]";
	}
}
