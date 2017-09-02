package kr.co.future.sslvpn.xtmconf.network;

import java.util.HashMap;
import java.util.Map;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import kr.co.future.sslvpn.xtmconf.NodeWrapper;
import kr.co.future.sslvpn.xtmconf.XtmConfig;

public class VirtualIpv6 extends XtmConfig {
	private int num;
	private String cid;
	private String iface; // 인터페이스
	private String ip; // 가상IPv6
	private int prefix; // Prefix

	@Override
	public String getXmlFilename() {
		return "network_virtual_ipv6.xml";
	}

	@Override
	public String getRootTagName() {
		return "network";
	}

	public static VirtualIpv6 parse(NodeWrapper nw) {
		if (!nw.isName("vip"))
			return null;

		VirtualIpv6 vi = new VirtualIpv6();
		vi.num = nw.intAttr("num");
		vi.cid = nw.attr("cid");

		for (NodeWrapper c : nw.children()) {
			if (c.isName("interface"))
				vi.iface = c.value();
			else if (c.isName("ip"))
				vi.ip = c.value();
			else if (c.isName("prefix"))
				vi.prefix = c.intValue();
		}

		return vi;
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

	public String getIface() {
		return iface;
	}

	public void setIface(String iface) {
		this.iface = iface;
	}

	public String getIp() {
		return ip;
	}

	public void setIp(String ip) {
		this.ip = ip;
	}

	public int getPrefix() {
		return prefix;
	}

	public void setPrefix(int prefix) {
		this.prefix = prefix;
	}

	@Override
	protected Element convertToElement(Document doc) {
		Element e = doc.createElement("vip");

		e.setAttribute("num", String.valueOf(num));
		e.setAttribute("cid", cid);
		appendChild(doc, e, "interface", iface);
		appendChild(doc, e, "ip", ip);
		appendChild(doc, e, "prefix", prefix);

		return e;
	}

	@Override
	public Map<String, Object> marshal() {
		Map<String, Object> m = new HashMap<String, Object>();
		m.put("num", num);
		m.put("cid", cid);
		m.put("interface", iface);
		m.put("ip", ip);
		m.put("prefix", prefix);

		return m;
	}

	@Override
	public String toString() {
		return "VirtualIpv6 [num=" + num + ", cid=" + cid + ", iface=" + iface + ", ip=" + ip + ", prefix=" + prefix
				+ "]";
	}
}
