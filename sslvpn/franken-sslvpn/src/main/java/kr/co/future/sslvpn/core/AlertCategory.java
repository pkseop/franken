package kr.co.future.sslvpn.core;

public enum AlertCategory {
	FileIntegrity("file_integrity"),
	TunnelCapacity("tunnel_capacity"),
	LoginBruteforce("login_bruteforce"),
	AuthPolicyViolation("auth_policy_violation"),
	NacIsolation("nac_isolation"),
	AccessViolation("access_violation");
	
	AlertCategory(String code) {
		this.code = code;
	}
	
	public String getCode() {
		return code;
	}
	
	private String code;
}
