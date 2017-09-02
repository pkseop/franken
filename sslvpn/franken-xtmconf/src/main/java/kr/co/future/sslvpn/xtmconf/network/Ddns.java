package kr.co.future.sslvpn.xtmconf.network;

import java.util.HashMap;
import java.util.Map;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import kr.co.future.sslvpn.xtmconf.AttributeBuilder;
import kr.co.future.sslvpn.xtmconf.MarshalValue;
import kr.co.future.sslvpn.xtmconf.NodeWrapper;
import kr.co.future.sslvpn.xtmconf.XtmConfig;

public class Ddns extends XtmConfig {
	public static enum Server {
		Dyndns("dyndns.org"), Freedns("freedns.afraid.org");

		private String url;

		private Server(String url) {
			this.url = url;
		}

		public static Server get(String str) {
			for (Server s : Server.values()) {
				if (s.toString().equals(str))
					return s;
			}
			return null;
		}

		@Override
		public String toString() {
			return url;
		}
	}

	private boolean use; // Dynamic DNS 서비스 사용
	private Server server; // 서비스 제공서버
	private String host; // 호스트명
	private String name; // 사용자명
	private String password; // 비밀번호
	private Integer cycle; // 갱신주기(초)
	private String freednsHash; // 사용자 해시값
	private Integer freednsTime; // 갱신주기(초)

	@Override
	public String getXmlFilename() {
		return "network_ddns.xml";
	}

	@Override
	public String getRootTagName() {
		return "network";
	}

	public static Ddns parse(NodeWrapper nw) {
		if (!nw.isName("ddns"))
			return null;

		Ddns d = new Ddns();

		for (NodeWrapper c : nw.children()) {
			if (c.isName("setting"))
				d.use = c.boolAttr("chk_use");
			else if (c.isName("server"))
				d.server = Server.get(c.value());
			else if (c.isName("host"))
				d.host = c.value();
			else if (c.isName("name"))
				d.name = c.value();
			else if (c.isName("password"))
				d.password = c.value();
			else if (c.isName("cycle"))
				d.cycle = c.intValue();
			else if (c.isName("freedns_hash"))
				d.freednsHash = c.value();
			else if (c.isName("freedns_time"))
				d.freednsTime = c.intValue();
		}

		return d;
	}

	public boolean isUse() {
		return use;
	}

	public void setUse(boolean use) {
		this.use = use;
	}

	public Server getServer() {
		return server;
	}

	public void setServer(Server server) {
		this.server = server;
	}

	public String getHost() {
		return host;
	}

	public void setHost(String host) {
		this.host = host;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public Integer getCycle() {
		return cycle;
	}

	public void setCycle(Integer cycle) {
		this.cycle = cycle;
	}

	public String getFreednsHash() {
		return freednsHash;
	}

	public void setFreednsHash(String freednsHash) {
		this.freednsHash = freednsHash;
	}

	public Integer getFreednsTime() {
		return freednsTime;
	}

	public void setFreednsTime(Integer freednsTime) {
		this.freednsTime = freednsTime;
	}

	@Override
	protected Element convertToElement(Document doc) {
		Element e = doc.createElement("ddns");

		appendChild(doc, e, "setting", null, new AttributeBuilder("chk_use", use));
		appendChild(doc, e, "server", server);
		appendChild(doc, e, "host", host);
		appendChild(doc, e, "name", name);
		appendChild(doc, e, "password", password);
		appendChild(doc, e, "cycle", cycle);
		appendChild(doc, e, "freedns_hash", freednsHash);
		appendChild(doc, e, "freedns_time", freednsTime);

		return e;
	}

	@Override
	public Map<String, Object> marshal() {
		Map<String, Object> m = new HashMap<String, Object>();
		m.put("use", use);
		m.put("server", server);
		m.put("host", host);
		m.put("name", name);
		m.put("password", password);
		m.put("cycle", cycle);
		m.put("freedns", new MarshalValue("hash", freednsHash).put("time", freednsTime).get());

		return m;
	}

	@Override
	public String toString() {
		return "Ddns [use=" + use + ", server=" + server + ", host=" + host + ", name=" + name + ", password="
				+ password + ", cycle=" + cycle + ", freednsHash=" + freednsHash + ", freednsTime=" + freednsTime + "]";
	}
}
