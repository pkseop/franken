package kr.co.future.sslvpn.xtmconf.msgbus;

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
import kr.co.future.sslvpn.xtmconf.nac.Setting;
import kr.co.future.sslvpn.xtmconf.nac.Setting.Type;

@Component(name = "frodo-xtmconf-nac-plugin")
@MsgbusPlugin
public class NacPlugin {
	@MsgbusMethod
	public void getSetting(Request req, Response resp) {
		resp.put("config", Marshaler.marshal(XtmConfig.readConfig(Setting.class)));
	}

	@SuppressWarnings("unchecked")
	@MsgbusMethod
	public void setSetting(Request req, Response resp) {
		Setting setting = new Setting();
		setting.setType(Type.Setting);
		setting.setTimeout(Utils.getIntegerFromMap(req.get("setting"), "timeout"));
		setting.setIface(Utils.getStringFromMap(req.get("setting"), "interface"));

		Setting legacy = new Setting();
		legacy.setType(Type.Legacy);
		legacy.setLegacy((List<String>) Utils.getFromMap(req.get("legacy"), "mac"));

		XtmConfig.writeConfig(Setting.class, Arrays.asList(setting, legacy));
	}
}
