package kr.co.future.sslvpn.xtmconf.object;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import kr.co.future.sslvpn.xtmconf.MarshalValue;
import kr.co.future.sslvpn.xtmconf.NodeWrapper;
import kr.co.future.sslvpn.xtmconf.XtmConfig;

public class ServiceRpc extends XtmConfig {
	public static enum Protocol {
		TCP
	}

	public static enum Rpc {
		portmapper(100000), rstatd(100001), rusersd(100002), nfs(100003), ypserv(100004), mountd(100005), ypbind(100007), walld(
				100008), yppasswdd(100009), etherstatd(100010), rquotad(100011), sprayd(100012), _3270_mapper(100013), rje_mapper(
				100014), selection_svc(100015), database_svc(100016), rexd(100017), alis(100018), sched(100019), llockmgr(
				100020), nlockmgr(100021), x25inr(100022), statmon(100023), status(100024), bootparam(100026), ypupdated(
				100028), keyserv(100029), sunlink_mapper(100033), tfsd(100037), nsed(100038), nsemntd(100039), showfhd(
				100043);

		private int code;

		private Rpc(int code) {
			this.code = code;
		}

		public int getCode() {
			return code;
		}
	}

	private int num;
	private String cid;
	private String name; // 객체명
	private String desc; // 설명
	private Protocol protocol; // 프로토콜
	private int sourceStart; // 출발지포트
	private int sourceEnd; // 출발지포트
	private int destStart; // 목적지포트
	private int destEnd; // 목적지포트
	private List<Integer> rpc = new ArrayList<Integer>(); // RPC NUMBER

	@Override
	public String getXmlFilename() {
		return "object_service_rpc.xml";
	}

	@Override
	public String getRootTagName() {
		return "object";
	}

	public static ServiceRpc parse(NodeWrapper nw) {
		if (!nw.isName("service") || !nw.attr("type").equals("rpc"))
			return null;

		ServiceRpc sr = new ServiceRpc();
		sr.num = nw.intAttr("num");
		sr.cid = nw.attr("cid");

		for (NodeWrapper c : nw.children()) {
			if (c.isName("name"))
				sr.name = c.value();
			else if (c.isName("protocol"))
				sr.protocol = Protocol.valueOf(c.value());
			else if (c.isName("source")) {
				for (NodeWrapper s : c.children()) {
					if (s.isName("start"))
						sr.sourceStart = s.intValue();
					else if (s.isName("end"))
						sr.sourceEnd = s.intValue();
				}
			} else if (c.isName("dest")) {
				for (NodeWrapper d : c.children()) {
					if (d.isName("start"))
						sr.destStart = d.intValue();
					else if (d.isName("end"))
						sr.destEnd = d.intValue();
				}
			} else if (c.isName("rpc"))
				sr.rpc.add(c.intValue());
		}

		return sr;
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

	public Protocol getProtocol() {
		return protocol;
	}

	public void setProtocol(Protocol protocol) {
		this.protocol = protocol;
	}

	public int getSourceStart() {
		return sourceStart;
	}

	public void setSourceStart(int sourceStart) {
		this.sourceStart = sourceStart;
	}

	public int getSourceEnd() {
		return sourceEnd;
	}

	public void setSourceEnd(int sourceEnd) {
		this.sourceEnd = sourceEnd;
	}

	public int getDestStart() {
		return destStart;
	}

	public void setDestStart(int destStart) {
		this.destStart = destStart;
	}

	public int getDestEnd() {
		return destEnd;
	}

	public void setDestEnd(int destEnd) {
		this.destEnd = destEnd;
	}

	public List<Integer> getRpc() {
		return rpc;
	}

	public void setRpc(List<Integer> rpc) {
		this.rpc = rpc;
	}

	@Override
	protected Element convertToElement(Document doc) {
		Element e = doc.createElement("service");

		e.setAttribute("type", "rpc");
		e.setAttribute("num", String.valueOf(num));
		e.setAttribute("cid", cid);
		e.setAttribute("count", String.valueOf(rpc.size()));
		appendChild(doc, e, "name", name);
		appendChild(doc, e, "desc", desc);
		appendChild(doc, e, "protocol", protocol);
		Element source = appendChild(doc, e, "source", null);
		appendChild(doc, source, "start", sourceStart);
		appendChild(doc, source, "end", sourceEnd);
		Element dest = appendChild(doc, e, "dest", null);
		appendChild(doc, dest, "start", destStart);
		appendChild(doc, dest, "end", destEnd);

		for (Integer r : rpc)
			appendChild(doc, e, "rpc", r);

		return e;
	}

	@Override
	public Map<String, Object> marshal() {
		Map<String, Object> m = new HashMap<String, Object>();
		m.put("num", num);
		m.put("cid", cid);
		m.put("name", name);
		m.put("desc", desc);
		m.put("protocol", protocol);
		m.put("source", new MarshalValue("start", sourceStart).put("end", sourceEnd).get());
		m.put("dest", new MarshalValue("start", destStart).put("end", destEnd).get());
		m.put("rpc", rpc);

		return m;
	}

	@Override
	public String toString() {
		return "ServiceRpc [num=" + num + ", cid=" + cid + ", name=" + name + ", desc=" + desc + ", protocol="
				+ protocol + ", sourceStart=" + sourceStart + ", sourceEnd=" + sourceEnd + ", destStart=" + destStart
				+ ", destEnd=" + destEnd + ", rpc=" + rpc + "]";
	}
}
