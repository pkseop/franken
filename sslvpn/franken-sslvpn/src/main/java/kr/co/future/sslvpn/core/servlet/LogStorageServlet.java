package kr.co.future.sslvpn.core.servlet;

import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.util.ArrayList;
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
import kr.co.future.sslvpn.core.servlet.LogStorageServlet;
import kr.co.future.sslvpn.core.util.DownloadUtil;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Invalidate;
import org.apache.felix.ipojo.annotations.Requires;
import org.apache.felix.ipojo.annotations.Validate;

import io.netty.handler.codec.http.HttpHeaders;
import kr.co.future.confdb.ConfigService;
import kr.co.future.httpd.HttpContext;
import kr.co.future.httpd.HttpService;
import kr.co.future.logdb.LogQuery;
import kr.co.future.logdb.LogQueryService;
import kr.co.future.msgbus.MessageBus;
import kr.co.future.msgbus.PushApi;
import kr.co.future.msgbus.Session;
import kr.co.future.msgbus.Token;
import kr.co.future.msgbus.TokenApi;

import org.json.JSONConverter;
import org.json.JSONException;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

import com.google.common.base.Strings;

import au.com.bytecode.opencsv.CSVWriter;

@Component(name = "frodo-logstorage-servlet")
public class LogStorageServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;
	private final Logger logger = LoggerFactory.getLogger(LogStorageServlet.class);

	@Requires
	private HttpService httpd;

	@Requires
	private LogQueryService logService;

	@Requires
	private TokenApi tokenApi;

	@Requires
	private ConfigService conf;

	@Requires
	private FileRedirectService fileRedirectApi;
	
	@Requires
	private GlobalConfigApi configApi;
	
	@Requires
	private PushApi pushApi;
	
	@Requires
	private MessageBus msgbus;

	private File baseDir = null;
	private BundleContext bc;

	public LogStorageServlet(BundleContext bc) {
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
		
		HttpContext ctx = httpd.ensureContext(adminCtx);
		ctx.addServlet("logstorage", this, "/log/*");
		
	}

	@Invalidate
	public void stop() {
		if (httpd != null) {
			String adminCtx = "frodo";
			GlobalConfig config = configApi.getGlobalConfig();
			if(config != null)
				adminCtx = config.getAdminConsoleContext();
			HttpContext ctx = httpd.ensureContext(adminCtx);
			ctx.removeServlet("logstorage");
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		String host = req.getHeader("Host");
		if (req.getPathInfo().equals("/export")) {
			String tokenId = req.getParameter("token_id");
			if (tokenId == null) {
				logger.error("frodo core: cannot log export, token not found");
				resp.sendError(401);
				return;
			}

			Token token = tokenApi.getToken(tokenId);

			if (token == null) {
				logger.trace("frodo core: cannot log export, token id [{}] not found", tokenId);
				String error = null;
				Bundle bundle = findUiBundle();
				String fileName = "token_expired.html";

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

			Map<String, Object> tokenData = (Map<String, Object>) token.getData();
			String type = (String) tokenData.get("type");

			if (type == null) {
				logger.error("frodo core: cannot log export, type not found");
				resp.sendError(401);
				return;
			}

			LogQuery query = logService.getQuery((Integer) tokenData.get("query_id"));

			if (query == null) {
				logger.trace("frodo core: cannot log export, query id [{}] not found ", tokenData.get("query_id"));
				tokenApi.removeToken(tokenId);
				resp.sendError(500);
				return;
			}

			token.setCount(token.getCount() - 1);
			tokenApi.setToken(token);

			try {
				if (type.equals("csv")) {
					Charset charset = Charset.forName("utf-8");
					String charsetName = (String) tokenData.get("charset");
					if (charsetName != null)
						charset = Charset.forName(charsetName);
	
					exportCsv(charset, resp, query, token);
					if (token.getCount() == 0)
						tokenApi.removeToken(tokenId);
					return;
				} else if (type.equals("xml")) {
					exportXml(resp, query, token);
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
		}
	}

	@SuppressWarnings("unchecked")
	private void exportXml(HttpServletResponse resp, LogQuery query, Token token) throws TransformerConfigurationException,
			SAXException, IllegalArgumentException, IOException {
		resp.setContentType("application/vnd.ms-excel");
		Map<String, Object> tokenData = (Map<String, Object>) token.getData();
		resp.setHeader("Content-Disposition", "attachment; filename=" + tokenData.get("file_name"));
		resp.setHeader(HttpHeaders.Names.CONNECTION, "close");

		ServletOutputStream os = null;
		try {
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

			List<Map<String, Object>> result = null;
			if (tokenData.get("offset") != null && tokenData.get("limit") != null)
				result = query.getResultAsList((Integer) tokenData.get("offset"), (Integer) tokenData.get("limit"));
			else
				result = query.getResultAsList();

			for (Map<String, Object> m : result) {
				if (count == 0) {
					atts.clear();
					handle.startElement("", "", "Row", atts);
					for (String key : m.keySet()) {
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
				for (String key : m.keySet()) {
					atts.clear();
					handle.startElement("", "", "Cell", atts);
					atts.clear();
					atts.addAttribute("", "", "ss:Type", "CDATA", "String");
					handle.startElement("", "", "Data", atts);
					Object value = m.get(key);
					if (value != null) {
						if(value instanceof Map) {
							String strValue = "";
							try {
								strValue = JSONConverter.jsonize(value);
							} catch (JSONException e) {
								logger.error("error occurred during convert map object to json", e);
							}
							handle.characters(strValue.toCharArray(), 0, strValue.length());
						} else
							handle.characters(value.toString().toCharArray(), 0, value.toString().length());
					}
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

	@SuppressWarnings("unchecked")
	private void exportCsv(Charset charset, HttpServletResponse resp, LogQuery query, Token token) throws IOException {
		resp.setContentType("text/csv; charset=" + charset.name());
		Map<String, Object> tokenData = (Map<String, Object>) token.getData();
		resp.setHeader("Content-Disposition", "attachment; filename=" + tokenData.get("file_name"));
		resp.setHeader(HttpHeaders.Names.CONNECTION, "close");

		CSVWriter writer = null;

		OutputStreamWriter out = new OutputStreamWriter(resp.getOutputStream(), charset);
		try {
			writer = new CSVWriter(out);

			List<Map<String, Object>> result = null;

			if (tokenData.get("offset") != null && tokenData.get("limit") != null)
				result = query.getResultAsList((Integer) tokenData.get("offset"), (Integer) tokenData.get("limit"));
			else
				result = query.getResultAsList();

			int count = 0;
			for (Map<String, Object> m : result) {
				int size = m.keySet().size();
				if (count == 0) {
					writer.writeNext(m.keySet().toArray(new String[size]));
				}

				List<String> values = new ArrayList<String>();
				for (String key : m.keySet()) {
					String value = null;
					if(m.get(key) == null)
						value = "";
					else if(m.get(key) instanceof Map) {
						try {
							value = JSONConverter.jsonize(m.get(key));
						} catch (JSONException e) {
							value = "";
							logger.error("error occurred during convert map object to json", e);
						}
					} else
						value = m.get(key).toString();
					
					values.add(value);
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

	private Bundle findUiBundle() {
		for (Bundle b : bc.getBundles())
			if (b.getSymbolicName().equals("kr.co.future.watchcat.ui"))
				return b;

		return null;
	}
}
