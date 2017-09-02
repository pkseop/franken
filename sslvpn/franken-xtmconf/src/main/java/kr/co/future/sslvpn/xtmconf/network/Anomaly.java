package kr.co.future.sslvpn.xtmconf.network;

import java.util.HashMap;
import java.util.Map;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import kr.co.future.sslvpn.xtmconf.AttributeBuilder;
import kr.co.future.sslvpn.xtmconf.MarshalValue;
import kr.co.future.sslvpn.xtmconf.NodeWrapper;
import kr.co.future.sslvpn.xtmconf.Utils;
import kr.co.future.sslvpn.xtmconf.XtmConfig;

public class Anomaly extends XtmConfig {
	public static enum Type {
		DOS, DDOS, HTTP_CC;

		public static Type get(String str) {
			for (Type t : Type.values()) {
				if (t.toString().equals(str))
					return t;
			}
			return null;
		}

		@Override
		public String toString() {
			return name().toLowerCase();
		}
	}

	public static enum Level {
		Low, Middle, High;

		public static Level get(String str) {
			for (Level l : Level.values()) {
				if (l.toString().equals(str))
					return l;
			}
			return null;
		}

		@Override
		public String toString() {
			return name().toLowerCase();
		}
	}

	private Type type;
	private boolean use; // 사용여부 (체크)
	private AnomalyAction action; // Action
	private Integer blockTime; // Block Time
	private Level synLevel; // SYN 민감도
	private Integer syn; // SYN PPS (분산도)
	private Level udpLevel; // UDP 민감도
	private Integer udp; // UDP PPS (분산도)
	private Level icmpLevel; // ICMP 민감도
	private Integer icmp; // ICMP PPS (분산도)
	private Level httpGetLevel; // HTTP GET 민감도
	private Integer httpGet; // HTTP GET PPS (분산도)
	private Integer pingLimit; // Ping Packet Size Limit

	@Override
	public String getXmlFilename() {
		return "network_anomaly.xml";
	}

	@Override
	public String getRootTagName() {
		return "anomaly";
	}

	public static Anomaly parse(NodeWrapper nw) {
		if (!nw.isName("dos") && !nw.isName("ddos") && !nw.isName("http_cc"))
			return null;

		Anomaly a = new Anomaly();
		a.type = Type.get(nw.name());

		a.use = nw.boolAttr("chk_use");
		for (NodeWrapper c : nw.children()) {
			if (c.isName("action"))
				a.action = AnomalyAction.get(c.value());
			else if (c.isName("block_time") && (a.type == Type.DOS || a.type == Type.DDOS))
				a.blockTime = c.intValue();
			else if (c.isName("syn") && (a.type == Type.DOS || a.type == Type.DDOS))
				a.syn = c.intValue();
			else if (c.isName("udp") && (a.type == Type.DOS || a.type == Type.DDOS))
				a.udp = c.intValue();
			else if (c.isName("icmp") && (a.type == Type.DOS || a.type == Type.DDOS))
				a.icmp = c.intValue();
			else if (c.isName("http_get") && (a.type == Type.DOS || a.type == Type.DDOS))
				a.httpGet = c.intValue();
			else if (c.isName("ping_limit") && a.type == Type.DOS)
				a.pingLimit = c.intValue();

			if (c.isName("syn") && a.type == Type.DDOS)
				a.synLevel = Level.get(c.attr("level"));
			else if (c.isName("udp") && a.type == Type.DDOS)
				a.udpLevel = Level.get(c.attr("level"));
			else if (c.isName("icmp") && a.type == Type.DDOS)
				a.icmpLevel = Level.get(c.attr("level"));
			else if (c.isName("http_get") && a.type == Type.DDOS)
				a.httpGetLevel = Level.get(c.attr("level"));
		}

		return a;
	}

	public Type getType() {
		return type;
	}

	public void setType(Type type) {
		this.type = type;
	}

	public boolean isUse() {
		return use;
	}

	public void setUse(boolean use) {
		this.use = use;
	}

	public AnomalyAction getAction() {
		return action;
	}

	public void setAction(AnomalyAction action) {
		this.action = action;
	}

	public Integer getBlockTime() {
		return blockTime;
	}

	public void setBlockTime(Integer blockTime) {
		this.blockTime = blockTime;
	}

	public Level getSynLevel() {
		return synLevel;
	}

	public void setSynLevel(Level synLevel) {
		this.synLevel = synLevel;
	}

	public Integer getSyn() {
		return syn;
	}

	public void setSyn(Integer syn) {
		this.syn = syn;
	}

	public Level getUdpLevel() {
		return udpLevel;
	}

	public void setUdpLevel(Level udpLevel) {
		this.udpLevel = udpLevel;
	}

	public Integer getUdp() {
		return udp;
	}

	public void setUdp(Integer udp) {
		this.udp = udp;
	}

	public Level getIcmpLevel() {
		return icmpLevel;
	}

	public void setIcmpLevel(Level icmpLevel) {
		this.icmpLevel = icmpLevel;
	}

	public Integer getIcmp() {
		return icmp;
	}

	public void setIcmp(Integer icmp) {
		this.icmp = icmp;
	}

	public Level getHttpGetLevel() {
		return httpGetLevel;
	}

	public void setHttpGetLevel(Level httpGetLevel) {
		this.httpGetLevel = httpGetLevel;
	}

	public Integer getHttpGet() {
		return httpGet;
	}

	public void setHttpGet(Integer httpGet) {
		this.httpGet = httpGet;
	}

	public Integer getPingLimit() {
		return pingLimit;
	}

	public void setPingLimit(Integer pingLimit) {
		this.pingLimit = pingLimit;
	}

	@Override
	protected Element convertToElement(Document doc) {
		Element e = doc.createElement(type.toString());

		e.setAttribute("chk_use", Utils.bool(use));
		appendChild(doc, e, "action", action);

		if (type == Type.DOS) {
			appendChild(doc, e, "block_time", blockTime);
			appendChild(doc, e, "syn", syn);
			appendChild(doc, e, "udp", udp);
			appendChild(doc, e, "icmp", icmp);
			appendChild(doc, e, "http_get", httpGet);
			appendChild(doc, e, "ping_limit", pingLimit);
		} else if (type == Type.DDOS) {
			appendChild(doc, e, "block_time", blockTime);
			appendChild(doc, e, "syn", syn, new AttributeBuilder("level", synLevel));
			appendChild(doc, e, "udp", udp, new AttributeBuilder("level", udpLevel));
			appendChild(doc, e, "icmp", icmp, new AttributeBuilder("level", icmpLevel));
			appendChild(doc, e, "http_get", httpGet, new AttributeBuilder("level", httpGetLevel));
		}

		return e;
	}

	@Override
	public Map<String, Object> marshal() {
		Map<String, Object> m = new HashMap<String, Object>();
		m.put("type", type);
		m.put("use", use);
		m.put("action", action);

		if (type == Type.DOS) {
			m.put("block_time", blockTime);
			m.put("syn", syn);
			m.put("udp", udp);
			m.put("icmp", icmp);
			m.put("http_get", httpGet);
			m.put("ping_limit", pingLimit);
		} else if (type == Type.DDOS) {
			m.put("block_time", blockTime);
			m.put("syn", new MarshalValue("dispersion", syn).put("level", synLevel).get());
			m.put("udp", new MarshalValue("dispersion", udp).put("level", udpLevel).get());
			m.put("icmp", new MarshalValue("dispersion", icmp).put("level", icmpLevel).get());
			m.put("http_get", new MarshalValue("dispersion", httpGet).put("level", httpGetLevel).get());
		}

		return m;
	}

	@Override
	public String toString() {
		return "Anomaly [type=" + type + ", use=" + use + ", action=" + action + ", blockTime=" + blockTime
				+ ", synLevel=" + synLevel + ", syn=" + syn + ", udpLevel=" + udpLevel + ", udp=" + udp
				+ ", icmpLevel=" + icmpLevel + ", icmp=" + icmp + ", httpGetLevel=" + httpGetLevel + ", httpGet="
				+ httpGet + ", pingLimit=" + pingLimit + "]";
	}
}
