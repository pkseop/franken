package kr.co.future.sslvpn.xtmconf.manage;

import java.util.HashMap;
import java.util.Map;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import kr.co.future.sslvpn.xtmconf.NodeWrapper;
import kr.co.future.sslvpn.xtmconf.XtmConfig;

public class LogRttSetting extends XtmConfig {
	private String name; // 인터페이스
	private String ip; // IP

	@Override
	public String getXmlFilename() {
		return "log_rtt_setting.xml";
	}

	@Override
	public String getRootTagName() {
		return "manage";
	}

	public static LogRttSetting parse(NodeWrapper nw) {
		if (!nw.isName("rtt"))
			return null;

		LogRttSetting lrs = new LogRttSetting();

		for (NodeWrapper c : nw.children()) {
			if (c.isName("name"))
				lrs.name = c.value();
			else if (c.isName("ip"))
				lrs.ip = c.value();
		}

		return lrs;
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

	@Override
	protected Element convertToElement(Document doc) {
		Element e = doc.createElement("rtt");

		appendChild(doc, e, "name", name);
		appendChild(doc, e, "ip", ip);

		return e;
	}

	@Override
	public Map<String, Object> marshal() {
		Map<String, Object> m = new HashMap<String, Object>();
		m.put("name", name);
		m.put("ip", ip);

		return m;
	}

	@Override
	public String toString() {
		return "LogRttSetting [name=" + name + ", ip=" + ip + "]";
	}
}
