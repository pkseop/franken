package kr.co.future.sslvpn.syslog.impl;

import kr.co.future.sslvpn.syslog.SyslogRelayConfig;
import kr.co.future.sslvpn.syslog.SyslogRelayServer;
import kr.co.future.api.Script;
import kr.co.future.api.ScriptArgument;
import kr.co.future.api.ScriptContext;
import kr.co.future.api.ScriptUsage;

public class SyslogRelayScript implements Script {
	private SyslogRelayServer server;
	private ScriptContext context;

	public SyslogRelayScript(SyslogRelayServer server) {
		this.server = server;
	}

	@Override
	public void setScriptContext(ScriptContext context) {
		this.context = context;
	}

	@ScriptUsage(description = "list all syslog destinations")
	public void list(String[] args) {
		context.println("Syslog Relay");
		context.println("--------------");
		for (SyslogRelayConfig c : server.getConfigs()) {
			context.println(c);
		}
	}

	@ScriptUsage(description = "add syslog destination", arguments = {
			@ScriptArgument(name = "ip", type = "string", description = "syslog server ip"),
			@ScriptArgument(name = "port", type = "int", description = "syslog server port", optional = true) })
	public void add(String[] args) {
		try {
			String ip = args[0];
			Integer port = 514;
			if (args.length > 1)
				port = Integer.valueOf(args[1]);

			SyslogRelayConfig c = new SyslogRelayConfig(ip, port);
			server.addConfig(c);
			context.println("added");
		} catch (NumberFormatException e) {
			context.println("invalid port format");
		} catch (IllegalStateException e) {
			context.println("duplicated config");
		}
	}

	@ScriptUsage(description = "remove syslog destination", arguments = {
			@ScriptArgument(name = "ip", type = "string", description = "syslog"),
			@ScriptArgument(name = "port", type = "int", description = "syslog server port", optional = true) })
	public void remove(String[] args) {
		try {
			String ip = args[0];
			Integer port = 514;
			if (args.length > 1)
				port = Integer.valueOf(args[1]);
			
			server.removeConfig(new SyslogRelayConfig(ip, port));
			context.println("removed");
		} catch (NumberFormatException e) {
			context.println("invalid port format");
		} catch (IllegalStateException e) {
			context.println("not found");
		}
	}
}
