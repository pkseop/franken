package kr.co.future.sslvpn.core.servlet;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import kr.co.future.sslvpn.core.DupLoginCheck;
import kr.co.future.sslvpn.core.GlobalConfig;
import kr.co.future.sslvpn.core.GlobalConfigApi;
import kr.co.future.sslvpn.core.xenics.XenicsService;
import kr.co.future.sslvpn.core.xenics.impl.Users;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Invalidate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.apache.felix.ipojo.annotations.Validate;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.type.TypeReference;
import org.json.JSONConverter;
import org.json.JSONException;
import kr.co.future.httpd.HttpContext;
import kr.co.future.httpd.HttpService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;

@Component(name = "dup-login-check-servlet")
@Provides
public class DupLoginCheckServlet extends HttpServlet implements DupLoginCheck {
	private static final long serialVersionUID = 1L;
	private final Logger logger = LoggerFactory.getLogger(DupLoginCheckServlet.class);
	
	private final static String URL_FORMAT = "http://%s/dup_login%s";
	
	@Requires
	private HttpService httpd;
	
	@Requires
	private GlobalConfigApi configApi;
	
	@Requires 
	private XenicsService xenicsService;
	
	private List<String> dupLoginCheckNodes = new ArrayList<String>();
	
	private ConcurrentMap<String, String> loginInfoMap = new ConcurrentHashMap<String, String>();
	
	@Override
	public ConcurrentMap<String, String> getLoginInfoMap() {
		return loginInfoMap;
	}
	
	@Validate
	public void start() {
		HttpContext ctx = httpd.ensureContext("frodo");
		ctx.addServlet("dup_login", this, "/dup_login/*");
		
		GlobalConfig config = configApi.getGlobalConfig();
		if(config != null && Strings.isNullOrEmpty(config.getDupLoginCheckNodes()) == false) {
			setNodes(config.getDupLoginCheckNodes());
		}
		initializeLoginInfo();
	}

	@Invalidate
	public void stop() {
		if (httpd != null) {
			HttpContext ctx = httpd.ensureContext("frodo");
			ctx.removeServlet("dup_login");
		}
		sendLogoutAll();
	}
	
	@Override
	public void setUseDuplicateLoginCheck(boolean use) {
		if(use) {
			initializeLoginInfo();
		} else {
			loginInfoMap.clear();
			sendLogoutAll();
		}
	}
	
	private void initializeLoginInfo() {
		if(useDuplicateLoginCheck() == false)
			return;
		
		loginInfoMap.clear();
		logger.info("duplogin init thread start.");
		new Thread(new Runnable() {
			public void run() {
				logger.info("duplogin init start.");
				fetchLoginInfoFromNodes();
				logger.info("duplogin fetchLoginInfoFromNodes end.");
				initLocalLoginInfo();
				logger.info("duplogin initLocalLoginInfo end.");
			}
		}).start();
	}
	
	private void fetchLoginInfoFromNodes() {
		try{
			for(String node : dupLoginCheckNodes) {
				String url = String.format(URL_FORMAT, node, "/retrieve_all_info");
				StringBuilder sb = new StringBuilder();
				if(sendGetRequest2(url, sb) && sb.toString().length() > 3) { //it it receives "{}" then skip
					ObjectMapper mapper = new ObjectMapper();
					HashMap<String, String> m = new HashMap<String, String>();
					
					logger.trace("fetched from node=[{}]. data=[{}]", new Object[]{node, sb.toString()});
					
					m = mapper.readValue(sb.toString(), new TypeReference<HashMap<String,String>>(){});
					for(Iterator<Map.Entry<String, String>> iter = m.entrySet().iterator(); iter.hasNext();) {
						Map.Entry<String, String> entry = iter.next();
						loginInfoMap.put(entry.getKey(), entry.getValue().equals("localhost") ? node : entry.getValue());
					}
					break;
				}
			}
		} catch (Exception e) {
			logger.error("fetch login info failed", e);
		}
	}
	
	private void initLocalLoginInfo() {
		List<Users> list = xenicsService.getConnectedTunnelInfo();
		StringBuilder sb = new StringBuilder();
		
		
		for(Users user : list) {
			loginInfoMap.put(user.user_id, "localhost");
			if(sb.length() > 0) {
				sb.append(",");
			}
			sb.append(user.user_id);
		}
		if(sb.length() > 0) {
			for(String node : dupLoginCheckNodes) {
				String url = String.format(URL_FORMAT, node, "/update_data");
				sendPostRequest(url, sb.toString());
			}
		}
	}
	
	@Override
	public boolean setNodes(String nodes) {
		try {
			String[] arr = nodes.split(",");
			dupLoginCheckNodes.clear();
			for(String node : arr) {
				dupLoginCheckNodes.add(node.trim());
			}
		} catch (Exception e) {
			logger.info("error occurred whild set duplicate login check nodes", e);
			return false;
		}
		return true;
	}
	
	@Override
	public boolean useDuplicateLoginCheck() {
		GlobalConfig config = configApi.getGlobalConfig();
		if(config != null) {
			Boolean b = config.getUseDupLoginCheck();
			if(b == null)
				return false;
			return b;
		}
		else 
			return false;
	}
	
	@Override
	public boolean isBlock() {
		GlobalConfig config = configApi.getGlobalConfig();
		if(config != null) {
			Boolean b = config.getBlockDupLoginCheck();
			if(b == null)
				return false;
			return b;
		}
		else 
			return false;
	}

	@Override
	public boolean isDuplicateLogin(String loginName) {
		if(loginInfoMap.containsKey(loginName))
			return true;
		return false;
	}
	
	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		if(verifyRequest(req) == false) {
			resp.sendError(404);
			return;
		}
		
		try {
			String path = req.getPathInfo();
			String remote = req.getRemoteAddr();
			if(path.equals("/login")) {
				String loginName = req.getParameter("login_name");
				loginInfoMap.put(loginName, remote);
			} else if(path.equals("/logout")) {
				String loginName = req.getParameter("login_name");
				loginInfoMap.remove(loginName);
			} else if(path.equals("/remove_node")) {
				String node = req.getParameter("node");
				removeLoginInfoOfNode(node);
			} else if(path.equals("/logout_all")) {
				removeLoginInfoOfNode(remote);
			} else if(path.equals("/retrieve_all_info")) {
				retrieveAllInfo(req, resp);
			} else if(path.equals("/kill_dup_login_tunnel")) {
				String loginName = req.getParameter("login_name");
				if(loginInfoMap.get(loginName).equals("localhost") && xenicsService.isAlreadyLoggedin(loginName)) {
					xenicsService.killDuploginTunnel(loginName);
				}
			}
		} catch (Exception e) {
			logger.error("error occurred while duplicate login info sync", e);
			resp.sendError(500);
		}
	}
	
	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		if(verifyRequest(req) == false) {
			resp.sendError(404);
			return;
		}
		
		try {
			String path = req.getPathInfo();
			if(path.equals("/update_data")) {
				updateData(req, resp);
			}
		} catch (Exception e) {
			logger.error("error occurred while duplicate login info sync", e);
			resp.sendError(500);
		}
		
	}
	
	private void updateData(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		StringBuilder sb = new StringBuilder();
		String line = null;
		BufferedReader reader = req.getReader();
		while ((line = reader.readLine()) != null)
			sb.append(line);
		
		if(sb.length() > 0) {
			String[] loginNames = sb.toString().split(",");
			String remote = req.getRemoteAddr();
			for(String loginName : loginNames)
				loginInfoMap.put(loginName, remote);
		}
	}
	
	private void retrieveAllInfo(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		try {
			PrintWriter out = resp.getWriter();
			HashMap<String, String> m = new HashMap<String, String>();
			m.putAll(loginInfoMap);
			String allInfo = JSONConverter.jsonize(m);
			out.print(allInfo);
		} catch (JSONException e) {
			logger.error("jsonize error", e);
			resp.sendError(500);
		} 
	}
	
	private boolean verifyRequest(HttpServletRequest req) {
		if(useDuplicateLoginCheck() == false) {
            return false;
		}
		
		String remote = req.getRemoteAddr();
		boolean isNode = false;
		for(String node : dupLoginCheckNodes) {
			if(remote.equals(node)) {
				isNode = true;
				break;
			}
		}
		
		if(isNode == false) {
            return false;
		}
		return true;
	}
	
	private void removeLoginInfoOfNode(String node) {
		for(Iterator<Map.Entry<String, String>> iter = loginInfoMap.entrySet().iterator(); iter.hasNext();) {
			Map.Entry<String, String> entry = iter.next();
			if(node.equals(entry.getValue()))
				iter.remove();
		}
		
	}

	@Override
	public void sendLoginInfo(String loginName) {
		loginInfoMap.put(loginName, "localhost");
		for(String node : dupLoginCheckNodes) {
			String url = String.format(URL_FORMAT, node, "/login?login_name="+loginName);
			sendGetRequest(node, url);
		}
	}

	@Override
	public void sendLogoutInfo(String loginName) {
		loginInfoMap.remove(loginName);
		for(String node : dupLoginCheckNodes) {
			String url = String.format(URL_FORMAT, node, "/logout?login_name="+loginName);
			sendGetRequest(node, url);
		}
	}
	
	public void sendLogoutAll() {
		loginInfoMap.clear();
		logger.info("duplogin sendLogoutAll thread start.");
		new Thread(new Runnable() {
			public void run() {
				for(String node : dupLoginCheckNodes) {
					String url = String.format(URL_FORMAT, node, "/logout_all");
					sendGetRequest(node, url);
				}
			}
		}).start();
	}
	
	private void sendGetRequest(String node, String url) {
		sendGetRequest2(url, null);		
	}
	
	private boolean sendGetRequest2(String url, StringBuilder sb) {
		int responseCode = 0;
		try {
			URL obj = new URL(url);
			HttpURLConnection con = (HttpURLConnection) obj.openConnection();
			con.setConnectTimeout(1000);
			con.setReadTimeout(1000);
	 
			responseCode = con.getResponseCode();
	 
			//print result
			logger.trace("send request to url [{}] return code [{}]", new Object[]{url, responseCode});
			
			if(sb != null && responseCode == 200) {
				return readJson(con, sb);
			}
		} catch (IOException e) {
			logger.error("send request [{}] failed", url);
		}
		return (responseCode == 200);
	}
	
	private boolean sendPostRequest(String url, String data) {
		int responseCode = 0;
		try {
			URL obj = new URL(url);
			HttpURLConnection con = (HttpURLConnection) obj.openConnection();
			con.setConnectTimeout(1000);
			con.setReadTimeout(1000);
			con.setDoOutput (true);
			// optional default is GET
			con.setRequestMethod("POST");
			con.setRequestProperty("Content-Type", "application/json");
			con.setRequestProperty("Content-Length", "" + data.length());
			con.connect();
			
			if(Strings.isNullOrEmpty(data) == false) {
				DataOutputStream output = new DataOutputStream(con.getOutputStream());
				output.writeBytes(data);
                output.flush();
                output.close();
			}
	 
			responseCode = con.getResponseCode();
	 
			//print result
			logger.trace("send request to url [{}] return code [{}]", new Object[]{url, responseCode});
			
		} catch (IOException e) {
			logger.error("send request [{}] failed", url);
		}
		return (responseCode == 200);
	}
	
	public boolean readJson(HttpURLConnection con, StringBuilder sb) {
		try {
			BufferedReader rd = new BufferedReader(new InputStreamReader(con.getInputStream()));
		
		    String line;
		    while ((line = rd.readLine()) != null) {
		       sb.append(line);
		    }
		    logger.trace("retreieve data: [{}]", sb.toString());
		} catch (IOException e) {
			logger.error("retrieve data failed", e);
			return false;
		}
		return true;
	}
	
	@Override
	public void killDuplicateLoginTunnel(String loginName) {
		if(loginInfoMap.get(loginName).equals("localhost") && xenicsService.isAlreadyLoggedin(loginName)) {
			xenicsService.killDuploginTunnel(loginName);
		} else {
			for(String node : dupLoginCheckNodes) {
				String url = String.format(URL_FORMAT, node, "/kill_dup_login_tunnel?login_name="+loginName);
				sendGetRequest(node, url);
			}
		}
	}
	
}
