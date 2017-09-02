package kr.co.future.sslvpn.xtmconf.msgbus;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.felix.ipojo.annotations.Component;

import kr.co.future.msgbus.Marshaler;
import kr.co.future.msgbus.Request;
import kr.co.future.msgbus.Response;
import kr.co.future.msgbus.handler.MsgbusMethod;
import kr.co.future.msgbus.handler.MsgbusPlugin;
import kr.co.future.sslvpn.xtmconf.Utils;
import kr.co.future.sslvpn.xtmconf.XtmConfig;
import kr.co.future.sslvpn.xtmconf.ha.ActMode;
import kr.co.future.sslvpn.xtmconf.ha.BranchScript;
import kr.co.future.sslvpn.xtmconf.ha.HeadScript;
import kr.co.future.sslvpn.xtmconf.ha.LinkPackScript;
import kr.co.future.sslvpn.xtmconf.ha.SyncPolicySynchronize;
import kr.co.future.sslvpn.xtmconf.ha.SyncSessionBackup;
import kr.co.future.sslvpn.xtmconf.ha.SyncSessionGuarantee;
import kr.co.future.sslvpn.xtmconf.ha.BranchScript.BranchMode;
import kr.co.future.sslvpn.xtmconf.ha.BranchScript.Checker;
import kr.co.future.sslvpn.xtmconf.ha.BranchScript.ExternalChecker;
import kr.co.future.sslvpn.xtmconf.ha.BranchScript.PacketRelay;
import kr.co.future.sslvpn.xtmconf.ha.BranchScript.Type;
import kr.co.future.sslvpn.xtmconf.ha.BranchScript.ExternalChecker.CheckerMode;
import kr.co.future.sslvpn.xtmconf.ha.BranchScript.PacketRelay.Action;
import kr.co.future.sslvpn.xtmconf.ha.HeadScript.Mode;
import kr.co.future.sslvpn.xtmconf.ha.LinkPackScript.PackType;
import kr.co.future.sslvpn.xtmconf.ha.SyncPolicySynchronize.IntervalType;
import kr.co.future.sslvpn.xtmconf.ha.SyncSessionBackup.Act;

@Component(name = "frodo-xtmconf-ha-plugin")
@MsgbusPlugin
public class HaPlugin {
	@MsgbusMethod
	public void getBranchScript(Request req, Response resp) {
		resp.put("config", Marshaler.marshal(XtmConfig.readConfig(BranchScript.class)));
	}

	@MsgbusMethod
	public void setBranchScript(Request req, Response resp) {
		BranchScript mode = new BranchScript();
		mode.setType(BranchScript.Type.BranchMode);
		setBranchScript(mode, req.get(BranchScript.Type.BranchMode.toString()));

		BranchScript main = new BranchScript();
		main.setType(BranchScript.Type.Main);
		setBranchScript(main, req.get(BranchScript.Type.Main.toString()));

		BranchScript sub = new BranchScript();
		sub.setType(BranchScript.Type.Sub);
		setBranchScript(sub, req.get(BranchScript.Type.Sub.toString()));

		XtmConfig.writeConfig(BranchScript.class, Arrays.asList(mode, main, sub));
	}

	private void setBranchScript(BranchScript bs, Object map) {
		if (bs.getType() == Type.BranchMode)
			bs.setMode(BranchMode.get((String) map));
		else if (bs.getType() == Type.Main) {
			bs.setRelayMac(Utils.getStringFromMap(map, "relay_mac"));
			for (Object obj : Utils.getListFromMap(map, "relay_source"))
				bs.getRelaySource().add(getPacketRelay(obj));
			for (Object obj : Utils.getListFromMap(map, "relay_dest"))
				bs.getRelayDest().add(getPacketRelay(obj));
		}

		if (bs.getType() == Type.Main || bs.getType() == Type.Sub) {
			bs.setChecker(getBranchChecker(Utils.getFromMap(map, "checker")));
			for (Object obj : Utils.getListFromMap(map, "external"))
				bs.getExternal().add(getExternalChecker(obj));
		}
	}

	private PacketRelay getPacketRelay(Object map) {
		PacketRelay pr = new PacketRelay();
		pr.setAction(Action.get(Utils.getStringFromMap(map, "action")));
		pr.setIp(Utils.getStringFromMap(map, "ip"));
		return pr;
	}

	private Checker getBranchChecker(Object map) {
		Checker c = new Checker();
		c.setIface(Utils.getStringFromMap(map, "interface"));
		c.setName(Utils.getStringFromMap(map, "name"));
		c.setTargetIp(Utils.getStringFromMap(map, "target_ip"));
		c.setTimeout(Utils.getIntegerFromMap(map, "timeout"));
		return c;
	}

	private ExternalChecker getExternalChecker(Object map) {
		ExternalChecker ec = new ExternalChecker();
		ec.setIface(Utils.getStringFromMap(map, "interface"));
		ec.setName(Utils.getStringFromMap(map, "name"));
		ec.setTargetIp(Utils.getStringFromMap(map, "target_ip"));
		ec.setTimeout(Utils.getIntegerFromMap(map, "timeout"));
		ec.setMode(CheckerMode.get(Utils.getStringFromMap(map, "mode")));
		return ec;
	}

	@MsgbusMethod
	public void getHeadScript(Request req, Response resp) {
		resp.put("config", Marshaler.marshal(XtmConfig.readConfig(HeadScript.class)));
	}

	@MsgbusMethod
	public void setHeadScript(Request req, Response resp) {
		HeadScript head = new HeadScript();
		head.setType(HeadScript.Type.HeadMode);
		setHeadScript(head, req.get(HeadScript.Type.HeadMode.toString()));

		HeadScript mb = new HeadScript();
		mb.setType(HeadScript.Type.MasterBackup);
		setHeadScript(mb, req.get(HeadScript.Type.MasterBackup.toString()));

		HeadScript bridge = new HeadScript();
		bridge.setType(HeadScript.Type.Bridge);
		setHeadScript(bridge, req.get(HeadScript.Type.Bridge.toString()));

		XtmConfig.writeConfig(HeadScript.class, Arrays.asList(head, mb, bridge));
	}

	private void setHeadScript(HeadScript hs, Object map) {
		if (hs.getType() == HeadScript.Type.HeadMode)
			hs.setMode(Mode.get((String) map));
		else if (hs.getType() == HeadScript.Type.MasterBackup) {
			for (Object obj : Utils.getListFromMap(map, "checker"))
				hs.getChecker().add(getHeadChecker(obj));
			hs.setUseRealIp(Utils.getBooleanFromMap(map, "use_real_ip"));
		} else if (hs.getType() == HeadScript.Type.Bridge) {
			hs.setCheckerIface(Utils.getStringFromMap(Utils.getFromMap(map, "checker"), "interface"));
			hs.setCheckerName(Utils.getStringFromMap(Utils.getFromMap(map, "checker"), "name"));
			hs.setCheckerTargetIp(Utils.getStringFromMap(Utils.getFromMap(map, "checker"), "target_ip"));
			hs.setCheckerTimeout(Utils.getIntegerFromMap(Utils.getFromMap(map, "checker"), "timeout"));
			hs.setRelayInIface(Utils.getStringFromMap(Utils.getFromMap(Utils.getFromMap(map, "packet_relay"), "internal"),
					"interface"));
			hs.setRelayInMac(Utils.getStringFromMap(Utils.getFromMap(Utils.getFromMap(map, "packet_relay"), "internal"), "mac"));
			hs.setRelayExIface(Utils.getStringFromMap(Utils.getFromMap(Utils.getFromMap(map, "packet_relay"), "external"),
					"interface"));
			hs.setRelayExMac(Utils.getStringFromMap(Utils.getFromMap(Utils.getFromMap(map, "packet_relay"), "external"), "mac"));
		}
	}

	private HeadScript.Checker getHeadChecker(Object map) {
		HeadScript.Checker c = new HeadScript.Checker();
		c.setIface(Utils.getStringFromMap(map, "interface"));
		c.setName(Utils.getStringFromMap(map, "name"));
		c.setTargetIp(Utils.getStringFromMap(map, "target_ip"));
		c.setMode(HeadScript.Checker.Mode.get(Utils.getStringFromMap(map, "mode")));
		c.setVirtualIp(Utils.getStringFromMap(map, "virtual_ip"));
		c.setTimeout(Utils.getIntegerFromMap(map, "timeout"));
		return c;
	}

	@MsgbusMethod
	public void getLinkPackScript(Request req, Response resp) {
		resp.put("config", Marshaler.marshal(XtmConfig.readConfig(LinkPackScript.class)));
	}

	@SuppressWarnings("unchecked")
	@MsgbusMethod
	public void setLinkPackScript(Request req, Response resp) {
		List<LinkPackScript> config = new ArrayList<LinkPackScript>();

		for (Object map : (List<Object>) req.get("config")) {
			LinkPackScript lps = new LinkPackScript();
			lps.setType(PackType.get(Utils.getStringFromMap(map, "type")));
			lps.setName(Utils.getStringFromMap(map, "name"));
			lps.setChk(((List<Boolean>) Utils.getFromMap(map, "chk")).toArray(new Boolean[0]));
			config.add(lps);
		}

		XtmConfig.writeConfig(LinkPackScript.class, config);
	}

	@MsgbusMethod
	public void getSyncPolicySynchronize(Request req, Response resp) {
		resp.put("config", Marshaler.marshal(XtmConfig.readConfig(SyncPolicySynchronize.class)));
	}

	@MsgbusMethod
	public void setSyncPolicySynchronize(Request req, Response resp) {
		SyncPolicySynchronize sps = new SyncPolicySynchronize();
		sps.setUse(req.getBoolean("use"));
		sps.setAct(ActMode.get(req.getString("act")));
		sps.setPassword(req.getString("password"));
		sps.setIp(req.getString("ip"));
		sps.setPort(req.getInteger("port"));
		sps.setInterval(Utils.getBooleanFromMap(req.get("interval"), "use"));
		sps.setIntervalType(IntervalType.get(Utils.getStringFromMap(req.get("interval"), "type")));
		sps.setIntervalTerm(Utils.getIntegerFromMap(req.get("interval"), "term"));
		sps.setFwV4spd(Utils.getBooleanFromMap(req.get("fw_v4"), "spd"));
		sps.setFwV4nat(Utils.getBooleanFromMap(req.get("fw_v4"), "nat"));
		sps.setFwV6spd(Utils.getBooleanFromMap(req.get("fw_v6"), "spd"));
		sps.setFwV6nat(Utils.getBooleanFromMap(req.get("fw_v6"), "nat"));
		sps.setObjectIp(Utils.getBooleanFromMap(req.get("object"), "ip"));
		sps.setObjectService(Utils.getBooleanFromMap(req.get("object"), "service"));
		sps.setObjectFlow(Utils.getBooleanFromMap(req.get("object"), "flow"));
		sps.setObjectSchedule(Utils.getBooleanFromMap(req.get("object"), "schedule"));
		sps.setObjectQos(Utils.getBooleanFromMap(req.get("object"), "qos"));
		sps.setDpi(req.getBoolean("dpi"));
		sps.setIpsec(req.getBoolean("ipsec"));
		sps.setLog(Utils.getBooleanFromMap(req.get("log"), "log"));
		sps.setSyslog(Utils.getBooleanFromMap(req.get("log"), "syslog"));
		sps.setLogAlarm(Utils.getBooleanFromMap(req.get("log"), "alarm"));
		sps.setDomSync(Utils.getBooleanFromMap(req.get("dbsync"), "domsync"));
		Boolean chkDevice = Utils.getBooleanFromMap(req.get("dbsync"), "devicesync");
		sps.setDeviceSync(chkDevice == null ? false : chkDevice);

		XtmConfig.writeConfig(SyncPolicySynchronize.class, Arrays.asList(sps));
	}

	@MsgbusMethod
	public void getSyncSessionBackup(Request req, Response resp) {
		resp.put("config", Marshaler.marshal(XtmConfig.readConfig(SyncSessionBackup.class)));
	}

	@MsgbusMethod
	public void setSyncSessionBackup(Request req, Response resp) {
		SyncSessionBackup ssb = new SyncSessionBackup();
		ssb.setUse(req.getBoolean("use"));
		ssb.setMode(Act.get(req.getString("mode")));
		ssb.setSelectMode(ActMode.get(req.getString("select_mode")));
		ssb.setIface(req.getString("interface"));
		ssb.setIp(req.getString("ip"));
		ssb.setMac(req.getString("mac"));

		XtmConfig.writeConfig(SyncSessionBackup.class, Arrays.asList(ssb));
	}

	@MsgbusMethod
	public void getSyncSessionGuarantee(Request req, Response resp) {
		resp.put("config", Marshaler.marshal(XtmConfig.readConfig(SyncSessionGuarantee.class)));
	}

	@MsgbusMethod
	public void setSyncSessionGuarantee(Request req, Response resp) {
		SyncSessionGuarantee ssg = new SyncSessionGuarantee();
		ssg.setUse(req.getBoolean("use"));
		ssg.setAct(ActMode.get(req.getString("mode")));
		ssg.setIface(req.getString("interface"));
		ssg.setSgmac(req.getString("sgmac"));

		XtmConfig.writeConfig(SyncSessionGuarantee.class, Arrays.asList(ssg));
	}
}
