package kr.co.future.sslvpn.core;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.NoSuchProviderException;
import java.security.cert.CRLException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509CRL;
import java.util.Hashtable;

import javax.naming.Context;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import javax.naming.directory.SearchResult;

import kr.co.future.sslvpn.core.CrlValidator;
import kr.co.future.sslvpn.xtmconf.SSLTrustHelper;
import kr.co.future.sslvpn.core.impl.CrlConnectionErrorException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CrlValidator {

	public static boolean isRevoked(String url, BigInteger serial) throws CrlConnectionErrorException {
		try {
			Logger logger = LoggerFactory.getLogger(CrlValidator.class.getName());
			X509CRL crl = null;
			if (url.startsWith("ldap://")) {
				if (!url.contains("?certificateRevocationList"))
					url = url + "?certificateRevocationList";

				logger.trace("frodo core: checking ldap crl [{}]", url);
				crl = getLdapCrl(url);
			} else if (url.startsWith("http://") || url.startsWith("https://")) {
				// 64bit의 문제인지 간혹 localhost 로 호출하는 url connection 에 지연이 발생되어 ip로 replace하도록 수정처리 
				if (url.contains("localhost")){
					url = url.replaceAll("localhost", "127\\.0\\.0\\.1");
				}
				logger.trace("frodo core: checking http crl [{}]", url);
				crl = getHttpCrl(url);
			}

			// if crl cannot be fetched (e.g. network problem), treat it as
			// revoked
			if (crl == null) {
				logger.warn("frodo core: cannot fetch crl [{}, serial {}]", url, serial.toString());
				return true;
			}

			return crl.getRevokedCertificate(serial) != null;
		} catch (javax.naming.CommunicationException e) {
			throw new CrlConnectionErrorException(e);
		} catch (Exception e) {
			throw new IllegalStateException(e);
		}
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static X509CRL getLdapCrl(String url) throws NamingException, CertificateException, CRLException,
			NoSuchProviderException {
		Hashtable env = new Hashtable();
		env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
		env.put("com.sun.jndi.ldap.connect.timeout", "5000");
		env.put("com.sun.jndi.ldap.read.timeout", "5000");
		env.put(Context.PROVIDER_URL, url);
		DirContext dir = new InitialDirContext(env);

		NamingEnumeration it = dir.search(url, "(objectClass=*)", null);

		while ((it != null) && (it.hasMore())) {
			SearchResult result = (SearchResult) it.next();
			Attributes attrs = result.getAttributes();
			if (attrs == null)
				continue;

			for (NamingEnumeration e = attrs.getAll(); e.hasMore();) {
				Attribute attr = (Attribute) e.next();
				String id = attr.getID();
				if (!id.startsWith("certificateRevocationList"))
					continue;

				CertificateFactory cf = CertificateFactory.getInstance("X.509", "BC");
				return (X509CRL) cf.generateCRL(new ByteArrayInputStream((byte[]) attr.getAll().next()));
			}
		}

		return null;
	}

	private static X509CRL getHttpCrl(String url) throws MalformedURLException, IOException, CertificateException, CRLException {
		URL u = new URL(url);
		SSLTrustHelper.trustAll();
		HttpURLConnection con = (HttpURLConnection) u.openConnection();
		con.setConnectTimeout(5000);
		con.setReadTimeout(5000);

		ByteArrayOutputStream os = new ByteArrayOutputStream();
		InputStream is = con.getInputStream();
		try {
			byte[] buf = new byte[8096];
			while (true) {
				int readBytes = is.read(buf);
				if (readBytes <= 0)
					break;

				os.write(buf, 0, readBytes);
			}
		} finally {
			is.close();
		}

		CertificateFactory cf = CertificateFactory.getInstance("X.509");
		return (X509CRL) cf.generateCRL(new ByteArrayInputStream(os.toByteArray()));
	}
}
