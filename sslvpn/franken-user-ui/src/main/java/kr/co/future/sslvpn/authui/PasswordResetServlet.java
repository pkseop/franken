package kr.co.future.sslvpn.authui;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.net.URL;
import java.net.UnknownHostException;
import java.nio.charset.Charset;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMessage.RecipientType;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import kr.co.future.sslvpn.core.AuthService;
import kr.co.future.sslvpn.core.ExternalAuthService;
import kr.co.future.sslvpn.core.GlobalConfig;
import kr.co.future.sslvpn.core.GlobalConfigApi;
import kr.co.future.sslvpn.model.AccessGateway;
import kr.co.future.sslvpn.model.AccessProfile;
import kr.co.future.sslvpn.model.UserExtension;
import kr.co.future.sslvpn.model.AccessGateway.LdapMode;
import kr.co.future.sslvpn.model.api.AccessGatewayApi;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Invalidate;
import org.apache.felix.ipojo.annotations.Requires;
import org.apache.felix.ipojo.annotations.Validate;
import org.json.JSONConverter;
import org.json.JSONException;
import kr.co.future.confdb.Config;
import kr.co.future.confdb.ConfigDatabase;
import kr.co.future.confdb.ConfigService;
import kr.co.future.confdb.Predicates;
import kr.co.future.dom.api.UserApi;
import kr.co.future.dom.model.User;
import kr.co.future.httpd.HttpContext;
import kr.co.future.httpd.HttpService;
import kr.co.future.mail.MailerConfig;
import kr.co.future.mail.MailerRegistry;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(name = "frodo-passwd-reset-servlet")
public class PasswordResetServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;
	private final Logger logger = LoggerFactory.getLogger(PasswordResetServlet.class.getName());

	@Requires
	private ConfigService conf;

	@Requires
	private UserApi domUserApi;

	@Requires
	private MailerRegistry mailer;

	@Requires
	private HttpService httpd;

	@Requires
	private PasswordResetService reset;

	@Requires
	private AuthService auth;

	@Requires
	private ExternalAuthService externalService;

	@Requires
	private UserApi userApi;

	@Requires
	private AccessGatewayApi gwApi;
	
	@Requires
	private GlobalConfigApi configApi;

	private BundleContext bc;
	private ExecutorService threadPool;
	private File baseDir;

	public PasswordResetServlet(BundleContext bc) {
		this.bc = bc;
	}

	@Validate
	public void start() {
		HttpContext ctx = httpd.ensureContext("frodo");
		ctx.addServlet("reset", this, "/reset/*");
		threadPool = Executors.newCachedThreadPool();

		GlobalConfig config = configApi.getGlobalConfig();

		if (config != null) {
			String userUiPath = config.getUserUiPath();
			if (userUiPath != null && !userUiPath.trim().isEmpty()) {
				File webDir = new File(userUiPath);

				if (webDir != null && webDir.isDirectory())
					baseDir = webDir;
			}
		}
	}

	@Invalidate
	public void stop() {
		threadPool.shutdown();
		if (httpd != null) {
			HttpContext ctx = httpd.ensureContext("frodo");
			ctx.removeServlet("reset");
		}
	}

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		logger.trace("frodo user ui: >> http request [{}]", req.getPathInfo());

		// show reset page
		byte[] b = new byte[8096];
		InputStream is = null;
		try {
			if (baseDir == null) {
				URL url = bc.getBundle().getResource("/WEB-INF" + req.getPathInfo());

				if (url == null) {
					resp.sendError(404);
					return;
				}
				is = url.openStream();
				logger.trace("frodo auth api: open bundle resource [{}]", url);
			} else {
				File reqFile = new File(baseDir, req.getPathInfo());
				if (!reqFile.exists()) {
					resp.sendError(404);
					return;
				}

				is = new FileInputStream(reqFile);
				logger.trace("frodo auth api: open file [{}]", reqFile.getAbsolutePath());
			}

			ByteArrayOutputStream os = new ByteArrayOutputStream();

			while (true) {
				int read = is.read(b);
				if (read <= 0)
					break;
				os.write(b, 0, read);
			}

			if (req.getPathInfo().equals("/error.html")) {
				String s = os.toString("utf-8");
				String type = req.getParameter("type");
				String msg = "";
				if (type == null) {
				} else if (type.equals("no_token"))
					msg = "인증 토큰이 전달되지 않았습니다.";
				else if (type.equals("used_token"))
					msg = "이미 사용된 인증 토큰입니다.";
				else if (type.equals("expired_token"))
					msg = "만료된 인증 토큰입니다.";
				else if (type.equals("no_user"))
					msg = "사용자를 찾을 수 없습니다.";
				else if (type.equals("no_email"))
					msg = "이메일 주소가 없습니다.";
				else if (type.equals("password_not_match"))
					msg = "입력한 패스워드가 서로 일치하지 않습니다.";
				else if (type.equals("null_password"))
					msg = "재설정할 패스워드를 입력하여 주십시오.";
				else if (type.equals("null_old_password"))
					msg = "기존 패스워드를 입력하여 주십시오.";
				else if (type.equals("old_password_not_match"))
					msg = "기존 패스워드와 일치하지 않습니다.";
				else if (type.equals("can_not_reset"))
					msg = "패스워드를 변경할 수 없는 계정입니다.";
				else if (type.equals("internal_server_error"))
					msg = "패스워드 변경 중 알 수 없는 오류가 발생했습니다.";

				s = s.replaceAll("\\$type", msg);
				resp.getOutputStream().write(s.getBytes("utf-8"));
			} else if (req.getPathInfo().equals("/reset.html")) {
				String token = req.getParameter("token");

				ConfigDatabase db = conf.ensureDatabase("frodo");
				Config c = db.findOne(PasswordResetToken.class, Predicates.field("token", token));
				if (c == null) {
					resp.sendRedirect("/reset/error.html?type=no_token");
					return;
				}

				PasswordResetToken prt = c.getDocument(PasswordResetToken.class);
				String s = os.toString("utf-8");
				s = s.replaceAll("\\$login_name", prt.getLoginName());
				s = s.replaceAll("\\$token", token);
				resp.getOutputStream().write(s.getBytes("utf-8"));
			} else
				resp.getOutputStream().write(os.toByteArray());
		} finally {
			if (is != null)
				is.close();

			resp.getOutputStream().close();
			logger.trace("frodo user ui: << http request [{}]", req.getPathInfo());
		}
	}

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		logger.trace("frodo user ui: http post request [{}]", req.getPathInfo());

		if (req.getPathInfo().equals("/change")) {
			String result = getPasswordChangeResult(req);
			logger.trace("frodo user ui: change password result [{}]", result);

			if (result.equals("null_password") || result.equals("null_old_password") || result.equals("null_login_name")) {
				resp.sendError(401);
				return;
			}

			String type = req.getParameter("type");
			responseResult(resp, result, type);
			return;
		} else if (req.getPathInfo().equals("/reset")) {
			String result = getPasswordResetResult(req);
			logger.trace("frodo user ui: reset password result [{}]", result);

			String type = req.getParameter("type");
			responseResult(resp, result, type);
			return;
		} else if (req.getPathInfo().equals("/sendmail")) {
			// reset request (generate token)
			String type = req.getParameter("type");
			PrintWriter out = null;

			try {
				String result = sendMail(req);
				if (result.equals("no_mail_template") || result.equals("invalid_email_address")) {
					resp.sendError(500);
					return;
				}

				out = resp.getWriter();
				if (type != null && type.equals("json"))
					sendJsonResult(out, result);
				else if (!result.equals("ok")) {
					resp.sendRedirect("/reset/error.html?type=" + result);
				} else {
					out.println("ok");
				}

			} catch (JSONException e) {
				logger.error("frodo user ui: jsonize fail", e);
				return;
			} finally {
				if (out != null)
					out.close();
			}
		} else {
			logger.warn("frodo user ui: invalid request [{}]", req.getRequestURI());
			resp.sendError(500);
		}
	}

	private void responseResult(HttpServletResponse resp, String result, String type) throws IOException {
		if (type != null && type.equals("json")) {
			resp.setContentType("text/json;charset=utf-8");
			PrintWriter out = null;
			try {
				out = resp.getWriter();
				Map<String, String> m = new HashMap<String, String>();
				m.put("result", result);
				out.write(JSONConverter.jsonize(m));
				return;
			} catch (JSONException e) {
				logger.error("frodo user ui: jsonize fail, password reset result [{}]", result, e);
				resp.sendError(500);
				return;
			} finally {
				if (out != null)
					out.close();
			}
		}

		if (result.equals("success")) {
			resp.sendRedirect("/reset/complete.html");
			return;
		} else {
			resp.sendRedirect("/reset/error.html?type=" + result);
			return;
		}
	}

	private String sendMail(HttpServletRequest req) {
		String loginName = req.getParameter("login_name");
		if (loginName == null) {
			logger.warn("frodo user ui: login name is null");
			return "null_login_name";
		}

		MailerConfig c = mailer.getConfig("frodo");
		if (c == null) {
			logger.warn("frodo user ui: frodo mailer config not found");
			return "no_config";
		}

		User user = domUserApi.findUser("localhost", loginName);
		if (user == null) {
			logger.warn("frodo user ui: user [{}] not found", loginName);
			return "no_user";
		}

		if (user.getEmail() == null || user.getEmail().isEmpty()) {
			logger.warn("frodo user ui: user [{}] email not found", loginName);
			return "no_email";
		}

		// set guid token
		String token = UUID.randomUUID().toString();
		PasswordResetToken t = new PasswordResetToken(token, loginName);
		ConfigDatabase db = conf.ensureDatabase("frodo");
		db.add(t, "frodo-auth-ui", "generated token for " + loginName);

		MailTemplate mt = reset.getTemplate();

		if (mt == null) {
			logger.warn("frodo user ui: reset mail template not found");
			return "no_mail_template";
		}

		String body = getMailTemplate();

		if (body == null) {
			logger.warn("frodo user ui: frodo mailer config not found");
			return "no_config";
		}

		body = body.replaceAll("\\$host", req.getHeader("host"));
		body = body.replaceAll("\\$token", token);
		body = body.replaceAll("\\$login_name", loginName);
		body = body.replaceAll("\\$name", user.getName());

		Session session = mailer.getSession(c);
		MimeMessage msg = new MimeMessage(session);
		try {
			msg.setSentDate(new Date());
			msg.setFrom(new InternetAddress("admin@future.co.kr"));
			msg.setRecipient(RecipientType.TO, new InternetAddress(user.getEmail()));
			msg.setSubject(mt.getSubject(), "utf-8");
			msg.setContent(body, "text/html; charset=utf-8");

			// submit
			threadPool.submit(new MailTask(msg));
		} catch (Exception e) {
			logger.warn("frodo user ui: email address format error", e);
			return "invalid_email_address";
		}

		return "ok";
	}

	private String getPasswordResetResult(HttpServletRequest req) {
		String token = req.getParameter("token");
		String password = req.getParameter("password");
		String repassword = req.getParameter("repassword");

		if (password == null || repassword == null || password.isEmpty()) {
			logger.trace("frodo user ui: invalid reset password [{}]", password);
			return "null_password";
		}

		if (!password.equals(repassword))
			return "password_not_match";

		logger.trace("frodo user ui: request token [{}]", token);
		ConfigDatabase db = conf.ensureDatabase("frodo");
		Config c = db.findOne(PasswordResetToken.class, Predicates.field("token", token));

		if (c == null)
			return "no_token";

		PasswordResetToken prt = c.getDocument(PasswordResetToken.class);
		if (prt.isUsed())
			return "used_token";

		Date now = new Date();
		if (now.getTime() - prt.getCreated().getTime() > reset.getExpireInterval() * 1000)
			return "expired_token";

		User user = domUserApi.findUser("localhost", prt.getLoginName());
		if (user == null) {
			logger.error("frodo user ui: user not found for [{}]", prt.getLoginName());
			return "no_user";
		}

		prt.setUsed(true);
		db.update(c, prt);

		try {
			auth.changePassword(prt.getLoginName(), password);
		} catch (UnsupportedOperationException e) {
			return "not-supported";
		} catch (Throwable t) {
			logger.error("frodo user ui: cannot change password of " + user.getLoginName(), t);
			return "internal_server_error";
		}
		return "success";
	}

	private String getPasswordChangeResult(HttpServletRequest req) throws UnknownHostException {
		String loginName = req.getParameter("login_name");
		String newPassword = req.getParameter("password");
		String oldPassword = req.getParameter("old_password");
		
		logger.trace("frodo user ui: login_name: [{}]    password: [{}]    old_password: [{}]", new Object[]{loginName, newPassword, oldPassword});

		if (oldPassword == null || oldPassword.isEmpty()) {
			logger.error("frodo user ui: cannot change password, null old password");
			return "null_old_password";
		}

		if (newPassword == null || newPassword.isEmpty()) {
			logger.error("frodo user ui: cannot change password, null new password");
			return "null_password";
		}

		if (loginName == null) {
			logger.error("frodo user ui: cannot change password, null login name");
			return "null_login_name";
		}

		User user = domUserApi.findUser("localhost", loginName);

		if (user == null)
			return "no_user";

		int loginMethod = gwApi.getCurrentAccessGateway().getLoginMethod();
		AccessProfile profile = auth.determineProfile(loginName);

		if (profile.getLoginMethod() != null)
			loginMethod = profile.getLoginMethod();

		UserExtension.getPasswordResetable(user, loginMethod, externalService.isPasswordChangeSupported());
		if (!isResetable(user, profile, loginMethod))
			return "can_not_reset";

		String status = auth.checkPassword(loginName, oldPassword).getStatus();

		if (!(status.equals("success") || status.equals("password-expired")))
			return "old_password_not_match";

		try {
			auth.changePassword(loginName, newPassword);
		} catch (UnsupportedOperationException e) {
			return "not-supported";
		} catch (Throwable t) {
			logger.error("frodo user ui: change password fail", t);
			return "internal_server_error";
		}
		return "success";
	}

	private boolean isResetable(User user, AccessProfile profile, int loginMethod) {
		String sourceType = user.getSourceType();
		if (sourceType == null) {
			if (user.getPassword() != null)
				return true;
		} else {
			AccessGateway gw = gwApi.getCurrentAccessGateway();
			// 어디서 생성되었는지를 안다면 변경이 허용된 타입인지 확인. 후에 추가(ldap 등)가 필요.
			if (sourceType.equals("local") || sourceType.equals("ldap"))
				return true;
			else if (loginMethod == 6)	// LoginMethod_PW_OTP
				return true;
			else if (sourceType.equals("ldap")) {
				if (gw.getLdapMode() == LdapMode.LDAP_SYNC_EXTEND && profile.getLdapAttributes() != null)
					return profile.getLdapAttributes().isUsePasswordChange();

				return true;
			} else if (sourceType.equals("external"))
				return externalService.isPasswordChangeSupported();
		}
		return false;
	}

	private void sendJsonResult(PrintWriter out, String result) throws JSONException {
		Map<String, Object> m = new HashMap<String, Object>();
		m.put("result", result);
		out.write(JSONConverter.jsonize(m));
	}

	private String getMailTemplate() {
		StringBuilder sb = new StringBuilder();

		InputStream is = getInputStream("mail.html");
		if(is == null)
			return null;
		
		try {
			byte[] b = new byte[8096];
			while (true) {
				int readBytes = is.read(b);
				if (readBytes <= 0)
					break;

				sb.append(new String(b, Charset.forName("utf-8")));
			}

			return sb.toString();
		} catch (IOException e) {
			logger.error("frodo user ui: cannot read mail template html", e);
			return null;
		} finally {
			if (is != null)
				try {
					is.close();
				} catch (IOException e) {
				}
		}
	}
	
	private InputStream getInputStream(String path) {
		if (baseDir != null) {
			File f = new File(baseDir, path);

			InputStream is = null;

			try {
				is = new FileInputStream(f);
				return is;
			} catch (Exception e) {
				logger.error("frodo user ui: cannot open file base [{}] path [{}]", baseDir.getAbsolutePath(), path);
			}
		}

		Bundle b = bc.getBundle();
		try {
			URL url = b.getEntry("/WEB-INF/" + path);
			return url.openStream();
		} catch (Exception e) {
			logger.error("frodo user ui: cannot open bundle [{}] path [{}]", b.getBundleId(), path);
			return null;
		}
	}

	private class MailTask implements Runnable {
		private MimeMessage msg;

		public MailTask(MimeMessage msg) {
			this.msg = msg;
		}

		@Override
		public void run() {
			try {
				Transport.send(msg);
			} catch (MessagingException e) {
				logger.error("frodo user ui: frodo mailer config not found", e);
			}
		}
	}
}
