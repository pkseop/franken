package kr.co.future.sslvpn.xtmconf.filtering;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import kr.co.future.sslvpn.xtmconf.NodeWrapper;
import kr.co.future.sslvpn.xtmconf.Utils;
import kr.co.future.sslvpn.xtmconf.XtmConfig;

public class FirewallIpv6Nat extends XtmConfig {
	public static enum Type {
		Masquerading, Nat;

		public static Type get(String str) {
			for (Type t : Type.values()) {
				if (t.toString().equals(str))
					return t;
			}
			return null;
		}

		@Override
		public String toString() {
			return name().toLowerCase();
		}
	}

	public static enum NatType {
		V6toV4, V4toV6;

		public static NatType get(String str) {
			for (NatType n : NatType.values()) {
				if (n.toString().equals(str))
					return n;
			}
			return null;
		}

		@Override
		public String toString() {
			return name().toLowerCase();
		}
	}

	private Type type;
	private String masqPrefix; // PREFIX
	private int num;
	private String cid;
	private boolean use; // 동작
	private NatType natType; // 종류
	private Address ip; // 변경전주소
	private Service service; // 서비스
	private Address xip; // 변경후주소
	private String desc; // 기타정보

	@Override
	public String getXmlFilename() {
		return "firewall_IPv6_nat.xml";
	}

	@Override
	public String getRootTagName() {
		return "policy";
	}

	public static boolean hasCid(String cid) {
		List<FirewallIpv6Nat> objs = XtmConfig.readConfig(FirewallIpv6Nat.class);
		for (FirewallIpv6Nat fn : objs) {
			if (fn.ip != null && cid.equals(fn.ip.getCid()))
				return true;
			if (fn.service != null && cid.equals(fn.service.getCid()))
				return true;
			if (fn.xip != null && cid.equals(fn.xip.getCid()))
				return true;
		}
		return false;
	}

	public static void updateCid(String cid, String newName) {
		List<FirewallIpv6Nat> objs = XtmConfig.readConfig(FirewallIpv6Nat.class);
		boolean update = false;
		for (FirewallIpv6Nat fn : objs) {
			if (fn.ip != null && cid.equals(fn.ip.getCid())) {
				fn.ip.setName(newName);
				update = true;
			}
			if (fn.service != null && cid.equals(fn.service.getCid())) {
				fn.service.setName(newName);
				update = true;
			}
			if (fn.xip != null && cid.equals(fn.xip.getCid())) {
				fn.xip.setName(newName);
				update = true;
			}
		}

		if (update)
			XtmConfig.writeConfig(FirewallIpv6Nat.class, objs);
	}

	public static FirewallIpv6Nat parse(NodeWrapper nw) {
		if (Type.get(nw.name()) == null)
			return null;

		FirewallIpv6Nat fin = new FirewallIpv6Nat();
		fin.type = Type.get(nw.name());

		if (fin.type == Type.Masquerading) {
			for (NodeWrapper c : nw.children()) {
				if (c.isName("prefix"))
					fin.masqPrefix = c.value();
			}
		} else if (fin.type == Type.Nat) {
			fin.num = nw.intAttr("num");
			fin.cid = nw.attr("cid");
			fin.use = nw.boolAttr("use");
			fin.natType = NatType.get(nw.attr("type"));

			for (NodeWrapper c : nw.children()) {
				if (c.isName("ip"))
					fin.ip = new Address(c);
				else if (c.isName("service"))
					fin.service = new Service(c);
				else if (c.isName("xip"))
					fin.xip = new Address(c);
				else if (c.isName("desc"))
					fin.desc = c.value();
			}
		}

		return fin;
	}

	public Type getType() {
		return type;
	}

	public void setType(Type type) {
		this.type = type;
	}

	public String getMasqPrefix() {
		return masqPrefix;
	}

	public void setMasqPrefix(String masqPrefix) {
		this.masqPrefix = masqPrefix;
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

	public boolean isUse() {
		return use;
	}

	public void setUse(boolean use) {
		this.use = use;
	}

	public NatType getNatType() {
		return natType;
	}

	public void setNatType(NatType natType) {
		this.natType = natType;
	}

	public Address getIp() {
		return ip;
	}

	public void setIp(Address ip) {
		this.ip = ip;
	}

	public Service getService() {
		return service;
	}

	public void setService(Service service) {
		this.service = service;
	}

	public Address getXip() {
		return xip;
	}

	public void setXip(Address xip) {
		this.xip = xip;
	}

	public String getDesc() {
		return desc;
	}

	public void setDesc(String desc) {
		this.desc = desc;
	}

	@Override
	protected Element convertToElement(Document doc) {
		Element e = doc.createElement(type.toString());

		if (type == Type.Masquerading)
			appendChild(doc, e, "prefix", masqPrefix);
		else if (type == Type.Nat) {
			e.setAttribute("num", String.valueOf(num));
			e.setAttribute("cid", cid);
			e.setAttribute("use", Utils.bool(use));
			e.setAttribute("type", natType.toString());
			e.appendChild(ip.toElement(doc, "ip"));
			e.appendChild(service.toElement(doc, "service"));
			e.appendChild(xip.toElement(doc, "xip"));
			appendChild(doc, e, "desc", desc);
		}

		return e;
	}

	@Override
	public Map<String, Object> marshal() {
		Map<String, Object> m = new HashMap<String, Object>();
		m.put("type", type);
		if (type == Type.Masquerading)
			m.put("prefix", masqPrefix);
		else if (type == Type.Nat) {
			m.put("num", num);
			m.put("cid", cid);
			m.put("use", use);
			m.put("nat_type", natType);
			m.put("ip", ip.marshal());
			m.put("service", service.marshal());
			m.put("xip", xip.marshal());
			m.put("desc", desc);
		}

		return m;
	}

	@Override
	public String toString() {
		return "FirewallIpv6Nat [type=" + type + ", masqPrefix=" + masqPrefix + ", num=" + num + ", cid=" + cid
				+ ", use=" + use + ", natType=" + natType + ", ip=" + ip + ", service=" + service + ", xip=" + xip
				+ ", desc=" + desc + "]";
	}
}
