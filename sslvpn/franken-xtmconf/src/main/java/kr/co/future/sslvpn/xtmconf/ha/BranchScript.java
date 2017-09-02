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
import kr.co.future.sslvpn.xtmconf.Utils;
import kr.co.future.sslvpn.xtmconf.XtmConfig;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class BranchScript extends XtmConfig {
	public static enum Type {
		BranchMode("branch_mode"), Main("main"), Sub("sub");

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

	public static enum BranchMode {
		Main, Sub;

		public static BranchMode get(String str) {
			for (BranchMode b : BranchMode.values()) {
				if (b.toString().equals(str))
					return b;
			}
			return null;
		}

		@Override
		public String toString() {
			return name().toLowerCase();
		}
	}

	private Type type;
	private BranchMode mode; // Mode
	private String relayMac; // Packet Relay시 Sub장비 인터페이스 MAC
	// 출발지 기반 Packet Relay
	private List<PacketRelay> relaySource = new ArrayList<PacketRelay>();
	// 목적지 기반 Packet Relay
	private List<PacketRelay> relayDest = new ArrayList<BranchScript.PacketRelay>();
	private Checker checker; // Sub(Main) 장비 Checker
	// 외부망 Checker
	private List<ExternalChecker> external = new ArrayList<BranchScript.ExternalChecker>();

	public static class PacketRelay implements Marshalable {
		public static enum Action {
			Accept, Redirect;

			public static Action get(String str) {
				for (Action a : Action.values()) {
					if (a.toString().equals(str))
						return a;
				}
				return null;
			}

			@Override
			public String toString() {
				return name().toLowerCase();
			}
		}

		private Action action;
		private String ip;

		public PacketRelay() {
		}

		public PacketRelay(NodeWrapper nw) {
			for (NodeWrapper c : nw.children()) {
				if (c.isName("action"))
					this.action = Action.get(c.value());
				else if (c.isName("ip"))
					this.ip = c.value();
			}
		}

		public Element toElement(Document doc, String tagName) {
			Element e = doc.createElement(tagName);

			appendChild(doc, e, "action", action);
			appendChild(doc, e, "ip", ip,
					new AttributeBuilder("version", Utils.ipVersion(ip)).put("type", Utils.ipType(ip)));

			return e;
		}

		public Action getAction() {
			return action;
		}

		public void setAction(Action action) {
			this.action = action;
		}

		public String getIp() {
			return ip;
		}

		public void setIp(String ip) {
			this.ip = ip;
		}

		@Override
		public Map<String, Object> marshal() {
			Map<String, Object> m = new HashMap<String, Object>();
			m.put("action", action);
			m.put("ip", new MarshalValue("value", ip).put("version", Utils.ipVersion(ip)).put("type", Utils.ipType(ip))
					.get());

			return m;
		}

		@Override
		public String toString() {
			return "PacketRelay [action=" + action + ", ip=" + ip + "]";
		}
	}

	public static class Checker implements Marshalable {
		protected String iface;
		protected String name;
		protected String targetIp;
		protected Integer timeout;

		public Checker() {
		}

		public Checker(NodeWrapper nw) {
			for (NodeWrapper c : nw.children()) {
				if (c.isName("interface"))
					this.iface = c.value();
				else if (c.isName("name"))
					this.name = c.value();
				else if (c.isName("target_ip"))
					this.targetIp = c.value();
				else if (c.isName("timeout"))
					this.timeout = c.intValue();
			}
		}

		public Element toElement(Document doc, String tagName) {
			Element e = doc.createElement(tagName);

			appendChild(doc, e, "interface", iface);
			appendChild(doc, e, "name", name);
			appendChild(doc, e, "target_ip", targetIp);
			appendChild(doc, e, "timeout", timeout);

			return e;
		}

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
			m.put("timeout", timeout);

			return m;
		}

		@Override
		public String toString() {
			return "Checker [iface=" + iface + ", name=" + name + ", targetIp=" + targetIp + ", timeout=" + timeout
					+ "]";
		}
	}

	public static class ExternalChecker extends Checker {
		public static enum CheckerMode {
			None, Linelb, Standby;

			public static CheckerMode get(String str) {
				for (CheckerMode a : CheckerMode.values()) {
					if (a.toString().equals(str))
						return a;
				}
				return null;
			}

			@Override
			public String toString() {
				return name().toLowerCase();
			}
		}

		private CheckerMode mode;

		public ExternalChecker() {
		}

		public ExternalChecker(NodeWrapper nw) {
			super(nw);
			for (NodeWrapper c : nw.children()) {
				if (c.isName("mode"))
					this.mode = CheckerMode.get(c.value());
			}
		}

		public Element toElement(Document doc, String tagName) {
			Element e = doc.createElement(tagName);

			appendChild(doc, e, "interface", iface);
			appendChild(doc, e, "name", name);
			appendChild(doc, e, "target_ip", targetIp);
			appendChild(doc, e, "mode", mode);
			appendChild(doc, e, "timeout", timeout);

			return e;
		}

		public CheckerMode getMode() {
			return mode;
		}

		public void setMode(CheckerMode mode) {
			this.mode = mode;
		}

		@Override
		public Map<String, Object> marshal() {
			Map<String, Object> m = super.marshal();
			m.put("mode", mode);

			return m;
		}

		@Override
		public String toString() {
			return "ExternalChecker [mode=" + mode + ", iface=" + iface + ", name=" + name + ", targetIp=" + targetIp
					+ ", timeout=" + timeout + "]";
		}
	}

	@Override
	public String getXmlFilename() {
		return "ha_branch_script.xml";
	}

	@Override
	public String getRootTagName() {
		return "ha_branch_script";
	}

	public static BranchScript parse(NodeWrapper nw) {
		if (Type.get(nw.name()) == null)
			return null;

		BranchScript b = new BranchScript();
		b.type = Type.get(nw.name());

		if (b.type == Type.BranchMode)
			b.mode = BranchMode.get(nw.attr("mode"));
		else if (b.type == Type.Main || b.type == Type.Sub) {
			for (NodeWrapper c : nw.children()) {
				if (c.isName("packet_relay") && b.type == Type.Main) {
					for (NodeWrapper p : c.children()) {
						if (p.isName("mac"))
							b.relayMac = p.value();
						else if (p.isName("source_network"))
							b.relaySource.add(new PacketRelay(p));
						else if (p.isName("dest_network"))
							b.relayDest.add(new PacketRelay(p));
					}
				} else if ((c.isName("checker_sub") && b.type == Type.Main)
						|| (c.isName("checker_main") && b.type == Type.Sub))
					b.checker = new Checker(c);
				else if (c.isName("checker_external"))
					b.external.add(new ExternalChecker(c));
			}
		}

		return b;
	}

	public Type getType() {
		return type;
	}

	public void setType(Type type) {
		this.type = type;
	}

	public BranchMode getMode() {
		return mode;
	}

	public void setMode(BranchMode mode) {
		this.mode = mode;
	}

	public String getRelayMac() {
		return relayMac;
	}

	public void setRelayMac(String relayMac) {
		this.relayMac = relayMac;
	}

	public List<PacketRelay> getRelaySource() {
		return relaySource;
	}

	public void setRelaySource(List<PacketRelay> relaySource) {
		this.relaySource = relaySource;
	}

	public List<PacketRelay> getRelayDest() {
		return relayDest;
	}

	public void setRelayDest(List<PacketRelay> relayDest) {
		this.relayDest = relayDest;
	}

	public Checker getChecker() {
		return checker;
	}

	public void setChecker(Checker checker) {
		this.checker = checker;
	}

	public List<ExternalChecker> getExternal() {
		return external;
	}

	public void setExternal(List<ExternalChecker> external) {
		this.external = external;
	}

	@Override
	protected Element convertToElement(Document doc) {
		Element e = doc.createElement(type.toString());

		if (type == Type.BranchMode)
			e.setAttribute("mode", mode.toString());
		else if (type == Type.Main || type == Type.Sub) {
			if (type == Type.Main) {
				Element pr = appendChild(doc, e, "packet_relay", null);
				appendChild(doc, pr, "mac", relayMac);
				for (PacketRelay p : relaySource)
					pr.appendChild(p.toElement(doc, "source_network"));
				for (PacketRelay p : relayDest)
					pr.appendChild(p.toElement(doc, "dest_network"));
			}
			e.appendChild(checker.toElement(doc, (type == Type.Main) ? "checker_sub" : "checker_main"));
			for (ExternalChecker ec : external)
				e.appendChild(ec.toElement(doc, "checker_external"));
		}

		return e;
	}

	@Override
	public Map<String, Object> marshal() {
		Map<String, Object> m = new HashMap<String, Object>();
		m.put("type", type);
		if (type == Type.BranchMode)
			m.put("mode", mode);
		else if (type == Type.Main) {
			m.put("relay_mac", relayMac);
			m.put("relay_source", Marshaler.marshal(relaySource));
			m.put("relay_dest", Marshaler.marshal(relayDest));
		}

		if (type == Type.Main || type == Type.Sub) {
			m.put("checker", checker.marshal());
			m.put("external", Marshaler.marshal(external));
		}

		return m;
	}

	@Override
	public String toString() {
		return "BranchScript [mode=" + mode + ", relayMac=" + relayMac + ", relaySource=" + relaySource
				+ ", relayDest=" + relayDest + ", checker=" + checker + ", external=" + external + "]";
	}
}
