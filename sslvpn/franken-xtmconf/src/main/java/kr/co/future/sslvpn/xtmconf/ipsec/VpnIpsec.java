package kr.co.future.sslvpn.xtmconf.ipsec;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import kr.co.future.sslvpn.xtmconf.AttributeBuilder;
import kr.co.future.sslvpn.xtmconf.MarshalValue;
import kr.co.future.sslvpn.xtmconf.NodeWrapper;
import kr.co.future.sslvpn.xtmconf.Utils;
import kr.co.future.sslvpn.xtmconf.XtmConfig;

public class VpnIpsec extends XtmConfig {
	private int num;
	private String cid;
	private boolean use;
	private String id; // ID/CID
	private String ip; // IP주소
	private String isakmpsaCid; // ISAKMP SA.cid
	private String isakmpsaName; // ISAKMP SA
	private String iface; // 인터페이스
	private int group; // Group ID

	@Override
	public String getXmlFilename() {
		return "vpn_ipsec.xml";
	}

	@Override
	public String getRootTagName() {
		return "vpn";
	}

	public static boolean hasCid(String cid) {
		List<VpnIpsec> objs = XtmConfig.readConfig(VpnIpsec.class);
		for (VpnIpsec obj : objs) {
			if (cid.equals(obj.isakmpsaCid))
				return true;
		}
		return false;
	}

	public static void updateCid(String cid, String newName) {
		List<VpnIpsec> objs = XtmConfig.readConfig(VpnIpsec.class);
		boolean update = false;
		for (VpnIpsec obj : objs) {
			if (cid.equals(obj.isakmpsaCid)) {
				obj.isakmpsaName = newName;
				update = true;
			}
		}

		if (update)
			XtmConfig.writeConfig(VpnIpsec.class, objs);
	}

	public static VpnIpsec parse(NodeWrapper nw) {
		if (!nw.isName("ipsec"))
			return null;

		VpnIpsec vi = new VpnIpsec();
		vi.num = nw.intAttr("num");
		vi.cid = nw.attr("cid");
		vi.use = nw.boolAttr("use");

		for (NodeWrapper c : nw.children()) {
			if (c.isName("id"))
				vi.id = c.value();
			else if (c.isName("ip"))
				vi.ip = c.value();
			else if (c.isName("isakmpsa")) {
				vi.isakmpsaCid = c.attr("cid");
				vi.isakmpsaName = c.value();
			} else if (c.isName("interface"))
				vi.iface = c.value();
			else if (c.isName("group"))
				vi.group = c.intValue();
		}

		return vi;
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

	public boolean isUse() {
		return use;
	}

	public void setUse(boolean use) {
		this.use = use;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getIp() {
		return ip;
	}

	public void setIp(String ip) {
		this.ip = ip;
	}

	public String getIsakmpsaCid() {
		return isakmpsaCid;
	}

	public void setIsakmpsaCid(String isakmpsaCid) {
		this.isakmpsaCid = isakmpsaCid;
	}

	public String getIsakmpsaName() {
		return isakmpsaName;
	}

	public void setIsakmpsaName(String isakmpsaName) {
		this.isakmpsaName = isakmpsaName;
	}

	public String getIface() {
		return iface;
	}

	public void setIface(String iface) {
		this.iface = iface;
	}

	public int getGroup() {
		return group;
	}

	public void setGroup(int group) {
		this.group = group;
	}

	@Override
	protected Element convertToElement(Document doc) {
		Element e = doc.createElement("ipsec");

		e.setAttribute("num", String.valueOf(num));
		e.setAttribute("cid", cid);
		e.setAttribute("use", Utils.bool(use));
		appendChild(doc, e, "id", id);
		appendChild(doc, e, "ip", ip, new AttributeBuilder("version", Utils.ipVersion(ip)));
		appendChild(doc, e, "isakmpsa", isakmpsaName, new AttributeBuilder("cid", isakmpsaCid));
		appendChild(doc, e, "interface", iface);
		appendChild(doc, e, "group", group);

		return e;
	}

	@Override
	public Map<String, Object> marshal() {
		Map<String, Object> m = new HashMap<String, Object>();
		m.put("num", num);
		m.put("cid", cid);
		m.put("use", use);
		m.put("id", id);
		m.put("ip", new MarshalValue("value", ip).put("version", Utils.ipVersion(ip)).get());
		m.put("isakmpsa", new MarshalValue("cid", isakmpsaCid).put("name", isakmpsaName).get());
		m.put("interface", iface);
		m.put("group", group);

		return m;
	}

	@Override
	public String toString() {
		return "VpnIpsec [num=" + num + ", cid=" + cid + ", use=" + use + ", id=" + id + ", ip=" + ip
				+ ", isakmpsaCid=" + isakmpsaCid + ", isakmpsaName=" + isakmpsaName + ", iface=" + iface + ", group="
				+ group + "]";
	}
}
