package kr.co.future.sslvpn.xtmconf.msgbus;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;

import org.apache.felix.ipojo.annotations.Component;

import kr.co.future.msgbus.Marshaler;
import kr.co.future.msgbus.MsgbusException;
import kr.co.future.msgbus.Request;
import kr.co.future.msgbus.Response;
import kr.co.future.msgbus.handler.MsgbusMethod;
import kr.co.future.msgbus.handler.MsgbusPlugin;
import kr.co.future.sslvpn.xtmconf.Utils;
import kr.co.future.sslvpn.xtmconf.XtmConfig;
import kr.co.future.sslvpn.xtmconf.filtering.FirewallIpv6;
import kr.co.future.sslvpn.xtmconf.filtering.FirewallPolicy;
import kr.co.future.sslvpn.xtmconf.ipsec.KeySwapGroup;
import kr.co.future.sslvpn.xtmconf.ipsec.VpnHost;
import kr.co.future.sslvpn.xtmconf.ipsec.VpnIpsec;
import kr.co.future.sslvpn.xtmconf.ipsec.VpnIpsecsa;
import kr.co.future.sslvpn.xtmconf.ipsec.VpnIsakmpsa;
import kr.co.future.sslvpn.xtmconf.ipsec.VpnScript;
import kr.co.future.sslvpn.xtmconf.ipsec.VpnSetting;
import kr.co.future.sslvpn.xtmconf.ipsec.VpnIpsecsa.PhaseMode;
import kr.co.future.sslvpn.xtmconf.ipsec.VpnIpsecsa.PhaseProtocol;
import kr.co.future.sslvpn.xtmconf.ipsec.VpnIsakmpsa.Encryption;
import kr.co.future.sslvpn.xtmconf.ipsec.VpnIsakmpsa.IkeAuth;
import kr.co.future.sslvpn.xtmconf.ipsec.VpnIsakmpsa.PhaseAuth;
import kr.co.future.sslvpn.xtmconf.ipsec.VpnIsakmpsa.SettingMode;
import kr.co.future.sslvpn.xtmconf.ipsec.VpnIsakmpsa.XAuthLocation;
import kr.co.future.sslvpn.xtmconf.ipsec.VpnIsakmpsa.XAuthType;
import kr.co.future.sslvpn.xtmconf.ipsec.VpnScript.MultipathType;
import kr.co.future.sslvpn.xtmconf.ipsec.VpnScript.Type;

@Component(name = "frodo-xtmconf-ipsec-plugin")
@MsgbusPlugin
public class IpsecPlugin {
	
	@MsgbusMethod
	public void getVpnHost(Request req, Response resp) {
		resp.put("config", Marshaler.marshal(XtmConfig.readConfig(VpnHost.class)));
	}

	@MsgbusMethod
	public void addVpnHost(Request req, Response resp) {
		VpnHost vh = new VpnHost();
		vh.setCid(UUID.randomUUID().toString());
		setVpnHost(vh, req);

		List<VpnHost> config = XtmConfig.readConfig(VpnHost.class);
		config.add(vh);
		Utils.setConfigNum(config);
		XtmConfig.writeConfig(VpnHost.class, config);
	}

	@MsgbusMethod
	public void modifyVpnHost(Request req, Response resp) {
		String cid = req.getString("cid");

		List<VpnHost> config = XtmConfig.readConfig(VpnHost.class);
		for (VpnHost vh : config) {
			if (cid.equals(vh.getCid()))
				setVpnHost(vh, req);
		}
		XtmConfig.writeConfig(VpnHost.class, config);
	}

	@SuppressWarnings("unchecked")
	@MsgbusMethod
	public void removeVpnHost(Request req, Response resp) {
		List<String> cids = (List<String>) req.get("cid");

		List<VpnHost> config = XtmConfig.readConfig(VpnHost.class);
		List<VpnHost> objs = new ArrayList<VpnHost>();
		for (VpnHost vh : config) {
			if (cids.contains(vh.getCid()))
				objs.add(vh);
		}
		for (VpnHost obj : objs)
			config.remove(obj);
		Utils.setConfigNum(config);
		XtmConfig.writeConfig(VpnHost.class, config);
	}

	private void setVpnHost(VpnHost vh, Request req) {
		vh.setIp(req.getString("ip"));
	}

	@MsgbusMethod
	public void getVpnIpsec(Request req, Response resp) {
		resp.put("config", Marshaler.marshal(XtmConfig.readConfig(VpnIpsec.class)));
	}

	@MsgbusMethod
	public void addVpnIpsec(Request req, Response resp) {
		VpnIpsec vi = new VpnIpsec();
		vi.setCid(UUID.randomUUID().toString());
		setVpnIpsec(vi, req);

		List<VpnIpsec> config = XtmConfig.readConfig(VpnIpsec.class);
		config.add(vi);
		Utils.setConfigNum(config);
		XtmConfig.writeConfig(VpnIpsec.class, config);
	}

	@MsgbusMethod
	public void modifyVpnIpsec(Request req, Response resp) {
		String cid = req.getString("cid");

		List<VpnIpsec> config = XtmConfig.readConfig(VpnIpsec.class);
		for (VpnIpsec vi : config) {
			if (cid.equals(vi.getCid()))
				setVpnIpsec(vi, req);
		}
		XtmConfig.writeConfig(VpnIpsec.class, config);
	}

	@SuppressWarnings("unchecked")
	@MsgbusMethod
	public void removeVpnIpsec(Request req, Response resp) {
		List<String> cids = (List<String>) req.get("cid");

		List<VpnIpsec> config = XtmConfig.readConfig(VpnIpsec.class);
		List<VpnIpsec> objs = new ArrayList<VpnIpsec>();
		for (VpnIpsec vi : config) {
			if (cids.contains(vi.getCid()))
				objs.add(vi);
		}
		for (VpnIpsec obj : objs)
			config.remove(obj);
		Utils.setConfigNum(config);
		XtmConfig.writeConfig(VpnIpsec.class, config);
	}

	private void setVpnIpsec(VpnIpsec vi, Request req) {
		vi.setId(req.getString("id"));
		vi.setUse(req.getBoolean("use"));
		vi.setIp(req.getString("ip"));
		vi.setIsakmpsaCid(Utils.getStringFromMap(req.get("isakmpsa"), "cid"));
		vi.setIsakmpsaName(Utils.getStringFromMap(req.get("isakmpsa"), "name"));
		vi.setIface(req.getString("interface"));
		vi.setGroup(req.getInteger("group"));
	}

	@MsgbusMethod
	public void getVpnIpsecsa(Request req, Response resp) {
		resp.put("config", Marshaler.marshal(XtmConfig.readConfig(VpnIpsecsa.class)));
	}

	@MsgbusMethod
	public void addVpnIpsecsa(Request req, Response resp) {
		VpnIpsecsa vi = new VpnIpsecsa();
		vi.setCid(UUID.randomUUID().toString());
		setVpnIpsecsa(vi, req);

		List<VpnIpsecsa> config = XtmConfig.readConfig(VpnIpsecsa.class);
		config.add(vi);
		Utils.setConfigNum(config);
		XtmConfig.writeConfig(VpnIpsecsa.class, config);
	}

	@MsgbusMethod
	public void modifyVpnIpsecsa(Request req, Response resp) {
		String cid = req.getString("cid");

		List<VpnIpsecsa> config = XtmConfig.readConfig(VpnIpsecsa.class);
		for (VpnIpsecsa vi : config) {
			if (cid.equals(vi.getCid()))
				setVpnIpsecsa(vi, req);
		}
		XtmConfig.writeConfig(VpnIpsecsa.class, config);
	}

	@SuppressWarnings("unchecked")
	@MsgbusMethod
	public void removeVpnIpsecsa(Request req, Response resp) {
		List<String> cids = (List<String>) req.get("cid");
		for (String cid : cids) {
			boolean using = false;
			using |= VpnIsakmpsa.hasCid(cid);
			if (using)
				throw new MsgbusException("frodo", "occupied cid: isakmpsa");
			
			using |= FirewallIpv6.hasCid(cid);
			if (using)
				throw new MsgbusException("frodo", "occupied cid: firewall-ipv6");

			using |= FirewallPolicy.hasCid(cid);
			if (using)
				throw new MsgbusException("frodo", "occupied cid: firewall-policy");
		}

		List<VpnIpsecsa> config = XtmConfig.readConfig(VpnIpsecsa.class);
		List<VpnIpsecsa> objs = new ArrayList<VpnIpsecsa>();
		for (VpnIpsecsa vi : config) {
			if (cids.contains(vi.getCid()))
				objs.add(vi);
		}
		for (VpnIpsecsa obj : objs)
			config.remove(obj);
		Utils.setConfigNum(config);
		XtmConfig.writeConfig(VpnIpsecsa.class, config);
	}

	private void setVpnIpsecsa(VpnIpsecsa vi, Request req) {
		vi.setReplay(req.getBoolean("replay"));
		vi.setName(req.getString("name"));
		vi.setPhaseMode(PhaseMode.get(req.getString("phase_mode")));
		vi.setProtocol(PhaseProtocol.get(req.getString("protocol")));
		vi.setEncryption(req.getString("encryption"));
		vi.setAuth(req.getString("auth"));
		vi.setPfs(Utils.getBooleanFromMap(req.get("pfs"), "use"));
		vi.setPfsGroup(KeySwapGroup.get(Utils.getStringFromMap(req.get("pfs"), "group")));
		vi.setTime(req.getInteger("time"));
		vi.setLocal(req.getString("local"));
		vi.setRemote(req.getString("remote"));
	}

	@MsgbusMethod
	public void getVpnIsakmpsa(Request req, Response resp) {
		resp.put("config", Marshaler.marshal(XtmConfig.readConfig(VpnIsakmpsa.class)));
	}

	@MsgbusMethod
	public void addVpnIsakmpsa(Request req, Response resp) {
		VpnIsakmpsa vi = new VpnIsakmpsa();
		vi.setCid(UUID.randomUUID().toString());
		setVpnIsakmpsa(vi, req);

		List<VpnIsakmpsa> config = XtmConfig.readConfig(VpnIsakmpsa.class);
		config.add(vi);
		Utils.setConfigNum(config);
		XtmConfig.writeConfig(VpnIsakmpsa.class, config);
	}

	@MsgbusMethod
	public void modifyVpnIsakmpsa(Request req, Response resp) {
		String cid = req.getString("cid");
		String name = req.getString("name");
		VpnIpsec.updateCid(cid, name);

		List<VpnIsakmpsa> config = XtmConfig.readConfig(VpnIsakmpsa.class);
		for (VpnIsakmpsa vi : config) {
			if (cid.equals(vi.getCid()))
				setVpnIsakmpsa(vi, req);
		}
		XtmConfig.writeConfig(VpnIsakmpsa.class, config);
	}

	@SuppressWarnings("unchecked")
	@MsgbusMethod
	public void removeVpnIsakmpsa(Request req, Response resp) {
		List<String> cids = (List<String>) req.get("cid");
		for (String cid : cids) {
			boolean using = false;
			using |= VpnIpsec.hasCid(cid);
			if (using)
				throw new MsgbusException("frodo", "occupied cid: ipsec");
		}

		List<VpnIsakmpsa> config = XtmConfig.readConfig(VpnIsakmpsa.class);
		List<VpnIsakmpsa> objs = new ArrayList<VpnIsakmpsa>();
		for (VpnIsakmpsa vi : config) {
			if (cids.contains(vi.getCid()))
				objs.add(vi);
		}
		for (VpnIsakmpsa obj : objs)
			config.remove(obj);
		Utils.setConfigNum(config);
		XtmConfig.writeConfig(VpnIsakmpsa.class, config);
	}

	@SuppressWarnings("unchecked")
	private void setVpnIsakmpsa(VpnIsakmpsa vi, Request req) {
		vi.setNat(req.getBoolean("nat"));
		vi.setMode(SettingMode.valueOf(req.getString("mode")));
		vi.setAuth(IkeAuth.get(req.getString("auth")));
		vi.setName(req.getString("name"));
		vi.setPsk(req.getString("psk"));
		vi.setCert(req.getString("cert"));
		vi.setDpdPeriod(Utils.getIntegerFromMap(req.get("dpd"), "period"));
		vi.setDpdFail(Utils.getIntegerFromMap(req.get("dpd"), "fail"));
		vi.setPhaseMode(VpnIsakmpsa.PhaseMode.get(req.getString("phase_mode")));
		vi.setEncryption(Encryption.get(req.getString("encryption")));
		vi.setPhaseAuth(PhaseAuth.valueOf(req.getString("phase_auth")));
		vi.setSwapGroup(KeySwapGroup.valueOf(req.getString("swap_group")));
		vi.setTime(req.getInteger("time"));
		vi.setIpsecsa((List<String>) req.get("ipsecsa"));
		vi.setHost((List<String>) req.get("host"));
		vi.setUseIdChange(Utils.getBooleanFromMap(req.get("id_change"), "use"));
		vi.setIdChangeType(Utils.getStringFromMap(req.get("id_change"), "type"));
		vi.setIdChangeData(Utils.getStringFromMap(req.get("id_change"), "data"));
		vi.setUseXAuth(Utils.getBooleanFromMap(req.get("xauth"), "use"));
		vi.setxAuthType(XAuthType.get(Utils.getStringFromMap(req.get("xauth"), "type")));
		vi.setxAuthLocation(XAuthLocation.get(Utils.getStringFromMap(req.get("xauth"), "location")));
		vi.setxAuthId(Utils.getStringFromMap(req.get("xauth"), "id"));
		vi.setxAuthPassword(Utils.getStringFromMap(req.get("xauth"), "password"));
		vi.setPassive(req.getBoolean("passive"));
	}

	@MsgbusMethod
	public void getVpnScript(Request req, Response resp) {
		resp.put("config", Marshaler.marshal(XtmConfig.readConfig(VpnScript.class)));
	}

	@SuppressWarnings("unchecked")
	@MsgbusMethod
	public void setVpnScript(Request req, Response resp) {
		List<Object> conf = (List<Object>) req.get("config");

		// clear old config corresponds to input type
		List<VpnScript> config = XtmConfig.readConfig(VpnScript.class);

		Set<Type> types = new TreeSet<Type>();
		for (Object map : conf) {			
			Type type = Type.get(Utils.getStringFromMap(map, "type"));
			types.add(type);
		}

		List<VpnScript> targets = new ArrayList<VpnScript>();

		boolean hasVpnPolicy = false;

		for (VpnScript o : config) {
			if (types.contains(o.getType()))
				targets.add(o);

			if (o.getType().equals("vpn_policy"))
				hasVpnPolicy = true;
		}

		if (!hasVpnPolicy)
			for (VpnScript o : config)
				targets.add(o);

		for (VpnScript o : targets)
			config.remove(o);

		// add new config
		for (Object c : conf) {
			VpnScript vs = new VpnScript();
			setVpnScript(vs, c);
			config.add(vs);
		}

		XtmConfig.writeConfig(VpnScript.class, config);
	}

	@SuppressWarnings("unchecked")
	private void setVpnScript(VpnScript vs, Object map) {
		Type type = Type.get(Utils.getStringFromMap(map, "type"));
		vs.setType(type);
		if (type == Type.HeadOffice) {
			vs.setUse(Utils.getBooleanFromMap(map, "use"));
			vs.setTimeout(Utils.getIntegerFromMap(map, "timeout"));
		} else if (type == Type.ManageForward) {
			vs.setUse(Utils.getBooleanFromMap(map, "use"));
			vs.setIface(Utils.getStringFromMap(map, "interface"));
			vs.setIp(Utils.getStringFromMap(map, "ip"));
		} else if (type == Type.HubAndSpoke) {
			vs.setUse(Utils.getBooleanFromMap(map, "use"));
			vs.setUse2(Utils.getBooleanFromMap(map, "use_packet_relay"));
			vs.setIface(Utils.getStringFromMap(map, "interface"));
			vs.setMac(Utils.getStringFromMap(map, "mac"));
		} else if (type == Type.HeadOfficeBackup) {
			vs.setUse(Utils.getBooleanFromMap(map, "use"));
			vs.setTimeout(Utils.getIntegerFromMap(map, "timeout"));
			vs.setPrimaryIp((List<String>) Utils.getFromMap(map, "primary"));
			vs.setBackupIp((List<String>) Utils.getFromMap(map, "backup"));
		} else if (type == Type.VpnPolicy) {
			vs.setIp(Utils.getStringFromMap(map, "tunnel_ip"));
			vs.setIp2(Utils.getStringFromMap(map, "dest_ip"));
		} else if (type == Type.MultipathType)
			vs.setMultipathType(MultipathType.values()[Utils.getIntegerFromMap(map, "multipath_type")]);
		else if (type == Type.LineTimeout)
			vs.setTimeout(Utils.getIntegerFromMap(map, "timeout"));
		else if (type == Type.InoutInterfaceSync)
			vs.setIp(Utils.getStringFromMap(map, "ip"));
		else if (type == Type.VpnStandby || type == Type.VpnIpsecHostIpsec || type == Type.VpnForceUdpDecaps)
			vs.setUse(Utils.getBooleanFromMap(map, "use"));
		else if (type == Type.VpnXauthPoolIp)
			vs.setxAuthIp(Utils.getStringFromMap(map, "vpn_xauth_pool_ip"));
		else if (type == Type.VpnXauthPoolMask)
			vs.setxAuthNetmask(Utils.getStringFromMap(map, "vpn_xauth_pool_mask"));
		
	}

	@MsgbusMethod
	public void getVpnSetting(Request req, Response resp) {
		resp.put("config", Marshaler.marshal(XtmConfig.readConfig(VpnSetting.class)));
	}

	@SuppressWarnings("unchecked")
	@MsgbusMethod
	public void setVpnSetting(Request req, Response resp) {
		List<VpnSetting> config = new ArrayList<VpnSetting>();

		List<Object> c = (List<Object>) req.get("config");
		for (Object map : c) {
			VpnSetting vs = new VpnSetting();
			vs.setName(Utils.getStringFromMap(map, "name"));
			vs.setUse(Utils.getBooleanFromMap(map, "use"));
			config.add(vs);
		}

		XtmConfig.writeConfig(VpnSetting.class, config);
	}
}
