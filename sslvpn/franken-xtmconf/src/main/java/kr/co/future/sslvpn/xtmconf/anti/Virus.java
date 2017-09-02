package kr.co.future.sslvpn.xtmconf.anti;

import java.util.HashMap;
import java.util.Map;

import kr.co.future.msgbus.Marshalable;
import kr.co.future.sslvpn.xtmconf.AttributeBuilder;
import kr.co.future.sslvpn.xtmconf.MarshalValue;
import kr.co.future.sslvpn.xtmconf.NodeWrapper;
import kr.co.future.sslvpn.xtmconf.Utils;
import kr.co.future.sslvpn.xtmconf.XtmConfig;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class Virus extends XtmConfig {
	private boolean protocolSmtp; // Protocol-SMPT
	private boolean protocolFtp; // Protocol-FTP
	private boolean protocolHttp; // Protocol-HTTP
	private boolean protocolPop3; // Protocol-POP3
	private boolean useAlarm; // 바이러스 탐지시
	private String alarm; // 바이러스 발송 허용시 알림 제목
	private Option smtp; // 옵션-SMTP
	private Option ftp; // 옵션-FTP
	private Option http; // 옵션-HTTP

	public static class Option implements Marshalable {
		private boolean run; // 실행파일
		private boolean zip; // 압축파일
		private boolean office; // Office
		private boolean image; // 그림파일
		private boolean pdf; // PDF
		private boolean html; // HTML

		public Option() {
		}

		public Option(NodeWrapper nw) {
			this.run = nw.boolAttr("chk_run");
			this.zip = nw.boolAttr("chk_zip");
			this.office = nw.boolAttr("chk_office");
			this.image = nw.boolAttr("chk_image");
			this.pdf = nw.boolAttr("chk_pdf");
			this.html = nw.boolAttr("chk_html");
		}

		public Element toElement(Document doc, String tagName) {
			Element e = doc.createElement(tagName);

			e.setAttribute("chk_run", Utils.bool(run));
			e.setAttribute("chk_zip", Utils.bool(zip));
			e.setAttribute("chk_office", Utils.bool(office));
			e.setAttribute("chk_image", Utils.bool(image));
			e.setAttribute("chk_pdf", Utils.bool(pdf));
			e.setAttribute("chk_html", Utils.bool(html));

			return e;
		}

		public boolean isRun() {
			return run;
		}

		public void setRun(boolean run) {
			this.run = run;
		}

		public boolean isZip() {
			return zip;
		}

		public void setZip(boolean zip) {
			this.zip = zip;
		}

		public boolean isOffice() {
			return office;
		}

		public void setOffice(boolean office) {
			this.office = office;
		}

		public boolean isImage() {
			return image;
		}

		public void setImage(boolean image) {
			this.image = image;
		}

		public boolean isPdf() {
			return pdf;
		}

		public void setPdf(boolean pdf) {
			this.pdf = pdf;
		}

		public boolean isHtml() {
			return html;
		}

		public void setHtml(boolean html) {
			this.html = html;
		}

		@Override
		public Map<String, Object> marshal() {
			Map<String, Object> m = new HashMap<String, Object>();
			m.put("run", run);
			m.put("zip", zip);
			m.put("office", office);
			m.put("image", image);
			m.put("pdf", pdf);
			m.put("html", html);

			return m;
		}

		@Override
		public String toString() {
			return "Option [run=" + run + ", zip=" + zip + ", office=" + office + ", image=" + image + ", pdf=" + pdf
					+ ", html=" + html + "]";
		}
	}

	@Override
	public String getXmlFilename() {
		return "anti_virus.xml";
	}

	@Override
	public String getRootTagName() {
		return "virus";
	}

	public static Virus parse(NodeWrapper nw) {
		if (!nw.isName("setting"))
			return null;

		Virus v = new Virus();

		for (NodeWrapper c : nw.children()) {
			if (c.isName("protocol")) {
				v.protocolSmtp = c.boolAttr("chk_smtp");
				v.protocolFtp = c.boolAttr("chk_ftp");
				v.protocolHttp = c.boolAttr("chk_http");
				v.protocolPop3 = c.boolAttr("chk_pop3");
			} else if (c.isName("alarm")) {
				v.useAlarm = c.attr("action").equals("pass");
				v.alarm = c.value();
			} else if (c.isName("smtp"))
				v.smtp = new Option(c);
			else if (c.isName("ftp"))
				v.ftp = new Option(c);
			else if (c.isName("http"))
				v.http = new Option(c);
		}

		return v;
	}

	public boolean isProtocolSmtp() {
		return protocolSmtp;
	}

	public void setProtocolSmtp(boolean protocolSmtp) {
		this.protocolSmtp = protocolSmtp;
	}

	public boolean isProtocolFtp() {
		return protocolFtp;
	}

	public void setProtocolFtp(boolean protocolFtp) {
		this.protocolFtp = protocolFtp;
	}

	public boolean isProtocolHttp() {
		return protocolHttp;
	}

	public void setProtocolHttp(boolean protocolHttp) {
		this.protocolHttp = protocolHttp;
	}

	public boolean isProtocolPop3() {
		return protocolPop3;
	}

	public void setProtocolPop3(boolean protocolPop3) {
		this.protocolPop3 = protocolPop3;
	}

	public boolean isUseAlarm() {
		return useAlarm;
	}

	public void setUseAlarm(boolean useAlarm) {
		this.useAlarm = useAlarm;
	}

	public String getAlarm() {
		return alarm;
	}

	public void setAlarm(String alarm) {
		this.alarm = alarm;
	}

	public Option getSmtp() {
		return smtp;
	}

	public void setSmtp(Option smtp) {
		this.smtp = smtp;
	}

	public Option getFtp() {
		return ftp;
	}

	public void setFtp(Option ftp) {
		this.ftp = ftp;
	}

	public Option getHttp() {
		return http;
	}

	public void setHttp(Option http) {
		this.http = http;
	}

	@Override
	protected Element convertToElement(Document doc) {
		Element e = doc.createElement("setting");

		AttributeBuilder protocolAttr = new AttributeBuilder("chk_smtp", protocolSmtp).put("chk_ftp", protocolFtp)
				.put("chk_http", protocolHttp).put("chk_pop3", protocolPop3);
		appendChild(doc, e, "protocol", null, protocolAttr);
		appendChild(doc, e, "alarm", alarm, new AttributeBuilder("action", useAlarm ? "pass" : "deny"));
		e.appendChild(smtp.toElement(doc, "smtp"));
		e.appendChild(ftp.toElement(doc, "ftp"));
		e.appendChild(http.toElement(doc, "http"));

		return e;
	}

	@Override
	public Map<String, Object> marshal() {
		Map<String, Object> m = new HashMap<String, Object>();
		m.put("protocol",
				new MarshalValue("smtp", protocolSmtp).put("ftp", protocolFtp).put("http", protocolHttp)
						.put("pop3", protocolPop3).get());
		m.put("use_alarm", useAlarm);
		m.put("alarm", alarm);
		m.put("smtp", smtp.marshal());
		m.put("ftp", ftp.marshal());
		m.put("http", http.marshal());

		return m;
	}

	@Override
	public String toString() {
		return "Virus [protocolSmtp=" + protocolSmtp + ", protocolFtp=" + protocolFtp + ", protocolHttp="
				+ protocolHttp + ", protocolPop3=" + protocolPop3 + ", useAlarm=" + useAlarm + ", alarm=" + alarm
				+ ", smtp=" + smtp + ", ftp=" + ftp + ", http=" + http + "]";
	}
}
