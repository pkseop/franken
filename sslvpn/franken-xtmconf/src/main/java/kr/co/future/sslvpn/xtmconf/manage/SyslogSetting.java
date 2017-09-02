package kr.co.future.sslvpn.xtmconf.manage;

import java.util.HashMap;
import java.util.Map;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import kr.co.future.sslvpn.xtmconf.NodeWrapper;
import kr.co.future.sslvpn.xtmconf.Utils;
import kr.co.future.sslvpn.xtmconf.XtmConfig;

public class SyslogSetting extends XtmConfig {
	public static enum Type {
		XTM, Standard;

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

	public static enum Format {
		Standard, WELF
	}

	private Type type;
	private boolean ipsec; // IPSec 암호화 사용
	private int num;
	private Format format; // 표준 로그포맷
	private String ip; // 로그서버 IP
	private Integer port; // 로그서버 Port

	@Override
	public String getXmlFilename() {
		return "syslog_setting.xml";
	}

	@Override
	public String getRootTagName() {
		return "manage";
	}

	public static SyslogSetting parse(NodeWrapper nw) {
		if (Type.get(nw.name()) == null)
			return null;

		SyslogSetting ss = new SyslogSetting();
		ss.type = Type.get(nw.name());

		if (ss.type == Type.XTM)
			ss.ipsec = nw.boolAttr("chk_ipsec");
		else if (ss.type == Type.Standard)
			ss.num = nw.intAttr("num");

		for (NodeWrapper c : nw.children()) {
			if (c.isName("format") && ss.type == Type.Standard)
				ss.format = Format.valueOf(c.value());
			else if (c.isName("ip"))
				ss.ip = c.value();
			else if (c.isName("port"))
				ss.port = c.intValue();
		}

		return ss;
	}

	public Type getType() {
		return type;
	}

	public void setType(Type type) {
		this.type = type;
	}

	public boolean isIpsec() {
		return ipsec;
	}

	public void setIpsec(boolean ipsec) {
		this.ipsec = ipsec;
	}

	public int getNum() {
		return num;
	}

	public void setNum(int num) {
		this.num = num;
	}

	public Format getFormat() {
		return format;
	}

	public void setFormat(Format format) {
		this.format = format;
	}

	public String getIp() {
		return ip;
	}

	public void setIp(String ip) {
		this.ip = ip;
	}

	public Integer getPort() {
		return port;
	}

	public void setPort(Integer port) {
		this.port = port;
	}

	@Override
	protected Element convertToElement(Document doc) {
		Element e = doc.createElement(type.toString());

		if (type == Type.XTM)
			e.setAttribute("chk_ipsec", Utils.bool(ipsec));
		else if (type == Type.Standard) {
			e.setAttribute("num", String.valueOf(num));
			appendChild(doc, e, "format", format);
		}

		appendChild(doc, e, "ip", ip);
		appendChild(doc, e, "port", port);

		return e;
	}

	@Override
	public Map<String, Object> marshal() {
		Map<String, Object> m = new HashMap<String, Object>();
		m.put("type", type);

		if (type == Type.XTM)
			m.put("ipsec", ipsec);
		else if (type == Type.Standard) {
			m.put("num", num);
			m.put("format", format);
		}
		m.put("ip", ip);
		m.put("port", port);

		return m;
	}

	@Override
	public String toString() {
		return "SyslogSetting [type=" + type + ", ipsec=" + ipsec + ", num=" + num + ", format=" + format + ", ip="
				+ ip + ", port=" + port + "]";
	}
}
