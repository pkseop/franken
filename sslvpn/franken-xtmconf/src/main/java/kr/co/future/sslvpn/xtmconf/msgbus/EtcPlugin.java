package kr.co.future.sslvpn.xtmconf.msgbus;

import java.util.Arrays;

import org.apache.felix.ipojo.annotations.Component;

import kr.co.future.msgbus.Marshaler;
import kr.co.future.msgbus.Request;
import kr.co.future.msgbus.Response;
import kr.co.future.msgbus.handler.MsgbusMethod;
import kr.co.future.msgbus.handler.MsgbusPlugin;
import kr.co.future.sslvpn.xtmconf.XtmConfig;
import kr.co.future.sslvpn.xtmconf.etc.FwScript;
import kr.co.future.sslvpn.xtmconf.etc.FwScript.Type;

@Component(name = "frodo-xtmconf-etc-plugin")
@MsgbusPlugin
public class EtcPlugin {
	@MsgbusMethod
	public void getFwScript(Request req, Response resp) {
		resp.put("config", Marshaler.marshal(XtmConfig.readConfig(FwScript.class)));
	}

	@MsgbusMethod
	public void setFwScript(Request req, Response resp) {
		FwScript mcForwarding = new FwScript();
		mcForwarding.setType(Type.McForwarding);
		mcForwarding.setMcForwarding(req.getBoolean("mc_forwarding"));

		FwScript idsPort = new FwScript();
		idsPort.setType(Type.IDSPort);
		idsPort.setIdsIface(req.getString("interface"));

		FwScript sip = new FwScript();
		sip.setType(Type.SIP);
		sip.setSip(req.getBoolean("sip"));

		XtmConfig.writeConfig(FwScript.class, Arrays.asList(mcForwarding, idsPort, sip));
	}
}
