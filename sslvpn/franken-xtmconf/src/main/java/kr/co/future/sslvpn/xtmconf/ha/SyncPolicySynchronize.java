package kr.co.future.sslvpn.xtmconf.ha;

import java.util.HashMap;
import java.util.Map;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import kr.co.future.sslvpn.xtmconf.AttributeBuilder;
import kr.co.future.sslvpn.xtmconf.MarshalValue;
import kr.co.future.sslvpn.xtmconf.NodeWrapper;
import kr.co.future.sslvpn.xtmconf.Utils;
import kr.co.future.sslvpn.xtmconf.XtmConfig;

public class SyncPolicySynchronize extends XtmConfig {
	public static enum IntervalType {
		Hour, Day;

		public static IntervalType get(String str) {
			for (IntervalType i : IntervalType.values()) {
				if (i.toString().equals(str))
					return i;
			}
			return null;
		}

		@Override
		public String toString() {
			return name().toLowerCase();
		}
	}

	private boolean use; // 정책동기화 사용
	private ActMode act; // 선택모드
	private String ip; // SLAVE IP
	private Integer port; // SLAVE PORT
	private boolean interval; // 실행주기
	private IntervalType intervalType; // 실행주기
	private Integer intervalTerm; // 실행주기
	private boolean fwV4spd; // IPv4 필터링 룰
	private boolean fwV4nat; // IPv4 주소변환 룰
	private boolean fwV6spd; // IPv6 필터링 룰
	private boolean fwV6nat; // IPv6 주소변환 룰
	private boolean objectIp; // IP 주소
	private boolean objectService; // 서비스
	private boolean objectFlow; // 세션사용량 제한
	private boolean objectSchedule; // 스케쥴
	private boolean objectQos; // QOS
	private boolean dpi; // 룰 정보(세부설정포함)
	private boolean ipsec; // IPSEC 설정
	private boolean log; // 로그 설정
	private boolean syslog; // SYSLOG설정
	private boolean logAlarm; // 알람 설정
	private boolean domSync; // kraken-dom-localhost db
	private boolean deviceSync;// devices.tmp
	private String password;// password

	@Override
	public String getXmlFilename() {
		return "sync_policy_synchronize.xml";
	}

	@Override
	public String getRootTagName() {
		return "sync";
	}

	public static SyncPolicySynchronize parse(NodeWrapper nw) {
		if (!nw.isName("synchronize"))
			return null;

		SyncPolicySynchronize s = new SyncPolicySynchronize();
		s.use = nw.boolAttr("chk_use");

		for (NodeWrapper c : nw.children()) {
			if (c.isName("act"))
				s.act = ActMode.get(c.attr("mode"));
			else if (c.isName("password"))
				s.password = c.value();
			else if (c.isName("ip"))
				s.ip = c.value();
			else if (c.isName("port"))
				s.port = c.intValue();
			else if (c.isName("interval")) {
				s.interval = c.boolAttr("chk_use");
				s.intervalType = IntervalType.get(c.attr("type"));
				s.intervalTerm = c.intAttr("term");
			} else if (c.isName("option")) {
				for (NodeWrapper o : c.children()) {
					if (o.isName("fw")) {
						s.fwV4spd = o.boolAttr("chk_v4spd");
						s.fwV4nat = o.boolAttr("chk_v4nat");
						s.fwV6spd = o.boolAttr("chk_v6spd");
						s.fwV6nat = o.boolAttr("chk_v6nat");
					} else if (o.isName("object")) {
						s.objectIp = o.boolAttr("chk_ip");
						s.objectService = o.boolAttr("chk_service");
						s.objectFlow = o.boolAttr("chk_flow");
						s.objectSchedule = o.boolAttr("chk_schedule");
						s.objectQos = o.boolAttr("chk_qos");
					} else if (o.isName("dpi"))
						s.dpi = o.boolAttr("chk_dpi");
					else if (o.isName("ipsec"))
						s.ipsec = o.boolAttr("chk_ipsec");
					else if (o.isName("log")) {
						s.log = o.boolAttr("chk_log");
						s.syslog = o.boolAttr("chk_syslog");
						s.logAlarm = o.boolAttr("chk_alarm");
					} else if (o.isName("dbsync")) {
						s.domSync = o.boolAttr("chk_domsync");
						s.deviceSync = o.boolAttr("chk_devicesync");
					}
				}
			}
		}

		return s;
	}

	public boolean isDeviceSync() {
		return deviceSync;
	}

	public void setDeviceSync(boolean deviceSync) {
		this.deviceSync = deviceSync;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public boolean isUse() {
		return use;
	}

	public void setUse(boolean use) {
		this.use = use;
	}

	public ActMode getAct() {
		return act;
	}

	public void setAct(ActMode act) {
		this.act = act;
	}

	public String getIp() {
		return ip;
	}

	public void setIp(String ip) {
		this.ip = ip;
	}

	public Integer getPort() {
		return port;
	}

	public void setPort(Integer port) {
		this.port = port;
	}

	public boolean isInterval() {
		return interval;
	}

	public void setInterval(boolean interval) {
		this.interval = interval;
	}

	public IntervalType getIntervalType() {
		return intervalType;
	}

	public void setIntervalType(IntervalType intervalType) {
		this.intervalType = intervalType;
	}

	public Integer getIntervalTerm() {
		return intervalTerm;
	}

	public void setIntervalTerm(Integer intervalTerm) {
		this.intervalTerm = intervalTerm;
	}

	public boolean isFwV4spd() {
		return fwV4spd;
	}

	public void setFwV4spd(boolean fwV4spd) {
		this.fwV4spd = fwV4spd;
	}

	public boolean isFwV4nat() {
		return fwV4nat;
	}

	public void setFwV4nat(boolean fwV4nat) {
		this.fwV4nat = fwV4nat;
	}

	public boolean isFwV6spd() {
		return fwV6spd;
	}

	public void setFwV6spd(boolean fwV6spd) {
		this.fwV6spd = fwV6spd;
	}

	public boolean isFwV6nat() {
		return fwV6nat;
	}

	public void setFwV6nat(boolean fwV6nat) {
		this.fwV6nat = fwV6nat;
	}

	public boolean isObjectIp() {
		return objectIp;
	}

	public void setObjectIp(boolean objectIp) {
		this.objectIp = objectIp;
	}

	public boolean isObjectService() {
		return objectService;
	}

	public void setObjectService(boolean objectService) {
		this.objectService = objectService;
	}

	public boolean isObjectFlow() {
		return objectFlow;
	}

	public void setObjectFlow(boolean objectFlow) {
		this.objectFlow = objectFlow;
	}

	public boolean isObjectSchedule() {
		return objectSchedule;
	}

	public void setObjectSchedule(boolean objectSchedule) {
		this.objectSchedule = objectSchedule;
	}

	public boolean isObjectQos() {
		return objectQos;
	}

	public void setObjectQos(boolean objectQos) {
		this.objectQos = objectQos;
	}

	public boolean isDpi() {
		return dpi;
	}

	public void setDpi(boolean dpi) {
		this.dpi = dpi;
	}

	public boolean isIpsec() {
		return ipsec;
	}

	public void setIpsec(boolean ipsec) {
		this.ipsec = ipsec;
	}

	public boolean isLog() {
		return log;
	}

	public void setLog(boolean log) {
		this.log = log;
	}

	public boolean isSyslog() {
		return syslog;
	}

	public void setSyslog(boolean syslog) {
		this.syslog = syslog;
	}

	public boolean isLogAlarm() {
		return logAlarm;
	}

	public void setLogAlarm(boolean logAlarm) {
		this.logAlarm = logAlarm;
	}

	public boolean isDomSync() {
		return domSync;
	}

	public void setDomSync(boolean domSync) {
		this.domSync = domSync;
	}

	@Override
	protected Element convertToElement(Document doc) {
		Element e = doc.createElement("synchronize");

		e.setAttribute("chk_use", Utils.bool(use));
		appendChild(doc, e, "act", null, new AttributeBuilder("mode", act));
		appendChild(doc, e, "password", password);
		appendChild(doc, e, "ip", ip);
		appendChild(doc, e, "port", port);
		AttributeBuilder intervalAttr = new AttributeBuilder("chk_use", interval).put("type", intervalType).put("term",
				intervalTerm);
		appendChild(doc, e, "interval", null, intervalAttr);
		Element option = appendChild(doc, e, "option", null);
		AttributeBuilder fwAttr = new AttributeBuilder("chk_v4spd", fwV4spd).put("chk_v4nat", fwV4nat).put("chk_v6spd", fwV6spd)
				.put("chk_v6nat", fwV6nat);
		appendChild(doc, option, "fw", null, fwAttr);
		AttributeBuilder objectAttr = new AttributeBuilder("chk_ip", objectIp).put("chk_service", objectService)
				.put("chk_flow", objectFlow).put("chk_schedule", objectSchedule).put("chk_qos", objectQos);
		appendChild(doc, option, "object", null, objectAttr);
		appendChild(doc, option, "dpi", null, new AttributeBuilder("chk_dpi", dpi));
		appendChild(doc, option, "ipsec", null, new AttributeBuilder("chk_ipsec", ipsec));
		AttributeBuilder logAttr = new AttributeBuilder("chk_log", log).put("chk_syslog", syslog).put("chk_alarm", logAlarm);
		appendChild(doc, option, "log", null, logAttr);
		appendChild(doc, option, "dbsync", null, new AttributeBuilder("chk_domsync", domSync).put("chk_devicesync", deviceSync));

		return e;
	}

	@Override
	public Map<String, Object> marshal() {
		Map<String, Object> m = new HashMap<String, Object>();
		m.put("use", use);
		m.put("act", act);
		m.put("password", password);
		m.put("ip", ip);
		m.put("port", port);
		m.put("interval", new MarshalValue("use", interval).put("type", intervalType).put("term", intervalTerm).get());
		m.put("fw_v4", new MarshalValue("spd", fwV4spd).put("nat", fwV4nat).get());
		m.put("fw_v6", new MarshalValue("spd", fwV6spd).put("nat", fwV6nat).get());
		m.put("object",
				new MarshalValue("ip", objectIp).put("service", objectService).put("flow", objectFlow)
						.put("schedule", objectSchedule).put("qos", objectQos).get());
		m.put("dpi", dpi);
		m.put("ipsec", ipsec);
		m.put("log", new MarshalValue("log", log).put("syslog", syslog).put("alarm", logAlarm).get());
		m.put("dbsync", new MarshalValue("domsync", domSync).put("devicesync", deviceSync).get());

		return m;
	}

	@Override
	public String toString() {
		return "SyncPolicySynchronize [use=" + use + ", act=" + act + ", ip=" + ip + ", port=" + port + ", interval=" + interval
				+ ", intervalType=" + intervalType + ", intervalTerm=" + intervalTerm + ", fwV4spd=" + fwV4spd + ", fwV4nat="
				+ fwV4nat + ", fwV6spd=" + fwV6spd + ", fwV6nat=" + fwV6nat + ", objectIp=" + objectIp + ", objectService="
				+ objectService + ", objectFlow=" + objectFlow + ", objectSchedule=" + objectSchedule + ", objectQos="
				+ objectQos + ", dpi=" + dpi + ", ipsec=" + ipsec + ", log=" + log + ", syslog=" + syslog + ", logAlarm="
				+ logAlarm + ", dbSync=" + domSync + ", deviceSync=" + deviceSync + "]";
	}

}
