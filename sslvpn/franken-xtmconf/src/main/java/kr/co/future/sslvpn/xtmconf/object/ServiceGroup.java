package kr.co.future.sslvpn.xtmconf.object;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import kr.co.future.sslvpn.xtmconf.NodeWrapper;
import kr.co.future.sslvpn.xtmconf.XtmConfig;

public class ServiceGroup extends XtmConfig {
	private int num;
	private String cid;
	private String name; // 그룹명
	private String desc; // 설명
	private List<String> member = new ArrayList<String>(); // 멤버서비스객체

	@Override
	public String getXmlFilename() {
		return "object_service_group.xml";
	}

	@Override
	public String getRootTagName() {
		return "object";
	}

	public static boolean hasCid(String cid) {
		List<ServiceGroup> objs = XtmConfig.readConfig(ServiceGroup.class);
		for (ServiceGroup sg : objs) {
			if (sg.member.contains(cid))
				return true;
		}
		return false;
	}

	public static ServiceGroup parse(NodeWrapper nw) {
		// if (!nw.isName("group") || !getAttr(nw, "type").equals("service"))
		if (!nw.isName("group"))
			return null;

		ServiceGroup sg = new ServiceGroup();
		sg.num = nw.intAttr("num");
		sg.cid = nw.attr("cid");

		for (NodeWrapper c : nw.children()) {
			if (c.isName("name"))
				sg.name = c.value();
			else if (c.isName("desc"))
				sg.desc = c.value();
			else if (c.isName("member"))
				sg.member.add(c.value());
		}

		return sg;
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

	public List<String> getMember() {
		return member;
	}

	public void setMember(List<String> member) {
		this.member = member;
	}

	@Override
	protected Element convertToElement(Document doc) {
		Element e = doc.createElement("group");

		e.setAttribute("type", "service");
		e.setAttribute("num", String.valueOf(num));
		e.setAttribute("cid", cid);
		e.setAttribute("count", String.valueOf(member.size()));
		appendChild(doc, e, "name", name);
		appendChild(doc, e, "desc", desc);
		for (String s : member)
			appendChild(doc, e, "member", s);

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
		return "ServiceGroup [num=" + num + ", cid=" + cid + ", name=" + name + ", desc=" + desc + ", member=" + member
				+ "]";
	}
}
