package kr.co.future.sslvpn.xtmconf.msgbus;

import org.apache.felix.ipojo.annotations.Component;

import kr.co.future.msgbus.Request;
import kr.co.future.msgbus.Response;
import kr.co.future.msgbus.handler.MsgbusMethod;
import kr.co.future.msgbus.handler.MsgbusPlugin;
import kr.co.future.sslvpn.xtmconf.system.PolicySync;

@Component(name = "frodo-xtmconf-policy-sync-plugin")
@MsgbusPlugin
public class PolicySyncPlugin {
	@MsgbusMethod
	public void sync(Request req, Response resp) throws Exception {
		resp.put("policy_sync", PolicySync.sync());
	}
}