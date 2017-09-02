package kr.co.future.sslvpn.xtmconf.network;

import java.util.HashMap;
import java.util.Map;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import kr.co.future.sslvpn.xtmconf.AttributeBuilder;
import kr.co.future.sslvpn.xtmconf.NodeWrapper;
import kr.co.future.sslvpn.xtmconf.XtmConfig;

public class InnerDns extends XtmConfig {
	private DnsType type;
	private boolean use;
	private String iface;
	private DnsPermit permit;
	private DnsDomain domain;

	@Override
	public String getXmlFilename() {
		return "network_splitDNS_innerDNS.xml";
	}

	@Override
	public String getRootTagName() {
		return "dns";
	}

	public static InnerDns parse(NodeWrapper nw) {
		if (!nw.isName("info") && !nw.isName("permit") && !nw.isName("domain"))
			return null;

		InnerDns id = new InnerDns();
		id.type = DnsType.get(nw.name());

		if (id.type == DnsType.Info) {
			for (NodeWrapper c : nw.children()) {
				if (c.isName("setting")) {
					id.use = c.boolAttr("chk_use");
					id.iface = c.attr("interface");
				}
			}
		} else if (id.type == DnsType.Permit)
			id.permit = new DnsPermit(nw);
		else if (id.type == DnsType.Domain)
			id.domain = new DnsDomain(nw);

		return id;
	}

	public DnsType getType() {
		return type;
	}

	public void setType(DnsType type) {
		this.type = type;
	}

	public boolean isUse() {
		return use;
	}

	public void setUse(boolean use) {
		this.use = use;
	}

	public String getIface() {
		return iface;
	}

	public void setIface(String iface) {
		this.iface = iface;
	}

	public DnsPermit getPermit() {
		return permit;
	}

	public void setPermit(DnsPermit permit) {
		this.permit = permit;
	}

	public DnsDomain getDomain() {
		return domain;
	}

	public void setDomain(DnsDomain domain) {
		this.domain = domain;
	}

	@Override
	protected Element convertToElement(Document doc) {
		if (type == DnsType.Info) {
			Element e = doc.createElement(type.toString());
			appendChild(doc, e, "setting", null, new AttributeBuilder("chk_use", use).put("interface", iface));
			return e;
		} else if (type == DnsType.Permit)
			return permit.toElement(doc);
		else if (type == DnsType.Domain)
			return domain.toElement(doc);

		return null;
	}

	@Override
	public Map<String, Object> marshal() {
		Map<String, Object> m = new HashMap<String, Object>();
		m.put("type", type);

		if (type == DnsType.Info) {
			m.put("use", use);
			m.put("interface", iface);
		} else if (type == DnsType.Permit)
			m.putAll(permit.marshal());
		else if (type == DnsType.Domain)
			m.putAll(domain.marshal());

		return m;
	}

	@Override
	public String toString() {
		return "InnerDns [type=" + type + ", use=" + use + ", iface=" + iface + ", permit=" + permit + ", domain="
				+ domain + "]";
	}
}
