package kr.co.future.sslvpn.xtmconf.network;

import java.util.HashMap;
import java.util.Map;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import kr.co.future.sslvpn.xtmconf.NodeWrapper;
import kr.co.future.sslvpn.xtmconf.Utils;
import kr.co.future.sslvpn.xtmconf.XtmConfig;

public class L2TPConfig extends XtmConfig {
	private boolean use;
	private String clientIp;
	private String clientNetmask;
	private String secret;
	private String auth;
	private String dns1;
	private String dns2;

	public static L2TPConfig parse(NodeWrapper nw) {
		if (!nw.isName("l2tp"))
			return null;

		L2TPConfig l = new L2TPConfig();
		l.setUse(nw.boolAttr("chk_use"));

		for (NodeWrapper c : nw.children()) {
			if (c.isName("client_ip"))
				l.setClientIp(c.value());
			else if (c.isName("client_netmask"))
				l.setClientNetmask(c.value());
			else if (c.isName("secret"))
				l.setSecret(c.value());
			else if (c.isName("auth"))
				l.setAuth(c.value());
			else if (c.isName("dns1"))
				l.setDns1(c.value());
			else if (c.isName("dns2"))
				l.setDns2(c.value());
		}

		return l;
	}

	@Override
	public Map<String, Object> marshal() {
		Map<String, Object> m = new HashMap<String, Object>();
		m.put("use", use);
		m.put("client_ip", clientIp);
		m.put("client_netmask", clientNetmask);
		m.put("secret", secret);
		m.put("auth", auth);
		m.put("dns1", dns1);
		m.put("dns2", dns2);
		return m;
	}

	@Override
	public String getXmlFilename() {
		return "l2tp_config.xml";
	}

	@Override
	public String getRootTagName() {
		return "l2tp_config";
	}

	@Override
	protected Element convertToElement(Document doc) {
		Element e = doc.createElement("l2tp");
		e.setAttribute("chk_use", Utils.bool(use));

		appendChild(doc, e, "client_ip", clientIp);
		appendChild(doc, e, "client_netmask", clientNetmask);
		appendChild(doc, e, "secret", secret);
		appendChild(doc, e, "auth", auth);
		appendChild(doc, e, "dns1", dns1);
		appendChild(doc, e, "dns2", dns2);

		return e;
	}

	public boolean isUse() {
		return use;
	}

	public void setUse(boolean use) {
		this.use = use;
	}

	public String getClientIp() {
		return clientIp;
	}

	public void setClientIp(String clientIp) {
		this.clientIp = clientIp;
	}

	public String getClientNetmask() {
		return clientNetmask;
	}

	public void setClientNetmask(String clientNetmask) {
		this.clientNetmask = clientNetmask;
	}

	public String getSecret() {
		return secret;
	}

	public void setSecret(String secret) {
		this.secret = secret;
	}

	public String getAuth() {
		return auth;
	}

	public void setAuth(String auth) {
		this.auth = auth;
	}

	public String getDns1() {
		return dns1;
	}

	public void setDns1(String dns1) {
		this.dns1 = dns1;
	}

	public String getDns2() {
		return dns2;
	}

	public void setDns2(String dns2) {
		this.dns2 = dns2;
	}

}
