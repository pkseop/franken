package kr.co.future.sslvpn.userui.script;

import kr.co.future.sslvpn.userui.ClientDownloadConfig;
import kr.co.future.sslvpn.userui.ClientDownloadConfigApi;
import kr.co.future.api.Script;
import kr.co.future.api.ScriptArgument;
import kr.co.future.api.ScriptContext;
import kr.co.future.api.ScriptUsage;

public class ClientDownloadScript implements Script{

	private ScriptContext context;
	private ClientDownloadConfigApi clientDownloadConfigApi;
	
	public ClientDownloadScript(ClientDownloadConfigApi clientDownloadConfigApi) {
		this.clientDownloadConfigApi = clientDownloadConfigApi;
	}
	
	@Override
	public void setScriptContext(ScriptContext context) {
		this.context = context;
	}
	
	@ScriptUsage(description = "enable client download", arguments = @ScriptArgument(name = "enable client download", type = "boolean", optional = true))
	public void enable(String[] args) {
		if(args.length == 1) {
			if(args[0].equals("true") || args[0].equals("false")){
				ClientDownloadConfig config = clientDownloadConfigApi.getClientDownloadConfig();
				config.setEnable(Boolean.valueOf(args[0]));
				clientDownloadConfigApi.setClientDownloadConfig(config);
				return;
			} else {
				context.print("enter true or false");
				return;
			}
		} else if(args.length == 0) {
			context.println("enable: " + clientDownloadConfigApi.isEnabled());
		} else {
			context.println("enter true or false");
			return;
		}
		
	}
	
	@ScriptUsage(description = "set client download redirect url", arguments = @ScriptArgument(name = "client download redirect url", type = "String", optional = true))
	public void setRedirectURL(String[] args) {
		if(args.length == 1) {
		 	String redirectURL = args[0];
		 	if (redirectURL == "")
		 		context.println("client download redirect url set \"\". client download redirect not used.");
		 	ClientDownloadConfig config = clientDownloadConfigApi.getClientDownloadConfig();
		 	config.setRedirectURL(redirectURL);
		 	clientDownloadConfigApi.setClientDownloadConfig(config);
		} else if(args.length == 0) {
			String redirectURL = clientDownloadConfigApi.getRedirectURL();
			context.println("client download redirect url : " + redirectURL);
			if (redirectURL == null || redirectURL == "")
				context.println("enter redirect url. (ex. 192.168.0.1)");
		} else {
			context.println("enter redirect url. (ex. 192.168.0.1)");
			return;
		}
		
	}
	
	@ScriptUsage(description = "enable local client download", arguments = @ScriptArgument(name = "enable local client download", type = "boolean", optional = true))
	public void localDownload(String[] args) {
		if(args.length == 1) {
			if(args[0].equals("true") || args[0].equals("false")){
				ClientDownloadConfig config = clientDownloadConfigApi.getClientDownloadConfig();
				config.setEnableLocalDownload(Boolean.valueOf(args[0]));
				clientDownloadConfigApi.setClientDownloadConfig(config);
				return;
			} else {
				context.print("enter true or false");
				return;
			}
		} else if(args.length == 0) {
			context.println("enable: " + clientDownloadConfigApi.isEnableLocalDownload());
		} else {
			context.println("enter true or false");
			return;
		}
	}

    @ScriptUsage(description = "enable specific client download", arguments = @ScriptArgument(name = "enable specific client download", type = "boolean", optional = true))
    public void enableSpecClientDownload(String[] args) {
        if(args.length == 1) {
            if(args[0].equals("true") || args[0].equals("false")){
                ClientDownloadConfig config = clientDownloadConfigApi.getClientDownloadConfig();
                config.setEnableSpecificDownload(Boolean.valueOf(args[0]));
                clientDownloadConfigApi.setClientDownloadConfig(config);
                return;
            } else {
                context.print("enter true or false");
                return;
            }
        } else if(args.length == 0) {
            context.println("enable: " + clientDownloadConfigApi.isEnableSpecificDownload());
        } else {
            context.println("enter true or false");
            return;
        }
    }

    @ScriptUsage(description = "set client download redirect url", arguments = @ScriptArgument(name = "client download redirect url", type = "String", optional = true))
	public void useHttpRedirectForSetupFile(String[] args) {
    	if(args.length == 0) {
    		ClientDownloadConfig config = clientDownloadConfigApi.getClientDownloadConfig();
    		context.println("use http redirect for setup file: " + config.useHttpRedirectForSetupFile());
    	} else if (args.length == 1) {
    		if(!args[0].equals("true") && !args[0].equals("false")) {
    			context.println("input only true or false.");
    			return;
    		}
    		
    		ClientDownloadConfig config = clientDownloadConfigApi.getClientDownloadConfig();
    		if(args[0].equals("true"))
    			config.setUseHttpRedirectForSetupFile(true);
    		else
    			config.setUseHttpRedirectForSetupFile(false);
    		context.println("set as " + args[0]);
    	} else {
    		context.println("input only true or false.");
    	}
    }
}
