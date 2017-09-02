package kr.co.future.sslvpn.xtmconf.manage;

import java.util.HashMap;
import java.util.Map;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import kr.co.future.sslvpn.xtmconf.AttributeBuilder;
import kr.co.future.sslvpn.xtmconf.NodeWrapper;
import kr.co.future.sslvpn.xtmconf.XtmConfig;

public class Monitor extends XtmConfig {
	private boolean session; // 세션 사용량 모니터
	private boolean host; // 호스트별 TOP-10
	private boolean service; // 서비스별 TOP-10
	private boolean flow; // 플로우별 TOP-10
	private String eth; // 트래픽 모니터링 설정

	@Override
	public String getXmlFilename() {
		return "manage_monitor.xml";
	}

	@Override
	public String getRootTagName() {
		return "manage";
	}

	public static Monitor parse(NodeWrapper nw) {
		if (!nw.isName("monitor"))
			return null;

		Monitor m = new Monitor();

		for (NodeWrapper c : nw.children()) {
			if (c.isName("setting")) {
				m.session = c.boolAttr("chk_session");
				m.host = c.boolAttr("chk_host");
				m.service = c.boolAttr("chk_service");
				m.flow = c.boolAttr("chk_flow");
			} else if (c.isName("eth"))
				m.eth = c.value();
		}

		return m;
	}

	public boolean isSession() {
		return session;
	}

	public void setSession(boolean session) {
		this.session = session;
	}

	public boolean isHost() {
		return host;
	}

	public void setHost(boolean host) {
		this.host = host;
	}

	public boolean isService() {
		return service;
	}

	public void setService(boolean service) {
		this.service = service;
	}

	public boolean isFlow() {
		return flow;
	}

	public void setFlow(boolean flow) {
		this.flow = flow;
	}

	public String getEth() {
		return eth;
	}

	public void setEth(String eth) {
		this.eth = eth;
	}

	@Override
	protected Element convertToElement(Document doc) {
		Element e = doc.createElement("monitor");

		AttributeBuilder settingAttr = new AttributeBuilder("chk_session", session).put("chk_host", host)
				.put("chk_service", service).put("chk_flow", flow);
		appendChild(doc, e, "setting", null, settingAttr);
		appendChild(doc, e, "eth", eth);

		return e;
	}

	@Override
	public Map<String, Object> marshal() {
		Map<String, Object> m = new HashMap<String, Object>();
		m.put("session", session);
		m.put("host", host);
		m.put("service", service);
		m.put("flow", flow);
		m.put("eth", eth);

		return m;
	}

	@Override
	public String toString() {
		return "Monitor [session=" + session + ", host=" + host + ", service=" + service + ", flow=" + flow + ", eth="
				+ eth + "]";
	}
}
