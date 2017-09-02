package kr.co.future.sslvpn.xtmconf.object;

import java.util.HashMap;
import java.util.Map;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import kr.co.future.sslvpn.xtmconf.AttributeBuilder;
import kr.co.future.sslvpn.xtmconf.MarshalValue;
import kr.co.future.sslvpn.xtmconf.NodeWrapper;
import kr.co.future.sslvpn.xtmconf.XtmConfig;

public class ServicePort extends XtmConfig {
	public static enum Protocol {
		TCP, UDP, ICMP, IGMP, AH, ESP, GRE, ICMPv6, EIGRP, OSPF, RIP, ISIS, PIM, IGRP, RSVP
	}

	public static enum IcmpType {
		Any, echo_request, echo_reply, destination_unreachable, source_quench, redirect, router_advertisement, router_solicitation, time_exceeded, timestamp_request, timestamp_reply, address_mask_request, address_mask_reply;

		public static IcmpType get(String str) {
			for (IcmpType i : IcmpType.values()) {
				if (i.toString().equals(str))
					return i;
			}
			return null;
		}

		@Override
		public String toString() {
			return name().replace("_", "-");
		}
	}

	private int num;
	private String cid;
	private String name; // 객체명
	private String desc; // 설명
	private Protocol protocol; // 프로토콜
	private IcmpType icmp; // 타입
	private boolean ftp; // FTP
	private int sourceStart; // 출발지포트
	private int sourceEnd; // 출발지포트
	private int destStart; // 목적지포트
	private int destEnd; // 목적지포트

	@Override
	public String getXmlFilename() {
		return "object_service_port.xml";
	}

	@Override
	public String getRootTagName() {
		return "object";
	}

	public static ServicePort parse(NodeWrapper nw) {
		if (!nw.isName("service") || !nw.attr("type").equals("port"))
			return null;

		ServicePort sp = new ServicePort();
		sp.num = nw.intAttr("num");
		sp.cid = nw.attr("cid");

		for (NodeWrapper c : nw.children()) {
			if (c.isName("name"))
				sp.name = c.value();
			else if (c.isName("desc"))
				sp.desc = c.value();
			else if (c.isName("protocol")) {
				sp.protocol = Protocol.valueOf(c.attr("type"));
				sp.icmp = IcmpType.get(c.attr("icmp"));
				sp.ftp = c.boolAttr("ftp");
			} else if (c.isName("source")) {
				for (NodeWrapper s : c.children()) {
					if (s.isName("start"))
						sp.sourceStart = Integer.parseInt(s.value());
					else if (s.isName("end"))
						sp.sourceEnd = Integer.parseInt(s.value());
				}
			} else if (c.isName("dest")) {
				for (NodeWrapper d : c.children()) {
					if (d.isName("start"))
						sp.destStart = Integer.parseInt(d.value());
					else if (d.isName("end"))
						sp.destEnd = Integer.parseInt(d.value());
				}
			}
		}

		return sp;
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

	public IcmpType getIcmp() {
		return icmp;
	}

	public void setIcmp(IcmpType icmp) {
		this.icmp = icmp;
	}

	public boolean isFtp() {
		return ftp;
	}

	public void setFtp(boolean ftp) {
		this.ftp = ftp;
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

	@Override
	protected Element convertToElement(Document doc) {
		Element e = doc.createElement("service");

		e.setAttribute("type", "port");
		e.setAttribute("num", String.valueOf(num));
		e.setAttribute("cid", cid);
		appendChild(doc, e, "name", name);
		appendChild(doc, e, "desc", desc);
		AttributeBuilder protocolAttr = new AttributeBuilder("type", protocol).put("icmp", icmp).put("ftp", ftp);
		appendChild(doc, e, "protocol", null, protocolAttr);
		Element source = appendChild(doc, e, "source", null);
		appendChild(doc, source, "start", sourceStart);
		appendChild(doc, source, "end", sourceEnd);
		Element dest = appendChild(doc, e, "dest", null);
		appendChild(doc, dest, "start", destStart);
		appendChild(doc, dest, "end", destEnd);

		return e;
	}

	@Override
	public Map<String, Object> marshal() {
		Map<String, Object> m = new HashMap<String, Object>();
		m.put("num", num);
		m.put("cid", cid);
		m.put("name", name);
		m.put("desc", desc);
		m.put("protocol", new MarshalValue("type", protocol).put("icmp", icmp).put("ftp", ftp).get());
		m.put("source", new MarshalValue("start", sourceStart).put("end", sourceEnd).get());
		m.put("dest", new MarshalValue("start", destStart).put("end", destEnd).get());

		return m;
	}

	@Override
	public String toString() {
		return "ServicePort [num=" + num + ", cid=" + cid + ", name=" + name + ", desc=" + desc + ", protocol="
				+ protocol + ", icmp=" + icmp + ", ftp=" + ftp + ", sourceStart=" + sourceStart + ", sourceEnd="
				+ sourceEnd + ", destStart=" + destStart + ", destEnd=" + destEnd + "]";
	}
}
