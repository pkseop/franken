package kr.co.future.sslvpn.xtmconf.ssl;

import java.util.HashMap;
import java.util.Map;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import kr.co.future.sslvpn.xtmconf.AttributeBuilder;
import kr.co.future.sslvpn.xtmconf.MarshalValue;
import kr.co.future.sslvpn.xtmconf.NodeWrapper;
import kr.co.future.sslvpn.xtmconf.Utils;
import kr.co.future.sslvpn.xtmconf.XtmConfig;

public class Setting extends XtmConfig {
	public static enum Protocol {
		TCP, UDP
	}

	private boolean use; // SSL VPN 서비스 사용
	private Protocol protocol; // 터널 프로토콜
	private boolean settingOption; // Client to Client
	private int port; // 터널 포트
	private String ip; // Client IP Group 네트워크주소
	private String netmask; // Client IP Group 넷마스크
	private int methodAuth; // 인증방식, 로컬 인증(0), 외부 인증 (Radius)(1)
	private int methodLogin; // 로그인 방식, 아이디/패스워드방식(0), 아이디/패스워드+인증서 방식(1)

	@Override
	public String getXmlFilename() {
		return "ssl_setting.xml";
	}

	@Override
	public String getRootTagName() {
		return "ssl";
	}

	public static Setting parse(NodeWrapper nw) {
		if (!nw.isName("vpn"))
			return null;

		Setting s = new Setting();
		s.use = nw.attr("chk_use").equals("on");

		for (NodeWrapper c : nw.children()) {
			if (c.isName("setting")) {
				s.protocol = Protocol.valueOf(c.attr("protocol"));
				s.settingOption = c.boolAttr("option");
			} else if (c.isName("port"))
				s.port = c.intValue();
			else if (c.isName("ip"))
				s.ip = c.value();
			else if (c.isName("netmask"))
				s.netmask = c.value();
			else if (c.isName("method")) {
				s.methodAuth = c.intAttr("auth");
				s.methodLogin = c.intAttr("login");
			}
		}

		return s;
	}

	public boolean isUse() {
		return use;
	}

	public void setUse(boolean use) {
		this.use = use;
	}

	public Protocol getProtocol() {
		return protocol;
	}

	public void setProtocol(Protocol protocol) {
		this.protocol = protocol;
	}

	public boolean isSettingOption() {
		return settingOption;
	}

	public void setSettingOption(boolean settingOption) {
		this.settingOption = settingOption;
	}

	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
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

	public int getMethodAuth() {
		return methodAuth;
	}

	public void setMethodAuth(int methodAuth) {
		this.methodAuth = methodAuth;
	}

	public int getMethodLogin() {
		return methodLogin;
	}

	public void setMethodLogin(int methodLogin) {
		this.methodLogin = methodLogin;
	}

	@Override
	protected Element convertToElement(Document doc) {
		Element e = doc.createElement("vpn");

		e.setAttribute("chk_use", Utils.bool(use));
		appendChild(doc, e, "setting", null, new AttributeBuilder("protocol", protocol).put("option", settingOption));
		appendChild(doc, e, "port", port);
		appendChild(doc, e, "ip", ip);
		appendChild(doc, e, "netmask", netmask);
		appendChild(doc, e, "method", null, new AttributeBuilder("auth", methodAuth).put("login", methodLogin));

		return e;
	}

	@Override
	public Map<String, Object> marshal() {
		Map<String, Object> m = new HashMap<String, Object>();
		m.put("ues", use);
		m.put("protocol", protocol);
		m.put("option", settingOption);
		m.put("port", port);
		m.put("ip", ip);
		m.put("netmask", netmask);
		m.put("method", new MarshalValue("auth", methodAuth).put("login", methodLogin).get());

		return m;
	}

	@Override
	public String toString() {
		return "Setting [use=" + use + ", protocol=" + protocol + ", settingOption=" + settingOption + ", port=" + port
				+ ", ip=" + ip + ", netmask=" + netmask + ", methodAuth=" + methodAuth + ", methodLogin=" + methodLogin
				+ "]";
	}
}
