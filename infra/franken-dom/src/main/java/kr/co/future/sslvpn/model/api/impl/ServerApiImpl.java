package kr.co.future.sslvpn.model.api.impl;

import java.util.Date;
import java.util.List;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;

import kr.co.future.confdb.Config;
import kr.co.future.confdb.ConfigDatabase;
import kr.co.future.confdb.ConfigIterator;
import kr.co.future.confdb.ConfigService;
import kr.co.future.confdb.Predicates;
import kr.co.future.dom.api.DefaultEntityEventProvider;
import kr.co.future.msgbus.MsgbusException;
import kr.co.future.sslvpn.model.IpEndpoint;
import kr.co.future.sslvpn.model.Server;
import kr.co.future.sslvpn.model.api.ServerApi;

@Component(name = "frodo-server-api")
@Provides
public class ServerApiImpl extends DefaultEntityEventProvider<Server> implements ServerApi {
	@Requires
	private ConfigService conf;

	@Override
	public List<Server> getServers() {
		ConfigDatabase db = conf.ensureDatabase("frodo");
		ConfigIterator it = db.findAll(Server.class);
		return (List<Server>) it.getDocuments(Server.class);
	}

	@Override
	public Server getServer(String guid) {
		ConfigDatabase db = conf.ensureDatabase("frodo");
		Config c = db.findOne(Server.class, Predicates.field("guid", guid));
		if (c == null)
			return null;
		return c.getDocument(Server.class);
	}

	@Override
	public void createServer(Server server) {
		ConfigDatabase db = conf.ensureDatabase("frodo");
		server.setCreateDateTime(new Date());
		server.setUpdateDateTime(new Date());
		db.add(server);
		fireEntityAdded("localhost", server);
	}

	@Override
	public void updateServer(Server server) {
		ConfigDatabase db = conf.ensureDatabase("frodo");
		Config c = db.findOne(Server.class, Predicates.field("guid", server.getGuid()));
		if (c == null)
			throw new IllegalStateException("server not found: " + server.getGuid());

		Server old = c.getDocument(Server.class);

		old.setName(server.getName());
		old.setDescription(server.getDescription());
		old.setOperator(server.getOperator());
		old.setPhone(server.getPhone());
		old.setUpdateDateTime(new Date());

		old.getEndpoints().clear();

		for (IpEndpoint e : server.getEndpoints())
			old.getEndpoints().add(e);

		db.update(c, old, true);

		fireEntityUpdated("localhost", server);
	}

	@Override
	public void removeServer(String id) {
		ConfigDatabase db = conf.ensureDatabase("frodo");
		Config c = db.findOne(Server.class, Predicates.field("guid", id));

		if (c == null)
			throw new MsgbusException("frodo", "server-not-found");

		Server server = c.getDocument(Server.class);
		db.remove(c, true);

		fireEntityRemoved("localhost", server);
	}

	@Override
	public void removeServers(List<String> ids) {
		ConfigDatabase db = conf.ensureDatabase("frodo");
		ConfigIterator it = db.find(Server.class, Predicates.in("guid", ids));
		List<Server> servers = (List<Server>) it.getDocuments(Server.class);
		if (servers.size() != ids.size())
			throw new MsgbusException("frodo", "server-not-found");

		it = db.find(Server.class, Predicates.in("guid", ids));
		while (it.hasNext()) {
			Config c = it.next();
			Server server = c.getDocument(Server.class);
			db.remove(c, true);
			fireEntityRemoved("localhost", server);
		}
	}
}
