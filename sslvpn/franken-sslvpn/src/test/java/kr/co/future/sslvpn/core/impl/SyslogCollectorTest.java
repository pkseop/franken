package kr.co.future.sslvpn.core.impl;

import kr.co.future.sslvpn.core.log.XtmLog;
import kr.co.future.sslvpn.core.impl.SyslogCollector;

import org.junit.Test;

import static org.junit.Assert.*;

public class SyslogCollectorTest {
	@Test
	public void parseTest() {
		String line = "0;20110727 155520; ;172.20.2.51;0.0.0.0;63044;0;172.20.15.255;0.0.0.0;22936;0;17;1;537133067;4;.... ;Close[00:10:00] R[5]; ; ;4; ;240;;eth0;";
		XtmLog log = SyslogCollector.parse(line);

		assertEquals("xtm", log.getType());
		assertEquals("172.20.2.51", log.getSourceIp());
		assertEquals("0.0.0.0", log.getNatSourceIp());
		assertEquals(63044, (int) log.getSourcePort());
		assertEquals(0, (int) log.getNatSourcePort());
		assertEquals("172.20.15.255", log.getDestinationIp());
		assertEquals("0.0.0.0", log.getNatDestinationIp());
		assertEquals(22936, (int) log.getDestinationPort());
		assertEquals("udp", log.getProtocol());
		assertEquals("fw", log.getCategory());
		assertEquals(537133067, (long) log.getLogType());
		assertEquals(4, (int) log.getLevel());
		assertEquals("....", log.getProduct());
		assertEquals("Close[00:10:00] R[5]", log.getNote());
		assertEquals("4", log.getRule());
		assertEquals(240, (long) log.getUsage());
		assertNull(log.getUser());
		assertEquals("eth0", log.getIface());
	}

	@Test
	public void parseLdapLogTest() {
		String line = "0;20110816 000501; ;0.0.0.0;0.0.0.0;0;0;0.0.0.0;0.0.0.0;0;0;0;0;302055453;1;center;AD / LDAP Connection Success; ; ;0; ; ; ; ; ";
		XtmLog log = SyslogCollector.parse(line);
		assertEquals("AD / LDAP Connection Success", log.getNote());
	}

	@Test
	public void parseRadiusLogTest() {
		String line = "0;20110816 001001; ;0.0.0.0;0.0.0.0;0;0;0.0.0.0;0.0.0.0;0;0;0;0;302055455;1;center;RADIUS Connection Success; ; ;0; ; ; ; ; ";
		XtmLog log = SyslogCollector.parse(line);
		assertEquals("RADIUS Connection Success", log.getNote());
	}
}
