package kr.co.future.sslvpn.xtmconf.ipsec;

import java.util.HashMap;
import java.util.Map;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import kr.co.future.sslvpn.xtmconf.AttributeBuilder;
import kr.co.future.sslvpn.xtmconf.MarshalValue;
import kr.co.future.sslvpn.xtmconf.NodeWrapper;
import kr.co.future.sslvpn.xtmconf.Utils;
import kr.co.future.sslvpn.xtmconf.XtmConfig;

public class VpnHost extends XtmConfig {
	private int num;
	private String cid;
	private String ip; // 보안호스트

	@Override
	public String getXmlFilename() {
		return "vpn_host.xml";
	}

	@Override
	public String getRootTagName() {
		return "vpn";
	}

	public static VpnHost parse(NodeWrapper nw) {
		if (!nw.isName("host"))
			return null;

		VpnHost vh = new VpnHost();
		vh.num = nw.intAttr("num");
		vh.cid = nw.attr("cid");

		for (NodeWrapper c : nw.children()) {
			if (c.isName("ip"))
				vh.ip = c.value();
		}

		return vh;
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

	public String getIp() {
		return ip;
	}

	public void setIp(String ip) {
		this.ip = ip;
	}

	@Override
	protected Element convertToElement(Document doc) {
		Element e = doc.createElement("host");

		e.setAttribute("num", String.valueOf(num));
		e.setAttribute("cid", cid);
		appendChild(doc, e, "ip", ip,
				new AttributeBuilder("version", Utils.ipVersion(ip)).put("type", Utils.ipType(ip)));

		return e;
	}

	@Override
	public Map<String, Object> marshal() {
		Map<String, Object> m = new HashMap<String, Object>();
		m.put("num", num);
		m.put("cid", cid);
		m.put("ip", new MarshalValue("value", ip).put("version", Utils.ipVersion(ip)).put("type", Utils.ipType(ip))
				.get());

		return m;
	}

	@Override
	public String toString() {
		return "VpnHost [num=" + num + ", cid=" + cid + ", ip=" + ip + "]";
	}
}
