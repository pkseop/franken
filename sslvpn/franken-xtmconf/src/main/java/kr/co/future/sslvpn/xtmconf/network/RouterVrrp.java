package kr.co.future.sslvpn.xtmconf.network;

import java.util.HashMap;
import java.util.Map;

import kr.co.future.sslvpn.xtmconf.AttributeBuilder;
import kr.co.future.sslvpn.xtmconf.NodeWrapper;
import kr.co.future.sslvpn.xtmconf.XtmConfig;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class RouterVrrp extends XtmConfig {
	public static enum Mode {
		Master, Backup;

		public static Mode get(String str) {
			for (Mode m : Mode.values()) {
				if (m.toString().equals(str))
					return m;
			}
			return null;
		}

		@Override
		public String toString() {
			return name().toLowerCase();
		}
	}

	private int num;
	private String iface; // 인터페이스
	private Mode mode; // Failover-동작모드
	private int vid; // 그룹 ID
	private int priority; // Priority
	private int period; // 주기
	private String vip; // 그룹 IP
	private Integer boostup; // Failover-boostup

	@Override
	public String getXmlFilename() {
		return "network_router_vrrp.xml";
	}

	@Override
	public String getRootTagName() {
		return "routing";
	}

	public static RouterVrrp parse(NodeWrapper nw) {
		if (!nw.isName("vrrp"))
			return null;

		RouterVrrp v = new RouterVrrp();
		v.num = nw.intAttr("num");

		for (NodeWrapper c : nw.children()) {
			if (c.isName("setting")) {
				v.iface = c.attr("interface");
				v.mode = Mode.get(c.attr("mode"));
			} else if (c.isName("vid"))
				v.vid = c.intValue();
			else if (c.isName("priority"))
				v.priority = c.intValue();
			else if (c.isName("period"))
				v.period = c.intValue();
			else if (c.isName("vip"))
				v.vip = c.value();
			else if (c.isName("boostup"))
				v.boostup = c.intValue();
		}

		return v;
	}

	public int getNum() {
		return num;
	}

	public void setNum(int num) {
		this.num = num;
	}

	public String getIface() {
		return iface;
	}

	public void setIface(String iface) {
		this.iface = iface;
	}

	public Mode getMode() {
		return mode;
	}

	public void setMode(Mode mode) {
		this.mode = mode;
	}

	public int getVid() {
		return vid;
	}

	public void setVid(int vid) {
		this.vid = vid;
	}

	public int getPriority() {
		return priority;
	}

	public void setPriority(int priority) {
		this.priority = priority;
	}

	public int getPeriod() {
		return period;
	}

	public void setPeriod(int period) {
		this.period = period;
	}

	public String getVip() {
		return vip;
	}

	public void setVip(String vip) {
		this.vip = vip;
	}

	public Integer getBoostup() {
		return boostup;
	}

	public void setBoostup(Integer bootsup) {
		this.boostup = bootsup;
	}

	@Override
	protected Element convertToElement(Document doc) {
		Element e = doc.createElement("vrrp");

		e.setAttribute("num", String.valueOf(num));
		appendChild(doc, e, "setting", null, new AttributeBuilder("interface", iface).put("mode", mode));
		appendChild(doc, e, "vid", vid);
		appendChild(doc, e, "priority", priority);
		appendChild(doc, e, "period", period);
		appendChild(doc, e, "vip", vip);
		appendChild(doc, e, "boostup", boostup);

		return e;
	}

	@Override
	public Map<String, Object> marshal() {
		Map<String, Object> m = new HashMap<String, Object>();
		m.put("num", num);
		m.put("interface", iface);
		m.put("mode", mode);
		m.put("vid", vid);
		m.put("priority", priority);
		m.put("period", period);
		m.put("vip", vip);
		m.put("boostup", boostup);

		return m;
	}

	@Override
	public String toString() {
		return "Vrrp [num=" + num + ", iface=" + iface + ", mode=" + mode + ", vid=" + vid + ", priority=" + priority
				+ ", period=" + period + ", vip=" + vip + ", boostup=" + boostup + "]";
	}
}
