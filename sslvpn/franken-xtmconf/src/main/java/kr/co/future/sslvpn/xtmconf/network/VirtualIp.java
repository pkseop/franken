package kr.co.future.sslvpn.xtmconf.network;

import java.util.HashMap;
import java.util.Map;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import kr.co.future.sslvpn.xtmconf.NodeWrapper;
import kr.co.future.sslvpn.xtmconf.XtmConfig;

public class VirtualIp extends XtmConfig {
	private int num;
	private String cid;
	private String iface; // 인터페이스
	private String ip; // 가상IP
	private String netmask; // Netmask

	@Override
	public String getXmlFilename() {
		return "network_virtual_ip.xml";
	}

	@Override
	public String getRootTagName() {
		return "network";
	}

	public static VirtualIp parse(NodeWrapper nw) {
		if (!nw.isName("vip"))
			return null;

		VirtualIp vi = new VirtualIp();
		vi.num = nw.intAttr("num");
		vi.cid = nw.attr("cid");

		for (NodeWrapper c : nw.children()) {
			if (c.isName("interface"))
				vi.iface = c.value();
			else if (c.isName("ip"))
				vi.ip = c.value();
			else if (c.isName("netmask"))
				vi.netmask = c.value();
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

	public String getNetmask() {
		return netmask;
	}

	public void setNetmask(String netmask) {
		this.netmask = netmask;
	}

	@Override
	protected Element convertToElement(Document doc) {
		Element e = doc.createElement("vip");

		e.setAttribute("num", String.valueOf(num));
		e.setAttribute("cid", cid);
		appendChild(doc, e, "interface", iface);
		appendChild(doc, e, "ip", ip);
		appendChild(doc, e, "netmask", netmask);

		return e;
	}

	@Override
	public Map<String, Object> marshal() {
		Map<String, Object> m = new HashMap<String, Object>();
		m.put("num", num);
		m.put("cid", cid);
		m.put("interface", iface);
		m.put("ip", ip);
		m.put("netmask", netmask);

		return m;
	}

	@Override
	public String toString() {
		return "VirtualIp [num=" + num + ", cid=" + cid + ", iface=" + iface + ", ip=" + ip + ", netmask=" + netmask
				+ "]";
	}
}
