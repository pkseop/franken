package kr.co.future.sslvpn.auth.ncc.impl;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import kr.co.future.sslvpn.auth.ncc.NccAuthApi;
import kr.co.future.sslvpn.auth.ncc.NccConfig;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.apache.felix.ipojo.annotations.Validate;

import kr.co.future.confdb.Config;
import kr.co.future.confdb.ConfigDatabase;
import kr.co.future.confdb.ConfigService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(name = "ncc-auth-service")
@Provides
public class NccAuthService implements NccAuthApi {

	private final Logger logger = LoggerFactory.getLogger(NccAuthService.class.getName());

	@Requires
	private ConfigService conf;

	private NccConfig config;

	public static void main(String[] args) {
		System.out.println(now());
	}

	@Validate
	public void start() {
		config = getConfig();
	}

	@Override
	public Object login(Map<String, Object> props) {
		String id = (String) props.get("id");
		String password = (String) props.get("pw");

		Map<String, Object> m = new HashMap<String, Object>();
		Map<String, Object> verify = null;
		try {
			verify = verifyPassword(id, password);
		} catch (Throwable t) {
			m.put("auth_code", -2);
			logger.error("frodo-auth-ncc: cannot verify password for " + id, t);
			return m;
		}

		if ((Boolean) verify.get("result")) {
			m.put("name", verify.get("name"));
			m.put("auth_code", 0);
			logger.trace("frodo-auth-ncc: login [{}] success", id);
		} else {
			m.put("auth_code", 2); // password fail
			logger.trace("frodo-auth-ncc: login [{}] fail", id);
		}

		return m;
	}

	private String getResult(String id, String password, String type) {

		if (config == null) {
			logger.warn("frodo-auth-ncc: config not set");
			return null;
		}

		logger.debug("frodo-auth-ncc: call Result http method [{}]", type);

		String url = config.getUrl();
		String result = null;

		if (type.equals("GET"))
			url += "?id=" + id;

		// milliseconds to seconds
		int timeout = config.getTimeout() * 1000;
		BufferedReader buf = null;
		HttpURLConnection con = null;
		OutputStream os = null;
		try {
			con = (HttpURLConnection) new URL(url).openConnection();

			if (type.equals("POST"))
				con.setDoOutput(true);
			else
				con.setDoOutput(false);

			con.setDoInput(true);
			con.setUseCaches(false);
			HttpURLConnection.setFollowRedirects(false);
			con.setRequestMethod(type);

			con.setConnectTimeout(timeout);
			con.setReadTimeout(timeout);

			if (type.equals("POST")) {
				con.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
				con.setRequestProperty("User-Agent", "Mozilla/4.0(compatible; MSIE 5.5; Windows NT 5.0)");
			}

			con.connect();

			if (type.equals("POST")) {
				os = con.getOutputStream();
				PrintWriter out = new PrintWriter(os);
				out.write("id=" + id + "&pwd=" + password);
				out.flush();
			}

			InputStreamReader in = new InputStreamReader(con.getInputStream(), Charset.forName("utf-8"));
			buf = new BufferedReader(in);

			String inputLine = null;
			result = "";

			while ((inputLine = buf.readLine()) != null) {
				result += inputLine;
			}
		} catch (MalformedURLException e) { // url
			logger.error("frodo-auth-ncc: MalformedURL error", e);
		} catch (ProtocolException e) {
			logger.error("frodo-auth-ncc: protocol error", e);
		} catch (IOException e) {
			logger.error("frodo-auth-ncc: io error", e);
		} finally {
			try {
				buf.close();
			} catch (IOException e) {
				logger.error("frodo-auth-ncc: buf close error", e);
			}

			try {
				if (os != null)
					os.close();
			} catch (IOException e) {
				logger.error("frodo-auth-ncc: buf close error", e);
			}
		}

		return result;
	}

	@Override
	public Map<String, Object> verifyUser(String loginName) {
		if (config == null) {
			logger.warn("frodo-auth-ncc: config not set");
			return null;
		}

		logger.trace("frodo-auth-ncc: trying to verify user [{}]", loginName);

		String result = "";
		result = getResult(loginName, null, "GET");
		logger.debug("frodo-auth-ncc: user search result [{}]", result);
		return splitResult(result, "/", ":");
	}

	private Map<String, Object> verifyPassword(String id, String password) {
		if (config == null) {
			logger.warn("frodo-auth-ncc: config not set");
			return null;
		}

		logger.trace("frodo-auth-ncc: trying to verify [{}]'s password", id);

		String result = getResult(id, password, "POST");
		logger.debug("frodo-auth-ncc: password match result [{}]", result);

		return splitResult(result, "/", ":");
	}

	/**
	 * 계정이 존재하는 경우 result:true/username:홍현철
	 * 
	 * 계정이 존재하지 않는 경우 result:false
	 */
	private Map<String, Object> splitResult(String str, String c1, String c2) {

		Map<String, Object> m = new HashMap<String, Object>();

		if (str.indexOf(c1) != -1) {

			String[] temp = str.split(c1);

			String[] resultArray = temp[0].split(c2);
			String result = resultArray[1];

			String[] nameArray = temp[1].split(c2);
			String name = nameArray[1];

			m.put("result", Boolean.parseBoolean(result));
			m.put("name", name);

		} else {
			String[] temp = str.split(c2);
			String result = temp[1];

			m.put("result", Boolean.parseBoolean(result));
		}

		return m;
	}

	@Override
	public void setConfig(NccConfig config) {
		ConfigDatabase db = conf.ensureDatabase("ncc");
		Config c = db.findOne(NccConfig.class, null);
		if (c != null) {
			db.update(c, config);
		} else {
			db.add(config);
		}

		this.config = config;

	}

	@Override
	public NccConfig getConfig() {
		ConfigDatabase db = conf.ensureDatabase("ncc");
		Config c = db.findOne(NccConfig.class, null);
		if (c != null)
			return c.getDocument(NccConfig.class);
		return null;
	}

	@Override
	public String getIdn(String id) {
		return null;
	}

	private static int now() {
		Date now = new Date();

		Calendar c = Calendar.getInstance();
		c.set(Calendar.ZONE_OFFSET, 0);
		c.set(Calendar.YEAR, 1970);
		c.set(Calendar.MONTH, 1);
		c.set(Calendar.DAY_OF_YEAR, 1);
		c.set(Calendar.HOUR_OF_DAY, 0);
		c.set(Calendar.MINUTE, 0);
		c.set(Calendar.SECOND, 0);
		c.set(Calendar.MILLISECOND, 0);

		return (int) ((now.getTime() - c.getTime().getTime()) / 1000);
	}

	@Override
	public String getSubjectDn(String loginName) {
		return null;
	}

	@Override
	public boolean isPasswordChangeSupported() {
		return false;
	}

	@Override
	public void changePassword(String account, String newPassword) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean isPasswordExpirySupported() {
		return false;
	}

	@Override
	public long getPasswordExpiry(String loginName) {
		throw new UnsupportedOperationException();
	}

	@Override
	public long getAccountExpiry(String loginName) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean isAccountExpirySupported() {
		return false;
	}

	@Override
	public boolean useSso() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public String getSsoToken(String loginName) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean verifySso(String loginName, String clientIp) {
		// TODO Auto-generated method stub
		return false;
	}

}
