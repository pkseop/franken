package kr.co.future.sslvpn.core.impl;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Date;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import kr.co.future.sslvpn.model.AuthorizedDevice;
import kr.co.future.sslvpn.model.UserExtension;
import kr.co.future.sslvpn.model.api.AuthorizedDeviceApi;
import kr.co.future.sslvpn.model.api.UserApi;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Invalidate;
import org.apache.felix.ipojo.annotations.Requires;
import org.apache.felix.ipojo.annotations.Validate;
import kr.co.future.httpd.HttpContext;
import kr.co.future.httpd.HttpService;

@Component(name = "frodo-device-register-servlet")
public class DeviceRegisterServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;

	@Requires
	private HttpService httpd;

	@Requires
	private AuthorizedDeviceApi authDeviceApi;

	@Requires
	private UserApi userApi;

	@Validate
	public void start() {
		HttpContext ctx = httpd.ensureContext("frodo");
		ctx.addServlet("register", this, "/register");
	}

	@Invalidate
	public void stop() {
		if (httpd != null) {
			HttpContext ctx = httpd.ensureContext("frodo");
			ctx.removeServlet("register");
		}
	}

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		try {
			String authKey = req.getParameter("auth_key");

			// check auth key
			UserExtension ext = userApi.checkAuthKey(authKey);
			if (ext == null) {
				resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "auth key not found");
				return;
			}

			// check key expire date
			if (new Date().after(ext.getKeyExpireDateTime())) {
				resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "auth key expired");
				return;
			}

			AuthorizedDevice device = new AuthorizedDevice();
			device.setType(Integer.valueOf(req.getParameter("type")));
			device.setDeviceKey(req.getParameter("device_key"));
			device.setHostName(req.getParameter("host_name"));
			device.setDescription(req.getParameter("description"));
			device.setOwner(ext.getUser().getName());
			device.setLoginName(ext.getUser().getLoginName());
			device.setRemoteIp(req.getRemoteAddr());
			device.setBlocked(false);
			if(ext.getUser().getOrgUnit() != null)
				device.setOrgUnitName(ext.getUser().getOrgUnit().getName());

			authDeviceApi.registerDevice(device);

			// purge auth key
			ext.setDeviceAuthKey(null);
			ext.setKeyExpireDateTime(null);
			userApi.setUserExtension(ext);

			// send response
			resp.getWriter().write("success");
			resp.getWriter().close();

		} catch (Exception e) {
			String msg = getErrorMessage(e);
			resp.sendError(HttpServletResponse.SC_BAD_REQUEST, msg);
		}
	}

	private String getErrorMessage(Exception e) {
		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw);
		try {
			e.printStackTrace(pw);
			pw.flush();
			return sw.getBuffer().toString();
		} finally {
			try {
				sw.close();
			} catch (IOException e1) {
			}

			pw.close();
		}
	}

}
