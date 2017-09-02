package kr.co.future.sslvpn.core.login;

import java.util.ArrayList;
import java.util.List;

import kr.co.future.sslvpn.core.AuthCode;
import kr.co.future.sslvpn.model.AccessGateway;
import kr.co.future.sslvpn.model.AccessProfile;
import kr.co.future.sslvpn.model.AccessGateway.LdapMode;
import kr.co.future.ldap.LdapProfile;
import kr.co.future.ldap.LdapServerType;
import kr.co.future.sslvpn.core.pipeline.BaseError;
import kr.co.future.sslvpn.core.pipeline.PipelineContext;
import kr.co.future.sslvpn.core.pipeline.Stage;
import kr.co.future.sslvpn.core.servlet.AuthServiceServlet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LdapAccountPWCheckStage extends Stage {
	private final Logger logger = LoggerFactory.getLogger(AuthServiceServlet.class.getName());

	@Override
	public void doExecute(PipelineContext context) {
		LoginContext loginContext = (LoginContext)context;
		LoginUtil loginUtil = loginContext.loginUtil;
		
		AccessGateway gw = loginContext.getGw();
		String loginName = loginContext.getLoginName();
		AccessProfile profile = loginContext.getProfile();
		
		try{
			if (gw.getLdapMode() == LdapMode.LDAP_SYNC_EXTEND) {
				List<LdapProfile> profiles = new ArrayList<LdapProfile>();
				LdapProfile defaultProfile = loginUtil.getLdapProfile();
				if (defaultProfile != null)
					profiles.add(defaultProfile);
	
				profiles.addAll(loginUtil.ldap.getProfiles());
				for (LdapProfile ldapProfile : profiles) {
					if (ldapProfile.getServerType() != LdapServerType.ActiveDirectory)
						continue;
	
					try {
						if (loginUtil.ldap.isAccountExpired(ldapProfile, loginName)) {
							loginUtil.fail(AuthCode.Expired, profile);
							return;
						}
						if (loginUtil.ldap.isPasswordExpired(ldapProfile, loginName)) {
							loginUtil.fail(AuthCode.PasswordExpired, profile);
							return;
						}
					} catch (IllegalStateException e) {
						logger.error("frodo core: cannot fetch ldap user, ldap profile [{}]. try next ldap profile", ldapProfile);
					}
				}
			}
		} catch (Exception e) {
			BaseError error = new BaseError("login fail", "cannot login", e);
			loginContext.addError(error);
		}
	}

}
