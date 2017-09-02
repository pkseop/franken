package kr.co.future.sslvpn.xtmconf.waf;

import java.util.HashMap;
import java.util.Map;

import kr.co.future.msgbus.Marshalable;
import kr.co.future.sslvpn.xtmconf.NodeWrapper;
import kr.co.future.sslvpn.xtmconf.Utils;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class Pattern implements Marshalable {
	public static enum Action {
		Pass, Deny;

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

	private int id;
	private Action action;
	private int priority;
	private boolean use;
	private String value;

	public Pattern() {
	}

	public Pattern(NodeWrapper nw) {
		this.id = Integer.parseInt(nw.name().substring(3));
		this.action = Action.get(nw.attr("action"));
		this.priority = nw.intAttr("priority");
		this.use = nw.boolAttr("use");
		this.value = nw.value();
	}

	public Element toElement(Document doc) {
		Element e = doc.createElement("id_" + id);

		e.setAttribute("action", action.toString());
		e.setAttribute("priority", String.valueOf(priority));
		e.setAttribute("use", Utils.bool(use));
		e.setTextContent(value);

		return e;
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public Action getAction() {
		return action;
	}

	public void setAction(Action action) {
		this.action = action;
	}

	public int getPriority() {
		return priority;
	}

	public void setPriority(int priority) {
		this.priority = priority;
	}

	public boolean isUse() {
		return use;
	}

	public void setUse(boolean use) {
		this.use = use;
	}

	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}

	@Override
	public Map<String, Object> marshal() {
		Map<String, Object> m = new HashMap<String, Object>();
		m.put("id", id);
		m.put("action", action);
		m.put("priority", priority);
		m.put("use", use);
		m.put("value", value);

		return m;
	}

	@Override
	public String toString() {
		return "Pattern [id=" + id + ", action=" + action + ", priority=" + priority + ", use=" + use + ", value="
				+ value + "]";
	}
}
