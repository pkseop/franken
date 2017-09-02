package kr.co.future.sslvpn.core.servlet;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import kr.co.future.sslvpn.core.DnsCheckService;
import kr.co.future.sslvpn.core.servlet.DnsCheckServlet;
import kr.co.future.sslvpn.model.DnsCheck;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Invalidate;
import org.apache.felix.ipojo.annotations.Requires;
import org.apache.felix.ipojo.annotations.Validate;
import org.json.JSONConverter;
import org.json.JSONException;

import kr.co.future.httpd.HttpContext;
import kr.co.future.httpd.HttpService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(name = "frodo-dns-servlet")
public class DnsCheckServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;
	private final Logger logger = LoggerFactory.getLogger(DnsCheckServlet.class);

	@Requires
	private HttpService httpd;

	@Requires
	private DnsCheckService dnsCheckApi;

	@Validate
	public void start() {
		HttpContext ctx = httpd.ensureContext("frodo");
		ctx.addServlet("dnscheck", this, "/dnscheck/*");
	}

	@Invalidate
	public void stop() {
		if (httpd != null) {
			HttpContext ctx = httpd.ensureContext("frodo");
			ctx.removeServlet("dnscheck");
		}
	}

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		String pathInfo = req.getPathInfo();
		logger.debug("frodo core: pathInfo [{}]", pathInfo);

		PrintWriter out = resp.getWriter();
		try {
			if (pathInfo.equals("/config")) {
				handleDnsCheckInfo(req, resp, out);
				return;
			} else {
				resp.sendError(404);
			}
		} catch (JSONException e) {
			logger.error("frodo core: cannot convert object to json string", e);
			resp.sendError(500);
		} finally {
			out.close();
		}
	}

	private void handleDnsCheckInfo(HttpServletRequest req, HttpServletResponse resp, PrintWriter out) throws IOException,
			JSONException {
		Collection<DnsCheck> dnsChecks = dnsCheckApi.getDnsCheckList();

		Map<String, Object> mm = new HashMap<String, Object>();
		for (DnsCheck c : dnsChecks) {
			mm.put(c.getDomain(), c.getDnsIpList());
		}

		Map<String, Object> m = new HashMap<String, Object>();
		m.put("domains", mm);

		String json = JSONConverter.jsonize(m);
		logger.debug("frodo core: dns check list to JSON[{}]", json);

		out.print(json);
	}

}
