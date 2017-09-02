package kr.co.future.sslvpn.core.impl;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Date;

import kr.co.future.sslvpn.core.impl.LoginBruteforceDetector;

import org.junit.Test;

import static org.junit.Assert.*;

public class LoginBruteforceDetectorTest {
	@Test
	public void detectIpTest() throws UnknownHostException {
		LoginBruteforceDetector d = new LoginBruteforceDetector(5, 3, 5000);

		Date base = new Date(new Date().getTime() - 10000);

		InetAddress ip1 = InetAddress.getByName("1.2.3.4");
		String account1 = "test1";
		String account2 = "test2";
		boolean alert = false;

		alert = d.check(base, ip1, account1, false);
		assertFalse(alert);

		alert = d.check(add(base, 1000), ip1, account1, false);
		assertFalse(alert);

		alert = d.check(add(base, 2000), ip1, account2, false);
		assertFalse(alert);

		alert = d.check(add(base, 3000), ip1, account2, false);
		assertFalse(alert);

		// alert here
		alert = d.check(add(base, 4000), ip1, account2, false);
		assertTrue(alert);
	}

	@Test
	public void detectAccountTest() throws UnknownHostException {
		LoginBruteforceDetector d = new LoginBruteforceDetector(5, 3, 5000);

		Date base = new Date(new Date().getTime() - 10000);

		InetAddress ip1 = InetAddress.getByName("1.2.3.4");
		String account1 = "test1";
		boolean alert = false;

		alert = d.check(base, ip1, account1, false);
		assertFalse(alert);

		alert = d.check(add(base, 1000), ip1, account1, false);
		assertFalse(alert);

		// alert here
		alert = d.check(add(base, 2000), ip1, account1, false);
		assertTrue(alert);

		// should be reset
		alert = d.check(add(base, 3000), ip1, account1, false);
		assertFalse(alert);
	}

	@Test
	public void idleResetIpTest() throws UnknownHostException {
		LoginBruteforceDetector d = new LoginBruteforceDetector(5, 3, 5000);

		Date base = new Date(new Date().getTime() - 10000);

		InetAddress ip1 = InetAddress.getByName("1.2.3.4");
		String account1 = "test1";
		String account2 = "test2";
		boolean alert = false;

		alert = d.check(base, ip1, account1, false);
		assertFalse(alert);

		alert = d.check(add(base, 1000), ip1, account1, false);
		assertFalse(alert);

		alert = d.check(add(base, 2000), ip1, account2, false);
		assertFalse(alert);

		alert = d.check(add(base, 3000), ip1, account2, false);
		assertFalse(alert);

		// +8000 should alert
		alert = d.check(add(base, 8001), ip1, account2, false);
		assertFalse(alert);
	}

	private static Date add(Date base, int milliseconds) {
		return new Date(base.getTime() + milliseconds);
	}
}
