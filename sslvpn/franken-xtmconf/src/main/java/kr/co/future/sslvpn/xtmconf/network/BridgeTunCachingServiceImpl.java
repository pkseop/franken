package kr.co.future.sslvpn.xtmconf.network;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Validate;

import kr.co.future.msgbus.Marshaler;
import kr.co.future.sslvpn.xtmconf.BridgeTunCachingService;
import kr.co.future.sslvpn.xtmconf.XtmConfig;
import kr.co.future.sslvpn.xtmconf.system.CommandUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(name = "frodo-xtmconf-bridgetun-cache")
@Provides
public class BridgeTunCachingServiceImpl implements BridgeTunCachingService {
	private final Logger logger = LoggerFactory.getLogger(BridgeTunCachingServiceImpl.class.getName());
	private final File XtmConfXmlDirectory = new File("/utm/conf/xml/");

	// bridge information cache
	private String bridgedTunIp;
	private String bridgedTunNetmask;

	@Validate
	public void start() {
		reload();
	}

	@Override
	public void reload() {
		try {
			CommandUtil.run(new File("/"), "mount", "-t", "ext3", "/dev/hda2", "/utm/conf");
			List<Object> bridgeList = Marshaler.marshal(XtmConfig.readConfig(Bridge.class, XtmConfXmlDirectory));
			List<Object> virtualIpList = Marshaler.marshal(XtmConfig.readConfig(VirtualIp.class, XtmConfXmlDirectory));
			CommandUtil.run(new File("/"), "umount", "/utm/conf");
			reloadBridgedTunIp(bridgeList, virtualIpList);
		} catch (IOException e) {
			logger.trace("frodo xtmconf: does not got bridge information, mount error", e);
		}

	}

	@SuppressWarnings("unchecked")
	private void reloadBridgedTunIp(List<Object> bridgeList, List<Object> virtualIpList) {
		for (Object b : bridgeList) {
			Map<String, Object> bridge = (Map<String, Object>) b;
			List<String> iface = (List<String>) bridge.get("interface");

			// 1. tun0을 갖고 있는 bridge 객체의 이름을 찾아낸다.
			String bridgeName = null;
			if (iface.contains("tun0"))
				bridgeName = (String) bridge.get("name");

			// 2. 검색된 bridge 이름으로 가상 ipv4 의 인터페이스를 검색하여 일치하는것의 ip를 반환
			for (Object v : virtualIpList) {
				Map<String, Object> virtualIp = (Map<String, Object>) v;
				if (virtualIp.get("interface").equals(bridgeName)) {
					bridgedTunIp = (String) virtualIp.get("ip");
					bridgedTunNetmask = (String) virtualIp.get("netmask");
					logger.info("frodo xtmconf: find bridged tunIp [{}],  netmask [{}]", bridgedTunIp, bridgedTunNetmask);
					break;
				}
			}
		}
	}

	@Override
	public String getBridgedTunIp() {
		return bridgedTunIp;
	}

	@Override
	public String getBridgredTunNetmask() {
		return bridgedTunNetmask;
	}
}