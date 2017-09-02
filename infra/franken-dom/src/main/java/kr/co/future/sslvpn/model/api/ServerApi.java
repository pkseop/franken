package kr.co.future.sslvpn.model.api;

import java.util.List;

import kr.co.future.dom.api.EntityEventProvider;
import kr.co.future.sslvpn.model.Server;

public interface ServerApi extends EntityEventProvider<Server> {
	List<Server> getServers();

	Server getServer(String guid);

	void createServer(Server server);

	void updateServer(Server server);

	void removeServer(String id);

	void removeServers(List<String> ids);
}
