package kr.co.future.sslvpn.xtmconf.network;

import java.util.HashMap;
import java.util.Map;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import kr.co.future.sslvpn.xtmconf.AttributeBuilder;
import kr.co.future.sslvpn.xtmconf.NodeWrapper;
import kr.co.future.sslvpn.xtmconf.XtmConfig;

public class RouterChecker extends XtmConfig {
	public static enum Action {
		None, LineLB, Proxy, standby
	}

	private int num;
	private boolean continuous; // 결정모드-연속, 비연속
	private Action action; // 동작모드
	private String name; // 태그명
	private String iface; // 인터페이스
	private String ip; // 대상IP주소
	private int period; // 전송주기
	private Integer pool; // 비연속 범위
	private int fail; // Timeout 발생
	private String mac; // Proxy MAC

	@Override
	public String getXmlFilename() {
		return "network_router_checker.xml";
	}

	@Override
	public String getRootTagName() {
		return "routing";
	}

	public static RouterChecker parse(NodeWrapper nw) {
		if (!nw.isName("checker"))
			return null;

		RouterChecker rc = new RouterChecker();
		rc.num = nw.intAttr("num");

		for (NodeWrapper c : nw.children()) {
			if (c.isName("setting")) {
				rc.continuous = c.attr("type").equals("cont");
				rc.action = Action.valueOf(c.attr("action"));
			} else if (c.isName("name"))
				rc.name = c.value();
			else if (c.isName("interface"))
				rc.iface = c.value();
			else if (c.isName("ip"))
				rc.ip = c.value();
			else if (c.isName("period"))
				rc.period = c.intValue();
			else if (c.isName("pool"))
				rc.pool = c.intValue();
			else if (c.isName("fail"))
				rc.fail = c.intValue();
			else if (c.isName("mac"))
				rc.mac = c.value();
		}

		return rc;
	}

	public int getNum() {
		return num;
	}

	public void setNum(int num) {
		this.num = num;
	}

	public boolean isContinuous() {
		return continuous;
	}

	public void setContinuous(boolean continuous) {
		this.continuous = continuous;
	}

	public Action getAction() {
		return action;
	}

	public void setAction(Action action) {
		this.action = action;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getIface() {
		return iface;
	}

	public void setIface(String iface) {
		this.iface = iface;
	}

	public String getIp() {
		return ip;
	}

	public void setIp(String ip) {
		this.ip = ip;
	}

	public int getPeriod() {
		return period;
	}

	public void setPeriod(int period) {
		this.period = period;
	}

	public Integer getPool() {
		return pool;
	}

	public void setPool(Integer pool) {
		this.pool = pool;
	}

	public int getFail() {
		return fail;
	}

	public void setFail(int fail) {
		this.fail = fail;
	}

	public String getMac() {
		return mac;
	}

	public void setMac(String mac) {
		this.mac = mac;
	}

	@Override
	protected Element convertToElement(Document doc) {
		Element e = doc.createElement("checker");

		e.setAttribute("num", String.valueOf(num));
		appendChild(doc, e, "setting", null,
				new AttributeBuilder("type", continuous ? "cont" : "non").put("action", action));
		appendChild(doc, e, "name", name);
		appendChild(doc, e, "interface", iface);
		appendChild(doc, e, "ip", ip);
		appendChild(doc, e, "period", period);
		appendChild(doc, e, "pool", pool);
		appendChild(doc, e, "fail", fail);
		appendChild(doc, e, "mac", mac);

		return e;
	}

	@Override
	public Map<String, Object> marshal() {
		Map<String, Object> m = new HashMap<String, Object>();
		m.put("num", num);
		m.put("continuous", continuous);
		m.put("action", action);
		m.put("name", name);
		m.put("interface", iface);
		m.put("ip", ip);
		m.put("period", period);
		m.put("pool", pool);
		m.put("fail", fail);
		m.put("mac", mac);

		return m;
	}

	@Override
	public String toString() {
		return "RouterChecker [num=" + num + ", continuous=" + continuous + ", action=" + action + ", name=" + name
				+ ", iface=" + iface + ", ip=" + ip + ", period=" + period + ", pool=" + pool + ", fail=" + fail
				+ ", mac=" + mac + "]";
	}
}
