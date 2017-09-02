package kr.co.future.sslvpn.core.xenics.impl;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jolbox.bonecp.BoneCP;
import com.jolbox.bonecp.BoneCPConfig;

public class RemoteDB {
	private final Logger logger = LoggerFactory.getLogger(RemoteDB.class.getName());
	
	private static String DB_DRIVER = "com.mysql.jdbc.Driver";;
	private static String DB_CONNECTION;
	private static String DB_USER;
	private static String DB_PASSWORD;
	private static String DB_TABLE;
	
	protected BoneCP  connectionPool = null;
	
	public RemoteDB(String url, String user, String password, String table) {
		DB_CONNECTION = url;
		DB_USER = user;
		DB_PASSWORD = password;
		DB_TABLE = table;
		
		createConnectionPool(DB_CONNECTION, DB_USER, DB_PASSWORD);
	}
		
	public void shutdown() {
		if(connectionPool != null)
			connectionPool.shutdown();
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
        		logger.info("remote db: created connection pool. partition: 1, min: 2, max: 5, timeout(seconds): 30");
			} catch (SQLException e) {
				logger.error("create connection pool for remote db failed", e);
			}
		}
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
	
	public boolean isDupLogin(String loginName) {
		String sql = "select using_flag from " + DB_TABLE + " where user_id = ?";
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
		return (result >= 1);
	}
}
