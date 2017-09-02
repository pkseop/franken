package kr.co.future.sslvpn.auth.hl.servlet;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import kr.co.future.sslvpn.core.FileRedirectService;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Invalidate;
import org.apache.felix.ipojo.annotations.Requires;
import org.apache.felix.ipojo.annotations.Validate;
import kr.co.future.httpd.HttpContext;
import kr.co.future.httpd.HttpService;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(name = "hl-notice-servlet")
public class RelocateNoticeServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;
	private final Logger logger = LoggerFactory.getLogger(RelocateNoticeServlet.class);

	@Requires
	private HttpService httpd;

	@Requires
	private FileRedirectService fileService;

	private BundleContext bc;

	public RelocateNoticeServlet(BundleContext bc) {
		this.bc = bc;
	}

	@Validate
	public void start() {
		HttpContext ctx = httpd.ensureContext("frodo");
		ctx.addServlet("home", this, "/home");
	}

	@Invalidate
	public void stop() {
		if (httpd != null) {
			HttpContext ctx = httpd.ensureContext("frodo");
			ctx.removeServlet("home");
		}
	}

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		resp.setContentType("text/html;charset=utf-8");
		PrintWriter pw = null;

		try {
			Bundle bundle = null;
			for (Bundle b : bc.getBundles()) {
				if (b.getSymbolicName().equals("kr.co.future.frodo.auth.hl")) {
					bundle = b;
					break;
				}
			}

			if (bundle == null) {
				logger.error("frodo auth hl: cannot found hl bundle");
				return;
			}
			pw = resp.getWriter();
			pw.write(fileService.getFile(bundle, "notice.html"));
			pw.flush();
		} catch (Exception e) {
		} finally {
			if (pw != null)
				pw.close();
		}
	}
}
