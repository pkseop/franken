package kr.co.future.sslvpn.core.impl;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Provides;
import kr.co.future.dom.api.OtpApi;

@Component(name = "frodo-otp-api")
@Provides
public class OtpApiImpl implements OtpApi {
	@Override
	public String getOtpValue(String seed) {
		String time = Long.toString(System.currentTimeMillis() / 60000L);
		long otp = sha2Long(hash(seed)) + sha2Long(hash(time));
		otp /= otp % 1000 + 1;
		otp %= 1000000;
		return String.format("%06d", otp);
	}

	public static String hash(String text) {
		try {
			MessageDigest md = MessageDigest.getInstance("SHA-256");
			md.update(text.getBytes("utf-8"), 0, text.length());
			byte[] sha256hash = md.digest();
			return convertToHex(sha256hash);
		} catch (UnsupportedEncodingException e) {
			// ignore
		} catch (NoSuchAlgorithmException e) {
			// ignore
		}

		return null;
	}

	private static String convertToHex(byte[] data) {
		StringBuffer buf = new StringBuffer();
		for (int i = 0; i < data.length; i++) {
			int halfbyte = (data[i] >>> 4) & 0x0F;
			int two_halfs = 0;
			do {
				if ((0 <= halfbyte) && (halfbyte <= 9))
					buf.append((char) ('0' + halfbyte));
				else
					buf.append((char) ('a' + (halfbyte - 10)));
				halfbyte = data[i] & 0x0F;
			} while (two_halfs++ < 1);
		}
		return buf.toString();
	}

	private static long sha2Long(String hashed) {
		long l = 0;
		for (int i = 0; i < 4; i++)
			l += Long.parseLong(hashed.substring(i * 10, i * 10 + 10), 16);
		return l;
	}
}
