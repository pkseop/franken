package kr.co.future.sslvpn.authui;

import java.util.Date;

import kr.co.future.api.FieldOption;
import kr.co.future.confdb.CollectionName;

@CollectionName("password_reset_tokens")
public class PasswordResetToken {
	@FieldOption(nullable = false)
	private String token;

	@FieldOption(nullable = false)
	private String loginName;

	@FieldOption(nullable = false)
	private boolean used;

	@FieldOption(nullable = false)
	private Date created = new Date();

	public PasswordResetToken() {
	}

	public PasswordResetToken(String token, String loginName) {
		this.token = token;
		this.loginName = loginName;
	}

	public String getToken() {
		return token;
	}

	public void setToken(String token) {
		this.token = token;
	}

	public String getLoginName() {
		return loginName;
	}

	public void setLoginName(String loginName) {
		this.loginName = loginName;
	}

	public boolean isUsed() {
		return used;
	}

	public void setUsed(boolean used) {
		this.used = used;
	}

	public Date getCreated() {
		return created;
	}

	public void setCreated(Date created) {
		this.created = created;
	}

}
