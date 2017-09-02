package kr.co.future.sslvpn.xtmconf.system;

import java.util.HashMap;
import java.util.Map;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import kr.co.future.sslvpn.xtmconf.NodeWrapper;
import kr.co.future.sslvpn.xtmconf.XtmConfig;

public class UserAccess extends XtmConfig {
	private int user = 2; // 관리자 접속 허용 갯수
	private Integer limit; // 관리자 인증시도 한계값

	@Override
	public String getXmlFilename() {
		return "system_user_access.xml";
	}

	@Override
	public String getRootTagName() {
		return "system";
	}

	public static UserAccess parse(NodeWrapper nw) {
		if (!nw.isName("access"))
			return null;

		UserAccess ua = new UserAccess();

		for (NodeWrapper c : nw.children()) {
			if (c.isName("user"))
				ua.user = c.intValue();
			else if (c.isName("limit"))
				ua.limit = c.intValue();
		}

		return ua;
	}

	public int getUser() {
		return user;
	}

	public void setUser(int user) {
		this.user = user;
	}

	public Integer getLimit() {
		return limit;
	}

	public void setLimit(Integer limit) {
		this.limit = limit;
	}

	@Override
	protected Element convertToElement(Document doc) {
		Element e = doc.createElement("access");

		appendChild(doc, e, "user", user);
		if (limit != null)
			appendChild(doc, e, "limit", limit);

		return e;
	}

	@Override
	public Map<String, Object> marshal() {
		Map<String, Object> m = new HashMap<String, Object>();
		m.put("user", user);
		m.put("limit", limit);

		return m;
	}

	@Override
	public String toString() {
		return "UserAccess [user=" + user + ", limit=" + limit + "]";
	}
}
