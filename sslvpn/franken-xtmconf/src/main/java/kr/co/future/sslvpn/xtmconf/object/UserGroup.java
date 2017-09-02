package kr.co.future.sslvpn.xtmconf.object;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import kr.co.future.sslvpn.xtmconf.NodeWrapper;
import kr.co.future.sslvpn.xtmconf.XtmConfig;

public class UserGroup extends XtmConfig {
	private int num;
	private String cid;
	private String name; // 그룹명
	private String desc; // 설명
	private List<String> member = new ArrayList<String>(); // 그룹설정

	@Override
	public String getXmlFilename() {
		return "object_user_group.xml";
	}

	@Override
	public String getRootTagName() {
		return "object";
	}

	public static boolean hasCid(String cid) {
		List<UserGroup> objs = XtmConfig.readConfig(UserGroup.class);
		for (UserGroup ug : objs) {
			if (ug.member.contains(cid))
				return true;
		}
		return false;
	}

	public static UserGroup parse(NodeWrapper nw) {
		if (!nw.isName("group"))
			return null;

		UserGroup ug = new UserGroup();
		ug.num = nw.intAttr("num");
		ug.cid = nw.attr("cid");

		for (NodeWrapper c : nw.children()) {
			if (c.isName("name"))
				ug.name = c.value();
			else if (c.isName("desc"))
				ug.desc = c.value();
			else if (c.isName("member"))
				ug.member.add(c.value());
		}

		return ug;
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

		e.setAttribute("num", String.valueOf(num));
		e.setAttribute("cid", cid);
		e.setAttribute("count", String.valueOf(member.size()));
		appendChild(doc, e, "name", name);
		appendChild(doc, e, "desc", desc);

		for (String m : member)
			appendChild(doc, e, "member", m);

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
		return "UserGroup [num=" + num + ", cid=" + cid + ", name=" + name + ", desc=" + desc + ", member=" + member
				+ "]";
	}
}
