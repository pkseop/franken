package kr.co.future.sslvpn.core.servlet.csv;

import io.netty.handler.codec.http.HttpHeaders;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import kr.co.future.confdb.Config;
import kr.co.future.confdb.ConfigService;
import kr.co.future.confdb.Predicate;
import kr.co.future.confdb.Predicates;
import kr.co.future.httpd.HttpContext;
import kr.co.future.httpd.HttpService;
import kr.co.future.msgbus.MessageBus;
import kr.co.future.msgbus.PushApi;
import kr.co.future.msgbus.Session;
import kr.co.future.msgbus.Token;
import kr.co.future.msgbus.TokenApi;
import kr.co.future.sslvpn.core.FileRedirectService;
import kr.co.future.sslvpn.core.GlobalConfig;
import kr.co.future.sslvpn.core.GlobalConfigApi;
import kr.co.future.sslvpn.core.util.DownloadUtil;
import kr.co.future.sslvpn.model.AccessProfile;
import kr.co.future.sslvpn.model.AuthorizedDevice;
import kr.co.future.sslvpn.model.Server;
import kr.co.future.sslvpn.model.api.AccessProfileApi;
import kr.co.future.sslvpn.model.api.AuthorizedDeviceApi;
import kr.co.future.sslvpn.model.api.ServerApi;
import kr.co.future.sslvpn.model.api.UserApi;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Invalidate;
import org.apache.felix.ipojo.annotations.Requires;
import org.apache.felix.ipojo.annotations.Validate;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;

import au.com.bytecode.opencsv.CSVWriter;

@Component(name = "frodo-export-csv-servlet")
public class ExportCsvServlet extends HttpServlet {

	private static final String LOCALHOST = "localhost";
	private static final long serialVersionUID = 1L;
	private final Logger logger = LoggerFactory.getLogger(ExportCsvServlet.class);
	
	@Requires
	private HttpService httpd;

	@Requires
	private kr.co.future.dom.api.UserApi domUserApi;

	@Requires
	private UserApi userApi;

	@Requires
	private TokenApi tokenApi;

	@Requires
	private ConfigService conf;

	@Requires
	private FileRedirectService fileRedirectApi;
	
	@Requires
	private AccessProfileApi profileApi;
	
	@Requires
	private ServerApi serverApi;
	
	@Requires
	private AuthorizedDeviceApi authDeviceApi;
	
	@Requires
	private GlobalConfigApi configApi;
	
	@Requires
	private MessageBus msgbus;
	
	@Requires
	private PushApi pushApi;

	private File baseDir = null;
	private BundleContext bc;

	public ExportCsvServlet(BundleContext bc) {
		this.bc = bc;
	}
	
	@Validate
	public void start() {
		String adminCtx = "frodo";
		GlobalConfig config = configApi.getGlobalConfig();
		if (config != null) {
			adminCtx = config.getAdminConsoleContext();
			String adminConsolePath = config.getAdminConsolePath();
			if (adminConsolePath != null && !adminConsolePath.trim().isEmpty()) {
				File webDir = new File(adminConsolePath);

				if (webDir != null && webDir.isDirectory())
					baseDir = webDir;
			}
		}
		
		try{
			HttpContext ctx = httpd.ensureContext(adminCtx);
			ctx.addServlet("export", this, "/export/*");
		} catch(IllegalStateException e) {
			logger.info("export servlet already exist.");
		}
	}

	@Invalidate
	public void stop() {
		if (httpd != null) {
			String adminCtx = "frodo";
			GlobalConfig config = configApi.getGlobalConfig();
			if(config != null)
				adminCtx = config.getAdminConsoleContext();
			HttpContext ctx = httpd.ensureContext(adminCtx);
			ctx.removeServlet("export");
		}
	}
	
	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		String host = req.getHeader("Host");
		int method = 0;
		String pathInfo = req.getPathInfo();
		if (pathInfo.equals("/access_profile")) {
			method = 1;
		} else if(pathInfo.equals("/server")) {
			method = 2;
		} else if(pathInfo.equals("/auth_device")) {
			method = 3;
		} else if(pathInfo.equals("/unauth_device")) {
			method = 4;
		} else if(pathInfo.equals("/ipV4_filtering")){
			method = 5;
		} else if(pathInfo.equals("/ipV4_nat")){
			method = 6;
		} else {
			resp.sendError(404);
			return;
		}
		
		Token token = getToken(req, resp, host);
		if(token == null)
			return;
		
		logger.debug("frodo core: token count [{}]", token.getCount());
		@SuppressWarnings("unchecked")
		Map<String, Object> tokenData = (Map<String, Object>) token.getData();
		String charset = (String)tokenData.get("charset");
		String fileName = (String)tokenData.get("file_name");		
		token.setCount(token.getCount() - 1);
		tokenApi.setToken(token);
		
		try{
			if(method == 1) {
				exportAccessProfileInfo(resp,charset,fileName);
			} else if(method == 2) {
				exportServerInfo(resp,charset,fileName);
			} else if(method == 3) {
				exportAuthorizedDeviceInfo(req, resp,charset,fileName);
			} else if(method == 4) {
				exportUnauthorizedDeviceInfo(req, resp,charset,fileName);
			} else if(method == 5) {
				exportIPV4Filtering(req, resp,charset,fileName);
			} else if(method == 6) {
				exportIPV4NAT(req, resp,charset,fileName);
			}
		} catch (IndexOutOfBoundsException e) {
			if(e.getMessage().equals("excceeded-max-buffer-size")) {
				logger.warn("excceeded max buffer size for downloading [{}]", fileName);
				
				String sessionId = req.getParameter("session_id");
				if(!Strings.isNullOrEmpty(sessionId)) {
					Session session = msgbus.getSession(sessionId);
					DownloadUtil.pushMsgOfExceededMaxBufSize(pushApi, session);
				}
			} else {
				logger.error("error occurred during export csv file.", e);
				resp.setStatus(500);
			}
		} catch (Exception e) {
			logger.error("error occurred during export csv file.", e);
			resp.setStatus(500);
		}
		
		if (token.getCount() == 0) {
			String tokenId = req.getParameter("token_id");
			tokenApi.removeToken(tokenId);
		}
	}
	
	private Token getToken(HttpServletRequest req, HttpServletResponse resp, String host) throws IOException {
		String tokenId = req.getParameter("token_id");
		if (tokenId == null) {
			logger.error("frodo core: cannot export csv, token not found");
			resp.sendError(401);
			return null;
		}
		
		Token token = tokenApi.getToken(tokenId);
		if (token == null) {
			logger.trace("frodo core: cannot export csv, token id [{}] not found", tokenId);

			String error = null;
			String fileName = "token_expired.html";
			Bundle bundle = findUiBundle();
			if (baseDir != null)
				error = fileRedirectApi.getFileByConfig(bundle, baseDir, fileName);

			if (error == null)
				error = fileRedirectApi.getFile(bundle, fileName);

			if (error == null) {
				resp.sendError(404);
				return null;
			}

			if (host != null)
				error = error.replaceAll("\\$host", host);

			resp.setHeader("Content-Type", "text/html");
			resp.setContentType("text/html; charset=utf-8");
			ServletOutputStream os = null;
			try {
				os = resp.getOutputStream();
				os.write(error.getBytes(Charset.forName("utf-8")));
				os.flush();
			} finally {
				if (os != null)
					os.close();
			}

			return null;
		}
		return token;
	}
		
	
	private Bundle findUiBundle() {
		for (Bundle b : bc.getBundles())
			if (b.getSymbolicName().equals("kr.co.future.watchcat.ui"))
				return b;

		return null;
	}
	
	private void exportUnauthorizedDeviceInfo(HttpServletRequest req, HttpServletResponse resp, String charset, String fileName) throws IndexOutOfBoundsException, IOException, ParseException {
		OwnerMatcher ownerPred = req.getParameter("find_by_owner") != null ? new OwnerMatcher(req.getParameter("find_by_owner")) : null;
		Predicate pred = Predicates.and(ownerPred, Predicates.field("is_authorized", false));

		List<AuthorizedDevice> unauthDevieces = authDeviceApi.getDevices(0, Integer.MAX_VALUE, pred);
		ExportObject exportObject = new ExportAuthorizedDeviceInfo();
		exportCsv(resp, unauthDevieces, exportObject, charset, fileName);
	}
	
	private void exportAuthorizedDeviceInfo(HttpServletRequest req, HttpServletResponse resp, String charset, String fileName) throws IndexOutOfBoundsException, IOException, ParseException {
		OwnerMatcher ownerPred = req.getParameter("find_by_owner") != null ? new OwnerMatcher(req.getParameter("find_by_owner")) : null;
		Predicate pred = Predicates.and(ownerPred,
				Predicates.or(Predicates.field("is_authorized", null), Predicates.field("is_authorized", true)));

		List<AuthorizedDevice> authDevieces = authDeviceApi.getDevices(0, Integer.MAX_VALUE, pred);
		ExportObject exportObject = new ExportAuthorizedDeviceInfo();
		exportCsv(resp, authDevieces, exportObject, charset, fileName);
	}
	
	private void exportAccessProfileInfo(HttpServletResponse resp, String charset, String fileName) throws IndexOutOfBoundsException, IOException, ParseException {
		List<AccessProfile> profiles = profileApi.getAccessProfiles();
		ExportObject exportObject = new ExportAccessProfileInfo();
		exportCsv(resp, profiles, exportObject, charset, fileName);
	}
	
	private void exportServerInfo(HttpServletResponse resp, String charset, String fileName) throws IndexOutOfBoundsException, IOException, ParseException {
		List<Server> servers = serverApi.getServers();
		ExportObject exportObject = new ExportServerInfo();
		exportCsv(resp, servers, exportObject, charset, fileName);
	}
	
	private void exportIPV4Filtering(HttpServletRequest req, HttpServletResponse resp, String strCharset, String fileName) throws IndexOutOfBoundsException, IOException {
		executeFirewallToCSVPHP(req, resp, strCharset, fileName, "fType=1");
	}
	
	private void exportIPV4NAT(HttpServletRequest req, HttpServletResponse resp, String strCharset, String fileName) throws IndexOutOfBoundsException, IOException {
		executeFirewallToCSVPHP(req, resp, strCharset, fileName, "fType=2");
	}
	
	private void executeFirewallToCSVPHP(HttpServletRequest req, HttpServletResponse resp, String strCharset, String fileName, String param)  throws IndexOutOfBoundsException, IOException{
		OutputStreamWriter out = null;
		try {
			ProcessBuilder builder = new ProcessBuilder("/usr/bin/php", "-q", "/var/www/webadmin/firewall_toCSV.php", param);
			Process p = builder.start();
			logger.info("frodo core: running php {}, param {}", "/var/www/webadmin/firewall_toCSV.php", param);
			p.waitFor();
			InputStream is = p.getInputStream();
			BufferedReader br = new BufferedReader(new InputStreamReader(is, "euc-kr"));
			String line = br.readLine();
			clear(p.getErrorStream());
			StringBuilder sb = new StringBuilder();
			try {
				while (line != null){
					sb.append(line).append(System.lineSeparator());
					line = br.readLine();
				}
			} catch (IOException e) {
			} finally {
				if (is != null) {
					try {
						is.close();
					} catch (IOException e) {
					}
				}
			}
			
			logger.info("frodo core: {} export csv result start ====", param == "fType=1" ? "ipv4filtering":"ipv4nat");
			logger.info(sb.toString());
			logger.info("frodo core: {} export csv result end ====", param == "fType=1" ? "ipv4filtering":"ipv4nat");
			
			Charset charset = Charset.forName(strCharset);
			resp.setContentType("text/csv; charset=" + charset.name());
			resp.setHeader("Content-Disposition", "attachment; filename=" + fileName);
			resp.setHeader(HttpHeaders.Names.CONNECTION, "close");
			out = new OutputStreamWriter(resp.getOutputStream(), charset);
			out.write(sb.toString());
		} catch (InterruptedException e) {
		} finally{
			out.close();
		}
	}
	
	private void clear(InputStream is) {
		StringBuilder sb = new StringBuilder();
		try {
			byte[] b = new byte[4096];
			while (true) {
				int read = is.read(b);
				if (read <= 0)
					break;

				sb.append(new String(b, 0, read));
			}
		} catch (IOException e) {
		} finally {
			if (is != null) {
				try {
					is.close();
				} catch (IOException e) {
				}
			}

			logger.trace("frodo xtmconf: command output [{}]", sb.toString());
		}
	}
	
	private void exportCsv(HttpServletResponse resp, List<?> list, ExportObject exportObject, String strCharset, String fileName) throws IndexOutOfBoundsException,
			IOException, ParseException {
		Charset charset = Charset.forName(strCharset);
		resp.setContentType("text/csv; charset=" + charset.name());
		resp.setHeader("Content-Disposition", "attachment; filename=" + fileName);
		resp.setHeader(HttpHeaders.Names.CONNECTION, "close");
		CSVWriter writer = null;
		OutputStreamWriter out = new OutputStreamWriter(resp.getOutputStream(), charset);
		List<Map<String, Object>> listMap = exportObject.convertToListMap(list);
		List<String> keySet = exportObject.getKeyList(listMap);

		try {
			writer = new CSVWriter(out);
			int count = 0;
			for (Map<String, Object> m : listMap) {
				int size = m.keySet().size();
				if (count == 0) {
					writer.writeNext(keySet.toArray(new String[keySet.size()]));
				}

				List<String> values = new ArrayList<String>();
				for (String key : keySet) {
					values.add(m.get(key) == null ? "" : m.get(key).toString());
				}
				writer.writeNext(values.toArray(new String[size]));

				if (count % 1000 == 0)
					writer.flush();

				count++;
			}

		} finally {
			if (writer != null)
				writer.close();
		}
	}

	private class OwnerMatcher implements Predicate {
		private String target;

		public OwnerMatcher(String target) {
			this.target = target;
		}

		@Override
		public boolean eval(Config c) {
			@SuppressWarnings("unchecked")
			Map<String, Object> m = (Map<String, Object>) c.getDocument();

			String owner = (String) m.get("owner");
			if (owner != null && owner.contains(target))
				return true;

			String loginName = (String) m.get("login_name");
			if (loginName != null && loginName.contains(target))
				return true;

			return false;
		}
	}
}
