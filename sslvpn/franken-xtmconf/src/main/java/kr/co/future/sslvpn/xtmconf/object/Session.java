package kr.co.future.sslvpn.xtmconf.object;

import java.util.HashMap;
import java.util.Map;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import kr.co.future.sslvpn.xtmconf.NodeWrapper;
import kr.co.future.sslvpn.xtmconf.XtmConfig;

public class Session extends XtmConfig {
	private int num;
	private String cid;
	private String name; // 객체명
	private String desc; // 설명
	private int action; // 신규세션/초(0), 사용자별 동시세션(1)
	private int limit; // 허용세션 수

	@Override
	public String getXmlFilename() {
		return "object_session.xml";
	}

	@Override
	public String getRootTagName() {
		return "object";
	}

	public static Session parse(NodeWrapper nw) {
		if (!nw.isName("session"))
			return null;

		Session s = new Session();
		s.num = nw.intAttr("num");
		s.cid = nw.attr("cid");

		for (NodeWrapper c : nw.children()) {
			if (c.isName("name"))
				s.name = c.value();
			else if (c.isName("desc"))
				s.desc = c.value();
			else if (c.isName("action"))
				s.action = c.intValue();
			else if (c.isName("limit"))
				s.limit = c.intValue();
		}

		return s;
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

	public int getAction() {
		return action;
	}

	public void setAction(int action) {
		this.action = action;
	}

	public int getLimit() {
		return limit;
	}

	public void setLimit(int limit) {
		this.limit = limit;
	}

	@Override
	protected Element convertToElement(Document doc) {
		Element e = doc.createElement("session");

		e.setAttribute("num", String.valueOf(num));
		e.setAttribute("cid", cid);
		appendChild(doc, e, "name", name);
		appendChild(doc, e, "desc", desc);
		appendChild(doc, e, "action", action);
		appendChild(doc, e, "limit", limit);

		return e;
	}

	@Override
	public Map<String, Object> marshal() {
		Map<String, Object> m = new HashMap<String, Object>();
		m.put("num", num);
		m.put("cid", cid);
		m.put("name", name);
		m.put("desc", desc);
		m.put("action", action);
		m.put("limit", limit);

		return m;
	}

	@Override
	public String toString() {
		return "Session [num=" + num + ", cid=" + cid + ", name=" + name + ", desc=" + desc + ", action=" + action
				+ ", limit=" + limit + "]";
	}
}
