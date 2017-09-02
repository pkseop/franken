package kr.co.future.sslvpn.xtmconf.network;

import java.util.HashMap;
import java.util.Map;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import kr.co.future.sslvpn.xtmconf.AttributeBuilder;
import kr.co.future.sslvpn.xtmconf.NodeWrapper;
import kr.co.future.sslvpn.xtmconf.XtmConfig;

public class SixToFour extends XtmConfig {
	private boolean use; // 6to4 사용
	private String ip; // Ipv4
	private String relay; // Relay router IP

	@Override
	public String getXmlFilename() {
		return "network_tunneling_6to4.xml";
	}

	@Override
	public String getRootTagName() {
		return "network";
	}

	public static SixToFour parse(NodeWrapper nw) {
		if (!nw.isName("tunneling"))
			return null;

		SixToFour stf = new SixToFour();

		for (NodeWrapper c : nw.children()) {
			if (c.isName("setting"))
				stf.use = c.boolAttr("chk_use");
			else if (c.isName("ip"))
				stf.ip = c.value();
			else if (c.isName("relay"))
				stf.relay = c.value();
		}

		return stf;
	}

	public boolean isUse() {
		return use;
	}

	public void setUse(boolean use) {
		this.use = use;
	}

	public String getIp() {
		return ip;
	}

	public void setIp(String ip) {
		this.ip = ip;
	}

	public String getRelay() {
		return relay;
	}

	public void setRelay(String relay) {
		this.relay = relay;
	}

	@Override
	protected Element convertToElement(Document doc) {
		Element e = doc.createElement("tunneling");

		appendChild(doc, e, "setting", null, new AttributeBuilder("chk_use", use));
		appendChild(doc, e, "ip", ip);
		appendChild(doc, e, "relay", relay);

		return e;
	}

	@Override
	public Map<String, Object> marshal() {
		Map<String, Object> m = new HashMap<String, Object>();
		m.put("use", use);
		m.put("ip", ip);
		m.put("relay", relay);

		return m;
	}

	@Override
	public String toString() {
		return "SixToFour [use=" + use + ", ip=" + ip + ", relay=" + relay + "]";
	}
}
