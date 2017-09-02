package kr.co.future.sslvpn.xtmconf.object;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import kr.co.future.sslvpn.xtmconf.NodeWrapper;
import kr.co.future.sslvpn.xtmconf.XtmConfig;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class IpGroup extends XtmConfig {
	private int num;
	private String cid;
	private String name; // 그룹명
	private String desc; // 설명
	private List<String> member = new ArrayList<String>(); // IpAddress.cid

	@Override
	public String getXmlFilename() {
		return "object_ip_group.xml";
	}

	@Override
	public String getRootTagName() {
		return "object";
	}

	public static boolean hasCid(String cid) {
		List<IpGroup> objs = XtmConfig.readConfig(IpGroup.class);
		for (IpGroup ig : objs) {
			if (ig.member.contains(cid))
				return true;
		}
		return false;
	}

	public static IpGroup parse(NodeWrapper nw) {
		if (!nw.isName("group"))
			return null;

		IpGroup group = new IpGroup();
		group.num = nw.intAttr("num");
		group.cid = nw.attr("cid");

		for (NodeWrapper c : nw.children()) {
			if (c.isName("desc"))
				group.desc = c.value();
			else if (c.isName("name"))
				group.name = c.value();
			else if (c.isName("member"))
				group.member.add(c.value());
		}

		return group;
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

	public String getDesc() {
		return desc;
	}

	public void setDesc(String desc) {
		this.desc = desc;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
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
		appendChild(doc, e, "desc", desc);

		for (String cid : member)
			appendChild(doc, e, "member", cid);

		return e;
	}

	@Override
	public Map<String, Object> marshal() {
		Map<String, Object> m = new HashMap<String, Object>();
		m.put("num", num);
		m.put("cid", cid);
		m.put("name", name);
		m.put("desc", desc);
		m.put("member", member);

		return m;
	}

	@Override
	public String toString() {
		return "IpGroup [num=" + num + ", cid=" + cid + ", name=" + name + ", desc=" + desc + ", member=" + member
				+ "]";
	}
}
