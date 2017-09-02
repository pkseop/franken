package kr.co.future.sslvpn.xtmconf.network;

import java.util.HashMap;
import java.util.Map;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import kr.co.future.sslvpn.xtmconf.AttributeBuilder;
import kr.co.future.sslvpn.xtmconf.MarshalValue;
import kr.co.future.sslvpn.xtmconf.NodeWrapper;
import kr.co.future.sslvpn.xtmconf.Utils;
import kr.co.future.sslvpn.xtmconf.XtmConfig;

public class Interface extends XtmConfig {
	public static enum Type {
		Domain, Interface;

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

	public static enum IfaceType {
		None, Static, DHCP, PPPoE, HSDPA
	}

	public static enum Duplex {
		Auto, Full, Half
	}

	public static enum Speed {
		Auto, _10MBps, _100MBps, _1GBps, _10GBps;

		public static Speed get(String str) {
			for (Speed s : Speed.values()) {
				if (s.toString().equals(str))
					return s;
			}
			return null;
		}

		@Override
		public String toString() {
			return name().replace("_", "");
		}
	}

	public static enum InterfaceType {
		Internal, DMZ, External
	}

	public static enum Modem {
		None, Samsung, Hyundai
	}

	private Type type;
	private String mainDomain; // 주 도메인 네임서버
	private String subDomain; // 보조 도메인 네임서버
	private String ifaceName; // Interface
	private IfaceType ifaceType; // Type
	private Duplex duplex; // Duplex
	private Speed speed; // Speed
	private Integer mtu; // MTU
	private InterfaceType mode; // 구분
	private boolean frag; // Fragment
	private boolean multipath; // Multipath
	private Integer band; // QoS대역폭
	private Integer mss; // MSS
	private String ip; // IPv4 IP/Netmask
	private String ipv6; // Ipv6 IP/Prefix
	private String adslId; // PPPoE-ID
	private String adslPass; // PPPoE-Password
	private Modem modem; // PPPoE-Modem

	@Override
	public String getXmlFilename() {
		return "network_interface.xml";
	}

	@Override
	public String getRootTagName() {
		return "network";
	}

	public static Interface parse(NodeWrapper nw) {
		if (!nw.isName("domain") && !nw.isName("interface"))
			return null;

		Interface i = new Interface();
		i.type = Type.get(nw.name());

		if (i.type == Type.Domain) {
			for (NodeWrapper c : nw.children()) {
				if (c.isName("main"))
					i.mainDomain = c.value();
				else if (c.isName("sub"))
					i.subDomain = c.value();
			}
		} else if (i.type == Type.Interface) {
			for (NodeWrapper c : nw.children()) {
				if (c.isName("name"))
					i.ifaceName = c.value();
				else if (c.isName("setting")) {
					i.ifaceType = IfaceType.valueOf(c.attr("type"));
					i.duplex = Duplex.valueOf(c.attr("duplex"));
					i.speed = Speed.get(c.attr("speed"));
					i.mtu = (!c.attr("mtusize").equals("Default")) ? c.intAttr("mtusize") : null;
					i.mode = InterfaceType.valueOf(c.attr("mode"));
					i.frag = c.boolAttr("chk_frag");
					i.multipath = c.boolAttr("chk_multipath");
					i.band = c.intAttr("band");
					i.mss = c.intAttr("mss");
				} else if (c.isName("ip")) {
					if (c.attr("version") == null)
						i.ip = c.value();
					else
						i.ipv6 = c.value();
				} else if (c.isName("adsl")) {
					for (NodeWrapper a : c.children()) {
						if (a.isName("id"))
							i.adslId = a.value();
						else if (a.isName("password"))
							i.adslPass = a.value();
						else if (a.isName("modem"))
							i.modem = Modem.valueOf(a.value());
					}
				}
			}
		}

		return i;
	}

	public Type getType() {
		return type;
	}

	public void setType(Type type) {
		this.type = type;
	}

	public String getMainDomain() {
		return mainDomain;
	}

	public void setMainDomain(String mainDomain) {
		this.mainDomain = mainDomain;
	}

	public String getSubDomain() {
		return subDomain;
	}

	public void setSubDomain(String subDomain) {
		this.subDomain = subDomain;
	}

	public String getIfaceName() {
		return ifaceName;
	}

	public void setIfaceName(String ifaceName) {
		this.ifaceName = ifaceName;
	}

	public IfaceType getIfaceType() {
		return ifaceType;
	}

	public void setIfaceType(IfaceType ifaceType) {
		this.ifaceType = ifaceType;
	}

	public Duplex getDuplex() {
		return duplex;
	}

	public void setDuplex(Duplex duplex) {
		this.duplex = duplex;
	}

	public Speed getSpeed() {
		return speed;
	}

	public void setSpeed(Speed speed) {
		this.speed = speed;
	}

	public Integer getMtu() {
		return mtu;
	}

	public void setMtu(Integer mtu) {
		this.mtu = mtu;
	}

	public InterfaceType getMode() {
		return mode;
	}

	public void setMode(InterfaceType mode) {
		this.mode = mode;
	}

	public boolean isFrag() {
		return frag;
	}

	public void setFrag(boolean frag) {
		this.frag = frag;
	}

	public boolean isMultipath() {
		return multipath;
	}

	public void setMultipath(boolean multipath) {
		this.multipath = multipath;
	}

	public Integer getBand() {
		return band;
	}

	public void setBand(Integer band) {
		this.band = band;
	}

	public Integer getMss() {
		return mss;
	}

	public void setMss(Integer mss) {
		this.mss = mss;
	}

	public String getIp() {
		return ip;
	}

	public void setIp(String ip) {
		this.ip = ip;
	}

	public String getIpv6() {
		return ipv6;
	}

	public void setIpv6(String ipv6) {
		this.ipv6 = ipv6;
	}

	public String getAdslId() {
		return adslId;
	}

	public void setAdslId(String adslId) {
		this.adslId = adslId;
	}

	public String getAdslPass() {
		return adslPass;
	}

	public void setAdslPass(String adslPass) {
		this.adslPass = adslPass;
	}

	public Modem getModem() {
		return modem;
	}

	public void setModem(Modem modem) {
		this.modem = modem;
	}

	@Override
	protected Element convertToElement(Document doc) {
		Element e = doc.createElement(type.toString());

		if (type == Type.Domain) {
			appendChild(doc, e, "main", mainDomain);
			appendChild(doc, e, "sub", subDomain);
		} else if (type == Type.Interface) {
			appendChild(doc, e, "name", ifaceName);
			AttributeBuilder settingAttr = new AttributeBuilder("type", ifaceType).put("duplex", duplex).put("speed", speed)
					.put("mtusize", (mtu != null) ? mtu : "Default").put("mode", mode).put("chk_frag", frag)
					.put("chk_multipath", multipath).put("band", band).put("mss", mss);
			appendChild(doc, e, "setting", null, settingAttr);
			appendChild(doc, e, "ip", ip, new AttributeBuilder("type", (ip != null) ? Utils.ipType(ip) : (ipv6 != null ? "single"
					: null)));
			appendChild(doc, e, "ip", ipv6, new AttributeBuilder("type", (ipv6 != null) ? Utils.ipType(ipv6)
					: (ip != null ? "single" : null)).put("version", "v6"));
			Element adsl = appendChild(doc, e, "adsl", null);
			appendChild(doc, adsl, "id", adslId);
			appendChild(doc, adsl, "password", adslPass);
			appendChild(doc, adsl, "modem", modem);
		}

		return e;
	}

	@Override
	public Map<String, Object> marshal() {
		Map<String, Object> m = new HashMap<String, Object>();
		m.put("type", type);

		if (type == Type.Domain) {
			m.put("main", mainDomain);
			m.put("sub", subDomain);
		} else if (type == Type.Interface) {
			m.put("name", ifaceName);
			m.put("type", ifaceType);
			m.put("duplex", duplex);
			m.put("speed", speed);
			m.put("mtu", mtu);
			m.put("mode", mode);
			m.put("frag", frag);
			m.put("multipath", multipath);
			m.put("band", band);
			m.put("mss", mss);
			m.put("ip", new MarshalValue("value", ip).put("version", Utils.ipVersion(ip)).put("type", Utils.ipType(ip)).get());
			m.put("ipv6", new MarshalValue("value", ipv6).put("version", Utils.ipVersion(ipv6)).put("type", Utils.ipType(ipv6))
					.get());
			m.put("adsl", new MarshalValue("id", adslId).put("password", adslPass).put("modem", modem).get());
		}

		return m;
	}

	@Override
	public String toString() {
		return "Interface [type=" + type + ", mainDomain=" + mainDomain + ", subDomain=" + subDomain + ", ifaceName=" + ifaceName
				+ ", ifaceType=" + ifaceType + ", duplex=" + duplex + ", speed=" + speed + ", mtu=" + mtu + ", mode=" + mode
				+ ", frag=" + frag + ", multipath=" + multipath + ", band=" + band + ", mss=" + mss + ", ip=" + ip + ", ipv6="
				+ ipv6 + ", adslId=" + adslId + ", adslPass=" + adslPass + ", modem=" + modem + "]";
	}
}
