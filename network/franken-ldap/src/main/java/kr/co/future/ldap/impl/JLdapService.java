/*
 * Copyright 2011 Future Systems
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package kr.co.future.ldap.impl;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.Security;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.StringTokenizer;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;

import kr.co.future.ldap.LdapConfig;
import kr.co.future.ldap.LdapOrgUnit;
import kr.co.future.ldap.LdapProfile;
import kr.co.future.ldap.LdapServerType;
import kr.co.future.ldap.LdapService;
import kr.co.future.ldap.LdapUser;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.apache.felix.ipojo.annotations.Validate;
import kr.co.future.confdb.Config;
import kr.co.future.confdb.ConfigDatabase;
import kr.co.future.confdb.ConfigService;
import kr.co.future.confdb.Predicates;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.unboundid.asn1.ASN1OctetString;
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
import com.unboundid.ldap.sdk.controls.SimplePagedResultsControl;
import com.unboundid.util.LDAPTestUtils;
import com.unboundid.util.ssl.SSLUtil;

@Component(name = "ldap-service")
@Provides
public class JLdapService implements LdapService {
    private static final int DEFAULT_TIMEOUT = 20000;
    private final Logger logger = LoggerFactory.getLogger(JLdapService.class);

    private int initialConnectionPoolSize = 5;
    private int maxConnectionPoolSize = 10;

    @Requires
    private ConfigService conf;

//    public LDAPConnectionPool ldapConnectionPool;
//    public LDAPConnectionPool ldapsConnectionPool;

    @Validate
    public void start() {
        ConfigDatabase db = getDatabase();
        Config c = db.findOne(LdapConfig.class, null);

        if(c == null) {
            LdapConfig ldapConfig = new LdapConfig();
            ldapConfig.setPageSize(5000);	//default page size.
            db.add(ldapConfig);
        }

//        if (getLdapsProfile() != null) {
//            ldapsConnectionPool = ldapsConnectionPool(getLdapsProfile());
//            LDAPConnectionPoolHealthCheck healthCheck = ldapsConnectionPool.getHealthCheck();
//            logger.info("ldapsConnectionPool healthCheck\n" + healthCheck.toString());
//        }
//
//        if (getLdapProfile() != null) {
//            ldapConnectionPool = ldapConnectionPool(getLdapProfile());
//            LDAPConnectionPoolHealthCheck healthCheck = ldapConnectionPool.getHealthCheck();
//            logger.info("ldapConnectionPool healthCheck\n" + healthCheck.toString());
//        }
    }

    private int getPageSize() {
        ConfigDatabase db = getDatabase();
        Config c = db.findOne(LdapConfig.class, null);
        LdapConfig ldapConfig = c.getDocument(LdapConfig.class);
        return ldapConfig.getPageSize();
    }

    private ConfigDatabase getDatabase() {
        return conf.ensureDatabase("kraken-ldap");
    }

    @Override
    public Collection<LdapProfile> getProfiles() {
        return getDatabase().findAll(LdapProfile.class).getDocuments(LdapProfile.class);
    }

    @Override
    public LdapProfile getProfile(String name) {
        Config c = getDatabase().findOne(LdapProfile.class, Predicates.field("name", name));
        if (c == null)
            return null;
        return c.getDocument(LdapProfile.class);
    }

    @Override
    public void createProfile(LdapProfile profile) {
        if (getProfile(profile.getName()) != null)
            throw new IllegalStateException("already exist");
        getDatabase().add(profile);

        initializeLdapConnectionPool();
    }

    @Override
    public void updateProfile(LdapProfile profile) {
        ConfigDatabase db = getDatabase();
        Config c = db.findOne(LdapProfile.class, Predicates.field("name", profile.getName()));
        if (c == null)
            throw new IllegalStateException("not exist");
        db.update(c, profile);

        initializeLdapConnectionPool();
    }

    @Override
    public void removeProfile(String name) {
        ConfigDatabase db = getDatabase();
        Config c = db.findOne(LdapProfile.class, Predicates.field("name", name));
        if (c == null)
            throw new IllegalStateException("not exist");
        db.remove(c);

        initializeLdapConnectionPool();
    }

    @Override
    public Collection<LdapUser> getUsers(LdapProfile profile) {
        List<LdapUser> users = new ArrayList<LdapUser>();

        LDAPConnection connection = null;
        ASN1OctetString resumeCookie = null;
        int pageSize = getPageSize();
        int count = 0;

        try {
            connection = openLdapConnection(profile);

            while(true) {
                String baseDn = buildBaseDN(profile);
                Filter filter = null;
                if (profile.getServerType() != LdapServerType.ActiveDirectory)
                    filter = Filter.createEqualityFilter("objectClass", "inetOrgPerson");
                else
                    filter = Filter.createPresenceFilter("userPrincipalName");
                String idAttr = profile.getIdAttr() == null ? "uid" : profile.getIdAttr();

                SearchRequest searchRequest = new SearchRequest(baseDn, SearchScope.SUB, filter);
                searchRequest.setControls(new SimplePagedResultsControl(pageSize, resumeCookie));

                logger.debug("kraken-ldap: baseDN [{}]", baseDn);

                SearchResult searchResult = connection.search(searchRequest);
                for (SearchResultEntry entry : searchResult.getSearchEntries()) {
                    logger.debug("kraken-ldap: fetch [{}] entry [{}]", count, entry.getDN());
                    users.add(new LdapUser(entry, idAttr));
                    count++;
                }

                LDAPTestUtils.assertHasControl(searchResult, SimplePagedResultsControl.PAGED_RESULTS_OID);
                SimplePagedResultsControl responseControl = SimplePagedResultsControl.get(searchResult);
                if (responseControl.moreResultsToReturn())
                {
                    // The resume cookie can be included in the simple paged results
                    // control included in the next search to get the next page of results.
                    resumeCookie = responseControl.getCookie();
                }
                else
                {
                    break;
                }
            }
            logger.info("kraken-ldap: profile [{}], total {} ldap users", new Object[]{profile.getName(), count});
        } catch (Exception e) {
            logger.error("kraken-ldap: cannot fetch domain users", e);
            throw new IllegalStateException(e);
        } finally {
            if (connection != null && connection.isConnected())
                connection.close();
        }

        return users;
    }

    @Override
    public LdapUser findUser(LdapProfile profile, String uid) {
        LDAPConnection connection = null;
        try {
            connection = openLdapConnection(profile);
            Filter filter = Filter.createEqualityFilter("sAMAccountName", uid) ;
            if (profile.getServerType() != LdapServerType.ActiveDirectory)
                filter = buildUserFilter(profile, uid);

            String idAttr = profile.getIdAttr() == null ? "uid" : profile.getIdAttr();
            SearchRequest searchRequest = new SearchRequest(buildBaseDN(profile), SearchScope.SUB, filter);
            SearchResult searchResult = connection.search(searchRequest);

            if(searchResult.getEntryCount() == 1) {
                SearchResultEntry entry = searchResult.getSearchEntries().get(0);
                if (logger.isDebugEnabled())
                    logger.debug("kraken-ldap: fetch entry [{}]", entry);
                return new LdapUser(entry, idAttr);
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

    @Override
    public Collection<LdapOrgUnit> getOrgUnits(LdapProfile profile) {
        List<LdapOrgUnit> ous = new ArrayList<LdapOrgUnit>();
        ASN1OctetString resumeCookie = null;
        int pageSize = getPageSize();
        int count = 0;

        while(true) {
            LDAPConnection connection = null;
            try {
                connection = openLdapConnection(profile);

                String baseDn = buildBaseDN(profile);
                SearchRequest searchRequest = new SearchRequest(baseDn, SearchScope.SUB, Filter.createEqualityFilter("objectClass", "organizationalUnit"));
                searchRequest.setControls(new SimplePagedResultsControl(pageSize, resumeCookie));

                SearchResult searchResult = connection.search(searchRequest);

                for (SearchResultEntry entry : searchResult.getSearchEntries()) {
                    logger.debug("kraken-ldap: fetch org unit entry [{}]", entry.getDN());
                    ous.add(new LdapOrgUnit(entry));
                    count++;
                }

                LDAPTestUtils.assertHasControl(searchResult, SimplePagedResultsControl.PAGED_RESULTS_OID);
                SimplePagedResultsControl responseControl = SimplePagedResultsControl.get(searchResult);
                if (responseControl.moreResultsToReturn())
                {
                    // The resume cookie can be included in the simple paged results
                    // control included in the next search to get the next page of results.
                    resumeCookie = responseControl.getCookie();
                }
                else
                {
                    break;
                }
            } catch (Exception e) {
                logger.error("kraken-ldap: cannot fetch domain orgUnits");
                throw new IllegalStateException(e);
            } finally {
                if (connection != null && connection.isConnected())
                    connection.close();
            }


        }

        logger.info("kraken-ldap: profile [{}], total {} ldap org units", new Object[]{profile.getName(), count});

        return ous;
    }

    @Override
    public boolean verifyPassword(LdapProfile profile, String uid, String password) {
        return verifyPassword(profile, uid, password, DEFAULT_TIMEOUT);
    }

    @Override
    public boolean verifyPassword(LdapProfile profile, String uid, String password, int timeout) {
        boolean bindStatus = false;
        if (password == null || password.isEmpty())
            return false;

        LDAPConnection connection = null;
        boolean isAuthenticated = false;
        try {
        	connection = openLdapConnection(profile);
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
            if(searchResult.getEntryCount() == 1) {
                SearchResultEntry entry = searchResult.getSearchEntries().get(0);
                dn = entry.getDN();
            } else {
            	logger.info("can't find user [{}]'s dn", uid);
            	return false;
            }
            logger.info("user [{}] dn is [{}] password [{}], dc [{}]", new Object[]{uid,dn,password,profile.getDc()});
            
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
            if (connection != null && connection.isConnected() ) {
                connection.close();
            }
        }

        return isAuthenticated;
    }

    private Filter buildUserFilter(LdapProfile profile, String uid) {
        Filter filter = null;
        String idAttr = "uid";
        if (profile.getIdAttr() != null)
            idAttr = profile.getIdAttr();

        filter = Filter.createEqualityFilter(idAttr, uid);
        return filter;
    }

//not used code
//    private String getDN(String uid, LdapProfile profile) {
//        LDAPConnection connection = null;
//        try {
//            connection = openLdapConnection(profile);
//            Filter filter = null;
//            if (profile.getServerType() == LdapServerType.ActiveDirectory) {
//                filter = Filter.createEqualityFilter("sAMAccountName", uid);
//            } else {
//                filter = buildUserFilter(profile, uid);
//            }
// 
//            String baseDn = buildBaseDN(profile);
// 
//            SearchRequest searchRequest = new SearchRequest(baseDn, SearchScope.SUB, filter, "");
//            SearchResult searchResult = connection.search(searchRequest);
// 
//            if(searchResult.getEntryCount() == 1) {
//                SearchResultEntry entry = searchResult.getSearchEntries().get(0);
//                return entry.getDN();
//            }
//        } catch (LDAPException e) {
//            e.printStackTrace();
//        } finally {
//                connection.close();
//            }
//        return null;
//    }

    @Override
    public void testLdapConnection(LdapProfile profile, Integer timeout) {
        try {
            if (profile.getX509Certificate() == null) {
                final LDAPConnection connection = new LDAPConnection(profile.getDc(), profile.getPort());
                BindResult result = connection.bind(profile.getAccount(), profile.getPassword());

                if (result.getResultCode().equals(ResultCode.SUCCESS)) {
                    logger.info("kraken ldap: ldap profile [" + profile.getName() + "] connection failed");
                    connection.close();
                }
            } else {
                X509Certificate cert = profile.getX509Certificate();
                KeyStore ks = null;

                ks = KeyStore.getInstance("JKS");
                ks.load(null, null);
                ks.setCertificateEntry("mykey", cert);

                SSLContext ctx = SSLContext.getInstance("TLS");
                Security.addProvider(ctx.getProvider());
                TrustManagerFactory tmf = TrustManagerFactory.getInstance("X509");
                tmf.init(ks);

                TrustManager[] trustManagers = tmf.getTrustManagers();
                SSLUtil sslUtil = new SSLUtil(trustManagers);
                SSLSocketFactory sslSocketFactory = sslUtil.createSSLSocketFactory();
                final LDAPConnection connection = new LDAPConnection(sslSocketFactory);
                connection.connect(profile.getDc(), profile.getPort());
                connection.bind(profile.getAccount(), profile.getPassword());

                if (connection.isConnected()) {
                    logger.info("kraken ldap: ldap profile [" + profile.getName() + "] connection failed");
                    connection.close();
                }
            }
        } catch (KeyStoreException e) {
            logger.error("kraken ldap: ldap profile [" + profile.getName() + "] connection failed", e);
            throw new IllegalStateException("invalid profile [" + profile.getName() + "]", e);
        } catch (CertificateException e) {
            logger.error("kraken ldap: ldap profile [" + profile.getName() + "] connection failed", e);
            throw new IllegalStateException("invalid profile [" + profile.getName() + "]", e);
        } catch (NoSuchAlgorithmException e) {
            logger.error("kraken ldap: ldap profile [" + profile.getName() + "] connection failed", e);
            throw new IllegalStateException("invalid profile [" + profile.getName() + "]", e);
        } catch (IOException e) {
            logger.error("kraken ldap: ldap profile [" + profile.getName() + "] connection failed", e);
            throw new IllegalStateException("invalid profile [" + profile.getName() + "]", e);
        } catch (LDAPException e) {
            logger.error("kraken ldap: ldap profile [" + profile.getName() + "] connection failed", e);
            throw new IllegalStateException("invalid profile [" + profile.getName() + "]", e);
        } catch (GeneralSecurityException e) {
            logger.error("kraken ldap: ldap profile [" + profile.getName() + "] connection failed", e);
            throw new IllegalStateException("invalid profile [" + profile.getName() + "]", e);
        }
    }

    @Override
    public void changePassword(LdapProfile profile, String uid, String newPassword) {
        changePassword(profile, uid, newPassword, DEFAULT_TIMEOUT);
    }

    @Override
    public void changePassword(LdapProfile profile, String uid, String newPassword, int timeout) {

        // newPassword null check
        if (newPassword == null || newPassword.isEmpty())
            throw new IllegalArgumentException("password should be not null and not empty");

        // connection server
        LDAPConnection connection = null;

        try {
            connection = openLdapConnection(profile);
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

            if(searchResult.getEntryCount() == 1) {
                SearchResultEntry entry = searchResult.getSearchEntries().get(0);

                logger.trace("kraken ldap: change password for {}", entry);

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
                    Modification mod = new Modification(ModificationType.REPLACE, "userPassword", newPassword);
                    modifyRequest = new ModifyRequest(entry.getDN(), mod);
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

//    public LDAPConnectionPool ldapsConnectionPool(LdapProfile profile) {
//        try {
//            X509Certificate cert = profile.getX509Certificate();
//            KeyStore ks = null;
//
//            ks = KeyStore.getInstance("JKS");
//            ks.load(null, null);
//            ks.setCertificateEntry("mykey", cert);
//
//            SSLContext ctx = SSLContext.getInstance("TLS");
//            Security.addProvider(ctx.getProvider());
//            TrustManagerFactory tmf = TrustManagerFactory.getInstance("X509");
//            tmf.init(ks);
//
//            TrustManager[] trustManagers = tmf.getTrustManagers();
//            SSLUtil sslUtil = new SSLUtil(trustManagers);
//            SSLSocketFactory sslSocketFactory = sslUtil.createSSLSocketFactory();
//            final LDAPConnection connection = new LDAPConnection(sslSocketFactory);
//            connection.connect(profile.getDc(), profile.getPort());
//            connection.bind(profile.getAccount(), profile.getPassword());
//
//            return new LDAPConnectionPool(connection, initialConnectionPoolSize, maxConnectionPoolSize);
//        } catch (KeyStoreException e) {
//            logger.error("kraken ldap: ldap profile [" + profile.getName() + "] connection failed", e);
//            throw new IllegalStateException("invalid profile [" + profile.getName() + "]", e);
//        } catch (CertificateException e) {
//            logger.error("kraken ldap: ldap profile [" + profile.getName() + "] connection failed", e);
//            throw new IllegalStateException("invalid profile [" + profile.getName() + "]", e);
//        } catch (NoSuchAlgorithmException e) {
//            logger.error("kraken ldap: ldap profile [" + profile.getName() + "] connection failed", e);
//            throw new IllegalStateException("invalid profile [" + profile.getName() + "]", e);
//        } catch (IOException e) {
//            logger.error("kraken ldap: ldap profile [" + profile.getName() + "] connection failed", e);
//            throw new IllegalStateException("invalid profile [" + profile.getName() + "]", e);
//        } catch (GeneralSecurityException e) {
//            logger.error("kraken ldap: ldap profile [" + profile.getName() + "] connection failed", e);
//            throw new IllegalStateException("invalid profile [" + profile.getName() + "]", e);
//        } catch (LDAPException e) {
//            logger.error("kraken ldap: ldap profile [" + profile.getName() + "] connection failed", e);
//            throw new IllegalStateException("invalid profile [" + profile.getName() + "]", e);
//        }
//    }
//
//    public LDAPConnectionPool ldapConnectionPool(LdapProfile profile) {
//        try {
//            final LDAPConnection connection = new LDAPConnection(profile.getDc(), profile.getPort());
//            connection.bind(profile.getAccount(), profile.getPassword());
//
//            return new LDAPConnectionPool(connection, initialConnectionPoolSize, maxConnectionPoolSize);
//        } catch (LDAPException e) {
//            logger.error("kraken ldap: ldap profile [" + profile.getName() + "] connection failed", e);
//            throw new IllegalStateException("invalid profile [" + profile.getName() + "]", e);
//        }
//    }
//
//    public LdapProfile getLdapProfile() {
//        Collection<LdapProfile> profiles = new ArrayList<LdapProfile>();
//
//        profiles.addAll(getProfiles());
//
//        if (profiles.size() == 0) {
//            return null;
//        }
//
//        int count = 0;
//        for(LdapProfile ldapProfile : profiles) {
//            try {
//                if (ldapProfile.getX509Certificate() == null) {
//                    count++;
//                }
//            } catch (KeyStoreException e) {
//                e.printStackTrace();
//            } catch (CertificateException e) {
//                e.printStackTrace();
//            } catch (NoSuchAlgorithmException e) {
//                e.printStackTrace();
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//        }
//
//        if (count == 0) {
//            return null;
//        }
//
//        List<LdapProfile> tempList = new ArrayList<LdapProfile>();
//
//        for (LdapProfile ldapProfile : profiles) {
//            try {
//                if (ldapProfile.getX509Certificate() == null)
//                    tempList.add(ldapProfile);
//            } catch (Exception e) {
//                logger.error("frodo hl auth: getLdapProfiles() : " + e);
//            }
//        }
//
//        Collections.shuffle(tempList, new Random());
//
//        return tempList.get(0);
//    }
//
//    public LdapProfile getLdapsProfile() {
//        Collection<LdapProfile> profiles = new ArrayList<LdapProfile>();
//
//        profiles.addAll(getProfiles());
//
//        if (profiles.size() == 0) {
//            return null;
//        }
//
//        int count = 0;
//        for(LdapProfile ldapProfile : profiles) {
//            try {
//                if (ldapProfile.getX509Certificate() != null) {
//                    count++;
//                }
//            } catch (KeyStoreException e) {
//                e.printStackTrace();
//            } catch (CertificateException e) {
//                e.printStackTrace();
//            } catch (NoSuchAlgorithmException e) {
//                e.printStackTrace();
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//        }
//
//        if (count == 0) {
//            return null;
//        }
//
//        List<LdapProfile> tempList = new ArrayList<LdapProfile>();
//
//        for (LdapProfile ldapProfile : profiles) {
//            try {
//                if (ldapProfile.getX509Certificate() != null)
//                    tempList.add(ldapProfile);
//            } catch (Exception e) {
//                logger.error("frodo hl auth: getLdapProfiles() : " + e);
//            }
//        }
//
//        Collections.shuffle(tempList, new Random());
//
//        return tempList.get(0);
//    }


//    @Override
//    public LDAPConnection getLdapConnection() throws LDAPException {
//        return ldapConnectionPool.getConnection();
//    }
//
//    @Override
//    public LDAPConnection getLdapsConnection() throws LDAPException {
//        return ldapsConnectionPool.getConnection();
//    }
    
    @Override
	public LDAPConnection openLdapConnection(LdapProfile profile) throws LDAPException {
    	if (profile.getDc() == null)
			throw new IllegalArgumentException("ldap domain controller should be not null");

		if (profile.getPort() == null)
			throw new IllegalArgumentException("ldap port should be not null");

		if (profile.getAccount() == null)
			throw new IllegalArgumentException("ldap account should be not null");

		if (profile.getPassword() == null)
			throw new IllegalArgumentException("ldap password should be not null");
			
		if(profile.isCertUse())
			return getLdapsConnection(profile);
		else
			return getLdapConnection(profile);
    }
    
    @Override
    public LDAPConnection getLdapConnection(LdapProfile profile) throws LDAPException {
        LDAPConnection connection = new LDAPConnection(profile.getDc(), profile.getPort());
        connection.bind(profile.getAccount(), profile.getPassword());
        return connection;
    }
 
    @Override
    public LDAPConnection getLdapsConnection(LdapProfile profile) throws LDAPException {
        try {
            X509Certificate cert = profile.getX509Certificate();
            KeyStore ks = null;
 
            ks = KeyStore.getInstance("JKS");
            ks.load(null, null);
            ks.setCertificateEntry("mykey", cert);
 
            SSLContext ctx = SSLContext.getInstance("TLS");
            Security.addProvider(ctx.getProvider());
            TrustManagerFactory tmf = TrustManagerFactory.getInstance("X509");
            tmf.init(ks);
 
            TrustManager[] trustManagers = tmf.getTrustManagers();
            SSLUtil sslUtil = new SSLUtil(trustManagers);
            SSLSocketFactory sslSocketFactory = sslUtil.createSSLSocketFactory();
            final LDAPConnection connection = new LDAPConnection(sslSocketFactory);
            connection.connect(profile.getDc(), profile.getPort());
            connection.bind(profile.getAccount(), profile.getPassword());
 
            return connection;
        } catch (KeyStoreException e) {
            logger.error("kraken ldap: ldap profile [" + profile.getName() + "] connection failed", e);
            throw new IllegalStateException("invalid profile [" + profile.getName() + "]", e);
        } catch (CertificateException e) {
            logger.error("kraken ldap: ldap profile [" + profile.getName() + "] connection failed", e);
            throw new IllegalStateException("invalid profile [" + profile.getName() + "]", e);
        } catch (NoSuchAlgorithmException e) {
            logger.error("kraken ldap: ldap profile [" + profile.getName() + "] connection failed", e);
            throw new IllegalStateException("invalid profile [" + profile.getName() + "]", e);
        } catch (IOException e) {
            logger.error("kraken ldap: ldap profile [" + profile.getName() + "] connection failed", e);
            throw new IllegalStateException("invalid profile [" + profile.getName() + "]", e);
        } catch (GeneralSecurityException e) {
            logger.error("kraken ldap: ldap profile [" + profile.getName() + "] connection failed", e);
            throw new IllegalStateException("invalid profile [" + profile.getName() + "]", e);
        } catch (LDAPException e) {
            logger.error("kraken ldap: ldap profile [" + profile.getName() + "] connection failed", e);
            throw new IllegalStateException("invalid profile [" + profile.getName() + "]", e);
        }
    }

    @Override
    public void initializeLdapConnectionPool() {
//        if (getLdapsProfile() != null) {
//            ldapsConnectionPool = ldapsConnectionPool(getLdapsProfile());
//            LDAPConnectionPoolHealthCheck healthCheck = ldapsConnectionPool.getHealthCheck();
//            logger.info("ldapsConnectionPool healthCheck\n" + healthCheck.toString());
//        }
//
//        if (getLdapProfile() != null) {
//            ldapConnectionPool = ldapConnectionPool(getLdapProfile());
//            LDAPConnectionPoolHealthCheck healthCheck = ldapConnectionPool.getHealthCheck();
//            logger.info("ldapConnectionPool healthCheck\n" + healthCheck.toString());
//        }
    }

    @Override
    public void setInitialConnectionsSize(int initialConnectionPoolSize) {
        this.initialConnectionPoolSize = initialConnectionPoolSize;
    }

    @Override
    public void setMaxConnectionsSize(int maxConnectionPoolSize) {
        this.maxConnectionPoolSize = maxConnectionPoolSize;
    }

    @Override
    public int getInitialConnectionsSize() {
        return initialConnectionPoolSize;
    }

    @Override
    public int getMaxConnectionsSize() {
        return maxConnectionPoolSize;
    }
        
    //pks. 2014-11-10. frodo-ldap-extend's functions moved to here
    
    @Override
	public boolean isAccountExpired(LdapProfile profile, String loginName) {
		LdapUser user = findUser(profile, loginName);
		if (user == null)
			throw new IllegalStateException("cannot found user");

		return isAccountExpired(profile, user);
	}

	@Override
	public boolean isAccountExpired(LdapProfile profile, LdapUser ldapUser) {
		long now = new Date().getTime();

		if (ldapUser.getAccountExpires() == null)
			return false;

		long expire = ldapUser.getAccountExpires().getTime();
		if (expire == 0 || expire == Long.MAX_VALUE)
			return false;

		return (expire - now) > 0 ? false : true;
	}
    
	@Override
	public boolean isPasswordExpired(LdapProfile profile, String loginName) throws Exception {
		LdapUser user = findUser(profile, loginName);
		if (user == null)
			throw new IllegalStateException("cannot found user");

		return isPasswordExpired(profile, user);
	}

	@Override
	public boolean isPasswordExpired(LdapProfile profile, LdapUser ldapUser) throws Exception {
		int userAccountControl = ldapUser.getUserAccountControl();
		if ((userAccountControl & 0x10000) == 0x10000)
			return false;

		if (ldapUser.getPwdLastSet() == null)
			return false;

		LDAPConnection connection = null;

		try {
			connection = openLdapConnection(profile);
			
			String baseDn = buildBaseDN(profile);
			Filter filter = Filter.createEqualityFilter("objectClass", "domain");
			SearchRequest searchRequest = new SearchRequest(baseDn, SearchScope.SUB, filter);
			SearchResult searchResult = connection.search(searchRequest);
			
			for (SearchResultEntry entry : searchResult.getSearchEntries()) {
				try{
					Long maxPwdAge = entry.getAttributeValueAsLong("maxPwdAge");
					if(maxPwdAge == null)
						maxPwdAge = 0L;
					
					long pwdLastSet = ldapUser.getPwdLastSet().getTime();
					maxPwdAge = Math.abs(maxPwdAge) / 10000;
					long remainTime = (pwdLastSet + maxPwdAge) - new Date().getTime();
					return remainTime < 0;
				} catch(Exception e) {					
				}
			}
		} catch (LDAPException e) {
			throw e;
		} catch (Exception e) {
			throw e;
		} finally {
			if (connection != null && connection.isConnected() ) {
                connection.close();
            }
		}

		return false;
	}
}
