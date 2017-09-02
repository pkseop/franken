package kr.co.future.sslvpn.xtmconf.network;

import java.util.HashMap;
import java.util.Map;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import kr.co.future.sslvpn.xtmconf.NodeWrapper;
import kr.co.future.sslvpn.xtmconf.XtmConfig;

public class SixInFour extends XtmConfig {
	private int num;
	private String cid;
	private String local; // Local IPv4
	private String remote; // Remote IPv4
	private int ttl; // TTL
	private String index; // 장치명 Index

	@Override
	public String getXmlFilename() {
		return "network_tunneling_6in4.xml";
	}

	@Override
	public String getRootTagName() {
		return "network";
	}

	public static SixInFour parse(NodeWrapper nw) {
		if (!nw.isName("tunneling"))
			return null;

		SixInFour sif = new SixInFour();
		sif.num = nw.intAttr("num");
		sif.cid = nw.attr("cid");

		for (NodeWrapper c : nw.children()) {
			if (c.isName("local"))
				sif.local = c.value();
			else if (c.isName("remote"))
				sif.remote = c.value();
			else if (c.isName("ttl"))
				sif.ttl = c.intValue();
			else if (c.isName("index"))
				sif.index = c.value();
		}

		return sif;
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

	public String getLocal() {
		return local;
	}

	public void setLocal(String local) {
		this.local = local;
	}

	public String getRemote() {
		return remote;
	}

	public void setRemote(String remote) {
		this.remote = remote;
	}

	public int getTtl() {
		return ttl;
	}

	public void setTtl(int ttl) {
		this.ttl = ttl;
	}

	public String getIndex() {
		return index;
	}

	public void setIndex(String index) {
		this.index = index;
	}

	@Override
	protected Element convertToElement(Document doc) {
		Element e = doc.createElement("tunneling");

		e.setAttribute("num", String.valueOf(num));
		e.setAttribute("cid", cid);
		appendChild(doc, e, "local", local);
		appendChild(doc, e, "remote", remote);
		appendChild(doc, e, "ttl", ttl);
		appendChild(doc, e, "index", index);

		return e;
	}

	@Override
	public Map<String, Object> marshal() {
		Map<String, Object> m = new HashMap<String, Object>();
		m.put("num", num);
		m.put("cid", cid);
		m.put("local", local);
		m.put("remote", remote);
		m.put("ttl", ttl);
		m.put("index", index);

		return m;
	}

	@Override
	public String toString() {
		return "SixInFour [num=" + num + ", cid=" + cid + ", local=" + local + ", remote=" + remote + ", ttl=" + ttl
				+ ", index=" + index + "]";
	}
}
