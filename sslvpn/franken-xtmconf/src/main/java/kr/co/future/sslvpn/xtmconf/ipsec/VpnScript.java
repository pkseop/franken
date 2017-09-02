package kr.co.future.sslvpn.xtmconf.ipsec;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import kr.co.future.sslvpn.xtmconf.AttributeBuilder;
import kr.co.future.sslvpn.xtmconf.MarshalValue;
import kr.co.future.sslvpn.xtmconf.NodeWrapper;
import kr.co.future.sslvpn.xtmconf.Utils;
import kr.co.future.sslvpn.xtmconf.XtmConfig;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class VpnScript extends XtmConfig {	
	public static enum Type {
		HeadOffice("head_office"), // 본점 관련-Head-Office 설정
		ManageForward("manage_fwd"), // 본점 관련-Forwarding 되는 XTM 관리 패킷의...
		HubAndSpoke("hub_and_spoke"), // 본점 관련-Hub&Spoke에서 Hub로 동작
		HeadOfficeBackup("head_office_backup"), // 지점 관련-DR 자동화 기능
		VpnPolicy("vpn_policy"), // 지점 관련-VPN 터널 라우팅
		MultipathType("multipath_type"), // 기타설정-F/W-라인 선택 방법
		LineTimeout("line_timeout"), // 기타설정-F/W-라인 Timeout 결정시간
		InoutInterfaceSync("inout_interface_sync"), // 기타설정-F/W-Drop해야 할 출발지 주소
		VpnStandby("vpn_standby"), // 기타-Active-Active 구성에서 Standby장비로 설정
		VpnIpsecHostIpsec("vpn_ipsec_host_ipsec"), // 기타-IPSec over IPSec 기능
		VpnForceUdpDecaps("vpn_force_udp_decaps"), // 기타-ESP 패킷을 강제로 ...
		VpnXauthPoolIp("vpn_xauth_pool_ip"), // 기타-XAUTH IP POOL 네트워크주소
		VpnXauthPoolMask("vpn_xauth_pool_mask"); // 기타-XAUTH IP POOL 넷마스크

		private String name;

		private Type(String name) {
			this.name = name;
		}

		public static Type get(String str) {
			for (Type t : Type.values()) {
				if (t.toString().equals(str))
					return t;
			}
			return null;
		}

		@Override
		public String toString() {
			return name;
		}
	}

	public static enum MultipathType {
		DestRoutingBase, SourceIpBase, DestIpBase, SourceIpDestIpBase, PerPacketBase
	}

	private Type type;
	private boolean use;
	private boolean use2;
	private Integer timeout;
	private String iface;
	private String ip;
	private String ip2;
	private String mac;
	private List<String> primaryIp = new ArrayList<String>();
	private List<String> backupIp = new ArrayList<String>();
	private MultipathType multipathType;
	private String xAuthIp;
	private String xAuthNetmask;

	public String getxAuthIp() {
		return xAuthIp;
	}

	public void setxAuthIp(String xAuthIp) {
		this.xAuthIp = xAuthIp;
	}

	public String getxAuthNetmask() {
		return xAuthNetmask;
	}

	public void setxAuthNetmask(String xAuthNetmask) {
		this.xAuthNetmask = xAuthNetmask;
	}

	@Override
	public String getXmlFilename() {
		return "vpn_script.xml";
	}

	@Override
	public String getRootTagName() {
		return "vpn_script";
	}

	public static VpnScript parse(NodeWrapper nw) {
		if (Type.get(nw.name()) == null)
			return null;

		VpnScript vs = new VpnScript();
		vs.type = Type.get(nw.name());

		if (vs.type == Type.HeadOffice) {
			vs.use = nw.boolAttr("chk_use");
			vs.timeout = nw.intValue();
		} else if (vs.type == Type.ManageForward) {
			vs.use = nw.boolAttr("chk_use");
			for (NodeWrapper c : nw.children()) {
				if (c.isName("interface"))
					vs.iface = c.value();
				else if (c.isName("ip"))
					vs.ip = c.value();
			}
		} else if (vs.type == Type.HubAndSpoke) {
			vs.use = nw.boolAttr("chk_use");
			for (NodeWrapper c : nw.children()) {
				if (c.isName("packet_relay")) {
					vs.use2 = c.boolAttr("chk_use");
					for (NodeWrapper p : c.children()) {
						if (p.isName("interface"))
							vs.iface = p.value();
						else if (p.isName("mac"))
							vs.mac = p.value();
					}
				}
			}
		} else if (vs.type == Type.HeadOfficeBackup) {
			vs.use = nw.boolAttr("chk_use");
			for (NodeWrapper c : nw.children()) {
				if (c.isName("timeout"))
					vs.timeout = c.intValue();
				else if (c.isName("primary")) {
					for (NodeWrapper p : c.children()) {
						if (p.isName("ip"))
							vs.primaryIp.add(p.value());
					}
				} else if (c.isName("backup")) {
					for (NodeWrapper b : c.children()) {
						if (b.isName("ip"))
							vs.backupIp.add(b.value());
					}
				}
			}
		} else if (vs.type == Type.VpnPolicy) {
			for (NodeWrapper c : nw.children()) {
				if (c.isName("tunnel_ip"))
					vs.ip = c.value();
				else if (c.isName("dest_ip"))
					vs.ip2 = c.value();
			}
		} else if (vs.type == Type.MultipathType) {
			if (nw.intValue() != null)
				vs.multipathType = MultipathType.values()[nw.intValue()];
		} else if (vs.type == Type.LineTimeout)
			vs.timeout = nw.intValue();
		else if (vs.type == Type.InoutInterfaceSync) {
			for (NodeWrapper c : nw.children()) {
				if (c.isName("ip"))
					vs.ip = c.value();
			}
		} else if (vs.type == Type.VpnStandby || vs.type == Type.VpnIpsecHostIpsec || vs.type == Type.VpnForceUdpDecaps)
			vs.use = nw.boolAttr("chk_use");
		else if(vs.type == Type.VpnXauthPoolIp)
			vs.xAuthIp = nw.value();
		else if(vs.type == Type.VpnXauthPoolMask)
			vs.xAuthNetmask = nw.value();
		
		return vs;
	}

	public Type getType() {
		return type;
	}

	public void setType(Type type) {
		this.type = type;
	}

	public boolean isUse() {
		return use;
	}

	public void setUse(boolean use) {
		this.use = use;
	}

	public boolean isUse2() {
		return use2;
	}

	public void setUse2(boolean use2) {
		this.use2 = use2;
	}

	public Integer getTimeout() {
		return timeout;
	}

	public void setTimeout(Integer timeout) {
		this.timeout = timeout;
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

	public String getIp2() {
		return ip2;
	}

	public void setIp2(String ip2) {
		this.ip2 = ip2;
	}

	public String getMac() {
		return mac;
	}

	public void setMac(String mac) {
		this.mac = mac;
	}

	public List<String> getPrimaryIp() {
		return primaryIp;
	}

	public void setPrimaryIp(List<String> primaryIp) {
		this.primaryIp = primaryIp;
	}

	public List<String> getBackupIp() {
		return backupIp;
	}

	public void setBackupIp(List<String> backupIp) {
		this.backupIp = backupIp;
	}

	public MultipathType getMultipathType() {
		return multipathType;
	}

	public void setMultipathType(MultipathType multipathType) {
		this.multipathType = multipathType;
	}

	@Override
	protected Element convertToElement(Document doc) {
		Element e = doc.createElement(type.toString());

		if (type == Type.HeadOffice) {
			e.setAttribute("chk_use", Utils.bool(use));
			e.setTextContent((timeout != null) ? String.valueOf(timeout) : "");
		} else if (type == Type.ManageForward) {
			e.setAttribute("chk_use", Utils.bool(use));
			appendChild(doc, e, "interface", iface);
			appendChild(doc, e, "ip", ip, new AttributeBuilder("version", Utils.ipVersion(ip)));
		} else if (type == Type.HubAndSpoke) {
			e.setAttribute("chk_use", Utils.bool(use));
			Element pr = appendChild(doc, e, "packet_relay", null, new AttributeBuilder("chk_use", use2));
			appendChild(doc, pr, "interface", iface);
			appendChild(doc, pr, "mac", mac);
		} else if (type == Type.HeadOfficeBackup) {
			e.setAttribute("chk_use", Utils.bool(use));
			appendChild(doc, e, "timeout", timeout);
			Element p = appendChild(doc, e, "primary", null);
			for (String s : primaryIp)
				appendChild(doc, p, "ip", s, new AttributeBuilder("version", Utils.ipVersion(s)).put("type", Utils.ipType(s)));
			Element b = appendChild(doc, e, "backup", null);
			for (String s : backupIp)
				appendChild(doc, b, "ip", s, new AttributeBuilder("version", Utils.ipVersion(s)).put("type", Utils.ipType(s)));
		} else if (type == Type.VpnPolicy) {
			appendChild(doc, e, "tunnel_ip", ip, new AttributeBuilder("version", Utils.ipVersion(ip)));
			appendChild(doc, e, "dest_ip", ip2,
					new AttributeBuilder("version", Utils.ipVersion(ip2)).put("type", Utils.ipType(ip2)));
		} else if (type == Type.MultipathType) {
			if (multipathType != null) {
				e.setTextContent(String.valueOf(multipathType.ordinal()));
			}
		} else if (type == Type.LineTimeout)
			e.setTextContent((timeout != null) ? String.valueOf(timeout) : "");
		else if (type == Type.InoutInterfaceSync)
			appendChild(doc, e, "ip", ip, new AttributeBuilder("version", Utils.ipVersion(ip)));
		else if (type == Type.VpnStandby || type == Type.VpnIpsecHostIpsec || type == Type.VpnForceUdpDecaps)
			e.setAttribute("chk_use", Utils.bool(use));
		else if (type == Type.VpnXauthPoolIp)
			e.setTextContent(xAuthIp);
		else if (type == Type.VpnXauthPoolMask)
			e.setTextContent(xAuthNetmask);
		return e;
	}

	@Override
	public Map<String, Object> marshal() {
		Map<String, Object> m = new HashMap<String, Object>();
		m.put("type", type);

		if (type == Type.HeadOffice) {
			m.put("use", use);
			m.put("timeout", timeout);
		} else if (type == Type.ManageForward) {
			m.put("use", use);
			m.put("interface", iface);
			m.put("ip", new MarshalValue("value", ip).put("version", Utils.ipVersion(ip)).get());
		} else if (type == Type.HubAndSpoke) {
			m.put("use", use);
			m.put("use_packet_relay", use2);
			m.put("interface", iface);
			m.put("mac", mac);
		} else if (type == Type.HeadOfficeBackup) {
			m.put("use", use);
			m.put("timeout", timeout);
			List<Object> p = new ArrayList<Object>();
			for (String s : primaryIp)
				p.add(new MarshalValue("ip", s).put("version", Utils.ipVersion(s)).get());
			m.put("primary", p);
			List<Object> b = new ArrayList<Object>();
			for (String s : backupIp)
				b.add(new MarshalValue("ip", s).put("version", Utils.ipVersion(s)).get());
			m.put("backup", b);
		} else if (type == Type.VpnPolicy) {
			m.put("tunnel_ip", new MarshalValue("ip", ip).put("version", Utils.ipVersion(ip)).get());
			m.put("dest_ip", new MarshalValue("ip", ip2).put("version", Utils.ipVersion(ip2)).get());
		} else if (type == Type.MultipathType)
			m.put("multipath_type", multipathType == null ? null : multipathType.ordinal());
		else if (type == Type.LineTimeout)
			m.put("timeout", timeout);
		else if (type == Type.InoutInterfaceSync)
			m.put("ip", new MarshalValue("value", ip).put("version", Utils.ipVersion(ip)).get());
		else if (type == Type.VpnStandby || type == Type.VpnIpsecHostIpsec || type == Type.VpnForceUdpDecaps)
			m.put("use", use);
		else if (type == Type.VpnXauthPoolIp)
			m.put("vpn_xauth_pool_ip", xAuthIp);
		else if (type == Type.VpnXauthPoolMask)
			m.put("vpn_xauth_pool_mask", xAuthNetmask);

		return m;
	}

	@Override
	public String toString() {
		return "VpnScript [type=" + type + ", use=" + use + ", use2=" + use2 + ", timeout=" + timeout + ", iface=" + iface
				+ ", ip=" + ip + ", ip2=" + ip2 + ", mac=" + mac + ", primaryIp=" + primaryIp + ", backupIp=" + backupIp
				+ ", multipathType=" + multipathType + ", xAuthIP=" + xAuthIp + ", xAuthNetmask=" + xAuthNetmask + "]";
	}
}
