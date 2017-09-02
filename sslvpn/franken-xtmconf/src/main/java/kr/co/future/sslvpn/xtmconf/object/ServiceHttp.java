package kr.co.future.sslvpn.xtmconf.object;

import java.util.HashMap;
import java.util.Map;

import kr.co.future.msgbus.Marshalable;
import kr.co.future.sslvpn.xtmconf.AttributeBuilder;
import kr.co.future.sslvpn.xtmconf.MarshalValue;
import kr.co.future.sslvpn.xtmconf.NodeWrapper;
import kr.co.future.sslvpn.xtmconf.XtmConfig;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class ServiceHttp extends XtmConfig {
	public static enum Protocol {
		TCP
	}

	private int num;
	private String cid;
	private String name; // 객체명
	private String desc; // 설명
	private Protocol protocol; // 프로토콜
	private int sourceStart; // 출발지포트
	private int sourceEnd; // 출발지포트
	private int destStart; // 목적지포트
	private int destEnd; // 목적지포트
	private String content; // Content Filter URL
	private String contentRedirection; // Content Filter Redirection
	private Url game; // URL 차단그룹 게임
	private Url stock; // URL 차단그룹 증권
	private Url news; // URL 차단그룹 인터넷신문
	private Url itv; // URL 차단그룹 인터넷방송
	private Url email; // URL 차단그룹 이메일
	private Url webhard; // URL 차단그룹 웹하드
	private Url p2p; // URL 차단그룹 P2P
	private Url user; // URL 차단그룹 사용자정의
	private boolean siteKiscom; // 유해사이트 데이터베이스 사용
	private boolean siteSafenet; // 인터넷 자율등급 사용
	private boolean siteYouth; // 청소년 유해사이트 차단
	private String siteRedirection; // 유해사이트 차단 Redirection
	private int optionLanguage; // 언어
	private int optionNude; // 노출
	private int optionSex; // 성행위
	private int optionViolence; // 폭력
	private boolean optionEtc1; // 음주조장, 흡연조장
	private boolean optionEtc2; // 마약사용조장, 무기사용조장, 도박

	public static class Url implements Marshalable {
		private boolean use;
		private String redirect;
		
		public Url(boolean use, String redirect) {
			this.use = use;
			this.redirect = redirect;
		}

		public boolean isUse() {
			return use;
		}

		public void setUse(boolean use) {
			this.use = use;
		}

		public String getRedirect() {
			return redirect;
		}

		public void setRedirect(String redirect) {
			this.redirect = redirect;
		}

		@Override
		public Map<String, Object> marshal() {
			Map<String, Object> m = new HashMap<String, Object>();
			m.put("use", use);
			m.put("redirect", redirect);

			return m;
		}

		@Override
		public String toString() {
			return "Url [use=" + use + ", redirect=" + redirect + "]";
		}
	}

	@Override
	public String getXmlFilename() {
		return "object_service_http.xml";
	}

	@Override
	public String getRootTagName() {
		return "object";
	}

	public static ServiceHttp parse(NodeWrapper nw) {
		if (!nw.isName("service") || !nw.attr("type").equals("http"))
			return null;

		ServiceHttp sh = new ServiceHttp();
		sh.num = nw.intAttr("num");
		sh.cid = nw.attr("cid");

		for (NodeWrapper c : nw.children()) {
			if (c.isName("name"))
				sh.name = c.value();
			else if (c.isName("desc"))
				sh.desc = c.value();
			else if (c.isName("protocol"))
				sh.protocol = Protocol.valueOf(c.value());
			else if (c.isName("source")) {
				for (NodeWrapper s : c.children()) {
					if (s.isName("start"))
						sh.sourceStart = s.intValue();
					else if (s.isName("end"))
						sh.sourceEnd = s.intValue();
				}
			} else if (c.isName("dest")) {
				for (NodeWrapper d : c.children()) {
					if (d.isName("start"))
						sh.destStart = d.intValue();
					else if (d.isName("end"))
						sh.destEnd = d.intValue();
				}
			} else if (c.isName("content"))
				sh.content = c.value();
			else if (c.isName("content_redirection"))
				sh.contentRedirection = c.value();
			else if (c.isName("url")) {
				for (NodeWrapper u : c.children()) {
					if (u.isName("game"))
						sh.game = new Url(u.boolAttr("chk_use"), u.value());
					else if (u.isName("stock"))
						sh.stock = new Url(u.boolAttr("chk_use"), u.value());
					else if (u.isName("news"))
						sh.news = new Url(u.boolAttr("chk_use"), u.value());
					else if (u.isName("itv"))
						sh.itv = new Url(u.boolAttr("chk_use"), u.value());
					else if (u.isName("email"))
						sh.email = new Url(u.boolAttr("chk_use"), u.value());
					else if (u.isName("webhard"))
						sh.webhard = new Url(u.boolAttr("chk_use"), u.value());
					else if (u.isName("p2p"))
						sh.p2p = new Url(u.boolAttr("chk_use"), u.value());
					else if (u.isName("user"))
						sh.user = new Url(u.boolAttr("chk_use"), u.value());
				}
			} else if (c.isName("site")) {
				sh.siteKiscom = c.boolAttr("chk_kiscom");
				sh.siteSafenet = c.boolAttr("chk_safenet");
				sh.siteYouth = c.boolAttr("chk_youth");
			} else if (c.isName("site_redirection"))
				sh.siteRedirection = c.value();
			else if (c.isName("option")) {
				sh.optionLanguage = c.intAttr("language");
				sh.optionNude = c.intAttr("nude");
				sh.optionSex = c.intAttr("sex");
				sh.optionViolence = c.intAttr("violence");
				sh.optionEtc1 = c.boolAttr("chk_etc1");
				sh.optionEtc2 = c.boolAttr("chk_etc2");
			}
		}

		return sh;
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

	public Protocol getProtocol() {
		return protocol;
	}

	public void setProtocol(Protocol protocol) {
		this.protocol = protocol;
	}

	public int getSourceStart() {
		return sourceStart;
	}

	public void setSourceStart(int sourceStart) {
		this.sourceStart = sourceStart;
	}

	public int getSourceEnd() {
		return sourceEnd;
	}

	public void setSourceEnd(int sourceEnd) {
		this.sourceEnd = sourceEnd;
	}

	public int getDestStart() {
		return destStart;
	}

	public void setDestStart(int destStart) {
		this.destStart = destStart;
	}

	public int getDestEnd() {
		return destEnd;
	}

	public void setDestEnd(int destEnd) {
		this.destEnd = destEnd;
	}

	public String getContent() {
		return content;
	}

	public void setContent(String content) {
		this.content = content;
	}

	public String getContentRedirection() {
		return contentRedirection;
	}

	public void setContentRedirection(String contentRedirection) {
		this.contentRedirection = contentRedirection;
	}

	public Url getGame() {
		return game;
	}

	public void setGame(Url game) {
		this.game = game;
	}

	public Url getStock() {
		return stock;
	}

	public void setStock(Url stock) {
		this.stock = stock;
	}

	public Url getNews() {
		return news;
	}

	public void setNews(Url news) {
		this.news = news;
	}

	public Url getItv() {
		return itv;
	}

	public void setItv(Url itv) {
		this.itv = itv;
	}

	public Url getEmail() {
		return email;
	}

	public void setEmail(Url email) {
		this.email = email;
	}

	public Url getWebhard() {
		return webhard;
	}

	public void setWebhard(Url webhard) {
		this.webhard = webhard;
	}

	public Url getP2p() {
		return p2p;
	}

	public void setP2p(Url p2p) {
		this.p2p = p2p;
	}

	public Url getUser() {
		return user;
	}

	public void setUser(Url user) {
		this.user = user;
	}

	public boolean isSiteKiscom() {
		return siteKiscom;
	}

	public void setSiteKiscom(boolean siteKiscom) {
		this.siteKiscom = siteKiscom;
	}

	public boolean isSiteSafenet() {
		return siteSafenet;
	}

	public void setSiteSafenet(boolean siteSafenet) {
		this.siteSafenet = siteSafenet;
	}

	public boolean isSiteYouth() {
		return siteYouth;
	}

	public void setSiteYouth(boolean siteYouth) {
		this.siteYouth = siteYouth;
	}

	public String getSiteRedirection() {
		return siteRedirection;
	}

	public void setSiteRedirection(String siteRedirection) {
		this.siteRedirection = siteRedirection;
	}

	public int getOptionLanguage() {
		return optionLanguage;
	}

	public void setOptionLanguage(int optionLanguage) {
		this.optionLanguage = optionLanguage;
	}

	public int getOptionNude() {
		return optionNude;
	}

	public void setOptionNude(int optionNude) {
		this.optionNude = optionNude;
	}

	public int getOptionSex() {
		return optionSex;
	}

	public void setOptionSex(int optionSex) {
		this.optionSex = optionSex;
	}

	public int getOptionViolence() {
		return optionViolence;
	}

	public void setOptionViolence(int optionViolence) {
		this.optionViolence = optionViolence;
	}

	public boolean isOptionEtc1() {
		return optionEtc1;
	}

	public void setOptionEtc1(boolean optionEtc1) {
		this.optionEtc1 = optionEtc1;
	}

	public boolean isOptionEtc2() {
		return optionEtc2;
	}

	public void setOptionEtc2(boolean optionEtc2) {
		this.optionEtc2 = optionEtc2;
	}

	@Override
	protected Element convertToElement(Document doc) {
		Element e = doc.createElement("service");

		e.setAttribute("type", "http");
		e.setAttribute("num", String.valueOf(num));
		e.setAttribute("cid", cid);
		appendChild(doc, e, "name", name);
		appendChild(doc, e, "desc", desc);
		appendChild(doc, e, "protocol", protocol);
		Element source = appendChild(doc, e, "source", null);
		appendChild(doc, source, "start", sourceStart);
		appendChild(doc, source, "end", sourceEnd);
		Element dest = appendChild(doc, e, "dest", null);
		appendChild(doc, dest, "start", destStart);
		appendChild(doc, dest, "end", destEnd);
		appendChild(doc, e, "content", content);
		appendChild(doc, e, "content_redirection", contentRedirection);
		Element url = appendChild(doc, e, "url", null);
		appendChild(doc, url, "game", game.redirect, new AttributeBuilder("chk_use", game.use));
		appendChild(doc, url, "stock", stock.redirect, new AttributeBuilder("chk_use", stock.use));
		appendChild(doc, url, "news", news.redirect, new AttributeBuilder("chk_use", news.use));
		appendChild(doc, url, "itv", itv.redirect, new AttributeBuilder("chk_use", itv.use));
		appendChild(doc, url, "email", email.redirect, new AttributeBuilder("chk_use", email.use));
		appendChild(doc, url, "webhard", webhard.redirect, new AttributeBuilder("chk_use", webhard.use));
		appendChild(doc, url, "p2p", p2p.redirect, new AttributeBuilder("chk_use", p2p.use));
		appendChild(doc, url, "user", user.redirect, new AttributeBuilder("chk_use", user.use));
		AttributeBuilder siteAttr = new AttributeBuilder("chk_kiscom", siteKiscom).put("chk_safenet", siteSafenet).put(
				"chk_youth", siteYouth);
		appendChild(doc, e, "site", null, siteAttr);
		appendChild(doc, e, "site_redirection", siteRedirection);
		AttributeBuilder optionAttr = new AttributeBuilder("language", optionLanguage).put("nude", optionNude)
				.put("sex", optionSex).put("violence", optionViolence).put("chk_etc1", optionEtc1)
				.put("chk_etc2", optionEtc2);
		appendChild(doc, e, "option", null, optionAttr);

		return e;
	}

	@Override
	public Map<String, Object> marshal() {
		Map<String, Object> m = new HashMap<String, Object>();
		m.put("num", num);
		m.put("cid", cid);
		m.put("name", name);
		m.put("desc", desc);
		m.put("protocol", protocol);
		m.put("source", new MarshalValue("start", sourceStart).put("end", sourceEnd).get());
		m.put("dest", new MarshalValue("start", destStart).put("end", destEnd).get());
		m.put("content", new MarshalValue("value", content).put("redirection", contentRedirection).get());
		m.put("url",
				new MarshalValue("game", game.marshal()).put("stock", stock.marshal()).put("news", news.marshal())
						.put("itv", itv.marshal()).put("email", email.marshal()).put("webhard", webhard.marshal())
						.put("p2p", p2p.marshal()).put("user", user.marshal()).get());
		m.put("site",
				new MarshalValue("kiscom", siteKiscom).put("safenet", siteSafenet).put("youth", siteYouth)
						.put("redirection", siteRedirection).get());
		m.put("option",
				new MarshalValue("language", optionLanguage).put("nude", optionNude).put("sex", optionSex)
						.put("violence", optionViolence).put("etc1", optionEtc1).put("etc2", optionEtc2).get());

		return m;
	}

	@Override
	public String toString() {
		return "ServiceHttp [num=" + num + ", cid=" + cid + ", name=" + name + ", desc=" + desc + ", protocol="
				+ protocol + ", sourceStart=" + sourceStart + ", sourceEnd=" + sourceEnd + ", destStart=" + destStart
				+ ", destEnd=" + destEnd + ", content=" + content + ", contentredirection=" + contentRedirection
				+ ", game=" + game + ", stock=" + stock + ", news=" + news + ", itv=" + itv + ", email=" + email
				+ ", webhard=" + webhard + ", p2p=" + p2p + ", user=" + user + ", siteKiscom=" + siteKiscom
				+ ", siteSafenet=" + siteSafenet + ", siteYouth=" + siteYouth + ", siteRedirection=" + siteRedirection
				+ ", optionLanguage=" + optionLanguage + ", optionNude=" + optionNude + ", optionSex=" + optionSex
				+ ", optionViolence=" + optionViolence + ", optionEtc1=" + optionEtc1 + ", optionEtc2=" + optionEtc2
				+ "]";
	}
}
