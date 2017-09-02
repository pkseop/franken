package kr.co.future.sslvpn.core.impl;

import java.util.Map;

import kr.co.future.sslvpn.auth.ExternalAuthApi;
import kr.co.future.sslvpn.core.ExternalAuthConfig;
import kr.co.future.sslvpn.core.ExternalAuthService;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.apache.felix.ipojo.annotations.Validate;

import kr.co.future.confdb.Config;
import kr.co.future.confdb.ConfigDatabase;
import kr.co.future.confdb.ConfigService;
import kr.co.future.rpc.RpcException;
import kr.co.future.sslvpn.core.impl.ExternalAuthServiceImpl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(name = "frodo-external-auth")
@Provides
public class ExternalAuthServiceImpl implements ExternalAuthService {
	private final Logger logger = LoggerFactory.getLogger(ExternalAuthServiceImpl.class.getName());

	@Requires
	private ConfigService conf;

	@Requires(optional = true, nullable = true)
	private ExternalAuthApi authApi;

	private boolean enabled;

	@Validate
	public void start() {
		ExternalAuthConfig c = getConfig();
		if (c != null && c.isEnabled())
			this.enabled = true;
	}

	@Override
	public boolean isEnabled() {
		return enabled;
	}

	@Override
	public ExternalAuthConfig getConfig() {
		ConfigDatabase db = conf.ensureDatabase("frodo");
		Config c = db.findOne(ExternalAuthConfig.class, null);
		if (c == null)
			return null;

		return c.getDocument(ExternalAuthConfig.class);
	}

	@Override
	public void setConfig(ExternalAuthConfig config) {
		ConfigDatabase db = conf.ensureDatabase("frodo");
		Config c = db.findOne(ExternalAuthConfig.class, null);
		if (c == null)
			db.add(config);
		else
			db.update(c, config);

		this.enabled = config.isEnabled();
	}

	@SuppressWarnings("unchecked")
	@Override
	public Map<String, Object> login(Map<String, Object> props) {
		if (authApi == null)
			throw new RpcException("external auth is not configured");

		return (Map<String, Object>) authApi.login(props);
	}

	@Override
	public Map<String, Object> verifyUser(String loginName) {
		if (authApi == null)
			throw new RpcException("external auth is not configured");

		return authApi.verifyUser(loginName);
	}

	@Override
	public String getIdn(String loginName) {
		if (authApi == null)
			throw new RpcException("external auth is not configured");

		return authApi.getIdn(loginName);
	}

	@Override
	public String getSubjectDn(String loginName) {
		if (authApi == null)
			throw new RpcException("external auth is not configured");

		return authApi.getSubjectDn(loginName);
	}

	@Override
	public boolean isPasswordChangeSupported() {
		if (authApi == null)
			return false;

		return authApi.isPasswordChangeSupported();
	}

	@Override
	public void changePassword(String account, String newPassword) {
		if (authApi == null)
			throw new RpcException("external auth is not configured");

		authApi.changePassword(account, newPassword);

	}

	@Override
	public boolean isPasswordExpirySupported() {
		if (authApi == null)
			return false;

		return authApi.isPasswordExpirySupported();
	}

	@Override
	public long getPasswordExpiry(String loginName) {
		if (authApi == null)
			throw new RpcException("external auth is not configured");

		return authApi.getPasswordExpiry(loginName);
	}

	@Override
	public long getAccountExpiry(String loginName) {
		if (authApi == null)
			throw new RpcException("external auth is not configured");

		return authApi.getAccountExpiry(loginName);
	}

	@Override
	public boolean isAccountExpirySupported() {
		if (authApi == null)
			throw new RpcException("external auth is not configured");

		return authApi.isAccountExpirySupported();
	}

	@Override
	public boolean useSso() {
		if (authApi == null)
			throw new RpcException("external auth is not configured");

		return authApi.useSso();
	}

	@Override
	public String getSsoToken(String loginName) {
		if (authApi == null)
			throw new RpcException("external auth is not configured");

		return authApi.getSsoToken(loginName);
	}

	@Override
	public boolean verifySso(String loginName, String clientIp) {
		if (authApi == null)
			throw new RpcException("external auth is not configured");

		return authApi.verifySso(loginName, clientIp);
	}
}
