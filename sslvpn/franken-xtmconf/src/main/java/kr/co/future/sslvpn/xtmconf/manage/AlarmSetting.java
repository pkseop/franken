package kr.co.future.sslvpn.xtmconf.manage;

import java.util.HashMap;
import java.util.Map;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import kr.co.future.sslvpn.xtmconf.AttributeBuilder;
import kr.co.future.sslvpn.xtmconf.NodeWrapper;
import kr.co.future.sslvpn.xtmconf.XtmConfig;

public class AlarmSetting extends XtmConfig {
	public static enum Type {
		System, DPI, DOS, DDOS;

		public static Type get(String str) {
			for (Type t : Type.values()) {
				if (t.toString().equals(str))
					return t;
			}
			return null;
		}

		@Override
		public String toString() {
			return name().toLowerCase();
		}
	}

	private Type type;
	private boolean cpu; // CPU Full
	private boolean mem; // MEM Full
	private boolean session; // Max Session
	private boolean log; // 저장된 로그 일부 삭제
	private boolean object; // 오브젝트 변경
	private boolean spd; // 정책 적용
	private boolean boot; // 시스템 부팅
	private boolean stop; // 서비스 정지
	private boolean login; // 로그인 금지
	private boolean ike; // VPN 암호화 연산 실패
	private boolean mb; // 장비 상태 변화(Master -> Backup)
	private boolean bm; // 장비 상태 변화(Backup -> Master)
	private boolean integrity; // 무결성 검사 실패
	private boolean detect; // DPI 탐지
	private boolean block; // DPI 차단
	private boolean dos; // DOS 공격
	private boolean ddos; // DDOS 공격

	@Override
	public String getXmlFilename() {
		return "alarm_setting.xml";
	}

	@Override
	public String getRootTagName() {
		return "alarm";
	}

	public static AlarmSetting parse(NodeWrapper nw) {
		if (Type.get(nw.name()) == null)
			return null;

		AlarmSetting as = new AlarmSetting();
		as.type = Type.get(nw.name());

		for (NodeWrapper c : nw.children()) {
			if (c.isName("setting") && as.type == Type.System) {
				as.cpu = c.boolAttr("chk_cpu");
				as.mem = c.boolAttr("chk_mem");
				as.session = c.boolAttr("chk_session");
				as.log = c.boolAttr("chk_log");
				as.object = c.boolAttr("chk_object");
				as.spd = c.boolAttr("chk_spd");
				as.boot = c.boolAttr("chk_boot");
				as.stop = c.boolAttr("chk_stop");
				as.login = c.boolAttr("chk_login");
				as.ike = c.boolAttr("chk_ike");
				as.mb = c.boolAttr("chk_mb");
				as.bm = c.boolAttr("chk_bm");
				as.integrity = c.boolAttr("chk_integrity");
			} else if (c.isName("pattern") && as.type == Type.DPI) {
				as.detect = c.boolAttr("chk_detect");
				as.block = c.boolAttr("chk_block");
			} else if (c.isName("flooding")) {
				if (as.type == Type.DOS)
					as.dos = c.boolAttr("chk_dos");
				else if (as.type == Type.DDOS)
					as.ddos = c.boolAttr("chk_ddos");
			}
		}

		return as;
	}

	public Type getType() {
		return type;
	}

	public void setType(Type type) {
		this.type = type;
	}

	public boolean isCpu() {
		return cpu;
	}

	public void setCpu(boolean cpu) {
		this.cpu = cpu;
	}

	public boolean isMem() {
		return mem;
	}

	public void setMem(boolean mem) {
		this.mem = mem;
	}

	public boolean isSession() {
		return session;
	}

	public void setSession(boolean session) {
		this.session = session;
	}

	public boolean isLog() {
		return log;
	}

	public void setLog(boolean log) {
		this.log = log;
	}

	public boolean isObject() {
		return object;
	}

	public void setObject(boolean object) {
		this.object = object;
	}

	public boolean isSpd() {
		return spd;
	}

	public void setSpd(boolean spd) {
		this.spd = spd;
	}

	public boolean isBoot() {
		return boot;
	}

	public void setBoot(boolean boot) {
		this.boot = boot;
	}

	public boolean isStop() {
		return stop;
	}

	public void setStop(boolean stop) {
		this.stop = stop;
	}

	public boolean isLogin() {
		return login;
	}

	public void setLogin(boolean login) {
		this.login = login;
	}

	public boolean isIke() {
		return ike;
	}

	public void setIke(boolean ike) {
		this.ike = ike;
	}

	public boolean isMb() {
		return mb;
	}

	public void setMb(boolean mb) {
		this.mb = mb;
	}

	public boolean isBm() {
		return bm;
	}

	public void setBm(boolean bm) {
		this.bm = bm;
	}

	public boolean isIntegrity() {
		return integrity;
	}

	public void setIntegrity(boolean integrity) {
		this.integrity = integrity;
	}

	public boolean isDetect() {
		return detect;
	}

	public void setDetect(boolean detect) {
		this.detect = detect;
	}

	public boolean isBlock() {
		return block;
	}

	public void setBlock(boolean block) {
		this.block = block;
	}

	public boolean isDos() {
		return dos;
	}

	public void setDos(boolean dos) {
		this.dos = dos;
	}

	public boolean isDdos() {
		return ddos;
	}

	public void setDdos(boolean ddos) {
		this.ddos = ddos;
	}

	@Override
	protected Element convertToElement(Document doc) {
		Element e = doc.createElement(type.toString());

		AttributeBuilder attr = new AttributeBuilder();
		if (type == Type.System) {
			attr.put("chk_cpu", cpu).put("chk_mem", mem).put("chk_session", session).put("chk_log", log)
					.put("chk_object", object).put("chk_spd", spd).put("chk_boot", boot).put("chk_stop", stop)
					.put("chk_login", login).put("chk_ike", ike).put("chk_mb", mb).put("chk_bm", bm)
					.put("chk_integrity", integrity);
			appendChild(doc, e, "setting", null, attr);
		} else if (type == Type.DPI)
			appendChild(doc, e, "pattern", null, attr.put("chk_detect", detect).put("chk_block", block));
		else if (type == Type.DOS)
			appendChild(doc, e, "flooding", null, attr.put("chk_dos", dos));
		else if (type == Type.DDOS)
			appendChild(doc, e, "flooding", null, attr.put("chk_ddos", ddos));

		return e;
	}

	@Override
	public Map<String, Object> marshal() {
		Map<String, Object> m = new HashMap<String, Object>();
		m.put("type", type);

		if (type == Type.System) {
			m.put("cpu", cpu);
			m.put("mem", mem);
			m.put("session", session);
			m.put("log", log);
			m.put("object", object);
			m.put("spd", spd);
			m.put("boot", boot);
			m.put("stop", stop);
			m.put("login", login);
			m.put("ike", ike);
			m.put("mb", mb);
			m.put("bm", bm);
			m.put("integrity", integrity);
		} else if (type == Type.DPI) {
			m.put("detect", detect);
			m.put("block", block);
		} else if (type == Type.DOS)
			m.put("dos", dos);
		else if (type == Type.DDOS)
			m.put("ddos", ddos);

		return m;
	}

	@Override
	public String toString() {
		return "AlarmSetting [type=" + type + ", cpu=" + cpu + ", mem=" + mem + ", session=" + session + ", log=" + log
				+ ", object=" + object + ", spd=" + spd + ", boot=" + boot + ", stop=" + stop + ", login=" + login
				+ ", ike=" + ike + ", mb=" + mb + ", bm=" + bm + ", integrity=" + integrity + ", detect=" + detect
				+ ", block=" + block + ", dos=" + dos + ", ddos=" + ddos + "]";
	}
}
