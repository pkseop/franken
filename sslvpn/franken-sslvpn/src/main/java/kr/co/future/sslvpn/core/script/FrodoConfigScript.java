package kr.co.future.sslvpn.core.script;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import kr.co.future.sslvpn.core.GlobalConfig;
import kr.co.future.sslvpn.core.GlobalConfigApi;
import kr.co.future.sslvpn.core.SqlAuthService;
import kr.co.future.sslvpn.core.UserMigrationServiceApi;
import kr.co.future.api.DirectoryAutoCompleter;
import kr.co.future.api.Script;
import kr.co.future.api.ScriptArgument;
import kr.co.future.api.ScriptContext;
import kr.co.future.api.ScriptUsage;
import kr.co.future.dns.DnsResolverProvider;
import kr.co.future.dns.DnsService;

public class FrodoConfigScript implements Script{
	
	private ScriptContext context;
	private GlobalConfigApi configApi;
	private SqlAuthService sqlAuthService;
    private UserMigrationServiceApi userMigrationServiceApi;
    private DnsService dnsService;
	
	public FrodoConfigScript(GlobalConfigApi configApi, SqlAuthService sqlAuthService, UserMigrationServiceApi userMigrationServiceApi, DnsService dnsService) {
		this.configApi = configApi;
		this.sqlAuthService = sqlAuthService;
        this.userMigrationServiceApi = userMigrationServiceApi;
        this.dnsService = dnsService;
	}
	
	@Override
	public void setScriptContext(ScriptContext context) {
		this.context = context;
	}
	
	@ScriptUsage(description = "set user ui path", arguments = @ScriptArgument(name = "user ui path", type = "string", optional = true, autocompletion = DirectoryAutoCompleter.class))
	public void userUiPath(String[] args) {
		GlobalConfig config = configApi.getGlobalConfig();

		if (args.length < 1) {
			if (config == null)
				context.println("not set");
			else
				context.println("user ui path=" + config.getUserUiPath());
			return;
		}

		String dirPath = args[0];
		if (dirPath.trim().isEmpty()) {
			if (config != null) {
				config.setUserUiPath(null);
				configApi.setGlobalConfig(config);
				return;
			}
		}

		File dir = new File(dirPath);
		if (!dir.exists() || !dir.isDirectory()) {
			context.println("invalid path");
			return;
		}

		if (config != null) {
			config.setUserUiPath(dirPath);
		} else {
			config = new GlobalConfig();
			config.setId(1);
			config.setUserUiPath(dirPath);
		}
		configApi.setGlobalConfig(config);
		context.println("set user ui path" + config.getUserUiPath());
	}
	
	@ScriptUsage(description = "set do not show admin console", arguments = @ScriptArgument(name = "do not show admin console", type = "boolean", optional = true))
	public void donotShowAdmin(String[] args) {
		if(args.length > 1){
			context.println("enter \'true\' or \'false\'");
			return;
		}
		
		GlobalConfig config = configApi.getGlobalConfig();

		if (args.length == 0) {
			context.println("current do not show admin console is " + config.isDoNotShowAdmin());
			return;
		}

		boolean donotShowAdmin = Boolean.parseBoolean(args[0]);

		if (config != null) {
			config.setDoNotShowAdmin(donotShowAdmin);
		}
		configApi.setGlobalConfig(config);
		context.println("set do not show admin console = " + config.isDoNotShowAdmin());
	}

	@ScriptUsage(description = "set admin console path", arguments = @ScriptArgument(name = "admin console path", type = "string", optional = true))
	public void adminConsolePath(String[] args) {
		GlobalConfig config = configApi.getGlobalConfig();

		if (args.length < 1) {
			if (config == null)
				context.println("not set");
			else
				context.println("admin console path=" + config.getAdminConsolePath());
			return;
		}

		String dirPath = args[0];
		if (dirPath.trim().isEmpty()) {
			if (config != null) {
				config.setAdminConsolePath(null);
				configApi.setGlobalConfig(config);
				return;
			}
		}

		File dir = new File(dirPath);
		if (!dir.exists() || !dir.isDirectory()) {
			context.println("invalid path");
			return;
		}
		
		if (config != null) {
			config.setAdminConsolePath(dirPath);
		} else {
			config = new GlobalConfig();
			config.setId(1);
			config.setAdminConsolePath(dirPath);
		}
		configApi.setGlobalConfig(config);
		context.println("set admin console path=" + config.getAdminConsolePath());
	}
	
	@ScriptUsage(description = "set admin console context", arguments = @ScriptArgument(name = "admin console context", type = "string", optional = true))
	public void adminConsoleContext(String[] args) {
		GlobalConfig config = configApi.getGlobalConfig();
		if (args.length < 1) {
			if (config == null)
				context.println("not set");
			else
				context.println("admin console context=" + config.getAdminConsoleContext());
			return;
		}
		
		if (config != null) {
			config.setAdminConsoleContext(args[0]);
		} else {
			config = new GlobalConfig();
			config.setId(1);
			config.setAdminConsoleContext(args[0]);
		}
		configApi.setGlobalConfig(config);
		context.println("set admin console context=" + config.getAdminConsoleContext());
	}
	
	@ScriptUsage(description = "get download visibility")
	public void getDownloadVisibility(String[] args) {
		GlobalConfig config = configApi.getGlobalConfig();
		if (config == null) {
			context.println("not set");
			return;
		}

		context.println("download visibility");
		context.println("---------------------");
		context.println("windows client: " + config.isShowWindowsClientDownload());
		context.println("linux client: " + config.isShowLinuxClientDownload());
		context.println("ios client: " + config.isShowIosClientDownload());
		context.println("android client: " + config.isShowAndroidClientDownload());
		context.println("manual: " + config.isShowManualDownload());
	}
	
	@ScriptUsage(description = "set download visibility", arguments = {
			@ScriptArgument(name = "windows client", type = "boolean"), @ScriptArgument(name = "linux client", type = "boolean"),
			@ScriptArgument(name = "ios client", type = "boolean"), @ScriptArgument(name = "android client", type = "boolean"),
			@ScriptArgument(name = "manual", type = "boolean") })
	public void setDownloadVisibility(String[] args) {
		GlobalConfig config = configApi.getGlobalConfig();

		if (config == null) {
			config = new GlobalConfig();
			config.setId(1);
		}

		config.setShowWindowsClientDownload(Boolean.parseBoolean(args[0]));
		config.setShowLinuxClientDownload(Boolean.parseBoolean(args[1]));
		config.setShowIosClientDownload(Boolean.parseBoolean(args[2]));
		config.setShowAndroidClientDownload(Boolean.parseBoolean(args[3]));
		config.setShowManualDownload(Boolean.parseBoolean(args[4]));

		configApi.setGlobalConfig(config);
	}

	@ScriptUsage(description = "windows client icon", arguments = {
			@ScriptArgument(name = "windows client", type = "string", description = "true or false", optional = true),
			@ScriptArgument(name = "windows client connection icon add", type = "string", description = "true or false", optional = true) })
	public void windowsClientIcon(String[] args) {
		GlobalConfig config = configApi.getGlobalConfig();

		if (config == null) {
			config = new GlobalConfig();
			config.setId(1);
		}

		if (args == null || args.length <= 1) {
			context.println("windows client: " + config.isShowWindowsClientIcon());
			context.println("windows client connection icon add: " + config.isShowWindowsClientConnectionIcon());
			return;
		}

		config.setShowWindowsClientIcon(Boolean.parseBoolean(args[0]));
		config.setShowWindowsClientConnectionIcon(Boolean.parseBoolean(args[1]));
		configApi.setGlobalConfig(config);
		context.println("set");
	}
	
	@ScriptUsage(description = "set device key expire time", arguments = { @ScriptArgument(name = "days", type = "string", description = "days or unlimited") })
	public void setDeviceKeyExpireTime(String[] args) {
		GlobalConfig config = configApi.getGlobalConfig();

		if (config == null) {
			config = new GlobalConfig();
			config.setId(1);
		}

		if (args[0].equals("unlimited")) {
			config.setDeviceKeyExpiryDuration(null);
			context.println("device expire time set unlimited");
		} else {
			int days = Integer.parseInt(args[0]);
			config.setDeviceKeyExpiryDuration(days);
			context.println("device expire time set " + days + "days");
		}

		configApi.setGlobalConfig(config);
	}
	
	@ScriptUsage(description = "encrypt vpninfo", arguments = { @ScriptArgument(name = "encrypt", type = "boolean", description = "true or false", optional = true) })
	public void encryptVpninfo(String[] args) {
		if(args.length > 1){
			context.println("enter \'true\' or \'false\'");
			return;
		}
		
		GlobalConfig config = configApi.getGlobalConfig();
		
		if(args.length == 0){			
			context.println("encrypt vpninfo: " + config.isEncryptVpninfo());
			return;
		}

		if (config == null) {
			config = new GlobalConfig();
			config.setId(1);
		}

		if (args[0].equals("true")) {
			config.setEncryptVpninfo(true);
			context.println("encryption set");
		} else if(args[0].equals("false")){
			config.setEncryptVpninfo(false);
			context.println("encryption unset");
		} else {
			context.println("enter \'true\' or \'false\'");
			return;
		}

		configApi.setGlobalConfig(config);
	}
	
	@ScriptUsage(description = "sql connection pool config", arguments = { 
			@ScriptArgument(name = "partition count", type = "int", description = "Default: 1, minimum: 1, recommended: 3-4"),
			@ScriptArgument(name = "min connections per partition", type = "int", description = "The number of connections to start off with per partition."),
			@ScriptArgument(name = "max connections per partition", type = "int", description = "The number of connections to create per partition."),
			@ScriptArgument(name = "connection timeout(seconds)", type = "int", description = "connection timeout(seconds)"),
			@ScriptArgument(name = "idle connection test period(minutes)", type = "int", description = "idle connection test period(minutes)"),
			@ScriptArgument(name = "connection test statement", type = "String", description = "connection test statement")})
	public void setSqlConnPoolConfig(String[] args) {
		for(int i = 0; i < 5; i++){
			try{				
				Integer.parseInt(args[i]);
			} catch(NumberFormatException ex){
				context.println("input int value");
				return;
			}
		}
		String setting = args[0] + "," + args[1] + "," + args[2] + "," + args[3] + "," + args[4] + "," + args[5];
		
		GlobalConfig config = configApi.getGlobalConfig();
		config.setSqlConnPoolConfig(setting);
		configApi.setGlobalConfig(config);
		
		context.println("sql connection pool configured.");
	
		//apply changed setting to connection pool.
		sqlAuthService.resetConnectionPool();
	}
	
	public void sqlConnPoolConfig(String[] args) {
		GlobalConfig config = configApi.getGlobalConfig();
		String setting = config.getSqlConnPoolConfig();
		String[] str = setting.split(",");
		
		context.print("\n");
		context.println("\t partition count : " + str[0]);
		context.println("\t min connections per partition : " + str[1]);
		context.println("\t max connections per partition : " + str[2]);
		context.println("\t connection timeout(seconds) : " + str[3]);
		context.println("\t idle connection test period(minutes) : " + str[4]);
		context.println("\t connection test statement : " + str[5]);
		context.print("\n");
	}
	
	@ScriptUsage(description = "enable sql connection pool", arguments = { @ScriptArgument(name = "enable", type = "boolean", description = "true or false", optional = true) })
	public void enableSqlAuthConnPool(String[] args) {
		if(args.length > 1){
			context.println("enter \'true\' or \'false\'");
			return;
		}
		
		GlobalConfig config = configApi.getGlobalConfig();
		
		if(args.length == 0){			
			context.println("enable connection pool: " + config.isConnPoolEnabled());
			return;
		}		

		Boolean enableConnPool = null;
		if (args[0].equals("true")) {			
			config.enableConnPool(true);
			enableConnPool = true;
			context.println("enabled");
		} else if(args[0].equals("false")){
			config.enableConnPool(false);
			enableConnPool = false;
			context.println("disabled");
		} else {
			context.println("enter \'true\' or \'false\'");
			return;
		}

		configApi.setGlobalConfig(config);
		sqlAuthService.enableConnPool(enableConnPool);
	}

    @ScriptUsage(description = "start confdb user info migration to MySQL database", arguments = { })
    public void startUserInfoMigration(String[] args) {
        context.print("\n");
        context.println("start migration....");
        Map<String, Integer> result = userMigrationServiceApi.startMigration();
        context.println("migration result: user count: " + result.get("userCnt") + ", organization unit count: " + result.get("orgUnitCnt"));
        context.println("end migration....");

    }
    
    @ScriptUsage(description = "set default dns resolver provider", arguments = { })
    public void setDefaultDnsProvider(String[] args) {
    	GlobalConfig config = configApi.getGlobalConfig();
    	if (args.length == 1) {
    		try{
    			dnsService.setDefaultResolverProvider(args[0]);
    			config.setDefaultDnsResolverProvider(args[0]);
    			configApi.setGlobalConfig(config);
    			context.println(args[0] + " default dns resolver provider set complete.");
    		}catch (IllegalStateException e){
    			dnsService.setDefaultResolverProvider("frodo-proxy");
    			context.println(args[0] + " provider does not exist.");
    		}
    	} else {
    		
    		context.println("current default set is " + (config.getDefaultDnsResolverProvider() == null ? "frodo-proxy" : config.getDefaultDnsResolverProvider()));
    		context.println("==== dns resolver provider list. ====");
    		for (DnsResolverProvider provider : dnsService.getResolverProviders()) {
				context.println(provider.getName());
			} 
    	}
    }
    
    @ScriptUsage(description = "set enable dns resolver", arguments = { })
    public void enableDnsResolver(String[] args) {
    	GlobalConfig config = configApi.getGlobalConfig();
    	if (args.length == 1) {
			if ("true".equals(args[0])){
				if (!dnsService.getStatus().isRunning()){
					try {
						dnsService.open();
					} catch (IOException e) {
					}
				}
				config.setEnableDnsResolver(true);
				configApi.setGlobalConfig(config);
			}else if ("false".equals(args[0])){
				if (dnsService.getStatus().isRunning()){
					dnsService.close();
				}
				config.setEnableDnsResolver(false);
				configApi.setGlobalConfig(config);
			}else{
				context.println("enter \'true\' or \'false\'");
				return;
			}
			
			context.println(" enable dns resolver set " + args[0] + " complete.");
    	} else {
    		context.println("enter \'true\' or \'false\'");
    		context.println("current set is " + config.getEnableDnsResolver());
    	}
    }
}
