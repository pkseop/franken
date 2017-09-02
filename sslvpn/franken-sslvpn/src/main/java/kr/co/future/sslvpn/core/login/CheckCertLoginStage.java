package kr.co.future.sslvpn.core.login;

import java.math.BigInteger;
import java.util.Map;

import kr.co.future.dom.model.User;
import kr.co.future.ldap.LdapProfile;
import kr.co.future.ldap.LdapUser;
import kr.co.future.sslvpn.core.AuthCode;
import kr.co.future.sslvpn.core.PasswordHash;
import kr.co.future.sslvpn.core.SqlAuthResult;
import kr.co.future.sslvpn.core.pipeline.BaseError;
import kr.co.future.sslvpn.core.pipeline.PipelineContext;
import kr.co.future.sslvpn.core.pipeline.Stage;
import kr.co.future.sslvpn.core.servlet.AuthServiceServlet;
import kr.co.future.sslvpn.model.AccessGateway;
import kr.co.future.sslvpn.model.AccessGateway.IdentificationMode;
import kr.co.future.sslvpn.model.AccessGateway.LdapMode;
import kr.co.future.sslvpn.model.UserExtension;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CheckCertLoginStage extends Stage {

	private final Logger logger = LoggerFactory.getLogger(AuthServiceServlet.class.getName());
	
	@Override
   public void doExecute(PipelineContext context) {
		LoginContext loginContext = (LoginContext)context;
		if(loginContext.isPwAuthOk())		//don't need to check cert login.
			return;
		
		LoginUtil loginUtil = loginContext.loginUtil;
	   
		String loginName = loginContext.getLoginName();
		AccessGateway gw = loginContext.getGw();
		User user = loginContext.getUser();
		String crl = loginContext.getCrl();
		String subjectCn = loginContext.getSubjectCn();
		String subjectDn = loginContext.getSubjectDn();
		String hashOid = loginContext.getHashOid();
		String idn = loginContext.getIdn();
		byte[] vid = loginContext.getVid();
		byte[] r = loginContext.getR();
		
		Integer loginMethod = loginContext.getLoginMethod();
		AuthCode pwAuthCode = loginContext.getAuthCode();		//password 체크 결과
		UserExtension ext = loginContext.getUserExt();
		
		try{
			BigInteger serial = null;
			if (loginContext.props.containsKey("serial")) {
	            String val = (String) loginContext.props.get("serial");
	            if(val != null && !val.equals(""))
	                serial = new BigInteger(val);
	        }

			if (serial != null) { // cert exists
				if (user == null) {
					if (loginUtil.sqlAuth.isEnabled()) {
						SqlAuthResult result = loginUtil.sqlAuth.verifyUser(loginName);
						if (result.isSuccess())
							user = loginUtil.handleUserInfo(loginName, result.getName(), result.getOrgUnitName(), "sql", user);

					} else if (loginUtil.externalAuth.isEnabled()) {
						Map<String, Object> m = loginUtil.externalAuth.verifyUser(loginName);
						if ((Boolean) m.get("result")) {
							user = loginUtil.handleUserInfo(loginName, (String) m.get("name"), (String) m.get("org_unit_name"), "external",
									user);
						}

					} else if (gw.getLdapMode() == LdapMode.LDAP_SYNC) {
						LdapProfile ldapProfile = loginUtil.getLdapProfile();
						LdapUser ldapUser = loginUtil.ldap.findUser(ldapProfile, loginName);
						if (ldapUser != null)
							user = loginUtil.handleUserInfo(loginName, ldapUser.getDisplayName(), ldapUser.getOrganizationUnitName(), "ldap",
									user);
					} else if (gw.getLdapMode() == LdapMode.LDAP_SYNC_EXTEND) {
						LdapUser ldapUser = loginUtil.findLdapUser(loginName);
						if (ldapUser != null)
							user = loginUtil.handleUserInfo(loginName, ldapUser.getDisplayName(), ldapUser.getOrganizationUnitName(), "ldap",
									user);
					} else {
						loginContext.setAuthCode(AuthCode.UserNotFound);
						loginContext.setUser(user);
						return;
					}
				}

				AuthCode authCode = loginUtil.checkCertLogin(loginMethod, serial, crl, loginName, subjectCn);

				if (authCode == AuthCode.CrlConnectFail) {
					loginContext.setAuthCode(AuthCode.CrlConnectFail);
					loginContext.setUser(user);
					return;
				}

				logger.trace("frodo core: cert login result for [{}] -> {}", loginName, authCode);
				if (!authCode.getStatus().equals("success")) {
					loginContext.setAuthCode(authCode);
					loginContext.setUser(user);
					return;
				}

				if (loginUtil.isLoginMethodWithNPKI(gw, user, loginName)) {
					logger.trace("frodo core: using NPKI");
					if (vid == null) {// not NPKI{
						loginContext.setAuthCode(AuthCode.PolicyMismatch);
						loginContext.setUser(user);
						return;
					}
					// is registered NPKI?
					if (r == null && vid != null) {		//first check of npki auth
						authCode = loginUtil.checkNpki(gw, user, vid, subjectDn, pwAuthCode);
						//initial NPKI identification by subjectDn
						if (vid != null && (ext.getVid() == null || authCode == AuthCode.NpkiFail)
								&& gw.getIdentificationMode() == IdentificationMode.SubjectDn) {
							Boolean result = setSubjectDnVid(loginUtil, ext, loginName, subjectDn, vid, gw.getSubjectDnHashType());
							if (result == null) {
								authCode = AuthCode.SubjectDnNotFound;
							} else if (result == false) {
								authCode = AuthCode.NpkiFail;
							} else 
								authCode = AuthCode.NpkiSuccess;
						}
					} 
					//initial NPKI identification by idn in cert
					else if (r != null && hashOid != null && vid != null &&  idn != null) { 
						if(loginUtil.checkVidIntegrity(vid, idn, r, hashOid)) {	
							ext.setVid(loginUtil.toHex(vid));
							authCode = AuthCode.NpkiSuccess;
						} else {
							authCode = AuthCode.NpkiIdnFail;							
						}
					} else {
						authCode = AuthCode.NpkiIdnFail;
					}
					
					// to preserve radius auth result
					if (authCode.getStatus().equals("success")) {
						loginContext.setAuthCode(pwAuthCode);
						loginContext.setUser(user);
						return;
					} else {
						loginContext.setAuthCode(authCode);
						loginContext.setUser(user);
						return;
					}
				} else {
					logger.trace("frodo core: using private cert");
					if (vid != null) { // cert exists and !NPKI but vid presents
						loginContext.setAuthCode(AuthCode.PolicyMismatch);
						loginContext.setUser(user);
						return;
					} else {
						// to preserve radius auth result - Peter H. Nahm
						//if (pwAuthCode == AuthCode.RadiusSuccess ||
						//	pwAuthCode == AuthCode.SqlSuccess) {
						if (authCode.getStatus().equals("success")) {
							loginContext.setAuthCode(pwAuthCode);
							loginContext.setUser(user);
							return;
						} else {
							loginContext.setAuthCode(authCode);
							loginContext.setUser(user);
							return;
						}
					}
				}
			}
		} catch (Exception e) {
			BaseError error = new BaseError("login fail", "cannot login", e);
			loginContext.addError(error);
		}
   }

	private Boolean setSubjectDnVid(LoginUtil loginUtil, UserExtension ext, String loginName, String subjectDn, byte[] vid, String subjectDnHashType) {
		boolean foundButMismatch = false;

		// NOTE: DN 문자열을 만들 때 순서나 키의 대소문자가 다를 수 있으니 잘 안 되면 파싱해서 비교해야 함
		if (loginUtil.externalAuth.isEnabled()) {
			String remoteSubjectDn = loginUtil.externalAuth.getSubjectDn(loginName);
			if (remoteSubjectDn != null) {
				foundButMismatch = true;
				if (!subjectDn.equals(remoteSubjectDn)) {
					logger.error("frodo core: external auth subject_dn miss match, client [{}] server [{}]", subjectDn,
							remoteSubjectDn);
				} else {
					ext.setVid(loginUtil.toHex(vid));
					return true;
				}
			}
		}

		if (loginUtil.sqlAuth.isEnabled()) {
			String remoteSubjectDn = loginUtil.sqlAuth.getSubjectDn(loginName);
			if (remoteSubjectDn != null) {
				foundButMismatch = true;

				if (subjectDnHashType == null) {
					if (subjectDn.equals(remoteSubjectDn)) {
						ext.setVid(loginUtil.toHex(vid));
						return true;
					} else
						logger.trace("frodo core: sql auth subject_dn miss match, client [{}] server [{}]", subjectDn,
								remoteSubjectDn);
				} else {
					AccessGateway gw = loginUtil.gatewayApi.getCurrentAccessGateway();

					String hashedDn = PasswordHash.makeHash(subjectDnHashType, subjectDn, gw.getSubjectDnEncoding(),
							gw.getSubjectDnCharset());
					if (hashedDn == null) {
						logger.error("frodo core: invalid hash algorithm [{}]", subjectDnHashType);
						return null;
					}

					if (hashedDn.equals(remoteSubjectDn))
						return true;
					else
						logger.trace("frodo core: sql auth hashed subject_dn miss match, client [{}] server [{}]", subjectDn,
								remoteSubjectDn);
				}
			}
		}

		if (ext.getSubjectDn() == null) {
			if (foundButMismatch) {
				logger.debug("frodo core: found but subject_dn mismatch", subjectDn, ext.getSubjectDn());
				return false;
			} else {
				logger.error("frodo core: subjectDn has not found for user [{}]", loginName);
				return null;
			}
		}

		if (!ext.getSubjectDn().equals(subjectDn)) {
			logger.error("frodo core: subjectDn has mismatch [{}] != [{}]", subjectDn, ext.getSubjectDn());
			return false;
		}

		ext.setVid(loginUtil.toHex(vid));
		return true;

	}
}
