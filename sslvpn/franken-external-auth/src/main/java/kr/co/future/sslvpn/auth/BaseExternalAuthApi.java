package kr.co.future.sslvpn.auth;

public abstract class BaseExternalAuthApi implements ExternalAuthApi {

	@Override
	public String getIdn(String loginName) {
		return null;
	}

	@Override
	public String getSubjectDn(String loginName) {
		return null;
	}

	@Override
	public boolean isPasswordChangeSupported() {
		return false;
	}

	@Override
	public void changePassword(String account, String newPassword) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean isPasswordExpirySupported() {
		return false;
	}

	@Override
	public boolean isAccountExpirySupported() {
		return false;
	}

	@Override
	public long getPasswordExpiry(String loginName) {
		throw new UnsupportedOperationException();
	}

	@Override
	public long getAccountExpiry(String loginName) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean useSso() {
		return false;
	}

	@Override
	public String getSsoToken(String loginName) {
		return null;
	}

	@Override
	public boolean verifySso(String loginName, String clientIp) {
		return false;
	}
}
