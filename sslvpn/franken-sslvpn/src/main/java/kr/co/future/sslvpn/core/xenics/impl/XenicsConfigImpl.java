package kr.co.future.sslvpn.core.xenics.impl;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import kr.co.future.sslvpn.model.IOSVpnServerConfig;
import kr.co.future.sslvpn.model.VpnServerConfig;
import kr.co.future.sslvpn.model.api.VpnServerConfigApi;
import kr.co.future.sslvpn.core.xenics.XenicsConfig;
import kr.co.future.sslvpn.core.xenics.XenicsService;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.apache.felix.ipojo.annotations.Validate;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.JsonProcessingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.SerializationConfig;
import org.codehaus.jackson.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Charsets;
import com.google.common.base.Strings;
import com.google.common.io.Files;

@Component(name = "xenics-config")
@Provides
public class XenicsConfigImpl implements XenicsConfig {
	
	private final Logger logger = LoggerFactory.getLogger(XenicsConfigImpl.class);
	
	@Requires
	private XenicsService xenicsService;
	
	@Requires
	private VpnServerConfigApi vpnServerConfigApi;
	
	//xenics.conf 파일의 설정 값
	private static boolean REMOTE_DB_USING = false;
	
	private static boolean XENICS_IOS_USE = false;
	
	//remote_sql.conf 파일 설정 값들
	private static String HOSTNAME;
	
	private static String LOGIN;
	
	private static String PASSWORD;
	
	private static String DB;
	
	private static String TABLE;
	
	private static String PORT;
	
	private static String SOCKET;
	
	@Validate
	public void start() {
		readRemoteDbUsing();
		readRemoteSqlConf();
		dbRestart();
	}
	
	@Override
	public boolean getRemoteDbUsing() {
		return REMOTE_DB_USING;
	}
	
	public void readRemoteDbUsing() {
		File file = new File(XENICS_CONF);
		if(!file.exists()){
			logger.error("can't find file [{}]", XENICS_CONF);
			return;
		}
		
		ObjectMapper om = new ObjectMapper();
		try {
	      JsonNode rootNode = om.readTree(file);
	      JsonNode node = rootNode.path("xenics_path");
	      node = node.path("remote_db_using");
	      
	      REMOTE_DB_USING = Boolean.parseBoolean(node.getTextValue());
	      
	      logger.info("read completed. remote_db_using [{}]", REMOTE_DB_USING);
      } catch (JsonProcessingException e) {
	      logger.error("error occurred while read xenics.conf file", e);
      } catch (IOException e) {
      	logger.error("error occurred while read xenics.conf file", e);
      }
	}
	
	@Override
	public void writeRemoteDbUsing(String flag) {
		File file = new File(XENICS_CONF);
		if(!file.exists()){
			logger.error("can't find file [{}]", XENICS_CONF);
			return;
		}
		
		ObjectMapper om = new ObjectMapper();
		om.enable(SerializationConfig.Feature.INDENT_OUTPUT);
		try {
	      JsonNode rootNode = om.readTree(file);
	      JsonNode xenics_pathNode = rootNode.path("xenics_path");

	      String value = String.valueOf(flag);
	      ((ObjectNode)xenics_pathNode).put("remote_db_using", value);

	      om.writeValue(file, rootNode);
	      
	      REMOTE_DB_USING = Boolean.parseBoolean(flag);
	      
	      dbRestart();
	      
	      logger.info("write completed. remote_db_using [{}]", REMOTE_DB_USING);
      } catch (JsonProcessingException e) {
	      logger.error("error occurred while read xenics.conf file", e);
      } catch (IOException e) {
      	logger.error("error occurred while read xenics.conf file", e);
      }
	}
	
	@Override
	public void writeIOSUsing(String flag) {
		File file = new File(XENICS_CONF);
		if(!file.exists()){
			logger.error("can't find file [{}]", XENICS_CONF);
			return;
		}
		
		ObjectMapper om = new ObjectMapper();
		om.enable(SerializationConfig.Feature.INDENT_OUTPUT);
		try {
	      JsonNode rootNode = om.readTree(file);
	      JsonNode xenics_pathNode = rootNode.path("xenics_path");

	      String value = String.valueOf(flag);
	      ((ObjectNode)xenics_pathNode).put("xenics_ios_use", value);

	      om.writeValue(file, rootNode);
	      
	      XENICS_IOS_USE = Boolean.parseBoolean(flag);
	      
	      logger.info("write completed. xenics_ios_use [{}]", XENICS_IOS_USE);
      } catch (JsonProcessingException e) {
	      logger.error("error occurred while read xenics.conf file", e);
      } catch (IOException e) {
      	logger.error("error occurred while read xenics.conf file", e);
      }
	}
	
	@Override
	public String getRemoteSqlConf() {
		StringBuilder sb = new StringBuilder();
		sb.append("hostname").append(" ").append(HOSTNAME).append("\n");
		sb.append("login").append(" ").append(LOGIN).append("\n");
		sb.append("password").append(" ").append(PASSWORD).append("\n");
		sb.append("db").append(" ").append(DB).append("\n");
		sb.append("table").append(" ").append(TABLE).append("\n");
		sb.append("port").append(" ").append(PORT).append("\n");
		sb.append("socket").append(" ").append(SOCKET).append("\n");
		return sb.toString();
	}
	
	public void readRemoteSqlConf() {
		File file = new File(REMOTE_SQL_CONF);
		if(!file.exists()){
			logger.error("can't find file [{}]", REMOTE_SQL_CONF);
			return;
		}
		
		BufferedReader bf = null;
      try {
	      bf = new BufferedReader(new FileReader(file));
	      String line;
	      while ((line = bf.readLine()) != null) {
		      String[] arr = line.split(" ");
		      if(arr[0].equals("hostname"))
		      	HOSTNAME = arr[1];
		      else if(arr[0].equals("login"))
		      	LOGIN = arr[1];
		      else if(arr[0].equals("password"))
		      	PASSWORD = arr[1];
		      else if(arr[0].equals("db"))
		      	DB = arr[1];
		      else if(arr[0].equals("table"))
		      	TABLE = arr[1];
		      else if(arr[0].equals("port"))
		      	PORT = arr[1];
		      else if(arr[0].equals("socket"))
		      	SOCKET = arr[1];
		   }
	      
	      logger.info("read remote_sql.conf done!");
      } catch (FileNotFoundException e) {
      	logger.error("remote_sql.conf file not found", e);
      } catch (IOException e) {
      	logger.error("remote_sql.conf read error", e);
      } finally {
         try {
         	if(bf != null)
         		bf.close();
         } catch (IOException e) {
         	logger.error("close error", e);
         }
      }
	}
	
	@Override
	public void writeRemoteSqlConf(String hostname, String login, String password, 
			String db, String table, String port, String socket) {
		HOSTNAME = hostname;
		LOGIN = login;
		PASSWORD = password;
		DB = db;
		TABLE = table;
		PORT = port;
		SOCKET = socket;
		
		File file = new File(REMOTE_SQL_CONF);
		
		// true 지정시 파일의 기존 내용에 이어서 작성
      FileWriter fw = null;
       
      // 파일안에 문자열 쓰기
      try {
      	fw = new FileWriter(file, false) ;
	      fw.write("hostname " + HOSTNAME + "\n");
	      fw.write("login " + LOGIN + "\n");
	      fw.write("password " + PASSWORD + "\n");
	      fw.write("db " + DB + "\n");
	      fw.write("table " + TABLE + "\n");
	      fw.write("port " + PORT + "\n");
	      fw.write("socket " + SOCKET);
	      fw.flush();
      } catch (IOException e) {
	      logger.error("remote_sql.conf write error", e);
	      return;
      } finally {
         try {
         	if(fw != null)
         		fw.close();
         } catch (IOException e) {
         	logger.error("close error", e);
         } 
      }
      
      logger.info("remote_sql.conf changed!!");
      
      dbRestart();
	}
	
	private void dbRestart() {
		xenicsService.remoteDbStop();
      if(REMOTE_DB_USING) {
      	remoteDbStart();
      }
	}
	
	private void remoteDbStart() {
		String url = getRemoteDbUrl();
      xenicsService.remoteDbStart(url, LOGIN, PASSWORD, TABLE);
	}
	
	private String getRemoteDbUrl() {
		StringBuilder sb = new StringBuilder();
		sb.append("jdbc:mysql://").append(HOSTNAME).append(":").append(PORT).append("/").append(DB);
		
		return sb.toString(); 
	}
	
	@Override
	public void applyVpnServerConf() throws IOException, InterruptedException {
		VpnServerConfig config = vpnServerConfigApi.getCurrentVpnServerConfig();
		applyServerConf(config);
		applyRemoteDbConf(config);
		applyProxyPort(config);
		restartXenics();
	}
	
	@Override
	public void applyIOSVpnServerConf() throws IOException, InterruptedException {
		IOSVpnServerConfig config = vpnServerConfigApi.getCurrentIOSVpnServerConfig();
		applyIOSServerConf(config);
		writeIOSUsing(config.getUseIOS().toString());
		restartIOSXenics();
	}
	
	private void restartXenics() throws IOException, InterruptedException {
		String command = "xenics restart";
		Process p = Runtime.getRuntime().exec(command);
		p.waitFor();
	}
	
	private void restartIOSXenics() throws IOException, InterruptedException {
		// IOS 용 제닉스만 재시작 하려면?
		String command = "xenics restart";
		Process p = Runtime.getRuntime().exec(command);
		p.waitFor();
	}
	
	private void applyProxyPort(VpnServerConfig config) throws IOException {
		File xenicsProxySh = new File(XENICS_PROXY_SH);
		List<String> lines = Files.readLines(xenicsProxySh, Charsets.UTF_8);
		List<String> newLines = new ArrayList<String>();
		
		for(String line : lines) {
			if(line.contains("server 0.0.0.0")) {
				int index = line.indexOf("server 0.0.0.0");
				String subStr = line.substring(0, index);
				newLines.add(subStr + "server 0.0.0.0:" + config.getProxyPort() + " &");
			} else {
				newLines.add(line);
			}
		}
		
		StringBuilder sb = new StringBuilder();
		for(String line : newLines) {
			sb.append(line).append("\n");
		}
		Files.write(sb.toString().getBytes(Charsets.UTF_8), xenicsProxySh);
	}
	
	private void applyRemoteDbConf(VpnServerConfig config) {
		if(config.getUseRemoteDb()) {
			writeRemoteSqlConf(config.getRemoteDbHostName(), config.getRemoteDbLoginName(), config.getRemoteDbPassword(),
					config.getRemoteDbName(), config.getRemoteDbTableName(), config.getRemoteDbPort(), config.getRemoteDbSocket());
		}
		writeRemoteDbUsing(String.valueOf(config.getUseRemoteDb()));
	}
	
	private void applyServerConf(VpnServerConfig config) throws IOException {
		File serverConfFile = new File(SERVER_CONF);
		List<String> lines = Files.readLines(serverConfFile, Charsets.UTF_8);
		List<String> newLines = new ArrayList<String>();
		
		//to avoid duplicated config line and to do not omit config.
		boolean ipFlag = true, portFlag = true, obfsFlag = true, obfsPadlenFlag = true, cipherFlag = true, dnsFlag = true,
				remoteKillIpFlag = true, enableDuplicateCheckFlag = true; 
		
		for(String line : lines) {
			if(line.startsWith("server") || line.startsWith(";server")) {
				genVpnIpConfigLine(config, ipFlag, newLines);
				ipFlag = false;
			} else if(line.startsWith("port") || line.startsWith(";port")) {
				genPortConfigLine(config, portFlag, newLines);
				portFlag = false;
			} else if(line.startsWith("obfs-salt") || line.startsWith(";obfs-salt")) {
				genObfsConfigLine(config, obfsFlag, newLines);
				obfsFlag = false;
			} else if(line.startsWith("obfs-padlen") || line.startsWith(";obfs-padlen")) {
				genObfsPadlen(config, obfsPadlenFlag, newLines);
				obfsPadlenFlag = false;
			} else if(line.startsWith("cipher") || line.startsWith(";cipher")) {
				genCipherConfigLine(config, cipherFlag, newLines);
				cipherFlag = false;
			} else if(line.startsWith("push \"dhcp-option DNS") || line.startsWith(";push \"dhcp-option DNS")) {
				genDnsConfigLine(config, dnsFlag, newLines);
				dnsFlag = false;
			} else if(line.startsWith("remote-kill-ip") || line.startsWith(";remote-kill-ip")) {
				genRemoteKillConfigLine(config, remoteKillIpFlag, newLines);
				remoteKillIpFlag = false;
			} else if(line.startsWith(";enable-duplicate-check")) {
				newLines.add("enable-duplicate-check");
			} else {
				newLines.add(line);
			}
		}
		
		//if the config line doesn't exist in server.conf then add the line.
		genVpnIpConfigLine(config, ipFlag, newLines);
		genPortConfigLine(config, portFlag, newLines);
		genObfsConfigLine(config, obfsFlag, newLines);
		genObfsPadlen(config, obfsPadlenFlag, newLines);
		genCipherConfigLine(config, cipherFlag, newLines);
		genDnsConfigLine(config, dnsFlag, newLines);
		genRemoteKillConfigLine(config, remoteKillIpFlag, newLines);
		
		StringBuilder sb = new StringBuilder();
		for(String line : newLines) {
			sb.append(line).append("\n");
		}
		Files.write(sb.toString().getBytes(Charsets.UTF_8), serverConfFile);
	}
	
	private void applyIOSServerConf(IOSVpnServerConfig config) throws IOException {
		File serverConfFile = new File(IOS_SERVER_CONF);
		List<String> lines = Files.readLines(serverConfFile, Charsets.UTF_8);
		List<String> newLines = new ArrayList<String>();
		
		//to avoid duplicated config line and to do not omit config.
		boolean ipFlag = true, portFlag = true, cipherFlag = true, dnsFlag = true;
		
		for(String line : lines) {
			if(line.startsWith("server") || line.startsWith(";server")) {
				genIOSVpnIpConfigLine(config, ipFlag, newLines);
				ipFlag = false;
			} else if(line.startsWith("port") || line.startsWith(";port")) {
				genIOSPortConfigLine(config, portFlag, newLines);
				portFlag = false;
			} else if(line.startsWith("cipher") || line.startsWith(";cipher")) {
				genIOSCipherConfigLine(config, cipherFlag, newLines);
				cipherFlag = false;
			} else if(line.startsWith("push \"dhcp-option DNS") || line.startsWith(";push \"dhcp-option DNS")) {
				genIOSDnsConfigLine(config, dnsFlag, newLines);
				dnsFlag = false;
			} else if(line.startsWith("dev tun")) {
				newLines.add("dev tun1");
			} else {
				newLines.add(line);
			}
		}
		
		//if the config line doesn't exist in server.conf then add the line.
		genIOSVpnIpConfigLine(config, ipFlag, newLines);
		genIOSPortConfigLine(config, portFlag, newLines);
		genIOSCipherConfigLine(config, cipherFlag, newLines);
		genIOSDnsConfigLine(config, dnsFlag, newLines);
		
		StringBuilder sb = new StringBuilder();
		for(String line : newLines) {
			sb.append(line).append("\n");
		}
		Files.write(sb.toString().getBytes(Charsets.UTF_8), serverConfFile);
	}
	
	private void genIOSVpnIpConfigLine(IOSVpnServerConfig config, boolean flag, List<String> newLines) {
		if(flag) {
			String line = "server " + config.getVpnIp() + " " + config.getVpnNetmask();
			newLines.add(line);
		}
	}
	
	private void genIOSPortConfigLine(IOSVpnServerConfig config, boolean flag, List<String> newLines) {
		if(flag) {
			String line = "port " + config.getSslPort();
			newLines.add(line);
		}
	}
	
	private void genIOSCipherConfigLine(IOSVpnServerConfig config, boolean flag, List<String> newLines) {
		if(flag) {
			String line = config.getEncryptions().get(0).toUpperCase();
			String cipherConfig = getCipherConfig(line);
			line = "cipher " + cipherConfig;
			newLines.add(line);
		}
	}
	
	private void genIOSDnsConfigLine(IOSVpnServerConfig config, boolean flag, List<String> newLines) {
		if(flag) {
			String dns = config.getDnsAddr1();
			if(Strings.isNullOrEmpty(dns)) {
				newLines.add(";push \"dhcp-option DNS 10.1.1.1\"");
			} else {
				newLines.add("push \"dhcp-option DNS " + dns + "\"");
			}
			dns = config.getDnsAddr2();
			if(Strings.isNullOrEmpty(dns)) {
				newLines.add(";push \"dhcp-option DNS 10.1.1.2\"");
			} else {
				newLines.add("push \"dhcp-option DNS " + dns + "\"");
			}
		}
	}
	
	private void genVpnIpConfigLine(VpnServerConfig config, boolean flag, List<String> newLines) {
		if(flag) {
			String line = "server " + config.getVpnIp() + " " + config.getVpnNetmask();
			newLines.add(line);
		}
	}
	
	private void genPortConfigLine(VpnServerConfig config, boolean flag, List<String> newLines) {
		if(flag) {
			String line = "port " + config.getSslPort();
			newLines.add(line);
		}
	}
	
	private void genObfsConfigLine(VpnServerConfig config, boolean flag, List<String> newLines) {
		if(flag) {
			if(config.isUseObfuscationKey()) {
				newLines.add("obfs-salt " + config.getObfuscationKey());
			} else {
				newLines.add(";obfs-salt cipher_china");
			}
		}
	}
	
	private void genObfsPadlen(VpnServerConfig config, boolean flag, List<String> newLines) {
		if(flag) {
			if(config.isUseObfuscationKey()) {
				newLines.add("obfs-padlen 10");
			} else {
				newLines.add(";obfs-padlen 10");
			}
		}
		
	}
	
	private void genCipherConfigLine(VpnServerConfig config, boolean flag, List<String> newLines) {
		if(flag) {
			String line = config.getEncryptions().get(0).toUpperCase();
			String cipherConfig = getCipherConfig(line);
			line = "cipher " + cipherConfig;
			newLines.add(line);
		}
	}
	
	private void genDnsConfigLine(VpnServerConfig config, boolean flag, List<String> newLines) {
		if(flag) {
			String dns = config.getDnsAddr1();
			if(Strings.isNullOrEmpty(dns)) {
				newLines.add(";push \"dhcp-option DNS 10.1.1.1\"");
			} else {
				newLines.add("push \"dhcp-option DNS " + dns + "\"");
			}
			dns = config.getDnsAddr2();
			if(Strings.isNullOrEmpty(dns)) {
				newLines.add(";push \"dhcp-option DNS 10.1.1.2\"");
			} else {
				newLines.add("push \"dhcp-option DNS " + dns + "\"");
			}
		}
	}
	
	private void genRemoteKillConfigLine(VpnServerConfig config, boolean flag, List<String> newLines) {
		if(flag) {
			String line = null;
			if(config.getUseRemoteDb()) {
				line = "remote-kill-ip " + config.getRemoteKillIp();
			} else {
				line = ";remote-kill-ip 10.1.1.1";
			}
			newLines.add(line);
		}
	}
	
	@Override
	public String getCipherConfig(String cipher) {
		StringBuilder sb = new StringBuilder();

		if(cipher.startsWith("SEED")) {
			sb.append("SEED-CBC");
		} else if(cipher.startsWith("ARIA") || cipher.startsWith("AES")) {
			if(cipher.startsWith("ARIA"))
				sb.append("ARIA");
			else
				sb.append("AES");
			
			if(cipher.endsWith("128")) {
				sb.append("-128");
			} else if(cipher.endsWith("192")) {
				sb.append("-192");
			} else if(cipher.endsWith("256")) {
				sb.append("-256");
			}
			sb.append("-CBC");
		}
		return sb.toString();
	}
	
	@Override
	public boolean isTunInteferfaceOk(String tun, boolean isIOS) {
		String cmd = "ifconfig " + tun;
		Process p = null;
        BufferedReader br = null;
        VpnServerConfig config = vpnServerConfigApi.getCurrentVpnServerConfig();
        IOSVpnServerConfig iosConfig = vpnServerConfigApi.getCurrentIOSVpnServerConfig();
        
        String ip = "";
        String vpnNetMask = "";
        
        if (isIOS){
        	String[] arr = iosConfig.getVpnIp().split("\\.");
        	ip = arr[0] + "." + arr[1];
            vpnNetMask = config.getVpnNetmask();
        }else {
        	String[] arr = config.getVpnIp().split("\\.");
        	ip = arr[0] + "." + arr[1];
            vpnNetMask = config.getVpnNetmask();
        }
        
        boolean ipOk = false, maskOk = false;
        
        for(int i = 0; i < 5; i++) {
	        try {
	            p = Runtime.getRuntime().exec(cmd);
	            br = new BufferedReader(new InputStreamReader(p.getInputStream()));
	            try {
	            	p.waitFor();
	            } catch (InterruptedException e) {
	            }
	
	            while(true) {
		            String s = br.readLine();
		            if (s == null)
		            	break;
		            
		            if(s.contains(ip))
		            	ipOk = true;
		            if(s.contains(vpnNetMask))
		            	maskOk = true;
		            
		            if(ipOk && maskOk)
		            	break;
	            }
	            if(ipOk && maskOk)
	            	break;
	        } catch (IOException e) {
	        } finally {
	            try {
	                if (br != null)
	                        br.close();
	            } catch (IOException e) {
	            }
	        }
	        
	        try {
	        	if(i != 4)
	        		Thread.sleep(1000);
			} catch (InterruptedException e) {
			}
        }
        if(ipOk && maskOk)
        	return true;
        else
        	return false;
	}
}
