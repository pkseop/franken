package kr.co.future.sslvpn.auth.ice;

import kr.co.future.confdb.CollectionName;

@CollectionName("config")
public class IceConfig {

	private String idAttr;
	
	private String ldapProfileName;

	public String getIdAttr() {
		return idAttr;
	}

	public void setIdAttr(String idAttr) {
		this.idAttr = idAttr;
	}
	
	public String getLdapProfileName() {
		return ldapProfileName;
	}

	public void setLdapProfileName(String ldapProfileName) {
		this.ldapProfileName = ldapProfileName;
	}

	@Override
	public String toString() {
		return "idAttribute=" + idAttr + ", ldapProfileName=" + ldapProfileName + "\n";
	}

}
