package kr.co.future.sslvpn.xtmconf.ipsec;

import java.util.ArrayList;
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

public class VpnIsakmpsa extends XtmConfig {
	public static enum SettingMode {
		Booting, Traffic
	}

	public static enum IkeAuth {
		PresharedKey("psk"), Certificate("cert");

		private String name;

		private IkeAuth(String name) {
			this.name = name;
		}

		public static IkeAuth get(String str) {
			for (IkeAuth i : IkeAuth.values()) {
				if (i.toString().equals(str))
					return i;
			}
			return null;
		}

		@Override
		public String toString() {
			return name;
		}
	}

	public static enum PhaseMode {
		MainMode("Main Mode"), Aggressive("Aggressive");

		private String name;

		private PhaseMode(String name) {
			this.name = name;
		}

		public static PhaseMode get(String str) {
			for (PhaseMode p : PhaseMode.values()) {
				if (p.toString().equals(str))
					return p;
			}
			return null;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		@Override
		public String toString() {
			return name;
		}
	}

	public static enum Encryption {
		_3DES, AES128, AES256, SEED, ARIA128, ARIA192, ARIA256;

		public static Encryption get(String str) {
			for (Encryption e : Encryption.values()) {
				if (e.toString().equals(str))
					return e;
			}
			return null;
		}

		@Override
		public String toString() {
			return name().replace("_", "");
		}
	}

	public static enum PhaseAuth {
		SHA1, SHA256
	}

	public static enum XAuthType {
		Client, Server;

		public static XAuthType get(String str) {
			for (XAuthType x : XAuthType.values()) {
				if (x.toString().equals(str))
					return x;
			}
			return null;
		}

		@Override
		public String toString() {
			return name().toLowerCase();
		}
	}

	public static enum XAuthLocation {
		Local, Remote;

		public static XAuthLocation get(String str) {
			for (XAuthLocation x : XAuthLocation.values()) {
				if (x.toString().equals(str))
					return x;
			}
			return null;
		}

		@Override
		public String toString() {
			return name().toLowerCase();
		}
	}

	private int num;
	private String cid;
	private boolean nat; // NAT Traversal 사용
	private SettingMode mode; // 수행시점
	private IkeAuth auth; // IKE 인증방식
	private String name; // 객체명
	private String psk; // Preshared Key
	private String cert; // Certificate
	private Integer dpdPeriod; // DPD 간격
	private Integer dpdFail; // DPD 실패
	private PhaseMode phaseMode; // ISAKMP SA Mode
	private Encryption encryption; // 암호 알고리즘
	private PhaseAuth phaseAuth; // 해쉬 알고리즘
	private KeySwapGroup swapGroup; // 키 교환 그룹
	private int time; // Life Time
	private boolean passive; // 타사장비 연동
	private List<String> ipsecsa = new ArrayList<String>(); // IPSec SA Object
	private List<String> host = new ArrayList<String>(); // Remote IPSec HOST
	private boolean useIdChange; // ID 교환 Type 변경 사용
	private String idChangeType; // ID 교환 Type 변경
	private String idChangeData; // ID 교환 Type 변경 - ID Data
	private boolean useXAuth; // XAuth 사용
	private XAuthType xAuthType; // XAuth
	private XAuthLocation xAuthLocation; // XAuth 인증방식
	private String xAuthId; // XAuth ID
	private String xAuthPassword; // XAuth Password
	// 필요없ㅋ엉ㅋ
	private String xAuthIp;
	private String xAuthRadiusSecret;
	private Integer xAuthPort;
	private Integer xAuthAccountPort;

	@Override
	public String getXmlFilename() {
		return "vpn_isakmpsa.xml";
	}

	@Override
	public String getRootTagName() {
		return "vpn";
	}

	public static boolean hasCid(String cid) {
		List<VpnIsakmpsa> objs = XtmConfig.readConfig(VpnIsakmpsa.class);
		for (VpnIsakmpsa obj : objs) {
			if (obj.ipsecsa.contains(cid))
				return true;
		}
		return false;
	}

	public static VpnIsakmpsa parse(NodeWrapper nw) {
		if (!nw.isName("isakmpsa"))
			return null;

		VpnIsakmpsa vi = new VpnIsakmpsa();
		vi.num = nw.intAttr("num");
		vi.cid = nw.attr("cid");

		for (NodeWrapper c : nw.children()) {
			if (c.isName("setting")) {
				vi.nat = c.boolAttr("chk_nat");
				vi.mode = SettingMode.valueOf(c.attr("mode"));
				vi.auth = IkeAuth.get(c.attr("ike_auth"));
			} else if (c.isName("name"))
				vi.name = c.value();
			else if (c.isName("presharedkey"))
				vi.psk = c.value();
			else if (c.isName("certificate"))
				vi.cert = c.value();
			else if (c.isName("dpd")) {
				for (NodeWrapper d : c.children()) {
					if (d.isName("period"))
						vi.dpdPeriod = d.intValue();
					else if (d.isName("fail"))
						vi.dpdFail = d.intValue();
				}
			} else if (c.isName("phase1")) {
				vi.phaseMode = PhaseMode.get(c.attr("mode"));
				vi.encryption = Encryption.get(c.attr("encryption"));
				vi.phaseAuth = PhaseAuth.valueOf(c.attr("auth"));
				vi.swapGroup = KeySwapGroup.valueOf(c.attr("group"));
				vi.time = c.intAttr("time");
			} else if (c.isName("passive"))
				vi.passive = c.boolAttr("chk_use");
			else if (c.isName("ipsecsa")) {
				for (NodeWrapper i : c.children()) {
					if (i.isName("member"))
						vi.ipsecsa.add(i.value());
				}
			} else if (c.isName("host")) {
				for (NodeWrapper h : c.children()) {
					if (h.isName("ip"))
						vi.host.add(h.value());
				}
			} else if (c.isName("id_change")) {
				vi.useIdChange = c.boolAttr("chk_use");
				for (NodeWrapper i : c.children()) {
					if (i.isName("id")) {
						vi.idChangeType = i.attr("type");
						vi.idChangeData = i.value();
					}
				}
			} else if (c.isName("xauth")) {
				vi.useXAuth = c.boolAttr("chk_use");
				vi.xAuthType = XAuthType.get(c.attr("type"));
				vi.xAuthLocation = XAuthLocation.get(c.attr("location"));
				for (NodeWrapper x : c.children()) {
					if (x.isName("id"))
						vi.xAuthId = x.value();
					else if (x.isName("password"))
						vi.xAuthPassword = x.value();
					else if (x.isName("ip"))
						vi.xAuthIp = x.value();
					else if (x.isName("radius_secret"))
						vi.xAuthRadiusSecret = x.value();
					else if (x.isName("auth_port"))
						vi.xAuthPort = x.intValue();
					else if (x.isName("account_port"))
						vi.xAuthAccountPort = x.intValue();
				}
			}
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

	public boolean isNat() {
		return nat;
	}

	public void setNat(boolean nat) {
		this.nat = nat;
	}

	public SettingMode getMode() {
		return mode;
	}

	public void setMode(SettingMode mode) {
		this.mode = mode;
	}

	public IkeAuth getAuth() {
		return auth;
	}

	public void setAuth(IkeAuth auth) {
		this.auth = auth;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getPsk() {
		return psk;
	}

	public void setPsk(String psk) {
		this.psk = psk;
	}

	public String getCert() {
		return cert;
	}

	public void setCert(String cert) {
		this.cert = cert;
	}

	public Integer getDpdPeriod() {
		return dpdPeriod;
	}

	public void setDpdPeriod(Integer dpdPeriod) {
		this.dpdPeriod = dpdPeriod;
	}

	public Integer getDpdFail() {
		return dpdFail;
	}

	public void setDpdFail(Integer dpdFail) {
		this.dpdFail = dpdFail;
	}

	public PhaseMode getPhaseMode() {
		return phaseMode;
	}

	public void setPhaseMode(PhaseMode phaseMode) {
		this.phaseMode = phaseMode;
	}

	public Encryption getEncryption() {
		return encryption;
	}

	public void setEncryption(Encryption encryption) {
		this.encryption = encryption;
	}

	public PhaseAuth getPhaseAuth() {
		return phaseAuth;
	}

	public void setPhaseAuth(PhaseAuth phaseAuth) {
		this.phaseAuth = phaseAuth;
	}

	public KeySwapGroup getSwapGroup() {
		return swapGroup;
	}

	public void setSwapGroup(KeySwapGroup swapGroup) {
		this.swapGroup = swapGroup;
	}

	public int getTime() {
		return time;
	}

	public void setTime(int time) {
		this.time = time;
	}

	public boolean isPassive() {
		return passive;
	}

	public void setPassive(boolean passive) {
		this.passive = passive;
	}

	public List<String> getIpsecsa() {
		return ipsecsa;
	}

	public void setIpsecsa(List<String> ipsecsa) {
		this.ipsecsa = ipsecsa;
	}

	public List<String> getHost() {
		return host;
	}

	public void setHost(List<String> host) {
		this.host = host;
	}

	public boolean isUseIdChange() {
		return useIdChange;
	}

	public void setUseIdChange(boolean useIdChange) {
		this.useIdChange = useIdChange;
	}

	public String getIdChangeType() {
		return idChangeType;
	}

	public void setIdChangeType(String idChangeType) {
		this.idChangeType = idChangeType;
	}

	public String getIdChangeData() {
		return idChangeData;
	}

	public void setIdChangeData(String idChangeData) {
		this.idChangeData = idChangeData;
	}

	public boolean isUseXAuth() {
		return useXAuth;
	}

	public void setUseXAuth(boolean useXAuth) {
		this.useXAuth = useXAuth;
	}

	public XAuthType getxAuthType() {
		return xAuthType;
	}

	public void setxAuthType(XAuthType xAuthType) {
		this.xAuthType = xAuthType;
	}

	public XAuthLocation getxAuthLocation() {
		return xAuthLocation;
	}

	public void setxAuthLocation(XAuthLocation xAuthLocation) {
		this.xAuthLocation = xAuthLocation;
	}

	public String getxAuthId() {
		return xAuthId;
	}

	public void setxAuthId(String xAuthId) {
		this.xAuthId = xAuthId;
	}

	public String getxAuthPassword() {
		return xAuthPassword;
	}

	public void setxAuthPassword(String xAuthPassword) {
		this.xAuthPassword = xAuthPassword;
	}

	public String getxAuthIp() {
		return xAuthIp;
	}

	public void setxAuthIp(String xAuthIp) {
		this.xAuthIp = xAuthIp;
	}

	public String getxAuthRadiusSecret() {
		return xAuthRadiusSecret;
	}

	public void setxAuthRadiusSecret(String xAuthRadiusSecret) {
		this.xAuthRadiusSecret = xAuthRadiusSecret;
	}

	public Integer getxAuthPort() {
		return xAuthPort;
	}

	public void setxAuthPort(Integer xAuthPort) {
		this.xAuthPort = xAuthPort;
	}

	public Integer getxAuthAccountPort() {
		return xAuthAccountPort;
	}

	public void setxAuthAccountPort(Integer xAuthAccountPort) {
		this.xAuthAccountPort = xAuthAccountPort;
	}

	@Override
	protected Element convertToElement(Document doc) {
		Element e = doc.createElement("isakmpsa");

		e.setAttribute("num", String.valueOf(num));
		e.setAttribute("cid", cid);
		appendChild(doc, e, "setting", null,
				new AttributeBuilder("chk_nat", nat).put("mode", mode).put("ike_auth", auth));
		appendChild(doc, e, "name", name);
		appendChild(doc, e, "presharedkey", psk);
		appendChild(doc, e, "certificate", cert);
		Element dpd = appendChild(doc, e, "dpd", null);
		appendChild(doc, dpd, "period", dpdPeriod);
		appendChild(doc, dpd, "fail", dpdFail);
		AttributeBuilder phaseAttr = new AttributeBuilder("mode", phaseMode).put("encryption", encryption)
				.put("auth", phaseAuth).put("group", swapGroup).put("time", time);
		appendChild(doc, e, "phase1", null, phaseAttr);
		appendChild(doc, e, "passive", null, new AttributeBuilder("chk_use", passive));
		Element i = appendChild(doc, e, "ipsecsa", null, new AttributeBuilder("count", ipsecsa.size()));
		for (String s : ipsecsa)
			appendChild(doc, i, "member", s);
		Element h = appendChild(doc, e, "host", null, new AttributeBuilder("count", host.size()));
		for (String s : host)
			appendChild(doc, h, "ip", s,
					new AttributeBuilder("version", Utils.ipVersion(s)).put("type", Utils.ipType(s)));
		Element c = appendChild(doc, e, "id_change", null, new AttributeBuilder("chk_use", useIdChange));
		appendChild(doc, c, "id", idChangeData, new AttributeBuilder("type", idChangeType));
		Element x = appendChild(doc, e, "xauth", null, new AttributeBuilder("chk_use", useXAuth).put("type", xAuthType)
				.put("location", xAuthLocation));
		appendChild(doc, x, "id", xAuthId);
		appendChild(doc, x, "password", xAuthPassword);
		appendChild(doc, x, "ip", xAuthIp, new AttributeBuilder("version", Utils.ipType(xAuthIp)));
		appendChild(doc, x, "radius_secret", xAuthRadiusSecret);
		appendChild(doc, x, "auth_port", xAuthPort);
		appendChild(doc, e, "account_port", xAuthAccountPort);

		return e;
	}

	@Override
	public Map<String, Object> marshal() {
		Map<String, Object> m = new HashMap<String, Object>();
		m.put("num", num);
		m.put("cid", cid);
		m.put("nat", nat);
		m.put("mode", mode);
		m.put("auth", auth);
		m.put("name", name);
		m.put("psk", psk);
		m.put("cert", cert);
		m.put("dpd", new MarshalValue("period", dpdPeriod).put("fail", dpdFail).get());
		m.put("phase_mode", phaseMode);
		m.put("encryption", encryption);
		m.put("phase_auth", phaseAuth);
		m.put("swap_group", swapGroup);
		m.put("time", time);
		m.put("passive", passive);
		m.put("ipsecsa", ipsecsa);
		m.put("host", host);
		m.put("id_change", new MarshalValue("use", useIdChange).put("type", idChangeType).put("data", idChangeData)
				.get());
		m.put("xauth",
				new MarshalValue("use", useXAuth).put("type", xAuthType).put("location", xAuthLocation)
						.put("id", xAuthId).put("password", xAuthPassword).get());

		return m;
	}

	@Override
	public String toString() {
		return "VpnIsakmpsa [num=" + num + ", cid=" + cid + ", nat=" + nat + ", mode=" + mode + ", auth=" + auth
				+ ", name=" + name + ", psk=" + psk + ", cert=" + cert + ", dpdPeriod=" + dpdPeriod + ", dpdFail="
				+ dpdFail + ", phaseMode=" + phaseMode + ", encryption=" + encryption + ", phaseAuth=" + phaseAuth
				+ ", swapGroup=" + swapGroup + ", time=" + time + ", passive=" + passive + ", ipsecsa=" + ipsecsa
				+ ", host=" + host + ", useIdChange=" + useIdChange + ", idChangeType=" + idChangeType
				+ ", idChangeData=" + idChangeData + ", useXAuth=" + useXAuth + ", xAuthType=" + xAuthType
				+ ", xAuthLocation=" + xAuthLocation + ", xAuthId=" + xAuthId + ", xAuthPassword=" + xAuthPassword
				+ "]";
	}
}
