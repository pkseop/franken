package kr.co.future.sslvpn.xtmconf.network;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import kr.co.future.sslvpn.xtmconf.AttributeBuilder;
import kr.co.future.sslvpn.xtmconf.NodeWrapper;
import kr.co.future.sslvpn.xtmconf.Utils;
import kr.co.future.sslvpn.xtmconf.XtmConfig;

public class AnomalySpoofing extends XtmConfig {
	private boolean use; // IP Spoofing 검사
	private String v4Cid; // IPv4 내부망
	private String v6Cid; // IPv6 내부망

	@Override
	public String getXmlFilename() {
		return "network_anomaly_spoofing.xml";
	}

	@Override
	public String getRootTagName() {
		return "anomaly";
	}

	public static boolean hasCid(String cid) {
		List<AnomalySpoofing> objs = XtmConfig.readConfig(AnomalySpoofing.class);
		for (AnomalySpoofing as : objs) {
			if (cid.equals(as.v4Cid))
				return true;
			if (cid.equals(as.v6Cid))
				return true;
		}
		return false;
	}

	public static AnomalySpoofing parse(NodeWrapper nw) {
		if (!nw.isName("spoofing"))
			return null;

		AnomalySpoofing as = new AnomalySpoofing();
		as.use = nw.boolAttr("chk_use");

		for (NodeWrapper c : nw.children()) {
			if (c.isName("vnet") && c.attr("version").equals("v4"))
				as.v4Cid = c.attr("cid");
			else if (c.isName("vnet") && c.attr("version").equals("v6"))
				as.v6Cid = c.attr("cid");
		}

		return as;
	}

	public boolean isUse() {
		return use;
	}

	public void setUse(boolean use) {
		this.use = use;
	}

	public String getV4Cid() {
		return v4Cid;
	}

	public void setV4Cid(String v4Cid) {
		this.v4Cid = v4Cid;
	}

	public String getV6Cid() {
		return v6Cid;
	}

	public void setV6Cid(String v6Cid) {
		this.v6Cid = v6Cid;
	}

	@Override
	protected Element convertToElement(Document doc) {
		Element e = doc.createElement("spoofing");

		e.setAttribute("chk_use", Utils.bool(use));
		appendChild(doc, e, "vnet", null, new AttributeBuilder("version", "v4").put("cid", v4Cid));
		appendChild(doc, e, "vnet", null, new AttributeBuilder("version", "v6").put("cid", v6Cid));

		return e;
	}

	@Override
	public Map<String, Object> marshal() {
		Map<String, Object> m = new HashMap<String, Object>();
		m.put("use", use);
		m.put("v4", v4Cid);
		m.put("v6", v6Cid);

		return m;
	}

	@Override
	public String toString() {
		return "AnomalySpoofing [use=" + use + ", v4Cid=" + v4Cid + ", v6Cid=" + v6Cid + "]";
	}
}
