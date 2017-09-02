package kr.co.future.sslvpn.core.impl;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import kr.co.future.sslvpn.core.AlertCategory;
import kr.co.future.sslvpn.core.AlertStatus;
import kr.co.future.sslvpn.core.DashboardService;
import kr.co.future.sslvpn.core.LicenseStatus;
import kr.co.future.sslvpn.core.LiveStatus;
import kr.co.future.sslvpn.core.NetworkService;
import kr.co.future.sslvpn.model.AccessGateway;
import kr.co.future.sslvpn.model.AccessGateway.LdapMode;
import kr.co.future.sslvpn.model.api.AccessGatewayApi;
import kr.co.future.sslvpn.xtmconf.XtmConfig;
import kr.co.future.sslvpn.xtmconf.network.Radius;
import kr.co.future.sslvpn.xtmconf.network.Radius.AuthMethod;
import kr.co.future.sslvpn.xtmconf.system.License;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;

import kr.co.future.cron.PeriodicJob;
import kr.co.future.ldap.LdapService;
import kr.co.future.radius.client.RadiusClient;
import kr.co.future.radius.client.auth.ChapAuthenticator;
import kr.co.future.radius.client.auth.PapAuthenticator;
import kr.co.future.rpc.RpcAgent;
import kr.co.future.sslvpn.core.impl.DashboardServiceImpl;
import kr.co.future.sslvpn.core.impl.LdapVerifyHelper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(name = "frodo-dashboard")
@Provides
@PeriodicJob("* * * * *")
public class DashboardServiceImpl implements DashboardService, Runnable {
	private Logger logger = LoggerFactory.getLogger(DashboardServiceImpl.class);
	
	@Requires
	private AccessGatewayApi gwApi;

	@Requires
	private LdapService ldap;

	private ConcurrentMap<NetworkService, LiveStatus> statuses = new ConcurrentHashMap<NetworkService, LiveStatus>();
	private ConcurrentMap<AlertCategory, AlertStatus> alerts = new ConcurrentHashMap<AlertCategory, AlertStatus>();

	@Requires
	private RpcAgent agent;

	public DashboardServiceImpl() {
		for (NetworkService ns : NetworkService.values())
			statuses.put(ns, LiveStatus.Disabled);

		for (AlertCategory cat : AlertCategory.values())
			alerts.put(cat, new AlertStatus());
	}

	@Override
	public LiveStatus getServiceStatus(NetworkService name) {
		return statuses.get(name);
	}

	@Override
	public LicenseStatus getLicenseStatus() {
		try {
			License license = License.load();
			String type = license.getType();

			if (type.equals("registered"))
				return LicenseStatus.Registered;
			else if (type.equals("expired"))
				return LicenseStatus.Expired;

		} catch (Exception e) {
		}

		return LicenseStatus.Unregistered;
	}

	@Override
	public AlertStatus getAlert(AlertCategory category) {
		return alerts.get(category);
	}

	@Override
	public void setAlert(AlertCategory category, Date date, boolean alert) {
		alerts.put(category, new AlertStatus(date, alert));
	}

	/**
	 * update live status
	 */
	@Override
	public void run() {
		AccessGateway gw = gwApi.getCurrentAccessGateway();

		// cpu, mem, disk usages

		// radius
		updateRadiusStatus(gw);

		// ldap
		updateLdapStatus(gw);

		// vpn server
		updateVpnStatus();

		// license
	}

	private void updateVpnStatus() {
//		for (RpcConnection conn : agent.getConnections()) {
//			if (conn.getPeerCertificate() != null) {
//				statuses.put(NetworkService.SslEngine, LiveStatus.Connected);
//				return;
//			}
//		}
//
//		statuses.put(NetworkService.SslEngine, LiveStatus.Disconnected);
		
		Runtime r = Runtime.getRuntime();
		Process p;
		BufferedReader br = null;
      try {
	      p = r.exec("ps");      
			br = new BufferedReader(new InputStreamReader(p.getInputStream()));			
			while (true) {
				String msg = br.readLine();
				if (msg == null)
					break;
				else {
					if (msg.contains("xenics_daemon")) {
						statuses.put(NetworkService.SslEngine, LiveStatus.Connected);
						return;
					}
				}
			}
      } catch (IOException e) {
      	logger.error("error occurred during update vpn status", e);
      } finally{
         try {
         	if(br != null)
         		br.close();
         } catch (IOException e) {
         	logger.error("error occurred during update vpn status", e);
         }
      }
		
		statuses.put(NetworkService.SslEngine, LiveStatus.Disconnected);
	}

	private void updateLdapStatus(AccessGateway gw) {
		if (gw != null && gw.getLdapMode() == LdapMode.LDAP_SYNC) {
			List<Radius> configs = XtmConfig.readConfig(Radius.class);
			Radius ldapConfig = null;

			for (Radius config : configs)
				if (config.getType().equals(Radius.Type.Domain))
					ldapConfig = config;

			if (ldapConfig == null)
				return;

			if (ldapConfig.getLdapCycle() == 0)
				return;
			
			statuses.put(NetworkService.Ldap, LdapVerifyHelper.testConnection(ldap, ldapConfig));
		} else {
			statuses.put(NetworkService.Ldap, LiveStatus.Disabled);
		}
	}

	private void updateRadiusStatus(AccessGateway gw) {
		if (gw != null && gw.isUseRadiusAuth()) {
			List<Radius> configs = XtmConfig.readConfig(Radius.class);
			Radius radius = null;

			for (Radius config : configs)
				if (config.getType().equals(Radius.Type.Radius))
					radius = config;

			if (radius == null)
				return;

			if (radius.getRadiusCycle() == 0)
				return;
			
			try {
				InetAddress addr = InetAddress.getByName(radius.getRadiusIp());
				RadiusClient client = new RadiusClient(addr, radius.getRadiusPassword());
				if (radius.getAuthMethod() == AuthMethod.PAP) {
					PapAuthenticator pap = new PapAuthenticator(client, "test", "test");
					client.authenticate(pap);
				} else {
					ChapAuthenticator chap = new ChapAuthenticator(client, "test", "test");
					client.authenticate(chap);
				}
				statuses.put(NetworkService.Radius, LiveStatus.Connected);
			} catch (Exception e) {
				statuses.put(NetworkService.Radius, LiveStatus.Disconnected);
			}

		} else {
			statuses.put(NetworkService.Radius, LiveStatus.Disabled);
		}
	}
}
