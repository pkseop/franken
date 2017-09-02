package kr.co.future.sslvpn.xtmconf.msgbus;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import kr.co.future.sslvpn.model.api.UserApi;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Requires;

import kr.co.future.api.PrimitiveConverter;
import kr.co.future.msgbus.Marshaler;
import kr.co.future.msgbus.Request;
import kr.co.future.msgbus.Response;
import kr.co.future.msgbus.handler.MsgbusMethod;
import kr.co.future.msgbus.handler.MsgbusPlugin;
import kr.co.future.sslvpn.xtmconf.LastHitService;
import kr.co.future.sslvpn.xtmconf.Utils;
import kr.co.future.sslvpn.xtmconf.XtmConfig;
import kr.co.future.sslvpn.xtmconf.filtering.Address;
import kr.co.future.sslvpn.xtmconf.filtering.EtcAction;
import kr.co.future.sslvpn.xtmconf.filtering.FirewallIpv6;
import kr.co.future.sslvpn.xtmconf.filtering.FirewallIpv6Nat;
import kr.co.future.sslvpn.xtmconf.filtering.FirewallNat;
import kr.co.future.sslvpn.xtmconf.filtering.FirewallPolicy;
import kr.co.future.sslvpn.xtmconf.filtering.LogLevel;
import kr.co.future.sslvpn.xtmconf.filtering.Service;
import kr.co.future.sslvpn.xtmconf.filtering.VlanSetting;
import kr.co.future.sslvpn.xtmconf.filtering.Address.AddressType;
import kr.co.future.sslvpn.xtmconf.filtering.FirewallIpv6Nat.NatType;
import kr.co.future.sslvpn.xtmconf.filtering.FirewallIpv6Nat.Type;
import kr.co.future.sslvpn.xtmconf.filtering.Service.ServiceType;
import kr.co.future.sslvpn.xtmconf.ipsec.VpnIpsecsa;
import kr.co.future.sslvpn.xtmconf.network.Bridge;
import kr.co.future.sslvpn.xtmconf.network.Vlan;
import kr.co.future.sslvpn.xtmconf.object.IpAddress;
import kr.co.future.sslvpn.xtmconf.object.IpGroup;
import kr.co.future.sslvpn.xtmconf.object.Ipv6Address;
import kr.co.future.sslvpn.xtmconf.object.Ipv6Group;
import kr.co.future.sslvpn.xtmconf.object.Ipv6Header;
import kr.co.future.sslvpn.xtmconf.object.Qos;
import kr.co.future.sslvpn.xtmconf.object.Schedule;
import kr.co.future.sslvpn.xtmconf.object.ServiceGroup;
import kr.co.future.sslvpn.xtmconf.object.ServiceHttp;
import kr.co.future.sslvpn.xtmconf.object.ServicePort;
import kr.co.future.sslvpn.xtmconf.object.ServiceRpc;
import kr.co.future.sslvpn.xtmconf.object.Session;
import kr.co.future.sslvpn.xtmconf.object.UserGroup;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(name = "frodo-xtmconf-filtering-plugin")
@MsgbusPlugin
public class FilteringPlugin {
	private final Logger logger = LoggerFactory.getLogger(FilteringPlugin.class.getName());
	private final static String FireWallUidFile = "/etc/webadmin/xml/firewall_policy.uid";

	@Requires
	private kr.co.future.dom.api.UserApi domUserApi;

	@Requires
	private UserApi userApi;

	@Requires
	private LastHitService lastHitApi;

	@MsgbusMethod
	public void getLastHits(Request req, Response resp) {
		Map<String, Date> lastHits = lastHitApi.getLastHits();
		resp.put("lastHits", PrimitiveConverter.serialize(lastHits));
	}

	@MsgbusMethod
	public void getLastHit(Request req, Response resp) {
		String uid = req.getString("uid");
		logger.debug("frodo core: uid is [{}]", uid);

		Date lastHit = lastHitApi.getLastHit(uid);
		resp.put("lastHit", lastHit);
	}

	@MsgbusMethod
	public void getAllIpv6NatObjects(Request req, Response resp) {
		resp.put("ipv4_addr", Marshaler.marshal(XtmConfig.readConfig(IpAddress.class)));
		resp.put("ipv4_group", Marshaler.marshal(XtmConfig.readConfig(IpGroup.class)));
		resp.put("ipv6_addr", Marshaler.marshal(XtmConfig.readConfig(Ipv6Address.class)));
		resp.put("ipv6_group", Marshaler.marshal(XtmConfig.readConfig(Ipv6Group.class)));
		resp.put("service_port", Marshaler.marshal(XtmConfig.readConfig(ServicePort.class)));
		resp.put("service_group", Marshaler.marshal(XtmConfig.readConfig(ServiceGroup.class)));
		resp.put("service_http", Marshaler.marshal(XtmConfig.readConfig(ServiceHttp.class)));
		resp.put("service_rpc", Marshaler.marshal(XtmConfig.readConfig(ServiceRpc.class)));
		resp.put("nat", Marshaler.marshal(XtmConfig.readConfig(FirewallIpv6Nat.class)));
	}

	@MsgbusMethod
	public void getAllIpv6FilteringObjects(Request req, Response resp) {

		resp.put("ipv6_addr", Marshaler.marshal(XtmConfig.readConfig(Ipv6Address.class)));
		resp.put("ipv6_group", Marshaler.marshal(XtmConfig.readConfig(Ipv6Group.class)));
		resp.put("user_group", Marshaler.marshal(XtmConfig.readConfig(UserGroup.class)));
		resp.put("service_port", Marshaler.marshal(XtmConfig.readConfig(ServicePort.class)));
		resp.put("service_group", Marshaler.marshal(XtmConfig.readConfig(ServiceGroup.class)));
		resp.put("service_http", Marshaler.marshal(XtmConfig.readConfig(ServiceHttp.class)));
		resp.put("service_rpc", Marshaler.marshal(XtmConfig.readConfig(ServiceRpc.class)));
		resp.put("ipv6_header", Marshaler.marshal(XtmConfig.readConfig(Ipv6Header.class)));
		resp.put("schedule", Marshaler.marshal(XtmConfig.readConfig(Schedule.class)));
		resp.put("qos", Marshaler.marshal(XtmConfig.readConfig(Qos.class)));
		resp.put("session", Marshaler.marshal(XtmConfig.readConfig(Session.class)));
		resp.put("ipsecsa", Marshaler.marshal(XtmConfig.readConfig(VpnIpsecsa.class)));
		resp.put("filter", Marshaler.marshal(XtmConfig.readConfig(FirewallIpv6.class)));
	}

	@MsgbusMethod
	public void getAllIpv4NatObjects(Request req, Response resp) {
		resp.put("names", Utils.getInterfaceNames());
		resp.put("ipv4_addr", Marshaler.marshal(XtmConfig.readConfig(IpAddress.class)));
		resp.put("ipv4_group", Marshaler.marshal(XtmConfig.readConfig(IpGroup.class)));
		resp.put("user_group", Marshaler.marshal(XtmConfig.readConfig(UserGroup.class)));
		resp.put("service_port", Marshaler.marshal(XtmConfig.readConfig(ServicePort.class)));
		resp.put("service_group", Marshaler.marshal(XtmConfig.readConfig(ServiceGroup.class)));
		resp.put("vlan", Marshaler.marshal(XtmConfig.readConfig(Vlan.class)));
		resp.put("bridge", Marshaler.marshal(XtmConfig.readConfig(Bridge.class)));
		resp.put("nat", Marshaler.marshal(XtmConfig.readConfig(FirewallNat.class)));

	}

	@MsgbusMethod
	public void getAllIpv4FilteringObjects(Request req, Response resp) {

		resp.put("names", Utils.getInterfaceNames());
		resp.put("vlan", Marshaler.marshal(XtmConfig.readConfig(Vlan.class)));
		resp.put("bridge", Marshaler.marshal(XtmConfig.readConfig(Bridge.class)));
		resp.put("ipv4_addr", Marshaler.marshal(XtmConfig.readConfig(IpAddress.class)));
		resp.put("ipv4_group", Marshaler.marshal(XtmConfig.readConfig(IpGroup.class)));
		resp.put("user_group", Marshaler.marshal(XtmConfig.readConfig(UserGroup.class)));
		resp.put("service_port", Marshaler.marshal(XtmConfig.readConfig(ServicePort.class)));
		resp.put("service_group", Marshaler.marshal(XtmConfig.readConfig(ServiceGroup.class)));
		resp.put("service_http", Marshaler.marshal(XtmConfig.readConfig(ServiceHttp.class)));
		resp.put("service_rpc", Marshaler.marshal(XtmConfig.readConfig(ServiceRpc.class)));
		resp.put("schedule", Marshaler.marshal(XtmConfig.readConfig(Schedule.class)));
		resp.put("qos", Marshaler.marshal(XtmConfig.readConfig(Qos.class)));
		resp.put("session", Marshaler.marshal(XtmConfig.readConfig(Session.class)));
		resp.put("ipsecsa", Marshaler.marshal(XtmConfig.readConfig(VpnIpsecsa.class)));
		if (req.has("vlan_id"))
			FirewallPolicy.setVlanId(req.getInteger("vlan_id"));
		resp.put("filter", Marshaler.marshal(XtmConfig.readConfig(FirewallPolicy.class)));
		FirewallPolicy.setVlanId(null);
		resp.put("last_hits", lastHitApi.getLastHits());

	}

	@MsgbusMethod
	public void getFirewallIpv6(Request req, Response resp) {
		resp.put("config", Marshaler.marshal(XtmConfig.readConfig(FirewallIpv6.class)));
	}

	@MsgbusMethod
	public void addFirewallIpv6(Request req, Response resp) {
		FirewallIpv6 fi = new FirewallIpv6();
		fi.setCid(UUID.randomUUID().toString());
		setFirewallIpv6(fi, req);

		List<FirewallIpv6> config = XtmConfig.readConfig(FirewallIpv6.class);
		if (req.has("index"))
			config.add(req.getInteger("index"), fi);
		else
			config.add(fi);
		Utils.setConfigNum(config);
		XtmConfig.writeConfig(FirewallIpv6.class, config);
	}

	@MsgbusMethod
	public void modifyFirewallIpv6(Request req, Response resp) {
		String cid = req.getString("cid");

		List<FirewallIpv6> config = XtmConfig.readConfig(FirewallIpv6.class);
		FirewallIpv6 obj = null;
		for (FirewallIpv6 fi : config) {
			if (fi.getCid().equals(cid)) {
				if (req.getInteger("num") == fi.getNum())
					setFirewallIpv6(fi, req);
				else {
					obj = fi;
					break;
				}
			}
		}
		if (obj != null) {
			config.remove(obj);
			config.add(req.getInteger("num") - 1, obj);
			Utils.setConfigNum(config);
		}
		XtmConfig.writeConfig(FirewallIpv6.class, config);
	}

	@SuppressWarnings("unchecked")
	@MsgbusMethod
	public void removeFirewallIpv6(Request req, Response resp) {
		List<String> cids = (List<String>) req.get("cid");

		List<FirewallIpv6> config = XtmConfig.readConfig(FirewallIpv6.class);
		List<FirewallIpv6> objs = new ArrayList<FirewallIpv6>();
		for (FirewallIpv6 fi : config) {
			if (cids.contains(fi.getCid()))
				objs.add(fi);
		}
		for (FirewallIpv6 obj : objs)
			config.remove(obj);
		Utils.setConfigNum(config);
		XtmConfig.writeConfig(FirewallIpv6.class, config);
	}

	private void setFirewallIpv6(FirewallIpv6 fi, Request req) {
		fi.setUse(req.getBoolean("use"));
		fi.setSrc(getAddress(req.get("src")));
		fi.setDest(getAddress(req.get("dest")));
		fi.setHeaderCid(Utils.getStringFromMap(req.get("header"), "cid"));
		fi.setHeaderName(Utils.getStringFromMap(req.get("header"), "name"));
		fi.setService(getService(req.get("service")));
		fi.setScheduleCid(Utils.getStringFromMap(req.get("schedule"), "cid"));
		fi.setScheduleName(Utils.getStringFromMap(req.get("schedule"), "name"));
		fi.setQosCid(Utils.getStringFromMap(req.get("qos"), "cid"));
		fi.setQosName(Utils.getStringFromMap(req.get("qos"), "name"));
		fi.setSessionCid(Utils.getStringFromMap(req.get("session"), "cid"));
		fi.setSessionName(Utils.getStringFromMap(req.get("session"), "name"));
		fi.setIpsPid(Utils.getIntegerFromMap(req.get("ips"), "pid"));
		fi.setIps(Utils.getBooleanFromMap(req.get("ips"), "use"));
		fi.setIpsecsaCid(Utils.getStringFromMap(req.get("ipsecsa"), "cid"));
		fi.setIpsecsaName(Utils.getStringFromMap(req.get("ipsecsa"), "name"));
		fi.setEtcAction(EtcAction.valueOf(Utils.getStringFromMap(req.get("etc"), "action")));
		fi.setEtcTimeout(Utils.getIntegerFromMap(req.get("etc"), "timeout"));
		fi.setEtcCrossSpd(Utils.getBooleanFromMap(req.get("etc"), "cross_spd"));
		fi.setLoglevel(LogLevel.valueOf(req.getString("log_level")));
		fi.setDesc(req.getString("desc"));
	}

	@MsgbusMethod
	public void getFirewallIpv6Nat(Request req, Response resp) {
		resp.put("config", Marshaler.marshal(XtmConfig.readConfig(FirewallIpv6Nat.class)));
	}

	@MsgbusMethod
	public void setFirewallIpv6NatPrefix(Request req, Response resp) {
		List<FirewallIpv6Nat> config = XtmConfig.readConfig(FirewallIpv6Nat.class);
		FirewallIpv6Nat prefix = null;
		for (FirewallIpv6Nat fin : config) {
			if (fin.getType() == Type.Masquerading)
				prefix = fin;
		}

		if (prefix == null) {
			prefix = new FirewallIpv6Nat();
			prefix.setType(Type.Masquerading);
			config.add(prefix);
		}

		prefix.setMasqPrefix(req.getString("prefix"));

		XtmConfig.writeConfig(FirewallIpv6Nat.class, config);
	}

	@MsgbusMethod
	public void addFirewallIpv6Nat(Request req, Response resp) {
		FirewallIpv6Nat fin = new FirewallIpv6Nat();
		fin.setCid(UUID.randomUUID().toString());
		setFirewallIpv6Nat(fin, req);

		List<FirewallIpv6Nat> config = XtmConfig.readConfig(FirewallIpv6Nat.class);
		if (req.has("index"))
			config.add(req.getInteger("index"), fin);
		else
			config.add(fin);
		Utils.setConfigNum(config, Type.Nat);
		XtmConfig.writeConfig(FirewallIpv6Nat.class, config);
	}

	@MsgbusMethod
	public void modifyFirewallIpv6Nat(Request req, Response resp) {
		String cid = req.getString("cid");

		List<FirewallIpv6Nat> config = XtmConfig.readConfig(FirewallIpv6Nat.class);
		FirewallIpv6Nat obj = null;
		for (FirewallIpv6Nat fin : config) {
			if (fin.getType() == Type.Nat && cid.equals(fin.getCid())) {
				if (req.getInteger("num") == fin.getNum())
					setFirewallIpv6Nat(fin, req);
				else {
					obj = fin;
					break;
				}
			}
		}
		if (obj != null) {
			config.remove(obj);
			config.add(req.getInteger("num") - 1, obj);
			Utils.setConfigNum(config);
		}
		XtmConfig.writeConfig(FirewallIpv6Nat.class, config);
	}

	@SuppressWarnings("unchecked")
	@MsgbusMethod
	public void removeFirewallIpv6Nat(Request req, Response resp) {
		List<String> cids = (List<String>) req.get("cid");

		List<FirewallIpv6Nat> config = XtmConfig.readConfig(FirewallIpv6Nat.class);
		List<FirewallIpv6Nat> objs = new ArrayList<FirewallIpv6Nat>();
		for (FirewallIpv6Nat fin : config) {
			if (fin.getType() == Type.Nat && cids.contains(fin.getCid()))
				objs.add(fin);
		}
		for (FirewallIpv6Nat obj : objs)
			config.remove(obj);
		Utils.setConfigNum(config, Type.Nat);
		XtmConfig.writeConfig(FirewallIpv6Nat.class, config);
	}

	private void setFirewallIpv6Nat(FirewallIpv6Nat fin, Request req) {
		fin.setType(Type.Nat);
		fin.setUse(req.getBoolean("use"));
		fin.setNatType(NatType.get(req.getString("nat_type")));
		fin.setIp(getAddress(req.get("ip")));
		fin.setService(getService(req.get("service")));
		fin.setXip(getAddress(req.get("xip")));
		fin.setDesc(req.getString("desc"));
	}

	@MsgbusMethod
	public void getFirewallNat(Request req, Response resp) {
		resp.put("config", Marshaler.marshal(XtmConfig.readConfig(FirewallNat.class)));
	}

	@MsgbusMethod
	public void addFirewallNat(Request req, Response resp) {
		FirewallNat fn = new FirewallNat();
		fn.setCid(UUID.randomUUID().toString());
		setFirewallNat(fn, req);

		List<FirewallNat> config = XtmConfig.readConfig(FirewallNat.class);
		if (req.has("index"))
			config.add(req.getInteger("index"), fn);
		else
			config.add(fn);
		Utils.setConfigNum(config);
		XtmConfig.writeConfig(FirewallNat.class, config);
	}

	@MsgbusMethod
	public void modifyFirewallNat(Request req, Response resp) {
		String cid = req.getString("cid");

		List<FirewallNat> config = XtmConfig.readConfig(FirewallNat.class);
		FirewallNat obj = null;
		for (FirewallNat fn : config) {
			if (cid.equals(fn.getCid())) {
				if (req.getInteger("num") == fn.getNum())
					setFirewallNat(fn, req);
				else {
					obj = fn;
					break;
				}
			}
		}
		if (obj != null) {
			config.remove(obj);
			config.add(req.getInteger("num") - 1, obj);
			Utils.setConfigNum(config);
		}
		XtmConfig.writeConfig(FirewallNat.class, config);
	}

	@SuppressWarnings("unchecked")
	@MsgbusMethod
	public void removeFirewallNat(Request req, Response resp) {
		List<String> cids = (List<String>) req.get("cid");

		List<FirewallNat> config = XtmConfig.readConfig(FirewallNat.class);
		List<FirewallNat> objs = new ArrayList<FirewallNat>();
		for (FirewallNat fn : config) {
			if (cids.contains(fn.getCid()))
				objs.add(fn);
		}
		for (FirewallNat obj : objs)
			config.remove(obj);
		Utils.setConfigNum(config);
		XtmConfig.writeConfig(FirewallNat.class, config);
	}

	private void setFirewallNat(FirewallNat fn, Request req) {
		fn.setType(FirewallNat.NatType.valueOf(req.getString("type")));
		fn.setUse(req.getBoolean("use"));
		fn.setSrc(getAddress(req.get("src")));
		fn.setDest(getAddress(req.get("dest")));
		fn.setService(getService(req.get("service")));
		fn.setIface(req.getString("interface"));
		fn.setXsrc(getAddress(req.get("xsrc")));
		fn.setXdest(getAddress(req.get("xdest")));
		fn.setXservice(getService(req.get("xservice")));
		fn.setDesc(req.getString("desc"));
	}

	@MsgbusMethod
	public void getFirewallPolicy(Request req, Response resp) {
		if (req.has("vlan_id"))
			FirewallPolicy.setVlanId(req.getInteger("vlan_id"));
		resp.put("config", Marshaler.marshal(XtmConfig.readConfig(FirewallPolicy.class)));
		FirewallPolicy.setVlanId(null);
	}

	@MsgbusMethod
	public void addFirewallPolicy(Request req, Response resp) {
		if (req.has("vlan_id"))
			FirewallPolicy.setVlanId(req.getInteger("vlan_id"));

		FirewallPolicy fp = new FirewallPolicy();
		fp.setCid(UUID.randomUUID().toString());
		fp.setCreate(new Date());
		setFirewallPolicy(fp, req);

		int uid = 0;

		// initialize Uid Log
		File file = new File("/etc/webadmin/xml/firewall_policy.uid");

		if (!file.isFile()) {

			BufferedWriter bw;
			try {
				bw = new BufferedWriter(new FileWriter(FireWallUidFile));
				bw.write("0");
				bw.close();
			} catch (IOException e) {
				logger.error("frodo xtmconf: IOException [{}]", e);
			}

			uid = 0;
			fp.setUid(uid);
			logger.info("frodo xtmconf: initialize UID [{}]", 0);
		} else {

			BufferedReader br;
			BufferedWriter bw;
			try {
				br = new BufferedReader(new FileReader(FireWallUidFile));
				String line = br.readLine();
				line.replace("\r\n", "");
				int latestUid = Integer.valueOf(line);

				br.close();

				int CurrentUid = latestUid + 1;
				logger.debug("frodo xtmconf: increase UID [{}]", CurrentUid);

				if (CurrentUid > 65535) {
					logger.debug("frodo xtmconf: excessive number of maximum UID [{}]", CurrentUid);
					return;
				}

				bw = new BufferedWriter(new FileWriter(FireWallUidFile));
				bw.write(String.valueOf(CurrentUid));
				bw.close();

				logger.debug("frodo xtmconf: current UID [{}]", CurrentUid);

				uid = CurrentUid;
				fp.setUid(uid);

			} catch (FileNotFoundException e) {
				logger.error("frodo xtmconf: file Not Found [{}]", e);
			} catch (NumberFormatException e) {
				logger.error("frodo xtmconf: Number Format Error [{}]", e);
			} catch (IOException e) {
				logger.error("frodo xtmconf: IOException [{}]", e);
			}
		}

		List<FirewallPolicy> config = XtmConfig.readConfig(FirewallPolicy.class);
		if (req.has("index"))
			config.add(req.getInteger("index"), fp);
		else
			config.add(fp);
		Utils.setConfigNum(config);
		XtmConfig.writeConfig(FirewallPolicy.class, config);

		FirewallPolicy.setVlanId(null);
	}

	@MsgbusMethod
	public void modifyFirewallPolicy(Request req, Response resp) {
		if (req.has("vlan_id"))
			FirewallPolicy.setVlanId(req.getInteger("vlan_id"));

		String cid = req.getString("cid");

		List<FirewallPolicy> config = XtmConfig.readConfig(FirewallPolicy.class);
		FirewallPolicy obj = null;
		for (FirewallPolicy fp : config) {
			if (cid.equals(fp.getCid())) {
				if (req.getInteger("num") == fp.getNum())
					setFirewallPolicy(fp, req);
				else {
					obj = fp;
					break;
				}
			}
		}
		if (obj != null) {
			config.remove(obj);
			config.add(req.getInteger("num") - 1, obj);
			Utils.setConfigNum(config);
		}
		XtmConfig.writeConfig(FirewallPolicy.class, config);

		FirewallPolicy.setVlanId(null);
	}

	@SuppressWarnings("unchecked")
	@MsgbusMethod
	public void removeFirewallPolicy(Request req, Response resp) {
		if (req.has("vlan_id"))
			FirewallPolicy.setVlanId(req.getInteger("vlan_id"));

		List<String> cids = (List<String>) req.get("cid");

		List<FirewallPolicy> config = XtmConfig.readConfig(FirewallPolicy.class);
		List<FirewallPolicy> objs = new ArrayList<FirewallPolicy>();
		for (FirewallPolicy fp : config) {
			if (cids.contains(fp.getCid()))
				objs.add(fp);
		}
		for (FirewallPolicy obj : objs)
			config.remove(obj);
		Utils.setConfigNum(config);
		XtmConfig.writeConfig(FirewallPolicy.class, config);

		FirewallPolicy.setVlanId(null);
	}

	private void setFirewallPolicy(FirewallPolicy fp, Request req) {
		fp.setUse(req.getBoolean("use"));
		fp.setSrc(getAddress(req.get("src")));
		fp.setDest(getAddress(req.get("dest")));
		fp.setService(getService(req.get("service")));
		fp.setScheduleCid(Utils.getStringFromMap(req.get("schedule"), "cid"));
		fp.setScheduleName(Utils.getStringFromMap(req.get("schedule"), "name"));
		fp.setQosCid(Utils.getStringFromMap(req.get("qos"), "cid"));
		fp.setQosName(Utils.getStringFromMap(req.get("qos"), "name"));
		fp.setSessionCid(Utils.getStringFromMap(req.get("session"), "cid"));
		fp.setSessionName(Utils.getStringFromMap(req.get("session"), "name"));
		fp.setIps(Utils.getBooleanFromMap(req.get("ips"), "use"));
		fp.setIpsPid(Utils.getIntegerFromMap(req.get("ips"), "pid"));
		fp.setIpsecsaCid(Utils.getStringFromMap(req.get("ipsecsa"), "cid"));
		fp.setIpsecsaName(Utils.getStringFromMap(req.get("ipsecsa"), "name"));
		fp.setEtcAction(EtcAction.valueOf(Utils.getStringFromMap(req.get("etc"), "action")));
		fp.setEtcTimeout(Utils.getIntegerFromMap(req.get("etc"), "timeout"));
		fp.setEtcCrossSpd(Utils.getBooleanFromMap(req.get("etc"), "cross_spd"));
		fp.setLoglevel(LogLevel.valueOf(req.getString("log_level")));
		fp.setDesc(req.getString("desc"));
	}

	@MsgbusMethod
	public void getVlanSetting(Request req, Response resp) {
		resp.put("config", Marshaler.marshal(XtmConfig.readConfig(VlanSetting.class)));
	}

	@MsgbusMethod
	public void addVlanSetting(Request req, Response resp) {
		VlanSetting vs = new VlanSetting();
		vs.setCid(UUID.randomUUID().toString());
		setVlanSetting(vs, req);

		List<VlanSetting> config = XtmConfig.readConfig(VlanSetting.class);
		config.add(vs);
		Utils.setConfigNum(config);
		XtmConfig.writeConfig(VlanSetting.class, config);

		FirewallPolicy.setVlanId(vs.getId());
		XtmConfig.writeConfig(FirewallPolicy.class, new ArrayList<FirewallPolicy>());
		FirewallPolicy.setVlanId(null);
	}

	@MsgbusMethod
	public void modifyVlanSetting(Request req, Response resp) {
		String cid = req.getString("cid");

		List<VlanSetting> config = XtmConfig.readConfig(VlanSetting.class);
		for (VlanSetting vs : config) {
			if (cid.equals(vs.getCid()))
				setVlanSetting(vs, req);
		}
		XtmConfig.writeConfig(VlanSetting.class, config);
	}

	@SuppressWarnings("unchecked")
	@MsgbusMethod
	public void removeVlanSetting(Request req, Response resp) {
		List<String> cids = (List<String>) req.get("cid");

		List<VlanSetting> config = XtmConfig.readConfig(VlanSetting.class);
		List<VlanSetting> objs = new ArrayList<VlanSetting>();
		int id;
		for (VlanSetting vs : config) {
			if (cids.contains(vs.getCid()))
				objs.add(vs);
		}
		for (VlanSetting obj : objs) {
			config.remove(obj);

			id = obj.getId();
			FirewallPolicy.setVlanId(id);
			File f = new File(XtmConfig.getXmlDir(), new FirewallPolicy().getXmlFilename());
			f.delete();
			f.getParentFile().delete();
		}
		Utils.setConfigNum(config);
		XtmConfig.writeConfig(VlanSetting.class, config);

		FirewallPolicy.setVlanId(null);
	}

	private void setVlanSetting(VlanSetting vs, Request req) {
		vs.setName(req.getString("name"));
		vs.setId(req.getInteger("id"));
	}

	private Address getAddress(Object map) {
		Address a = new Address();
		a.setCid(Utils.getStringFromMap(map, "cid"));
		a.setType(AddressType.get(Utils.getStringFromMap(map, "type")));
		a.setName(Utils.getStringFromMap(map, "name"));
		return a;
	}

	private Service getService(Object map) {
		Service s = new Service();
		s.setCid(Utils.getStringFromMap(map, "cid"));
		s.setType(ServiceType.get(Utils.getStringFromMap(map, "type")));
		s.setName(Utils.getStringFromMap(map, "name"));
		return s;
	}
}
