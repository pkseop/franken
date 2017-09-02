package kr.co.future.sslvpn.core.impl;

import java.util.Random;

public class SaltGenerator {

	private static final char[] chars = new char[62];
	static {
		int i = 0;
		char c = 'a';
		for (; i < 26; i++)
			chars[i] = c++;
		c = 'A';
		for (; i < 52; i++)
			chars[i] = c++;
		c = '0';
		for (; i < 62; i++)
			chars[i] = c++;
	}

	private SaltGenerator() {
	}

	public static String createSalt(int len) {
		Random random = new Random();
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < len; i++)
			sb.append(chars[random.nextInt(62)]);
		return sb.toString();
	}
}
