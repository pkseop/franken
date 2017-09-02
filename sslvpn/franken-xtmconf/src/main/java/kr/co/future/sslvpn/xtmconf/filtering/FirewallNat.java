package kr.co.future.sslvpn.xtmconf.filtering;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import kr.co.future.sslvpn.xtmconf.NodeWrapper;
import kr.co.future.sslvpn.xtmconf.Utils;
import kr.co.future.sslvpn.xtmconf.XtmConfig;

public class FirewallNat extends XtmConfig {
	public static enum NatType {
		DNAT, SNAT, FNAT, XNAT, TNAT
	}

	private int num;
	private String cid;
	private NatType type; // 종류
	private boolean use; // 동작
	private Address src; // 출발주소
	private Address dest; // 도착주소
	private Service service; // 서비스
	private String iface; // 인터페이스
	private Address xsrc; // 변경 출발주소
	private Address xdest; // 변경 도착주소
	private Service xservice; // 변경 서비스
	private String desc; // 기타정보

	@Override
	public String getXmlFilename() {
		return "firewall_nat.xml";
	}

	@Override
	public String getRootTagName() {
		return "policy";
	}

	public static boolean hasCid(String cid) {
		List<FirewallNat> objs = XtmConfig.readConfig(FirewallNat.class);
		for (FirewallNat fn : objs) {
			if (fn.src != null && cid.equals(fn.src.getCid()))
				return true;
			if (fn.dest != null && cid.equals(fn.dest.getCid()))
				return true;
			if (fn.service != null && cid.equals(fn.service.getCid()))
				return true;
			if (fn.xsrc != null && cid.equals(fn.xsrc.getCid()))
				return true;
			if (fn.xdest != null && cid.equals(fn.xdest.getCid()))
				return true;
			if (fn.xservice != null && cid.equals(fn.xservice.getCid()))
				return true;
		}
		return false;
	}

	public static void updateCid(String cid, String newName) {
		List<FirewallNat> objs = XtmConfig.readConfig(FirewallNat.class);
		boolean update = false;
		for (FirewallNat fn : objs) {
			if (fn.src != null && cid.equals(fn.src.getCid())) {
				fn.src.setName(newName);
				update = true;
			}
			if (fn.dest != null && cid.equals(fn.dest.getCid())) {
				fn.dest.setName(newName);
				update = true;
			}
			if (fn.service != null && cid.equals(fn.service.getCid())) {
				fn.service.setName(newName);
				update = true;
			}
			if (fn.xsrc != null && cid.equals(fn.xsrc.getCid())) {
				fn.xsrc.setName(newName);
				update = true;
			}
			if (fn.xdest != null && cid.equals(fn.xdest.getCid())) {
				fn.xdest.setName(newName);
				update = true;
			}
			if (fn.xservice != null && cid.equals(fn.xservice.getCid())) {
				fn.xservice.setName(newName);
				update = true;
			}
		}

		if (update)
			XtmConfig.writeConfig(FirewallNat.class, objs);
	}

	public static FirewallNat parse(NodeWrapper nw) {
		if (!nw.isName("nat"))
			return null;

		FirewallNat fn = new FirewallNat();
		fn.num = nw.intAttr("num");
		fn.cid = nw.attr("cid");
		fn.type = NatType.valueOf(nw.attr("type"));
		fn.use = nw.boolAttr("use");

		for (NodeWrapper c : nw.children()) {
			if (c.isName("src"))
				fn.src = new Address(c);
			else if (c.isName("dest"))
				fn.dest = new Address(c);
			else if (c.isName("service"))
				fn.service = new Service(c);
			else if (c.isName("interface"))
				fn.iface = c.value();
			else if (c.isName("xsrc"))
				fn.xsrc = new Address(c);
			else if (c.isName("xdest"))
				fn.xdest = new Address(c);
			else if (c.isName("xservice"))
				fn.xservice = new Service(c);
			else if (c.isName("desc"))
				fn.desc = c.value();
		}

		return fn;
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

	public NatType getType() {
		return type;
	}

	public void setType(NatType type) {
		this.type = type;
	}

	public boolean isUse() {
		return use;
	}

	public void setUse(boolean use) {
		this.use = use;
	}

	public Address getSrc() {
		return src;
	}

	public void setSrc(Address src) {
		this.src = src;
	}

	public Address getDest() {
		return dest;
	}

	public void setDest(Address dest) {
		this.dest = dest;
	}

	public Service getService() {
		return service;
	}

	public void setService(Service service) {
		this.service = service;
	}

	public String getIface() {
		return iface;
	}

	public void setIface(String iface) {
		this.iface = iface;
	}

	public Address getXsrc() {
		return xsrc;
	}

	public void setXsrc(Address xsrc) {
		this.xsrc = xsrc;
	}

	public Address getXdest() {
		return xdest;
	}

	public void setXdest(Address xdest) {
		this.xdest = xdest;
	}

	public Service getXservice() {
		return xservice;
	}

	public void setXservice(Service xservice) {
		this.xservice = xservice;
	}

	public String getDesc() {
		return desc;
	}

	public void setDesc(String desc) {
		this.desc = desc;
	}

	@Override
	protected Element convertToElement(Document doc) {
		Element e = doc.createElement("nat");

		e.setAttribute("num", String.valueOf(num));
		e.setAttribute("cid", cid);
		e.setAttribute("type", type.toString());
		e.setAttribute("use", Utils.bool(use));
		e.appendChild(src.toElement(doc, "src"));
		e.appendChild(dest.toElement(doc, "dest"));
		e.appendChild(service.toElement(doc, "service"));
		appendChild(doc, e, "interface", iface);
		e.appendChild(xsrc.toElement(doc, "xsrc"));
		e.appendChild(xdest.toElement(doc, "xdest"));
		e.appendChild(xservice.toElement(doc, "xservice"));
		appendChild(doc, e, "desc", desc);

		return e;
	}

	@Override
	public Map<String, Object> marshal() {
		Map<String, Object> m = new HashMap<String, Object>();
		m.put("num", num);
		m.put("cid", cid);
		m.put("type", type);
		m.put("use", use);
		m.put("src", src.marshal());
		m.put("dest", dest.marshal());
		m.put("service", service.marshal());
		m.put("interface", iface);
		m.put("xsrc", xsrc.marshal());
		m.put("xdest", xdest.marshal());
		m.put("xservice", xservice.marshal());
		m.put("desc", desc);

		return m;
	}

	@Override
	public String toString() {
		return "FirewallNat [num=" + num + ", cid=" + cid + ", type=" + type + ", use=" + use + ", src=" + src
				+ ", dest=" + dest + ", service=" + service + ", iface=" + iface + ", xsrc=" + xsrc + ", xdest="
				+ xdest + ", xservice=" + xservice + ", desc=" + desc + "]";
	}
}
