package kr.co.future.sslvpn.core.impl;

import kr.co.future.ldap.LdapProfile;
import kr.co.future.ldap.LdapProfile.CertificateType;
import kr.co.future.ldap.LdapService;
import kr.co.future.sslvpn.core.impl.LdapVerifyHelper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import kr.co.future.sslvpn.core.LiveStatus;
import kr.co.future.sslvpn.xtmconf.network.Radius;

public class LdapVerifyHelper {
	public static LiveStatus testConnection(LdapService ldap, Radius radius) {
		Logger logger = LoggerFactory.getLogger(LdapVerifyHelper.class);

		LdapProfile profile = new LdapProfile();
		try {
			profile.setName("frodo-test");
			profile.setDc(radius.getLdapAddress());
			profile.setAccount(radius.getLdapAccount());
			profile.setPassword(radius.getLdapPassword());
			profile.setBaseDn(radius.getLdapBaseDn());
			profile.setPort(389);
			if (radius.isLdapUseTrustStore() && radius.getLdapTrustStore() != null)
				profile.setTrustStore(CertificateType.X509, radius.getLdapTrustStore());

			if (profile.getDc() == null || profile.getAccount() == null || profile.getPassword() == null)
				return LiveStatus.Disabled;

			if (profile.getDc().isEmpty() || profile.getAccount().isEmpty() || profile.getPassword().isEmpty())
				return LiveStatus.Disabled;

			ldap.testLdapConnection(profile, 5000);
			return LiveStatus.Connected;
		} catch (Exception e) {
			logger.error("frodo core: cannot connect ldap profile [" + profile.toString() + "]", e);
			return LiveStatus.Disconnected;
		}
	}
}
