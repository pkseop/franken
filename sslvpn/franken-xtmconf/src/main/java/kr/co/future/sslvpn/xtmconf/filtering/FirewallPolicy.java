package kr.co.future.sslvpn.xtmconf.filtering;

import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import kr.co.future.sslvpn.xtmconf.AttributeBuilder;
import kr.co.future.sslvpn.xtmconf.MarshalValue;
import kr.co.future.sslvpn.xtmconf.NodeWrapper;
import kr.co.future.sslvpn.xtmconf.Utils;
import kr.co.future.sslvpn.xtmconf.XtmConfig;

public class FirewallPolicy extends XtmConfig {
	private static Integer vlanId;
	private int num;
	private String cid;
	private boolean use; // 동작
	private Address src; // 출발주소
	private Address dest; // 도착주소
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
	private boolean etcCrossSpd; // 양방향정책
	private LogLevel loglevel; // 로그
	private Date create;
	private int uid;
	private String desc; // 기타정보

	@Override
	public String getXmlFilename() {
		String prefix = (vlanId != null) ? String.format("VLAN_ID_%d/", vlanId) : "";
		return prefix + "firewall_policy.xml";
	}

	public static Integer getVlanId() {
		return vlanId;
	}

	public static void setVlanId(Integer vlanId) {
		FirewallPolicy.vlanId = vlanId;
	}

	@Override
	public String getRootTagName() {
		return "policy";
	}

	public static boolean hasCid(String cid) {
		try {
			Iterator<VlanSetting> vsi = XtmConfig.readConfig(VlanSetting.class).iterator();
			Integer vlanId = null;
			do {
				setVlanId(vlanId);
				List<FirewallPolicy> objs = XtmConfig.readConfig(FirewallPolicy.class);
				for (FirewallPolicy fn : objs) {
					if (fn.src != null && cid.equals(fn.src.getCid()))
						return true;
					if (fn.dest != null && cid.equals(fn.dest.getCid()))
						return true;
					if (fn.service != null && cid.equals(fn.service.getCid()))
						return true;
					if (cid.equals(fn.scheduleCid))
						return true;
					if (cid.equals(fn.qosCid))
						return true;
					if (cid.equals(fn.sessionCid))
						return true;
					if (cid.equals(fn.ipsecsaCid))
						return true;
				}
				if (!vsi.hasNext())
					break;
				vlanId = vsi.next().getId();
			} while (true);
			return false;
		} finally {
			setVlanId(null);
		}
	}

	public static void updateCid(String cid, String newName) {
		try {
			Iterator<VlanSetting> vsi = XtmConfig.readConfig(VlanSetting.class).iterator();
			Integer vlanId = null;
			do {
				setVlanId(vlanId);
				List<FirewallPolicy> objs = XtmConfig.readConfig(FirewallPolicy.class);
				boolean update = false;
				for (FirewallPolicy fn : objs) {
					if (fn.src != null && cid.equals(fn.src.getCid())) {
						fn.src.setName(newName);
						update = true;
					}
					if (fn.dest != null && cid.equals(fn.dest.getCid())) {
						fn.dest.setName(newName);
						update = true;
					}
					if (fn.service != null && cid.equals(fn.service.getCid())) {
						fn.service.setName(newName);
						update = true;
					}
					if (cid.equals(fn.scheduleCid)) {
						fn.scheduleName = newName;
						update = true;
					}
					if (cid.equals(fn.qosCid)) {
						fn.qosName = newName;
						update = true;
					}
					if (cid.equals(fn.sessionCid)) {
						fn.sessionName = newName;
						update = true;
					}
					if (cid.equals(fn.ipsecsaCid)) {
						fn.ipsecsaName = newName;
						update = true;
					}
				}

				if (update) {
					setVlanId(vlanId);
					XtmConfig.writeConfig(FirewallPolicy.class, objs);
				}
				if (!vsi.hasNext())
					break;
				vlanId = vsi.next().getId();
			} while (true);
		} finally {
			setVlanId(null);
		}
	}

	public static FirewallPolicy parse(NodeWrapper nw) {
		if (!nw.isName("spd"))
			return null;

		FirewallPolicy fp = new FirewallPolicy();
		fp.num = nw.intAttr("num");
		fp.cid = nw.attr("cid");
		fp.use = nw.boolAttr("use");

		for (NodeWrapper c : nw.children()) {
			if (c.isName("src"))
				fp.src = new Address(c);
			else if (c.isName("dest"))
				fp.dest = new Address(c);
			else if (c.isName("service"))
				fp.service = new Service(c);
			else if (c.isName("schedule")) {
				fp.scheduleCid = c.attr("cid");
				fp.scheduleName = c.value();
			} else if (c.isName("qos")) {
				fp.qosCid = c.attr("cid");
				fp.qosName = c.value();
			} else if (c.isName("session")) {
				fp.sessionCid = c.attr("cid");
				fp.sessionName = c.value();
			} else if (c.isName("ips")) {
				fp.ipsPid = c.intAttr("pid");
				fp.ips = c.boolValue();
			} else if (c.isName("ipsecsa")) {
				fp.ipsecsaCid = c.attr("cid");
				fp.ipsecsaName = c.value();
			} else if (c.isName("etc")) {
				fp.etcAction = EtcAction.valueOf(c.attr("action"));
				fp.etcTimeout = c.intAttr("timeout");
				fp.loglevel = LogLevel.valueOf(c.attr("loglevel"));
				fp.etcCrossSpd = c.boolAttr("cross_spd");
			} else if (c.isName("create"))
				fp.create = new Date(c.intValue() * 1000L);
			else if (c.isName("uid"))
				fp.uid = c.intValue();
			else if (c.isName("desc"))
				fp.desc = c.value();
		}

		return fp;
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

	public Date getCreate() {
		return create;
	}

	public void setCreate(Date create) {
		this.create = create;
	}

	public int getUid() {
		return uid;
	}

	public void setUid(int uid) {
		this.uid = uid;
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
		e.appendChild(service.toElement(doc, "service"));
		appendChild(doc, e, "schedule", scheduleName, new AttributeBuilder("cid", scheduleCid));
		appendChild(doc, e, "qos", qosName, new AttributeBuilder("cid", qosCid));
		appendChild(doc, e, "session", sessionName, new AttributeBuilder("cid", sessionCid));
		appendChild(doc, e, "ips", ips, new AttributeBuilder("pid", ipsPid));
		appendChild(doc, e, "ipsecsa", ipsecsaName, new AttributeBuilder("cid", ipsecsaCid));
		AttributeBuilder actionAttr = new AttributeBuilder("action", etcAction).put("cross_spd", etcCrossSpd)
				.put("timeout", etcTimeout).put("loglevel", loglevel);
		appendChild(doc, e, "etc", null, actionAttr);
		if (create == null)
			create = new Date();
		appendChild(doc, e, "create", create.getTime() / 1000L);
		appendChild(doc, e, "uid", uid);
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
		m.put("service", service.marshal());
		m.put("schedule", new MarshalValue("cid", scheduleCid).put("name", scheduleName).get());
		m.put("qos", new MarshalValue("cid", qosCid).put("name", qosName).get());
		m.put("session", new MarshalValue("cid", sessionCid).put("name", sessionName).get());
		m.put("ips", new MarshalValue("use", ips).put("pid", ipsPid).get());
		m.put("ipsecsa", new MarshalValue("cid", ipsecsaCid).put("name", ipsecsaName).get());
		m.put("etc", new MarshalValue("action", etcAction).put("cross_spd", etcCrossSpd).put("timeout", etcTimeout)
				.get());
		m.put("log_level", loglevel);
		m.put("create", Utils.dateFormat(create));
		m.put("uid", uid);
		m.put("desc", desc);

		return m;
	}

	@Override
	public String toString() {
		return "FirewallPolicy [num=" + num + ", cid=" + cid + ", use=" + use + ", src=" + src + ", dest=" + dest
				+ ", service=" + service + ", scheduleCid=" + scheduleCid + ", scheduleName=" + scheduleName
				+ ", qosCid=" + qosCid + ", qosName=" + qosName + ", sessionCid=" + sessionCid + ", sessionName="
				+ sessionName + ", ipsPid=" + ipsPid + ", ips=" + ips + ", ipsecsaCid=" + ipsecsaCid + ", ipsecsaName="
				+ ipsecsaName + ", etcAction=" + etcAction + ", etcTimeout=" + etcTimeout + ", etcCrossSpd="
				+ etcCrossSpd + ", loglevel=" + loglevel + ", create=" + create + ", uid=" + uid + ", desc=" + desc
				+ "]";
	}
}
