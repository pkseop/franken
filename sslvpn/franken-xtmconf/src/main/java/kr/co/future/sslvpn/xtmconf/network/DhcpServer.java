package kr.co.future.sslvpn.xtmconf.network;

import java.util.HashMap;
import java.util.Map;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import kr.co.future.sslvpn.xtmconf.NodeWrapper;
import kr.co.future.sslvpn.xtmconf.Utils;
import kr.co.future.sslvpn.xtmconf.XtmConfig;

public class DhcpServer extends XtmConfig {
	private int num;
	private String cid;
	private boolean use; // 사용
	private String iface; // 인터페이스
	private int time; // 할당 시간(초)
	private String domain1; // 주 도메인 서버
	private String domain2; // 보조 도메인 서버
	private String gateway; // 기본 게이트웨이
	private String saddr; // 시작주소
	private String daddr; // 끝주소
	private String netmask; // 넷마스크

	@Override
	public String getXmlFilename() {
		return "network_dhcp_server.xml";
	}

	@Override
	public String getRootTagName() {
		return "network";
	}

	public static DhcpServer parse(NodeWrapper nw) {
		if (!nw.isName("dhcp"))
			return null;

		DhcpServer ds = new DhcpServer();
		ds.num = nw.intAttr("num");
		ds.cid = nw.attr("cid");
		ds.use = nw.boolAttr("use");

		for (NodeWrapper c : nw.children()) {
			if (c.isName("interface"))
				ds.iface = c.value();
			else if (c.isName("time"))
				ds.time = c.intValue();
			else if (c.isName("domain1"))
				ds.domain1 = c.value();
			else if (c.isName("domain2"))
				ds.domain2 = c.value();
			else if (c.isName("gateway"))
				ds.gateway = c.value();
			else if (c.isName("saddr"))
				ds.saddr = c.value();
			else if (c.isName("daddr"))
				ds.daddr = c.value();
			else if (c.isName("netmask"))
				ds.netmask = c.value();
		}

		return ds;
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

	public boolean isUse() {
		return use;
	}

	public void setUse(boolean use) {
		this.use = use;
	}

	public String getIface() {
		return iface;
	}

	public void setIface(String iface) {
		this.iface = iface;
	}

	public int getTime() {
		return time;
	}

	public void setTime(int time) {
		this.time = time;
	}

	public String getDomain1() {
		return domain1;
	}

	public void setDomain1(String domain1) {
		this.domain1 = domain1;
	}

	public String getDomain2() {
		return domain2;
	}

	public void setDomain2(String domain2) {
		this.domain2 = domain2;
	}

	public String getGateway() {
		return gateway;
	}

	public void setGateway(String gateway) {
		this.gateway = gateway;
	}

	public String getSaddr() {
		return saddr;
	}

	public void setSaddr(String saddr) {
		this.saddr = saddr;
	}

	public String getDaddr() {
		return daddr;
	}

	public void setDaddr(String daddr) {
		this.daddr = daddr;
	}

	public String getNetmask() {
		return netmask;
	}

	public void setNetmask(String netmask) {
		this.netmask = netmask;
	}

	@Override
	protected Element convertToElement(Document doc) {
		Element e = doc.createElement("dhcp");

		e.setAttribute("num", String.valueOf(num));
		e.setAttribute("cid", cid);
		e.setAttribute("use", Utils.bool(use));
		appendChild(doc, e, "interface", iface);
		appendChild(doc, e, "time", time);
		appendChild(doc, e, "domain1", domain1);
		appendChild(doc, e, "domain2", domain2);
		appendChild(doc, e, "gateway", gateway);
		appendChild(doc, e, "saddr", saddr);
		appendChild(doc, e, "daddr", daddr);
		appendChild(doc, e, "netmask", netmask);

		return e;
	}

	@Override
	public Map<String, Object> marshal() {
		Map<String, Object> m = new HashMap<String, Object>();
		m.put("num", num);
		m.put("cid", cid);
		m.put("use", use);
		m.put("interface", iface);
		m.put("time", time);
		m.put("domain1", domain1);
		m.put("domain2", domain2);
		m.put("gateway", gateway);
		m.put("saddr", saddr);
		m.put("daddr", daddr);
		m.put("netmask", netmask);

		return m;
	}

	@Override
	public String toString() {
		return "DhcpServer [num=" + num + ", cid=" + cid + ", use=" + use + ", iface=" + iface + ", time=" + time
				+ ", domain1=" + domain1 + ", domain2=" + domain2 + ", gateway=" + gateway + ", saddr=" + saddr
				+ ", daddr=" + daddr + ", netmask=" + netmask + "]";
	}
}
