package kr.co.future.sslvpn.core.servlet;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import kr.co.future.sslvpn.model.AuthorizedDevice;
import kr.co.future.sslvpn.model.api.AuthorizedDeviceApi;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Invalidate;
import org.apache.felix.ipojo.annotations.Requires;
import org.apache.felix.ipojo.annotations.Validate;
import kr.co.future.confdb.ConfigService;
import kr.co.future.confdb.Predicates;
import kr.co.future.dom.api.UserApi;
import kr.co.future.httpd.HttpContext;
import kr.co.future.httpd.HttpService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(name = "frodo-wips-servlet")
public class WipsRemoveUserServlet extends HttpServlet {
	private final Logger logger = LoggerFactory.getLogger(WipsRemoveUserServlet.class);

	private static final long serialVersionUID = 1L;
	
	@Requires
	private HttpService httpd;

	@Requires
	private ConfigService conf;

	@Requires
	private UserApi domUserApi;

	@Requires
	private AuthorizedDeviceApi deviceApi;

	@Validate
	public void start() {
		HttpContext ctx = httpd.ensureContext("frodo");
		ctx.addServlet("wips", this, "/wips/*");
	}

	@Invalidate
	public void stop() {
		if (httpd != null) {
			HttpContext ctx = httpd.ensureContext("frodo");
			ctx.removeServlet("wips");
		}
	}

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		String requestMethod = req.getPathInfo();
		logger.debug("frodo core: request path info [{}]", requestMethod);
		if (requestMethod.equals("/remove")) {
			String macAddress = req.getParameter("mac_address");
			if (macAddress == null) {
				logger.error("frodo core: cannot found mac address");
				resp.sendError(400);
				return;
			}

			AuthorizedDevice device = deviceApi.getDevice(Predicates.field("mac_address", macAddress));
			if (device == null) {
				logger.error("frodo core: cannot found authorized device [{}]", macAddress);
				return;
			}

			String loginName = device.getLoginName();
			deviceApi.unregisterDevice(device.getGuid());
			domUserApi.removeUser("localhost", loginName);
		}
	}
}
