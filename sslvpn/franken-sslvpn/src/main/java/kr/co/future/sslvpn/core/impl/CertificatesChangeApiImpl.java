package kr.co.future.sslvpn.core.impl;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import kr.co.future.sslvpn.core.CertificatesChangeApi;
import kr.co.future.sslvpn.xtmconf.system.CommandUtil;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.bouncycastle.util.encoders.Base64;

import kr.co.future.api.KeyStoreManager;
import kr.co.future.httpd.HttpConfiguration;
import kr.co.future.httpd.HttpServer;
import kr.co.future.httpd.HttpService;
import kr.co.future.httpd.VirtualHost;
import kr.co.future.sslvpn.core.impl.CertificatesChangeApiImpl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(name = "frodo-certificates-api")
@Provides
public class CertificatesChangeApiImpl implements CertificatesChangeApi {
	private Logger logger = LoggerFactory.getLogger(CertificatesChangeApiImpl.class.getName());
	private static final String CONF_DIR = "/utm/log/cert/";
	private static final String KEYTOOL_DIR = "/usr/sslplus/jre/bin/";
	private static final String DEST_PW = "123456";
	private static final int DEFAULT_IDLE_TIMEOUT = 120;
	private static final String LISTEN_ADDR = "0.0.0.0";

	@Requires
	private KeyStoreManager manager;

	@Requires
	private HttpService httpd;

	@Override
	public void changeCertificates(List<Map<String, Object>> certFiles, String password) {

		Map<String, Object> certFile = certFiles.get(0);
		String fileName = ((String) certFile.get("file_name")).toLowerCase();

		if (certFiles.size() == 1) {
			byte[] data = Base64.decode(certFile.get("data").toString().getBytes());
			handleCertInfo(DEST_PW.toCharArray(), fileName, data);
		} else
			convertToJKS(certFiles, password);

	}

	@Override
	public void rollBack() {

		try {
			CommandUtil.run(new File("/"), "rm", "-rf", CONF_DIR + "*.cer");
			CommandUtil.run(new File("/"), "rm", "-rf", CONF_DIR + "*.crt");
			CommandUtil.run(new File("/"), "rm", "-rf", CONF_DIR + "*.pfx");
			CommandUtil.run(new File("/"), "rm", "-rf", CONF_DIR + "*.jks");
		} catch (Throwable t) {
			logger.error("frodo core: rollback error [{}]", t.getMessage());
		}

		reInitCertificates("rpc-agent", "rpc-ca");

	}

	private void convertToJKS(List<Map<String, Object>> certFiles, String password) {

		logger.debug("frodo core: run convert to jks");

		Map<String, Object> serverCert = null;
		Map<String, Object> intermediateCA = null;
		Map<String, Object> rootCA = null;

		for (Map<String, Object> certFile : certFiles) {
			if (certFile.get("use").equals("server"))
				serverCert = certFile;

			if (certFile.get("use").equals("intermediate"))
				intermediateCA = certFile;

			if (certFile.get("use").equals("root"))
				rootCA = certFile;
		}

		logger.debug("frodo core: assigned certificate in map");

		try {
			if (serverCert == null)
				throw new IllegalStateException("server certificates not found");

			if (rootCA == null)
				throw new IllegalStateException("root ca not found");

			convertToJKSUseKeytool(intermediateCA, rootCA, serverCert, password.toString());

			String alias = ((String) serverCert.get("file_name")).split("\\.")[0];
			String JksFileName = alias + ".jks";
			File cert = new File(CONF_DIR + JksFileName);

			byte[] data = getCertficateBytes(cert);

			handleCertInfo(DEST_PW.toCharArray(), JksFileName, data);

		} catch (IOException e) {
			throw new IllegalStateException("cannot convert to jks: " + e);
		}
	}

	private void convertToJKSUseKeytool(Map<String, Object> intermediateCA, Map<String, Object> rootCA,
			Map<String, Object> serverCert, String password) throws IOException {

		logger.debug("frodo core: convert to jks use keytool");

		try {

			saveCertFiles(intermediateCA, rootCA, serverCert);

			String fileName = (String) serverCert.get("file_name");
			String use = (String) serverCert.get("use");
			String alias = fileName.split("\\.")[0];

			CommandUtil.run(new File("/"), KEYTOOL_DIR + "keytool", "-importkeystore", "-srckeystore", CONF_DIR + fileName,
					"-srcstoretype", "pkcs12", "-destkeystore", CONF_DIR + alias + ".jks", "-deststoretype", "JKS", "-storepass",
					DEST_PW, "-srcstorepass", password, "-noprompt");

			logger.debug("frodo core: keytool import server certificate [{}]", fileName);

			if (intermediateCA != null) {
				fileName = (String) intermediateCA.get("file_name");
				use = (String) intermediateCA.get("use");

				CommandUtil.run(new File("/"), KEYTOOL_DIR + "keytool", "-import", "-trustcacerts", "-alias" + use + "-file",
						CONF_DIR + fileName, "-keystore", CONF_DIR + alias + ".jks", "-storepass" + DEST_PW + "-noprompt");

				logger.debug("frodo core: keytool import intermediate certificate [{}]", fileName);
			}

			fileName = (String) rootCA.get("file_name");
			use = (String) rootCA.get("use");

			CommandUtil.run(new File("/"), KEYTOOL_DIR + "keytool", "-import", "-trustcacerts", "-alias" + use + "-file",
					CONF_DIR + fileName, "-keystore", CONF_DIR + alias + ".jks", "-storepass" + DEST_PW + "-noprompt");

			logger.debug("frodo core: keytool import root certificate [{}]", fileName);
		} catch (Throwable t) {
			logger.error("frodo core: convert to jks use keytool error [{}]", t.getMessage());
		}
	}

	private byte[] getCertficateBytes(File cert) throws IOException {
		ByteArrayOutputStream baos = null;
		FileInputStream fis = null;
		try {
			fis = new FileInputStream(cert);
			baos = new ByteArrayOutputStream();

			byte[] buf = new byte[1024];
			int readLength = 0;
			while ((readLength = fis.read(buf)) != -1)
				baos.write(buf, 0, readLength);

			return baos.toByteArray();
		} catch (IOException e) {
			throw e;
		} finally {
			if (fis != null)
				try {
					fis.close();
				} catch (IOException e1) {
				}
			if (baos != null)
				try {
					baos.close();
				} catch (IOException e) {
				}
		}
	}

	private void handleCertInfo(char[] password, String fileName, byte[] data) {
		if (!fileName.endsWith(".jks"))
			throw new IllegalStateException("incorrect cert file type [" + fileName.split(".")[1] + "]");

		String certFilePath = CONF_DIR + fileName;
		String alias = fileName.split("\\.")[0];

		// /utm/log/cert/ 아래에 저장
		saveCertFile(data, certFilePath);

		// keystore에 등록
		registerKeyStore(password, certFilePath, alias);

		// ssl 포트 재 오픈
		reInitCertificates(alias, alias);
	}

	private void reInitCertificates(String alias, String trustAlias) {

		for (InetSocketAddress binding : httpd.getListenAddresses()) {
			HttpServer server = httpd.getServer(binding);
			HttpConfiguration config = server.getConfiguration();
			int port = config.getListenAddress().getPort();
			if (port == 443) {
				// 기존 포트 닫기
				InetSocketAddress newListen = closeSslPort(config);

				// 변경된 인증서로 등록한 키스토어로 ssl포트 열기
				openSslPort(alias, trustAlias, newListen);

				// 가상 호스트 사용시
				// addVirtualHost(newServer, config);

				// IdleTimeOUT 설정
				setIdleTimeout(port);

				break;
			}
		}
	}

	private void setIdleTimeout(int port) {
		try {
			InetAddress addr = InetAddress.getByName(LISTEN_ADDR);
			int idleTimeout = DEFAULT_IDLE_TIMEOUT;
			InetSocketAddress listenAddr = new InetSocketAddress(addr, port);
			HttpServer server = httpd.getServer(listenAddr);
			if (server == null) {
				logger.error("frodo core: http server not found");
				return;
			}
			HttpConfiguration config = server.getConfiguration();
			config.setIdleTimeout(idleTimeout);
			logger.debug("frodo core: set idletimeout [{}]", idleTimeout);
		} catch (UnknownHostException e) {
			logger.error("frodo core: invalid listen ip format", e);
		}
	}

	private void addVirtualHost(HttpServer server, HttpConfiguration config) {
		String hostNamePattern = ".*";
		VirtualHost v = new VirtualHost();
		v.setHttpContextName("frodo");
		v.setHostNames(Arrays.asList(hostNamePattern));
		server.addVirtualHost(v);
		logger.info("frodo core: added virtual host");
	}

	private HttpServer openSslPort(String alias, String trustAlias, InetSocketAddress newListen) {
		HttpConfiguration config;
		config = new HttpConfiguration(newListen, alias, trustAlias);
		config.setDefaultHttpContext("frodo");
		HttpServer server = httpd.createServer(config);
		try {
			server.open();
			logger.info("opened https server");
		} catch (Exception e) {
			logger.error("frodo core: cannot open ssl port");
		}

		return server;
	}

	private InetSocketAddress closeSslPort(HttpConfiguration config) {
		int port = config.getListenAddress().getPort();
		InetSocketAddress listen = new InetSocketAddress(port);
		if (port == 443) {
			try {
				httpd.removeServer(listen);
			} catch (Exception e) {
				logger.error("frodo core: cannot close port", e);
			}
		}
		return listen;
	}

	private void registerKeyStore(char[] password, String certFilePath, String alias) {
		try {
			if (manager.getKeyStore(alias) != null)
				manager.unregisterKeyStore(alias);

			manager.registerKeyStore(alias, "JKS", certFilePath, password);
		} catch (KeyStoreException e) {
			logger.warn("keystore.register: ", e);
		} catch (NoSuchAlgorithmException e) {
			logger.warn("keystore.register: ", e);
		} catch (CertificateException e) {
			logger.warn("keystore.register: ", e);
		} catch (IOException e) {
			logger.warn("keystore.register: ", e);
		}
	}

	private void saveCertFiles(Map<String, Object> intermediateCA, Map<String, Object> rootCA, Map<String, Object> serverCert) {
		List<Map<String, Object>> l = new ArrayList<Map<String, Object>>();
		l.add(serverCert);
		l.add(rootCA);
		if (intermediateCA != null)
			l.add(intermediateCA);

		for (Map<String, Object> certMap : l) {
			String fileName = (String) certMap.get("file_name");
			byte[] data = Base64.decode(certMap.get("data").toString().getBytes());
			saveCertFile(data, CONF_DIR + fileName);
		}
	}

	private void saveCertFile(byte[] data, String certFilePath) {
		File f = new File(certFilePath);
		FileOutputStream fos = null;
		try {
			fos = new FileOutputStream(f);
			fos.write(data);
		} catch (IOException e) {

		} finally {
			if (fos != null) {
				try {
					fos.close();
				} catch (IOException e) {
				}
			}
		}
	}

}
