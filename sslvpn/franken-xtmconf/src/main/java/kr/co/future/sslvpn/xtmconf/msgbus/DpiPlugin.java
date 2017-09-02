package kr.co.future.sslvpn.xtmconf.msgbus;

import java.util.Arrays;

import org.apache.felix.ipojo.annotations.Component;

import kr.co.future.msgbus.Marshaler;
import kr.co.future.msgbus.Request;
import kr.co.future.msgbus.Response;
import kr.co.future.msgbus.handler.MsgbusMethod;
import kr.co.future.msgbus.handler.MsgbusPlugin;
import kr.co.future.sslvpn.xtmconf.XtmConfig;
import kr.co.future.sslvpn.xtmconf.dpi.IpsSetting;

@Component(name = "frodo-xtmconf-dpi-plugin")
@MsgbusPlugin
public class DpiPlugin {
	@MsgbusMethod
	public void getIpsSetting(Request req, Response resp) {
		resp.put("config", Marshaler.marshal(XtmConfig.readConfig(IpsSetting.class)));
	}

	@MsgbusMethod
	public void setIpsSetting(Request req, Response resp) {
		IpsSetting is = new IpsSetting();
		is.setReassemble(req.getBoolean("reassemble"));
		is.setTcpServer(req.getBoolean("tcp_server"));
		is.setDecompression(req.getBoolean("decompression"));
		is.setIds(req.getBoolean("ids"));
		is.setAsymmetric(req.getBoolean("asymmetric"));
		is.setBypass(req.getBoolean("bypass"));

		XtmConfig.writeConfig(IpsSetting.class, Arrays.asList(is));
	}
}
