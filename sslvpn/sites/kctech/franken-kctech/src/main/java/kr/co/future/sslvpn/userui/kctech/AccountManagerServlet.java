package kr.co.future.sslvpn.userui.kctech;

import kr.co.future.sslvpn.core.ActivationListener;
import kr.co.future.sslvpn.model.AccessProfile;
import kr.co.future.sslvpn.model.UserExtension;
import kr.co.future.sslvpn.model.api.AccessProfileApi;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Invalidate;
import org.apache.felix.ipojo.annotations.Requires;
import org.apache.felix.ipojo.annotations.Validate;
import org.json.JSONConverter;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;
import kr.co.future.dom.api.OrganizationUnitApi;
import kr.co.future.dom.api.UserApi;
import kr.co.future.dom.model.OrganizationUnit;
import kr.co.future.dom.model.User;
import kr.co.future.httpd.HttpContext;
import kr.co.future.httpd.HttpService;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Component(name = "frodo-account-manager-kctech")
public class AccountManagerServlet extends HttpServlet implements ActivationListener {
	private static final long serialVersionUID = 1L;
    private final Logger logger = LoggerFactory.getLogger(AccountManagerServlet.class.getName());

    private static final String RESULT = "Result";
    private static final String RESULT_SUCCESS = "Success";
    private static final String RESULT_FAIL = "Fail";
    private static final String RESULT_MESSAGE = "ResultMessage";

    private static final String RESULT_USER_NOT_FOUND = "사용자 아이디가 없습니다.";
    private static final String RESULT_USER_DELETE_SUCCESS = "사용자 삭제 성공 했습니다.";
    private static final String RESULT_USER_DELETE_FAIL = "삭제하려는 사용자가 존재하지 않습니다.";
    private static final String RESULT_USER_ALREADY_EXIST = "생성하려는 사용자가 이미 존재 합니다.";
    private static final String RESULT_LOGIN_NAME_IS_NULL = "로그인 아이디가 파라메터에 존재하지 않습니다.";
    private static final String RESULT_USER_CREATE_SUCCESS = "사용자 생성에 성공 하였습니다.";
    private static final String RESULT_USER_UPDATE_SUCCESS = "사용자 정보 수정에 성공 하였습니다.";

	@Requires
	private HttpService httpd;
	private BundleContext bc;
	
	@Requires
	private OrganizationUnitApi orgUnitApi;
	
	@Requires
	private UserApi domUserApi;

    @Requires
    private AccessProfileApi accessProfileApi;
	
	@Requires
	private kr.co.future.sslvpn.model.api.UserApi userApi;
	
	private ExecutorService threadPool;

	public AccountManagerServlet(BundleContext bc) {
		this.bc = bc;
	}

	@Validate
	public void start() {
		HttpContext ctx = httpd.ensureContext("frodo");
		ctx.addServlet("account", this, "/kctech/account/*");
		threadPool = Executors.newCachedThreadPool();
	}

	@Invalidate
	public void stop() {
		threadPool.shutdown();
		
		if (httpd != null) {
			HttpContext ctx = httpd.ensureContext("frodo");
			ctx.removeServlet("account");
		}
	}

	@Override
	public void onActivated() {
		logger.trace("frodo core: adding account management page to /kctech/account/*");
		HttpContext ctx = httpd.ensureContext("frodo");
		ctx.addServlet("account", this, "/kctech/account/*");
	}

	@SuppressWarnings("resource")
	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		String host = req.getHeader("Host");
		String path = req.getPathInfo();
		logger.trace("frodo account kctech: get host [{}] path [{}]", host, path);

		PrintWriter out = null;
		
		if (path.equals("/deleteUser")) {
			setHttpServletResponseHeader(resp);
			
			out = resp.getWriter();
			
			String loginName = req.getParameter("loginName");
			
			logger.debug("recevice loginName : " + loginName);
			
			Map<String, Object> returnJsonMsg = new HashMap<String, Object>();
			
			if (loginName == null || loginName.length() == 0) {
                returnJsonMsg.put(RESULT, RESULT_FAIL);
                returnJsonMsg.put(RESULT_MESSAGE, RESULT_USER_NOT_FOUND);

                jsonConverter(out, resp, returnJsonMsg);
			} else {
                User user = domUserApi.getUser("localhost", loginName);

                if (user == null) {
                    returnJsonMsg.put(RESULT, RESULT_FAIL);
                    returnJsonMsg.put(RESULT_MESSAGE, RESULT_USER_DELETE_FAIL);

                    jsonConverter(out, resp, returnJsonMsg);
                } else {
                    domUserApi.removeUser("localhost", loginName);

                    returnJsonMsg.put(RESULT, RESULT_SUCCESS);
                    returnJsonMsg.put(RESULT_MESSAGE, RESULT_USER_DELETE_SUCCESS);

                    jsonConverter(out, resp, returnJsonMsg);
                }
            }
		}

	}
	
	private void setHttpServletResponseHeader(HttpServletResponse resp) {
		resp.setHeader("Access-Control-Allow-Origin", "*");
		resp.setHeader("Access-Control-Allow-Methods", "GET,POST");
		resp.setHeader("Access-Control-Max-Age", "360");
		resp.setHeader("Access-Control-Allow-Headers", "X-PINGOTHER, Origin, X-Requested-With, Content-Type, Accept");
		resp.setHeader("Access-Control-Allow-Credentials", "true");
		resp.setContentType("application/json; charset=UTF-8");
	}
	
	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		if (req.getPathInfo() == null) {
			resp.sendError(404);
			return;
		}
		
		PrintWriter out = null;
		
		String host = req.getHeader("Host");
		String path = req.getPathInfo();
		logger.trace("frodo kctech: get host [{}] path [{}]", host, path);
		
		if (path.equals("/createUser")) {
            String loginName = req.getParameter("loginName");

            setHttpServletResponseHeader(resp);

            out = resp.getWriter();

            logger.debug("received loginName : " + loginName);

            Map<String, Object> returnJsonMsg = new HashMap<String, Object>();

            if (loginName == null || loginName.length() == 0) {
                returnJsonMsg.put(RESULT, RESULT_FAIL);
                returnJsonMsg.put(RESULT_MESSAGE, RESULT_LOGIN_NAME_IS_NULL);

                jsonConverter(out, resp, returnJsonMsg);

                logger.debug("return json result message.");
            } else {
                User user = domUserApi.getUser("localhost", loginName);

                if (user != null) {
                    returnJsonMsg.put(RESULT, RESULT_FAIL);
                    returnJsonMsg.put(RESULT_MESSAGE, RESULT_USER_ALREADY_EXIST);

                    jsonConverter(out, resp, returnJsonMsg);

                    logger.debug("return json result message.");
                } else {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(req.getInputStream(), "UTF-8"));
                    String json = reader.readLine();

                    logger.debug("received json : " + json);

                    JSONTokener tokener = new JSONTokener(json);
                    JSONObject jsonObject = null;

                    try {
                        jsonObject = new JSONObject(tokener);
                    } catch (JSONException e) {
                        logger.error("frodo user kps ui: cannot convert object to json string", e);
                    }

                    Map<String, Object> receivedMsg = JSONConverter.parse(jsonObject);

                    String revLoginName = (String)receivedMsg.get("loginName");
                    String revUserName = (String)receivedMsg.get("userName");
                    String revOrgUnitName = (String)receivedMsg.get("orgUnitName");
                    String revTitle = (String)receivedMsg.get("title");
                    String revEmail = (String)receivedMsg.get("eMail");
                    String revPhoneNumber = (String)receivedMsg.get("phoneNumber");
                    String revPassword = (String)receivedMsg.get("password");
                    String revDescription = (String)receivedMsg.get("description");
                    String revAccessProfileName = (String)receivedMsg.get("accessProfileName");
                    String revStartDate = (String)receivedMsg.get("startDate");
                    String revEndDate = (String)receivedMsg.get("endDate");
                    Integer revDeviceKeyCount = Integer.parseInt((String)receivedMsg.get("deviceKeyCount"));

                    createLocalUser(revLoginName, revPassword, revUserName, revOrgUnitName, revEmail, revTitle, revPhoneNumber, revDescription, revAccessProfileName, revStartDate, revEndDate, revDeviceKeyCount);

                    returnJsonMsg.put(RESULT, RESULT_SUCCESS);
                    returnJsonMsg.put(RESULT_MESSAGE, RESULT_USER_CREATE_SUCCESS);

                    jsonConverter(out, resp, returnJsonMsg);

                    logger.debug("return json result message.");
                }
            }
		} else if (path.equals("/updateUser")) {
            String loginName = req.getParameter("loginName");
            setHttpServletResponseHeader(resp);

            out = resp.getWriter();

            logger.debug("received loginName : " + loginName);

            Map<String, Object> returnJsonMsg = new HashMap<String, Object>();

            if (loginName == null || loginName.length() == 0) {
                returnJsonMsg.put(RESULT, RESULT_FAIL);
                returnJsonMsg.put(RESULT_MESSAGE, RESULT_LOGIN_NAME_IS_NULL);

                jsonConverter(out, resp, returnJsonMsg);
            } else {
                User user = domUserApi.getUser("localhost", loginName);

                if (user == null) {
                    returnJsonMsg.put(RESULT, RESULT_FAIL);
                    returnJsonMsg.put(RESULT_MESSAGE, RESULT_USER_NOT_FOUND);

                    jsonConverter(out, resp, returnJsonMsg);
                } else {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(req.getInputStream(), "UTF-8"));
                    String json = reader.readLine();

                    logger.debug("received json : " + json);

                    JSONTokener tokener = new JSONTokener(json);
                    JSONObject jsonObject = null;

                    try {
                        jsonObject = new JSONObject(tokener);
                    } catch (JSONException e) {
                        logger.error("frodo user kps ui: cannot convert object to json string", e);
                    }

                    Map<String, Object> receivedMsg = JSONConverter.parse(jsonObject);

                    String revUserName = (String)receivedMsg.get("userName");
                    String revOrgUnitName = (String)receivedMsg.get("orgUnitName");
                    String revTitle = (String)receivedMsg.get("title");
                    String revEmail = (String)receivedMsg.get("eMail");
                    String revPhoneNumber = (String)receivedMsg.get("phoneNumber");
                    String revPassword = (String)receivedMsg.get("password");
                    String revDescription = (String)receivedMsg.get("description");
                    String revAccessProfileName = (String)receivedMsg.get("accessProfileName");
                    String revStartDate = (String)receivedMsg.get("startDate");
                    String revEndDate = (String)receivedMsg.get("endDate");
                    Integer revDeviceKeyCount = Integer.parseInt((String)receivedMsg.get("deviceKeyCount"));

                    updateLocalUser(user, revPassword, revUserName, revOrgUnitName, revEmail, revTitle, revPhoneNumber, revDescription, revAccessProfileName, revStartDate, revEndDate, revDeviceKeyCount);

                    returnJsonMsg.put(RESULT, RESULT_SUCCESS);
                    returnJsonMsg.put(RESULT_MESSAGE, RESULT_USER_UPDATE_SUCCESS);

                    jsonConverter(out, resp, returnJsonMsg);
                }
            }
        }

	}

	private static Timestamp getCurrentTimeStamp() {
		java.util.Date today = new java.util.Date();
		return new Timestamp(today.getTime());
	}

    public static java.util.Date getExpireDate(String date) throws ParseException {
        SimpleDateFormat format = new SimpleDateFormat("yyyy.MM.dd HH:mm:ss");
        String strExpireDate = date + " 23:59:59";
        java.util.Date expireDate =  new java.util.Date(format.parse(strExpireDate).getTime());
        return expireDate;
    }
	
	private void jsonConverter(PrintWriter out, HttpServletResponse resp, List<Map<String, Object>> returnJsonMsg) {
		try {
			out.write(JSONConverter.jsonize(returnJsonMsg));
		} catch (JSONException e) {
			logger.debug("frodo user kps ui: cannot convert object to json string", e);
			try {
				resp.sendError(500);
			} catch (IOException e1) {
				logger.error("frodo user kps ui: cannot convert object to json string", e1);
			}
		}
	}
	
	private void jsonConverter(PrintWriter out, HttpServletResponse resp, Map<String, Object> returnJsonMsg) {
		try {
			out.write(JSONConverter.jsonize(returnJsonMsg));
		} catch (JSONException e) {
			logger.debug("frodo kctech: cannot convert object to json string", e);
			try {
				resp.sendError(500);
			} catch (IOException e1) {
				logger.error("frodo kctech: cannot convert object to json string", e1);
			}
		} finally {
            out.close();
        }
    }

	private User createLocalUser(String loginName, String password, String userName, String orgUnitName, String eMail, String title, String phoneNum, String desc, String accessProfileName, String startDateSt, String endDateSt, int deviceKeyCnt) {
		User user;
		OrganizationUnit unit = null;
		if (orgUnitName != null) {
			logger.trace("frodo core: org unit [{}] for login [{}]", orgUnitName, loginName);
			unit = orgUnitApi.findOrganizationUnitByName("localhost", orgUnitName);
			if (unit == null) {
				unit = new OrganizationUnit();
				unit.setName(orgUnitName);
				orgUnitApi.createOrganizationUnit("localhost", unit);

			}
		}

		user = new User();
		user.setOrgUnit(unit);
		user.setLoginName(loginName);
		user.setName(userName);
        user.setTitle(title);
        user.setEmail(eMail);
		user.setPassword(password);
        user.setPhone(phoneNum);
        user.setDescription(desc);
		user.setCreated(new java.util.Date());
		user.setUpdated(new java.util.Date());
		user.setSourceType("local");

		logger.trace("frodo kctech: create local user [{}]", user.toString());

		domUserApi.createUser("localhost", user);

        java.util.Date startDate = null;
        java.util.Date endDate = null;

        UserExtension ext = new UserExtension();
        ext.setUser(user);

        try {
            SimpleDateFormat format = new SimpleDateFormat("yyyy.MM.dd");
            startDate =  new java.util.Date(format.parse(startDateSt).getTime());
        } catch (ParseException e) {
            logger.error("frodo kctech: cannot convert object to json string", e);
        }

        try {
            endDate =  getExpireDate(endDateSt);
        } catch (ParseException e) {
            logger.error("frodo kctech: cannot convert object to json string", e);
        }

        ext.setStartDateTime(startDate);
        ext.setExpireDateTime(endDate);
        ext.setDeviceKeyCountSetting(deviceKeyCnt);

        List<AccessProfile> accessProfileList = accessProfileApi.getAccessProfiles();

        for (AccessProfile accessProfile : accessProfileList) {
            //logger.debug("accessProfile.getName(): " + accessProfile.getName() +", accessProfileName: " + accessProfileName);
            if (accessProfile.getName().equals(accessProfileName)) {
                ext.setProfile(accessProfile);
            }
        }

        ext.setCreateDateTime(new java.util.Date());
        ext.setUpdateDateTime(new java.util.Date());

        userApi.setUserExtension(ext);

		return user;
	}

    private User updateLocalUser(User user, String password, String userName, String orgUnitName, String eMail, String title, String phoneNum, String desc, String accessProfileName, String startDateSt, String endDateSt, int deviceKeyCnt) {
        OrganizationUnit unit = null;
        if (orgUnitName != null) {
            logger.trace("frodo kctech: org unit [{}] for login [{}]", orgUnitName, user.getLoginName());
            unit = orgUnitApi.findOrganizationUnitByName("localhost", orgUnitName);
            if (unit == null) {
                unit = new OrganizationUnit();
                unit.setName(orgUnitName);
                orgUnitApi.createOrganizationUnit("localhost", unit);

            }
        }

        user.setOrgUnit(unit);
        user.setName(userName);
        user.setTitle(title);
        user.setEmail(eMail);

        if (password != null && password.length() > 0)
            user.setPassword(password);

        user.setPhone(phoneNum);
        user.setDescription(desc);
        user.setUpdated(new java.util.Date());
        user.setSourceType("local");

        logger.trace("frodo kctech: update local user [{}]", user.toString());

        if (password != null && password.length() > 0)
            domUserApi.updateUser("localhost", user, true);
        else
            domUserApi.updateUser("localhost", user, false);

        java.util.Date startDate = null;
        java.util.Date endDate = null;

        UserExtension ext = new UserExtension();
        ext.setUser(user);

        try {
            SimpleDateFormat format = new SimpleDateFormat("yyyy.MM.dd");
            startDate =  new java.util.Date(format.parse(startDateSt).getTime());
        } catch (ParseException e) {
            logger.error("frodo kctech: cannot convert object to json string", e);
        }

        try {
            endDate =  getExpireDate(endDateSt);
        } catch (ParseException e) {
            logger.error("frodo kctech: cannot convert object to json string", e);
        }

        ext.setStartDateTime(startDate);
        ext.setExpireDateTime(endDate);
        ext.setDeviceKeyCountSetting(deviceKeyCnt);

        List<AccessProfile> accessProfileList = accessProfileApi.getAccessProfiles();

        for (AccessProfile accessProfile : accessProfileList) {
            if (accessProfile.getName().equals(accessProfileName)) {
                ext.setProfile(accessProfile);
            }
        }

        ext.setUpdateDateTime(new java.util.Date());

        userApi.setUserExtension(ext);

        return user;
    }
}
