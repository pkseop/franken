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

public class IpManager extends XtmConfig {
	public static enum Type {
		Response, Manager;

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

	public static enum Action {
		Accept, Deny
	}

	private Type type;
	private int num;
	private boolean bypassMac; // 우회MAC
	private String iface; // 인터페이스
	private String ip; // IP
	private String mac; // MAC, 우회 MAC
	private Action action; // 설정

	@Override
	public String getXmlFilename() {
		return "ip_manager.xml";
	}

	@Override
	public String getRootTagName() {
		return "network";
	}

	public static IpManager parse(NodeWrapper nw) {
		if (Type.get(nw.name()) == null)
			return null;

		IpManager im = new IpManager();
		im.type = Type.get(nw.name());

		if (im.type == Type.Response) {
			for (NodeWrapper c : nw.children()) {
				if (c.isName("mac"))
					im.mac = c.value();
			}
		} else if (im.type == Type.Manager) {
			im.num = nw.intAttr("num");
			im.bypassMac = nw.boolAttr("chk_mac");

			for (NodeWrapper c : nw.children()) {
				if (c.isName("interface"))
					im.iface = c.value();
				else if (c.isName("ip"))
					im.ip = c.value();
				else if (c.isName("mac"))
					im.mac = c.value();
				else if (c.isName("action"))
					im.action = Action.valueOf(c.value());
			}
		}

		return im;
	}

	public Type getType() {
		return type;
	}

	public void setType(Type type) {
		this.type = type;
	}

	public int getNum() {
		return num;
	}

	public void setNum(int num) {
		this.num = num;
	}

	public boolean isBypassMac() {
		return bypassMac;
	}

	public void setBypassMac(boolean bypassMac) {
		this.bypassMac = bypassMac;
	}

	public String getIface() {
		return iface;
	}

	public void setIface(String iface) {
		this.iface = iface;
	}

	public String getIp() {
		return ip;
	}

	public void setIp(String ip) {
		this.ip = ip;
	}

	public String getMac() {
		return mac;
	}

	public void setMac(String mac) {
		this.mac = mac;
	}

	public Action getAction() {
		return action;
	}

	public void setAction(Action action) {
		this.action = action;
	}

	@Override
	protected Element convertToElement(Document doc) {
		Element e = doc.createElement(type.toString());

		if (type == Type.Response)
			appendChild(doc, e, "mac", mac);
		else if (type == Type.Manager) {
			e.setAttribute("num", String.valueOf(num));
			e.setAttribute("chk_mac", Utils.bool(bypassMac));
			appendChild(doc, e, "interface", iface);
			appendChild(doc, e, "ip", ip, new AttributeBuilder("type", Utils.ipType(ip)));
			appendChild(doc, e, "mac", mac);
			appendChild(doc, e, "action", action);
		}

		return e;
	}

	@Override
	public Map<String, Object> marshal() {
		Map<String, Object> m = new HashMap<String, Object>();
		m.put("type", type);
		m.put("mac", mac);

		if (type == Type.Manager) {
			m.put("num", num);
			m.put("bypass", bypassMac);
			m.put("interface", iface);
			m.put("ip", new MarshalValue("value", ip).put("type", Utils.ipType(ip)).get());
			m.put("action", action);
		}

		return m;
	}

	@Override
	public String toString() {
		return "IpManager [type=" + type + ", num=" + num + ", bypassMac=" + bypassMac + ", iface=" + iface + ", ip="
				+ ip + ", mac=" + mac + ", action=" + action + "]";
	}
}
