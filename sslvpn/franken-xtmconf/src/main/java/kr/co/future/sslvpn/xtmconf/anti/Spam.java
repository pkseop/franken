package kr.co.future.sslvpn.xtmconf.anti;

import java.util.HashMap;
import java.util.Map;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import kr.co.future.sslvpn.xtmconf.AttributeBuilder;
import kr.co.future.sslvpn.xtmconf.NodeWrapper;
import kr.co.future.sslvpn.xtmconf.XtmConfig;

public class Spam extends XtmConfig {
	public static enum Type {
		DB, Content;

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

	public static enum SpamRule {
		Detect, Alert
	}

	public static enum BlockingRule {
		WhitelistSender("Whitelist_sender"), WhitelistRecipient("Whitelist_recipient"), WhitelistSubject(
				"Whitelist_subject"), Sender("sender"), Recipient("recipient"), Subject("subject"), Content("content"), URI(
				"URI"), Rawbody("rawbody");

		private String name;

		private BlockingRule(String name) {
			this.name = name;
		}

		public static BlockingRule get(String str) {
			for (BlockingRule b : BlockingRule.values()) {
				if (b.toString().equals(str))
					return b;
			}
			return null;
		}

		@Override
		public String toString() {
			return name;
		}
	}

	private Type type;
	private boolean spamdb; // 스팸DB
	private boolean autolearn; // 자동 스팸DB 학습기능
	private String server; // RBL 서버명
	private SpamRule spam; // SPAM
	private boolean smtp; // SMTP
	private boolean pop3; // POP3
	private int num;
	private BlockingRule blockingRule; // 차단규칙
	private String blockingName; // 차단내용

	@Override
	public String getXmlFilename() {
		return "anti_spam.xml";
	}

	@Override
	public String getRootTagName() {
		return "spam";
	}

	public static Spam parse(NodeWrapper nw) {
		if (Type.get(nw.name()) == null)
			return null;

		Spam s = new Spam();
		s.type = Type.get(nw.name());
		if (s.type == Type.Content)
			s.num = nw.intAttr("num");

		for (NodeWrapper c : nw.children()) {
			if (c.isName("setting") && s.type == Type.DB) {
				s.spamdb = c.boolAttr("chk_spamdb");
				s.autolearn = c.boolAttr("chk_autolearn");
			} else if (c.isName("server") && s.type == Type.DB)
				s.server = c.value();
			else if (c.isName("spam") && s.type == Type.DB)
				s.spam = SpamRule.valueOf(c.value());
			else if (c.isName("protocol") && s.type == Type.DB) {
				s.smtp = c.boolAttr("chk_smtp");
				s.pop3 = c.boolAttr("chk_pop3");
			} else if (c.isName("blocking") && s.type == Type.Content) {
				s.blockingRule = BlockingRule.get(c.attr("rule"));
				s.blockingName = c.value();
			}
		}

		return s;
	}

	public Type getType() {
		return type;
	}

	public void setType(Type type) {
		this.type = type;
	}

	public boolean isSpamdb() {
		return spamdb;
	}

	public void setSpamdb(boolean spamdb) {
		this.spamdb = spamdb;
	}

	public boolean isAutolearn() {
		return autolearn;
	}

	public void setAutolearn(boolean autolearn) {
		this.autolearn = autolearn;
	}

	public String getServer() {
		return server;
	}

	public void setServer(String server) {
		this.server = server;
	}

	public SpamRule getSpam() {
		return spam;
	}

	public void setSpam(SpamRule spam) {
		this.spam = spam;
	}

	public boolean isSmtp() {
		return smtp;
	}

	public void setSmtp(boolean smtp) {
		this.smtp = smtp;
	}

	public boolean isPop3() {
		return pop3;
	}

	public void setPop3(boolean pop3) {
		this.pop3 = pop3;
	}

	public int getNum() {
		return num;
	}

	public void setNum(int num) {
		this.num = num;
	}

	public BlockingRule getBlockingRule() {
		return blockingRule;
	}

	public void setBlockingRule(BlockingRule blockingRule) {
		this.blockingRule = blockingRule;
	}

	public String getBlockingName() {
		return blockingName;
	}

	public void setBlockingName(String blockingName) {
		this.blockingName = blockingName;
	}

	@Override
	protected Element convertToElement(Document doc) {
		Element e = doc.createElement(type.toString());

		if (type == Type.DB) {
			appendChild(doc, e, "setting", null,
					new AttributeBuilder("chk_spamdb", spamdb).put("chk_autolearn", autolearn));
			appendChild(doc, e, "server", server);
			appendChild(doc, e, "spam", spam);
			appendChild(doc, e, "protocol", null, new AttributeBuilder("chk_smtp", smtp).put("chk_pop3", pop3));
		} else if (type == Type.Content) {
			e.setAttribute("num", String.valueOf(num));
			appendChild(doc, e, "blocking", blockingName, new AttributeBuilder("rule", blockingRule));
		}

		return e;
	}

	@Override
	public Map<String, Object> marshal() {
		Map<String, Object> m = new HashMap<String, Object>();
		m.put("type", type);
		if (type == Type.DB) {
			m.put("spamdb", spamdb);
			m.put("autolearn", autolearn);
			m.put("server", server);
			m.put("spam", spam);
			m.put("smtp", smtp);
			m.put("pop3", pop3);
		} else if (type == Type.Content) {
			m.put("num", num);
			m.put("blocking_rule", blockingRule);
			m.put("blocking_name", blockingName);
		}

		return m;
	}

	@Override
	public String toString() {
		return "Spam [type=" + type + ", spamdb=" + spamdb + ", autolearn=" + autolearn + ", server=" + server
				+ ", spam=" + spam + ", smtp=" + smtp + ", pop3=" + pop3 + ", num=" + num + ", blockingRule="
				+ blockingRule + ", blockingName=" + blockingName + "]";
	}
}
