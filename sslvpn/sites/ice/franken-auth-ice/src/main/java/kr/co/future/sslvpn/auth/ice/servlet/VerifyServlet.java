package kr.co.future.sslvpn.auth.ice.servlet;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URLDecoder;
import java.security.spec.KeySpec;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.DESedeKeySpec;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import kr.co.future.sslvpn.auth.ice.IceAuthApi;
import kr.co.future.sslvpn.core.AuthCode;
import kr.co.future.sslvpn.core.log.AuthLog;
import kr.co.future.sslvpn.model.AccessProfile;
import kr.co.future.sslvpn.model.UserExtension;
import kr.co.future.sslvpn.model.api.AccessProfileApi;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Invalidate;
import org.apache.felix.ipojo.annotations.Requires;
import org.apache.felix.ipojo.annotations.Validate;
import org.json.JSONConverter;
import org.json.JSONException;
import kr.co.future.codec.Base64;
import kr.co.future.dom.api.OrganizationUnitApi;
import kr.co.future.dom.api.UserApi;
import kr.co.future.dom.model.OrganizationUnit;
import kr.co.future.dom.model.User;
import kr.co.future.httpd.HttpContext;
import kr.co.future.httpd.HttpService;
import kr.co.future.ldap.LdapProfile;
import kr.co.future.ldap.LdapService;
import kr.co.future.ldap.LdapUser;
import kr.co.future.logstorage.Log;
import kr.co.future.logstorage.LogStorage;
import kr.co.future.logstorage.LogStorageStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.epki.api.EpkiApi;
import com.epki.cert.CertValidator;
import com.epki.cert.X509Certificate;
import com.epki.conf.ServerConf;
import com.epki.crypto.PrivateKey;
import com.epki.exception.EpkiException;
import com.epki.session.SecureSession;

@Component(name = "ice-auth-verify-servlet")
public class VerifyServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private final Logger logger = LoggerFactory.getLogger(VerifyServlet.class.getName());

    private static final String EPKI_WORK_PATH = "/utm/log/epki/gpki/";
    //private static final String EPKI_WORK_PATH = "/tmp/epki/gpki/";

    private static String SERVER_CERT_BASE64 = null;

    @Requires
    private HttpService httpd;

    @Requires
    private IceAuthApi iceAuthApi;

    @Requires
    private UserApi domUserApi;

    @Requires
    private OrganizationUnitApi orgUnitApi;

    @Requires
    private kr.co.future.sslvpn.model.api.UserApi userApi;

    @Requires
    private LdapService ldapApi;

    @Requires
    private AccessProfileApi profileApi;

    @Requires
    private LogStorage storage;

    ServerConf sc;
    PrivateKey priKey;
    X509Certificate cert;
    CertValidator cv;

    @Validate
    public void start() {
        HttpContext ctx = httpd.ensureContext("frodo");
        ctx.addServlet("verify", this, "/verify/*");

        try {
            EpkiApi.initApp(EPKI_WORK_PATH);

            sc = new ServerConf();

            logger.debug("get server cert start..");
            cert = sc.getServerCert(ServerConf.CERT_TYPE_KM);
            logger.debug("get server cert end..");

            logger.info("start get Server Private key file..");
            priKey = sc.getServerPriKey(ServerConf.CERT_TYPE_KM);
            logger.info("end get Server Private key file..");

            // 인증서를 검증하기 위해 CertValidator 객체를 생성합니다.
            logger.debug("create CertValidator start..");
            cv = new CertValidator();
            logger.debug("create CertValidator end..");

            // 클라이언트 인증서를 검증할 옵션을 설정합니다.
            logger.debug("set cert validation option start..");
            cv.setValidateCertPathOption(CertValidator.CERT_VERIFY_FULLPATH);
            cv.setValidateRevokeOption(CertValidator.REVOKE_CHECK_CRL);
            logger.debug("set cert validation option end..");
        } catch (Exception ex) {
            logger.error("e: " + ex);
        }
    }

    @Invalidate
    public void stop() {
        if (httpd != null) {
            HttpContext ctx = httpd.ensureContext("frodo");
            ctx.removeServlet("verify");
        }
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        if (req.getPathInfo() == null) {
            resp.sendError(404);
            return;
        }

        PrintWriter out = null;
        try {
            out = resp.getWriter();

            Map<String, Object> m = new HashMap<String, Object>();
            m.put("result", true);
            m.put("session_id", req.getSession().getId());
            out.write(JSONConverter.jsonize(m));
        } catch (JSONException e) {
            logger.debug("frodo-auth-ice: cannot convert object to json string", e);
            resp.sendError(500);
        } finally {
            if (out != null)
                out.close();
        }
    }

    private void responseJson(HttpServletResponse resp, Map<String, Object> m) throws IOException {
        PrintWriter out = null;
        try {
            out = resp.getWriter();
            out.write(JSONConverter.jsonize(m));
        } catch (JSONException e) {
            logger.error("frodo-auth-ice: cannot convert object to json string", e);
            resp.sendError(500);
        } finally {
            if (out != null)
                out.close();
        }
    }

    private void authCheck(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String loginName = URLDecoder.decode(req.getParameter("id"), "UTF-8");
        //String password = URLDecoder.decode(req.getParameter("pw"), "UTF-8");
        String password = replacer(new StringBuffer(req.getParameter("pw")));

        logger.debug("loginName: [" + loginName + "], password: [" + password + "]");

        if (loginName == null || password == null) {
            logger.error("loginName: " + loginName + ", password: " + password);
            resp.sendError(400);
            return;
        }

        LdapProfile profile = ldapApi.getProfile(iceAuthApi.getConfig().getLdapProfileName());
        LdapUser ldapUser = iceAuthApi.findUser(profile, loginName);

        Map<String, Object> m = new HashMap<String, Object>();

        if (ldapUser == null) {
            setResult(m, 1, 0, null);
        } else {
            User user = domUserApi.findUser("localhost", loginName);
            UserExtension ext = userApi.getUserExtension(user);

            if (ext != null && ext.isLocked() && !isUserUnlocked(req, ext, loginName)) {
                AccessProfile accessProfile = profileApi.determineProfile(loginName);
                int userUnlockTime = accessProfile.getUserUnlockTime();
                m.put("msg", "계정이 잠겼습니다. " + userUnlockTime + "분 후에 다시 로그인 시도해 주시기 바랍니다.");
                setResult(m, 3, 0, null);
            } else {
                boolean isAccountExpired = iceAuthApi.isAccountExpired(loginName);

                if (isAccountExpired) {
                    setResult(m, 4, 0, null);
                } else {
                    boolean result = iceAuthApi.verifyPassword(profile, loginName, password, 0);

                    if (result) {
                        setResult(m, 0, 0, retrieveServerCertBase64Str());
                    } else {
                        if (ext != null) {
                            setResult(m, 2, ext.getLoginFailures() + 1, null);
                            passwordFailUpdate(req, ext, loginName);
                        } else {
                            setResult(m, 2, 1, null);
                        }
                    }
                }
            }
        }

        responseJson(resp, m);
    }

    private boolean isUserUnlocked(HttpServletRequest req, UserExtension ext, String loginName) {
        AccessProfile profile = profileApi.determineProfile(ext.getUser());
        if (ext.isAutoLocked() != null && ext.isAutoLocked()) {
            loginFailReportLog(req, loginName, profile, AuthCode.AutoLocked);
            return false;
        }

        if (profile.getUserUnlockTime() != null && ext.getLastPasswordFailTime() != null) {
            logger.debug("frodo core: use unlock user, login_name [{}], last fail time [{}]", loginName,
                    ext.getLastPasswordFailTime());
            long now = new Date().getTime();
            //Change hours -> minutes
            //long interval = profile.getUserUnlockTime() * 60 * 60 * 1000;
            long interval = profile.getUserUnlockTime() * 60 * 1000;
            if (now - ext.getLastPasswordFailTime().getTime() > interval) {
                ext.setLocked(false);
                ext.setLoginFailures(0);
                ext.setLastPasswordFailTime(null);
                domUserApi.updateUser("localhost", ext.getUser(), false);
                return true;
            } else {/* Bug Fix */
                loginFailReportLog(req, loginName, profile, AuthCode.Locked);
                return false;
            }
        } else {
            loginFailReportLog(req, loginName, profile, AuthCode.Locked);
            return false;
        }
    }

    private void setResult(Map<String, Object> m, int result, int failCnt, String serverCert) {
        m.put("result", result);                //0 = success, 1 = user not found, 2 = password failed, 3 = locked, 4 = account expired
        m.put("fail_cnt", failCnt);
        m.put("server_cert", serverCert);
    }

    private void passwordFailUpdate(HttpServletRequest req, UserExtension ext, String loginName) {
        // update login failure (only if user failure)
        ext.setLoginFailures(ext.getLoginFailures() + 1);
        ext.setLastPasswordFailTime(new Date());

        AccessProfile profile = profileApi.determineProfile(ext.getUser());
        logger.debug("passwd fail count {}/{}", ext.getLoginFailures(), profile.getFailLimitCount());
        if (profile.isUseFailLimit() && profile.getFailLimitCount() <= ext.getLoginFailures()) {
            logger.debug("user {} locked", loginName);
            ext.setLocked(true);
        }

        domUserApi.updateUser("localhost", ext.getUser(), false);
        loginFailReportLog(req, loginName, profile, AuthCode.PasswordFail);
    }

    private void loginFailReportLog(HttpServletRequest req, String loginName, AccessProfile profile, AuthCode authCode) {
        AuthLog log = new AuthLog();

        log.setDate(new Date());
        log.setType("login");
        log.setCode(authCode.getStatus());
        log.setLogin(loginName);
        log.setProfile(profile != null ? profile.getName() : "");
        log.setRemoteIp(req.getRemoteAddr());
        log.setRemotePort(req.getRemotePort());
        log.setNatIp(null);        //사용하지 않으므로 null 처리.
        log.setTunnel(1);            //로그인 실패 시에는 그냥 1로 함.
        log.setOsType(3);            //교육청은 액티브 엑스만 사용하므로
        log.setDeviceKey("1111111111111111111111111111111111111111");

        Log l = log.toLog();
        if (storage.getStatus() == LogStorageStatus.Open)
            storage.write(l);
    }

    private String retrieveServerCertBase64Str() {
        logger.debug("retrieveServerCertBase64Str method start..");
        if (SERVER_CERT_BASE64 == null) {
            try {
                EpkiApi.initApp(EPKI_WORK_PATH);
                ServerConf sc = new ServerConf();
                X509Certificate cert = sc.getServerCert(ServerConf.CERT_TYPE_KM);
                SERVER_CERT_BASE64 = new String(Base64.encode(cert.getCert()));
            } catch (EpkiException e) {
                logger.error("retrieve server cert failed", e);
            }
        }
        logger.debug("retrieveServerCertBase64Str method end..");
        return SERVER_CERT_BASE64;
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String path = req.getPathInfo();
        if (path == null) {
            resp.sendError(404);
            return;
        }

        if (path.equals("/auth")) {    //인증서 체크 전에 id/pw 체크하도록.
            logger.debug("/verify/auth method start..");
            authCheck(req, resp);
            logger.debug("/verify/auth method end..");
            return;
        }

        logger.debug("/verify method start..");
        PrintWriter out = null;

        try {
            out = resp.getWriter();

            String loginName = URLDecoder.decode(req.getParameter("id"), "UTF-8");
            String password = replacer(new StringBuffer(req.getParameter("pw")));

            String strSignedData = req.getParameter("strSignedData");

            if (strSignedData != null) {
                strSignedData = strSignedData.replace(" ", "+");
            } else {
                logger.info("user cert validation cancel..");
                return;
            }

            String sessionId = req.getParameter("session_id");

            logger.trace("frodo auth ice: receive login parameter login_name [{}], password [{}], session_id [{}]", new Object[]{loginName, password, sessionId});

            X509Certificate clientCert;

            EpkiApi.initApp(EPKI_WORK_PATH);

            // SecureSession 객체 및 ServerConf 객체를 생성합니다.
            SecureSession ss = new SecureSession();
            //ServerConf sc = new ServerConf();

            // 세션 ID를 설정합니다.
            ss.setSessionID(sessionId);

            // 인증 요청 메시지를 분석합니다.
            ss.response(cert, priKey, strSignedData);

            // 인증 요청 메시지로부터 클라이언트 인증서를 획득합니다.
            clientCert = ss.getClientCert();

            // 클라이언트 인증서를 검증합니다.
            logger.debug("cert validation start..");
            cv.validate(CertValidator.CERT_TYPE_SIGN, clientCert);
            logger.debug("cert validation end..");

            byte[] subjectDn = clientCert.getSubjectName().getBytes();
            String responseSubjectDn = iceAuthApi.getSubjectDn(loginName);

            Map<String, Object> m = new HashMap<String, Object>();
            if (responseSubjectDn == null) {
                m.put("error", "true");
                m.put("msg", "사용자 주체 DN 이 존재하지 않습니다.");
                out.write(JSONConverter.jsonize(m));
                return;
            }

            byte[] resSubDn = responseSubjectDn.getBytes();
            logger.debug("frodo-auth-ice: client subjectDn=[{}], response ldap subjectDn=[{}]", new Object[]{getHex(subjectDn), getHex(resSubDn)});

            if (!Arrays.equals(subjectDn, resSubDn)) {
                m.put("error", "true");
                m.put("msg", "잘못된 인증서 입니다.");
                out.write(JSONConverter.jsonize(m));
                return;
            }

            Map<String, Object> userInfo = iceAuthApi.verifyUser(loginName);

            String userName = (String) userInfo.get("name");
            String orgUnitName = (String) userInfo.get("org_unit_name");
            String sourceType = "external";

            User user = domUserApi.findUser("localhost", loginName);

            if (user == null) {
                user = createLocalUser(loginName, userName, orgUnitName, sourceType);
            }

            UserExtension ext = userApi.getUserExtension(user);

            if (ext == null) {
                ext = new UserExtension();
                ext.setUser(user);
                ext.setCreateDateTime(new Date());
                ext.setUpdateDateTime(new Date());
            }

            // 현재 시간을 마지막 로그인 시간으로 설정
            ext.setLastLoginTime(new Date());
            userApi.setUserExtension(ext);

            // 사용자의 마지막 로그인 시간을 암호키로 사용
            Date lastLoginAt = ext.getLastLoginTime();

            // 시크릿 키 생성
            SecretKey key = generateSecretKey(lastLoginAt);

            // 암호화
            String encrypt = encrypt(key, password);

            m.put("id", loginName);
            m.put("pw", encrypt);
            m.put("error", "false");

            out.write(JSONConverter.jsonize(m));

            logger.debug("/verify method end..");

            return;
        } catch (EpkiException e) {
            logger.error("frodo-auth-ice: Certificate error", e);
            Map<String, Object> m = new HashMap<String, Object>();
            m.put("error", "true");
            m.put("msg", e.getMessage());
            try {
                out.write(JSONConverter.jsonize(m));
            } catch (JSONException e1) {
                logger.error("error occurred while converting objects to json", e);
            }
            return;
        } catch (JSONException e) {
            logger.error("frodo-auth-ice: cannot convert object to json string", e);
            resp.sendError(500);
        } catch (Exception e) {
            logger.error("frodo-auth-ice: verify faied", e);
            resp.sendError(500);
        } finally {
            if (out != null)
                out.close();
        }

    }

    private SecretKey generateSecretKey(Date lastLoginAt) {
        String algorithm = "DESede";
        byte[] keyValue = lastLoginAt.toString().getBytes();

        SecretKey secretKey = null;
        try {
            KeySpec keySpec = new DESedeKeySpec(keyValue);
            SecretKeyFactory secretKeyFactory = SecretKeyFactory.getInstance(algorithm);
            secretKey = (SecretKey) secretKeyFactory.generateSecret(keySpec);
        } catch (Exception e) {
            logger.debug("frodo-auth-ice: cannot generate secret key", e);
        }

        return secretKey;
    }

    private String encrypt(SecretKey key, String text) {
        Cipher cipher = null;

        try {
            cipher = Cipher.getInstance("DESede/ECB/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE, key);

            byte[] plain = text.getBytes();
            byte[] encrypt = cipher.doFinal(plain);

            char[] encodeBase64 = Base64.encode(encrypt);
            return new String(encodeBase64, 0, encodeBase64.length);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    private User createLocalUser(String loginName, String userName, String orgUnitName, String sourceType) {
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
        user.setCreated(new Date());
        user.setUpdated(new Date());
        user.setSourceType(sourceType);

        logger.trace("frodo auth ice: create local user [{}]", user.toString());

        domUserApi.createUser("localhost", user);
        return user;
    }

    static final String HEXES = "0123456789ABCDEF";

    public static String getHex(byte[] raw) {
        if (raw == null) {
            return null;
        }

        final StringBuilder hex = new StringBuilder(2 * raw.length);

        for (final byte b : raw) {
            hex.append(HEXES.charAt((b & 0xF0) >> 4))
                    .append(HEXES.charAt((b & 0x0F)));
        }

        return hex.toString();
    }

    public static String replacer(StringBuffer outBuffer) {
        String data = outBuffer.toString();
        try {
            data = data.replaceAll("%(?![0-9a-fA-F]{2})", "%25");
            data = data.replaceAll("\\+", "%2B");
            data = URLDecoder.decode(data, "utf-8");
        } catch (Exception e) {
            e.printStackTrace();
        }
        return data;
    }
}
