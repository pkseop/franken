package kr.co.future.sslvpn.xtmconf.network;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import kr.co.future.msgbus.Marshalable;
import kr.co.future.msgbus.Marshaler;
import kr.co.future.sslvpn.xtmconf.NodeWrapper;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class DnsDomain implements Marshalable {
	private String name;
	private List<DnsList> list = new ArrayList<DnsDomain.DnsList>();

	public static class DnsList implements Marshalable {
		public static enum Field {
			NS, HOST, MX, ALIAS
		}

		private boolean ptr;
		private Field field;
		private String host;
		private String ip;
		private int ttl;

		public DnsList() {
		}

		public DnsList(NodeWrapper nw) {
			this.ptr = nw.boolAttr("chk_ptr");

			for (NodeWrapper c : nw.children()) {
				if (c.isName("host")) {
					this.field = Field.valueOf(c.attr("field"));
					this.host = c.value();
				} else if (c.isName("ip"))
					this.ip = c.value();
				else if (c.isName("ttl"))
					this.ttl = c.intValue();
			}
		}

		public boolean isPtr() {
			return ptr;
		}

		public void setPtr(boolean ptr) {
			this.ptr = ptr;
		}

		public Field getField() {
			return field;
		}

		public void setField(Field field) {
			this.field = field;
		}

		public String getHost() {
			return host;
		}

		public void setHost(String host) {
			this.host = host;
		}

		public String getIp() {
			return ip;
		}

		public void setIp(String ip) {
			this.ip = ip;
		}

		public int getTtl() {
			return ttl;
		}

		public void setTtl(int ttl) {
			this.ttl = ttl;
		}

		public Element toElement(Document doc) {
			Element e = doc.createElement("list");
			e.setAttribute("chk_ptr", ptr ? "on" : "off");

			Element h = doc.createElement("host");
			h.setAttribute("field", field.toString());
			h.setTextContent(host);
			Element i = doc.createElement("ip");
			i.setTextContent(ip);
			Element t = doc.createElement("ttl");
			t.setTextContent(String.valueOf(ttl));

			e.appendChild(h);
			e.appendChild(i);
			e.appendChild(t);

			return e;
		}

		@Override
		public Map<String, Object> marshal() {
			Map<String, Object> m = new HashMap<String, Object>();
			m.put("ptr", ptr);
			m.put("field", field);
			m.put("host", host);
			m.put("ip", ip);
			m.put("ttl", ttl);

			return m;
		}

		@Override
		public String toString() {
			return "DnsList [ptr=" + ptr + ", field=" + field + ", host=" + host + ", ip=" + ip + ", ttl=" + ttl + "]";
		}
	}

	public DnsDomain() {
	}

	public DnsDomain(NodeWrapper nw) {
		for (NodeWrapper c : nw.children()) {
			if (c.isName("name"))
				this.name = c.value();
			else if (c.isName("list"))
				this.list.add(new DnsList(c));
		}
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public List<DnsList> getList() {
		return list;
	}

	public void setList(List<DnsList> list) {
		this.list = list;
	}

	public Element toElement(Document doc) {
		Element e = doc.createElement("domain");
		e.setAttribute("count", String.valueOf(list.size()));

		Element n = doc.createElement("name");
		n.setTextContent(name);
		e.appendChild(n);

		for (DnsList dl : list)
			e.appendChild(dl.toElement(doc));

		return e;
	}

	@Override
	public Map<String, Object> marshal() {
		Map<String, Object> m = new HashMap<String, Object>();
		m.put("name", name);
		m.put("dns", Marshaler.marshal(list));

		return m;
	}

	@Override
	public String toString() {
		return "DnsDomain [name=" + name + ", list=" + list + "]";
	}
}
