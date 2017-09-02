package kr.co.future.sslvpn.xtmconf.network;

import java.util.HashMap;
import java.util.Map;

import kr.co.future.msgbus.Marshalable;
import kr.co.future.sslvpn.xtmconf.NodeWrapper;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class DnsPermit implements Marshalable {
	private String ip;
	private String netmask;

	public DnsPermit() {
	}

	public DnsPermit(NodeWrapper nw) {
		for (NodeWrapper c : nw.children()) {
			if (c.isName("ip"))
				this.ip = c.value();
			else if (c.isName("netmask"))
				this.netmask = c.value();
		}
	}

	public Element toElement(Document doc) {
		Element e = doc.createElement("permit");

		Element ip = doc.createElement("ip");
		ip.setTextContent(this.ip);
		Element netmask = doc.createElement("netmask");
		netmask.setTextContent(this.netmask);

		e.appendChild(ip);
		e.appendChild(netmask);

		return e;
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
	public Map<String, Object> marshal() {
		Map<String, Object> m = new HashMap<String, Object>();
		m.put("ip", ip);
		m.put("netmask", netmask);

		return m;
	}

	@Override
	public String toString() {
		return "DnsPermit [ip=" + ip + ", netmask=" + netmask + "]";
	}
}
