package kr.co.future.sslvpn.core.impl;

import java.net.InetAddress;
import java.util.Date;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

public class LoginBruteforceDetector {

	private int ipThreshold;
	private int accountThreshold;
	private int resetInterval;

	private ConcurrentMap<InetAddress, FailureStat> ipFails;
	private ConcurrentMap<String, FailureStat> accountFails;

	/**
	 * Construct bruteforce detector
	 * 
	 * @param ipThreshold
	 *            the threshold for successive ip fails
	 * @param accountThreshold
	 *            the threshold for successive account fails
	 * @param resetInterval
	 *            the reset interval in milliseconds
	 */
	public LoginBruteforceDetector(int ipThreshold, int accountThreshold, int resetInterval) {
		this.ipThreshold = ipThreshold;
		this.accountThreshold = accountThreshold;
		this.resetInterval = resetInterval;
		clear();
	}

	/**
	 * Add event and return alert status
	 * 
	 * @param date
	 *            the event date
	 * @param ip
	 *            the remote ip address
	 * @param account
	 *            the login name
	 * @param success
	 *            the login result
	 * @return is alert
	 */
	public boolean check(Date date, InetAddress ip, String account, boolean success) {
		boolean alert = false;

		FailureStat fs = new FailureStat(date);
		FailureStat old = ipFails.putIfAbsent(ip, fs);
		if (old != null)
			fs = old;

		if (success) {
			fs.lastOccurred = null;
			fs.count.set(0);
		} else {
			if (fs.lastOccurred != null && (date.getTime() - fs.lastOccurred.getTime()) > resetInterval)
				fs.count.set(0);

			fs.lastOccurred = date;
			if (ipThreshold <= fs.count.incrementAndGet()) {
				fs.count.set(0);
				alert = true;
			}
		}

		fs = new FailureStat(date);
		old = accountFails.putIfAbsent(account, fs);
		if (old != null)
			fs = old;

		if (success) {
			fs.lastOccurred = null;
			fs.count.set(0);
		} else {
			if (fs.lastOccurred != null && (date.getTime() - fs.lastOccurred.getTime()) > resetInterval)
				fs.count.set(0);

			fs.lastOccurred = date;
			if (accountThreshold <= fs.count.incrementAndGet()) {
				fs.count.set(0);
				alert = true;
			}
		}

		return alert;
	}

	/**
	 * Clear accumulated data if admin acknowledged
	 */
	public void clear() {
		ipFails = new ConcurrentHashMap<InetAddress, FailureStat>();
		accountFails = new ConcurrentHashMap<String, FailureStat>();
	}

	public int getIpThreshold() {
		return ipThreshold;
	}

	public void setIpThreshold(int ipThreshold) {
		this.ipThreshold = ipThreshold;
	}

	public int getAccountThreshold() {
		return accountThreshold;
	}

	public void setAccountThreshold(int accountThreshold) {
		this.accountThreshold = accountThreshold;
	}

	public int getResetInterval() {
		return resetInterval;
	}

	public void setResetInterval(int resetInterval) {
		this.resetInterval = resetInterval;
	}

	private static class FailureStat {
		public Date lastOccurred;
		public AtomicInteger count;

		public FailureStat(Date date) {
			lastOccurred = date;
			count = new AtomicInteger();
		}
	}
}
