package kr.co.future.sslvpn.core.impl;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigInteger;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPrivateKey;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;

import kr.co.future.sslvpn.core.Config;
import kr.co.future.sslvpn.core.InstallMonitor;
import kr.co.future.sslvpn.core.InstallerApi;
import kr.co.future.sslvpn.xtmconf.system.CommandUtil;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Invalidate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.apache.felix.ipojo.annotations.Validate;

import kr.co.future.api.KeyStoreManager;
import kr.co.future.ca.CertificateAuthority;
import kr.co.future.ca.CertificateAuthorityService;
import kr.co.future.ca.CertificateMetadata;
import kr.co.future.ca.CertificateRequest;
import kr.co.future.ca.util.CertificateExporter;
import kr.co.future.confdb.ConfigService;
import kr.co.future.dom.api.DefaultEntityEventListener;
import kr.co.future.dom.api.OrganizationApi;
import kr.co.future.dom.api.UserApi;
import kr.co.future.dom.model.Organization;
import kr.co.future.httpd.HttpConfiguration;
import kr.co.future.httpd.HttpServer;
import kr.co.future.httpd.HttpService;
import kr.co.future.rpc.RpcAgent;
import kr.co.future.rpc.RpcBindingProperties;
import kr.co.future.sslvpn.core.impl.InitialDomSchema;
import kr.co.future.sslvpn.core.impl.InitialFrodoSchema;
import kr.co.future.sslvpn.core.impl.InstallerApiImpl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(name = "frodo-installer")
@Provides
public class InstallerApiImpl implements InstallerApi {
	private final Logger logger = LoggerFactory.getLogger(InstallerApiImpl.class.getName());

	@Requires
	private ConfigService conf;

	@Requires
	private OrganizationApi orgApi;

	@Requires
	private UserApi domUserApi;

	@Requires
	private CertificateAuthorityService ca;

	@Requires
	private HttpService httpd;

	@Requires
	private RpcAgent rpcAgent;

	@Requires
	private KeyStoreManager ksm;

    @Requires
    private InitialDomSchema initialDomSchema;

	private DefaultEntityEventListener<Organization> orgUnitEventListner = new DefaultEntityEventListener<Organization>() {
		@Override
		public void entityAdded(String domain, Organization obj, Object state) {
			if (obj.getParameters() == null)
				return;

			updateSshTrustHosts("dom.admin.trust_hosts", obj.getParameters().get("dom.admin.trust_hosts"));
		}

		@Override
		public void entityUpdated(String domain, Organization obj, Object state) {
			if (obj.getParameters() == null)
				return;

			updateSshTrustHosts("dom.admin.trust_hosts", obj.getParameters().get("dom.admin.trust_hosts"));
		}

		@Override
		public void entityRemoved(String domain, Organization obj, Object state) {
			updateSshTrustHosts("dom.admin.trust_hosts", null);
		}
	};

	@Validate
	public void start() {
		orgApi.addEntityEventListener(orgUnitEventListner);
	}

	@Invalidate
	public void stop() {
		if (orgUnitEventListner != null)
			orgApi.removeEntityEventListener(orgUnitEventListner);
	}

	@Override
	public void install(InstallMonitor monitor) throws Exception {
		registerKeyStores(ksm, monitor);

		if (ksm.getKeyStore("rpc-agent") == null) {
			monitor.println("rpc-agent keystore not found");
			return;
		}

		if (ksm.getKeyStore("rpc-ca") == null) {
			monitor.println("rpc-ca keystore not found");
			return;
		}

		// create dom db schema
		try {
            initialDomSchema.generate(conf, domUserApi);
			monitor.println("created dom schema");
		} catch (Exception e) {
			logger.error("frodo core: cannot create dom schema", e);
		}

		// create frodo db schema
		try {
			InitialFrodoSchema.generate(conf);
			monitor.println("created frodo schema");
		} catch (Exception e) {
			logger.error("frodo core: cannot create frodo schema", e);
		}

		if (orgApi.getOrganizationParameter("localhost", "max_sessions") == null)
			orgApi.setOrganizationParameter("localhost", "max_sessions", 1);

		// force salt length to 0
		if (domUserApi.getSaltLength("localhost") != 0)
			domUserApi.setSaltLength("localhost", 0);

		// check ca cert and priv key
		if (ca.getAuthority(Config.Cert.caCommonName) == null) {
			Date notBefore = new Date();
			Calendar c = Calendar.getInstance();
			c.setTime(notBefore);
			c.add(Calendar.DAY_OF_YEAR, 3650);
			Date notAfter = c.getTime();

			CertificateRequest req = new CertificateRequest();
			req.setIssuerDn(Config.Cert.caDistinguishedName);
			req.setSubjectDn(Config.Cert.caDistinguishedName);
			req.setSignatureAlgorithm("SHA512withRSA");
			req.setNotBefore(notBefore);
			req.setNotAfter(notAfter);
			req.setKeyPassword(Config.Cert.caKeystorePass);

			// key pair generation
			KeyPairGenerator keyPairGen = KeyPairGenerator.getInstance("RSA", "BC");
			KeyPair keyPair = keyPairGen.generateKeyPair();
			req.setKeyPair(keyPair);
			req.setIssuerKey(keyPair.getPrivate());

			req.setSerial(new BigInteger("1"));

			ca.createAuthority(Config.Cert.caCommonName, req);
			String dataDir = System.getProperty("kraken.data.dir");
			File caDir = new File(dataDir, "/kraken-ca/CA/local");
			caDir.mkdirs();

			CertificateAuthority authority = ca.getAuthority(Config.Cert.caCommonName);
			CertificateMetadata root = authority.getRootCertificate();
			X509Certificate rootCert = root.getCertificate();
			RSAPrivateKey privateKey = root.getPrivateKey(authority.getRootKeyPassword());

			File pem = new File(caDir, Config.Cert.caCommonName + ".pem");

			monitor.println("root ca cert has been generated");
			monitor.println(rootCert.toString());

			CertificateExporter.writePemFile(rootCert, privateKey, pem, false);
//			monitor.println("restart marlin");
//			CommandUtil.run("killall", "marlin");

			monitor.println("exported PEM file");
		}

		CertificateAuthority authority = ca.getAuthority(Config.Cert.caCommonName);
		if (authority.getCrlDistPoint() == null) {
			try {
				authority.setCrlDistPoint(new URL("https://localhost"));
				monitor.println("set crl dist point [https://localhost]");
			} catch (MalformedURLException e) {
			}
		}

		// open web port
		try {
			InetSocketAddress port4502 = new InetSocketAddress(4502);
			InetSocketAddress port443 = new InetSocketAddress(443);
			InetSocketAddress port80 = new InetSocketAddress(80);

			if (httpd.getServer(port4502) == null) {
				HttpConfiguration conf4502 = new HttpConfiguration(port4502);
				conf4502.setDefaultHttpContext("webconsole");

				HttpServer server = httpd.createServer(conf4502);
				server.open();
				monitor.println("port 4502 opened");
			}

			if (httpd.getServer(port443) == null) {
				HttpConfiguration conf443 = new HttpConfiguration(port443, "rpc-agent", "rpc-ca");
				conf443.setDefaultHttpContext("frodo");
				conf443.setIdleTimeout(120);

				HttpServer server = httpd.createServer(conf443);
				server.open();
				monitor.println("port 443 opened");
			}

			if (httpd.getServer(port80) == null) {
				HttpConfiguration conf80 = new HttpConfiguration(port80);
				conf80.setDefaultHttpContext("frodo");
				conf80.setIdleTimeout(120);

				HttpServer server = httpd.createServer(conf80);
				server.open();
				monitor.println("port 80 opened");
			}
		} catch (Exception e) {
			logger.error("frodo core: cannot open websocket server", e);
		}

		openRpcPort(rpcAgent);
		updateSshTrustHosts("dom.admin.trust_hosts", orgApi.getOrganizationParameter("localhost", "dom.admin.trust_hosts"));
	}

	private void registerKeyStores(KeyStoreManager ksm, InstallMonitor monitor) {
		Collection<String> keyStoreNames = ksm.getKeyStoreNames();
		if (!keyStoreNames.contains("rpc-ca")) {
			try {
				ksm.registerKeyStore("rpc-ca", "JKS", "${kraken.cert.dir}/CA.jks", "123456".toCharArray());
				monitor.println("registered rpc-ca keystore");
			} catch (Exception e) {
				monitor.println("cannot register rpc-ca keystore");
				logger.warn("frodo core: cannot register CA.jks keystore", e);
			}
		}

		if (!keyStoreNames.contains("rpc-agent")) {
			try {
				ksm.registerKeyStore("rpc-agent", "PKCS12", "${kraken.cert.dir}/sslplus.pfx", "1234".toCharArray());
				monitor.println("registered rpc-agent keystore");
			} catch (Exception e) {
				monitor.println("cannot register rpc-agent keystore");
				logger.warn("frodo core: cannot register sslplus.pfx keystore", e);
			}
		}

	}

	private void openRpcPort(RpcAgent rpcAgent) {
		for (RpcBindingProperties r : rpcAgent.getBindings())
			if (r.getPort() == 7140)
				return;

		RpcBindingProperties prop = new RpcBindingProperties("0.0.0.0", 7140, "rpc-agent", "rpc-ca");
		rpcAgent.open(prop);
	}

	private void updateSshTrustHosts(String key, Object value) {
		// orgApi.getOrganizationParameter("localhost",
		// "dom.admin.trust_hosts");
		File hostsAllow = new File("/etc/hosts.allow");
		File hostsDeny = new File("/etc/hosts.deny");

		if (value == null || value.toString().trim().isEmpty()) {
			hostsAllow.delete();
			hostsDeny.delete();
			return;
		}

		BufferedWriter allowWriter = null;
		BufferedWriter denyWriter = null;
		try {
			if (!hostsAllow.exists())
				hostsAllow.createNewFile();
			if (!hostsDeny.exists())
				hostsDeny.createNewFile();

			allowWriter = new BufferedWriter(new FileWriter(hostsAllow, false));
			denyWriter = new BufferedWriter(new FileWriter(hostsDeny, false));

			denyWriter.write("SSHD: ALL");
			allowWriter.write("SSHD: " + value.toString().replaceAll(",", " "));

			denyWriter.flush();
			allowWriter.flush();
			logger.info("frodo core: update ssh trust hosts [{}]", value);
		} catch (IOException e) {
			logger.error("frodo core: cannot write ssh trust hosts file", e);
		} finally {
			if (allowWriter != null)
				try {
					allowWriter.close();
				} catch (IOException e) {
				}
			if (denyWriter != null)
				try {
					denyWriter.close();
				} catch (IOException e) {
				}
		}

	}
}
