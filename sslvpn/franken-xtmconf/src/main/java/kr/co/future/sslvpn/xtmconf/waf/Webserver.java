package kr.co.future.sslvpn.xtmconf.waf;

import java.util.HashMap;
import java.util.Map;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import kr.co.future.sslvpn.xtmconf.AttributeBuilder;
import kr.co.future.sslvpn.xtmconf.NodeWrapper;
import kr.co.future.sslvpn.xtmconf.XtmConfig;

public class Webserver extends XtmConfig {
	private int num;
	private String cid;
	private String name; // 서버명
	private String ip; // 서버IP
	private String domain; // 도메인 이름
	private String port; // 포트
	private boolean wa; // 웹가속 사용

	@Override
	public String getXmlFilename() {
		return "waf_webserver.xml";
	}

	@Override
	public String getRootTagName() {
		return "waf";
	}

	public static Webserver parse(NodeWrapper nw) {
		if (!nw.isName("webserver"))
			return null;

		Webserver w = new Webserver();
		w.num = nw.intAttr("num");
		w.cid = nw.attr("cid");

		for (NodeWrapper c : nw.children()) {
			if (c.isName("name"))
				w.name = c.value();
			else if (c.isName("ip"))
				w.ip = c.value();
			else if (c.isName("domain"))
				w.domain = c.value();
			else if (c.isName("port"))
				w.port = c.value();
			else if (c.isName("wa"))
				w.wa = c.boolAttr("chk_use");
		}

		return w;
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

	public String getDomain() {
		return domain;
	}

	public void setDomain(String domain) {
		this.domain = domain;
	}

	public String getPort() {
		return port;
	}

	public void setPort(String port) {
		this.port = port;
	}

	public boolean isWa() {
		return wa;
	}

	public void setWa(boolean wa) {
		this.wa = wa;
	}

	@Override
	protected Element convertToElement(Document doc) {
		Element e = doc.createElement("webserver");

		e.setAttribute("num", String.valueOf(num));
		e.setAttribute("cid", cid);
		appendChild(doc, e, "name", name);
		appendChild(doc, e, "ip", ip);
		appendChild(doc, e, "domain", domain);
		appendChild(doc, e, "port", port);
		appendChild(doc, e, "wa", null, new AttributeBuilder("chk_use", wa));

		return e;
	}

	@Override
	public Map<String, Object> marshal() {
		Map<String, Object> m = new HashMap<String, Object>();
		m.put("num", num);
		m.put("cid", cid);
		m.put("name", name);
		m.put("ip", ip);
		m.put("domain", domain);
		m.put("port", port);
		m.put("wa", wa);

		return m;
	}

	@Override
	public String toString() {
		return "Webserver [num=" + num + ", cid=" + cid + ", name=" + name + ", ip=" + ip + ", domain=" + domain
				+ ", port=" + port + ", wa=" + wa + "]";
	}
}
