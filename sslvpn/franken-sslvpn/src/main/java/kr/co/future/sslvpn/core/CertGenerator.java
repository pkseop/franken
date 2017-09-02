package kr.co.future.sslvpn.core;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.security.AlgorithmParameters;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPrivateKey;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Random;

import javax.crypto.Cipher;
import javax.crypto.EncryptedPrivateKeyInfo;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.PBEParameterSpec;

import kr.co.future.sslvpn.core.CertAndPrivKey;
import kr.co.future.sslvpn.core.CertGenerator;
import kr.co.future.sslvpn.core.Config;
import kr.co.future.sslvpn.model.Cert;

import org.bouncycastle.openssl.PEMWriter;

import kr.co.future.ca.CertificateAuthority;
import kr.co.future.ca.CertificateAuthorityService;
import kr.co.future.ca.CertificateMetadata;
import kr.co.future.ca.CertificateRequest;
import kr.co.future.ca.RevocationReason;
import kr.co.future.confdb.ConfigDatabase;
import kr.co.future.confdb.ConfigService;
import kr.co.future.confdb.Predicates;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CertGenerator {
	public static CertAndPrivKey generate(CertificateAuthorityService ca, String loginName, String keypass, ConfigService conf)
			throws Exception {
		CertificateAuthority authority = ca.getAuthority(Config.Cert.caCommonName);
		if (authority == null)
			throw new IllegalStateException("CA authority not found: " + Config.Cert.caCommonName);

		SimpleDateFormat sf = new SimpleDateFormat("yyyyMMdd");
		String strNow = sf.format(new Date());
		
		Date notBefore = sf.parse(strNow);
		Calendar c = Calendar.getInstance();
		c.setTime(notBefore);
		c.add(Calendar.DAY_OF_YEAR, 365);
		c.add(Calendar.HOUR_OF_DAY, 23);
		c.add(Calendar.MINUTE, 59);
		c.add(Calendar.SECOND, 59);
		Date notAfter = c.getTime();

		String certDN = String.format("CN=%s", loginName);

		CertificateRequest req = new CertificateRequest();
		req.setIssuerDn(authority.getRootCertificate().getSubjectDn());
		req.setSubjectDn(certDN);
		req.setSignatureAlgorithm("SHA512withRSA");
		req.setNotBefore(notBefore);
		req.setNotAfter(notAfter);
		req.setKeyPassword(keypass);

		KeyPairGenerator keyPairGen = KeyPairGenerator.getInstance("RSA", "BC");
		KeyPair keyPair = keyPairGen.generateKeyPair();
		req.setKeyPair(keyPair);

		ConfigDatabase db = conf.ensureDatabase("frodo");
		kr.co.future.confdb.Config oldDbConfig = db.findOne(Cert.class, Predicates.field("login_name", loginName));

		CertificateMetadata cert = authority.issueCertificate(req);

		CertAndPrivKey ret = new CertAndPrivKey();
		ret.setPkcs8PrivKey(getEncodedEncryptedPrivateKey(cert.getPrivateKey(req.getKeyPassword()), req.getKeyPassword()));
		ret.setX509Certificate(cert.getCertificate(keypass).getEncoded());
		ret.setLocalCACert(getCertificateAsPEM(authority.getRootCertificate().getCertificate()));
		ret.setMetadata(cert);

		if (oldDbConfig != null)
			revokeCert(ca, loginName, cert.getSerial(), oldDbConfig, conf);

		return ret;
	}

	private static void revokeCert(CertificateAuthorityService ca, String loginName, String newSerial,
			kr.co.future.confdb.Config oldConfig, ConfigService conf) {
		Logger logger = LoggerFactory.getLogger(CertGenerator.class.getName());

		String oldSerial = oldConfig.getDocument(Cert.class).getSerial();
		CertificateAuthority authority = ca.getAuthority(Config.Cert.caCommonName);
		CertificateMetadata cm = authority.findCertificate("serial", oldSerial);
		try {
			if (cm != null) {
				authority.revoke(cm, RevocationReason.Superseded);
				logger.trace("frodo core: revoked certificate, login_name [{}], serial [{}]", loginName, oldSerial);
			} else {
				conf.ensureDatabase("frodo").remove(oldConfig, true);
				logger.trace("frodo core: cannot find old certificate, login_name [{}], serial [{}]", loginName, oldSerial);
			}
		} catch (IllegalStateException e) {
			if (e.getMessage().contains("already revoked")) {
				logger.trace("frodo core: already revoked certificate, login_name [{}], serial [{}]", loginName, oldSerial);
				conf.ensureDatabase("frodo").remove(oldConfig);
			}
		}

	}

	private static byte[] getCertificateAsPEM(X509Certificate certificate) throws IOException {
		ByteArrayOutputStream byteStream = new ByteArrayOutputStream(8192);
		PEMWriter writer = new PEMWriter(new PrintWriter(byteStream), "PC");
		try {
			writer.writeObject(certificate);
			writer.flush();
		} finally {
			writer.close();
		}

		return byteStream.toByteArray();
	}

	private static byte[] getEncodedEncryptedPrivateKey(RSAPrivateKey privKey, String pkPassword) throws Exception {
		// code from
		// http://stackoverflow.com/questions/5127379/how-to-generate-a-rsa-keypair-with-a-privatekey-encrypted-with-password
		byte[] encodedPrivKey = privKey.getEncoded();
		int hashIterationCount = 20;
		Random random = new Random();
		byte[] salt = new byte[8];
		random.nextBytes(salt);

		PBEParameterSpec pbeParamSpec = new PBEParameterSpec(salt, hashIterationCount);
		PBEKeySpec pbeKeySpec = new PBEKeySpec(pkPassword.toCharArray());
		String pbeAlgorithm = "PBEWithSHA1AndDESede";
		SecretKeyFactory keyFac = SecretKeyFactory.getInstance(pbeAlgorithm);
		SecretKey pbeKey = keyFac.generateSecret(pbeKeySpec);

		Cipher pbeCipher = Cipher.getInstance(pbeAlgorithm);

		pbeCipher.init(Cipher.ENCRYPT_MODE, pbeKey, pbeParamSpec);

		byte[] ciphertext = pbeCipher.doFinal(encodedPrivKey);

		AlgorithmParameters algparams = AlgorithmParameters.getInstance(pbeAlgorithm);
		algparams.init(pbeParamSpec);
		EncryptedPrivateKeyInfo encInfo = new EncryptedPrivateKeyInfo(algparams, ciphertext);

		return encInfo.getEncoded();
	}
}
