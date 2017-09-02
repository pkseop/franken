package kr.co.future.sslvpn.core;

import kr.co.future.sslvpn.core.AuthCode;

public enum AuthCode {
	CertSuccess("success", 0), 
	LocalSuccess("success", 0), 
	RadiusSuccess("success", 0),
	LdapSuccess("success", 0),
	NpkiSuccess("success", 0),
	SqlSuccess("success", 0),
	ExternalSuccess("success", 0),
	UserNotFound("user-not-found", 1), 
	PasswordFail("password-fail", 2), 
	CrlConnectFail("crl-connect-fail", 3), 
	OcspConnectFail("ocsp-connect-fail", 4), 
	CertRevoked("cert-revoked", 5), 
	Locked("policy-locked", 6), 
	Expired("policy-expired", 7),
	ClientIPRange("policy-client-iprange", 8), 
	Schedule("policy-schedule", 9), 
	RadiusNoUse("radius-no-use", -1), 
	RadiusReject("radius-reject", 10), 
	RadiusTimeout("radius-timeout", 11), 
	LdapNoUse("ldap-no-use", -1), 
	RpcError("rpc-error", -2),
	IOSNoUse("ios-no-use", -3),
	LdapConnectFail("ldap-connect-fail", 12), 
	LdapReject("ldap-reject", 13),
	NpkiFail("npki-fail", 14),
	IpLeaseFail("ip-lease-fail", 15),
	NpkiIdnFail("npki-idn-fail", 16),
	NpkiNotFound("npki-not-found", 17),
	PolicyMismatch("policy-mismatch", 18),
	TunnelLimit("tunnel-limit", 19),
	DeviceFail("device-fail", 20),
	CertExpired("cert-expired", 21),
	CertVerifyFail("cert-verify-fail", 22),
	ProfileNotFound("profile-not-found", 23),
	PasswordExpired("password-expired", 24),
	OtpFail("otp-fail", 25),
	IdnNotFound("idn-not-found", 26),
	DeviceExpired("device-expired", 27),
	DeviceLocked("device-locked", 28),
	SubjectDnNotFound("subject-dn-not-found", 29),
	UserLimit("user-limit", 30),
	AutoLocked("auto-locked", 32),
	DuplicatedLogin("duplicated-login", 33),
	ResidentNumberFail("resident-number-fail", 34);

	AuthCode(String status, int code) {
		this.status = status;
		this.code = code;
	}

	private String status;
	private int code;

	public String getStatus() {
		return status;
	}

	public int getCode() {
		return code;
	}

	public static AuthCode parse(int code) {
		for (AuthCode c : values())
			if (c.getCode() == code)
				return c;

		return null;
	}
}