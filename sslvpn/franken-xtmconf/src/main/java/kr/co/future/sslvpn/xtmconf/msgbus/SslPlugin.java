package kr.co.future.sslvpn.xtmconf.msgbus;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import org.apache.felix.ipojo.annotations.Component;

import kr.co.future.msgbus.Marshaler;
import kr.co.future.msgbus.Request;
import kr.co.future.msgbus.Response;
import kr.co.future.msgbus.handler.MsgbusMethod;
import kr.co.future.msgbus.handler.MsgbusPlugin;
import kr.co.future.sslvpn.xtmconf.Utils;
import kr.co.future.sslvpn.xtmconf.XtmConfig;
import kr.co.future.sslvpn.xtmconf.ssl.AccessGroup;
import kr.co.future.sslvpn.xtmconf.ssl.AccessServer;
import kr.co.future.sslvpn.xtmconf.ssl.Setting;
import kr.co.future.sslvpn.xtmconf.ssl.Setting.Protocol;

@Component(name = "frodo-xtmconf-ssl-plugin")
@MsgbusPlugin
public class SslPlugin {
	@MsgbusMethod
	public void getAccessGroup(Request req, Response resp) {
		resp.put("config", Marshaler.marshal(XtmConfig.readConfig(AccessGroup.class)));
	}

	@MsgbusMethod
	public void addAccessGroup(Request req, Response resp) {
		AccessGroup ag = new AccessGroup();
		ag.setCid(UUID.randomUUID().toString());
		setAccessGroup(ag, req);

		List<AccessGroup> config = XtmConfig.readConfig(AccessGroup.class);
		config.add(ag);
		Utils.setConfigNum(config);
		XtmConfig.writeConfig(AccessGroup.class, config);
	}

	@MsgbusMethod
	public void modifyAccessGroup(Request req, Response resp) {
		String cid = req.getString("cid");

		List<AccessGroup> config = XtmConfig.readConfig(AccessGroup.class);
		for (AccessGroup ag : config) {
			if (cid.equals(ag.getCid()))
				setAccessGroup(ag, req);
		}
		XtmConfig.writeConfig(AccessGroup.class, config);
	}

	@SuppressWarnings("unchecked")
	@MsgbusMethod
	public void removeAccessGroup(Request req, Response resp) {
		List<String> cids = (List<String>) req.get("cid");

		List<AccessGroup> config = XtmConfig.readConfig(AccessGroup.class);
		List<AccessGroup> objs = new ArrayList<AccessGroup>();
		for (AccessGroup ag : config) {
			if (cids.contains(ag.getCid()))
				objs.add(ag);
		}
		for (AccessGroup obj : objs)
			config.remove(obj);
		Utils.setConfigNum(config);
		XtmConfig.writeConfig(AccessGroup.class, config);
	}

	@SuppressWarnings("unchecked")
	private void setAccessGroup(AccessGroup ag, Request req) {
		ag.setName(req.getString("name"));
		ag.setIp(req.getString("ip"));
		ag.setNetmask(req.getString("netmask"));
		ag.setMember((List<String>) req.get("member"));
	}

	@MsgbusMethod
	public void getAccessServer(Request req, Response resp) {
		resp.put("config", Marshaler.marshal(XtmConfig.readConfig(AccessServer.class)));
	}

	@MsgbusMethod
	public void addAccessServer(Request req, Response resp) {
		AccessServer as = new AccessServer();
		as.setCid(UUID.randomUUID().toString());
		setAccessServer(as, req);

		List<AccessServer> config = XtmConfig.readConfig(AccessServer.class);
		config.add(as);
		Utils.setConfigNum(config);
		XtmConfig.writeConfig(AccessServer.class, config);
	}

	@MsgbusMethod
	public void modifyAccessServer(Request req, Response resp) {
		String cid = req.getString("cid");

		List<AccessServer> config = XtmConfig.readConfig(AccessServer.class);
		for (AccessServer as : config) {
			if (cid.equals(as.getCid()))
				setAccessServer(as, req);
		}
		XtmConfig.writeConfig(AccessServer.class, config);
	}

	@SuppressWarnings("unchecked")
	@MsgbusMethod
	public void removeAccessServer(Request req, Response resp) {
		List<String> cids = (List<String>) req.get("cid");

		List<AccessServer> config = XtmConfig.readConfig(AccessServer.class);
		List<AccessServer> objs = new ArrayList<AccessServer>();
		for (AccessServer as : config) {
			if (cids.contains(as.getCid()))
				objs.add(as);
		}
		for (AccessServer obj : objs)
			config.remove(obj);
		Utils.setConfigNum(config);
		XtmConfig.writeConfig(AccessServer.class, config);
	}

	private void setAccessServer(AccessServer as, Request req) {
		as.setName(req.getString("name"));
		as.setIp(req.getString("ip"));
		as.setNetmask(req.getString("netmask"));
	}

	@MsgbusMethod
	public void getSetting(Request req, Response resp) {
		resp.put("config", Marshaler.marshal(XtmConfig.readConfig(Setting.class)));
	}

	@MsgbusMethod
	public void setSetting(Request req, Response resp) {
		Setting s = new Setting();
		s.setUse(req.getBoolean("use"));
		s.setProtocol(Protocol.valueOf(req.getString("protocol")));
		s.setSettingOption(req.getBoolean("option"));
		s.setPort(req.getInteger("port"));
		s.setIp(req.getString("ip"));
		s.setNetmask(req.getString("netmask"));
		s.setMethodAuth(Utils.getIntegerFromMap(req.get("method"), "auth"));
		s.setMethodLogin(Utils.getIntegerFromMap(req.get("method"), "login"));

		XtmConfig.writeConfig(Setting.class, Arrays.asList(s));
	}
}
