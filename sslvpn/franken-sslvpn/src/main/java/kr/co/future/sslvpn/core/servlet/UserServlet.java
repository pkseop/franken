package kr.co.future.sslvpn.core.servlet;

import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.sax.TransformerHandler;
import javax.xml.transform.stream.StreamResult;

import kr.co.future.sslvpn.core.FileRedirectService;
import kr.co.future.sslvpn.core.GlobalConfig;
import kr.co.future.sslvpn.core.GlobalConfigApi;
import kr.co.future.sslvpn.core.servlet.UserServlet;
import kr.co.future.sslvpn.core.util.DownloadUtil;
import kr.co.future.sslvpn.model.ClientIpRange;
import kr.co.future.sslvpn.model.UserExtension;
import kr.co.future.sslvpn.model.api.AccessProfileApi;
import kr.co.future.sslvpn.model.api.UserApi;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Invalidate;
import org.apache.felix.ipojo.annotations.Requires;
import org.apache.felix.ipojo.annotations.Validate;

import io.netty.handler.codec.http.HttpHeaders;
import kr.co.future.confdb.ConfigService;
import kr.co.future.dom.api.OrganizationUnitApi;
import kr.co.future.dom.model.OrganizationUnit;
import kr.co.future.dom.model.User;
import kr.co.future.httpd.HttpContext;
import kr.co.future.httpd.HttpService;
import kr.co.future.msgbus.MessageBus;
import kr.co.future.msgbus.PushApi;
import kr.co.future.msgbus.Session;
import kr.co.future.msgbus.Token;
import kr.co.future.msgbus.TokenApi;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

import com.google.common.base.Strings;

import au.com.bytecode.opencsv.CSVWriter;

@Component(name = "frodo-user-servlet")
public class UserServlet extends HttpServlet {
	private static final String LOCALHOST = "localhost";
	private static final long serialVersionUID = 1L;
	private final Logger logger = LoggerFactory.getLogger(UserServlet.class);

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
	private OrganizationUnitApi orgUnitApi;
	
	@Requires
	private GlobalConfigApi configApi;
	
	@Requires
	private PushApi pushApi;
	
	@Requires
	private MessageBus msgbus;

	private File baseDir = null;
	private BundleContext bc;

	public UserServlet(BundleContext bc) {
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
			ctx.addServlet("user", this, "/user/*");
		} catch(IllegalStateException e) {
			logger.info("user servlet already exist.");
		}
	}

	@Invalidate
	public void stop() {
		if (httpd != null) {
			String adminCtx = "frodo";
			GlobalConfig gc = configApi.getGlobalConfig();
			if(gc != null)
				adminCtx = gc.getAdminConsoleContext();
			HttpContext ctx = httpd.ensureContext(adminCtx);
			ctx.removeServlet("user");
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		String host = req.getHeader("Host");
		if (req.getPathInfo().equals("/export")) {
			String tokenId = req.getParameter("token_id");
			if (tokenId == null) {
				logger.error("frodo core: cannot user export, token not found");
				resp.sendError(401);
				return;
			}

			Token token = tokenApi.getToken(tokenId);
			if (token == null) {
				logger.trace("frodo core: cannot user export, token id [{}] not found", tokenId);

				String error = null;
				String fileName = "token_expired.html";
				Bundle bundle = findUiBundle();
				if (baseDir != null)
					error = fileRedirectApi.getFileByConfig(bundle, baseDir, fileName);

				if (error == null)
					error = fileRedirectApi.getFile(bundle, fileName);

				if (error == null) {
					resp.sendError(404);
					return;
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

				return;
			}

			logger.debug("frodo core: token count [{}]", token.getCount());

			Map<String, Object> tokenData = (Map<String, Object>) token.getData();
			String type = (String) tokenData.get("type");

			if (type == null) {
				logger.error("frodo core: cannot user export, type not found");
				resp.sendError(401);
				return;
			}

			List<User> users = null;

			if ((Boolean) tokenData.get("selected")) {
				Collection<String> userList = (Collection<String>) tokenData.get("user_list");
				users = (List<User>) domUserApi.getUsers(LOCALHOST, userList);
			} else {
				users = (List<User>) domUserApi.getUsers(LOCALHOST);
			}

			token.setCount(token.getCount() - 1);
			tokenApi.setToken(token);

			try {
				if (type.equals("csv")) {
					Charset charset = Charset.forName("utf-8");
					String charsetName = (String) tokenData.get("charset");
					if (charsetName != null)
						charset = Charset.forName(charsetName);
	
					exportCsv(charset, resp, users, token);
					if (token.getCount() == 0)
						tokenApi.removeToken(tokenId);
	
					return;
				} else if (type.equals("xml")) {
					exportXml(resp, users, token);
					if (token.getCount() == 0)
						tokenApi.removeToken(tokenId);

					return;
				} else {
					logger.error("frodo core: invalid type [{}]", type);
				}
			} catch (IndexOutOfBoundsException e) {
				if(e.getMessage().equals("excceeded-max-buffer-size")) {
					logger.warn("excceeded max buffer size for downloading");
					
					String sessionId = req.getParameter("session_id");
					if(!Strings.isNullOrEmpty(sessionId)) {
						Session session = msgbus.getSession(sessionId);
						DownloadUtil.pushMsgOfExceededMaxBufSize(pushApi, session);
					}
				} else {
					logger.error("error occurred during export file.", e);
					resp.setStatus(500);
				}
			} catch (Exception e) {
				logger.error("cannot export file", e);
			} 

		} else {
			resp.sendError(404);
		}
	}

	private List<String> getKeyList() {
		List<String> keyList = new ArrayList<String>();
		keyList.add("login_name");
		keyList.add("name");
		keyList.add("description");
		keyList.add("dept");
		keyList.add("title");
		keyList.add("email");
		keyList.add("phone");
		keyList.add("static_ip");
		keyList.add("allow_ip_ranges");
		keyList.add("is_locked");
		keyList.add("expire");
		keyList.add("start");
		keyList.add("access_profile");
		keyList.add("admin_role");
		keyList.add("admin_profile");
		keyList.add("device_key_count");
		keyList.add("device_key_count_setting");
		keyList.add("last_login_at");
		keyList.add("last_logout_at");
		keyList.add("source_type");
		keyList.add("allow_time_table_id");
		
		return keyList;
	}

	@SuppressWarnings("unchecked")
	private void exportCsv(Charset charset, HttpServletResponse resp, List<User> users, Token token) throws IOException,
			ParseException {
		resp.setContentType("text/csv; charset=" + charset.name());
		Map<String, Object> tokenData = (Map<String, Object>) token.getData();
		resp.setHeader("Content-Disposition", "attachment; filename=" + tokenData.get("file_name"));
		resp.setHeader(HttpHeaders.Names.CONNECTION, "close");
		CSVWriter writer = null;
		OutputStreamWriter out = new OutputStreamWriter(resp.getOutputStream(), charset);
		List<Map<String, Object>> userList = convertToListMap(users);
		List<String> keyList = getKeyList();

		try {
			writer = new CSVWriter(out);
			int count = 0;
			for (Map<String, Object> m : userList) {
				int size = m.keySet().size();
				if (count == 0) {
					writer.writeNext(keyList.toArray(new String[keyList.size()]));
				}

				List<String> values = new ArrayList<String>();
				for (String key : keyList) {
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

	@SuppressWarnings("unchecked")
	private void exportXml(HttpServletResponse resp, List<User> users, Token token) throws IOException,
			TransformerConfigurationException, SAXException, ParseException {
		resp.setContentType("application/vnd.ms-excel");
		Map<String, Object> tokenData = (Map<String, Object>) token.getData();
		resp.setHeader("Content-Disposition", "attachment; filename=" + tokenData.get("file_name"));
		resp.setHeader(HttpHeaders.Names.CONNECTION, "close");

		ServletOutputStream os = null;
		try {
			List<String> keyList = getKeyList();
			os = resp.getOutputStream();
			SAXTransformerFactory tf = (SAXTransformerFactory) SAXTransformerFactory.newInstance();
			TransformerHandler handle = tf.newTransformerHandler();
			Transformer serializer = handle.getTransformer();
			serializer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
			serializer.setOutputProperty(OutputKeys.METHOD, "xml");
			serializer.setOutputProperty(OutputKeys.INDENT, "yes");
			handle.setResult(new StreamResult(os));
			handle.processingInstruction("mso-application", "progid=\"Excel.Sheet\"");
			AttributesImpl atts = new AttributesImpl();
			startXml(handle, atts);
			int count = 0;

			List<Map<String, Object>> userList = convertToListMap(users);

			for (Map<String, Object> m : userList) {
				if (count == 0) {
					atts.clear();
					handle.startElement("", "", "Row", atts);
					for (String key : keyList) {
						atts.clear();
						handle.startElement("", "", "Cell", atts);
						atts.clear();
						atts.addAttribute("", "", "ss:Type", "CDATA", "String");
						handle.startElement("", "", "Data", atts);
						handle.characters(key.toCharArray(), 0, key.length());
						handle.endElement("", "", "Data");
						handle.endElement("", "", "Cell");
					}
					handle.endElement("", "", "Row");
				}
				atts.clear();
				handle.startElement("", "", "Row", atts);
				for (String key : keyList) {
					atts.clear();
					handle.startElement("", "", "Cell", atts);
					atts.clear();
					atts.addAttribute("", "", "ss:Type", "CDATA", "String");
					handle.startElement("", "", "Data", atts);
					Object value = m.get(key);
					if (value != null)
						handle.characters(value.toString().toCharArray(), 0, value.toString().length());
					handle.endElement("", "", "Data");
					handle.endElement("", "", "Cell");
				}
				count++;
				handle.endElement("", "", "Row");
			}
			endXml(handle);
		} finally {
			if (os != null)
				os.close();
		}
	}

	private void endXml(TransformerHandler handle) throws SAXException {
		handle.endElement("", "", "Table");
		handle.endElement("", "", "Worksheet");
		handle.endElement("", "", "Workbook");
		handle.endPrefixMapping("html");
		handle.endPrefixMapping("ss");
		handle.endPrefixMapping("x");
		handle.endPrefixMapping("");
		handle.endDocument();
	}

	private void startXml(TransformerHandler handle, AttributesImpl atts) throws SAXException {
		handle.startDocument();
		handle.startPrefixMapping("", "urn:schemas-microsoft-com:office:spreadsheet");
		handle.startPrefixMapping("x", "urn:schemas-microsoft-com:office:excel");
		handle.startPrefixMapping("ss", "urn:schemas-microsoft-com:office:spreadsheet");
		handle.startPrefixMapping("html", "http://www.w3.org/TR/REC-html40");
		atts.clear();
		handle.startElement("", "", "Workbook", atts);
		atts.clear();
		atts.addAttribute("", "", "ss:Name", "CDATA", "test");
		handle.startElement("", "", "Worksheet", atts);
		atts.clear();
		handle.startElement("", "", "Table", atts);
		atts.clear();
		atts.addAttribute("", "", "ss:Index", "CDATA", "1");
		atts.addAttribute("", "", "ss:AutoFitWidth", "CDATA", "0");
		atts.addAttribute("", "", "ss:Width", "CDATA", "110");
		handle.startElement("", "", "Column", atts);
		handle.endElement("", "", "Column");
	}
	
	private void genOrgUnitNameMap(Map<String, String> map) {
		Collection<OrganizationUnit> orgUnits = orgUnitApi.getOrganizationUnits(LOCALHOST, true);
		genOrgUnitNameMap(map, orgUnits, null);
	}
	
	private void genOrgUnitNameMap(Map<String, String> map, Collection<OrganizationUnit> orgUnits, String parentName) {
		for(OrganizationUnit orgUnit : orgUnits) {
			String orgName = orgUnit.getName();
			logger.debug("parent name: [{}] org name: [{}]", new Object[]{parentName, orgName});
			orgName = (parentName == null ? orgName : parentName + "#" + orgName);
			
			String guid = orgUnit.getGuid(); 
			if(map.containsKey(guid)) {
				String curOrgName = map.get(guid);
				if(curOrgName.length() >= orgName.length())
					continue;
			}
			
			map.put(guid, orgName);
			Collection<OrganizationUnit> children = orgUnit.getChildren(); 
			if(children != null && children.size() > 0) {
				genOrgUnitNameMap(map, children, orgName);
			}
		}
	}

	@SuppressWarnings("unchecked")
   private List<Map<String, Object>> convertToListMap(List<User> users) throws ParseException {
		List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ssZ");
		HashMap<String, String> mapOrgUnits = new HashMap<String, String>();
		genOrgUnitNameMap(mapOrgUnits);
		
		try{
			for (User user : users) {
				Map<String, Object> m = new HashMap<String, Object>();
				UserExtension ext = userApi.getUserExtension(user);
				OrganizationUnit orgUnit = user.getOrgUnit();
	
				m.put("login_name", user.getLoginName());
				m.put("name", user.getName());
				m.put("description", user.getDescription());
				m.put("dept", orgUnit != null ?  mapOrgUnits.get(orgUnit.getGuid()) : "");
				m.put("title", user.getTitle());
				m.put("email", user.getEmail());
				m.put("phone", user.getPhone());
				m.put("static_ip", ext == null ? "" : ext.getStaticIp4());
				String allowIpRanges = "";
				if (ext != null) {
					for (ClientIpRange ipRange : ext.getAllowIpRanges()) {
						allowIpRanges += ipRange.getIpFrom() + "-" + ipRange.getIpTo();
						allowIpRanges += " ";
					}
				}
				m.put("allow_ip_ranges", allowIpRanges.trim());
				m.put("is_locked", ext == null ? "" : ext.isLocked());
				m.put("expire", ext == null || ext.getExpireDateTime() == null ? "" : dateFormat.format(ext.getExpireDateTime()));
				m.put("start", ext == null || ext.getStartDateTime() == null ? "" : dateFormat.format(ext.getStartDateTime()));
				m.put("access_profile", ext == null || ext.getProfile() == null ? "" : ext.getProfile().getName() + "|" + ext.getProfile().getGuid());
				
				String adminRole = null;
				String adminProfile = null;
				if(user.getExt() != null && user.getExt().get("admin") != null) {
					Map<String, Object> admin = (Map<String, Object>)user.getExt().get("admin");
					if(admin.get("role") != null) {
						Map<String, Object> role = (Map<String, Object>)admin.get("role");
						adminRole = (String)role.get("name");
					}
					if(admin.get("profile") != null) {					
						Map<String, Object> profile = (Map<String, Object>)admin.get("profile");
						adminProfile = (String)profile.get("name");					
					}
				}
				m.put("admin_role", adminRole == null ? "" : adminRole);
				m.put("admin_profile", adminProfile == null ? "" : adminProfile);
				m.put("device_key_count", ext == null || ext.getDeviceKeyCount() == null ? "" : ext.getDeviceKeyCount());
				m.put("device_key_count_setting", ext == null || ext.getDeviceKeyCountSetting() == null ? "" : ext.getDeviceKeyCountSetting());
				m.put("last_login_at", ext == null || ext.getLastLoginTime() == null ? "" : dateFormat.format(ext.getLastLoginTime()));
				m.put("last_logout_at", ext == null || ext.getLastLogoutTime() == null ? "" : dateFormat.format(ext.getLastLogoutTime()));
				m.put("source_type", user.getSourceType());
				m.put("allow_time_table_id", ext == null || ext.getAllowTimeTableId() == null ? "" : ext.getAllowTimeTableId());
				
				list.add(m);
			}
		}catch(Exception e) {
			logger.error("error occurred during export", e);
			return null;
		}
		return list;
	}

	private Bundle findUiBundle() {
		for (Bundle b : bc.getBundles())
			if (b.getSymbolicName().equals("kr.co.future.watchcat.ui"))
				return b;

		return null;
	}
}
