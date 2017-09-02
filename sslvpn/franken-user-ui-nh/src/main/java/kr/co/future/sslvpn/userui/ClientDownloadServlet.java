package kr.co.future.sslvpn.userui;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.Charset;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import kr.co.future.sslvpn.core.ActivationListener;
import kr.co.future.sslvpn.core.ActivationService;
import kr.co.future.sslvpn.xtmconf.system.License;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Invalidate;
import org.apache.felix.ipojo.annotations.Requires;
import org.apache.felix.ipojo.annotations.Validate;
import kr.co.future.httpd.HttpContext;
import kr.co.future.httpd.HttpService;
import kr.co.future.httpd.MimeTypes;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(name = "frodo-userui-loader")
public class ClientDownloadServlet extends HttpServlet implements ActivationListener {
	private static final long serialVersionUID = 1L;
	private final Logger logger = LoggerFactory.getLogger(ClientDownloadServlet.class.getName());

	/*
	@Requires
	private ActivationService activation;
	*/

	@Requires
	private HttpService httpd;

	private BundleContext bc;

	public ClientDownloadServlet(BundleContext bc) {
		this.bc = bc;
	}

	@Validate
	public void start() {
		//activation.addListener(this);

		try {
			License license = License.load();

			// register download page only if sslplus is activated
			//if (license != null && !license.getType().equals("unregistered")) {
				HttpContext ctx = httpd.ensureContext("frodo");
				//ctx.removeServlet("activation");
				ctx.addServlet("client", this, "/*");
			//}
		} catch (IOException e) {
		}
	}

	@Invalidate
	public void stop() {
		if (httpd != null) {
			HttpContext ctx = httpd.ensureContext("frodo");
			ctx.removeServlet("client");
		}

		/*
		if (activation != null)
			activation.removeListener(this);
		*/
	}

	@Override
	public void onActivated() {
		logger.trace("frodo core: adding client download page to /*");
		HttpContext ctx = httpd.ensureContext("frodo");
//		ctx.removeServlet("activation");
		ctx.addServlet("client", this, "/*");
	}

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		String host = req.getHeader("Host");
		String path = req.getPathInfo();
		logger.trace("frodo user ui: get host [{}] path [{}]", host, path);

		if (path.isEmpty() || path.equals("/") || path.equals("/index.html")) {
			String index = getIndex();
			if (host != null)
				index = index.replaceAll("\\$host", host);

			resp.setHeader("Content-Type", "text/html");
			resp.getOutputStream().write(index.getBytes(Charset.forName("utf-8")));
			resp.getOutputStream().flush();
		} else {
			sendFile(req, resp);
		}
	}

	private void sendFile(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		InputStream is = getInputStream(req);

		if (is == null)
			resp.sendError(404);
		else {
			resp.setHeader("Content-Type", MimeTypes.instance().getByFile(req.getPathInfo()));

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
	}

	private String getIndex() {
		Bundle bundle = bc.getBundle();
		URL url = bundle.getEntry("/WEB-INF/index.html");
		byte[] b = new byte[4096];
		InputStream is = null;
		StringBuilder sb = new StringBuilder();

		try {
			is = url.openStream();

			int len;
			while ((len = is.read(b)) != -1)
				sb.append(new String(b, 0, len, Charset.forName("utf-8")));
		} catch (Exception e) {
			logger.error("frodo user ui: cannot load index", e);
		} finally {
			if (is != null)
				try {
					is.close();
				} catch (IOException e) {
				}
		}

		return sb.toString();
	}

	private InputStream getInputStream(HttpServletRequest req) {
		String host = req.getHeader("Host");
		String path = req.getPathInfo();

		if (path.contains("_" + host + ".exe"))
			path = path.replace("_" + host + "", "");

		Bundle b = bc.getBundle();
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
