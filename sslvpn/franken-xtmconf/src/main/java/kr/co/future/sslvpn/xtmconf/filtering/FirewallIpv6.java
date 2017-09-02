package kr.co.future.sslvpn.xtmconf.filtering;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import kr.co.future.sslvpn.xtmconf.AttributeBuilder;
import kr.co.future.sslvpn.xtmconf.MarshalValue;
import kr.co.future.sslvpn.xtmconf.NodeWrapper;
import kr.co.future.sslvpn.xtmconf.Utils;
import kr.co.future.sslvpn.xtmconf.XtmConfig;

public class FirewallIpv6 extends XtmConfig {
	private int num;
	private String cid;
	private boolean use; // 동작
	private Address src; // 출발주소
	private Address dest; // 도착주소
	private String headerCid;
	private String headerName; // 헤더
	private Service service; // 서비스
	private String scheduleCid;
	private String scheduleName; // 스케쥴
	private String qosCid;
	private String qosName; // QOS
	private String sessionCid;
	private String sessionName; // 세션제한
	private int ipsPid; // DPI 그룹, 통합 보안(0), 웹서버 보안(1), 서버 보안(2), PC 보안(3)
	private boolean ips; // DPI 사용
	private String ipsecsaCid;
	private String ipsecsaName; // IPSEC SA Object
	private EtcAction etcAction; // 행위
	private int etcTimeout; // 타임아웃
	private boolean etcCrossSpd;
	private LogLevel loglevel; // 로그
	private String desc; // 기타정보

	@Override
	public String getXmlFilename() {
		return "firewall_IPv6.xml";
	}

	@Override
	public String getRootTagName() {
		return "policy";
	}

	public static boolean hasCid(String cid) {
		List<FirewallIpv6> objs = XtmConfig.readConfig(FirewallIpv6.class);
		for (FirewallIpv6 fi : objs) {
			if (fi.src != null && cid.equals(fi.src.getCid()))
				return true;
			if (fi.dest != null && cid.equals(fi.dest.getCid()))
				return true;
			if (cid.equals(fi.headerCid))
				return true;
			if (fi.service != null && cid.equals(fi.service.getCid()))
				return true;
			if (cid.equals(fi.scheduleCid))
				return true;
			if (cid.equals(fi.qosCid))
				return true;
			if (cid.equals(fi.sessionCid))
				return true;
			if (cid.equals(fi.ipsecsaCid))
				return true;
		}
		return false;
	}

	public static void updateCid(String cid, String newName) {
		List<FirewallIpv6> objs = XtmConfig.readConfig(FirewallIpv6.class);
		boolean update = false;
		for (FirewallIpv6 fi : objs) {
			if (fi.src != null && cid.equals(fi.src.getCid())) {
				fi.src.setName(newName);
				update = true;
			}
			if (fi.dest != null && cid.equals(fi.dest.getCid())) {
				fi.dest.setName(newName);
				update = true;
			}
			if (cid.equals(fi.headerCid)) {
				fi.headerName = newName;
				update = true;
			}
			if (fi.service != null && cid.equals(fi.service.getCid())) {
				fi.service.setName(newName);
				update = true;
			}
			if (cid.equals(fi.scheduleCid)) {
				fi.scheduleName = newName;
				update = true;
			}
			if (cid.equals(fi.qosCid)) {
				fi.qosName = newName;
				update = true;
			}
			if (cid.equals(fi.sessionCid)) {
				fi.sessionName = newName;
				update = true;
			}
			if (cid.equals(fi.ipsecsaCid)) {
				fi.ipsecsaName = newName;
				update = true;
			}
		}

		if (update)
			XtmConfig.writeConfig(FirewallIpv6.class, objs);
	}

	public static FirewallIpv6 parse(NodeWrapper nw) {
		if (!nw.isName("spd"))
			return null;

		FirewallIpv6 fi = new FirewallIpv6();
		fi.num = nw.intAttr("num");
		fi.cid = nw.attr("cid");
		fi.use = nw.boolAttr("use");

		for (NodeWrapper c : nw.children()) {
			if (c.isName("src"))
				fi.src = new Address(c);
			else if (c.isName("dest"))
				fi.dest = new Address(c);
			else if (c.isName("v6header")) {
				fi.headerCid = c.attr("cid");
				fi.headerName = c.value();
			} else if (c.isName("service"))
				fi.service = new Service(c);
			else if (c.isName("schedule")) {
				fi.scheduleCid = c.attr("cid");
				fi.scheduleName = c.value();
			} else if (c.isName("qos")) {
				fi.qosCid = c.attr("cid");
				fi.qosName = c.value();
			} else if (c.isName("session")) {
				fi.sessionCid = c.attr("cid");
				fi.sessionName = c.value();
			} else if (c.isName("ips")) {
				fi.ipsPid = c.intAttr("pid");
				fi.ips = c.boolValue();
			} else if (c.isName("ipsecsa")) {
				fi.ipsecsaCid = c.attr("cid");
				fi.ipsecsaName = c.value();
			} else if (c.isName("etc")) {
				fi.etcAction = EtcAction.valueOf(c.attr("action"));
				fi.etcTimeout = c.intAttr("timeout");
				fi.etcCrossSpd = c.boolAttr("cross_spd");
				fi.loglevel = LogLevel.valueOf(c.attr("loglevel"));
			} else if (c.isName("desc"))
				fi.desc = c.value();
		}

		return fi;
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

	public Address getSrc() {
		return src;
	}

	public void setSrc(Address src) {
		this.src = src;
	}

	public Address getDest() {
		return dest;
	}

	public void setDest(Address dest) {
		this.dest = dest;
	}

	public String getHeaderCid() {
		return headerCid;
	}

	public void setHeaderCid(String headerCid) {
		this.headerCid = Utils.cid(headerCid);
	}

	public String getHeaderName() {
		return headerName;
	}

	public void setHeaderName(String headerName) {
		this.headerName = headerName;
	}

	public Service getService() {
		return service;
	}

	public void setService(Service service) {
		this.service = service;
	}

	public String getScheduleCid() {
		return scheduleCid;
	}

	public void setScheduleCid(String scheduleCid) {
		this.scheduleCid = Utils.cid(scheduleCid);
	}

	public String getScheduleName() {
		return scheduleName;
	}

	public void setScheduleName(String scheduleName) {
		this.scheduleName = scheduleName;
	}

	public String getQosCid() {
		return qosCid;
	}

	public void setQosCid(String qosCid) {
		this.qosCid = Utils.cid(qosCid);
	}

	public String getQosName() {
		return qosName;
	}

	public void setQosName(String qosName) {
		this.qosName = qosName;
	}

	public String getSessionCid() {
		return sessionCid;
	}

	public void setSessionCid(String sessionCid) {
		this.sessionCid = Utils.cid(sessionCid);
	}

	public String getSessionName() {
		return sessionName;
	}

	public void setSessionName(String sessionName) {
		this.sessionName = sessionName;
	}

	public int getIpsPid() {
		return ipsPid;
	}

	public void setIpsPid(int ipsPid) {
		this.ipsPid = ipsPid;
	}

	public boolean isIps() {
		return ips;
	}

	public void setIps(boolean ips) {
		this.ips = ips;
	}

	public String getIpsecsaCid() {
		return ipsecsaCid;
	}

	public void setIpsecsaCid(String ipsecsaCid) {
		this.ipsecsaCid = Utils.cid(ipsecsaCid);
	}

	public String getIpsecsaName() {
		return ipsecsaName;
	}

	public void setIpsecsaName(String ipsecsaName) {
		this.ipsecsaName = ipsecsaName;
	}

	public EtcAction getEtcAction() {
		return etcAction;
	}

	public void setEtcAction(EtcAction etcAction) {
		this.etcAction = etcAction;
	}

	public int getEtcTimeout() {
		return etcTimeout;
	}

	public void setEtcTimeout(int etcTimeout) {
		this.etcTimeout = etcTimeout;
	}

	public boolean isEtcCrossSpd() {
		return etcCrossSpd;
	}

	public void setEtcCrossSpd(boolean etcCrossSpd) {
		this.etcCrossSpd = etcCrossSpd;
	}

	public LogLevel getLoglevel() {
		return loglevel;
	}

	public void setLoglevel(LogLevel loglevel) {
		this.loglevel = loglevel;
	}

	public String getDesc() {
		return desc;
	}

	public void setDesc(String desc) {
		this.desc = desc;
	}

	@Override
	protected Element convertToElement(Document doc) {
		Element e = doc.createElement("spd");

		e.setAttribute("num", String.valueOf(num));
		e.setAttribute("cid", cid);
		e.setAttribute("use", Utils.bool(use));
		e.appendChild(src.toElement(doc, "src"));
		e.appendChild(dest.toElement(doc, "dest"));
		appendChild(doc, e, "v6header", headerName, new AttributeBuilder("cid", headerCid));
		e.appendChild(service.toElement(doc, "service"));
		appendChild(doc, e, "schedule", scheduleName, new AttributeBuilder("cid", scheduleCid));
		appendChild(doc, e, "qos", qosName, new AttributeBuilder("cid", qosCid));
		appendChild(doc, e, "session", sessionName, new AttributeBuilder("cid", sessionCid));
		appendChild(doc, e, "ips", ips, new AttributeBuilder("pid", ipsPid));
		appendChild(doc, e, "ipsecsa", ipsecsaName, new AttributeBuilder("cid", ipsecsaCid));
		AttributeBuilder etcAttr = new AttributeBuilder("action", etcAction).put("cross_spd", etcCrossSpd)
				.put("timeout", etcTimeout).put("loglevel", loglevel);
		appendChild(doc, e, "etc", null, etcAttr);
		appendChild(doc, e, "desc", desc);

		return e;
	}

	@Override
	public Map<String, Object> marshal() {
		Map<String, Object> m = new HashMap<String, Object>();
		m.put("num", num);
		m.put("cid", cid);
		m.put("use", use);
		m.put("src", src.marshal());
		m.put("dest", dest.marshal());
		m.put("header", new MarshalValue("cid", headerCid).put("name", headerName).get());
		m.put("service", service.marshal());
		m.put("schedule", new MarshalValue("cid", scheduleCid).put("name", scheduleName).get());
		m.put("qos", new MarshalValue("cid", qosCid).put("name", qosName).get());
		m.put("session", new MarshalValue("cid", sessionCid).put("name", sessionName).get());
		m.put("ips", new MarshalValue("use", ips).put("pid", ipsPid).get());
		m.put("ipsecsa", new MarshalValue("cid", ipsecsaCid).put("name", ipsecsaName).get());
		m.put("etc", new MarshalValue("action", etcAction).put("cross_spd", etcCrossSpd).put("timeout", etcTimeout)
				.get());
		m.put("log_level", loglevel);
		m.put("desc", desc);

		return m;
	}

	@Override
	public String toString() {
		return "FirewallIpv6 [num=" + num + ", cid=" + cid + ", use=" + use + ", src=" + src + ", dest=" + dest
				+ ", headerCid=" + headerCid + ", headerName=" + headerName + ", service=" + service + ", scheduleCid="
				+ scheduleCid + ", scheduleName=" + scheduleName + ", qosCid=" + qosCid + ", qosName=" + qosName
				+ ", sessionCid=" + sessionCid + ", sessionName=" + sessionName + ", ipsPid=" + ipsPid + ", ips=" + ips
				+ ", ipsecsaCid=" + ipsecsaCid + ", ipsecsaName=" + ipsecsaName + ", etcAction=" + etcAction
				+ ", etcTimeout=" + etcTimeout + ", etcCrossSpd=" + etcCrossSpd + ", loglevel=" + loglevel + ", desc="
				+ desc + "]";
	}
}
