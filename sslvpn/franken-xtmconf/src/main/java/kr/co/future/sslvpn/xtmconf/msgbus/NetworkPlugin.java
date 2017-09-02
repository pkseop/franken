package kr.co.future.sslvpn.xtmconf.msgbus;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Provides;

import kr.co.future.ldap.LdapProfile;
import kr.co.future.linux.api.NetworkInterfaceInformation;
import kr.co.future.msgbus.Marshaler;
import kr.co.future.msgbus.Request;
import kr.co.future.msgbus.Response;
import kr.co.future.msgbus.handler.MsgbusMethod;
import kr.co.future.msgbus.handler.MsgbusPlugin;
import kr.co.future.sslvpn.xtmconf.ConfigEventListener;
import kr.co.future.sslvpn.xtmconf.ConfigEventProvider;
import kr.co.future.sslvpn.xtmconf.Utils;
import kr.co.future.sslvpn.xtmconf.XtmConfig;
import kr.co.future.sslvpn.xtmconf.network.Anomaly;
import kr.co.future.sslvpn.xtmconf.network.AnomalyAction;
import kr.co.future.sslvpn.xtmconf.network.AnomalyPortscan;
import kr.co.future.sslvpn.xtmconf.network.AnomalySpoofing;
import kr.co.future.sslvpn.xtmconf.network.Bonding;
import kr.co.future.sslvpn.xtmconf.network.Bridge;
import kr.co.future.sslvpn.xtmconf.network.Ddns;
import kr.co.future.sslvpn.xtmconf.network.DhcpLease;
import kr.co.future.sslvpn.xtmconf.network.DhcpRelay;
import kr.co.future.sslvpn.xtmconf.network.DhcpServer;
import kr.co.future.sslvpn.xtmconf.network.DnsDomain;
import kr.co.future.sslvpn.xtmconf.network.DnsPermit;
import kr.co.future.sslvpn.xtmconf.network.DnsType;
import kr.co.future.sslvpn.xtmconf.network.InnerDns;
import kr.co.future.sslvpn.xtmconf.network.Interface;
import kr.co.future.sslvpn.xtmconf.network.IpManager;
import kr.co.future.sslvpn.xtmconf.network.IpScan;
import kr.co.future.sslvpn.xtmconf.network.L2TPConfig;
import kr.co.future.sslvpn.xtmconf.network.OuterDns;
import kr.co.future.sslvpn.xtmconf.network.Radius;
import kr.co.future.sslvpn.xtmconf.network.RouterChecker;
import kr.co.future.sslvpn.xtmconf.network.RouterMulticast;
import kr.co.future.sslvpn.xtmconf.network.RouterPolicy;
import kr.co.future.sslvpn.xtmconf.network.RouterScript;
import kr.co.future.sslvpn.xtmconf.network.RouterStatic;
import kr.co.future.sslvpn.xtmconf.network.RouterVrrp;
import kr.co.future.sslvpn.xtmconf.network.SixInFour;
import kr.co.future.sslvpn.xtmconf.network.SixToFour;
import kr.co.future.sslvpn.xtmconf.network.VirtualIp;
import kr.co.future.sslvpn.xtmconf.network.VirtualIpv6;
import kr.co.future.sslvpn.xtmconf.network.Vlan;
import kr.co.future.sslvpn.xtmconf.network.Anomaly.Level;
import kr.co.future.sslvpn.xtmconf.network.Anomaly.Type;
import kr.co.future.sslvpn.xtmconf.network.AnomalyPortscan.Extention;
import kr.co.future.sslvpn.xtmconf.network.AnomalyPortscan.Protocol;
import kr.co.future.sslvpn.xtmconf.network.Bonding.Mode;
import kr.co.future.sslvpn.xtmconf.network.Ddns.Server;
import kr.co.future.sslvpn.xtmconf.network.DnsDomain.DnsList;
import kr.co.future.sslvpn.xtmconf.network.DnsDomain.DnsList.Field;
import kr.co.future.sslvpn.xtmconf.network.Interface.Duplex;
import kr.co.future.sslvpn.xtmconf.network.Interface.IfaceType;
import kr.co.future.sslvpn.xtmconf.network.Interface.InterfaceType;
import kr.co.future.sslvpn.xtmconf.network.Interface.Modem;
import kr.co.future.sslvpn.xtmconf.network.Interface.Speed;
import kr.co.future.sslvpn.xtmconf.network.IpManager.Action;
import kr.co.future.sslvpn.xtmconf.network.Radius.AuthMethod;
import kr.co.future.sslvpn.xtmconf.network.Radius.Fileserver;
import kr.co.future.sslvpn.xtmconf.network.RouterMulticast.RP;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(name = "frodo-xtmconf-network-plugin")
@Provides
@MsgbusPlugin
public class NetworkPlugin implements ConfigEventProvider {
	private Logger logger = LoggerFactory.getLogger(NetworkPlugin.class);

	private enum IdType {
		Cid, Num
	}

	@MsgbusMethod
	public void getAnomaly(Request req, Response resp) {
		resp.put("config", Marshaler.marshal(XtmConfig.readConfig(Anomaly.class)));
	}

	@MsgbusMethod
	public void setAnomaly(Request req, Response resp) {
		List<Anomaly> config = new ArrayList<Anomaly>();
		for (Type type : Type.values()) {
			Anomaly a = new Anomaly();
			a.setType(type);
			setAnomaly(a, req.get(type.toString()));
			config.add(a);
		}
		XtmConfig.writeConfig(Anomaly.class, config);
	}

	private void setAnomaly(Anomaly a, Object map) {
		a.setUse(Utils.getBooleanFromMap(map, "use"));
		a.setAction(AnomalyAction.get(Utils.getStringFromMap(map, "action")));

		if (a.getType() == Type.DOS) {
			a.setBlockTime(Utils.getIntegerFromMap(map, "block_time"));
			a.setSyn(Utils.getIntegerFromMap(map, "syn"));
			a.setUdp(Utils.getIntegerFromMap(map, "udp"));
			a.setIcmp(Utils.getIntegerFromMap(map, "icmp"));
			a.setHttpGet(Utils.getIntegerFromMap(map, "http_get"));
			a.setPingLimit(Utils.getIntegerFromMap(map, "ping_limit"));
		} else if (a.getType() == Type.DDOS) {
			a.setBlockTime(Utils.getIntegerFromMap(map, "block_time"));
			a.setSyn(Utils.getIntegerFromMap(Utils.getFromMap(map, "syn"), "dispersion"));
			a.setSynLevel(Level.get(Utils.getStringFromMap(Utils.getFromMap(map, "syn"), "level")));
			a.setUdp(Utils.getIntegerFromMap(Utils.getFromMap(map, "udp"), "dispersion"));
			a.setUdpLevel(Level.get(Utils.getStringFromMap(Utils.getFromMap(map, "udp"), "level")));
			a.setIcmp(Utils.getIntegerFromMap(Utils.getFromMap(map, "icmp"), "dispersion"));
			a.setIcmpLevel(Level.get(Utils.getStringFromMap(Utils.getFromMap(map, "icmp"), "level")));
			a.setHttpGet(Utils.getIntegerFromMap(Utils.getFromMap(map, "http_get"), "dispersion"));
			a.setHttpGetLevel(Level.get(Utils.getStringFromMap(Utils.getFromMap(map, "http_get"), "level")));
		}
	}

	@MsgbusMethod
	public void getAnomalyPortscan(Request req, Response resp) {
		resp.put("config", Marshaler.marshal(XtmConfig.readConfig(AnomalyPortscan.class)));
	}

	@SuppressWarnings("unchecked")
	@MsgbusMethod
	public void setAnomalyPortscan(Request req, Response resp) {
		List<AnomalyPortscan> config = new ArrayList<AnomalyPortscan>();

		AnomalyPortscan portscan = new AnomalyPortscan();
		portscan.setType(AnomalyPortscan.Type.Portscan);
		setAnomalyPortscan(portscan, req.get("portscan"));
		config.add(portscan);

		AnomalyPortscan extention = new AnomalyPortscan();
		extention.setType(AnomalyPortscan.Type.Extention);
		setAnomalyPortscan(extention, req.get("extention"));
		config.add(extention);

		List<Object> watch = (List<Object>) req.get("watch");
		for (Object map : watch) {
			AnomalyPortscan w = new AnomalyPortscan();
			w.setType(AnomalyPortscan.Type.Watch);
			setAnomalyPortscan(w, map);
			config.add(w);
		}

		List<Object> scanner = (List<Object>) req.get("scanner");
		for (Object map : scanner) {
			AnomalyPortscan s = new AnomalyPortscan();
			s.setType(AnomalyPortscan.Type.Scanner);
			setAnomalyPortscan(s, map);
			config.add(s);
		}

		List<Object> scanned = (List<Object>) req.get("scanned");
		for (Object map : scanned) {
			AnomalyPortscan s = new AnomalyPortscan();
			s.setType(AnomalyPortscan.Type.Scanned);
			setAnomalyPortscan(s, map);
			config.add(s);
		}

		XtmConfig.writeConfig(AnomalyPortscan.class, config);
	}

	@SuppressWarnings("unchecked")
	private void setAnomalyPortscan(AnomalyPortscan ap, Object map) {
		if (ap.getType() == AnomalyPortscan.Type.Portscan) {
			ap.setUse(Utils.getBooleanFromMap(map, "use"));
			ap.setProtocol(Protocol.valueOf(Utils.getStringFromMap(map, "protocol")));
			ap.setLevel(AnomalyPortscan.Level.valueOf(Utils.getStringFromMap(map, "level")));
		} else if (ap.getType() == AnomalyPortscan.Type.Extention) {
			List<Object> l = (List<Object>) map;
			for (Object m : l) {
				Extention e = new Extention();
				e.setScanType(Utils.getStringFromMap(m, "scan_type"));
				e.setAction(AnomalyAction.get(Utils.getStringFromMap(m, "action")));
				e.setTime(Utils.getIntegerFromMap(m, "time"));
				e.setDropType(Utils.getIntegerFromMap(m, "drop_type"));
				e.setUse(Utils.getBooleanFromMap(m, "use"));
				ap.getExtention().add(e);
			}
		} else if (ap.getType() == AnomalyPortscan.Type.Watch || ap.getType() == AnomalyPortscan.Type.Scanner
				|| ap.getType() == AnomalyPortscan.Type.Scanned) {
			ap.setIp(Utils.getStringFromMap(map, "ip"));
			ap.setNetmask(Utils.getStringFromMap(map, "netmask"));
		}
	}

	@MsgbusMethod
	public void getAnomalySpoofing(Request req, Response resp) {
		resp.put("config", Marshaler.marshal(XtmConfig.readConfig(AnomalySpoofing.class)));
	}

	@MsgbusMethod
	public void setAnomalySpoofing(Request req, Response resp) {
		AnomalySpoofing as = new AnomalySpoofing();

		as.setUse(req.getBoolean("use"));
		as.setV4Cid(req.getString("v4"));
		as.setV6Cid(req.getString("v6"));

		XtmConfig.writeConfig(AnomalySpoofing.class, Arrays.asList(as));
	}

	@MsgbusMethod
	public void getBonding(Request req, Response resp) {
		resp.put("config", Marshaler.marshal(XtmConfig.readConfig(Bonding.class)));
	}

	@MsgbusMethod
	public void addBonding(Request req, Response resp) {
		addNetwork(Bonding.class, req);
	}

	@MsgbusMethod
	public void modifyBonding(Request req, Response resp) {
		modifyNetwork(Bonding.class, req, IdType.Num);
	}

	@MsgbusMethod
	public void removeBonding(Request req, Response resp) {
		removeNetwork(Bonding.class, req, IdType.Num);
	}

	@SuppressWarnings({ "unchecked", "unused" })
	private void setBonding(Bonding b, Request req) {
		b.setName(req.getString("name"));
		b.setIface((List<String>) req.get("interface"));
		b.setMode(Mode.values()[req.getInteger("mode")]);
		b.setMonitorInterval(Utils.getIntegerFromMap(req.get("monitor"), "interval"));
		b.setMonitorUpdelay(Utils.getIntegerFromMap(req.get("monitor"), "updelay"));
		b.setMonitorDowndelay(Utils.getIntegerFromMap(req.get("monitor"), "downdelay"));
		b.setArpInterval(Utils.getIntegerFromMap(req.get("arp"), "interval"));
		b.setArp((List<String>) Utils.getFromMap(req.get("arp"), "ip"));
		b.setLacp(req.getInteger("lacp"));
		b.setPrimary(req.getString("primary"));
		b.setHash(req.getInteger("hash"));
	}

	@MsgbusMethod
	public void getBridge(Request req, Response resp) {
		resp.put("config", Marshaler.marshal(XtmConfig.readConfig(Bridge.class)));
	}

	@MsgbusMethod
	public void addBridge(Request req, Response resp) {
		addNetwork(Bridge.class, req);
	}

	@MsgbusMethod
	public void modifyBridge(Request req, Response resp) {
		modifyNetwork(Bridge.class, req, IdType.Num);
	}

	@MsgbusMethod
	public void removeBridge(Request req, Response resp) {
		removeNetwork(Bridge.class, req, IdType.Num);
	}

	@SuppressWarnings({ "unchecked", "unused" })
	private void setBridge(Bridge b, Request req) {
		b.setStp(req.getBoolean("stp"));
		b.setLearningTime(req.getInteger("learning_time"));
		b.setName(req.getString("name"));
		b.setIface((List<String>) req.get("interface"));
	}

	@MsgbusMethod
	public void getDdns(Request req, Response resp) {
		resp.put("config", Marshaler.marshal(XtmConfig.readConfig(Ddns.class)));
	}

	@MsgbusMethod
	public void setDdns(Request req, Response resp) {
		Ddns d = new Ddns();
		d.setUse(req.getBoolean("use"));
		d.setServer(Server.get(req.getString("server")));
		d.setHost(req.getString("host"));
		d.setName(req.getString("name"));
		d.setPassword(req.getString("password"));
		d.setCycle(req.getInteger("cycle"));
		d.setFreednsHash(Utils.getStringFromMap(req.get("freedns"), "hash"));
		d.setFreednsTime(Utils.getIntegerFromMap(req.get("freedns"), "time"));

		XtmConfig.writeConfig(Ddns.class, Arrays.asList(d));
	}

	@MsgbusMethod
	public void getDhcpRelay(Request req, Response resp) {
		resp.put("config", Marshaler.marshal(XtmConfig.readConfig(DhcpRelay.class)));
	}

	@SuppressWarnings("unchecked")
	@MsgbusMethod
	public void setDhcpRelay(Request req, Response resp) {
		DhcpRelay dr = new DhcpRelay();
		dr.setUse(req.getBoolean("use"));
		dr.setServer(req.getString("server"));
		dr.setIface((List<String>) req.get("interface"));

		XtmConfig.writeConfig(DhcpRelay.class, Arrays.asList(dr));
	}

	@MsgbusMethod
	public void getDhcpServer(Request req, Response resp) {
		resp.put("config", Marshaler.marshal(XtmConfig.readConfig(DhcpServer.class)));
	}

	@MsgbusMethod
	public void addDhcpServer(Request req, Response resp) {
		addNetwork(DhcpServer.class, req);
	}

	@MsgbusMethod
	public void modifyDhcpServer(Request req, Response resp) {
		modifyNetwork(DhcpServer.class, req, IdType.Cid);
	}

	@MsgbusMethod
	public void removeDhcpServer(Request req, Response resp) {
		removeNetwork(DhcpServer.class, req, IdType.Cid);
	}

	@MsgbusMethod
	public void getDhcpLeases(Request req, Response resp) throws IOException {
		resp.put("logs", Marshaler.marshal(DhcpLease.load()));
	}

	@SuppressWarnings("unused")
	private void setDhcpServer(DhcpServer ds, Request req) {
		ds.setUse(req.getBoolean("use"));
		ds.setIface(req.getString("interface"));
		ds.setTime(req.getInteger("time"));
		ds.setDomain1(req.getString("domain1"));
		ds.setDomain2(req.getString("domain2"));
		ds.setGateway(req.getString("gateway"));
		ds.setSaddr(req.getString("saddr"));
		ds.setDaddr(req.getString("daddr"));
		ds.setNetmask(req.getString("netmask"));
	}

	@MsgbusMethod
	public void getInnerDns(Request req, Response resp) {
		resp.put("config", Marshaler.marshal(XtmConfig.readConfig(InnerDns.class)));
	}

	@SuppressWarnings("unchecked")
	@MsgbusMethod
	public void setInnerDns(Request req, Response resp) {
		List<InnerDns> config = new ArrayList<InnerDns>();

		InnerDns info = new InnerDns();
		info.setType(DnsType.Info);
		setInnerDns(info, req.get("info"));
		config.add(info);

		List<Object> permit = (List<Object>) req.get("permit");
		for (Object map : permit) {
			InnerDns p = new InnerDns();
			p.setType(DnsType.Permit);
			setInnerDns(p, map);
			config.add(p);
		}

		List<Object> domain = (List<Object>) req.get("domain");
		for (Object map : domain) {
			InnerDns d = new InnerDns();
			d.setType(DnsType.Domain);
			setInnerDns(d, map);
			config.add(d);
		}

		XtmConfig.writeConfig(InnerDns.class, config);
	}

	private void setInnerDns(InnerDns id, Object map) {
		if (id.getType() == DnsType.Info) {
			id.setUse(Utils.getBooleanFromMap(map, "use"));
			id.setIface(Utils.getStringFromMap(map, "interface"));
		} else if (id.getType() == DnsType.Permit) {
			id.setPermit(getDnsPermit(map));
		} else if (id.getType() == DnsType.Domain) {
			id.setDomain(getDnsDomain(map));
		}
	}

	@MsgbusMethod
	public void getInterface(Request req, Response resp) {
		resp.put("config", Marshaler.marshal(XtmConfig.readConfig(Interface.class)));
		try {
			resp.put("stats", getNetworkInterfaceInforms());
		} catch (IOException e) {
			logger.error("frodo xtmconf: cannot obtain network interface information", e);
		}
	}

	private Map<String, Object> getNetworkInterfaceInforms() throws IOException {

		Map<String, Object> result = new HashMap<String, Object>();
		for (NetworkInterfaceInformation ni : NetworkInterfaceInformation.getNetworkInterfaceInformations()) {
			Map<String, Object> data = new HashMap<String, Object>();
			data.put("rx_bytes", ni.getRxBytes());
			data.put("rx_packets", ni.getRxPackets());
			data.put("rx_errs", ni.getRxErrs());
			data.put("rx_drop", ni.getRxDrop());

			data.put("tx_bytes", ni.getTxBytes());
			data.put("tx_packets", ni.getTxPackets());
			data.put("tx_errs", ni.getTxErrs());
			data.put("tx_drop", ni.getTxDrop());

			result.put(ni.getName(), data);
		}
		return result;

	}

	@MsgbusMethod
	public void setInterface(Request req, Response resp) {
		List<Interface> config = XtmConfig.readConfig(Interface.class);
		boolean modified = false;
		Interface tap0 = null;

		if(req.getString("name") != null && req.getString("name").equals("tun0"))
			return;
		
		for (Interface iface : config) {
			if (req.getString("type").equals("domain")) {
				if (iface.getType().equals(Interface.Type.Domain)) {
					iface.setMainDomain(req.getString("main"));
					iface.setSubDomain(req.getString("sub"));
					modified = true;
				}
			} else if (req.getString("name").equals(iface.getIfaceName())) {
				setInterface(iface, req);
				modified = true;
				// check modification of tap0 config
				if (iface.getIfaceName().equals("tap0") && iface.getIfaceType() == IfaceType.Static) {
					tap0 = iface;
				}
			}
		}

		if (!modified) {
			Interface iface = new Interface();
			if (req.getString("type").equals("domain")) {
				iface.setType(Interface.Type.Domain);
				iface.setMainDomain(req.getString("main"));
				iface.setSubDomain(req.getString("sub"));
			} else {
				iface.setType(Interface.Type.Interface);
				iface.setIfaceName(req.getString("name"));
				setInterface(iface, req);
				// check modification of tap0 config also in creation
				if (iface.getIfaceName().equals("tap0") && iface.getIfaceType() == IfaceType.Static)
					tap0 = iface;
			}
			config.add(iface);
		}

		if (tap0 != null) {
			try {
				String ipnetmask = tap0.getIp();
				String ip = ipnetmask.substring(0, ipnetmask.indexOf("/"));
				InetAddress tap0Ip = InetAddress.getByName(ip);
				for (ConfigEventListener listener : listeners) {
					listener.onTapIPChanged(tap0Ip);
				}
			} catch (UnknownHostException e) {
				logger.error("frodo core: cannot change interface", e);
			}
		}

		Comparator<Interface> comparator = new Comparator<Interface>() {
			@Override
			public int compare(Interface interface1, Interface interface2) {
				if (interface1.getType() == Interface.Type.Domain)
					return 1;

				if (interface2.getType() == Interface.Type.Domain)
					return -1;

				Pattern p = Pattern.compile("(\\D*)(\\d*)");
				Matcher m1 = p.matcher(interface1.getIfaceName());
				Matcher m2 = p.matcher(interface2.getIfaceName());
				logger.trace("frodo xtmconf: interface name [{}], [{}]", interface1.getIfaceName(), interface2.getIfaceName());
				if (m1.matches() && m2.matches()) {
					if (m1.group(1).equals("tap"))
						return 1;
					if (m2.group(1).equals("tap"))
						return -1;
					if (m1.group(1).equals("tunl"))
						return 1;
					if (m2.group(1).equals("tunl"))
						return -1;

					Integer ifaceNumber1 = Integer.parseInt(m1.group(2));
					Integer ifaceNumber2 = Integer.parseInt(m2.group(2));

					return ifaceNumber1.compareTo(ifaceNumber2);
				}
				return 0;
			}
		};

		Collections.sort(config, comparator);

		XtmConfig.writeConfig(Interface.class, config);
	}

	private void setInterface(Interface iface, Request req) {
		iface.setIfaceType(IfaceType.valueOf(req.getString("type")));
		iface.setDuplex(Duplex.valueOf(req.getString("duplex")));
		iface.setSpeed(Speed.get(req.getString("speed")));
		iface.setMtu(req.getInteger("mtu"));
		iface.setMode(InterfaceType.valueOf(req.getString("mode")));
		iface.setFrag(req.getBoolean("frag"));
		iface.setMultipath(req.getBoolean("multipath"));
		iface.setBand(req.getInteger("band"));
		iface.setMss(req.getInteger("mss"));
		iface.setIp(req.getString("ip"));
		iface.setIpv6(req.getString("ipv6"));
		iface.setAdslId(Utils.getStringFromMap(req.get("adsl"), "id"));
		iface.setAdslPass(Utils.getStringFromMap(req.get("adsl"), "password"));
		iface.setModem(Modem.valueOf(Utils.getStringFromMap(req.get("adsl"), "modem")));
	}

	@MsgbusMethod
	public void getIpManager(Request req, Response resp) {
		resp.put("config", Marshaler.marshal(XtmConfig.readConfig(IpManager.class)));
	}

	@MsgbusMethod
	public void setIpManagerResponse(Request req, Response resp) {
		List<IpManager> config = XtmConfig.readConfig(IpManager.class);
		IpManager obj = null;
		for (IpManager im : config) {
			if (im.getType() == IpManager.Type.Response)
				obj = im;
		}

		if (obj == null) {
			obj = new IpManager();
			obj.setType(IpManager.Type.Response);
			config.add(obj);
		}

		setIpManager(obj, req);

		XtmConfig.writeConfig(IpManager.class, config);
	}

	@MsgbusMethod
	public void addIpManagerManager(Request req, Response resp) {
		IpManager im = new IpManager();
		im.setType(IpManager.Type.Manager);
		setIpManager(im, req);

		List<IpManager> config = XtmConfig.readConfig(IpManager.class);
		config.add(im);
		Utils.setConfigNum(config, IpManager.Type.Manager);
		XtmConfig.writeConfig(IpManager.class, config);
	}

	@MsgbusMethod
	public void modifyIpManagerManager(Request req, Response resp) {
		int num = req.getInteger("num");

		List<IpManager> config = XtmConfig.readConfig(IpManager.class);
		for (IpManager im : config) {
			if (im.getNum() == num)
				setIpManager(im, req);
		}
		XtmConfig.writeConfig(IpManager.class, config);
	}

	@SuppressWarnings("unchecked")
	@MsgbusMethod
	public void removeIpManagerManager(Request req, Response resp) {
		List<Integer> nums = (List<Integer>) req.get("num");

		List<IpManager> config = XtmConfig.readConfig(IpManager.class);
		List<IpManager> objs = new ArrayList<IpManager>();
		for (IpManager im : config) {
			if (nums.contains(im.getNum()))
				objs.add(im);
		}
		for (IpManager obj : objs)
			config.remove(obj);
		Utils.setConfigNum(config, IpManager.Type.Manager);
		XtmConfig.writeConfig(IpManager.class, config);
	}

	private void setIpManager(IpManager im, Request req) {
		im.setMac(req.getString("mac"));

		if (im.getType() == IpManager.Type.Manager) {
			im.setBypassMac(req.getBoolean("bypass"));
			im.setIface(req.getString("interface"));
			im.setIp(req.getString("ip"));
			im.setAction(Action.valueOf(req.getString("action")));
		}
	}

	@MsgbusMethod
	public void getIpScan(Request req, Response resp) {
		resp.put("config", Marshaler.marshal(XtmConfig.readConfig(IpScan.class)));
	}

	@MsgbusMethod
	public void addIpScan(Request req, Response resp) {
		addNetwork(IpScan.class, req);
	}

	@MsgbusMethod
	public void modifyIpScan(Request req, Response resp) {
		modifyNetwork(IpScan.class, req, IdType.Num);
	}

	@MsgbusMethod
	public void removeIpScan(Request req, Response resp) {
		removeNetwork(IpScan.class, req, IdType.Num);
	}

	@SuppressWarnings("unused")
	private void setIpScan(IpScan is, Request req) {
		is.setIface(req.getString("interface"));
		is.setIp(req.getString("ip"));
		is.setCycle(req.getInteger("cycle"));
		is.setType(IpScan.Type.valueOf(req.getString("type")));
	}

	@MsgbusMethod
	public void getOuterDns(Request req, Response resp) {
		resp.put("config", Marshaler.marshal(XtmConfig.readConfig(OuterDns.class)));
	}

	@SuppressWarnings("unchecked")
	@MsgbusMethod
	public void setOuterDns(Request req, Response resp) {
		List<OuterDns> config = new ArrayList<OuterDns>();

		OuterDns info = new OuterDns();
		info.setType(DnsType.Info);
		setOuterDns(info, req.get("info"));
		config.add(info);

		List<Object> permit = (List<Object>) req.get("permit");
		for (Object map : permit) {
			OuterDns p = new OuterDns();
			p.setType(DnsType.Permit);
			setOuterDns(p, map);
			config.add(p);
		}

		List<Object> domain = (List<Object>) req.get("domain");
		for (Object map : domain) {
			OuterDns d = new OuterDns();
			d.setType(DnsType.Domain);
			setOuterDns(d, map);
			config.add(d);
		}

		XtmConfig.writeConfig(OuterDns.class, config);
	}

	private void setOuterDns(OuterDns od, Object map) {
		if (od.getType() == DnsType.Info) {
			od.setUse(Utils.getBooleanFromMap(map, "use"));
			od.setCache(Utils.getBooleanFromMap(map, "cache"));
			od.setIface(Utils.getStringFromMap(map, "interface"));
		} else if (od.getType() == DnsType.Permit) {
			od.setPermit(getDnsPermit(map));
		} else if (od.getType() == DnsType.Domain) {
			od.setDomain(getDnsDomain(map));
		}
	}

	private DnsPermit getDnsPermit(Object map) {
		DnsPermit dp = new DnsPermit();
		dp.setIp(Utils.getStringFromMap(map, "ip"));
		dp.setNetmask(Utils.getStringFromMap(map, "netmask"));
		return dp;
	}

	private DnsDomain getDnsDomain(Object map) {
		DnsDomain dd = new DnsDomain();
		dd.setName(Utils.getStringFromMap(map, "name"));
		List<Object> list = Utils.getListFromMap(map, "dns");
		for (Object m : list)
			dd.getList().add(getDnsList(m));
		return dd;
	}

	private DnsList getDnsList(Object map) {
		DnsList dl = new DnsList();
		dl.setPtr(Utils.getBooleanFromMap(map, "ptr"));
		dl.setField(Field.valueOf(Utils.getStringFromMap(map, "field")));
		dl.setHost(Utils.getStringFromMap(map, "host"));
		dl.setIp(Utils.getStringFromMap(map, "ip"));
		dl.setTtl(Utils.getIntegerFromMap(map, "ttl"));
		return dl;
	}

	@MsgbusMethod
	public void getRadius(Request req, Response resp) {
		resp.put("config", Marshaler.marshal(XtmConfig.readConfig(Radius.class)));
	}

	@MsgbusMethod
	public void setRadius(Request req, Response resp) {
		Radius radius = new Radius();
		radius.setType(Radius.Type.Radius);
		setRadius(radius, req.get("radius"));

		Radius domain = new Radius();
		domain.setType(Radius.Type.Domain);
		setRadius(domain, req.get("domain"));

		XtmConfig.writeConfig(Radius.class, Arrays.asList(radius, domain));
	}

	private void setRadius(Radius r, Object map) {
		if (r.getType() == Radius.Type.Radius) {
			r.setRadiusUse(Utils.getBooleanFromMap(map, "use"));
			r.setRadiusIp(Utils.getStringFromMap(map, "ip"));
			r.setRadiusPassword(Utils.getStringFromMap(map, "password"));
			r.setAuthMethod(AuthMethod.valueOf(Utils.getStringFromMap(map, "auth_method")));
			r.setAuthPort(Utils.getIntegerFromMap(map, "auth_port"));
			r.setAccountPort(Utils.getIntegerFromMap(map, "account_port"));
			r.setRadiusCycle(Utils.getIntegerFromMap(map, "radius_cycle"));
		} else if (r.getType() == Radius.Type.Domain) {
			r.setLdapAddress(Utils.getStringFromMap(map, "name"));
			r.setLdapUse(Utils.getBooleanFromMap(Utils.getFromMap(map, "directory"), "use"));
			r.setLdapType(Fileserver.get(Utils.getStringFromMap(Utils.getFromMap(map, "directory"), "value")));
			r.setLdapBaseDn(Utils.getStringFromMap(map, "base_dn"));
			r.setLdapAccount(Utils.getStringFromMap(map, "id"));
			r.setLdapPassword(Utils.getStringFromMap(map, "password"));
			r.setLdapCycle(Utils.getIntegerFromMap(map, "fs_cycle"));
			r.setLdapUseTrustStore(Utils.getBooleanFromMap(Utils.getFromMap(map, "trust_store"), "use"));
			if (r.isLdapUseTrustStore()) {
				String ts = Utils.getStringFromMap(Utils.getFromMap(map, "trust_store"), "value");
				if (ts != null) {
					try {
						new LdapProfile().setTrustStore(LdapProfile.CertificateType.X509, ts);
						r.setLdapTrustStore(ts);
					} catch (Exception e) {
						throw new RuntimeException(e);
					}
				}
			} else {
				r.setLdapTrustStore(null);
			}
		}
	}

	@MsgbusMethod
	public void getRouterChecker(Request req, Response resp) {
		resp.put("config", Marshaler.marshal(XtmConfig.readConfig(RouterChecker.class)));
	}

	@MsgbusMethod
	public void addRouterChecker(Request req, Response resp) {
		addNetwork(RouterChecker.class, req);
	}

	@MsgbusMethod
	public void modifyRouterChecker(Request req, Response resp) {
		modifyNetwork(RouterChecker.class, req, IdType.Num);
	}

	@MsgbusMethod
	public void removeRouterChecker(Request req, Response resp) {
		removeNetwork(RouterChecker.class, req, IdType.Num);
	}

	@SuppressWarnings("unused")
	private void setRouterChecker(RouterChecker rc, Request req) {
		rc.setContinuous(req.getBoolean("continuous"));
		rc.setAction(RouterChecker.Action.valueOf(req.getString("action")));
		rc.setName(req.getString("name"));
		rc.setIface(req.getString("interface"));
		rc.setIp(req.getString("ip"));
		rc.setPeriod(req.getInteger("period"));
		rc.setPool(req.getInteger("pool"));
		rc.setFail(req.getInteger("fail"));
		rc.setMac(req.getString("mac"));
	}

	@MsgbusMethod
	public void getRouterMulticast(Request req, Response resp) {
		resp.put("config", Marshaler.marshal(XtmConfig.readConfig(RouterMulticast.class)));
	}

	@SuppressWarnings("unchecked")
	@MsgbusMethod
	public void setRouterMulticast(Request req, Response resp) {
		RouterMulticast rm = new RouterMulticast();
		rm.setUse(req.getBoolean("use"));
		rm.setRp(RP.get(req.getString("rp")));
		rm.setBootstrapPriority(req.getInteger("bootstrap_priority"));
		rm.setCandidateCycle(Utils.getIntegerFromMap(req.get("candidate"), "cycle"));
		rm.setCandidatePriority(Utils.getIntegerFromMap(req.get("candidate"), "priority"));
		rm.setAddress(req.getString("address"));
		rm.setMultiRp(req.getBoolean("multi_rp"));
		rm.setRegisterRate(Utils.getIntegerFromMap(Utils.getFromMap(req.get("spt"), "register"), "rate"));
		rm.setRegisterCycle(Utils.getIntegerFromMap(Utils.getFromMap(req.get("spt"), "register"), "cycle"));
		rm.setDataRate(Utils.getIntegerFromMap(Utils.getFromMap(req.get("spt"), "data"), "rate"));
		rm.setDataCycle(Utils.getIntegerFromMap(Utils.getFromMap(req.get("spt"), "data"), "cycle"));
		rm.setIface((List<String>) req.get("interface"));

		XtmConfig.writeConfig(RouterMulticast.class, Arrays.asList(rm));
	}

	@MsgbusMethod
	public void getRouterPolicy(Request req, Response resp) {
		resp.put("config", Marshaler.marshal(XtmConfig.readConfig(RouterPolicy.class)));
	}

	@MsgbusMethod
	public void addRouterPolicy(Request req, Response resp) {
		addNetwork(RouterPolicy.class, req);
	}

	@MsgbusMethod
	public void modifyRouterPolicy(Request req, Response resp) {
		modifyNetwork(RouterPolicy.class, req, IdType.Num);
	}

	@MsgbusMethod
	public void removeRouterPolicy(Request req, Response resp) {
		removeNetwork(RouterPolicy.class, req, IdType.Num);
	}

	@SuppressWarnings("unused")
	private void setRouterPolicy(RouterPolicy rp, Request req) {
		rp.setPolicy(req.getInteger("policy"));
		rp.setIp(req.getString("ip"));
	}

	@MsgbusMethod
	public void getRouterStatic(Request req, Response resp) {
		resp.put("config", Marshaler.marshal(XtmConfig.readConfig(RouterStatic.class)));
	}

	@MsgbusMethod
	public void addRouterStatic(Request req, Response resp) {
		addNetwork(RouterStatic.class, req);
	}

	@MsgbusMethod
	public void modifyRouterStatic(Request req, Response resp) {
		modifyNetwork(RouterStatic.class, req, IdType.Num);
	}

	@MsgbusMethod
	public void removeRouterStatic(Request req, Response resp) {
		removeNetwork(RouterStatic.class, req, IdType.Num);
	}

	@SuppressWarnings("unused")
	private void setRouterStatic(RouterStatic rs, Request req) {
		rs.setUse(req.getBoolean("use"));
		rs.setPolicy((req.getString("policy").equals("Any")) ? null : Integer.parseInt(req.getString("policy")));
		rs.setIp(req.getString("ip"));
		rs.setGateway(req.getString("gateway"));
		rs.setIface(req.getString("interface"));
		rs.setMetric(req.getInteger("metric"));
	}

	@MsgbusMethod
	public void getRouterVrrp(Request req, Response resp) {
		resp.put("config", Marshaler.marshal(XtmConfig.readConfig(RouterVrrp.class)));
	}

	@MsgbusMethod
	public void addRouterVrrp(Request req, Response resp) {
		addNetwork(RouterVrrp.class, req);
	}

	@MsgbusMethod
	public void modifyRouterVrrp(Request req, Response resp) {
		modifyNetwork(RouterVrrp.class, req, IdType.Num);
	}

	@MsgbusMethod
	public void removeRouterVrrp(Request req, Response resp) {
		removeNetwork(RouterVrrp.class, req, IdType.Num);
	}

	@SuppressWarnings("unused")
	private void setRouterVrrp(RouterVrrp rv, Request req) {
		rv.setIface(req.getString("interface"));
		rv.setMode(RouterVrrp.Mode.get(req.getString("mode")));
		rv.setVid(req.getInteger("vid"));
		rv.setPriority(req.getInteger("priority"));
		rv.setPeriod(req.getInteger("period"));
		rv.setVip(req.getString("vip"));
		rv.setBoostup(req.getInteger("boostup"));
	}

	@MsgbusMethod
	public void getRouterScript(Request req, Response resp) {
		resp.put("config", Marshaler.marshal(XtmConfig.readConfig(RouterScript.class)));
	}

	@MsgbusMethod
	public void setRouterScript(Request req, Response resp) {
		String script = req.getString("script");

		List<RouterScript> config = new ArrayList<RouterScript>();
		RouterScript rs = new RouterScript();
		rs.setUseScript(req.getBoolean("use_script"));
		config.add(rs);
		XtmConfig.writeConfig(RouterScript.class, config);

		// write routing_script file
		FileOutputStream os = null;
		try {
			File f = new File("/etc/webadmin/conf/routing_script");
			os = new FileOutputStream(f);
			os.write(script.getBytes("utf-8"));
		} catch (FileNotFoundException e) {
			logger.error("frodo xtmconf: cannot open routing_script file", e);
		} catch (UnsupportedEncodingException e) {
		} catch (IOException e) {
			logger.error("frodo xtmconf: cannot write routing_script file", e);
		} finally {
			if (os != null)
				try {
					os.close();
				} catch (IOException e) {
				}
		}
	}

	@MsgbusMethod
	public void getSixInFour(Request req, Response resp) {
		resp.put("config", Marshaler.marshal(XtmConfig.readConfig(SixInFour.class)));
	}

	@MsgbusMethod
	public void addSixInFour(Request req, Response resp) {
		addNetwork(SixInFour.class, req);
	}

	@MsgbusMethod
	public void modifySixInFour(Request req, Response resp) {
		modifyNetwork(SixInFour.class, req, IdType.Cid);
	}

	@MsgbusMethod
	public void removeSixInFour(Request req, Response resp) {
		removeNetwork(SixInFour.class, req, IdType.Cid);
	}

	@SuppressWarnings("unused")
	private void setSixInFour(SixInFour sif, Request req) {
		sif.setLocal(req.getString("local"));
		sif.setRemote(req.getString("remote"));
		sif.setTtl(req.getInteger("ttl"));
		sif.setIndex(req.getString("index"));
	}

	@MsgbusMethod
	public void getSixToFour(Request req, Response resp) {
		resp.put("config", Marshaler.marshal(XtmConfig.readConfig(SixToFour.class)));
	}

	@MsgbusMethod
	public void setSixToFour(Request req, Response resp) {
		SixToFour stf = new SixToFour();
		stf.setUse(req.getBoolean("use"));
		stf.setIp(req.getString("ip"));
		stf.setRelay(req.getString("relay"));

		XtmConfig.writeConfig(SixToFour.class, Arrays.asList(stf));
	}

	@MsgbusMethod
	public void getVirtualIp(Request req, Response resp) {
		resp.put("config", Marshaler.marshal(XtmConfig.readConfig(VirtualIp.class)));
	}

	@MsgbusMethod
	public void addVirtualIp(Request req, Response resp) {
		addNetwork(VirtualIp.class, req);
	}

	@MsgbusMethod
	public void modifyVirtualIp(Request req, Response resp) {
		modifyNetwork(VirtualIp.class, req, IdType.Cid);
	}

	@MsgbusMethod
	public void removeVirtualIp(Request req, Response resp) {
		removeNetwork(VirtualIp.class, req, IdType.Cid);
	}

	@SuppressWarnings("unused")
	private void setVirtualIp(VirtualIp vi, Request req) {
		vi.setIface(req.getString("interface"));
		vi.setIp(req.getString("ip"));
		vi.setNetmask(req.getString("netmask"));
	}

	@MsgbusMethod
	public void getVirtualIpv6(Request req, Response resp) {
		resp.put("config", Marshaler.marshal(XtmConfig.readConfig(VirtualIpv6.class)));
	}

	@MsgbusMethod
	public void addVirtualIpv6(Request req, Response resp) {
		addNetwork(VirtualIpv6.class, req);
	}

	@MsgbusMethod
	public void modifyVirtualIpv6(Request req, Response resp) {
		modifyNetwork(VirtualIpv6.class, req, IdType.Cid);
	}

	@MsgbusMethod
	public void removeVirtualIpv6(Request req, Response resp) {
		removeNetwork(VirtualIpv6.class, req, IdType.Cid);
	}

	@SuppressWarnings("unused")
	private void setVirtualIpv6(VirtualIpv6 vi, Request req) {
		vi.setIface(req.getString("interface"));
		vi.setIp(req.getString("ip"));
		vi.setPrefix(req.getInteger("prefix"));
	}

	@MsgbusMethod
	public void getVlan(Request req, Response resp) {
		resp.put("config", Marshaler.marshal(XtmConfig.readConfig(Vlan.class)));
	}

	@MsgbusMethod
	public void addVlan(Request req, Response resp) {
		addNetwork(Vlan.class, req);
	}

	@MsgbusMethod
	public void modifyVlan(Request req, Response resp) {
		modifyNetwork(Vlan.class, req, IdType.Num);
	}

	@MsgbusMethod
	public void removeVlan(Request req, Response resp) {
		removeNetwork(Vlan.class, req, IdType.Num);
	}

	@SuppressWarnings("unused")
	private void setVlan(Vlan v, Request req) {
		v.setIface(req.getString("interface"));
		v.setId(req.getInteger("id"));
		v.setReordering(req.getBoolean("reordering"));
	}

	@MsgbusMethod
	public void setL2tpConfig(Request req, Response resp) {
		List<L2TPConfig> configs = new ArrayList<L2TPConfig>(2);
		L2TPConfig config = new L2TPConfig();
		config.setUse(req.getBoolean("use"));
		config.setClientIp(req.getString("client_ip"));
		config.setClientNetmask(req.getString("client_netmask"));
		config.setSecret(req.getString("secret"));
		config.setAuth(req.getString("auth"));
		config.setDns1(req.getString("dns1"));
		config.setDns2(req.getString("dns2"));

		configs.add(config);

		XtmConfig.writeConfig(L2TPConfig.class, configs);
	}

	@MsgbusMethod
	public void getL2tpConfig(Request req, Response resp) {
		resp.put("config", Marshaler.marshal(XtmConfig.readConfig(L2TPConfig.class)));
	}

	private <T extends XtmConfig> void addNetwork(Class<T> cls, Request req) {
		T t = null;
		try {
			t = cls.newInstance();
			try {
				cls.getMethod("setCid", String.class).invoke(t, UUID.randomUUID().toString());
			} catch (NoSuchMethodException e) {
			}
			Method method = this.getClass().getDeclaredMethod("set" + cls.getSimpleName(), cls, Request.class);
			method.setAccessible(true);
			method.invoke(this, t, req);

			List<T> config = XtmConfig.readConfig(cls);
			config.add(t);
			Utils.setConfigNum(config);
			XtmConfig.writeConfig(cls, config);
		} catch (Exception e) {
			logger.error("frodo-xtmconf: error in add object.", e);
			throw new IllegalArgumentException(e);
		}
	}

	private <T extends XtmConfig> void modifyNetwork(Class<T> cls, Request req, IdType id) {
		Object cid = req.get(id.name().toLowerCase());

		List<T> config = XtmConfig.readConfig(cls);
		try {
			for (T t : config) {
				if (cid.equals(t.getClass().getMethod("get" + id).invoke(t))) {
					Method method = this.getClass().getDeclaredMethod("set" + cls.getSimpleName(), cls, Request.class);
					method.setAccessible(true);
					method.invoke(this, t, req);
				}
			}

			XtmConfig.writeConfig(cls, config);
		} catch (Exception e) {
			logger.error("frodo-xtmconf: error in modify object.", e);
			throw new IllegalArgumentException(e);
		}
	}

	@SuppressWarnings("unchecked")
	private <T extends XtmConfig> void removeNetwork(Class<T> cls, Request req, IdType id) {
		List<Object> cids = (List<Object>) req.get(id.name().toLowerCase());

		List<T> config = XtmConfig.readConfig(cls);
		List<T> objs = new ArrayList<T>();
		try {
			for (T t : config) {
				if (cids.contains(t.getClass().getMethod("get" + id).invoke(t)))
					objs.add(t);
			}
			for (T obj : objs)
				config.remove(obj);

			Utils.setConfigNum(config);
			XtmConfig.writeConfig(cls, config);
		} catch (Exception e) {
			logger.error("frood-xtmconf: error in remove object.", e);
			throw new IllegalArgumentException(e);
		}
	}

	CopyOnWriteArraySet<ConfigEventListener> listeners = new CopyOnWriteArraySet<ConfigEventListener>();

	@Override
	public void addListener(ConfigEventListener listener) {
		listeners.add(listener);
		logger.trace("ConfigEventListener added: {}", listener);
	}

	@Override
	public void removeListener(ConfigEventListener listener) {
		listeners.remove(listener);
		logger.trace("ConfigEventListener removed: {}", listener);
	}
}
