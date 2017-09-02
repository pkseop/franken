package kr.co.future.sslvpn.xtmconf.network;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import kr.co.future.msgbus.Marshalable;
import kr.co.future.msgbus.Marshaler;
import kr.co.future.sslvpn.xtmconf.AttributeBuilder;
import kr.co.future.sslvpn.xtmconf.NodeWrapper;
import kr.co.future.sslvpn.xtmconf.XtmConfig;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class AnomalyPortscan extends XtmConfig {
	public static enum Type {
		Portscan, Extention, Watch, Scanner, Scanned;

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

	public static enum Protocol {
		Any, TCP, UDP, ICMP
	}

	public static enum Level {
		Low, Normal, High
	}

	private Type type;
	private boolean use; // Portscan Anomaly 검사
	private Protocol protocol; // Protocol
	private Level level; // Sense Level
	// Scan Type Extention
	private List<Extention> extention = new ArrayList<Extention>();
	private String ip; // IP
	private String netmask; // Netmask

	public static class Extention implements Marshalable {
		private String scanType; // Scan Type
		private AnomalyAction action; // Action
		private int time; // 차단시간
		private Integer dropType; // 차단유형, 1:N(1), N:1(2)
		private boolean use; // 사용

		public String getScanType() {
			return scanType;
		}

		public void setScanType(String scanType) {
			this.scanType = scanType;
		}

		public AnomalyAction getAction() {
			return action;
		}

		public void setAction(AnomalyAction action) {
			this.action = action;
		}

		public int getTime() {
			return time;
		}

		public void setTime(int time) {
			this.time = time;
		}

		public Integer getDropType() {
			return dropType;
		}

		public void setDropType(Integer dropType) {
			this.dropType = dropType;
		}

		public boolean isUse() {
			return use;
		}

		public void setUse(boolean use) {
			this.use = use;
		}

		@Override
		public Map<String, Object> marshal() {
			Map<String, Object> m = new HashMap<String, Object>();
			m.put("scan_type", scanType);
			m.put("action", action);
			m.put("time", time);
			m.put("drop_type", dropType);
			m.put("use", use);

			return m;
		}

		@Override
		public String toString() {
			return "Extention [scanType=" + scanType + ", action=" + action + ", time=" + time + ", dropType="
					+ dropType + ", use=" + use + "]";
		}
	}

	@Override
	public String getXmlFilename() {
		return "network_anomaly_portscan.xml";
	}

	@Override
	public String getRootTagName() {
		return "anomaly";
	}

	public static AnomalyPortscan parse(NodeWrapper nw) {
		if (Type.get(nw.name()) == null)
			return null;

		AnomalyPortscan ap = new AnomalyPortscan();
		ap.type = Type.get(nw.name());

		for (NodeWrapper c : nw.children()) {
			if (ap.type == Type.Portscan) {
				if (c.isName("setting")) {
					ap.use = c.boolAttr("chk_use");
					ap.protocol = Protocol.valueOf(c.attr("protocol"));
					ap.level = Level.valueOf(c.attr("level"));
				}
			} else if (ap.type == Type.Extention) {
				if (c.isName("setting")) {
					Extention e = new Extention();
					e.scanType = c.attr("scan_type");
					e.action = AnomalyAction.get(c.attr("action"));
					e.time = c.intAttr("time");
					e.dropType = c.intAttr("drop_type");
					e.use = c.boolAttr("use");
					ap.extention.add(e);
				}
			} else if (ap.type == Type.Watch || ap.type == Type.Scanner || ap.type == Type.Scanned) {
				if (c.isName("ip"))
					ap.ip = c.value();
				else if (c.isName("netmask"))
					ap.netmask = c.value();
			}
		}

		return ap;
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

	public Protocol getProtocol() {
		return protocol;
	}

	public void setProtocol(Protocol protocol) {
		this.protocol = protocol;
	}

	public Level getLevel() {
		return level;
	}

	public void setLevel(Level level) {
		this.level = level;
	}

	public List<Extention> getExtention() {
		return extention;
	}

	public void setExtention(List<Extention> extention) {
		this.extention = extention;
	}

	public String getIp() {
		return ip;
	}

	public void setIp(String ip) {
		this.ip = ip;
	}

	public String getNetmask() {
		return netmask;
	}

	public void setNetmask(String netmask) {
		this.netmask = netmask;
	}

	@Override
	protected Element convertToElement(Document doc) {
		Element e = doc.createElement(type.toString());

		if (type == Type.Portscan) {
			appendChild(doc, e, "setting", null,
					new AttributeBuilder("chk_use", use).put("protocol", protocol).put("level", level));
		} else if (type == Type.Extention) {
			for (Extention ex : extention) {
				AttributeBuilder exAttr = new AttributeBuilder("scan_type", ex.scanType).put("action", ex.action)
						.put("time", ex.time).put("drop_type", ex.dropType).put("use", ex.use);
				appendChild(doc, e, "setting", null, exAttr);
			}
		} else if (type == Type.Watch || type == Type.Scanner || type == Type.Scanned) {
			appendChild(doc, e, "ip", ip);
			appendChild(doc, e, "netmask", netmask);
		}

		return e;
	}

	@Override
	public Map<String, Object> marshal() {
		Map<String, Object> m = new HashMap<String, Object>();
		m.put("type", type);

		if (type == Type.Portscan) {
			m.put("use", use);
			m.put("protocol", protocol);
			m.put("level", level);
		} else if (type == Type.Extention)
			m.put("extention", Marshaler.marshal(extention));
		else if (type == Type.Watch || type == Type.Scanner || type == Type.Scanned) {
			m.put("ip", ip);
			m.put("netmask", netmask);
		}

		return m;
	}

	@Override
	public String toString() {
		return "AnomalyPortscan [type=" + type + ", usePortscan=" + use + ", protocol=" + protocol + ", level=" + level
				+ ", extention=" + extention + ", ip=" + ip + ", netmask=" + netmask + "]";
	}
}
