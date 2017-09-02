package kr.co.future.sslvpn.core.impl;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;

import kr.co.future.sslvpn.xtmconf.XtmConfig;
import kr.co.future.sslvpn.xtmconf.network.Interface;
import kr.co.future.sslvpn.core.impl.TapValue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TapValue {
	private final Logger logger = LoggerFactory.getLogger(TapValue.class.getName());
	private InetAddress ip;
	private InetAddress netmask;

	public TapValue() throws UnknownHostException {
		List<Interface> ifaces = XtmConfig.readConfig(Interface.class);

		for (Interface iface : ifaces) {
			logger.debug("frodo core: get interface name [{}]", iface.getIfaceName());

			if (iface.getIfaceName() == null)
				continue;

			if (iface.getIfaceName().equals("tap0")) {
				if (iface.getIp() == null)
					throw new IllegalStateException("tap ip not set");

				String[] tap = iface.getIp().split("/");
				if (tap.length != 2)
					throw new IllegalStateException("invalid tap ip config: " + iface.getIp());

				this.ip = InetAddress.getByName(tap[0]);
				this.netmask = InetAddress.getByName(tap[1]);
			}
		}
	}

	public InetAddress getIp() {
		return ip;
	}

	public InetAddress getNetmask() {
		return netmask;
	}

}
