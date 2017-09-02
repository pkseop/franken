package kr.co.future.sslvpn.core.xenics.impl;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.concurrent.TimeUnit;

import kr.co.future.sslvpn.model.InternalNetworkRange;
import kr.co.future.sslvpn.model.IpEndpoint;
import kr.co.future.sslvpn.model.PortRange;
import kr.co.future.sslvpn.model.Server;
import kr.co.future.sslvpn.model.SplitRoutingEntry;
import kr.co.future.sslvpn.core.backup.FtpBackup;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jolbox.bonecp.BoneCP;
import com.jolbox.bonecp.BoneCPConfig;

public class XenicsDB {
	private static final String DB_DRIVER = "com.mysql.jdbc.Driver";
	private static final String DB_CONNECTION = "jdbc:mysql://127.0.0.1:3306/xenics";
	private static final String DB_USER = "root";
	private static final String DB_PASSWORD = "WeGuardia_01";
	
	private final Logger logger = LoggerFactory.getLogger(XenicsDB.class.getName());
	
	private BoneCP  connectionPool = null;
	
	public XenicsDB() {
		createConnectionPool(DB_CONNECTION, DB_USER, DB_PASSWORD);
	}
	
	public void shutdown() {
		if(connectionPool != null)
			connectionPool.shutdown();
	}
	
	private Connection getDBConnection() {
		Connection dbConnection = null;

		if (connectionPool != null) {
			try {
	            dbConnection = connectionPool.getConnection();
            } catch (SQLException e) {
         	    logger.error("XenicsDB: retrieve db connection from pool failed.", e);
            }
		} else {
			try {
				Class.forName(DB_DRIVER);
			} catch (ClassNotFoundException e) {
				logger.error("XenicsDB: connecting failed.", e);
				return null;
			}
	 
			try {
				dbConnection = (Connection) DriverManager.getConnection(DB_CONNECTION, DB_USER,DB_PASSWORD);
				return dbConnection;
			} catch (SQLException e) {
				logger.error("XenicsDB: connecting db failed.", e);
			}
		}
		return dbConnection;
	}
	
	private void createConnectionPool(String url, String id, String pw) {
		try {
			Class.forName(DB_DRIVER);
		} catch (Exception e) {
			logger.error("Can't create connetion pool", e);
			return;
		}

		if(connectionPool == null) {
			BoneCPConfig bonecpConfig = new BoneCPConfig();
			bonecpConfig.setJdbcUrl(url); 
			bonecpConfig.setUsername(id); 
			bonecpConfig.setPassword(pw);
        	bonecpConfig.setPartitionCount(1);
        	bonecpConfig.setMinConnectionsPerPartition(2);
        	bonecpConfig.setMaxConnectionsPerPartition(5);
        	bonecpConfig.setConnectionTimeout(30, TimeUnit.SECONDS);
        	bonecpConfig.setIdleConnectionTestPeriodInMinutes(5);
        	bonecpConfig.setConnectionTestStatement("SELECT 1");
        	bonecpConfig.setLazyInit(true);
        	
        	try {
        		connectionPool = new BoneCP(bonecpConfig); // setup the connection pool
        		logger.info("xenicx db: created connection pool. partition: 1, min: 2, max: 5, timeout(seconds): 30");
			} catch (SQLException e) {
				logger.error("frodo-sql-auth-service: create connection pool failed", e);
			}
		}
	}
	
	
	public List<Users> fetchConInfo(String sqlCond) {
		String sql = "select * from users" + sqlCond;
		
		Connection dbConnection = null;
		PreparedStatement stmt = null;
		ResultSet rs = null;
		List<Users> userList = new ArrayList<Users>();
		
		try {
			dbConnection = getDBConnection();
			
			stmt = dbConnection.prepareStatement(sql);
			
			rs = stmt.executeQuery();
			while (rs.next()) {
				Users users = new Users();
				setUsersInfo(users, rs);
				userList.add(users);
			}
		} catch (Exception e) {
			logger.error("fetch data failed.", e);
		} finally {
			close(dbConnection, stmt, rs);
		}
		return userList;
	}
	
	public Users getTunnelInfo(int tunnelId) {
		String sql = "select *  from users where tunnel_id = ?";
		Connection dbConnection = null;
		PreparedStatement stmt = null;
		ResultSet rs = null;
		Users users  = null;
		try {
			dbConnection = getDBConnection();
			stmt = dbConnection.prepareStatement(sql);
			stmt.setInt(1, tunnelId);
			rs = stmt.executeQuery();
			if (rs.next()) {
				users = new Users();
				setUsersInfo(users, rs);
			}
		} catch (Exception e) {
			logger.error("XenicsDB: connecting db failed.", e);
		} finally {
			close(dbConnection, stmt, rs);
		}
		return users;
	}
	
	public Users getConnectedTunnelInfo(String trustIp) {
		String sql = "select *  from users where rent_ip = ? and using_flag = 1";
		Connection dbConnection = null;
		PreparedStatement stmt = null;
		ResultSet rs = null;
		Users users  = null;
		try {
			dbConnection = getDBConnection();
			stmt = dbConnection.prepareStatement(sql);
			stmt.setString(1, trustIp);
			rs = stmt.executeQuery();
			if (rs.next()) {
				users = new Users();
				setUsersInfo(users, rs);
			}
		} catch (Exception e) {
			logger.error("XenicsDB: connecting db failed.", e);
		} finally {
			close(dbConnection, stmt, rs);
		}
		return users;
	}
	
	private void setUsersInfo(Users users, ResultSet rs) throws SQLException {
		users.tunnel_id = rs.getInt("tunnel_id");
		users.client_id = rs.getInt("client_id");
		users.user_id = rs.getString("user_id");
		users.rent_ip = rs.getString("rent_ip");
		users.trust_ip = rs.getString("trust_ip");
		users.login_date = rs.getTimestamp("login_date");
		users.tun_name = rs.getString("tun_name");
		users.using_flag = rs.getInt("using_flag");
	}
	
	public int getTotalNumberOfConnectedInfo() {
		String sql = "select count(*)  from users where using_flag = 1";
		Connection dbConnection = null;
		PreparedStatement stmt = null;
		ResultSet rs = null;
		int result = 0;
		try {
			dbConnection = getDBConnection();
			stmt = dbConnection.prepareStatement(sql);
			rs = stmt.executeQuery();
			if (rs.next()) {
				result = rs.getInt(1);
			}
		} catch (Exception e) {
			logger.error("XenicsDB: connecting db failed.", e);
		} finally {
			close(dbConnection, stmt, rs);
		}
		return result;
	}
	
	public int getTotalNumberOfConnectedInfoWithFilter(String filter) {
		String sql = "select count(*)  from users where (using_flag = 1) and (" + filter + ")";
		Connection dbConnection = null;
		PreparedStatement stmt = null;
		ResultSet rs = null;
		int result = 0;
		try {
			dbConnection = getDBConnection();
			stmt = dbConnection.prepareStatement(sql);
			rs = stmt.executeQuery();
			if (rs.next()) {
				result = rs.getInt(1);
			}
		} catch (Exception e) {
			logger.error("XenicsDB: connecting db failed.", e);
		} finally {
			close(dbConnection, stmt, rs);
		}
		return result;
	}
	
	public int getUsersUsingFlag(String loginName) {
		String sql = "select using_flag from users where user_id = ?";
		Connection dbConnection = null;
		PreparedStatement stmt = null;
		ResultSet rs = null;
		int result = 0;
		try {
			dbConnection = getDBConnection();
			stmt = dbConnection.prepareStatement(sql);
			stmt.setString(1, loginName);
			rs = stmt.executeQuery();
			if (rs.next()) {
				result = rs.getInt("using_flag");
			}
		} catch (Exception e) {
			logger.error("XenicsDB: connecting db failed.", e);
		} finally {
			close(dbConnection, stmt, rs);
		}
		return result;
	}
	
	public int getClientId(int tunnel_id) {
		String sql = "select client_id from users where tunnel_id = ?";
		Connection dbConnection = null;
		PreparedStatement stmt = null;
		ResultSet rs = null;
		int result = -1;				//none
		try {
			dbConnection = getDBConnection();
			stmt = dbConnection.prepareStatement(sql);
			stmt.setInt(1, tunnel_id);
			rs = stmt.executeQuery();
			if (rs.next()) {
				result = rs.getInt("client_id");
			}
		} catch (Exception e) {
			logger.error("XenicsDB: connecting db failed.", e);
		} finally {
			close(dbConnection, stmt, rs);			
		}
		return result;	
	}
	
	public int getClientId(String loginName) {
		String sql = "select client_id from users where user_id = ?";
		Connection dbConnection = null;
		PreparedStatement stmt = null;
		ResultSet rs = null;
		int result = -1;				//none
		try {
			dbConnection = getDBConnection();
			stmt = dbConnection.prepareStatement(sql);
			stmt.setString(1, loginName);
			rs = stmt.executeQuery();
			if (rs.next()) {
				result = rs.getInt("client_id");
			}
		} catch (Exception e) {
			logger.error("XenicsDB: connecting db failed.", e);
		} finally {
			close(dbConnection, stmt, rs);			
		}
		return result;	
	}
	
	private void close(Connection dbConnection, Statement stmt, ResultSet rs) {
		try {
			if(rs != null)
				rs.close();
			if(stmt != null)
				stmt.close();
			if(dbConnection != null)
				dbConnection.close();
		} catch (SQLException e) {
			logger.error("error occurred during close.", e);
		}
	}
	
	public int removeAllSplitRoute() {
		String sql = "delete from split_route";
		Connection dbConnection = null;
		PreparedStatement stmt = null;
		
		int result = 0;				//none
		try {
			dbConnection = getDBConnection();
			stmt = dbConnection.prepareStatement(sql);
			result = stmt.executeUpdate();			
		} catch (Exception e) {
			logger.error("XenicsDB: connecting db failed.", e);
		} finally {
			close(dbConnection, stmt, null);			
		}
		return result;			
	}
	
	public int removeAllDenyIpRange() {
		String sql = "delete from deny_ip_range";
		Connection dbConnection = null;
		PreparedStatement stmt = null;
		
		int result = 0;				//none
		try {
			dbConnection = getDBConnection();
			stmt = dbConnection.prepareStatement(sql);
			result = stmt.executeUpdate();			
		} catch (Exception e) {
			logger.error("XenicsDB: connecting db failed.", e);
		} finally {
			close(dbConnection, stmt, null);			
		}
		return result;		
	}
	
	public int removeAllAllowSvrRules() {
		String sql = "delete from allow_svr_rules";
		Connection dbConnection = null;
		PreparedStatement stmt = null;
		
		int result = 0;				//none
		try {
			dbConnection = getDBConnection();
			stmt = dbConnection.prepareStatement(sql);
			result = stmt.executeUpdate();			
		} catch (Exception e) {
			logger.error("XenicsDB: connecting db failed.", e);
		} finally {
			close(dbConnection, stmt, null);			
		}
		return result;		
	}
	
	public void insertSplitRoutingEntries(int profileId, List<SplitRoutingEntry> splitRoutingEntries) {
		String sql = "insert into split_route (access_policy_id, split_ip, cidr, gateway) values (?, ?, ?, ?)";
		Connection dbConnection = null;
		PreparedStatement stmt = null;
		
		try {
			dbConnection = getDBConnection();
			stmt = dbConnection.prepareStatement(sql);
			
			for(SplitRoutingEntry entry : splitRoutingEntries){
				stmt.setInt(1, profileId);
				stmt.setString(2, entry.getIp());
				stmt.setInt(3, entry.getCidr());
				stmt.setString(4, entry.getGateway());
				stmt.addBatch();
			}
			stmt.executeBatch();			
		} catch (Exception e) {
			logger.error("XenicsDB: connecting db failed.", e);
		} finally {
			close(dbConnection, stmt, null);			
		}		
	}
	
	public void insertAllowServers(int profileId, List<Server> normalAcl) {
		String sql = "insert into allow_svr_rules (access_policy_id, server_name, ipv4_subnets, proto, port_range) values (?, ?, ?, ?, ?)";
		Connection dbConnection = null;
		PreparedStatement stmt = null;
		
		try {
			dbConnection = getDBConnection();
			stmt = dbConnection.prepareStatement(sql);
			
			for(Server server : normalAcl){
				List<IpEndpoint> endpoints = server.getEndpoints();
				for(IpEndpoint endpoint : endpoints){
					List<PortRange> portRanges = endpoint.getPortRanges();
					for(PortRange portRange : portRanges) {
						stmt.setInt(1, profileId);
						stmt.setString(2, server.getName());
						stmt.setString(3, endpoint.getIp4Address() + "/" + endpoint.getIp4Mask());
						stmt.setString(4, portRange.getProtocol());
						stmt.setString(5, portRange.getPortFrom() + "-" + portRange.getPortTo());
						stmt.addBatch();
					}
				}
			}
			stmt.executeBatch();			
		} catch (Exception e) {
			logger.error("XenicsDB: connecting db failed.", e);
		} finally {
			close(dbConnection, stmt, null);			
		}		
	}
	
	public void insertInternalNetworkRanges(int profileId, List<InternalNetworkRange> internalNetworkRanges) {
		String sql = "insert into deny_ip_range (access_policy_id, deny_ip, cidr) values (?, ?, ?)";
		Connection dbConnection = null;
		PreparedStatement stmt = null;
		
		try {
			dbConnection = getDBConnection();
			stmt = dbConnection.prepareStatement(sql);
			
			for(InternalNetworkRange range : internalNetworkRanges){
				stmt.setInt(1, profileId);
				stmt.setString(2, range.getIp());
				stmt.setInt(3, range.getCidr());
				stmt.addBatch();
			}
			stmt.executeBatch();			
		} catch (Exception e) {
			logger.error("XenicsDB: connecting db failed.", e);
		} finally {
			close(dbConnection, stmt, null);			
		}		
	}
	
	public void createFtpBackupTable() {
		String sql = "CREATE  TABLE IF NOT EXISTS ftp_backup(" +
								"file_name VARCHAR(30) NOT NULL primary key," +
								"type TINYINT UNSIGNED default 0," +
								"file_size VARCHAR(10) NOT NULL," +  
								"backup_date DATETIME NOT NULL," +
								"state_code INT UNSIGNED default 0);";
		
		Connection dbConnection = null;
		Statement  stmt = null;
		
		try {
			dbConnection = getDBConnection();
			stmt = dbConnection.createStatement();
						
			stmt.executeUpdate(sql);
		} catch (Exception e) {
			logger.error("XenicsDB: create ftp_backup table failed.", e);
		} finally {			
			close(dbConnection, stmt, null);			
		}		
		
	}
	
	public void insertBackupInfo(int type, String fileName, String fileSize, int stateCode) {
		String sql = "insert into ftp_backup (type, file_name, file_size, backup_date, state_code) values (?, ?, ?, ?, ?)";
		Connection dbConnection = null;
		PreparedStatement stmt = null;
		
		try {
			dbConnection = getDBConnection();
			stmt = dbConnection.prepareStatement(sql);
			
			stmt.setInt(1, type);
			stmt.setString(2, fileName);
			stmt.setString(3, fileSize);
			
			long timeNow = Calendar.getInstance().getTimeInMillis();
			java.sql.Timestamp ts = new java.sql.Timestamp(timeNow);
			stmt.setTimestamp(4, ts);
			
			stmt.setInt(5, stateCode);			
			
			stmt.executeUpdate();			
		} catch (Exception e) {
			logger.error("XenicsDB: connecting db failed.", e);
		} finally {
			close(dbConnection, stmt, null);			
		}		
	}
	
	public void updateBackupState(String fileName, int stateCode) {
		String sql = "update ftp_backup set state_code=? where file_name=?";
		Connection dbConnection = null;
		PreparedStatement stmt = null;
		
		try {
			dbConnection = getDBConnection();
			stmt = dbConnection.prepareStatement(sql);
			
			stmt.setInt(1, stateCode);
			stmt.setString(2, fileName);			
			
			stmt.executeUpdate();			
		} catch (Exception e) {
			logger.error("XenicsDB: connecting db failed.", e);
		} finally {
			close(dbConnection, stmt, null);			
		}		
	}
	
	public int getTotalNumOfFtpBackup(int type) {
		String sql = "select count(*)  from ftp_backup where type = ?";
		Connection dbConnection = null;
		PreparedStatement stmt = null;
		ResultSet rs = null;
		int result = 0;
		try {
			dbConnection = getDBConnection();
			stmt = dbConnection.prepareStatement(sql);
			stmt.setInt(1, type);
			rs = stmt.executeQuery();
			if (rs.next()) {
				result = rs.getInt(1);
			}
		} catch (Exception e) {
			logger.error("XenicsDB: connecting db failed.", e);
		} finally {
			close(dbConnection, stmt, rs);
		}
		return result;
	}
	
	public List<FtpBackup> fetchFtpBackupRecords(int type, int limit, int offset) {
		String sql = "select * from ftp_backup where type = ? limit ? offset ?";
		
		Connection dbConnection = null;
		PreparedStatement stmt = null;
		ResultSet rs = null;
		List<FtpBackup> userList = new ArrayList<FtpBackup>();
		
		try {
			dbConnection = getDBConnection();
			
			stmt = dbConnection.prepareStatement(sql);
			stmt.setInt(1, type);
			stmt.setInt(2, limit);
			stmt.setInt(3, offset);
			
			rs = stmt.executeQuery();
			while (rs.next()) {
				FtpBackup ftpBackup = new FtpBackup();
				ftpBackup.file_name = rs.getString("file_name");
				ftpBackup.file_size = rs.getString("file_size");
				ftpBackup.backup_date = rs.getTimestamp("backup_date");				
				ftpBackup.state_code = rs.getInt("state_code");
				userList.add(ftpBackup);
			}
		} catch (Exception e) {
			logger.error("fetch data failed.", e);
		} finally {
			close(dbConnection, stmt, rs);
		}
		return userList;
	}
	
	public void deleteBackupRecord(String fileName) {
		String sql = "delete from ftp_backup where file_name=?";
		Connection dbConnection = null;
		PreparedStatement stmt = null;
		
		try {
			dbConnection = getDBConnection();
			stmt = dbConnection.prepareStatement(sql);
			
			stmt.setString(1, fileName);			
			
			stmt.executeUpdate();			
		} catch (Exception e) {
			logger.error("XenicsDB: connecting db failed.", e);
		} finally {
			close(dbConnection, stmt, null);			
		}		
	}
	
	public List<String> fetchFtpBackupFileNames(int type, int stateCode) {
		String sql = "select file_name from ftp_backup where type = ? and state_code = ?";
		
		Connection dbConnection = null;
		PreparedStatement stmt = null;
		ResultSet rs = null;
		List<String> fileNames = new ArrayList<String>();
		
		try {
			dbConnection = getDBConnection();
			
			stmt = dbConnection.prepareStatement(sql);
			stmt.setInt(1, type);
			stmt.setInt(2, stateCode);
			
			rs = stmt.executeQuery();
			while (rs.next()) {
				fileNames.add(rs.getString("file_name"));
			}
		} catch (Exception e) {
			logger.error("fetch data failed.", e);
		} finally {
			close(dbConnection, stmt, rs);
		}
		return fileNames;
	}
	
	public int retrieveFtpBackupStateCode(String fileName) {
		String sql = "select state_code from ftp_backup where file_name = ?";
		
		Connection dbConnection = null;
		PreparedStatement stmt = null;
		ResultSet rs = null;
		int result = -1;
		
		try {
			dbConnection = getDBConnection();
			
			stmt = dbConnection.prepareStatement(sql);
			stmt.setString(1, fileName);
			
			rs = stmt.executeQuery();
			if (rs.next()) {
				result = rs.getInt("state_code");
			}
		} catch (Exception e) {
			logger.error("fetch data failed.", e);
		} finally {
			close(dbConnection, stmt, rs);
		}
		return result;
	}
	
	public void updatePrevPolicyRollbackState(int stateCode) {
		String sql = "update ftp_backup set state_code=? where state_code=30 and type = 1"; //state is 'rollback'
		Connection dbConnection = null;
		PreparedStatement stmt = null;
		
		try {
			dbConnection = getDBConnection();
			stmt = dbConnection.prepareStatement(sql);
			
			stmt.setInt(1, stateCode);
			
			stmt.executeUpdate();			
		} catch (Exception e) {
			logger.error("XenicsDB: connecting db failed.", e);
		} finally {
			close(dbConnection, stmt, null);			
		}		
	}
}
