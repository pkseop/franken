package kr.co.future.sslvpn.core.servlet;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import kr.co.future.sslvpn.core.AuthCode;
import kr.co.future.sslvpn.core.AuthService;
import kr.co.future.sslvpn.core.CertAndPrivKey;
import kr.co.future.sslvpn.core.Config;
import kr.co.future.sslvpn.core.servlet.CertCenterServlet;
import kr.co.future.sslvpn.model.Cert;
import kr.co.future.sslvpn.model.UserExtension;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Invalidate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.apache.felix.ipojo.annotations.Validate;
import org.json.JSONConverter;

import kr.co.future.ca.CertificateAuthority;
import kr.co.future.ca.CertificateAuthorityService;
import kr.co.future.ca.CertificateMetadata;
import kr.co.future.codec.Base64;
import kr.co.future.confdb.ConfigDatabase;
import kr.co.future.confdb.ConfigService;
import kr.co.future.confdb.Predicates;
import kr.co.future.cron.PeriodicJob;
import kr.co.future.dom.api.UserApi;
import kr.co.future.dom.model.User;
import kr.co.future.httpd.HttpContext;
import kr.co.future.httpd.HttpService;
import kr.co.future.sslvpn.core.CertGenerator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(name = "frodo-cert-center-servlet")
@Provides
@PeriodicJob("* * * * *")
public class CertCenterServlet extends HttpServlet implements Runnable {
	private static final long serialVersionUID = 1L;
	private final Logger logger = LoggerFactory.getLogger(CertCenterServlet.class.getName());

	@Requires
	private HttpService httpd;

	@Requires
	private AuthService auth;

	@Requires
	private CertificateAuthorityService ca;

	@Requires
	private UserApi domUserApi;

	@Requires
	private ConfigService conf;
	
	@Requires
	private  kr.co.future.sslvpn.model.api.UserApi userApi;

	// session key to login name mappings
	private ConcurrentHashMap<String, Session> sessions = new ConcurrentHashMap<String, Session>();

	@Validate
	public void start() {
		HttpContext ctx = httpd.ensureContext("frodo");
		ctx.addServlet("certcenter", this, "/certcenter/*");
	}

	@Invalidate
	public void stop() {
		if (httpd != null) {
			HttpContext ctx = httpd.ensureContext("frodo");
			ctx.removeServlet("certcenter");
		}
	}

	@Override
	public void run() {
		long now = new Date().getTime();

		for (String sessionKey : sessions.keySet()) {
			Session session = sessions.get(sessionKey);
			long accessTime = session.getAccessDate().getTime();

			if (now - accessTime > 10 * 60 * 1000)
				sessions.remove(sessionKey);
		}
	}

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		resp.setContentType("text/json;charset=utf-8");
		PrintWriter out = null;

		if (req.getPathInfo() == null) {
			resp.sendError(404);
			return;
		}

		try {
			if (req.getPathInfo().equals("/login")) {
				out = resp.getWriter();
				String loginName = req.getParameter("login_name");
				String password = req.getParameter("password");
				String idn = req.getParameter("idn");

				if (loginName == null) {
					logger.trace("frodo core: cert center login, null login name");
					resp.sendError(400);
					return;
				}

				if (password == null) {
					logger.trace("frodo core: cert center login, null password");
					resp.sendError(400);
					return;
				}

				Map<String, Object> result = login(loginName, password);
				if(idn != null && ((Integer)result.get("auth_code")) == 0) {
					AuthCode authCode = idnCheck(loginName, idn);
					if(authCode == null) {
						resp.sendError(500);
						return;
					}
					result.put("auth_code", authCode.getCode());
				}
				
				out.write(JSONConverter.jsonize(result));
				out.close();
				return;

			} else if (req.getPathInfo().equals("/logout")) {
				String sessionKey = req.getParameter("session_key");

				if (sessionKey == null) {
					logger.error("frodo core: cert center logout, null session key");
					resp.sendError(400);
					return;
				}

				logout(sessionKey);
				out = resp.getWriter();
				out.write("{}");
				out.close();
				return;

			} else if (req.getPathInfo().equals("/issue_cert")) {
				String sessionKey = req.getParameter("session_key");
				String keyPassword = req.getParameter("key_password");
				String fileType = req.getParameter("file_type");

				Session session = checkLogin(sessionKey);
				if (session == null) {
					resp.sendError(401);
					return;
				}

				if (keyPassword == null) {
					logger.trace("frodo core: cert center issue cert, null key pass");
					resp.sendError(400);
					return;
				}

				out = resp.getWriter();
				Map<String, Object> cert = issueCert(session, keyPassword, fileType);

				out.write(JSONConverter.jsonize(cert));
				return;
			} else if (req.getPathInfo().equals("/download_cert")) {
				String sessionKey = req.getParameter("session_key");
				String fileType = req.getParameter("file_type");

				Session session = checkLogin(sessionKey);
				if (session == null) {
					resp.sendError(401);
					return;
				}

				out = resp.getWriter();
				Map<String, Object> cert = downloadCert(session, fileType);
				out.write(JSONConverter.jsonize(cert));
				return;

			}
		} catch (Exception e) {
			logger.error("frodo core: cert center servlet error", e);
			resp.sendError(500);
		} finally {
			if (out != null)
				out.close();
		}
	}

	private Map<String, Object> downloadCert(Session session, String fileType) throws Exception {
		session.setAccessDate(new Date());
		Map<String, Object> m = new HashMap<String, Object>();
		CertificateAuthority authority = ca.getAuthority(Config.Cert.caCommonName);
		if (authority == null) {
			m.put("result", "authority-not-found");
			return m;
		}

		ConfigDatabase db = conf.ensureDatabase("frodo");
		kr.co.future.confdb.Config c = db.findOne(Cert.class, Predicates.field("login_name", session.getLoginName()));

		if (c == null) {
			m.put("result", "not-found");
			return m;
		}

		Cert cert = c.getDocument(Cert.class);

		if (fileType.equals("pkcs12")) {
			CertificateMetadata cm = authority.findCertificate("serial", cert.getSerial());
			return serializePfx(authority, cm);
		}

		return m;
	}

	private Map<String, Object> issueCert(Session session, String keyPass, String fileType) throws Exception {
		session.setAccessDate(new Date());
		CertAndPrivKey cert = CertGenerator.generate(ca, session.getLoginName(), keyPass, conf);

		if (fileType != null && fileType.equals("pkcs12")) {
			CertificateAuthority authority = ca.getAuthority(Config.Cert.caCommonName);
			if (authority == null) {
				Map<String, Object> m = new HashMap<String, Object>();
				m.put("result", "authority-not-found");
				return m;
			}

			return serializePfx(authority, cert.getMetadata());
		}

		Map<String, Object> m = new HashMap<String, Object>();
		m.put("ca", new String(Base64.encode(cert.getLocalCACert())));
		m.put("private", new String(Base64.encode(cert.getPkcs8PrivKey())));
		m.put("public", new String(Base64.encode(cert.getX509Certificate())));
		return m;
	}

	private Map<String, Object> serializePfx(CertificateAuthority authority, CertificateMetadata cm) throws Exception {
		X509Certificate rootCert = authority.getRootCertificate().getCertificate();
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ssZ");
		Map<String, Object> m = new HashMap<String, Object>();
		m.put("result", "success");
		m.put("issued_at", dateFormat.format(cm.getIssuedDate()));
		m.put("not_after", dateFormat.format(cm.getNotAfter()));
		m.put("not_before", dateFormat.format(cm.getNotBefore()));
		m.put("serial", cm.getSerial());
		m.put("subject_dn", cm.getSubjectDn());
		m.put("ca_binary", new String(Base64.encode(rootCert.getEncoded())));
		m.put("pfx_binary", new String(Base64.encode(cm.getBinary())));
		return m;
	}

	private Map<String, Object> login(String loginName, String password) {
		AuthCode authCode = auth.checkPassword(loginName, password);
		Map<String, Object> m = new HashMap<String, Object>();
		m.put("auth_code", authCode.getCode());
		m.put("login_name", loginName);

		if (authCode.getCode() == 0) {
			User user = domUserApi.getUser("localhost", loginName);
			String sessionKey = UUID.randomUUID().toString();
			m.put("name", user.getName());
			m.put("session_key", sessionKey);

			Session old = sessions.putIfAbsent(sessionKey, new Session(loginName, sessionKey, new Date()));

			if (old != null) {
				logger.warn("frodo core: duplicate session key, login_name=[{}]", loginName);
			}
		}

		return m;
	}

	private void logout(String sessionKey) {
		if (sessionKey == null) {
			logger.trace("frodo core: cert center logout, null session key");
			return;
		}

		sessions.remove(sessionKey);
	}

	private Session checkLogin(String sessionKey) {
		if (sessionKey == null) {
			logger.error("frodo core: cert center logout, null session key");
			return null;
		}

		return sessions.get(sessionKey);
	}

	private class Session {
		private String loginName;
		private String sessionKey;
		private Date accessDate;

		public Session(String loginName, String sessionKey, Date accessDate) {
			this.loginName = loginName;
			this.sessionKey = sessionKey;
			this.accessDate = accessDate;
		}

		public String getLoginName() {
			return loginName;
		}

		public Date getAccessDate() {
			return accessDate;
		}

		public void setAccessDate(Date accessDate) {
			this.accessDate = accessDate;
		}

		@Override
		public String toString() {
			return "Session [loginName=" + loginName + ", sessionKey=" + sessionKey + ", accessDate=" + accessDate + "]";
		}
	}
	
	private AuthCode idnCheck(String loginName, String idn) {
		User user = domUserApi.findUser("localhost", loginName);
		UserExtension ext = userApi.getUserExtension(user);
		
		if (ext == null || ext.getIdnHash() == null) {
			logger.error("frodo core: IDN hash not found for user [{}]", loginName);
			return AuthCode.IdnNotFound;
		}
		
		String salt = ext.getSalt();
		if (salt == null)
			salt = loginName;

		try {
			MessageDigest md = MessageDigest.getInstance("SHA1");
			md.update(salt.getBytes("utf-8"));
			md.update(idn.getBytes());
			String idnHash = toHex(md.digest());
			
			if (!ext.getIdnHash().equals(idnHash)) {
				logger.error("frodo core: IDN hash mismatch [{}] != [{}]", idnHash, ext.getIdnHash());
				return AuthCode.NpkiIdnFail;
			}
		} catch (NoSuchAlgorithmException e) {
			logger.error("frodo core: generate [{}]'s idn hash value error", loginName);
			return null;
		} catch (UnsupportedEncodingException e) {
			logger.error("frodo core: generate [{}]'s idn hash value error", loginName);
			return null;
		}
		
		return AuthCode.CertSuccess;
	}
	
	private static String toHex(byte[] b) {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < b.length; i++)
			sb.append(String.format("%02x", b[i]));

		return sb.toString();
	}
}
