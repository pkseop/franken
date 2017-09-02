package kr.co.future.sslvpn.xtmconf.object;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import kr.co.future.msgbus.Marshalable;
import kr.co.future.msgbus.Marshaler;
import kr.co.future.sslvpn.xtmconf.AttributeBuilder;
import kr.co.future.sslvpn.xtmconf.MarshalValue;
import kr.co.future.sslvpn.xtmconf.NodeWrapper;
import kr.co.future.sslvpn.xtmconf.Utils;
import kr.co.future.sslvpn.xtmconf.XtmConfig;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class Schedule extends XtmConfig {
	private int num;
	private String cid;
	private String name; // 객체명
	private String desc; // 설명
	private boolean chkMon; // 요일-월
	private boolean chkTue; // 요일-화
	private boolean chkWed; // 요일-수
	private boolean chkThu; // 요일-목
	private boolean chkFri; // 요일-금
	private boolean chkSat; // 요일-토
	private boolean chkSun; // 요일-일
	private List<Period> period = new ArrayList<Period>(); // 특정기간
	private List<Time> time = new ArrayList<Time>(); // 시간

	public static class Period implements Marshalable {
		private Date start; // 특정기간 시작
		private Date end; // 특정기간 종료

		public Date getStart() {
			return start;
		}

		public void setStart(Date start) {
			this.start = start;
		}

		public Date getEnd() {
			return end;
		}

		public void setEnd(Date end) {
			this.end = end;
		}

		@Override
		public Map<String, Object> marshal() {
			Map<String, Object> m = new HashMap<String, Object>();
			m.put("start", Utils.dateFormat(start));
			m.put("end", Utils.dateFormat(end));
			return m;
		}

		@Override
		public String toString() {
			return "Period [start=" + start + ", end=" + end + "]";
		}
	}

	public static class Time implements Marshalable {
		private int start; // 시작시간
		private int end; // 종료시간

		public int getStart() {
			return start;
		}

		public void setStart(int start) {
			this.start = start;
		}

		public int getEnd() {
			return end;
		}

		public void setEnd(int end) {
			this.end = end;
		}

		@Override
		public Map<String, Object> marshal() {
			Map<String, Object> m = new HashMap<String, Object>();
			m.put("start", start);
			m.put("end", end);
			return m;
		}

		@Override
		public String toString() {
			return "Time [start=" + start + ", end=" + end + "]";
		}
	}

	@Override
	public String getXmlFilename() {
		return "object_schedule.xml";
	}

	@Override
	public String getRootTagName() {
		return "object";
	}

	public static Schedule parse(NodeWrapper nw) {
		if (!nw.isName("schedule"))
			return null;

		Schedule s = new Schedule();
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy:MM:dd");
		s.num = nw.intAttr("num");
		s.cid = nw.attr("cid");

		for (NodeWrapper c : nw.children()) {
			if (c.isName("name"))
				s.name = c.value();
			else if (c.isName("desc"))
				s.desc = c.value();
			else if (c.isName("week")) {
				s.chkMon = c.boolAttr("chk_mon");
				s.chkTue = c.boolAttr("chk_tue");
				s.chkWed = c.boolAttr("chk_wed");
				s.chkThu = c.boolAttr("chk_thu");
				s.chkFri = c.boolAttr("chk_fri");
				s.chkSat = c.boolAttr("chk_sat");
				s.chkSun = c.boolAttr("chk_sun");
			} else if (c.isName("period")) {
				Period period = new Period();
				for (NodeWrapper p : c.children()) {
					try {
						if (p.isName("start"))
							period.start = dateFormat.parse(p.value());
						else if (p.isName("end"))
							period.end = dateFormat.parse(p.value());
					} catch (ParseException e) {
					}
				}
				s.period.add(period);
			} else if (c.isName("time")) {
				Time time = new Time();
				for (NodeWrapper t : c.children()) {
					if (!t.value().contains(":"))
						continue;

					int hour = Integer.parseInt(t.value().substring(0, t.value().indexOf(":")));
					int min = Integer.parseInt(t.value().substring(t.value().indexOf(":") + 1));
					if (t.isName("start"))
						time.start = hour * 60 + min;
					else if (t.isName("end"))
						time.end = hour * 60 + min;
				}
				s.time.add(time);
			}
		}

		return s;
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

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getDesc() {
		return desc;
	}

	public void setDesc(String desc) {
		this.desc = desc;
	}

	public boolean isChkMon() {
		return chkMon;
	}

	public void setChkMon(boolean chkMon) {
		this.chkMon = chkMon;
	}

	public boolean isChkTue() {
		return chkTue;
	}

	public void setChkTue(boolean chkTue) {
		this.chkTue = chkTue;
	}

	public boolean isChkWed() {
		return chkWed;
	}

	public void setChkWed(boolean chkWed) {
		this.chkWed = chkWed;
	}

	public boolean isChkThu() {
		return chkThu;
	}

	public void setChkThu(boolean chkThu) {
		this.chkThu = chkThu;
	}

	public boolean isChkFri() {
		return chkFri;
	}

	public void setChkFri(boolean chkFri) {
		this.chkFri = chkFri;
	}

	public boolean isChkSat() {
		return chkSat;
	}

	public void setChkSat(boolean chkSat) {
		this.chkSat = chkSat;
	}

	public boolean isChkSun() {
		return chkSun;
	}

	public void setChkSun(boolean chkSun) {
		this.chkSun = chkSun;
	}

	public List<Period> getPeriod() {
		return period;
	}

	public void setPeriod(List<Period> period) {
		this.period = period;
	}

	public List<Time> getTime() {
		return time;
	}

	public void setTime(List<Time> time) {
		this.time = time;
	}

	@Override
	protected Element convertToElement(Document doc) {
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy:MM:dd");
		Element e = doc.createElement("schedule");

		e.setAttribute("num", String.valueOf(num));
		e.setAttribute("cid", cid);
		appendChild(doc, e, "name", name);
		appendChild(doc, e, "desc", desc);
		AttributeBuilder weekAttr = new AttributeBuilder("chk_mon", chkMon).put("chk_tue", chkTue)
				.put("chk_wed", chkWed).put("chk_thu", chkThu).put("chk_fri", chkFri).put("chk_sat", chkSat)
				.put("chk_sun", chkSun);
		appendChild(doc, e, "week", null, weekAttr);
		for (Period p : period) {
			Element period = appendChild(doc, e, "period", null);
			appendChild(doc, period, "start", dateFormat.format(p.start));
			appendChild(doc, period, "end", dateFormat.format(p.end));
		}
		for (Time t : time) {
			Element time = appendChild(doc, e, "time", null);
			appendChild(doc, time, "start", (t.start / 60) + ":" + (t.start % 60));
			appendChild(doc, time, "end", (t.end / 60) + ":" + (t.end % 60));
		}

		return e;
	}

	@Override
	public Map<String, Object> marshal() {
		Map<String, Object> m = new HashMap<String, Object>();
		m.put("num", num);
		m.put("cid", cid);
		m.put("name", name);
		m.put("desc", desc);
		m.put("week",
				new MarshalValue("mon", chkMon).put("tue", chkTue).put("wed", chkWed).put("thu", chkThu)
						.put("fri", chkFri).put("sat", chkSat).put("sun", chkSun).get());
		m.put("period", Marshaler.marshal(period));
		m.put("time", Marshaler.marshal(time));

		return m;
	}

	@Override
	public String toString() {
		return "Schedule [num=" + num + ", cid=" + cid + ", name=" + name + ", desc=" + desc + ", chkMon=" + chkMon
				+ ", chkTue=" + chkTue + ", chkWed=" + chkWed + ", chkThu=" + chkThu + ", chkFri=" + chkFri
				+ ", chkSat=" + chkSat + ", chkSun=" + chkSun + ", period=" + period + ", time=" + time + "]";
	}
}
