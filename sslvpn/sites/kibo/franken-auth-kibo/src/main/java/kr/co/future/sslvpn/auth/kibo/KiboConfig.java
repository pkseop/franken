package kr.co.future.sslvpn.auth.kibo;

import java.net.MalformedURLException;
import java.net.URL;

import kr.co.future.api.FieldOption;
import kr.co.future.confdb.CollectionName;

@CollectionName("config")
public class KiboConfig {
	@FieldOption(nullable = false)
	private String url;

	@FieldOption(nullable = false)
	private String adminAccount;

	@FieldOption(nullable = false)
	private String adminPassword;

	@FieldOption(nullable = false)
	private String secureKey;;

	private String ldapAdminAccount;
	private String ldapAdminPassword;

	public URL getUrl() {
		try {
			return new URL(url);
		} catch (MalformedURLException e) {
			return null;
		}
	}

	public void setUrl(URL url) {
		this.url = url.toString();
	}

	public String getAdminAccount() {
		return adminAccount;
	}

	public void setAdminAccount(String adminAccount) {
		this.adminAccount = adminAccount;
	}

	public String getAdminPassword() {
		return adminPassword;
	}

	public void setAdminPassword(String adminPassword) {
		this.adminPassword = adminPassword;
	}

	public String getSecureKey() {
		return secureKey;
	}

	public void setSecureKey(String secureKey) {
		this.secureKey = secureKey;
	}

	public String getLdapAdminAccount() {
		return ldapAdminAccount;
	}

	public void setLdapAdminAccount(String ldapAdminAccount) {
		this.ldapAdminAccount = ldapAdminAccount;
	}

	public String getLdapAdminPassword() {
		return ldapAdminPassword;
	}

	public void setLdapAdminPassword(String ldapAdminPassword) {
		this.ldapAdminPassword = ldapAdminPassword;
	}

	@Override
	public String toString() {
		String result = "";
		result += "URL=" + url.toString() + ", adminAccount=" + adminAccount + ", adminPassword=" + adminPassword
				+ ", ldapAdminAccount=" + ldapAdminAccount + ", ldapAdminPassword=" + ldapAdminPassword + ", secureKey="
				+ secureKey + "\n";
		return result;

	}

}
