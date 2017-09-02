package kr.co.future.sslvpn.xtmconf.ha;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import kr.co.future.sslvpn.xtmconf.AttributeBuilder;
import kr.co.future.sslvpn.xtmconf.NodeWrapper;
import kr.co.future.sslvpn.xtmconf.Utils;
import kr.co.future.sslvpn.xtmconf.XtmConfig;

public class SyncSessionBackup extends XtmConfig {
	public static enum Act {
		ActiveActive, ActiveStandby;

		public static Act get(String str) {
			for (Act a : Act.values()) {
				if (a.toString().equals(str))
					return a;
			}
			return null;
		}

		@Override
		public String toString() {
			return name().replaceAll("[a-z]", "");
		}
	}

	private boolean use; // 세션백업 사용
	private Act mode; // 동작모드
	private ActMode selectMode; // 선택모드
	private String iface; // 인터페이스
	private String ip; // IP주소
	private String mac; // MAC주소

	@Override
	public String getXmlFilename() {
		return "sync_session_backup.xml";
	}

	@Override
	public String getRootTagName() {
		return "sync";
	}

	public static SyncSessionBackup parse(NodeWrapper nw) {
		if (!nw.isName("backup"))
			return null;

		SyncSessionBackup s = new SyncSessionBackup();
		s.use = nw.boolAttr("chk_use");

		for (NodeWrapper c : nw.children()) {
			if (c.isName("act")) {
				s.mode = Act.get(c.value());
				s.selectMode = ActMode.get(c.attr("mode"));
			} else if (c.isName("interface"))
				s.iface = c.value();
			else if (c.isName("ip"))
				s.ip = c.value();
			else if (c.isName("mac"))
				s.mac = c.value();
		}

		return s;
	}

	public boolean isUse() {
		return use;
	}

	public void setUse(boolean use) {
		this.use = use;
	}

	public Act getMode() {
		return mode;
	}

	public void setMode(Act mode) {
		this.mode = mode;
	}

	public ActMode getSelectMode() {
		return selectMode;
	}

	public void setSelectMode(ActMode selectMode) {
		this.selectMode = selectMode;
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

	public String getMac() {
		return mac;
	}

	public void setMac(String mac) {
		this.mac = mac;
	}

	@Override
	protected Element convertToElement(Document doc) {
		Element e = doc.createElement("backup");

		e.setAttribute("chk_use", Utils.bool(use));
		appendChild(doc, e, "act", mode, new AttributeBuilder("mode", selectMode));
		appendChild(doc, e, "interface", iface);
		appendChild(doc, e, "ip", ip);
		appendChild(doc, e, "mac", mac);

		if (use && mode == Act.ActiveStandby)
			XtmConfig.writeConfig(SyncSessionGuarantee.class, Arrays.asList(new SyncSessionGuarantee()));

		return e;
	}

	@Override
	public Map<String, Object> marshal() {
		Map<String, Object> m = new HashMap<String, Object>();
		m.put("use", use);
		m.put("mode", mode);
		m.put("select_mode", selectMode);
		m.put("interface", iface);
		m.put("ip", ip);
		m.put("mac", mac);

		return m;
	}

	@Override
	public String toString() {
		return "SyncSessionBackup [use=" + use + ", mode=" + mode + ", selectMode=" + selectMode + ", iface=" + iface
				+ ", ip=" + ip + ", mac=" + mac + "]";
	}
}
