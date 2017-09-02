package kr.co.future.sslvpn.xtmconf.network;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import kr.co.future.sslvpn.xtmconf.AttributeBuilder;
import kr.co.future.sslvpn.xtmconf.MarshalValue;
import kr.co.future.sslvpn.xtmconf.NodeWrapper;
import kr.co.future.sslvpn.xtmconf.XtmConfig;

public class Bonding extends XtmConfig {
	public static enum Mode {
		RoundRobin, ActiveBackup, BalanceXOR, Broadcast, _802_3ad, BalanceTLB, BalanceALB
	}

	private int num;
	private String name; // Bonding 이름
	private List<String> iface = new ArrayList<String>(); // Bonding 멤버
	private Mode mode; // 동작 모드
	private int monitorInterval; // 링크 모니터링주기
	private int monitorUpdelay; // 업딜레이
	private int monitorDowndelay; // 다운딜레이
	private Integer arpInterval; // ARP 주기
	private List<String> arp = new ArrayList<String>(); // ARP 대상 IP
	private Integer lacp; // LACP rate, Slow(0), Fast(1)
	private String primary; // Primary인터페이스
	private Integer hash; // 해시 모드, Layer2(0), Layer3+4(1)

	@Override
	public String getXmlFilename() {
		return "network_bonding.xml";
	}

	@Override
	public String getRootTagName() {
		return "network";
	}

	public static Bonding parse(NodeWrapper nw) {
		if (!nw.isName("bonding"))
			return null;

		Bonding b = new Bonding();
		b.num = nw.intAttr("num");

		for (NodeWrapper c : nw.children()) {
			if (c.isName("name"))
				b.name = c.value();
			else if (c.isName("interface"))
				b.iface.add(c.value());
			else if (c.isName("mode"))
				b.mode = Mode.values()[c.intValue()];
			else if (c.isName("monitor")) {
				b.monitorInterval = c.intAttr("interval");
				b.monitorUpdelay = c.intAttr("updelay");
				b.monitorDowndelay = c.intAttr("downdelay");
			} else if (c.isName("arp")) {
				b.arpInterval = c.intAttr("interval");
				for (NodeWrapper a : c.children()) {
					if (a.isName("ip"))
						b.arp.add(a.value());
				}
			} else if (c.isName("lacp"))
				b.lacp = c.intValue();
			else if (c.isName("primary"))
				b.primary = c.value();
			else if (c.isName("hash"))
				b.hash = c.intValue();
		}

		return b;
	}

	public int getNum() {
		return num;
	}

	public void setNum(int num) {
		this.num = num;
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

	public Mode getMode() {
		return mode;
	}

	public void setMode(Mode mode) {
		this.mode = mode;
	}

	public int getMonitorInterval() {
		return monitorInterval;
	}

	public void setMonitorInterval(int monitorInterval) {
		this.monitorInterval = monitorInterval;
	}

	public int getMonitorUpdelay() {
		return monitorUpdelay;
	}

	public void setMonitorUpdelay(int monitorUpdelay) {
		this.monitorUpdelay = monitorUpdelay;
	}

	public int getMonitorDowndelay() {
		return monitorDowndelay;
	}

	public void setMonitorDowndelay(int monitorDowndelay) {
		this.monitorDowndelay = monitorDowndelay;
	}

	public Integer getArpInterval() {
		return arpInterval;
	}

	public void setArpInterval(Integer arpInterval) {
		this.arpInterval = arpInterval;
	}

	public List<String> getArp() {
		return arp;
	}

	public void setArp(List<String> arp) {
		this.arp = arp;
	}

	public Integer getLacp() {
		return lacp;
	}

	public void setLacp(Integer lacp) {
		this.lacp = lacp;
	}

	public String getPrimary() {
		return primary;
	}

	public void setPrimary(String primary) {
		this.primary = primary;
	}

	public Integer getHash() {
		return hash;
	}

	public void setHash(Integer hash) {
		this.hash = hash;
	}

	@Override
	protected Element convertToElement(Document doc) {
		Element e = doc.createElement("bonding");

		e.setAttribute("num", String.valueOf(num));
		appendChild(doc, e, "name", name);
		for (String s : iface)
			appendChild(doc, e, "interface", s);
		appendChild(doc, e, "mode", mode.ordinal());
		AttributeBuilder monitorAttr = new AttributeBuilder("interval", monitorInterval).put("updelay", monitorUpdelay)
				.put("downdelay", monitorDowndelay);
		appendChild(doc, e, "monitor", null, monitorAttr);
		Element a = appendChild(doc, e, "arp", null,
				new AttributeBuilder("interval", arpInterval).put("count", arp.size()));
		for (String s : arp)
			appendChild(doc, a, "ip", s);
		appendChild(doc, e, "lacp", lacp);
		appendChild(doc, e, "primary", primary);
		appendChild(doc, e, "hash", hash);

		return e;
	}

	@Override
	public Map<String, Object> marshal() {
		Map<String, Object> m = new HashMap<String, Object>();
		m.put("num", num);
		m.put("name", name);
		m.put("interface", iface);
		m.put("mode", mode.ordinal());
		m.put("monitor",
				new MarshalValue("interval", monitorInterval).put("updelay", monitorUpdelay)
						.put("downdelay", monitorDowndelay).get());
		m.put("arp", new MarshalValue("interval", arpInterval).put("ip", arp).get());
		m.put("lacp", lacp);
		m.put("primary", primary);
		m.put("hash", hash);

		return m;
	}

	@Override
	public String toString() {
		return "Bonding [num=" + num + ", name=" + name + ", iface=" + iface + ", mode=" + mode + ", monitorInterval="
				+ monitorInterval + ", monitorUpdelay=" + monitorUpdelay + ", monitorDowndelay=" + monitorDowndelay
				+ ", arpInterval=" + arpInterval + ", arp=" + arp + ", lacp=" + lacp + ", primary=" + primary
				+ ", hash=" + hash + "]";
	}
}
