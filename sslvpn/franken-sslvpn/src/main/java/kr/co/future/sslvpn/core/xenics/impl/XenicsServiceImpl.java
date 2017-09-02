package kr.co.future.sslvpn.core.xenics.impl;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import kr.co.future.sslvpn.model.AccessGateway;
import kr.co.future.sslvpn.model.AccessProfile;
import kr.co.future.sslvpn.model.InternalNetworkRange;
import kr.co.future.sslvpn.model.Server;
import kr.co.future.sslvpn.model.SplitRoutingEntry;
import kr.co.future.sslvpn.model.api.AccessGatewayApi;
import kr.co.future.sslvpn.model.api.AccessProfileApi;
import kr.co.future.sslvpn.core.backup.FtpBackup;
import kr.co.future.sslvpn.core.xenics.XenicsService;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Invalidate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.apache.felix.ipojo.annotations.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(name = "xenics-service")
@Provides
public class XenicsServiceImpl implements XenicsService{
	private final Logger logger = LoggerFactory.getLogger(XenicsServiceImpl.class);
	
	private XenicsDB db;
	
	private RemoteDB remoteDb;
	
	@Requires
	private AccessProfileApi profileApi;
	
	@Requires
	private kr.co.future.dom.api.UserApi domUserApi;
	
	@Requires
	private AccessGatewayApi gatewayApi;
	
	@Override
	public int getTotalNumOfTunnels() {		
		return db.getTotalNumberOfConnectedInfo();		
	}
	
	@Override
	public int getTotalNumOfTunnels(String filterField, String filterValue) {
		String filterQuery = genFilterQuery(filterField, filterValue);
		return db.getTotalNumberOfConnectedInfoWithFilter(filterQuery);
	}
	
	@Validate
	public void start() {
		db = new XenicsDB();
	}
	
	@Invalidate
	public void stop() {
		db.shutdown();
	}
	
	private String genFilterQuery(String filterField, String filterValue) {
		String filterQuery = "";
		if(filterField.equals("name")) {
			List<String> loginNames = domUserApi.searchLoginNameByName(filterValue, 0, 0);
			StringBuilder sb = new StringBuilder("");
			boolean isFirst = true;
			for(String loginName : loginNames) {
				if(isFirst) {
					isFirst = false;
				} else
					sb.append(",");
				sb.append("'").append(loginName).append("'");
			}
			
			filterQuery += "user_id in (" + sb.toString() + ")";
		} else {
			filterQuery += filterField + " like " + "'%" + filterValue + "%'";
		}		
		return filterQuery;
	}
	
	@Override
	public Users getTunnelInfo(int tunnelId) {
		return db.getTunnelInfo(tunnelId);
	}
	
	@Override
	public Users getConnectedTunnelInfo(String remoteAddr) {
		return db.getConnectedTunnelInfo(remoteAddr);
	}
	
	@Override
	public List<Object> getTunnels(int limit, int offset, String filterField, String filterValue) {
		if(limit <= 0 || offset < 0)
			return null;
		
		String sqlCond = null;
		if(filterField != null && filterValue != null) {
			sqlCond = " where using_flag = 1 and (" + genFilterQuery(filterField, filterValue) + ") limit " + limit + " offset " + offset;
		} else {
			sqlCond = " where using_flag = 1 limit " + limit + " offset " + offset;
		}
		
		List<Users> list = db.fetchConInfo(sqlCond);
		List<Object> tunnels = new ArrayList<Object>();
		for(Users conInfo : list) {
			tunnels.add(serialize(conInfo));
		}
		return tunnels;
	}
	
	@Override
	public List<Users> getConnectedTunnelInfo() {
		String sqlCond = " where using_flag = 1";
		List<Users> list = db.fetchConInfo(sqlCond);
		return list;
	}
	
	private Map<String, Object> serialize(Users users){
		AccessProfile profile = profileApi.determineProfile(users.user_id);
		Map<String, Object> m = users.mappingToTunnelInfo(profile);
		m.put("name", domUserApi.getUserNameByLoginName(users.user_id));
		return m;
	}

	@Override
   public boolean isAlreadyLoggedin(String loginName) {
	   int result = db.getUsersUsingFlag(loginName);
	   return result == 1 ? true : false;
   }
	
	@Override
	public int getClientId(int tunnelId) {
		int result = db.getClientId(tunnelId);
		return result;
	}
	
	@Override
	public int getClientId(String loginName) {
		int result = db.getClientId(loginName);
		return result;
	}
	
	@Override
   public String killTunnel(int tunnelId) {
		int clientId = db.getClientId(tunnelId);
		return execkillTunnel(clientId);
   }
	
	@Override
	public String killDuploginTunnel(String loginName) {
		int clientId = getClientId(loginName);
		String message = null;
		Runtime r = Runtime.getRuntime();
		Process p = null;
      
		try{
			if(clientId != -1) {
				String shCmd = " /usr/sslplus/xenics/utils/duplicate_c_kill.sh " + String.valueOf(clientId);
				p = r.exec(shCmd);
				BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
				
				while (true) {
					String msg = br.readLine();
					if (msg == null)
						break;
					message = msg;
				}
				
				br.close();
				
				if(message != null)
					logger.info("kill duplicate login tunnel executed. message =>[{}]", message);
				return message;
			}
		} catch(Exception e) {
			logger.error("error occurred during kill tunnel", e);			
		} finally {
			if (p != null) {
				closeInputStream(p.getErrorStream());
				closeInputStream(p.getInputStream());
				closeOutputStream(p.getOutputStream());
			}

		}
		return message;
	}
	
	@Override
	public String execkillTunnel(int clientId) {
		String message = null;
		Runtime r = Runtime.getRuntime();
		Process p = null;
      
		try{
			if(clientId != -1) {
				String shCmd = "/usr/sslplus/xenics/utils/c_kill.sh " + String.valueOf(clientId);
				p = r.exec(shCmd);
				BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
				
				while (true) {
					String msg = br.readLine();
					if (msg == null)
						break;
					message = msg;
				}
				
				br.close();
				
				if(message != null)
					logger.info("kill tunnel executed. message =>[{}]", message);
				return message;
			}
		} catch(Exception e) {
			logger.error("error occurred during kill tunnel", e);			
		} finally {
         if (p != null) {
            closeInputStream(p.getErrorStream());
            closeInputStream(p.getInputStream());
            closeOutputStream(p.getOutputStream());
        }

    }
		return message;
	}
	
	private void closeInputStream(InputStream is) {
      try {
          is.close();
      } catch (IOException e) {
          e.printStackTrace();
      }
  }

  private void closeOutputStream(OutputStream os) {
      try {
          os.close();
      } catch (IOException e) {
          e.printStackTrace();
      }
  }
	
	@Override
   public void updateAccessProfileInfo(List<AccessProfile> profiles) {
		removeAllAccessProfileInfo();
		logger.trace("whole access profile informations in db initialized");
		
		for(AccessProfile profile : profiles) {
			int profileId = profile.getId();
			
			List<SplitRoutingEntry> splitRoutingEntries = null;
			if(profile.isUseSplitRouting() == null){		//inherite
				AccessGateway gw = gatewayApi.getCurrentAccessGateway();
				if(gw.isUseSplitRouting())
					splitRoutingEntries = gw.getSplitRoutingEntries();
			} else if(profile.isUseSplitRouting()) {		//use split routing
				splitRoutingEntries = profile.getSplitRoutingEntries();
			}
			
			List<InternalNetworkRange> internalNetworkRanges = profile.getInternalNetworkRanges();
			List<Server> normalAcl = profile.getNormalAcl();
			
			if(splitRoutingEntries != null && splitRoutingEntries.size() > 0) {
				db.insertSplitRoutingEntries(profileId, splitRoutingEntries);
			}
			
			if(internalNetworkRanges.size() > 0) {
				db.insertInternalNetworkRanges(profileId, internalNetworkRanges);
			}
			
			if(normalAcl.size() > 0) {
				db.insertAllowServers(profileId, normalAcl);
			}
			
			logger.trace("[{}] access profile informations are inserted to db", profile.getName());
		}
   }
	
	public void removeAllAccessProfileInfo() {
		db.removeAllAllowSvrRules();
		db.removeAllDenyIpRange();
		db.removeAllSplitRoute();
	}
	
	@Override
	public void createTables() {
		db.createFtpBackupTable();
	}
	
	@Override
	public void insertBackupInfo(int type, String fileName, String fileSize, int stateCode) {
		db.insertBackupInfo(type, fileName, fileSize, stateCode);
	}
	
	@Override
	public void updateBackupState(String fileName, int stateCode) {
	   db.updateBackupState(fileName, stateCode);
	}

	@Override
	public int getTotalNumOfFtpBackupRecords(int type) {
		return db.getTotalNumOfFtpBackup(type);
	}
	
	@Override
	public List<Object> getFtpBackupRecords(int type, int limit, int offset) {
		if(limit <= 0 || offset < 0)
			return null;
		
		List<FtpBackup> list = db.fetchFtpBackupRecords(type, limit, offset);
		List<Object> tunnels = new ArrayList<Object>();
		for(FtpBackup conInfo : list) {
			tunnels.add(conInfo.marshal());
		}
		return tunnels;
	}
	
	@Override
	public void	deleteBackupRecord(String fileName) {
		db.deleteBackupRecord(fileName);
	}
	
	@Override
	public List<String> fetchFtpBackupFileNames(int type, int stateCode) {
		return db.fetchFtpBackupFileNames(type, stateCode);
	}
	
	@Override
	public int retrieveFtpBackupStateCode(String fileName) {
		return db.retrieveFtpBackupStateCode(fileName);
	}
	
	@Override
	public void updatePrevPolicyRollbackState(int stateCode) {
		db.updatePrevPolicyRollbackState(stateCode);
	}
	
	@Override
	public void remoteDbStart(String url, String user, String pw, String table) {
		remoteDb = new RemoteDB(url, user, pw, table);
	}

	@Override
	public void remoteDbStop() {
		if(remoteDb != null) {
			remoteDb.shutdown();
			remoteDb = null;
		}
	}
	
	@Override
	public boolean isDupLoginByRemoteDb(String loginName) {
		return remoteDb.isDupLogin(loginName);
	}
}
