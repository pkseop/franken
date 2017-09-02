package kr.co.future.sslvpn.auth.ice.impl;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.security.spec.KeySpec;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.DESedeKeySpec;

import kr.co.future.sslvpn.auth.BaseExternalAuthApi;
import kr.co.future.sslvpn.auth.ice.IceAuthApi;
import kr.co.future.sslvpn.auth.ice.IceConfig;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.apache.felix.ipojo.annotations.Validate;
import kr.co.future.codec.Base64;
import kr.co.future.confdb.Config;
import kr.co.future.confdb.ConfigDatabase;
import kr.co.future.confdb.ConfigService;
import kr.co.future.dom.api.UserApi;
import kr.co.future.dom.model.User;
import kr.co.future.ldap.LdapProfile;
import kr.co.future.ldap.LdapServerType;
import kr.co.future.ldap.LdapService;
import kr.co.future.ldap.LdapUser;
import kr.co.future.rpc.RpcException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.unboundid.ldap.sdk.BindResult;
import com.unboundid.ldap.sdk.Filter;
import com.unboundid.ldap.sdk.LDAPConnection;
import com.unboundid.ldap.sdk.LDAPException;
import com.unboundid.ldap.sdk.Modification;
import com.unboundid.ldap.sdk.ModificationType;
import com.unboundid.ldap.sdk.ModifyRequest;
import com.unboundid.ldap.sdk.ResultCode;
import com.unboundid.ldap.sdk.SearchRequest;
import com.unboundid.ldap.sdk.SearchResult;
import com.unboundid.ldap.sdk.SearchResultEntry;
import com.unboundid.ldap.sdk.SearchScope;

@Component(name = "ice-auth-service")
@Provides
public class IceAuthService extends BaseExternalAuthApi implements IceAuthApi {

    private final Logger logger = LoggerFactory.getLogger(IceAuthService.class.getName());

    private IceConfig config;

    @Requires
    private ConfigService conf;

    @Requires
    private LdapService ldapApi;

    @Requires
    private UserApi domUserApi;

    @Validate
    public void start() {
        // 번들 시작시 설정파일 읽어서 객체 할당
        this.config = getConfig();
    }

    @Override
    public Object login(Map<String, Object> props) {
        if (ldapApi.getProfile(config.getLdapProfileName()) == null) {
            logger.warn("frodo-auth-ice: ldap 'ice' profile not found");
            throw new RpcException("ice auth config not set");
        }

        String loginName = (String) props.get("id");
        String password = (String) props.get("pw");
        String pwChange = (String) props.get("client_otp");  //when trying password change.
        if (pwChange != null && pwChange.equals("password_change")) {
            LdapProfile profile = ldapApi.getProfile(config.getLdapProfileName());
            boolean verifyPassword = verifyPassword(profile, loginName, password, 0);
            Map<String, Object> m = new HashMap<String, Object>();
            if (verifyPassword) {
                m.put("auth_code", 0);
                return m;
            } else {
                m.put("auth_code", 2);
                return m;
            }
        }

        User user = domUserApi.getUser("localhost", loginName);
        Map<String, Object> ext = user.getExt();
        @SuppressWarnings("unchecked")
        Map<String, Object> frodo = (Map<String, Object>) ext.get("frodo");
        Date lastLoginAt = (Date) frodo.get("last_login_at");


        // 시크릿 키 생성
        SecretKey key = generateSecretKey(lastLoginAt);

        // 복호화
        String decryptPassword = decrypt(key, password);

        logger.trace("frodo-auth-ice: loginName=[{}], pw=[{}]", loginName, decryptPassword);

        Map<String, Object> m = new HashMap<String, Object>();
        Map<String, Object> verify = new HashMap<String, Object>();

        // verify user -> active directory check

        verify = verifyUser(loginName);
        boolean isSuccess = (Boolean) verify.get("result");
        if (isSuccess) {
            m.put("name", verify.get("name"));
            m.put("org_unit_name", verify.get("org_unit_name"));

            logger.trace("frodo-auth-ice: ldap success, name=[{}], org unit=[{}]", verify.get("name"),
                    verify.get("org_unit_name"));
        } else {
            // user not found
            m.put("auth_code", 1);
            logger.trace("frodo-auth-ice: ldap verify user [{}] fail", loginName);
            return m;
        }

        LdapProfile profile = ldapApi.getProfile(config.getLdapProfileName());
        boolean verifyPassword = verifyPassword(profile, loginName, decryptPassword, 0);

        if (!verifyPassword) {
            // return password fail
            m.put("auth_code", 2);
            logger.trace("frodo-auth-ice: ldap verify password fail");
            return m;
        }

        logger.trace("frodo-auth-ice: ldap verify password success");

        m.put("auth_code", 0);

        return m;
    }

    @Override
    public Map<String, Object> verifyUser(String loginName) {
        logger.trace("frodo-auth-ice: verifying user [{}]", loginName);

        LdapProfile profile = ldapApi.getProfile(config.getLdapProfileName());
        // LdapUser ldapUser = ldapApi.findUser(profile, loginName);
        LdapUser ldapUser = findUser(profile, loginName);

        Map<String, Object> m = new HashMap<String, Object>();

        if (ldapUser != null) {
            String neisUon = getAttribute(profile, loginName, "neisUon").toString();
            logger.trace("frodo-auth-ice: neisUon [{}]", neisUon);
            m.put("name", ldapUser.getSurname());
            m.put("org_unit_name", "그룹" + neisUon);
            m.put("result", true);
        } else {
            m.put("result", false);
        }
        return m;
    }

    @Override
    public boolean verifyPassword(LdapProfile profile, String uid, String password, int timeout) {
        boolean bindStatus = false;
        if (password == null || password.isEmpty())
            return false;

        password = replacer(new StringBuffer(password));

        LDAPConnection connection = null;
        boolean isAuthenticated = false;
        try {
            connection = ldapApi.openLdapConnection(profile);
            Filter filter = null;
            if (profile.getServerType() == LdapServerType.ActiveDirectory) {
                filter = Filter.createEqualityFilter("sAMAccountName", uid);
            } else {
                filter = buildUserFilter(profile, uid);
            }

            String baseDn = buildBaseDN(profile);

            SearchRequest searchRequest = new SearchRequest(baseDn, SearchScope.SUB, filter, "");
            SearchResult searchResult = connection.search(searchRequest);

            String dn = null;
            if (searchResult.getEntryCount() == 1) {
                SearchResultEntry entry = searchResult.getSearchEntries().get(0);
                dn = entry.getDN();
            } else {
                logger.info("can't find user [{}]'s dn", uid);
                return false;
            }
            logger.info("user [{}] dn is [{}] password [{}], dc [{}]", new Object[]{uid, dn, password, profile.getDc()});

            BindResult result = connection.bind(dn, password);

            if (result.getResultCode().equals(ResultCode.SUCCESS)) {
                isAuthenticated = true;
            }
        } catch (LDAPException le) {
            String message = le.getMessage();
            logger.info("authencation failed. [{}]", message);
        } catch (Exception e) {
            if (!bindStatus)
                throw new IllegalArgumentException("check ldap profile: " + profile.getName(), e);

            return false;
        } finally {
            if (connection != null && connection.isConnected()) {
                connection.close();
            }
        }

        return isAuthenticated;
    }

    @Override
    public LdapUser findUser(LdapProfile profile, String uid) {
        LDAPConnection connection = null;

        try {
            connection = ldapApi.openLdapConnection(profile);

            //Filter filter = Filter.createEqualityFilter("sAMAccountName", uid);
            Filter filter = Filter.createEqualityFilter("ssoid", uid);

            if (profile.getServerType() != LdapServerType.ActiveDirectory) {
                filter = buildUserFilter(profile, uid);
            }

            String idAttr = profile.getIdAttr() == null ? "uid" : profile.getIdAttr();
            SearchRequest searchRequest = new SearchRequest(buildBaseDN(profile), SearchScope.SUB, filter);
            SearchResult searchResult = connection.search(searchRequest);

            if (searchResult.getEntryCount() == 1) {
                SearchResultEntry entry = searchResult.getSearchEntries().get(0);
                if (logger.isDebugEnabled())
                    logger.debug("kraken-ldap: fetch entry [{}]", entry);

                try {
                    return new LdapUser(entry, idAttr);
                } catch (Exception ex) {
                    logger.error("error: " + ex);
                }
            }
        } catch (Exception e) {
            logger.error("kraken-ldap: cannot fetch domain users", e);
            throw new IllegalStateException(e);
        } finally {
            if (connection != null && connection.isConnected())
                connection.close();
        }

        return null;
    }

    private Filter buildUserFilter(LdapProfile profile, String uid) {
        Filter filter = null;
        String idAttr = config.getIdAttr();
        if (profile.getIdAttr() != null)
            idAttr = profile.getIdAttr();

        idAttr = "ssoid";
        filter = Filter.createEqualityFilter(idAttr, uid);

        return filter;
    }

    @Override
    public String getSubjectDn(String loginName) {
        LdapProfile profile = ldapApi.getProfile(config.getLdapProfileName());
        String subjectDn = getAttribute(profile, loginName, "sftsCredentiallv3");
        return subjectDn;
    }

    @Override
    public String getIdn(String loginName) {
        return null;
    }

    @Override
    public void changePassword(String uid, String newPassword) {
        logger.debug("loginName: [" + uid + "], password: [" + newPassword + "]");

//        try {
//            newPassword = URLDecoder.decode(newPassword, "UTF-8");
//        } catch (UnsupportedEncodingException e) {
//            e.printStackTrace();
//        }
        newPassword = replacer(new StringBuffer(newPassword));

        logger.debug("loginName: [" + uid + "], password: [" + newPassword + "]");

        LdapProfile profile = ldapApi.getProfile(config.getLdapProfileName());
        // newPassword null check
        if (newPassword == null || newPassword.isEmpty())
            throw new IllegalArgumentException("password should be not null and not empty");

        // connection server
        LDAPConnection connection = null;

        try {
            connection = ldapApi.openLdapConnection(profile);
            // set filter
            Filter filter = null;
            if (profile.getServerType() == LdapServerType.ActiveDirectory) {
                filter = Filter.createEqualityFilter("sAMAccountName", uid);
            } else {
                filter = buildUserFilter(profile, uid);
            }
            String baseDn = buildBaseDN(profile);

            SearchRequest searchRequest = new SearchRequest(baseDn, SearchScope.SUB, filter, "");
            SearchResult searchResult = connection.search(searchRequest);

            if (searchResult.getEntryCount() == 1) {
                SearchResultEntry entry = searchResult.getSearchEntries().get(0);

                logger.trace("ice auth: change password for {}", entry);

                ModifyRequest modifyRequest = null;
                if (profile.getServerType() == LdapServerType.ActiveDirectory) {
                    // ActiveDirectory - newPassword enclosed in quotation marks and
                    // UTF-16LE encoding
                    byte[] quotedPasswordBytes = null;

                    String tmpPassword = ldapPasswordEscape(newPassword);
                    try {
                        String quotedPassword = '"' + tmpPassword + '"';
                        quotedPasswordBytes = quotedPassword.getBytes("UTF-16LE");
                    } catch (UnsupportedEncodingException e) {
                        throw new IllegalStateException(e);
                    }

                    Modification mod = new Modification(ModificationType.REPLACE, "unicodePwd", quotedPasswordBytes);
                    modifyRequest = new ModifyRequest(entry.getDN(), mod);

                } else {
                    Modification mod1 = new Modification(ModificationType.REPLACE, "userPassword", newPassword);

                    SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
                    Modification mod2 = new Modification(ModificationType.REPLACE, "passLastchgymd", sdf.format(new Date()));

                    List<Modification> mods = new ArrayList<Modification>();
                    mods.add(mod1);
                    mods.add(mod2);

                    modifyRequest = new ModifyRequest(entry.getDN(), mods);
                }

                // modify
                connection.modify(modifyRequest);
            }

        } catch (Exception e) {
            throw new IllegalStateException("cannot change password, profile=" + profile.getName() + ", uid=" + uid, e);
        } finally {
            if (connection != null && connection.isConnected()) {
                connection.close();
            }
        }
    }

    public static String ldapPasswordEscape(String s) {

        for (int i = 0; i < s.length(); i++) {
            s.replaceAll("\"", "\\\"");
        }

        return s;
    }

    private String buildBaseDN(LdapProfile profile) {
        if (profile.getBaseDn() != null)
            return profile.getBaseDn();

        String domain = profile.getDc();
        StringTokenizer t = new StringTokenizer(domain, ".");
        String dn = "";
        int i = 0;
        while (t.hasMoreTokens()) {
            if (i++ != 0)
                dn += ",";

            dn += "dc=" + t.nextToken();
        }

        return dn;
    }

    @Override
    public String getAttribute(String loginName, String attributeName) {
        LdapProfile profile = ldapApi.getProfile(config.getLdapProfileName());
        return getAttribute(profile, loginName, attributeName);
    }

    private String getAttribute(LdapProfile profile, String loginName, String attributeName) {
        // connection server
        LDAPConnection connection = null;

        try {
            connection = ldapApi.openLdapConnection(profile);
            // set filter
            Filter filter = Filter.createEqualityFilter("ssoid", loginName);
            String baseDn = buildBaseDN(profile);

            SearchRequest searchRequest = new SearchRequest(baseDn, SearchScope.SUB, filter);
            SearchResult searchResult = connection.search(searchRequest);
            for (SearchResultEntry entry : searchResult.getSearchEntries()) {
                return entry.getAttributeValue(attributeName);
            }
        } catch (LDAPException e) {
            logger.error("error occurred during fetch attribute", e);
            throw new IllegalStateException("cannot return " + attributeName + ", profile=" + profile.getName() + ", uid="
                    + loginName, e);
        } finally {
            if (connection != null && connection.isConnected()) {
                connection.close();
            }
        }
        return null;
    }

    @Override
    public boolean isPasswordChangeSupported() {
        return true;
    }

    @Override
    public boolean isPasswordExpirySupported() {
        return true;
    }

    @Override
    public long getPasswordExpiry(String loginName) {
        LdapProfile profile = ldapApi.getProfile(config.getLdapProfileName());
        String lastChanged = getAttribute(profile, loginName, "passLastchgymd");
        String limit = getAttribute(profile, loginName, "passwChgstdrvalue");

        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
        try {
            Date passwordLastChanged = sdf.parse(lastChanged);
            Integer passwordChangeLimit = Integer.parseInt(limit);

            return (passwordChangeLimit * 86400) - ((new Date().getTime() - passwordLastChanged.getTime()) / 1000);

        } catch (ParseException e) {
            logger.error("frodo-auth-ice: invalide date format [{}]", lastChanged);
            // loginName, date value,
            throw new IllegalStateException("login_name: " + loginName + "invalide date format: " + lastChanged);
        }
    }

    @Override
    public long getAccountExpiry(String loginName) {
        LdapProfile profile = ldapApi.getProfile(config.getLdapProfileName());
        String userEndymd = getAttribute(profile, loginName, "userEndymd");

        SimpleDateFormat format = new SimpleDateFormat("yyyyMMddHHmmss");
        Date end = null;

        try {
            end = format.parse(userEndymd + "235959");
            //Long returnDate = end.getTime() - new Date().getTime();

            //logger.debug("returnDate: " + returnDate + ", days: " + returnDate / (24 * 60 * 60 * 1000));

            return end.getTime();
        } catch (ParseException e) {        //에러가 날 경우엔 우선 접속이 가능하도록 long 최대 값을 리턴함.
            logger.error("date parsing error", e);
            return Long.MAX_VALUE;
        }
    }

    @Override
    public boolean isAccountExpirySupported() {
        return true;
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

    private static String decrypt(SecretKey key, String text) {
        Cipher cipher = null;

        try {
            cipher = Cipher.getInstance("DESede/ECB/PKCS5Padding");
            cipher.init(Cipher.DECRYPT_MODE, key);

            byte[] encrypt = Base64.decode((text.toCharArray()));
            byte[] decrypt = cipher.doFinal(encrypt);

            String result = new String(decrypt, 0, decrypt.length);
            return result;
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    @Override
    public IceConfig getConfig() {
        ConfigDatabase db = conf.ensureDatabase("frodo-auth-ice");
        Config c = db.findOne(IceConfig.class, null);
        if (c != null)
            return c.getDocument(IceConfig.class);
        return null;
    }

    @Override
    public void setConfig(IceConfig config) {
        ConfigDatabase db = conf.ensureDatabase("frodo-auth-ice");
        Config c = db.findOne(IceConfig.class, null);
        if (c == null) {
            db.add(config);
        } else {
            db.update(c, config, true);
        }
        this.config = config;
    }

    @Override
    public boolean isAccountExpired(String loginName) {
        LdapProfile profile = ldapApi.getProfile(config.getLdapProfileName());
        String userPosblymd = getAttribute(profile, loginName, "userPosblymd");
        String userEndymd = getAttribute(profile, loginName, "userEndymd");

        SimpleDateFormat format = new SimpleDateFormat("yyyyMMddHHmmss");
        Date begin = null, end = null;
        try {
            begin = format.parse(userPosblymd + "000000");
            end = format.parse(userEndymd + "235959");
        } catch (ParseException e) {        //에러가 날 경우엔 우선 접속이 가능하도록 false를 리턴함.
            logger.error("date parsing error", e);
            return false;
        }

        Date now = new Date();
        if (now.after(begin) && now.before(end))
            return false;
        else
            return true;
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
