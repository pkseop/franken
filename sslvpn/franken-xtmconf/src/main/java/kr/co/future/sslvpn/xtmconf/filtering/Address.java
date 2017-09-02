package kr.co.future.sslvpn.xtmconf.filtering;

import java.util.HashMap;
import java.util.Map;

import kr.co.future.msgbus.Marshalable;
import kr.co.future.sslvpn.xtmconf.NodeWrapper;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class Address implements Marshalable {
	public static enum AddressType {
		Any("Any"), EnvVar("ENV_VAR"), User("user"), UserGroup("userG"), IPGroup("group"), IPv6Group("v6group"), IP("ip"), IPv6("ipv6");

		private String symbol;

		private AddressType(String symbol) {
			this.symbol = symbol;
		}

		public static AddressType get(String str) {
			for (AddressType a : AddressType.values()) {
				if (a.symbol.equals(str))
					return a;
			}
			return null;
		}

		@Override
		public String toString() {
			return symbol;
		}
	}

	private String cid;
	private AddressType type;
	private String name;

	public Address() {
	}

	public Address(NodeWrapper nw) {
		this.cid = nw.attr("cid");
		this.type = AddressType.get(nw.attr("otype"));
		this.name = nw.value();
	}

	public Element toElement(Document doc, String tagName) {
		Element e = doc.createElement(tagName);

		e.setAttribute("cid", (cid == null || cid.isEmpty()) ? "null" : cid);
		e.setAttribute("otype", type.toString());
		e.setTextContent(name);

		return e;
	}

	public String getCid() {
		return cid;
	}

	public void setCid(String cid) {
		this.cid = cid;
	}

	public AddressType getType() {
		return type;
	}

	public void setType(AddressType type) {
		this.type = type;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	@Override
	public Map<String, Object> marshal() {
		Map<String, Object> m = new HashMap<String, Object>();
		m.put("cid", cid);
		m.put("type", type);
		m.put("name", name);

		return m;
	}

	@Override
	public String toString() {
		return "Address [cid=" + cid + ", type=" + type + ", name=" + name + "]";
	}
}
