package kr.co.future.sslvpn.core.msgbus;

import java.util.List;

import kr.co.future.sslvpn.core.msgbus.ServerPlugin;
import kr.co.future.sslvpn.model.Server;
import kr.co.future.sslvpn.model.api.ServerApi;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Requires;

import kr.co.future.api.PrimitiveConverter;
import kr.co.future.msgbus.Marshaler;
import kr.co.future.msgbus.Request;
import kr.co.future.msgbus.Response;
import kr.co.future.msgbus.handler.MsgbusMethod;
import kr.co.future.msgbus.handler.MsgbusPlugin;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@MsgbusPlugin
@Component(name = "frodo-server-plugin")
public class ServerPlugin {
	private final Logger logger = LoggerFactory.getLogger(ServerPlugin.class.getName());

	@Requires
	private ServerApi serverApi;

	@MsgbusMethod
	public void getServers(Request req, Response resp) {
		List<Server> servers = serverApi.getServers();
		resp.put("servers", Marshaler.marshal(servers));
	}

	@MsgbusMethod
	public void createServer(Request req, Response resp) {
		Server server = (Server) PrimitiveConverter.overwrite(new Server(), req.getParams());
		logger.info("frodo core: server guid: " + server.getGuid());
		serverApi.createServer(server);
		resp.put("guid", server.getGuid());
	}

	@MsgbusMethod
	public void updateServer(Request req, Response resp) {
		Server server = serverApi.getServer(req.getString("guid"));
		server = (Server) PrimitiveConverter.overwrite(server, req.getParams());
		serverApi.updateServer(server);
	}

	@MsgbusMethod
	public void removeServer(Request req, Response resp) {
		String guid = req.getString("guid");
		serverApi.removeServer(guid);
	}

	@SuppressWarnings("unchecked")
	@MsgbusMethod
	public void removeServers(Request req, Response resp) {
		List<String> guids = (List<String>) req.get("guids");
		serverApi.removeServers(guids);
	}
}
