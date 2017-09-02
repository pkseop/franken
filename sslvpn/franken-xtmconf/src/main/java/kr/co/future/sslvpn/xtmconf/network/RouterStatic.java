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

public class RouterStatic extends XtmConfig {
	private int num;
	private boolean use; // 사용
	private Integer policy; // Policy Num
	private String ip; // 목적지IP/Netmask
	private String gateway; // 게이트웨이
	private String iface; // 인터페이스
	private int metric; // Metric

	@Override
	public String getXmlFilename() {
		return "network_router_static.xml";
	}

	@Override
	public String getRootTagName() {
		return "routing";
	}

	public static RouterStatic parse(NodeWrapper nw) {
		if (!nw.isName("list"))
			return null;

		RouterStatic rs = new RouterStatic();
		rs.num = nw.intAttr("num");
		rs.use = nw.boolAttr("use");

		for (NodeWrapper c : nw.children()) {
			if (c.isName("policy"))
				rs.policy = (c.value().equals("Any")) ? null : c.intValue();
			else if (c.isName("ip"))
				rs.ip = c.value();
			else if (c.isName("gateway"))
				rs.gateway = c.value();
			else if (c.isName("interface"))
				rs.iface = c.value();
			else if (c.isName("metric"))
				rs.metric = c.intValue();
		}

		return rs;
	}

	public int getNum() {
		return num;
	}

	public void setNum(int num) {
		this.num = num;
	}

	public boolean isUse() {
		return use;
	}

	public void setUse(boolean use) {
		this.use = use;
	}

	public Integer getPolicy() {
		return policy;
	}

	public void setPolicy(Integer policy) {
		this.policy = policy;
	}

	public String getIp() {
		return ip;
	}

	public void setIp(String ip) {
		this.ip = ip;
	}

	public String getGateway() {
		return gateway;
	}

	public void setGateway(String gateway) {
		this.gateway = gateway;
	}

	public String getIface() {
		return iface;
	}

	public void setIface(String iface) {
		this.iface = iface;
	}

	public int getMetric() {
		return metric;
	}

	public void setMetric(int metric) {
		this.metric = metric;
	}

	@Override
	protected Element convertToElement(Document doc) {
		Element e = doc.createElement("list");

		e.setAttribute("num", String.valueOf(num));
		e.setAttribute("use", Utils.bool(use));
		appendChild(doc, e, "policy", (policy == null) ? "Any" : policy);
		appendChild(doc, e, "ip", ip,
				new AttributeBuilder("version", Utils.ipVersion(ip)).put("type", Utils.ipType(ip)));
		appendChild(doc, e, "gateway", gateway,
				new AttributeBuilder("version", Utils.ipVersion(gateway)).put("type", Utils.ipType(gateway)));
		appendChild(doc, e, "interface", iface);
		appendChild(doc, e, "metric", metric);

		return e;
	}

	@Override
	public Map<String, Object> marshal() {
		Map<String, Object> m = new HashMap<String, Object>();
		m.put("num", num);
		m.put("use", use);
		m.put("policy", policy);
		m.put("ip", new MarshalValue("value", ip).put("version", Utils.ipVersion(ip)).put("type", Utils.ipType(ip))
				.get());
		m.put("gateway",
				new MarshalValue("value", gateway).put("version", Utils.ipVersion(gateway))
						.put("type", Utils.ipType(gateway)).get());
		m.put("interface", iface);
		m.put("metric", metric);

		return m;
	}

	@Override
	public String toString() {
		return "RouterStatic [num=" + num + ", use=" + use + ", policy=" + policy + ", ip=" + ip + ", gateway="
				+ gateway + ", iface=" + iface + ", metric=" + metric + "]";
	}
}
