package kr.co.future.sslvpn.core.impl;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Invalidate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.apache.felix.ipojo.annotations.Validate;

import kr.co.future.httpd.HttpContext;
import kr.co.future.httpd.HttpService;
import kr.co.future.httpd.MimeTypes;
import kr.co.future.msgbus.MessageBus;
import kr.co.future.msgbus.MsgbusException;
import kr.co.future.msgbus.PushApi;
import kr.co.future.msgbus.Session;
import kr.co.future.sslvpn.core.backup.Constants;
import kr.co.future.sslvpn.core.impl.DownloadServlet;
import kr.co.future.sslvpn.core.util.DownloadUtil;
import kr.co.future.sslvpn.core.xenics.XenicsService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;

import kr.co.future.sslvpn.core.DownloadService;
import kr.co.future.sslvpn.core.GlobalConfig;
import kr.co.future.sslvpn.core.GlobalConfigApi;
import kr.co.future.sslvpn.xtmconf.XtmConfig;

@Component(name = "frodo-download-servlet")
@Provides
public class DownloadServlet extends HttpServlet implements DownloadService {
	private final Logger logger = LoggerFactory.getLogger(DownloadServlet.class.getName());
	private static final long serialVersionUID = 1L;

	@Requires
	private HttpService httpd;
	
	@Requires
	private XenicsService xenicsService;

	private ConcurrentMap<String, Boolean> tokens = new ConcurrentHashMap<String, Boolean>();
	
	@Requires
	private GlobalConfigApi configApi;
	
	@Requires
	private PushApi pushApi;
	
	@Requires
	private MessageBus msgbus;

	/**
	 * Register servlet to servlet registry of webconsole
	 */
	@Validate
	public void start() {
		String adminCtx = "frodo";
		GlobalConfig config = configApi.getGlobalConfig();
		if (config != null)
			adminCtx = config.getAdminConsoleContext();
			
		HttpContext ctx = httpd.ensureContext(adminCtx);
		ctx.addServlet("download", this, "/download/*");
	}

	/**
	 * Unregister servlet from servlet registry
	 */
	@Invalidate
	public void stop() {
		if (httpd != null){
			String adminCtx = "frodo";
			GlobalConfig config = configApi.getGlobalConfig();
			if(config != null)
				adminCtx = config.getAdminConsoleContext();
			HttpContext ctx = httpd.ensureContext(adminCtx);
			ctx.removeServlet("download");
		}
	}

	@Override
	public void addDownloadToken(String token) {
		tokens.put(token, true);
	}

	@Override
	public void removeDownloadToken(String token) {
		tokens.remove(token);
	}

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException {
		String path = req.getParameter("path");
		String token = req.getParameter("token");

		if (path == null || path.contains("..") || (!path.endsWith(".gat") && !path.endsWith(".tar.gz"))) {
			try {
				resp.sendError(404);
				logger.trace("frodo core: path should be not null, path [{}], token [{}]", path, token);
			} catch (IOException e) {
				logger.error("frodo core: cannot send 404 error", e);
			}
			return;
		}

		path = XtmConfig.UTM_LOG + "policy_back/" + path;

		FileInputStream is = null;
		ServletOutputStream os = null;
		File f = null;

		try {
			if (token == null)
				throw new IllegalStateException("null download token");

			if (!tokens.containsKey(token))
				throw new IllegalStateException("download token not found");

			f = new File(path);
			is = new FileInputStream(f);
			os = resp.getOutputStream();
			logger.trace("frodo core: open downstream for {}", f.getAbsolutePath());

			String fileName = f.getName();
			String mimeType = MimeTypes.instance().getByFile(fileName);
			resp.setHeader("Content-Type", mimeType);

			String dispositionType = null;
			if (req.getParameter("force_download") != null) {
				dispositionType = "attachment";

				String userAgent = req.getHeader("User-Agent");
				if (userAgent != null && userAgent.contains("MSIE"))
					mimeType = "application/force-download";
			} else
				dispositionType = "inline";

			// monkey patch (remove known special character)
			fileName = fileName.replaceAll(":", "");

			String encodedFilename = URLEncoder.encode(fileName, "UTF-8").replaceAll("\\+", " ");
			resp.setHeader("Content-Disposition", dispositionType + "; filename=" + encodedFilename);
			resp.setStatus(200);
			resp.setContentLength((int) f.length());

			byte[] b = new byte[8096];

			while (true) {
				int readBytes = is.read(b);
				if (readBytes <= 0)
					break;

				os.write(b, 0, readBytes);
			}
			
//			if(req.getParameter("backup_file") != null)  //파일이 지워지지 않게 함.
//				removeBackupFile(f);

			f.delete();
		} catch (Exception e) {
			if(e.getMessage().equals("excceeded-max-buffer-size")) {
				logger.warn("excceeded max buffer size for downloading [{}]", f.getName());
				
				String sessionId = req.getParameter("session_id");
				if(!Strings.isNullOrEmpty(sessionId)) {
					Session session = msgbus.getSession(sessionId);
					DownloadUtil.pushMsgOfExceededMaxBufSize(pushApi, session);
				}
			} else {
				resp.setStatus(500);
				logger.error("cannot download " + path, e);
			}
		} finally {
			if(f != null && f.exists() && !f.getName().endsWith(".gat")) {
				f.delete();
			}
			
			if (is != null)
				try {
					is.close();
				} catch (IOException e) {
				}
			if (os != null)
				try {
					os.close();
				} catch (IOException e) {
				}
		}
	}
	
	private void removeBackupFile(File f) {
		String tempDir = uncompCollectedBackupFile(f);
		if(tempDir == null) {
			deleteBackupFile(f.getName());
		} else {
			try {
				File dir = new File(tempDir);
				File[] files = dir.listFiles();
				for(File file : files) {
					deleteBackupFile(file.getName());
				}
			} finally {
				String cmd = "rm -rf " + tempDir;
				execCmd(cmd);
			}
		}
	}
	
	private void deleteBackupFile(String fileName) {
		File file = new File(Constants.COMPRESS_LOG_DIR, fileName);
		if(file.isFile()) {
			file.delete();
		}
		file = new File(Constants.COMPRESS_POLICY_DIR, fileName);
		if(file.isFile()) {
			file.delete();
		}
		file = new File(Constants.PRESERVE_DIR, fileName);
		if(file.isFile()) {
			file.delete();
		}
		
		xenicsService.deleteBackupRecord(fileName);
	}
	
	private void execCmd(String cmd) {
		try{
			Process p = Runtime.getRuntime().exec(cmd);
			p.waitFor();
			
			logger.debug("cmd [{}] executed", cmd);
		} catch(Exception e) {
			logger.error("error occurred execute command", e);
			throw new MsgbusException("frodo", "exec-cmd-failed");
		}
	}
	
	private String uncompCollectedBackupFile(File file) {
		String fileName = file.getName();
		String tempDir = null;
		if(fileName.startsWith(Constants.PREFIX_LOG_FILES) || fileName.startsWith(Constants.PREFIX_POLICY_FILES)) {
			String path = file.getAbsolutePath();
			tempDir = file.getParent() + "/tempBackup";
			new File(tempDir).mkdirs();
			String cmd = "tar xzf " + path + " -C " + tempDir;
			execCmd(cmd);
		}
		
		return tempDir;
	}
}
