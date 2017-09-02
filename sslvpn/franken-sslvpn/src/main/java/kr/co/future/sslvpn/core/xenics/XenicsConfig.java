package kr.co.future.sslvpn.core.xenics;

import java.io.IOException;

public interface XenicsConfig {
	public static final String CONF_DIR = "/utm/log/sslplus/xenics/conf/";
	
	public static final String SERVER_CONF = CONF_DIR + "server.conf";
	
	public static final String IOS_SERVER_CONF = CONF_DIR + "server-ios.conf";
	
	public static final String XENICS_CONF = CONF_DIR +"xenics.conf";
	
	public static final String REMOTE_SQL_CONF = CONF_DIR + "remote_sql.conf";
	
	public static final String XENICS_PROXY_SH = CONF_DIR + "xenics_proxy.sh";
	
	public boolean getRemoteDbUsing();
	
	public void writeRemoteDbUsing(String flag);
	
	public void writeIOSUsing(String flag);
	
	public String getRemoteSqlConf();
	
	public void writeRemoteSqlConf(String hostname, String login, String password, 
			String db, String table, String port, String socket);
	
	public void applyVpnServerConf() throws IOException, InterruptedException;
	
	public void applyIOSVpnServerConf() throws IOException, InterruptedException;
	
	public boolean isTunInteferfaceOk(String tun, boolean isIOS);
	
	public String getCipherConfig(String cipher);
}