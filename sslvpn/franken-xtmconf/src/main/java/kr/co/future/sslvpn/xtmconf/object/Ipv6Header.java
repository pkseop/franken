package kr.co.future.sslvpn.xtmconf.object;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import kr.co.future.sslvpn.xtmconf.AttributeBuilder;
import kr.co.future.sslvpn.xtmconf.MarshalValue;
import kr.co.future.sslvpn.xtmconf.NodeWrapper;
import kr.co.future.sslvpn.xtmconf.XtmConfig;

public class Ipv6Header extends XtmConfig {
	private int num;
	private String cid;
	private String name; // 객체명
	private String desc; // 설명
	private boolean hop; // 홉바이홉 헤더
	private boolean dest; // 목적지 옵션 헤더
	private boolean ah; // 인증 헤더
	private boolean esp; // ESP 헤더
	private boolean fragment; // 프래그먼트 헤더
	private Integer fragmentSize; // 패킷 최소 크기
	private boolean route; // 라우팅 헤더
	private Boolean isRouteAddress;
	private String routeCid;
	private String routeName;

	@Override
	public String getXmlFilename() {
		return "object_ipv6_header.xml";
	}

	@Override
	public String getRootTagName() {
		return "object";
	}

	public static boolean hasCid(String cid) {
		List<Ipv6Header> objs = XtmConfig.readConfig(Ipv6Header.class);
		for (Ipv6Header ih : objs) {
			if (ih.routeCid.equals(cid))
				return true;
		}
		return false;
	}

	public static void updateCid(String cid, String newName) {
		List<Ipv6Header> objs = XtmConfig.readConfig(Ipv6Header.class);
		boolean update = false;
		for (Ipv6Header ih : objs) {
			if (ih.routeCid.equals(cid)) {
				ih.routeName = newName;
				update = true;
			}
		}

		if (update)
			XtmConfig.writeConfig(Ipv6Header.class, objs);
	}

	public static Ipv6Header parse(NodeWrapper nw) {
		if (!nw.isName("header"))
			return null;

		Ipv6Header header = new Ipv6Header();
		header.num = nw.intAttr("num");
		header.cid = nw.attr("cid");

		for (NodeWrapper c : nw.children()) {
			if (c.isName("name"))
				header.name = c.value();
			else if (c.isName("desc"))
				header.desc = c.value();
			else if (c.isName("setting")) {
				header.hop = c.boolAttr("chk_hop");
				header.dest = c.boolAttr("chk_dest");
				header.ah = c.boolAttr("chk_ah");
				header.esp = c.boolAttr("chk_esp");
			} else if (c.isName("fragment")) {
				header.fragment = c.boolAttr("chk_use");
				header.fragmentSize = c.intValue();
			} else if (c.isName("route")) {
				header.route = c.boolAttr("chk_use");
				for (NodeWrapper m : c.children()) {
					if (m.isName("member")) {
						header.isRouteAddress = m.attr("otype").equals("single");
						header.routeCid = m.attr("cid");
						header.routeName = m.value();
					}
				}
			}
		}

		return header;
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

	public boolean isHop() {
		return hop;
	}

	public void setHop(boolean hop) {
		this.hop = hop;
	}

	public boolean isDest() {
		return dest;
	}

	public void setDest(boolean dest) {
		this.dest = dest;
	}

	public boolean isAh() {
		return ah;
	}

	public void setAh(boolean ah) {
		this.ah = ah;
	}

	public boolean isEsp() {
		return esp;
	}

	public void setEsp(boolean esp) {
		this.esp = esp;
	}

	public boolean isFragment() {
		return fragment;
	}

	public void setFragment(boolean fragment) {
		this.fragment = fragment;
	}

	public Integer getFragmentSize() {
		return fragmentSize;
	}

	public void setFragmentSize(Integer fragmentSize) {
		this.fragmentSize = fragmentSize;
	}

	public boolean isRoute() {
		return route;
	}

	public void setRoute(boolean route) {
		this.route = route;
	}

	public Boolean isRouteAddress() {
		return isRouteAddress;
	}

	public void setRouteAddress(Boolean isRouteAddress) {
		this.isRouteAddress = isRouteAddress;
	}

	public String getRouteCid() {
		return routeCid;
	}

	public void setRouteCid(String routeCid) {
		this.routeCid = routeCid;
	}

	public String getRouteName() {
		return routeName;
	}

	public void setRouteName(String routeName) {
		this.routeName = routeName;
	}

	@Override
	protected Element convertToElement(Document doc) {
		Element e = doc.createElement("header");

		e.setAttribute("num", String.valueOf(num));
		e.setAttribute("cid", cid);
		appendChild(doc, e, "name", name);
		appendChild(doc, e, "desc", desc);
		AttributeBuilder settingAttr = new AttributeBuilder("chk_hop", hop).put("chk_dest", dest).put("chk_ah", ah)
				.put("chk_esp", esp);
		appendChild(doc, e, "setting", null, settingAttr);
		appendChild(doc, e, "fragment", fragmentSize, new AttributeBuilder("chk_use", fragment));

		Element r = appendChild(doc, e, "route", null, new AttributeBuilder("chk_use", route));
		if (route) {
			appendChild(doc, r, "member", routeName,
					new AttributeBuilder("cid", routeCid).put("otype", isRouteAddress ? "single" : "group"));
		} else
			appendChild(doc, r, "member", null, new AttributeBuilder("cid", "").put("otype", ""));

		return e;
	}

	@Override
	public Map<String, Object> marshal() {
		Map<String, Object> m = new HashMap<String, Object>();
		m.put("num", num);
		m.put("cid", cid);
		m.put("name", name);
		m.put("desc", desc);
		m.put("hop", hop);
		m.put("dest", dest);
		m.put("ah", ah);
		m.put("esp", esp);
		m.put("fragment", new MarshalValue("use", fragment).put("size", fragmentSize).get());
		m.put("route",
				new MarshalValue("use", route).put("cid", routeCid).put("name", routeName)
						.put("type", isRouteAddress ? "single" : "group").get());

		return m;
	}

	@Override
	public String toString() {
		return "Ipv6Header [num=" + num + ", cid=" + cid + ", name=" + name + ", desc=" + desc + ", hop=" + hop
				+ ", dest=" + dest + ", ah=" + ah + ", esp=" + esp + ", fragment=" + fragment + ", fragmentSize="
				+ fragmentSize + ", route=" + route + ", isRouteAddress=" + isRouteAddress + ", routeCid=" + routeCid
				+ ", routeName=" + routeName + "]";
	}
}
