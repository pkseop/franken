package kr.co.future.sslvpn.xtmconf.msgbus;

import java.io.IOException;
import java.util.Map;
import java.util.Set;

import org.apache.felix.ipojo.annotations.Component;

import kr.co.future.msgbus.Request;
import kr.co.future.msgbus.Response;
import kr.co.future.msgbus.handler.MsgbusMethod;
import kr.co.future.msgbus.handler.MsgbusPlugin;
import kr.co.future.sslvpn.xtmconf.system.Rollback;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(name = "frodo-xtmconf-rollback-plugin")
@MsgbusPlugin
public class RollbackPlugin {
	private Logger logger = LoggerFactory.getLogger(this.getClass());

	@MsgbusMethod
	public void rollback(Request req, Response resp) {
		try {
			String type = req.getString("type");
			String version = req.getString("version");
			
			String msg = Rollback.rollback(type, version);
			resp.put("result", msg);
		} catch (Exception e) {
			logger.error("frodo-xtmconf: failed to rollback\n" + e.getMessage());
		}
	}

	@MsgbusMethod
	public void getRollbackList(Request req, Response resp) {
		Map<String, Set<String>> m = Rollback.getRollbackList();
		resp.put("firmware", m.get("firmware"));
		resp.put("ramdisk", m.get("ramdisk"));
		resp.put("appimg", m.get("appimg"));
		resp.put("jre", m.get("jre"));
		resp.put("sslvpn", m.get("sslvpn"));
	}

	@MsgbusMethod
	public void getCurrentVersion(Request req, Response resp) {
		try {
			Map<String, String> m = Rollback.getCurrentVersion();
			resp.put("firmware", m.get("firmware") == null ? "" : m.get("firmware"));
			resp.put("ramdisk", m.get("ramdisk") == null ? "" : m.get("ramdisk"));
			resp.put("appimg", m.get("appimg") == null ? "" : m.get("appimg"));
			resp.put("jre", m.get("jre") == null ? "" : m.get("jre"));
			resp.put("sslvpn", m.get("sslvpn") == null ? "" : m.get("sslvpn"));
		} catch (IOException e) {
			logger.error("frodo-xtmconf: failed to get current version" + e);
		}
	}
}