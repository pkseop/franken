package kr.co.future.sslvpn.xtmconf.filtering;

import java.util.HashMap;
import java.util.Map;

import kr.co.future.msgbus.Marshalable;
import kr.co.future.sslvpn.xtmconf.NodeWrapper;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class Service implements Marshalable {
	public static enum ServiceType {
		Any("Any"), PORT("port"), HTTP("http"), RPC("rpc"), ServiceGroup("group");

		private String name;

		private ServiceType(String name) {
			this.name = name;
		}

		public static ServiceType get(String str) {
			for (ServiceType s : ServiceType.values()) {
				if (s.toString().equals(str))
					return s;
			}
			return null;
		}

		@Override
		public String toString() {
			return name;
		}
	}

	private String cid;
	private ServiceType type;
	private String name;

	public Service() {
	}

	public Service(NodeWrapper nw) {
		this.cid = nw.attr("cid");
		this.type = ServiceType.get(nw.attr("otype"));
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

	public ServiceType getType() {
		return type;
	}

	public void setType(ServiceType type) {
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
		return "Service [type=" + type + ", name=" + name + "]";
	}
}
