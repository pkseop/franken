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

public class RouterPolicy extends XtmConfig {
	private int num;
	private int policy; // Policy Num
	private String ip; // 출발지 IP/Netmask

	@Override
	public String getXmlFilename() {
		return "network_router_policy.xml";
	}

	@Override
	public String getRootTagName() {
		return "routing";
	}

	public static RouterPolicy parse(NodeWrapper nw) {
		if (!nw.isName("list"))
			return null;

		RouterPolicy rp = new RouterPolicy();
		rp.num = nw.intAttr("num");

		for (NodeWrapper c : nw.children()) {
			if (c.isName("policy"))
				rp.policy = c.intValue();
			else if (c.isName("ip"))
				rp.ip = c.value();
		}

		return rp;
	}

	public int getNum() {
		return num;
	}

	public void setNum(int num) {
		this.num = num;
	}

	public int getPolicy() {
		return policy;
	}

	public void setPolicy(int policy) {
		this.policy = policy;
	}

	public String getIp() {
		return ip;
	}

	public void setIp(String ip) {
		this.ip = ip;
	}

	@Override
	protected Element convertToElement(Document doc) {
		Element e = doc.createElement("list");

		e.setAttribute("num", String.valueOf(num));
		appendChild(doc, e, "policy", policy);
		appendChild(doc, e, "ip", ip,
				new AttributeBuilder("version", Utils.ipVersion(ip)).put("type", Utils.ipType(ip)));

		return e;
	}

	@Override
	public Map<String, Object> marshal() {
		Map<String, Object> m = new HashMap<String, Object>();
		m.put("num", num);
		m.put("policy", policy);
		m.put("ip", new MarshalValue("value", ip).put("version", Utils.ipVersion(ip)).put("type", Utils.ipType(ip))
				.get());

		return m;
	}

	@Override
	public String toString() {
		return "RouterPolicy [num=" + num + ", policy=" + policy + ", ip=" + ip + "]";
	}
}
