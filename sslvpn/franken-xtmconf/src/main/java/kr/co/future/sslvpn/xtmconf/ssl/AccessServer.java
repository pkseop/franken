package kr.co.future.sslvpn.xtmconf.ssl;

import java.util.HashMap;
import java.util.Map;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import kr.co.future.sslvpn.xtmconf.NodeWrapper;
import kr.co.future.sslvpn.xtmconf.XtmConfig;

public class AccessServer extends XtmConfig {
	private int num;
	private String cid;
	private String name; // 이름
	private String ip; // IP
	private String netmask; // Netmask

	@Override
	public String getXmlFilename() {
		return "ssl_access_server.xml";
	}

	@Override
	public String getRootTagName() {
		return "ssl";
	}

	public static AccessServer parse(NodeWrapper nw) {
		if (!nw.isName("server"))
			return null;

		AccessServer as = new AccessServer();
		as.num = nw.intAttr("num");
		as.cid = nw.attr("cid");

		for (NodeWrapper c : nw.children()) {
			if (c.isName("name"))
				as.name = c.value();
			else if (c.isName("ip"))
				as.ip = c.value();
			else if (c.isName("netmask"))
				as.netmask = c.value();
		}

		return as;
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

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
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
		Element e = doc.createElement("server");

		e.setAttribute("num", String.valueOf(num));
		e.setAttribute("cid", cid);
		appendChild(doc, e, "name", name);
		appendChild(doc, e, "ip", ip);
		appendChild(doc, e, "netmask", netmask);

		return e;
	}

	@Override
	public Map<String, Object> marshal() {
		Map<String, Object> m = new HashMap<String, Object>();
		m.put("num", num);
		m.put("cid", cid);
		m.put("name", name);
		m.put("ip", ip);
		m.put("netmask", netmask);

		return m;
	}

	@Override
	public String toString() {
		return "AccessServer [num=" + num + ", cid=" + cid + ", name=" + name + ", ip=" + ip + ", netmask=" + netmask
				+ "]";
	}
}
