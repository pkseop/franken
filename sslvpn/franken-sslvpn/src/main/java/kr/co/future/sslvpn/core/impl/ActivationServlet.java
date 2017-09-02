package kr.co.future.sslvpn.core.impl;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.concurrent.CopyOnWriteArraySet;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import kr.co.future.sslvpn.core.ActivationListener;
import kr.co.future.sslvpn.core.ActivationService;
import kr.co.future.sslvpn.xtmconf.system.CommandUtil;
import kr.co.future.sslvpn.xtmconf.system.License;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.apache.felix.ipojo.annotations.Validate;

import kr.co.future.httpd.HttpContext;
import kr.co.future.httpd.HttpService;
import kr.co.future.httpd.MimeTypes;
import kr.co.future.sslvpn.core.impl.ActivationServlet;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(name = "frodo-activation-servlet")
@Provides
public class ActivationServlet extends HttpServlet implements ActivationService {
	private static final long serialVersionUID = 1L;
	private final Logger logger = LoggerFactory.getLogger(ActivationServlet.class.getName());

	@Requires
	private HttpService httpd;

	private BundleContext bc;
	private CopyOnWriteArraySet<ActivationListener> callbacks;

	public ActivationServlet(BundleContext bc) {
		this.bc = bc;
	}

	@Validate
	public void start() {
		callbacks = new CopyOnWriteArraySet<ActivationListener>();

		// register activation page
		HttpContext ctx = httpd.ensureContext("frodo");
		ctx.removeServlet("client");
		ctx.addServlet("activation", this, "/*");
	}

	@Override
	public void activate() {
		for (ActivationListener callback : callbacks) {
			callback.onActivated();
		}
	}

	@Override
	public void addListener(ActivationListener listener) {
		callbacks.add(listener);
	}

	@Override
	public void removeListener(ActivationListener listener) {
		callbacks.remove(listener);
	}

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		License license = License.load();

		String path = req.getPathInfo();
		if (path.equals("/serial")) {
			String serial = license.getSerial();
			if (serial.isEmpty())
				serial = "No Serial";

			resp.setHeader("Cache-Control", "no-cache");

			resp.getWriter().write(serial);
			resp.getWriter().close();
			return;
		}

		sendFile(req, resp);
	}

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		String serial = req.getParameter("serial");
		String licenseKey = req.getParameter("license_key");
		logger.info("frodo core: try to activate with [serial {}, license {}]", serial, licenseKey);

		if (serial == null || licenseKey == null) {
			resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
			return;
		}

		// validation
		try {
			int code = CommandUtil.run(new File("/"), "validate", "-l", licenseKey);
			if (code == 0) {
				logger.info("frodo core: activated with [serial {}, license {}]", serial, licenseKey);
				License.write(licenseKey);

				// invoke activated callbacks
				for (ActivationListener callback : callbacks) {
					try {
						callback.onActivated();
					} catch (Exception e) {
						logger.error("frodo core: activation listener should not throw any exception", e);
					}
				}

				resp.getWriter().write("success");
				resp.getWriter().close();
			} else {
				logger.info("frodo core: invalid license [serial {}, license {}]", serial, licenseKey);
				resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "invalid license");
			}
		} catch (InterruptedException e) {
		}
	}

	private void sendFile(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		InputStream is = getInputStream(req);
		String path = req.getPathInfo();

		if (is == null && !path.equals("/")) {
			// redirect
			resp.sendRedirect("/");
			return;
		}

		String contentType = MimeTypes.instance().getByFile(path);
		if (path.isEmpty() || path.equals("/") || path.equals("/index.html"))
			contentType = "text/html";

		resp.setHeader("Content-Type", contentType);
		resp.setHeader("Cache-Control", "no-cache");

		try {
			byte[] b = new byte[4096];
			int len;
			while ((len = is.read(b)) != -1)
				resp.getOutputStream().write(b, 0, len);

			resp.getOutputStream().flush();
		} catch (IOException e) {
			throw e;
		} finally {
			is.close();
		}
	}

	private InputStream getInputStream(HttpServletRequest req) {
		String path = req.getPathInfo();
		Bundle b = bc.getBundle();

		if (path.isEmpty() || path.equals("/"))
			path = "/index.html";

		try {
			URL url = b.getEntry("/WEB-INF" + path);
			return url.openStream();
		} catch (Exception e) {
			logger.trace("kraken webconsole: cannot open bundle [{}] resource [{}]", b.getBundleId(),
					req.getRequestURI());
			return null;
		}
	}
}
