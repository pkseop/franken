package kr.co.future.sslvpn.core.impl;

import java.util.concurrent.atomic.AtomicLong;

import kr.co.future.sslvpn.core.AuthCode;
import kr.co.future.sslvpn.core.AuthService;

import kr.co.future.radius.server.RadiusUserDatabase;
import kr.co.future.radius.server.RadiusUserDatabaseFactory;

public class RadiusSslUserDatabase extends RadiusUserDatabase {
	private AuthService auth;
	private AtomicLong passes;
	private AtomicLong fails;

	public RadiusSslUserDatabase(String name, RadiusUserDatabaseFactory factory, AuthService auth) {
		super(name, factory);
		this.auth = auth;
		this.passes = new AtomicLong();
		this.fails = new AtomicLong();
	}

	@Override
	public boolean verifyPassword(String userName, String password) {
		AuthCode authCode = auth.checkPassword(userName, password);
		boolean result = authCode != null && authCode.getCode() == 0;
		if (result)
			passes.incrementAndGet();
		else
			fails.incrementAndGet();

		return result;
	}

	@Override
	public String toString() {
		return "SSLplus RADIUS User Database [pass=" + passes.longValue() + ", fail=" + fails.longValue() + "]";
	}

}
