package kr.co.future.sslvpn.xtmconf.system;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import kr.co.future.sslvpn.xtmconf.NodeWrapper;
import kr.co.future.sslvpn.xtmconf.Utils;
import kr.co.future.sslvpn.xtmconf.XtmConfig;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class Reservation extends XtmConfig {
	private boolean use; // 예약전송
	private Date date; // 날짜입력
	private Integer hour; // 시간
	private Integer minute; // 분

	@Override
	public String getXmlFilename() {
		return "system_reservation.xml";
	}

	@Override
	public String getRootTagName() {
		return "system";
	}

	public static Reservation parse(NodeWrapper nw) {
		if (!nw.isName("reservation"))
			return null;

		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd");
		Reservation r = new Reservation();
		r.use = nw.attr("chk_use").equals("on");

		for (NodeWrapper c : nw.children()) {
			if (c.isName("date")) {
				try {
					r.date = dateFormat.parse(c.value());
				} catch (ParseException e) {
				}
			} else if (c.isName("hour"))
				r.hour = c.intValue();
			else if (c.isName("minute"))
				r.minute = c.intValue();
		}

		return r;
	}

	public boolean isUse() {
		return use;
	}

	public void setUse(boolean use) {
		this.use = use;
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
		Element e = doc.createElement("reservation");

		e.setAttribute("chk_use", Utils.bool(use));
		appendChild(doc, e, "date", (date != null) ? dateFormat.format(date) : null);
		appendChild(doc, e, "hour", hour);
		appendChild(doc, e, "minute", minute);

		return e;
	}

	@Override
	public Map<String, Object> marshal() {
		Map<String, Object> m = new HashMap<String, Object>();
		m.put("use", use);
		m.put("date", Utils.dateFormat(date));
		m.put("hour", hour);
		m.put("minute", minute);

		return m;
	}

	@Override
	public String toString() {
		return "Reservation [use=" + use + ", date=" + date + ", hour=" + hour + ", minute=" + minute + "]";
	}
}
