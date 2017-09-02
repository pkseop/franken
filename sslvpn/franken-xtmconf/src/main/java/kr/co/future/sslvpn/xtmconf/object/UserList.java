package kr.co.future.sslvpn.xtmconf.object;

import java.util.HashMap;
import java.util.Map;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import kr.co.future.sslvpn.xtmconf.AttributeBuilder;
import kr.co.future.sslvpn.xtmconf.NodeWrapper;
import kr.co.future.sslvpn.xtmconf.XtmConfig;

public class UserList extends XtmConfig {
	public static enum AuthType {
		Local, Radius;

		public static AuthType get(String str) {
			for (AuthType t : AuthType.values()) {
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

	private int num;
	private String cid;
	private AuthType authType; // 인증방식
	private boolean modeAllow; // 접속제한
	private String name; // 객체명
	private String desc; // 설명
	private String id; // 아이디
	private String password; // 비밀번호
	private String salt;
	private String groupCid; // 접근서버 그룹 (ssl.AccessGroup.cid)

	@Override
	public String getXmlFilename() {
		return "object_user_list.xml";
	}

	@Override
	public String getRootTagName() {
		return "object";
	}

	public static UserList parse(NodeWrapper nw) {
		if (!nw.isName("user"))
			return null;

		UserList ul = new UserList();
		ul.num = Integer.parseInt(nw.attr("num"));
		ul.cid = nw.attr("cid");

		for (NodeWrapper c : nw.children()) {
			if (c.isName("setting")) {
				ul.authType = AuthType.get(c.attr("authtype"));
				ul.modeAllow = c.attr("mode").equals("allow");
			} else if (c.isName("name"))
				ul.name = c.value();
			else if (c.isName("desc"))
				ul.desc = c.value();
			else if (c.isName("id"))
				ul.id = c.value();
			else if (c.isName("password"))
				ul.password = c.value();
			else if (c.isName("salt"))
				ul.salt = c.value();
			else if (c.isName("group"))
				ul.groupCid = c.attr("cid");
		}

		return ul;
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

	public AuthType getAuthType() {
		return authType;
	}

	public void setAuthType(AuthType authType) {
		this.authType = authType;
	}

	public boolean isModeAllow() {
		return modeAllow;
	}

	public void setModeAllow(boolean modeAllow) {
		this.modeAllow = modeAllow;
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

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public String getSalt() {
		return salt;
	}

	public void setSalt(String salt) {
		this.salt = salt;
	}

	public String getGroupCid() {
		return groupCid;
	}

	public void setGroupCid(String groupCid) {
		this.groupCid = groupCid;
	}

	@Override
	protected Element convertToElement(Document doc) {
		Element e = doc.createElement("user");

		e.setAttribute("num", String.valueOf(num));
		e.setAttribute("cid", cid);
		appendChild(doc, e, "setting", null,
				new AttributeBuilder("authtype", authType).put("mode", modeAllow ? "allow" : "deny"));
		appendChild(doc, e, "name", name);
		appendChild(doc, e, "desc", desc);
		appendChild(doc, e, "id", id);
		appendChild(doc, e, "password", password);
		appendChild(doc, e, "salt", salt);
		appendChild(doc, e, "group", null, new AttributeBuilder("cid", groupCid));

		return e;
	}

	@Override
	public Map<String, Object> marshal() {
		Map<String, Object> m = new HashMap<String, Object>();
		m.put("num", num);
		m.put("cid", cid);
		m.put("auth_type", authType);
		m.put("mode_allow", modeAllow);
		m.put("name", name);
		m.put("desc", desc);
		m.put("id", id);
		m.put("group_cid", groupCid);

		return m;
	}

	@Override
	public String toString() {
		return "UserList [num=" + num + ", cid=" + cid + ", authType=" + authType + ", modeAllow=" + modeAllow
				+ ", name=" + name + ", desc=" + desc + ", id=" + id + ", groupCid=" + groupCid + "]";
	}
}
