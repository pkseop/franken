package kr.co.future.sslvpn.userui;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import kr.co.future.sslvpn.core.ActivationListener;
import kr.co.future.sslvpn.core.GlobalConfig;
import kr.co.future.sslvpn.core.GlobalConfigApi;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Invalidate;
import org.apache.felix.ipojo.annotations.Requires;
import org.apache.felix.ipojo.annotations.Validate;
import io.netty.handler.codec.http.HttpHeaders;
import kr.co.future.confdb.ConfigService;
import kr.co.future.dom.api.DefaultEntityEventListener;
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
	 * @Requires private ActivationService activation;
	 */

	@Requires
	private HttpService httpd;

	@Requires
	private ConfigService conf;

	@Requires
	private GlobalConfigApi configApi;
	
	@Requires
	private ClientDownloadConfigApi clientDownloadConfigApi;
	
	private BundleContext bc;
	private File baseDir = null;
	
	private GlobalConfigSynchronizer globalConfigSync;

	public ClientDownloadServlet(BundleContext bc) {
		this.bc = bc;
		globalConfigSync = new GlobalConfigSynchronizer();
	}

	@Validate
	public void start() {
		HttpContext ctx = httpd.ensureContext("frodo");
		ctx.addServlet("client", this, "/*");
		
		GlobalConfig config = configApi.getGlobalConfig();
		if (config != null) {
			String userUiPath = config.getUserUiPath();
			if (userUiPath != null && !userUiPath.trim().isEmpty()) {
				File webDir = new File(userUiPath);

				if (webDir != null && webDir.isDirectory())
					baseDir = webDir;
			}
		}
		configApi.addEntityEventListener(globalConfigSync);
    }

	@Invalidate
	public void stop() {
		if (httpd != null) {
			HttpContext ctx = httpd.ensureContext("frodo");
			ctx.removeServlet("client");
		}
		configApi.removeEntityEventListener(globalConfigSync);
	}

	@Override
	public void onActivated() {
		logger.trace("frodo core: adding client download page to /*");
		HttpContext ctx = httpd.ensureContext("frodo");
		ctx.addServlet("client", this, "/*");
	}

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		//pks: 2014-04-29. made option to handle access to download page.
		if(clientDownloadConfigApi.isEnabled() == false){
			resp.sendError(404);
			return;
		}
		
		String host = req.getHeader("Host");
		String path = req.getPathInfo();
		logger.trace("frodo user ui: get host [{}] path [{}]", host, path);
		String scheme = req.getScheme();
		String agent = req.getHeader("user-agent");
		String extension = req.getRequestURI();

        String clientDownVersion = req.getParameter("version");

        if (path.contains("../")) {
            resp.sendError(404);
            return;
        }

		logger.debug("frodo user ui: scheme [{}], user agent [{}], request uri [{}]", new Object[] { scheme, agent, extension });

		if (useRedirect(scheme, agent, extension, path)) {
			String redirectUri = getRedirectUrl(req, (scheme.equals("http") ? "https://" : "http://"));
			logger.debug("frodo user ui: redirect to [{}]", redirectUri);
			resp.sendRedirect(redirectUri);
			return;
		}

		if (path.isEmpty() || path.equals("/") || path.equals("/index.html")) {
			String index = null;
			if (baseDir != null)
				index = getIndexByConfig(baseDir);

			if (index == null)
				index = getIndex();

			if (index == null) {
				resp.sendError(404);
				return;
			}

			if (host != null)
				index = index.replaceAll("\\$host", host);

			resp.setHeader("Content-Type", "text/html");
			ServletOutputStream os = null;
			try {
				os = resp.getOutputStream();
				os.write(index.getBytes(Charset.forName("utf-8")));
				os.flush();
			} finally {
				if (os != null)
					os.close();
			}
		} else {
			String redirectURL = clientDownloadConfigApi.getRedirectURL();

            logger.debug("req.getPathInfo(): " + req.getRequestURI());
            logger.debug("clientDownVersion: " + clientDownVersion);

            boolean isContainWin = false;
      		if (path.contains("winsetup_" + host + ".exe")) {
      			isContainWin = true;
      			path = path.replace("win", "");
      		}
			if (redirectURL != null && !"".equals(redirectURL) && path.contains("setup_")){
                if (clientDownloadConfigApi.isEnableSpecificDownload() && clientDownVersion == null) {
               	 if(isContainWin == false) {
               		 resp.sendError(404);
               		 return;
               	 }
                }

                String location = String.format("http://%s%s", redirectURL, path);
                String checkURL = String.format("http://%s/checkDownload", redirectURL);
//                if (checkEnableDownload(checkURL)){
                    logger.debug("Client download redirect : {}", location);
                    resp.sendRedirect(location);
                    return;
//                } else if (!clientDownloadConfigApi.isEnableLocalDownload()){
//                    logger.debug("Client download local is not permitted.");
//                    resp.sendError(503);
//                    return;
//                }
			}

            if (path.contains("setup_") && clientDownloadConfigApi.isEnableSpecificDownload() && clientDownVersion == null) {
               if(isContainWin)
               	sendFile(req, resp);
               else {
	            	resp.sendError(404);
	                return;
               }
            } else {
                sendFile(req, resp);
            }
		}
	}
	
	private boolean checkEnableDownload(String checkURL){
		URL url;
        HttpURLConnection con =null;

		try {
			url = new URL(checkURL);
		    con = (HttpURLConnection) url.openConnection();
			con.setConnectTimeout(1000);
			con.connect();
			int code = con.getResponseCode();
			if (code == 200)
				return true;
		} catch (MalformedURLException e) {
			logger.error("checkEnableDownload fail. MalformedURLException.");
		} catch (IOException e) {
			logger.error("checkEnableDownload fail. IOException.");
		} finally {
            if (con != null) {
                logger.debug("HttpConnection disconnect!!");
                con.disconnect();
            }
        }

		return false;
	}
	
	private String getRedirectUrl(HttpServletRequest req, String scheme) {
		String host = req.getHeader(HttpHeaders.Names.HOST);
		if (host == null)
			scheme += req.getLocalAddr() + req.getRequestURI();
		else
			scheme += host + req.getRequestURI();

		if (req.getQueryString() != null && !req.getQueryString().equals(""))
			scheme += "?" + req.getQueryString();
		return scheme;
	}

	private boolean useRedirect(String scheme, String agent, String extension, String path) {
		if (agent == null)
			return false;

		if (scheme.equals("https")) {
			if (agent.contains("Android")) {
				if (checkExtension(extension))
					return true;
				else
					return false;
			} else if(clientDownloadConfigApi.useHttpRedirectForSetupFile() &&
					path.contains("setup") && extension.endsWith(".exe")) {		//redirect setup.exe to http
				return true;
			} else
				return false;
		} else {
			if (agent.contains("Android")) {
				if (checkExtension(extension))
					return false;
				else
					return true;
			} else if (agent.contains("iPhone") || agent.contains("iPad")) {
				return false;
			} else if(clientDownloadConfigApi.useHttpRedirectForSetupFile() &&
					path.contains("setup") && extension.endsWith(".exe")) {		//to download setup.exe by http
				return false;
			} else
				return true;
		}
	}

	private boolean checkExtension(String uri) {
		if (uri.endsWith(".exe"))
			return true;
		else if (uri.endsWith(".tgz"))
			return true;
		else if (uri.endsWith(".apk"))
			return true;
		else if (uri.endsWith(".pdf"))
			return true;

		return false;
	}

	private void sendFile(HttpServletRequest req, HttpServletResponse resp) {
		InputStream is = getInputStream(req);
        ServletOutputStream os = null;

		if (is == null)
			try {
				resp.sendError(404);
			} catch (IOException e1) {

			}
		else {
			resp.setHeader("Content-Type", MimeTypes.instance().getByFile(req.getPathInfo()));

			byte[] buf = new byte[8192];
			int bytesread = 0, bytesBuffered = 0;
			
			try {
				os = resp.getOutputStream();
				
				while ((bytesread = is.read(buf)) > -1) {
					os.write(buf, 0, bytesread);
					bytesBuffered += bytesread;
					
				    if (bytesBuffered > 1024 * 1024) { //flush after 1MB
				        bytesBuffered = 0;
				        os.flush();
				    }
				}
				
				os.flush();

			} catch (IOException e) {
				if (((e instanceof java.net.SocketException) || (e instanceof java.net.SocketException)) && (e.getMessage().equals("Broken pipe") || e.getMessage().contains("Connection reset"))) {
					//Broken pipe나 connection reset 경우에는 에러 메시지를 찍지 않음.
					logger.debug("frodo userui: cannot send file", e);
				} else {
					logger.error("frodo userui: cannot send file", e);	
				}
			} finally {
				try {
                    if (os != null)
                        os.close();
					is.close();
				} catch (IOException e) {
					logger.error("frodo userui: Stream close fail..", e);
				}
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

	private String getIndexByConfig(File baseDir) {
		File index = new File(baseDir, "index.html");
		if (!index.exists())
			return null;

		InputStream is = null;
		byte[] b = new byte[4096];
		StringBuilder sb = new StringBuilder();

		try {
			is = new FileInputStream(index);

			int len;
			while ((len = is.read(b)) != -1)
				sb.append(new String(b, 0, len, Charset.forName("utf-8")));
		} catch (Exception e) {
			logger.error("frodo user ui: cannot load index", e);
			return null;
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

		if (path.contains("_" + host + ".exe")) {
			path = path.replace("_" + host + "", "");
			if(path.contains("win")) {
				path = path.replace("win", "");
			}
		}

		if (baseDir != null) {
			File f = new File(baseDir, path);

			InputStream is = null;

			try {
				is = new FileInputStream(f);
				return is;
			} catch (Exception e) {
				logger.debug("frodo user ui: cannot open file base [{}] path [{}]", baseDir.getAbsolutePath(), path);
			}
		}

		Bundle b = bc.getBundle();
		try {
			URL url = b.getEntry("/WEB-INF" + path);
			return url.openStream();
		} catch (Exception e) {
			logger.debug("frodo user ui: cannot open bundle [{}] resource [{}]", b.getBundleId(), req.getRequestURI());
			return null;
		}
	}
	
	private class GlobalConfigSynchronizer extends DefaultEntityEventListener<GlobalConfig> {
		@Override
		public void entityUpdated(String domain, GlobalConfig config, Object state) {
			if (config != null) {
				String userUiPath = config.getUserUiPath();
				if (userUiPath != null && !userUiPath.trim().isEmpty()) {
					File webDir = new File(userUiPath);

					if (webDir != null && webDir.isDirectory())
						baseDir = webDir;
				}
			}
		}
	}

}
