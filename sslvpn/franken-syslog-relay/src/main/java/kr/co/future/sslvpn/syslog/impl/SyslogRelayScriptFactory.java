package kr.co.future.sslvpn.syslog.impl;

import kr.co.future.sslvpn.syslog.SyslogRelayServer;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.apache.felix.ipojo.annotations.ServiceProperty;

import kr.co.future.api.Script;
import kr.co.future.api.ScriptFactory;

@Component(name = "frodo-syslog-relay-script-factory")
@Provides
public class SyslogRelayScriptFactory implements ScriptFactory {

	@SuppressWarnings("unused")
	@ServiceProperty(name = "alias", value = "frodo-syslog-relay")
	private String alias;

	@Requires
	private SyslogRelayServer relay;

	@Override
	public Script createScript() {
		return new SyslogRelayScript(relay);
	}
}
