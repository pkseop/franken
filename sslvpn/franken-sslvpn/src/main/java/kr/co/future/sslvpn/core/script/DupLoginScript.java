package kr.co.future.sslvpn.core.script;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;

import kr.co.future.sslvpn.core.DupLoginCheck;
import kr.co.future.sslvpn.core.GlobalConfig;
import kr.co.future.sslvpn.core.GlobalConfigApi;

import kr.co.future.api.Script;
import kr.co.future.api.ScriptArgument;
import kr.co.future.api.ScriptContext;
import kr.co.future.api.ScriptUsage;

public class DupLoginScript implements Script {

	private ScriptContext context;
	private GlobalConfigApi configApi;
	private DupLoginCheck dupLoginCheck;
	
	@Override
	public void setScriptContext(ScriptContext context) {
		this.context = context;
	}

	public DupLoginScript(GlobalConfigApi configApi, DupLoginCheck dupLoginCheck) {
		this.configApi = configApi;
		this.dupLoginCheck = dupLoginCheck;
	}
	
	public void loginList(String[] args) {
		context.println("user		node");
		context.println("==================================");
		ConcurrentMap<String, String> map = dupLoginCheck.getLoginInfoMap();
		for(Iterator<Map.Entry<String, String>> iter = map.entrySet().iterator(); iter.hasNext();) {
			Map.Entry<String, String> entry = iter.next();
			context.println(entry.getKey() + "		" + entry.getValue());
		}
	}
	
	@ScriptUsage(description = "find login user info", arguments = {
			@ScriptArgument(name = "set for using duplicate login check", type = "string", description = "search field like 'user', 'node'"),
			@ScriptArgument(name = "set for using duplicate login check", type = "string", description = "search word")})
	public void findLoginUserInfo(String[] args) {
		context.println("user		node");
		context.println("==================================");
		if(args[0].equals("user")) {
			ConcurrentMap<String, String> map = dupLoginCheck.getLoginInfoMap();
			if(map.get(args[1]) != null) {
				context.println(args[1] + "		" + map.get(args[1]));
			} 
		} else if(args[0].equals("node")) {
			ConcurrentMap<String, String> map = dupLoginCheck.getLoginInfoMap();
			for(Iterator<Map.Entry<String, String>> iter = map.entrySet().iterator(); iter.hasNext();) {
				Map.Entry<String, String> entry = iter.next();
				if(args[1].equals(entry.getValue())) {
					context.println(entry.getKey() + "		" + entry.getValue());
				}
			}
		} else {
			context.println("enter 2 arguments. choose 'user' or 'node' for field argument");
		}
	}
	
	@ScriptUsage(description = "remove login info for specific user", arguments = {
			@ScriptArgument(name = "remove login info for specific user", type = "string", description = "specific user") })
	public void removeLoginInfo(String[] args) {
		ConcurrentMap<String, String> map = dupLoginCheck.getLoginInfoMap();
		map.remove(args[0]);
		context.println("removed");
	}
	
	public void removeAllLoginInfo(String[] args) {
		ConcurrentMap<String, String> map = dupLoginCheck.getLoginInfoMap();
		map.clear();
		context.println("removed all login data for duplicate login check");
	}
	
	@ScriptUsage(description = "use duplicate login check", arguments = {
			@ScriptArgument(name = "set for using duplicate login check", type = "boolean", description = "true/false", optional = true) })
	public void use(String[] args) {
		GlobalConfig config = configApi.getGlobalConfig();

		if (config == null) {
			config = new GlobalConfig();
			config.setId(1);
		}
		
		if(args.length == 0) {
			context.println("use duplicate login check: " + config.getUseDupLoginCheck());
			return;
		}

		if (args.length > 1 || !(args[0].equals("true") || args[0].equals("false"))) {
			context.println("input 'true' or 'false'");
			return;
		}

		config.setUseDupLoginCheck(Boolean.parseBoolean(args[0]));
		configApi.setGlobalConfig(config);
		context.println("set");
		
		dupLoginCheck.setUseDuplicateLoginCheck(Boolean.parseBoolean(args[0]));
	}
    
    @ScriptUsage(description = "duplicate login check nodes", arguments = {
			@ScriptArgument(name = "set nodes for check duplicate login", type = "string", description = "", optional = true) })
	public void nodes(String[] args) {
		GlobalConfig config = configApi.getGlobalConfig();

		if (config == null) {
			config = new GlobalConfig();
			config.setId(1);
		}
		
		if(args.length == 0) {
			context.println("duplicate login check nodes: " + config.getDupLoginCheckNodes());
			return;
		}

		if (args.length > 1) {
			context.println("enter node parameter like this => '192.168.1.1,192.168.1.2,192.168.1.3'");
			return;
		}

		config.setDupLoginCheckNodes(args[0]);
		if(dupLoginCheck.setNodes(args[0])){
			configApi.setGlobalConfig(config);
			context.println("set");
		} else {
			context.println("failed.\nenter node parameter like this => '192.168.1.1,192.168.1.2,192.168.1.3'");
		}
	}
    
    @ScriptUsage(description = "block login for already logged in user", arguments = {
			@ScriptArgument(name = "set for blocking login to maintain previously logged in user", type = "boolean", description = "true/false", optional = true) })
	public void block(String[] args) {
		GlobalConfig config = configApi.getGlobalConfig();

		if (config == null) {
			config = new GlobalConfig();
			config.setId(1);
		}
		
		if(args.length == 0) {
			context.println("block setting: " + config.getBlockDupLoginCheck());
			return;
		}

		if (args.length > 1 || !(args[0].equals("true") || args[0].equals("false"))) {
			context.println("input 'true' or 'false'");
			return;
		}

		config.setBlockDupLoginCheck(Boolean.parseBoolean(args[0]));
		configApi.setGlobalConfig(config);
		context.println("set");
	}
}
