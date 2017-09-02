package kr.co.future.sslvpn.xtmconf.ssl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import kr.co.future.sslvpn.xtmconf.AttributeBuilder;
import kr.co.future.sslvpn.xtmconf.NodeWrapper;
import kr.co.future.sslvpn.xtmconf.XtmConfig;

public class AccessGroup extends XtmConfig {
	private int num;
	private String cid;
	private String name; // 이름
	private String ip; // IP
	private String netmask; // Netmask
	private List<String> member = new ArrayList<String>(); // 접근서버

	@Override
	public String getXmlFilename() {
		return "ssl_access_group.xml";
	}

	@Override
	public String getRootTagName() {
		return "ssl";
	}

	public static AccessGroup parse(NodeWrapper nw) {
		if (!nw.isName("group"))
			return null;

		AccessGroup ag = new AccessGroup();
		ag.num = nw.intAttr("num");
		ag.cid = nw.attr("cid");

		for (NodeWrapper c : nw.children()) {
			if (c.isName("name"))
				ag.name = c.value();
			else if (c.isName("ip"))
				ag.ip = c.value();
			else if (c.isName("netmask"))
				ag.netmask = c.value();
			else if (c.isName("member"))
				ag.member.add(c.attr("cid"));
		}

		return ag;
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

	public String getIp() {
		return ip;
	}

	public void setIp(String ip) {
		this.ip = ip;
	}

	public String getNetmask() {
		return netmask;
	}

	public void setNetmask(String netmask) {
		this.netmask = netmask;
	}

	public List<String> getMember() {
		return member;
	}

	public void setMember(List<String> member) {
		this.member = member;
	}

	@Override
	protected Element convertToElement(Document doc) {
		Element e = doc.createElement("group");

		e.setAttribute("num", String.valueOf(num));
		e.setAttribute("cid", cid);
		e.setAttribute("count", String.valueOf(member.size()));
		appendChild(doc, e, "name", name);
		appendChild(doc, e, "ip", ip);
		appendChild(doc, e, "netmask", netmask);

		for (String m : member)
			appendChild(doc, e, "member", null, new AttributeBuilder("cid", m));

		return e;
	}

	@Override
	public Map<String, Object> marshal() {
		Map<String, Object> m = new HashMap<String, Object>();
		m.put("num", num);
		m.put("cid", cid);
		m.put("name", name);
		m.put("ip", ip);
		m.put("netmask", netmask);
		m.put("member", member);

		return m;
	}

	@Override
	public String toString() {
		return "AccessGroup [num=" + num + ", cid=" + cid + ", name=" + name + ", ip=" + ip + ", netmask=" + netmask
				+ ", member=" + member + "]";
	}
}
