package kr.co.future.sslvpn.xtmconf.network;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import kr.co.future.sslvpn.xtmconf.AttributeBuilder;
import kr.co.future.sslvpn.xtmconf.MarshalValue;
import kr.co.future.sslvpn.xtmconf.NodeWrapper;
import kr.co.future.sslvpn.xtmconf.Utils;
import kr.co.future.sslvpn.xtmconf.XtmConfig;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class RouterMulticast extends XtmConfig {
	public static enum RP {
		Bootstrap, Static;

		public static RP get(String str) {
			for (RP r : RP.values()) {
				if (r.toString().equals(str))
					return r;
			}
			return null;
		}

		@Override
		public String toString() {
			return name().toLowerCase();
		}
	}

	private boolean use; // 멀티캐스트 사용
	private RP rp; // RP 동작 모드
	private Integer bootstrapPriority; // Bootstrap 우선순위
	private Integer candidateCycle; // Candidate 주기
	private Integer candidatePriority; // Candidate 우선순위
	private String address; // RP 주소
	private Boolean multiRp; // 다중 RP 사용
	private Integer registerRate; // STP 전환조건-Register rate
	private Integer registerCycle; // STP 전환조건-검사 주기
	private Integer dataRate; // STP 전환조건-Data rate
	private Integer dataCycle; // STP 전환조건-검사 주기
	private List<String> iface = new ArrayList<String>(); // 사용 인터페이스

	@Override
	public String getXmlFilename() {
		return "network_router_multicast.xml";
	}

	@Override
	public String getRootTagName() {
		return "network";
	}

	public static RouterMulticast parse(NodeWrapper nw) {
		if (!nw.isName("multicast"))
			return null;

		RouterMulticast rm = new RouterMulticast();
		rm.use = nw.boolAttr("chk_use");

		for (NodeWrapper c : nw.children()) {
			if (c.isName("rp"))
				rm.rp = RP.get(c.attr("action"));
			else if (c.isName("bootstrap"))
				rm.bootstrapPriority = c.intAttr("priority");
			else if (c.isName("candidate")) {
				rm.candidateCycle = c.intAttr("cycle");
				rm.candidatePriority = c.intAttr("priority");
			} else if (c.isName("static"))
				rm.address = c.attr("address");
			else if (c.isName("multiRP"))
				rm.multiRp = c.boolValue();
			else if (c.isName("spt")) {
				for (NodeWrapper s : c.children()) {
					if (s.isName("register")) {
						rm.registerRate = s.intAttr("rate");
						rm.registerCycle = s.intAttr("cycle");
					} else if (s.isName("data")) {
						rm.dataRate = s.intAttr("rate");
						rm.dataCycle = s.intAttr("cycle");
					}
				}
			} else if (c.isName("interface"))
				rm.iface.add(c.value());
		}

		return rm;
	}

	public boolean isUse() {
		return use;
	}

	public void setUse(boolean use) {
		this.use = use;
	}

	public RP getRp() {
		return rp;
	}

	public void setRp(RP rp) {
		this.rp = rp;
	}

	public Integer getBootstrapPriority() {
		return bootstrapPriority;
	}

	public void setBootstrapPriority(Integer bootstrapPriority) {
		this.bootstrapPriority = bootstrapPriority;
	}

	public Integer getCandidateCycle() {
		return candidateCycle;
	}

	public void setCandidateCycle(Integer candidateCycle) {
		this.candidateCycle = candidateCycle;
	}

	public Integer getCandidatePriority() {
		return candidatePriority;
	}

	public void setCandidatePriority(Integer candidatePriority) {
		this.candidatePriority = candidatePriority;
	}

	public String getAddress() {
		return address;
	}

	public void setAddress(String address) {
		this.address = address;
	}

	public Boolean isMultiRp() {
		return multiRp;
	}

	public void setMultiRp(Boolean multiRp) {
		this.multiRp = multiRp;
	}

	public Integer getRegisterRate() {
		return registerRate;
	}

	public void setRegisterRate(Integer registerRate) {
		this.registerRate = registerRate;
	}

	public Integer getRegisterCycle() {
		return registerCycle;
	}

	public void setRegisterCycle(Integer registerCycle) {
		this.registerCycle = registerCycle;
	}

	public Integer getDataRate() {
		return dataRate;
	}

	public void setDataRate(Integer dataRate) {
		this.dataRate = dataRate;
	}

	public Integer getDataCycle() {
		return dataCycle;
	}

	public void setDataCycle(Integer dataCycle) {
		this.dataCycle = dataCycle;
	}

	public List<String> getIface() {
		return iface;
	}

	public void setIface(List<String> iface) {
		this.iface = iface;
	}

	@Override
	protected Element convertToElement(Document doc) {
		Element e = doc.createElement("multicast");

		e.setAttribute("chk_use", Utils.bool(use));
		appendChild(doc, e, "rp", null, new AttributeBuilder("action", rp));
		appendChild(doc, e, "bootstrap", null, new AttributeBuilder("priority", bootstrapPriority));
		appendChild(doc, e, "candidate", null, new AttributeBuilder("cycle", candidateCycle).put("priority", candidatePriority));
		appendChild(doc, e, "static", null, new AttributeBuilder("address", address));
		appendChild(doc, e, "multiRP", Utils.bool(multiRp));
		Element s = appendChild(doc, e, "spt", null);
		appendChild(doc, s, "register", null, new AttributeBuilder("rate", registerRate).put("cycle", registerCycle));
		appendChild(doc, s, "data", null, new AttributeBuilder("rate", dataRate).put("cycle", dataCycle));

		for (String str : iface)
			appendChild(doc, e, "interface", str);

		return e;
	}

	@Override
	public Map<String, Object> marshal() {
		Map<String, Object> m = new HashMap<String, Object>();
		m.put("use", use);
		m.put("rp", rp);
		m.put("bootstrap_priority", bootstrapPriority);
		m.put("candidate", new MarshalValue("cycle", candidateCycle).put("priority", candidatePriority).get());
		m.put("address", address);
		m.put("multi_rp", multiRp);
		m.put("spt",
				new MarshalValue("register", new MarshalValue("rate", registerRate).put("cycle", registerCycle).get()).put("data",
						new MarshalValue("rate", dataRate).put("cycle", dataCycle).get()).get());
		m.put("interface", iface);

		return m;
	}

	@Override
	public String toString() {
		return "RouterMulticast [use=" + use + ", rp=" + rp + ", bootstrapPriority=" + bootstrapPriority + ", candidateCycle=" + candidateCycle
				+ ", candidatePriority=" + candidatePriority + ", address=" + address + ", multiRp=" + multiRp + ", registerRate=" + registerRate
				+ ", registerCycle=" + registerCycle + ", dataRate=" + dataRate + ", dataCycle=" + dataCycle + ", iface=" + iface + "]";
	}
}
