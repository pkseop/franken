package kr.co.future.sslvpn.core.impl;

import java.util.ArrayList;
import java.util.List;

import kr.co.future.sslvpn.core.AuthService;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;

import kr.co.future.radius.server.RadiusConfigMetadata;
import kr.co.future.radius.server.RadiusInstanceConfig;
import kr.co.future.radius.server.RadiusModuleType;
import kr.co.future.radius.server.RadiusUserDatabase;
import kr.co.future.radius.server.RadiusUserDatabaseFactory;
import kr.co.future.sslvpn.core.impl.RadiusSslUserDatabase;

@Component(name = "frodo-radius-userdb-factory")
@Provides
public class RadiusSslUserDatabaseFactory implements RadiusUserDatabaseFactory {

	@Requires
	private AuthService auth;

	@Override
	public RadiusModuleType getModuleType() {
		return RadiusModuleType.UserDatabase;
	}

	@Override
	public String getName() {
		return "ssluserdb";
	}

	@Override
	public List<RadiusConfigMetadata> getConfigMetadatas() {
		return new ArrayList<RadiusConfigMetadata>();
	}

	@Override
	public RadiusUserDatabase newInstance(RadiusInstanceConfig config) {
		return new RadiusSslUserDatabase(config.getName(), this, auth);
	}
}
