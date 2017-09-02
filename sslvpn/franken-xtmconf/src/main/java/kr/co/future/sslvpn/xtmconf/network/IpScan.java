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

public class IpScan extends XtmConfig {
	public static enum Type {
		ARP, ICMP
	}

	private int num;
	private String iface; // 인터페이스
	private String ip; // IP
	private int cycle; // 실행주기
	private Type type; // SCAN 방법

	@Override
	public String getXmlFilename() {
		return "ip_scan.xml";
	}

	@Override
	public String getRootTagName() {
		return "network";
	}

	public static IpScan parse(NodeWrapper nw) {
		if (!nw.isName("scan"))
			return null;

		IpScan is = new IpScan();
		is.num = nw.intAttr("num");

		for (NodeWrapper c : nw.children()) {
			if (c.isName("interface"))
				is.iface = c.value();
			else if (c.isName("ip"))
				is.ip = c.value();
			else if (c.isName("cycle"))
				is.cycle = c.intValue();
			else if (c.isName("type"))
				is.type = Type.valueOf(c.value());
		}

		return is;
	}

	public int getNum() {
		return num;
	}

	public void setNum(int num) {
		this.num = num;
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

	public int getCycle() {
		return cycle;
	}

	public void setCycle(int cycle) {
		this.cycle = cycle;
	}

	public Type getType() {
		return type;
	}

	public void setType(Type type) {
		this.type = type;
	}

	@Override
	protected Element convertToElement(Document doc) {
		Element e = doc.createElement("scan");

		e.setAttribute("num", String.valueOf(num));
		appendChild(doc, e, "interface", iface);
		appendChild(doc, e, "ip", ip, new AttributeBuilder("type", Utils.ipType(ip)));
		appendChild(doc, e, "cycle", cycle);
		appendChild(doc, e, "type", type);

		return e;
	}

	@Override
	public Map<String, Object> marshal() {
		Map<String, Object> m = new HashMap<String, Object>();
		m.put("num", num);
		m.put("interface", iface);
		m.put("ip", new MarshalValue("value", ip).put("type", Utils.ipType(ip)).get());
		m.put("cycle", cycle);
		m.put("type", type);

		return m;
	}

	@Override
	public String toString() {
		return "IpScan [num=" + num + ", iface=" + iface + ", ip=" + ip + ", cycle=" + cycle + ", type=" + type + "]";
	}
}
