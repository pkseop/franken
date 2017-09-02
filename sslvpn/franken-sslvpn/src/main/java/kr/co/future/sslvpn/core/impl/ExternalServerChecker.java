package kr.co.future.sslvpn.core.impl;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import kr.co.future.sslvpn.core.LiveStatus;
import kr.co.future.sslvpn.xtmconf.KLogWriter;
import kr.co.future.sslvpn.xtmconf.XtmConfig;
//import kr.co.future.sslvpn.xtmconf.manage.SyslogSetting;
import kr.co.future.sslvpn.xtmconf.network.Radius;
import kr.co.future.sslvpn.xtmconf.system.CommandUtil;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Invalidate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.apache.felix.ipojo.annotations.ServiceProperty;
import org.apache.felix.ipojo.annotations.Validate;

import kr.co.future.cron.CronService;
import kr.co.future.cron.Schedule;
import kr.co.future.ldap.LdapService;
import kr.co.future.msgbus.PushApi;
import kr.co.future.radius.client.RadiusClient;
import kr.co.future.radius.client.auth.Authenticator;
import kr.co.future.radius.client.auth.ChapAuthenticator;
import kr.co.future.radius.client.auth.PapAuthenticator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(name = "frodo-external-server-checker")
@Provides
public class ExternalServerChecker implements Runnable {
	private Logger logger = LoggerFactory.getLogger(ExternalServerChecker.class);

	@Requires
	private CronService cronService;

	@ServiceProperty(name = "instance.name")
	private String instanceName;

	@Requires
	private PushApi pushApi;

	@Requires
	private LdapService ldap;

	// for periodic radius and ldap check
	private Date lastRadiusCheck = new Date();
	private Date lastLdapCheck = new Date();

	@Validate
	public void validate() {
		Integer cronJobId = getCronJobId();
		if (cronJobId == null) {
			try {
				Schedule schedule = new Schedule.Builder(instanceName).build("* * * * *");
				cronService.registerSchedule(schedule);
			} catch (Exception e) {
				logger.error("frodo core: server checker cron register failed");
			}
		}
	}

	@Invalidate
	public void invalidate() {
		Integer cronJobId = getCronJobId();
		if (cronJobId != null)
			cronService.unregisterSchedule(cronJobId);
	}

	private Integer getCronJobId() {
		Map<Integer, Schedule> schedules = cronService.getSchedules();
		for (Integer id : schedules.keySet()) {
			Schedule schedule = schedules.get(id);
			if (schedule.getTaskName().equals(instanceName))
				return id;
		}
		return null;
	}

	@Override
	public void run() {
		/* pks. 2015-08-07. ping 체크 하지 않게 수정.
		List<SyslogSetting> syslogSettings = XtmConfig.readConfig(SyslogSetting.class);
		for (SyslogSetting syslogSetting : syslogSettings) {
			String ip = syslogSetting.getIp();
			try {
				InetAddress addr = InetAddress.getByName(ip);
				if (ip == null || ip.equals("127.0.0.1") || addr.isLoopbackAddress())
					continue;

				new Thread(new PingChecker(new Server("syslog", addr))).start();
			} catch (UnknownHostException e) {
				push("syslog", ip, null);
			}
		}
		*/

		List<Radius> radiusSettings = XtmConfig.readConfig(Radius.class);
		for (Radius radius : radiusSettings) {
			if (radius.getType() == Radius.Type.Radius) {
				if (radius.isRadiusUse() && radius.getRadiusCycle() > 0)
					new Thread(new RadiusChecker(radius), "RADIUS Checker").start();
			} else if (radius.getType() == Radius.Type.Domain) {
				if (radius.isLdapUse() && radius.getLdapCycle() > 0)
					new Thread(new LdapChecker(radius), "LDAP Checker").start();
			}
		}
	}
/*
	private class PingChecker implements Runnable {
		private Server server;

		private PingChecker(Server server) {
			this.server = server;
		}

		@Override
		public void run() {
			try {
				if (!server.addr.isReachable(server.timeout))
					push(server.type, server.addr.getHostName(), server.addr.getHostAddress());
			} catch (IOException e) {
				push(server.type, server.addr.getHostName(), server.addr.getHostAddress());
			}
		}
	}
*/
	private class RadiusChecker implements Runnable {
		private Radius radius;

		public RadiusChecker(Radius radius) {
			this.radius = radius;
		}

		@Override
		public void run() {
			if (radius.getRadiusIp() == null)
				return;

			Date now = new Date();
			long gap = now.getTime() - lastRadiusCheck.getTime();
			if (gap < radius.getRadiusCycle() * 60 * 1000)
				return;

			lastRadiusCheck = now;

			try {
				InetAddress addr = InetAddress.getByName(radius.getRadiusIp());
				int port = radius.getAuthPort();
				String sharedSecret = radius.getRadiusPassword();
				String authMethod = null;
				if (radius.getAuthMethod() != null)
					authMethod = radius.getAuthMethod().toString();
				if (authMethod == null)
					authMethod = "PAP";

				RadiusClient client = new RadiusClient(addr, port, sharedSecret);

				Authenticator auth = null;
				if (authMethod.equals("PAP"))
					auth = new PapAuthenticator(client, "test-account", "test-password");
				else
					auth = new ChapAuthenticator(client, "test-account", "test-password");

				client.authenticate(auth);
				KLogWriter.write(0x1201001f, null, "RADIUS Connection Success");
				logger.trace("frodo core: external radius check success [{}]", radius.getRadiusIp());
			} catch (UnknownHostException e) {
				push("radius", radius.getRadiusIp(), null);
				KLogWriter.write(0x1201001f, null, "RADIUS Connection Fail");
				try {
					CommandUtil.run("/usr/bin/php", "/var/www/webadmin/alert_sendmail.php", "0x12050020",
							"\"Radius Connection Fail\"");
				} catch (IOException e1) {
					logger.error("frodo core: cannot alert mail", e1);
				}
			} catch (IOException e) {
				logger.trace("frodo core: external radius check failed", e);
				push("radius", radius.getRadiusIp(), "RADIUS Connection Fail");
				try {
					CommandUtil.run("/usr/bin/php", "/var/www/webadmin/alert_sendmail.php", "0x12050020",
							"\"Radius Connection Fail\"");
				} catch (IOException e1) {
					logger.error("frodo core: cannot alert mail", e1);
				}
			}
		}
	}

	private class LdapChecker implements Runnable {
		private Radius radius;

		public LdapChecker(Radius radius) {
			this.radius = radius;
		}

		@Override
		public void run() {
			if (radius.getLdapAddress() == null)
				return;

			Date now = new Date();
			long gap = now.getTime() - lastLdapCheck.getTime();
			if (gap < radius.getLdapCycle() * 60 * 1000)
				return;

			lastLdapCheck = now;

			if (LdapVerifyHelper.testConnection(ldap, radius) == LiveStatus.Connected) {
				KLogWriter.write(0x1201001d, null, "AD / LDAP Connection Success");
				logger.trace("frodo core: external ldap check success [{}]", radius.getLdapAddress());
			} else {
				push("ldap", radius.getLdapAddress(), null);
				KLogWriter.write(0x1205001e, null, "AD / LDAP Connection Fail");

				try {
					CommandUtil.run("/usr/bin/php", "/var/www/webadmin/alert_sendmail.php", "0x1205001e",
							"\"AD / LDAP Connection Fail\"");
				} catch (IOException e1) {
					logger.error("frodo core: cannot alert mail", e1);
				}
			}
		}
	}

	private void push(String type, String name, String addr) {
		logger.debug("frodo core: no response from {} server - [{}/{}]", new Object[] { type, name, addr });
		Map<String, Object> m = new HashMap<String, Object>();
		m.put("type", type);
		m.put("name", name);
		m.put("address", addr);

		// can be null at bundle restart
		if (pushApi != null)
			pushApi.push("localhost", "frodo-external-server-check", m);
	}

	private class Server {
		private String type;
		private InetAddress addr;
		private int timeout;

		private Server(String type, InetAddress addr) {
			this(type, addr, 5000);
		}

		private Server(String type, InetAddress addr, int timeout) {
			this.type = type;
			this.addr = addr;
			this.timeout = timeout;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + getOuterType().hashCode();
			result = prime * result + ((addr == null) ? 0 : addr.hashCode());
			result = prime * result + ((type == null) ? 0 : type.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			Server other = (Server) obj;
			if (!getOuterType().equals(other.getOuterType()))
				return false;
			if (addr == null) {
				if (other.addr != null)
					return false;
			} else if (!addr.equals(other.addr))
				return false;
			if (type == null) {
				if (other.type != null)
					return false;
			} else if (!type.equals(other.type))
				return false;
			return true;
		}

		private ExternalServerChecker getOuterType() {
			return ExternalServerChecker.this;
		}
	}
}
