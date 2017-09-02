package kr.co.future.sslvpn.core.xenics.script;

import kr.co.future.sslvpn.core.xenics.XenicsConfig;
import kr.co.future.api.Script;
import kr.co.future.api.ScriptContext;
import kr.co.future.api.ScriptArgument;
import kr.co.future.api.ScriptUsage;

public class XenicsScript implements Script{

	private ScriptContext context;
	
	private XenicsConfig xenicsConfig;
	
	@Override
   public void setScriptContext(ScriptContext context) {
		this.context = context;	   
   }
	
	public XenicsScript(XenicsConfig xenicsConfig) {
		this.xenicsConfig = xenicsConfig;
	}

	@ScriptUsage(description = "setting to use remote db", arguments = @ScriptArgument(name = "flag", type = "boolean", description = "true/false"))
	public void setRemoteDbUsing(String[] args) {
		if(args[0].equals("true") || args[0].equals("false")){
			xenicsConfig.writeRemoteDbUsing(args[0]);			
			context.println("done! you should restart xenics to apply this config");
		} else {
			context.println("input 'true' or 'false'");
		}
	}
	
	public void getRemoteDbUsing(String[] args) {
		context.println("remote_db_using is [" + xenicsConfig.getRemoteDbUsing() + "]");
	}
	
	public void getRemoteSqlConf(String[] args) {
		context.println(xenicsConfig.getRemoteSqlConf());
	}
	
	@ScriptUsage(description = "setting to use remote db", arguments = {			
		@ScriptArgument(name = "hostname", type = "String", description = "hostname"),
		@ScriptArgument(name = "login", type = "String", description = "user name"),
		@ScriptArgument(name = "password", type = "String", description = "password"),
		@ScriptArgument(name = "db", type = "String", description = "db"),
		@ScriptArgument(name = "table", type = "String", description = "table"),
		@ScriptArgument(name = "port", type = "String", description = "port"),
		@ScriptArgument(name = "socket", type = "String", description = "socket")})
	public void setRemoteSqlConf(String[] args) {
		xenicsConfig.writeRemoteSqlConf(args[0], args[1], args[2], args[3], args[4], args[5], args[6]);			
		context.println("done! you should restart xenics to apply remote db config.");
	}
}
