package kr.co.future.sslvpn.xtmconf.system;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import kr.co.future.sslvpn.xtmconf.AttributeBuilder;
import kr.co.future.sslvpn.xtmconf.MarshalValue;
import kr.co.future.sslvpn.xtmconf.NodeWrapper;
import kr.co.future.sslvpn.xtmconf.Utils;
import kr.co.future.sslvpn.xtmconf.XtmConfig;

public class Status extends XtmConfig {
	public static enum Type {
		Integrity, // 무결성 검사
		/*Backup, // 백업*/
		Reboot; // 서비스-시스템 재시작

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
	private boolean use; // 실행주기 체크박스
	private Periodic periodic; // 실행주기 (1)
	private Integer subtype; // 실행주기 (2)
	private String password; // 비밀번호
	private Date date; // 서비스-날짜입력
	private Integer hour; // 서비스-시간
	private Integer minute; // 서비스-분

	@Override
	public String getXmlFilename() {
		return "system_status.xml";
	}

	@Override
	public String getRootTagName() {
		return "status";
	}

	public static Status parse(NodeWrapper nw) {
		if (!nw.isName("integrity") && !nw.isName("backup") && !nw.isName("reboot"))
			return null;

		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd");
		Status s = new Status();
		s.type = Type.get(nw.name());
		s.use = nw.boolAttr("chk_use");

		for (NodeWrapper c : nw.children()) {
			if (c.isName("periodic") && (s.type == Type.Integrity /*|| s.type == Type.Backup*/)) {
				s.periodic = Periodic.get(c.attr("type"));
				s.subtype = c.intAttr("subtype");
				s.password = c.value();
			} else if (c.isName("date") && s.type == Type.Reboot) {
				try {
					s.date = dateFormat.parse(c.value());
				} catch (ParseException e) {
				}
			} else if (c.isName("hour") && s.type == Type.Reboot)
				s.hour = c.intValue();
			else if (c.isName("minute") && s.type == Type.Reboot)
				s.minute = c.intValue();
		}

		return s;
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

	public Periodic getPeriodic() {
		return periodic;
	}

	public void setPeriodic(Periodic periodic) {
		this.periodic = periodic;
	}

	public Integer getSubtype() {
		return subtype;
	}

	public void setSubtype(Integer subtype) {
		this.subtype = subtype;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public Date getDate() {
		return date;
	}

	public void setDate(Date date) {
		this.date = date;
	}

	public Integer getHour() {
		return hour;
	}

	public void setHour(Integer hour) {
		this.hour = hour;
	}

	public Integer getMinute() {
		return minute;
	}

	public void setMinute(Integer minute) {
		this.minute = minute;
	}

	@Override
	protected Element convertToElement(Document doc) {
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd");
		Element e = doc.createElement(type.toString());

		e.setAttribute("chk_use", Utils.bool(use));
		if (type == Type.Integrity /*|| type == Type.Backup*/) {
			appendChild(doc, e, "periodic", password,
					new AttributeBuilder("type", periodic).put("subtype", (subtype != null) ? subtype : "null"));
		} else if (type == Type.Reboot) {
			appendChild(doc, e, "date", (date != null) ? dateFormat.format(date) : "");
			appendChild(doc, e, "hour", hour);
			appendChild(doc, e, "minute", minute);
		}

		return e;
	}

	@Override
	public Map<String, Object> marshal() {
		Map<String, Object> m = new HashMap<String, Object>();
		m.put("type", type);
		m.put("use", use);

		if (type == Type.Integrity) {
			m.put("periodic", new MarshalValue("type", periodic).put("subtype", subtype).get());
		}
		/*
		else if (type == Type.Backup) {
			m.put("periodic", new MarshalValue("type", periodic).put("subtype", subtype).get());
			m.put("password", password);
		}
		*/
		else if (type == Type.Reboot) {
			m.put("date", Utils.dateFormat(date));
			m.put("hour", hour);
			m.put("minute", minute);
		}

		return m;
	}

	@Override
	public String toString() {
		return "Status [type=" + type + ", use=" + use + ", periodic=" + periodic + ", subtype=" + subtype
				+ ", password=" + password + ", date=" + date + ", hour=" + hour + ", minute=" + minute + "]";
	}
}
