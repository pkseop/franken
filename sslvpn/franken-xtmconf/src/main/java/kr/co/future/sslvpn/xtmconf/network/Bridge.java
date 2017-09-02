package kr.co.future.sslvpn.xtmconf.network;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import kr.co.future.sslvpn.xtmconf.NodeWrapper;
import kr.co.future.sslvpn.xtmconf.Utils;
import kr.co.future.sslvpn.xtmconf.XtmConfig;

public class Bridge extends XtmConfig {
	private int num;
	private boolean stp; // STP 사용
	private int learningTime; // Learning Time
	private String name; // 브릿지 이름
	private List<String> iface = new ArrayList<String>(); // 브릿지 멤버

	@Override
	public String getXmlFilename() {
		return "network_bridge.xml";
	}

	@Override
	public String getRootTagName() {
		return "network";
	}

	public static Bridge parse(NodeWrapper nw) {
		if (!nw.isName("bridge"))
			return null;

		Bridge b = new Bridge();
		b.num = nw.intAttr("num");
		b.stp = nw.boolAttr("chk_stp");
		b.learningTime = nw.intAttr("learning_time");

		for (NodeWrapper c : nw.children()) {
			if (c.isName("name"))
				b.name = c.value();
			else if (c.isName("interface"))
				b.iface.add(c.value());
		}

		return b;
	}

	public int getNum() {
		return num;
	}

	public void setNum(int num) {
		this.num = num;
	}

	public boolean isStp() {
		return stp;
	}

	public void setStp(boolean stp) {
		this.stp = stp;
	}

	public int getLearningTime() {
		return learningTime;
	}

	public void setLearningTime(int learningTime) {
		this.learningTime = learningTime;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public List<String> getIface() {
		return iface;
	}

	public void setIface(List<String> iface) {
		this.iface = iface;
	}

	@Override
	protected Element convertToElement(Document doc) {
		Element e = doc.createElement("bridge");

		e.setAttribute("num", String.valueOf(num));
		e.setAttribute("count", String.valueOf(iface.size()));
		e.setAttribute("chk_stp", Utils.bool(stp));
		e.setAttribute("learning_time", String.valueOf(learningTime));
		appendChild(doc, e, "name", name);
		for (String s : iface)
			appendChild(doc, e, "interface", s);

		return e;
	}

	@Override
	public Map<String, Object> marshal() {
		Map<String, Object> m = new HashMap<String, Object>();
		m.put("num", num);
		m.put("stp", stp);
		m.put("learning_time", learningTime);
		m.put("name", name);
		m.put("interface", iface);

		return m;
	}

	@Override
	public String toString() {
		return "Bridge [num=" + num + ", stp=" + stp + ", learningTime=" + learningTime + ", name=" + name + ", iface="
				+ iface + "]";
	}
}
