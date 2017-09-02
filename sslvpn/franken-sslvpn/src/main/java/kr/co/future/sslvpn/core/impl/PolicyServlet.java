package kr.co.future.sslvpn.core.impl;

import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import kr.co.future.sslvpn.model.AccessProfile;
import kr.co.future.sslvpn.model.ClientApp;
import kr.co.future.sslvpn.model.api.AccessProfileApi;
import kr.co.future.sslvpn.model.api.ClientAppApi;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Invalidate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.apache.felix.ipojo.annotations.Validate;
import org.json.JSONException;
import org.json.JSONWriter;

import kr.co.future.codec.Base64;
import kr.co.future.dom.api.FileUploadApi;
import kr.co.future.dom.model.UploadedFile;
import kr.co.future.httpd.HttpContext;
import kr.co.future.httpd.HttpService;
import kr.co.future.sslvpn.core.impl.PolicyServlet;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Deprecated
@Component(name = "frodo-policy-servlet")
@Provides
public class PolicyServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;
	public static final String LOCALHOST = "localhost";
	private final Logger logger = LoggerFactory.getLogger(PolicyServlet.class.getName());

	@Requires
	private AccessProfileApi accessProfileApi;

	@Requires
	private HttpService httpd;

	@Requires
	private FileUploadApi fileUploadApi;

	@Requires
	private ClientAppApi clientAppApi;

	private BundleContext bc;

	public PolicyServlet(BundleContext bc) {
		this.bc = bc;
	}

	@Validate
	public void start() {
		HttpContext ctx = httpd.ensureContext("frodo");
		ctx.addServlet("policy", this, "/policy");
	}

	@Invalidate
	public void stop() {
		if (httpd != null) {
			HttpContext ctx = httpd.ensureContext("frodo");
			ctx.removeServlet("policy");
		}
	}

	/*
	 * 리스트형 객체를 받아서 JSON 화 시킴
	 */
	private String jsonize(List<Object> l) {

		StringWriter writer = new StringWriter(10240);
		JSONWriter jsonWriter = new JSONWriter(writer);

		try {

			jsonWriter.array();

			for (Object o : l) {

				jsonWriter.object();

				@SuppressWarnings("unchecked")
				Map<String, Object> m = (Map<String, Object>) o;

				for (String key : m.keySet())
					jsonWriter.key(key).value(m.get(key));

				jsonWriter.endObject();
			}

			jsonWriter.endArray();

		} catch (JSONException e) {
			logger.error("frodo core: cannot serialize json", e);
		}

		return writer.toString();
	}

	private byte[] getIcon(String path) {
		byte[] b = null;

		ByteArrayOutputStream bos = null;
		InputStream fis = null;
		try {
			Bundle bundle = getClientAdapterBundle();
			if (bundle == null) {
				logger.error("frodo core: client adapter bundle not found, icon search failed");
				return null;
			}

			URL resource = bundle.getResource(path);
			if (resource == null) {
				logger.trace("frodo core: icon path [{}] not found in bundle [{}]", path, bundle.getBundleId());
				return null;
			}

			fis = resource.openStream();
			bos = new ByteArrayOutputStream();

			byte[] buf = new byte[1024];
			for (int readNum; (readNum = fis.read(buf)) != -1;) {
				bos.write(buf, 0, readNum);
			}
			b = bos.toByteArray();
		} catch (FileNotFoundException e) {
			logger.error("icon file not found: " + path);
		} catch (IOException e) {
			logger.error("IOException while reading icon", e);
		} finally {
			close(fis);
			close(bos);
		}

		return b;
	}

	/**
	 * 번들 정보 반환 함수
	 */
	private Bundle getClientAdapterBundle() {
		for (Bundle b : bc.getBundles())
			if (b.getSymbolicName().equals("kr.co.future.frodo.clientadapter"))
				return b;
		return null;
	}

	private byte[] getIconFromFileUploadApi(String guid) {
		byte[] b = null;

		ByteArrayOutputStream bos = null;
		InputStream fis = null;
		try {
			UploadedFile fileMetadata = fileUploadApi.getFileMetadata(LOCALHOST, guid);
			if (fileMetadata == null)
				return null;

			fis = new FileInputStream(fileMetadata.getFile());
			bos = new ByteArrayOutputStream();

			byte[] buf = new byte[1024];
			for (int readNum; (readNum = fis.read(buf)) != -1;) {
				bos.write(buf, 0, readNum);
			}
			b = bos.toByteArray();
		} catch (FileNotFoundException e) {
			logger.error("cannot download: " + guid);
		} catch (IOException e) {
			logger.error("IOException while reading icon", e);
		} finally {
			close(fis);
			close(bos);
		}

		return b;
	}

	private void close(Closeable bos) {
		if (bos == null)
			return;
		try {
			bos.close();
		} catch (IOException e) {
		}
	}

	private List<ClientApp> getClientApps(String loginName) {
		if (loginName == null) {
			logger.trace("frodo client adapter: fetch all client app list");
			return clientAppApi.getClientApps();
		}

		AccessProfile accessProfile = accessProfileApi.determineProfile(loginName);
		if (accessProfile == null)
			return null;

		logger.trace("frodo client adapter: fetch app list for [login={}, profile={}]", loginName, accessProfile.getName());
		return accessProfile.getClientApps();
	}

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		String loginName = req.getParameter("login_name");
		List<ClientApp> clientApps = getClientApps(loginName);
		if (clientApps == null) {
			logger.error("cannot determine access profile id for user {}", loginName);
			return;
		}

		PrintWriter out = null;
		try {
			out = resp.getWriter();
			List<Object> jsonApps = new ArrayList<Object>();
			for (ClientApp clientApp : clientApps) {
				Map<String, Object> m = new HashMap<String, Object>();

				String id = String.valueOf(clientApp.getId());
				String guid = clientApp.getGuid();
				String appGuid = clientApp.getAppGuid();
				String platform = clientApp.getPlatform();
				String name = clientApp.getName();
				String operator = clientApp.getOperator();
				String phone = clientApp.getPhone();

				String icon;

				if (clientApp.getIcon() == null) {
					icon = new String(Base64.encode(getIcon("icon/sslplus.png")));
				} else {
					try {
						byte[] iconBinary = getIconFromFileUploadApi(clientApp.getIcon());
						if (iconBinary != null)
							icon = new String(Base64.encode(iconBinary));
						else
							icon = new String(Base64.encode(getIcon("icon/sslplus.png")));
					} catch (Exception e) {
						logger.warn("frodo client adapter: getIconFromFileUploadApi failed", e);
						icon = new String(Base64.encode(getIcon("icon/sslplus.png")));
					}
				}

				String description = clientApp.getDescription();
				String mobile = clientApp.getMobile();
				String email = clientApp.getEmail();

				m.put("id", id);
				m.put("guid", guid);
				m.put("appGuid", appGuid);
				m.put("platform", platform);
				m.put("name", name);
				m.put("operator", operator);
				m.put("phone", phone);
				m.put("icon", icon);
				m.put("metadata", clientApp.getMetadata());
				m.put("description", description);
				m.put("mobile", mobile);
				m.put("email", email);

				jsonApps.add(m);
			}

			String json = jsonize(jsonApps);
			// print JSON
			out.print(json);
		} finally {
			if (out != null)
				out.close();
		}

	}
}
