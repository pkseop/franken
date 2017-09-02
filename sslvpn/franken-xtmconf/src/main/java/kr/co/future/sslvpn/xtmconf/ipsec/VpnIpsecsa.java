package kr.co.future.sslvpn.xtmconf.ipsec;

import java.util.HashMap;
import java.util.Map;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import kr.co.future.sslvpn.xtmconf.AttributeBuilder;
import kr.co.future.sslvpn.xtmconf.MarshalValue;
import kr.co.future.sslvpn.xtmconf.NodeWrapper;
import kr.co.future.sslvpn.xtmconf.Utils;
import kr.co.future.sslvpn.xtmconf.XtmConfig;

public class VpnIpsecsa extends XtmConfig {
	public static enum PhaseMode {
		Tunnel, Transport;

		public static PhaseMode get(String name) {
			if (name == null)
				return null;
			else
				return valueOf(name);
		}
	}

	public static enum PhaseProtocol {
		AH, ESP;

		public static PhaseProtocol get(String name) {
			if (name == null)
				return null;
			else
				return valueOf(name);
		}
	}

	private int num;
	private String cid;
	private boolean replay; // Replay Protection
	private String name; // 객체명
	private PhaseMode phaseMode; // IPSec SA Mode
	private PhaseProtocol protocol; // 암호 프로토콜
	private String encryption; // 암호 알고리즘
	private String auth; // 인증 알고리즘
	private boolean pfs; // Perfect Forward Security
	private KeySwapGroup pfsGroup; // 키 교환 그룹
	private int time; // Life Time
	private String local; // Local ID
	private String remote; // Remote ID

	@Override
	public String getXmlFilename() {
		return "vpn_ipsecsa.xml";
	}

	@Override
	public String getRootTagName() {
		return "vpn";
	}

	public static VpnIpsecsa parse(NodeWrapper nw) {
		if (!nw.isName("ipsecsa"))
			return null;

		VpnIpsecsa vi = new VpnIpsecsa();
		vi.num = nw.intAttr("num");
		vi.cid = nw.attr("cid");

		for (NodeWrapper c : nw.children()) {
			if (c.isName("setting"))
				vi.replay = c.boolAttr("replay");
			else if (c.isName("name"))
				vi.name = c.value();
			else if (c.isName("phase2")) {
				vi.phaseMode = PhaseMode.valueOf(c.attr("mode"));
				vi.protocol = PhaseProtocol.valueOf(c.attr("protocol"));
				vi.encryption = c.attr("encryption");
				vi.auth = c.attr("auth");
				vi.pfs = c.boolAttr("chk_pfs");
				vi.pfsGroup = !c.attr("group").equals("0") ? KeySwapGroup.valueOf(c.attr("group")) : null;
				vi.time = c.intAttr("time");
			} else if (c.isName("local"))
				vi.local = c.value();
			else if (c.isName("remote"))
				vi.remote = c.value();
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

	public boolean isReplay() {
		return replay;
	}

	public void setReplay(boolean replay) {
		this.replay = replay;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public PhaseMode getPhaseMode() {
		return phaseMode;
	}

	public void setPhaseMode(PhaseMode phaseMode) {
		this.phaseMode = phaseMode;
	}

	public PhaseProtocol getProtocol() {
		return protocol;
	}

	public void setProtocol(PhaseProtocol protocol) {
		this.protocol = protocol;
	}

	public String getEncryption() {
		return encryption;
	}

	public void setEncryption(String encryption) {
		this.encryption = encryption;
	}

	public String getAuth() {
		return auth;
	}

	public void setAuth(String auth) {
		this.auth = auth;
	}

	public boolean isPfs() {
		return pfs;
	}

	public void setPfs(boolean pfs) {
		this.pfs = pfs;
	}

	public KeySwapGroup getPfsGroup() {
		return pfsGroup;
	}

	public void setPfsGroup(KeySwapGroup pfsGroup) {
		this.pfsGroup = pfsGroup;
	}

	public int getTime() {
		return time;
	}

	public void setTime(int time) {
		this.time = time;
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

	@Override
	protected Element convertToElement(Document doc) {
		Element e = doc.createElement("ipsecsa");

		e.setAttribute("num", String.valueOf(num));
		e.setAttribute("cid", cid);
		appendChild(doc, e, "setting", null, new AttributeBuilder("replay", replay));
		AttributeBuilder phaseAttr = new AttributeBuilder("mode", phaseMode).put("protocol", protocol)
				.put("encryption", (protocol == PhaseProtocol.AH) ? "3DES" : encryption).put("auth", auth)
				.put("chk_pfs", pfs).put("group", (pfsGroup != null) ? pfsGroup : "0").put("time", time);
		appendChild(doc, e, "name", name);
		appendChild(doc, e, "phase2", null, phaseAttr);
		appendChild(doc, e, "local", local,
				new AttributeBuilder("version", Utils.ipVersion(local)).put("type", Utils.ipType(local)));
		appendChild(doc, e, "remote", remote,
				new AttributeBuilder("version", Utils.ipVersion(remote)).put("type", Utils.ipType(remote)));

		return e;
	}

	@Override
	public Map<String, Object> marshal() {
		Map<String, Object> m = new HashMap<String, Object>();
		m.put("num", num);
		m.put("cid", cid);
		m.put("replay", replay);
		m.put("name", name);
		m.put("phase_mode", phaseMode);
		m.put("protocol", protocol);
		m.put("encryption", encryption);
		m.put("auth", auth);
		m.put("pfs", new MarshalValue("use", pfs).put("group", pfsGroup).get());
		m.put("time", time);
		m.put("local",
				new MarshalValue("value", local).put("version", Utils.ipVersion(local))
						.put("type", Utils.ipType(local)).get());
		m.put("remote",
				new MarshalValue("value", remote).put("version", Utils.ipVersion(remote))
						.put("type", Utils.ipType(remote)).get());

		return m;
	}

	@Override
	public String toString() {
		return "VpnIpsecsa [num=" + num + ", cid=" + cid + ", replay=" + replay + ", name=" + name + ", phaseMode="
				+ phaseMode + ", protocol=" + protocol + ", encryption=" + encryption + ", auth=" + auth + ", pfs="
				+ pfs + ", pfsGroup=" + pfsGroup + ", time=" + time + ", local=" + local + ", remote=" + remote + "]";
	}
}
