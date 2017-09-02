package kr.co.future.sslvpn.auth.sm.impl;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.Map;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;

import kr.co.future.sslvpn.auth.BaseExternalAuthApi;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.apache.felix.ipojo.annotations.Validate;
import org.json.JSONConverter;
import org.json.JSONObject;
import org.json.JSONTokener;

import kr.co.future.rpc.RpcException;
import kr.co.future.sslvpn.auth.sm.SMAuthApi;
import kr.co.future.sslvpn.auth.sm.SMAuthUrl;
import kr.co.future.confdb.Config;
import kr.co.future.confdb.ConfigDatabase;
import kr.co.future.confdb.ConfigService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(name = "seoul-metro-auth-service")
@Provides
class SeoulMetroAuthServiceImpl extends BaseExternalAuthApi implements SMAuthApi {
	private final Logger logger = LoggerFactory.getLogger(SeoulMetroAuthServiceImpl.class);
	private final String USER_AGENT = "Mozilla/5.0";

    @Requires
    private ConfigService conf;

    private String portalUrl;
	
	@Validate
	public void start() {
        ConfigDatabase db = conf.ensureDatabase("sm");
        Config c = db.findOne(SMAuthUrl.class, null);
        if (c != null)
            portalUrl = c.getDocument(SMAuthUrl.class).getUrl();
    }

	@Override
	public Object login(Map<String, Object> props) {
		String userId = (String) props.get("id");
		String userPw = (String) props.get("pw");

		logger.debug("id: " + userId + ", pw: " + userPw);
		

		Map<String, Object> returnMsg = new HashMap<String, Object>();
		
		try {
			String portalResult = connectPortal(userId, userPw);
			
			logger.debug("portalResult: " + portalResult);
			
			JSONTokener tokener = new JSONTokener(portalResult);
			JSONObject jsonObject = new JSONObject(tokener);
			
			Map<String, Object> recevicedMsg = (Map<String, Object>) JSONConverter.parse(jsonObject);
			
			String resultCode = "" + recevicedMsg.get("result");
			
			if (resultCode.equals("0")) {
				returnMsg.put("auth_code", 0);
			} else {
				returnMsg.put("auth_code", 1);
			}
			
			return returnMsg;
		} catch (IOException e) {
			logger.error("login: " + e);
			returnMsg.put("auth_code", -1);
		} catch (Exception e) {
			logger.error("login: " + e);
			returnMsg.put("auth_code", -1);
		}
		
		return returnMsg;
	}

	@Override
	public Map<String, Object> verifyUser(String loginName) {
        Map<String, Object> m = new HashMap<String, Object>();

        if (loginName != null) {
            m.put("name", loginName);
            m.put("result", true);
        } else {
            m.put("result", false);
        }

        return m;
	}

	public byte[] Hash(byte[] input) throws Exception {
		MessageDigest md;
		byte[] out;

		md = MessageDigest.getInstance("SHA1"); 
		out = md.digest(input);

		return out;
	}
	
	public String connectPortal(String userId, String password) throws Exception {
        userId = URLEncoder.encode(userId, "UTF-8");
        password = URLEncoder.encode(password, "UTF-8");

//		String portalUrl = "http://172.16.2.90:5281/portal/mobile/authUser";

        String query = "account="  + userId + "&password=" + password;
		
		logger.info("frodo-auth-sm: connet to " + portalUrl);
		
		URL url = new URL(portalUrl);

		HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setRequestMethod("POST");
        con.setRequestProperty("User-Agent", USER_AGENT);
        con.setRequestProperty("Accept-Language", "en-US,en;q=0.5");
        con.setDoOutput(true);
        con.setDoInput(true);
        con.setAllowUserInteraction(false);

        DataOutputStream dos = new DataOutputStream(con.getOutputStream());
        dos.writeBytes(query);
        dos.flush();
        dos.close();
		
		BufferedReader rd = null;
		String line = null;
		StringBuffer resp = new StringBuffer();
		
		try {
			rd = new BufferedReader(new InputStreamReader(con.getInputStream(), "utf-8"));
			while ((line = rd.readLine()) != null) {
				resp.append(line);
			}
		} catch (Exception e) {
			logger.error("frodo-auth-sm: cannot connect to " + portalUrl, e);
			throw new RpcException("Web server connect failed", e);
		} finally {
			if (rd != null)
				rd.close();
		}

		logger.debug("frodo-auth-sm: connectionHttps reponse data [{}]", resp);

		if (con.getResponseCode() != 200) {
			throw new RpcException("Response code = " + con.getResponseCode() + ", msg = " + con.getResponseMessage());
		}

		return resp.toString();
	}

    @Override
    public boolean setAuthUrl(String url) {
        this.portalUrl = url;
        return true;
    }
}