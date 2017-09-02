package kr.co.future.sslvpn.xtmconf.ha;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import kr.co.future.msgbus.Marshalable;
import kr.co.future.msgbus.Marshaler;
import kr.co.future.sslvpn.xtmconf.AttributeBuilder;
import kr.co.future.sslvpn.xtmconf.MarshalValue;
import kr.co.future.sslvpn.xtmconf.NodeWrapper;
import kr.co.future.sslvpn.xtmconf.XtmConfig;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class HeadScript extends XtmConfig {
	public static enum Type {
		HeadMode("head_mode"), MasterBackup("master_backup"), Bridge("bridge");

		private String name;

		private Type(String name) {
			this.name = name;
		}

		public static Type get(String str) {
			for (Type t : Type.values()) {
				if (t.toString().equals(str))
					return t;
			}
			return null;
		}

		@Override
		public String toString() {
			return name;
		}
	}

	public static enum Mode {
		Master, Backup, Bridge;

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

	private Type type;
	private Mode mode; // Mode
	private List<Checker> checker = new ArrayList<HeadScript.Checker>(); // Checker
	private boolean useRealIp; // IKE통신시 Real IP 사용
	private String checkerIface; // Bridge-Checker Interface
	private String checkerName; // Bridge-Checker Name
	private String checkerTargetIp; // Bridge-Checker Target IP
	private String checkerMode; // ?
	private Integer checkerTimeout; // Bridge-Checker Info
	private String relayInIface; // Bridge-Packet Relay 내부 Interface
	private String relayInMac; // Bridge-Packet Relay 내부 목적지 MAC 주소
	private String relayExIface; // Bridge-Packet Relay 외부 Interface
	private String relayExMac; // Bridge-Packet Relay 외부 목적지 MAC 주소

	public static class Checker implements Marshalable {
		public static enum Mode {
			Primary, Backup, HA_Link;

			public static Mode get(String str) {
				for (Mode m : Mode.values()) {
					if (m.toString().equals(str))
						return m;
				}
				return null;
			}

			@Override
			public String toString() {
				return name().replace("_", "-").toLowerCase();
			}
		}

		private String iface;
		private String name;
		private String targetIp;
		private Mode mode;
		private String virtualIp;
		private Integer timeout;

		public String getIface() {
			return iface;
		}

		public void setIface(String iface) {
			this.iface = iface;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public String getTargetIp() {
			return targetIp;
		}

		public void setTargetIp(String targetIp) {
			this.targetIp = targetIp;
		}

		public Mode getMode() {
			return mode;
		}

		public void setMode(Mode mode) {
			this.mode = mode;
		}

		public String getVirtualIp() {
			return virtualIp;
		}

		public void setVirtualIp(String virtualIp) {
			this.virtualIp = virtualIp;
		}

		public Integer getTimeout() {
			return timeout;
		}

		public void setTimeout(Integer timeout) {
			this.timeout = timeout;
		}

		@Override
		public Map<String, Object> marshal() {
			Map<String, Object> m = new HashMap<String, Object>();
			m.put("interface", iface);
			m.put("name", name);
			m.put("target_ip", targetIp);
			m.put("mode", mode);
			m.put("virtual_ip", virtualIp);
			m.put("timeout", timeout);

			return m;
		}

		@Override
		public String toString() {
			return "Checker [iface=" + iface + ", name=" + name + ", targetIp=" + targetIp + ", mode=" + mode
					+ ", virtualIp=" + virtualIp + ", timeout=" + timeout + "]";
		}
	}

	@Override
	public String getXmlFilename() {
		return "ha_head_script.xml";
	}

	@Override
	public String getRootTagName() {
		return "ha_head_script";
	}

	public static HeadScript parse(NodeWrapper nw) {
		if (!nw.isName("head_mode") && !nw.isName("master_backup") && !nw.isName("bridge"))
			return null;

		HeadScript hs = new HeadScript();
		hs.type = Type.get(nw.name());

		if (nw.isName("head_mode"))
			hs.mode = Mode.get(nw.attr("mode"));
		else if (nw.isName("master_backup")) {
			for (NodeWrapper c : nw.children()) {
				if (c.isName("checker_ha")) {
					Checker checker = new Checker();
					for (NodeWrapper ch : c.children()) {
						if (ch.isName("interface"))
							checker.iface = ch.value();
						else if (ch.isName("name"))
							checker.name = ch.value();
						else if (ch.isName("target_ip"))
							checker.targetIp = ch.value();
						else if (ch.isName("mode"))
							checker.mode = Checker.Mode.get(ch.value());
						else if (ch.isName("virtual_ip"))
							checker.virtualIp = ch.value();
						else if (ch.isName("timeout"))
							checker.timeout = ch.intValue();
					}
					hs.checker.add(checker);
				} else if (c.isName("use_real_ip"))
					hs.useRealIp = c.boolAttr("chk_use");
			}
		} else if (nw.isName("bridge")) {
			for (NodeWrapper c : nw.children()) {
				if (c.isName("checker_link")) {
					for (NodeWrapper cl : c.children()) {
						if (cl.isName("interface"))
							hs.checkerIface = cl.value();
						else if (cl.isName("name"))
							hs.checkerName = cl.value();
						else if (cl.isName("target_ip"))
							hs.checkerTargetIp = cl.value();
						else if (cl.isName("mode"))
							hs.checkerMode = cl.value();
						else if (cl.isName("timeout"))
							hs.checkerTimeout = cl.intValue();
					}
				} else if (c.isName("packet_relay")) {
					for (NodeWrapper pr : c.children()) {
						if (pr.isName("internal")) {
							for (NodeWrapper in : pr.children()) {
								if (in.isName("interface"))
									hs.relayInIface = in.value();
								else if (in.isName("mac"))
									hs.relayInMac = in.value();
							}
						} else if (pr.isName("external")) {
							for (NodeWrapper ex : pr.children()) {
								if (ex.isName("interface"))
									hs.relayExIface = ex.value();
								else if (ex.isName("mac"))
									hs.relayExMac = ex.value();
							}
						}
					}
				}
			}
		}

		return hs;
	}

	public Type getType() {
		return type;
	}

	public void setType(Type type) {
		this.type = type;
	}

	public Mode getMode() {
		return mode;
	}

	public void setMode(Mode mode) {
		this.mode = mode;
	}

	public List<Checker> getChecker() {
		return checker;
	}

	public void setChecker(List<Checker> checker) {
		this.checker = checker;
	}

	public boolean isUseRealIp() {
		return useRealIp;
	}

	public void setUseRealIp(boolean useRealIp) {
		this.useRealIp = useRealIp;
	}

	public String getCheckerIface() {
		return checkerIface;
	}

	public void setCheckerIface(String checkerIface) {
		this.checkerIface = checkerIface;
	}

	public String getCheckerName() {
		return checkerName;
	}

	public void setCheckerName(String checkerName) {
		this.checkerName = checkerName;
	}

	public String getCheckerTargetIp() {
		return checkerTargetIp;
	}

	public void setCheckerTargetIp(String checkerTargetIp) {
		this.checkerTargetIp = checkerTargetIp;
	}

	public String getCheckerMode() {
		return checkerMode;
	}

	public void setCheckerMode(String checkerMode) {
		this.checkerMode = checkerMode;
	}

	public Integer getCheckerTimeout() {
		return checkerTimeout;
	}

	public void setCheckerTimeout(Integer checkerTimeout) {
		this.checkerTimeout = checkerTimeout;
	}

	public String getRelayInIface() {
		return relayInIface;
	}

	public void setRelayInIface(String relayInIface) {
		this.relayInIface = relayInIface;
	}

	public String getRelayInMac() {
		return relayInMac;
	}

	public void setRelayInMac(String relayInMac) {
		this.relayInMac = relayInMac;
	}

	public String getRelayExIface() {
		return relayExIface;
	}

	public void setRelayExIface(String relayExIface) {
		this.relayExIface = relayExIface;
	}

	public String getRelayExMac() {
		return relayExMac;
	}

	public void setRelayExMac(String relayExMac) {
		this.relayExMac = relayExMac;
	}

	@Override
	protected Element convertToElement(Document doc) {
		Element e = doc.createElement(type.toString());

		if (type == Type.HeadMode)
			e.setAttribute("mode", mode.toString());
		else if (type == Type.MasterBackup) {
			for (Checker c : checker) {
				Element el = appendChild(doc, e, "checker_ha", null);
				appendChild(doc, el, "interface", c.iface);
				appendChild(doc, el, "name", c.name);
				appendChild(doc, el, "target_ip", c.targetIp);
				appendChild(doc, el, "mode", c.mode);
				appendChild(doc, el, "virtual_ip", c.virtualIp);
				appendChild(doc, el, "timeout", c.timeout);
			}
			appendChild(doc, e, "use_real_ip", null, new AttributeBuilder("chk_use", useRealIp));
		} else if (type == Type.Bridge) {
			Element cl = appendChild(doc, e, "checker_link", null);
			appendChild(doc, cl, "interface", checkerIface);
			appendChild(doc, cl, "name", checkerName);
			appendChild(doc, cl, "target_ip", checkerTargetIp);
			appendChild(doc, cl, "mode", checkerMode);
			appendChild(doc, cl, "timeout", checkerTimeout);
			Element pr = appendChild(doc, e, "packet_relay", null);
			Element prIn = appendChild(doc, pr, "internal", null);
			appendChild(doc, prIn, "interface", relayInIface);
			appendChild(doc, prIn, "mac", relayInMac);
			Element prEx = appendChild(doc, pr, "external", null);
			appendChild(doc, prEx, "interface", relayExIface);
			appendChild(doc, prEx, "mac", relayExMac);
		}

		return e;
	}

	@Override
	public Map<String, Object> marshal() {
		Map<String, Object> m = new HashMap<String, Object>();
		m.put("type", type);

		if (type == Type.HeadMode)
			m.put("mode", mode);
		else if (type == Type.MasterBackup) {
			m.put("checker", Marshaler.marshal(checker));
			m.put("use_real_ip", useRealIp);
		} else if (type == Type.Bridge) {
			m.put("checker",
					new MarshalValue("interface", checkerIface).put("name", checkerName)
							.put("target_ip", checkerTargetIp).put("timeout", checkerTimeout).get());
			m.put("packet_relay",
					new MarshalValue("internal", new MarshalValue("interface", relayInIface).put("mac", relayInMac)
							.get()).put("external",
							new MarshalValue("interface", relayExIface).put("mac", relayExMac).get()).get());
		}

		return m;
	}

	@Override
	public String toString() {
		return "HeadScript [type=" + type + ", mode=" + mode + ", checker=" + checker + ", useRealIp=" + useRealIp
				+ ", checkerIface=" + checkerIface + ", checkerName=" + checkerName + ", checkerTargetIp="
				+ checkerTargetIp + ", checkerMode=" + checkerMode + ", checkerTimeout=" + checkerTimeout
				+ ", relayInIface=" + relayInIface + ", relayInMac=" + relayInMac + ", relayExIface=" + relayExIface
				+ ", relayExMac=" + relayExMac + "]";
	}
}
