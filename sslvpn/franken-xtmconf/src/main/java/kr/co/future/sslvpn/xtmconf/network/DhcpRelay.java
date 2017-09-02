package kr.co.future.sslvpn.xtmconf.network;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import kr.co.future.sslvpn.xtmconf.NodeWrapper;
import kr.co.future.sslvpn.xtmconf.Utils;
import kr.co.future.sslvpn.xtmconf.XtmConfig;

public class DhcpRelay extends XtmConfig {
	private boolean use; // DHCP 릴레이 사용
	private String server; // DHCP 서버 주소
	private List<String> iface = new ArrayList<String>(); // 대상 인터페이스

	@Override
	public String getXmlFilename() {
		return "network_dhcp_relay.xml";
	}

	@Override
	public String getRootTagName() {
		return "network";
	}

	public static DhcpRelay parse(NodeWrapper nw) {
		if (!nw.isName("dhcrelay"))
			return null;

		DhcpRelay dr = new DhcpRelay();
		dr.use = nw.boolAttr("chk_use");

		for (NodeWrapper c : nw.children()) {
			if (c.isName("server"))
				dr.server = c.value();
			else if (c.isName("interface"))
				dr.iface.add(c.value());
		}

		return dr;
	}

	public boolean isUse() {
		return use;
	}

	public void setUse(boolean use) {
		this.use = use;
	}

	public String getServer() {
		return server;
	}

	public void setServer(String server) {
		this.server = server;
	}

	public List<String> getIface() {
		return iface;
	}

	public void setIface(List<String> iface) {
		this.iface = iface;
	}

	@Override
	protected Element convertToElement(Document doc) {
		Element e = doc.createElement("dhcrelay");

		e.setAttribute("chk_use", Utils.bool(use));
		e.setAttribute("count", String.valueOf(iface.size()));
		appendChild(doc, e, "server", server);
		for (String s : iface)
			appendChild(doc, e, "interface", s);

		return e;
	}

	@Override
	public Map<String, Object> marshal() {
		Map<String, Object> m = new HashMap<String, Object>();
		m.put("use", use);
		m.put("server", server);
		m.put("interface", iface);

		return m;
	}

	@Override
	public String toString() {
		return "DhcpRelay [use=" + use + ", server=" + server + ", iface=" + iface + "]";
	}
}
