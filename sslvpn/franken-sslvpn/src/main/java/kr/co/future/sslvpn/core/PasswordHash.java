package kr.co.future.sslvpn.core;

import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import kr.co.future.codec.Base64;
import kr.co.future.sslvpn.core.PasswordHash;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PasswordHash {

	private final static Logger logger = LoggerFactory.getLogger(PasswordHash.class.getName());

	public static String makeHash(String algorithm, String passwd, String encoding) {
		return makeHash(algorithm, passwd, encoding, "utf-8");
	}

	public static String makeHash(String algorithm, String passwd, String encoding, String charset) {
		if (encoding == null)
			encoding = "hex";
		if (charset == null)
			charset = "utf-8";

		byte[] byteArray = passwd.getBytes(Charset.forName(charset));
		MessageDigest md = null;

		try {
			md = MessageDigest.getInstance(algorithm);
		} catch (NoSuchAlgorithmException e) {
			logger.error("frodo core: sql auth, no search algorithm", e);
		}

		if (md == null)
			return null;

		md.reset();
		md.update(byteArray);

		byte[] digest = md.digest();

		if (encoding.equals("base64")) {
			return new String(Base64.encode(digest));
		}

		StringBuffer sb = new StringBuffer();

		for (int i = 0; i < digest.length; i++) {
			sb.append(String.format("%02x", digest[i]));
		}

		return sb.toString();
	}
}
