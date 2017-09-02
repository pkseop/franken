package kr.co.future.sslvpn.xtmconf.system;

import java.util.HashMap;
import java.util.Map;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import kr.co.future.sslvpn.xtmconf.AttributeBuilder;
import kr.co.future.sslvpn.xtmconf.MarshalValue;
import kr.co.future.sslvpn.xtmconf.NodeWrapper;
import kr.co.future.sslvpn.xtmconf.Utils;
import kr.co.future.sslvpn.xtmconf.XtmConfig;

public class UpgradeMus extends XtmConfig {
	private boolean use; // 패턴업데이트 서버 설정
	private String ip; // IP
	private boolean interval; // 실행주기
	private Periodic intervalType; // 실행주기
	private int intervalTerm; // 실행주기

	@Override
	public String getXmlFilename() {
		return "upgrade_mus.xml";
	}

	@Override
	public String getRootTagName() {
		return "mus";
	}

	public static UpgradeMus parse(NodeWrapper nw) {
		if (!nw.isName("set"))
			return null;

		UpgradeMus um = new UpgradeMus();
		um.use = nw.boolAttr("update_use");

		for (NodeWrapper c : nw.children()) {
			if (c.isName("ip"))
				um.ip = c.value();
			else if (c.isName("interval")) {
				um.interval = c.boolAttr("chk_use");
				um.intervalType = Periodic.get(c.attr("type"));
				um.intervalTerm = c.intAttr("term");
			}
		}
		return um;
	}

	public boolean isUse() {
		return use;
	}

	public void setUse(boolean use) {
		this.use = use;
	}

	public String getIp() {
		return ip;
	}

	public void setIp(String ip) {
		this.ip = ip;
	}

	public boolean isInterval() {
		return interval;
	}

	public void setInterval(boolean interval) {
		this.interval = interval;
	}

	public Periodic getIntervalType() {
		return intervalType;
	}

	public void setIntervalType(Periodic intervalType) {
		this.intervalType = intervalType;
	}

	public int getIntervalTerm() {
		return intervalTerm;
	}

	public void setIntervalTerm(int intervalTerm) {
		this.intervalTerm = intervalTerm;
	}

	@Override
	protected Element convertToElement(Document doc) {
		Element e = doc.createElement("set");

		e.setAttribute("update_use", Utils.bool(use));
		appendChild(doc, e, "ip", ip);
		AttributeBuilder intervalAttr = new AttributeBuilder("chk_use", interval).put("type", intervalType).put("term",
				intervalTerm);
		appendChild(doc, e, "interval", null, intervalAttr);

		return e;
	}

	@Override
	public Map<String, Object> marshal() {
		Map<String, Object> m = new HashMap<String, Object>();
		m.put("use", use);
		m.put("ip", ip);
		m.put("interval", new MarshalValue("use", interval).put("type", intervalType).put("term", intervalTerm).get());

		return m;
	}

	@Override
	public String toString() {
		return "UpgradeMus [use=" + use + ", ip=" + ip + ", interval=" + interval + ", intervalType=" + intervalType
				+ ", intervalTerm=" + intervalTerm + "]";
	}
}
