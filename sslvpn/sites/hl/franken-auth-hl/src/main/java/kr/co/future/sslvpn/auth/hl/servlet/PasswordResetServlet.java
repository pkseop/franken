package kr.co.future.sslvpn.auth.hl.servlet;

import kr.co.future.sslvpn.core.FileRedirectService;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Invalidate;
import org.apache.felix.ipojo.annotations.Requires;
import org.apache.felix.ipojo.annotations.Validate;
import org.json.JSONConverter;
import org.json.JSONException;

import kr.co.future.httpd.HttpContext;
import kr.co.future.httpd.HttpService;
import kr.co.future.sslvpn.auth.hl.HlAuthApi;
import kr.co.future.sslvpn.auth.hl.impl.HlAuthService;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.Map;

import static org.json.JSONConverter.*;

@Component(name = "hl-pw-reset-servlet")
public class PasswordResetServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;
	private final Logger logger = LoggerFactory.getLogger(PasswordResetServlet.class);

	@Requires
	private HttpService httpd;

    @Requires
    private HlAuthApi hlAuthApi;

	private BundleContext bc;

	public PasswordResetServlet(BundleContext bc) {
		this.bc = bc;
	}

	@Validate
	public void start() {
		HttpContext ctx = httpd.ensureContext("frodo");
		ctx.addServlet("hl", this, "/hl/*");
	}

	@Invalidate
	public void stop() {
		if (httpd != null) {
			HttpContext ctx = httpd.ensureContext("frodo");
			ctx.removeServlet("hl");
		}
	}

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) {
        String pathInfo = req.getPathInfo();
        logger.info("pathInfo: " + pathInfo);
        PrintWriter out = null;

        try {
            if (pathInfo.equals("/resetPassword")) {
                String loginName = req.getParameter("login_name");
                if (loginName == null) {
                    logger.error("frodo core: login name is null");
                    resp.sendError(500);
                    return;
                }

                String name = URLDecoder.decode(req.getParameter("name"), "UTF-8");
                logger.debug("name:" + name);
                if (name == null) {
                    logger.error("frodo core: user name is null");
                    resp.sendError(500);
                    return;
                }

                out = resp.getWriter();

                // 사용자의 아이디와 이름을 Ldap에 쿼리하여 입력된 아이디와 이름이 Ldap에 있는 사용자의 정보와 일치하는 지를 검사 한다.
                if (!searchMatchUserName(loginName, name)) {
                    //일치하는 정보가 없을 경우에는 클라이언트에 일치하는 사용자 정보가 없음을 알린다.
                    Map<String, Object> returnMsg = new HashMap<String, Object>();
                    returnMsg.put("login_name", loginName);
                    returnMsg.put("name", name);
                    returnMsg.put("result", "fail");
                    returnMsg.put("reason", "matchInfoNotFound");

                    String resutlMsg = null;
                    try {
                        resutlMsg = jsonize(returnMsg);
                    } catch (JSONException e) {
                        logger.error("frodo hl servlet: cannot convert object to json string", e);
                        resp.sendError(500);
                    }

                    out.print(resutlMsg);
                    logger.info("frodo hl servlet: LoginName [{}], Name [{}] not found in ldap", loginName, name);

                    return;
                }

                if (resetUserPassword(loginName)) {
                    //구Ldap과 신Ldap의 사용자 패스워드를 성공적으로 수정 하였을 경우
                    Map<String, Object> returnMsg = new HashMap<String, Object>();
                    returnMsg.put("login_name", loginName);
                    returnMsg.put("name", name);
                    returnMsg.put("result", "success");
                    returnMsg.put("reason", "resetLdapPasswordSuccess");

                    String resutlMsg = null;
                    try {
                        resutlMsg = jsonize(returnMsg);
                    } catch (JSONException e) {
                        logger.error("frodo hl servlet: cannot convert object to json string", e);
                        resp.sendError(500);
                    }

                    out.print(resutlMsg);

                    logger.info("frodo hl servlet: LoginName [{}], Name [{}] password reset success in ldap", loginName, name);

                    return;
                } else {
                    //하나라도 패스워드 변경을 실패 하였을 경우. 해당 경우 발생 시 로그를 남겨 운영자가 직접 사용자의 Ldap 패스워드를 리셋 시키기로 협의 됨.
                    Map<String, Object> returnMsg = new HashMap<String, Object>();
                    returnMsg.put("login_name", loginName);
                    returnMsg.put("name", name);
                    returnMsg.put("result", "fail");
                    returnMsg.put("reason", "resetLdapPasswordFail");

                    String resutlMsg = null;
                    try {
                        resutlMsg = jsonize(returnMsg);
                    } catch (JSONException e) {
                        logger.error("frodo hl servlet: cannot convert object to json string", e);
                        resp.sendError(500);
                    }

                    out.print(resutlMsg);
                    logger.info("frodo hl servlet: LoginName [{}], Name [{}] password reset fail in ldap", loginName, name);

                    return;
                }
            } else {
                resp.sendError(404);
            }
        } catch (IOException e) {
            if (((e instanceof java.net.SocketException) || (e instanceof java.net.SocketException)) && (e.getMessage().equals("Broken pipe") || e.getMessage().contains("Connection reset"))) {
                //Broken pipe나 connection reset 경우에는 에러 메시지를 찍지 않음.
                logger.debug("frodo hl servlet: ", e);
            } else {
                logger.error("frodo hl servlet: cannot send json message", e);
            }
        } finally {
            out.close();
        }
	}

    private boolean searchMatchUserName(String loginName, String name) {
        //구Ldap과 신Ldap에 입력 받은 사용자 정보와 일치하는 사용자 정보가 있는 지를 확인한다.
        if (hlAuthApi.searchUserNameWithLoginName(loginName, name)) {
            return true;
        } else {
            return false;
        }
    }

    private boolean resetUserPassword(String loginName) {
        //사용자 아이디를 기준으로 디폴트 패스워드로 구Ldap과 신Ldap의 사용자 패스워드를 수정한다.
        if (hlAuthApi.resetUserPassword(loginName)) {
            return true;
        } else {
            return false;
        }
    }
}
