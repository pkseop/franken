package kr.co.future.sslvpn.xtmconf.msgbus;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.felix.ipojo.annotations.Component;

import kr.co.future.msgbus.Marshaler;
import kr.co.future.msgbus.Request;
import kr.co.future.msgbus.Response;
import kr.co.future.msgbus.handler.MsgbusMethod;
import kr.co.future.msgbus.handler.MsgbusPlugin;
import kr.co.future.sslvpn.xtmconf.Utils;
import kr.co.future.sslvpn.xtmconf.XtmConfig;
import kr.co.future.sslvpn.xtmconf.alg.FtpProxy;
import kr.co.future.sslvpn.xtmconf.alg.Permission;
import kr.co.future.sslvpn.xtmconf.alg.TelnetProxy;
import kr.co.future.sslvpn.xtmconf.alg.FtpProxy.User;
import kr.co.future.sslvpn.xtmconf.alg.FtpProxy.User.Type;

@Component(name = "frodo-xtmconf-alg-plugin")
@MsgbusPlugin
public class AlgPlugin {
	@MsgbusMethod
	public void getFtpProxy(Request req, Response resp) {
		resp.put("config", Marshaler.marshal(XtmConfig.readConfig(FtpProxy.class)));
	}

	@MsgbusMethod
	public void addFtpProxy(Request req, Response resp) {
		FtpProxy fp = new FtpProxy();
		setFtpProxy(fp, req);

		List<FtpProxy> config = XtmConfig.readConfig(FtpProxy.class);
		config.add(fp);
		Utils.setConfigNum(config);
		XtmConfig.writeConfig(FtpProxy.class, config);
	}

	@MsgbusMethod
	public void modifyFtpProxy(Request req, Response resp) {
		int num = req.getInteger("num");

		List<FtpProxy> config = XtmConfig.readConfig(FtpProxy.class);
		for (FtpProxy fp : config) {
			if (fp.getNum() == num)
				setFtpProxy(fp, req);
		}
		XtmConfig.writeConfig(FtpProxy.class, config);
	}

	@SuppressWarnings("unchecked")
	@MsgbusMethod
	public void removeFtpProxy(Request req, Response resp) {
		List<Integer> nums = (List<Integer>) req.get("num");

		List<FtpProxy> config = XtmConfig.readConfig(FtpProxy.class);
		List<FtpProxy> objs = new ArrayList<FtpProxy>();
		for (FtpProxy fp : config) {
			if (nums.contains(fp.getNum()))
				objs.add(fp);
		}

		for (FtpProxy obj : objs)
			config.remove(obj);
		Utils.setConfigNum(config);
		XtmConfig.writeConfig(FtpProxy.class, config);
	}

	private void setFtpProxy(FtpProxy fp, Request req) {
		fp.setAdverFtpIp(req.getString("advertised_ftp_id"));
		fp.setFtpIp(req.getString("ftp_ip"));
		fp.setIface(req.getString("interface"));
		fp.setPort(req.getInteger("port"));
		fp.setMaxUser(req.getInteger("max_user"));
		fp.setAction(req.getInteger("action"));
		fp.setUpload(Permission.get(req.getString("upload")));
		fp.setDownload(Permission.get(req.getString("download")));
		fp.setSizeUse(Utils.getBooleanFromMap(req.get("size"), "use"));
		fp.setSize(Utils.getIntegerFromMap(req.get("size"), "value"));
		fp.setExtensionUse(Utils.getBooleanFromMap(req.get("extension"), "use"));
		fp.setExtension(Utils.getStringFromMap(req.get("extension"), "value"));
		fp.setArgumentUse(Utils.getBooleanFromMap(req.get("argument"), "use"));
		fp.setArgument(Utils.getStringFromMap(req.get("argument"), "value"));
		for (Map<String, Object> m : Utils.convertList(Map.class, req.get("user"))) {
			User user = new User();
			user.setType(Type.get((String) m.get("type")));
			user.setPermission(Permission.get((String) m.get("permission")));
			user.setName((String) m.get("name"));
			fp.getUser().add(user);
		}
		fp.setCommandUse(Utils.getBooleanFromMap(req.get("command"), "use"));
		fp.setCommand(Utils.getStringFromMap(req.get("command"), "value"));
	}

	@MsgbusMethod
	public void getTelnetProxy(Request req, Response resp) {
		resp.put("config", Marshaler.marshal(XtmConfig.readConfig(TelnetProxy.class)));
	}

	@MsgbusMethod
	public void addTelnetProxy(Request req, Response resp) {
		TelnetProxy fp = new TelnetProxy();
		setTelnetProxy(fp, req);

		List<TelnetProxy> config = XtmConfig.readConfig(TelnetProxy.class);
		config.add(fp);
		Utils.setConfigNum(config);
		XtmConfig.writeConfig(TelnetProxy.class, config);
	}

	@MsgbusMethod
	public void modifyTelnetProxy(Request req, Response resp) {
		int num = req.getInteger("num");

		List<TelnetProxy> config = XtmConfig.readConfig(TelnetProxy.class);
		for (TelnetProxy tp : config) {
			if (tp.getNum() == num)
				setTelnetProxy(tp, req);
		}
		XtmConfig.writeConfig(TelnetProxy.class, config);
	}

	@SuppressWarnings("unchecked")
	@MsgbusMethod
	public void removeTelnetProxy(Request req, Response resp) {
		List<Integer> nums = (List<Integer>) req.get("num");

		List<TelnetProxy> config = XtmConfig.readConfig(TelnetProxy.class);
		List<TelnetProxy> objs = new ArrayList<TelnetProxy>();
		for (TelnetProxy tp : config) {
			if (nums.contains(tp.getNum()))
				objs.add(tp);
		}
		for (TelnetProxy obj : objs)
			config.remove(obj);
		Utils.setConfigNum(config);
		XtmConfig.writeConfig(TelnetProxy.class, config);
	}

	private void setTelnetProxy(TelnetProxy fp, Request req) {
		fp.setPublicIp(req.getString("public_ip"));
		fp.setIp(req.getString("ip"));
		fp.setPort(req.getInteger("port"));
		fp.setLimit(req.getInteger("limit"));
		fp.setUser(req.getInteger("user"));
		fp.setUpload(Permission.get(req.getString("upload")));
		fp.setDownload(Permission.get(req.getString("download")));
		fp.setAction(req.getInteger("action"));
	}
}
