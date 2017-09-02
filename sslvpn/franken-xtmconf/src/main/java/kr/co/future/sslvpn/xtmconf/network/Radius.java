package kr.co.future.sslvpn.xtmconf.network;

import java.util.HashMap;
import java.util.Map;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import kr.co.future.sslvpn.xtmconf.AttributeBuilder;
import kr.co.future.sslvpn.xtmconf.MarshalValue;
import kr.co.future.sslvpn.xtmconf.NodeWrapper;
import kr.co.future.sslvpn.xtmconf.Utils;
import kr.co.future.sslvpn.xtmconf.XtmConfig;

public class Radius extends XtmConfig {
	public static enum Type {
		Radius, Domain;

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

	public static enum AuthMethod {
		PAP, CHAP
	}

	public static enum Fileserver {
		Active_Directory, LDAP;

		public static Fileserver get(String str) {
			for (Fileserver f : Fileserver.values()) {
				if (f.toString().equals(str))
					return f;
			}
			return null;
		}

		@Override
		public String toString() {
			return name().replace("_", " ");
		}
	}

	private Type type;
	private boolean radiusUse; // Radius 서버사용
	private String radiusIp; // Radius 서버 IP
	private String radiusPassword; // 접근암호
	private AuthMethod authMethod; // 인증 방식
	private Integer authPort; // Auth 포트
	private Integer accountPort; // Account 포트
	private Integer radiusCycle; // Radius 상태체크
	private String ldapAddress; // Domain 서버주소
	private boolean ldapUse; // 파일서버 사용
	private Fileserver ldapType; // 파일서버
	private String ldapAccount; // 파일서버 ID
	private String ldapPassword; // 파일서버 암호
	private Integer ldapCycle; // 파일서버 상태체크
	private boolean ldapUseTrustStore;
	private String ldapTrustStore;
	private String ldapBaseDn; // BASE DN (신규 추가됨)

	@Override
	public String getXmlFilename() {
		return "network_radius.xml";
	}

	@Override
	public String getRootTagName() {
		return "nac";
	}

	public static Radius parse(NodeWrapper nw) {
		if (!nw.isName("radius") && !nw.isName("domain"))
			return null;

		Radius r = new Radius();
		r.type = Type.get(nw.name());

		if (r.type == Type.Radius) {
			r.radiusUse = nw.boolAttr("chk_use");

			for (NodeWrapper c : nw.children()) {
				if (c.isName("ip"))
					r.radiusIp = c.value();
				else if (c.isName("password"))
					r.radiusPassword = c.value();
				else if (c.isName("auth_method"))
					r.authMethod = AuthMethod.valueOf(c.value());
				else if (c.isName("auth_port"))
					r.authPort = c.intValue();
				else if (c.isName("account_port"))
					r.accountPort = c.intValue();
				else if (c.isName("radius_cycle"))
					r.radiusCycle = c.intValue();
			}
		} else if (r.type == Type.Domain) {
			for (NodeWrapper c : nw.children()) {
				if (c.isName("name"))
					r.ldapAddress = c.value();
				else if (c.isName("directory")) {
					r.ldapUse = c.boolAttr("chk_use");
					r.ldapType = Fileserver.get(c.value());
				} else if (c.isName("id"))
					r.ldapAccount = c.value();
				else if (c.isName("password"))
					r.ldapPassword = c.value();
				else if (c.isName("fs_cycle"))
					r.ldapCycle = c.intValue();
				else if (c.isName("base_dn")) {
					r.ldapBaseDn = c.value();
					if (r.ldapBaseDn != null && r.ldapBaseDn.isEmpty())
						r.ldapBaseDn = null;
				} else if (c.isName("trust_store")) {
					r.ldapUseTrustStore = c.boolAttr("chk_use");
					r.ldapTrustStore = c.value();
				}
			}
		}

		return r;
	}

	public Type getType() {
		return type;
	}

	public void setType(Type type) {
		this.type = type;
	}

	public boolean isRadiusUse() {
		return radiusUse;
	}

	public void setRadiusUse(boolean radiusUse) {
		this.radiusUse = radiusUse;
	}

	public String getRadiusIp() {
		return radiusIp;
	}

	public void setRadiusIp(String radiusIp) {
		this.radiusIp = radiusIp;
	}

	public String getRadiusPassword() {
		return radiusPassword;
	}

	public void setRadiusPassword(String radiusPassword) {
		this.radiusPassword = radiusPassword;
	}

	public AuthMethod getAuthMethod() {
		return authMethod;
	}

	public void setAuthMethod(AuthMethod authMethod) {
		this.authMethod = authMethod;
	}

	public Integer getAuthPort() {
		return authPort;
	}

	public void setAuthPort(Integer authPort) {
		this.authPort = authPort;
	}

	public Integer getAccountPort() {
		return accountPort;
	}

	public void setAccountPort(Integer accountPort) {
		this.accountPort = accountPort;
	}

	public Integer getRadiusCycle() {
		return radiusCycle;
	}

	public void setRadiusCycle(Integer radiusCycle) {
		this.radiusCycle = radiusCycle;
	}

	public String getLdapAddress() {
		return ldapAddress;
	}

	public void setLdapAddress(String ldapAddress) {
		this.ldapAddress = ldapAddress;
	}

	public boolean isLdapUse() {
		return ldapUse;
	}

	public void setLdapUse(boolean ldapUse) {
		this.ldapUse = ldapUse;
	}

	public Fileserver getLdapType() {
		return ldapType;
	}

	public void setLdapType(Fileserver ldapType) {
		this.ldapType = ldapType;
	}

	public String getLdapAccount() {
		return ldapAccount;
	}

	public void setLdapAccount(String ldapAccount) {
		this.ldapAccount = ldapAccount;
	}

	public String getLdapPassword() {
		return ldapPassword;
	}

	public void setLdapPassword(String ldapPassword) {
		this.ldapPassword = ldapPassword;
	}

	public Integer getLdapCycle() {
		return ldapCycle;
	}

	public void setLdapCycle(Integer ldapCycle) {
		this.ldapCycle = ldapCycle;
	}

	public boolean isLdapUseTrustStore() {
		return ldapUseTrustStore;
	}

	public void setLdapUseTrustStore(boolean ldapUseTrustStore) {
		this.ldapUseTrustStore = ldapUseTrustStore;
	}

	public String getLdapTrustStore() {
		return ldapTrustStore;
	}

	public void setLdapTrustStore(String ldapTrustStore) {
		this.ldapTrustStore = ldapTrustStore;
	}

	public String getLdapBaseDn() {
		return ldapBaseDn;
	}

	public void setLdapBaseDn(String ldapBaseDn) {
		this.ldapBaseDn = ldapBaseDn;
	}

	@Override
	protected Element convertToElement(Document doc) {
		Element e = doc.createElement(type.toString());

		if (type == Type.Radius) {
			e.setAttribute("chk_use", Utils.bool(radiusUse));
			appendChild(doc, e, "ip", radiusIp);
			appendChild(doc, e, "password", radiusPassword);
			appendChild(doc, e, "auth_method", authMethod);
			appendChild(doc, e, "auth_port", authPort);
			appendChild(doc, e, "account_port", accountPort);
			appendChild(doc, e, "radius_cycle", radiusCycle);
		} else if (type == Type.Domain) {
			appendChild(doc, e, "name", ldapAddress);
			appendChild(doc, e, "directory", ldapType, new AttributeBuilder("chk_use", ldapUse));
			appendChild(doc, e, "id", ldapAccount);
			appendChild(doc, e, "password", ldapPassword);
			appendChild(doc, e, "fs_cycle", ldapCycle);
			appendChild(doc, e, "trust_store", ldapTrustStore, new AttributeBuilder("chk_use", ldapUseTrustStore));
			appendChild(doc, e, "base_dn", ldapBaseDn);
		}

		return e;
	}

	@Override
	public Map<String, Object> marshal() {
		Map<String, Object> m = new HashMap<String, Object>();
		m.put("type", type);

		if (type == Type.Radius) {
			m.put("use", radiusUse);
			m.put("ip", radiusIp);
			m.put("password", radiusPassword);
			m.put("auth_method", authMethod);
			m.put("auth_port", authPort);
			m.put("account_port", accountPort);
			m.put("radius_cycle", radiusCycle);
		} else if (type == Type.Domain) {
			m.put("name", ldapAddress);
			m.put("directory", new MarshalValue("use", ldapUse).put("value", ldapType).get());
			m.put("id", ldapAccount);
			m.put("password", ldapPassword);
			m.put("fs_cycle", ldapCycle);
			m.put("trust_store", ldapUseTrustStore);
			m.put("base_dn", ldapBaseDn);
		}

		return m;
	}

	@Override
	public String toString() {
		return "Radius [type=" + type + ", radiusUse=" + radiusUse + ", radiusIp=" + radiusIp + ", radiusPassword="
				+ radiusPassword + ", authMethod=" + authMethod + ", authPort=" + authPort + ", accountPort=" + accountPort
				+ ", radiusCycle=" + radiusCycle + ", ldapAddress=" + ldapAddress + ", ldapUse=" + ldapUse + ", ldapType="
				+ ldapType + ", ldapAccount=" + ldapAccount + ", ldapPassword=" + ldapPassword + ", ldapCycle=" + ldapCycle
				+ ", ldapUseTrustStore=" + ldapUseTrustStore + ", ldapTrustStore=" + ldapTrustStore + "]";
	}
}
