package kr.co.future.sslvpn.xtmconf.object;

import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Map;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import kr.co.future.sslvpn.xtmconf.MarshalValue;
import kr.co.future.sslvpn.xtmconf.NodeWrapper;
import kr.co.future.sslvpn.xtmconf.XtmConfig;

public class Qos extends XtmConfig {
	private int num;
	private String cid;
	private String name; // 객체명
	private String desc; // 설명
	private int qdiscid; // num + 9
	private String iface; // 인터페이스
	private double minBandwidth; // 보장 대역폭
	private double maxBandwidth; // 제한 대역폭

	@Override
	public String getXmlFilename() {
		return "object_qos.xml";
	}

	@Override
	public String getRootTagName() {
		return "object";
	}

	public static Qos parse(NodeWrapper nw) {
		if (!nw.isName("qos"))
			return null;

		Qos qos = new Qos();
		qos.num = nw.intAttr("num");
		qos.cid = nw.attr("cid");

		for (NodeWrapper c : nw.children()) {
			if (c.isName("name"))
				qos.name = c.value();
			else if (c.isName("desc"))
				qos.desc = c.value();
			else if (c.isName("qdiscid"))
				qos.qdiscid = Integer.parseInt(c.value());
			else if (c.isName("interface"))
				qos.iface = c.value();
			else if (c.isName("bandwidth")) {
				for (NodeWrapper b : c.children()) {
					if (b.isName("min"))
						qos.minBandwidth = Double.parseDouble(b.value());
					else if (b.isName("max"))
						qos.maxBandwidth = Double.parseDouble(b.value());
				}
			}
		}

		return qos;
	}

	public int getNum() {
		return num;
	}

	public void setNum(int num) {
		this.num = num;
	}

	public String getCid() {
		return cid;
	}

	public void setCid(String cid) {
		this.cid = cid;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getDesc() {
		return desc;
	}

	public void setDesc(String desc) {
		this.desc = desc;
	}

	public int getQdiscid() {
		return qdiscid;
	}

	public void setQdiscid(int qdiscid) {
		this.qdiscid = qdiscid;
	}

	public String getIface() {
		return iface;
	}

	public void setIface(String iface) {
		this.iface = iface;
	}

	public double getMinBandwidth() {
		return minBandwidth;
	}

	public void setMinBandwidth(double minBandwidth) {
		this.minBandwidth = minBandwidth;
	}

	public double getMaxBandwidth() {
		return maxBandwidth;
	}

	public void setMaxBandwidth(double maxBandwidth) {
		this.maxBandwidth = maxBandwidth;
	}

	@Override
	protected Element convertToElement(Document doc) {
		DecimalFormat dFormat = new DecimalFormat("#.#");
		Element e = doc.createElement("qos");

		e.setAttribute("num", String.valueOf(num));
		e.setAttribute("cid", cid);
		appendChild(doc, e, "name", name);
		appendChild(doc, e, "desc", desc);
		appendChild(doc, e, "qdiscid", qdiscid);
		appendChild(doc, e, "interface", iface);
		Element bandwidth = appendChild(doc, e, "bandwidth", null);
		appendChild(doc, bandwidth, "min", dFormat.format(minBandwidth));
		appendChild(doc, bandwidth, "max", dFormat.format(maxBandwidth));

		return e;
	}

	@Override
	public Map<String, Object> marshal() {
		Map<String, Object> m = new HashMap<String, Object>();
		m.put("num", num);
		m.put("cid", cid);
		m.put("name", name);
		m.put("desc", desc);
		m.put("interface", iface);
		m.put("bandwidth", new MarshalValue("min", minBandwidth).put("max", maxBandwidth).get());

		return m;
	}

	@Override
	public String toString() {
		return "Qos [num=" + num + ", cid=" + cid + ", name=" + name + ", desc=" + desc + ", qdiscid=" + qdiscid
				+ ", iface=" + iface + ", minBandwidth=" + minBandwidth + ", maxBandwidth=" + maxBandwidth + "]";
	}
}
