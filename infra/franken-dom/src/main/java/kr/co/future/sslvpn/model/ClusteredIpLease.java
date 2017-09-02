package kr.co.future.sslvpn.model;

import kr.co.future.api.FieldOption;
import kr.co.future.confdb.CollectionName;

@CollectionName("ip_leases")
public class ClusteredIpLease {
	@FieldOption(nullable = false)
	private String loginName;

	@FieldOption(nullable = false)
	private String profileName;

	@FieldOption(nullable = false)
	private int poolSize;

	@FieldOption(nullable = false)
	private int offset;

	public String getLoginName() {
		return loginName;
	}

	public void setLoginName(String loginName) {
		this.loginName = loginName;
	}

	public String getProfileName() {
		return profileName;
	}

	public void setProfileName(String profileName) {
		this.profileName = profileName;
	}

	public int getPoolSize() {
		return poolSize;
	}

	public void setPoolSize(int poolSize) {
		this.poolSize = poolSize;
	}

	public int getOffset() {
		return offset;
	}

	public void setOffset(int offset) {
		this.offset = offset;
	}
}
