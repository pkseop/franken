package kr.co.future.sslvpn.core.impl;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import kr.co.future.sslvpn.core.GlobalConfig;
import kr.co.future.sslvpn.core.GlobalConfigApi;
import kr.co.future.sslvpn.core.PasswordHash;
import kr.co.future.sslvpn.core.SqlAuthResult;
import kr.co.future.sslvpn.core.SqlAuthService;
import kr.co.future.sslvpn.model.AccessGateway;
import kr.co.future.sslvpn.model.api.AccessGatewayApi;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Invalidate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.apache.felix.ipojo.annotations.Validate;

import kr.co.future.rpc.RpcException;
import kr.co.future.sslvpn.core.impl.SqlAuthServiceImpl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jolbox.bonecp.BoneCP;
import com.jolbox.bonecp.BoneCPConfig;

@Component(name = "frodo-sql-auth-service")
@Provides
public class SqlAuthServiceImpl implements SqlAuthService {
	private final Logger logger = LoggerFactory.getLogger(SqlAuthServiceImpl.class.getName());

	@Requires
	private AccessGatewayApi gatewayApi;
	
	//pks: 2014-04-30. use connection pool to enhance performance.
	private BoneCP  connectionPool = null;
	
	@Requires
	private GlobalConfigApi configApi;
	
	private Boolean enableConnPool = null;
	
	@Validate
	public void start() {
		GlobalConfig config = configApi.getGlobalConfig();
		enableConnPool = config.isConnPoolEnabled();
		String sqlConnPoolConfig = config.getSqlConnPoolConfig();
		if(sqlConnPoolConfig == null || sqlConnPoolConfig.equals("")){
			sqlConnPoolConfig ="1,3,10,30,5,SELECT 1"; 		//default setting.
			config.setSqlConnPoolConfig(sqlConnPoolConfig);
			configApi.setGlobalConfig(config);
		}
		settingChanged();		//to create connection pool when bundle restarted or updated.
	}
	
	@Invalidate
	public void stop() {
		shutdownConnectionPool();
	}

	@Override
	public boolean isEnabled() {
		AccessGateway gw = gatewayApi.getCurrentAccessGateway();
		if (gw == null)
			return false;

		return gw.isUseSqlAuth();
	}

	@Override
	public Map<String, Object> login(Map<String, Object> props) {
		AccessGateway gw = gatewayApi.getCurrentAccessGateway();
		if (gw == null) {
			logger.warn("frodo-sql-auth-service: sql auth, config not set");
			throw new RpcException("config not set");
		}

		String loginName = (String) props.get("id");
		String password = (String) props.get("pw");

		logger.trace("frodo-sql-auth-service: sql auth, verifying login name [{}]", loginName);

		Map<String, Object> m = new HashMap<String, Object>();
		SqlAuthResult verify = verifyPassword(loginName, password);

		if (verify.isSuccess()) {
			m.put("name", verify.getName());
			m.put("org_unit_name", verify.getOrgUnitName());
			m.put("auth_code", 0);
			logger.trace("frodo-sql-auth-service: sql auth, login [{}] success, name=[{}], orgUnitName=[{}]", new Object[] { loginName,
					verify.getName(), verify.getOrgUnitName() });
		} else {
			// password-fail
			m.put("auth_code", 2);
			logger.trace("frodo-sql-auth-service: sql auth, login [{}] fail", loginName);
		}

		return m;
	}

	@Override
	public String getIdn(String loginName) {
		AccessGateway gw = gatewayApi.getCurrentAccessGateway();
		if (gw == null || !gw.isUseSqlAuth()) {
			logger.warn("frodo-sql-auth-service: config not set");
			throw new RpcException("config not set");
		}

		String selectQuery = gw.getIdnSql();

		if (selectQuery == null) {
			logger.warn("frodo-sql-auth-service: select idn query not set");
			throw new RpcException("select idn query not set");
		}

		Connection con = null;
		PreparedStatement psmt = null;
		ResultSet rs = null;

		try {

			logger.trace("frodo-sql-auth-service: returning idn for [{}]", loginName);
			con = getConnection();

			if (con == null) {
				logger.warn("frodo-sql-auth-service: connection error");
				throw new RpcException("connection error");
			}

			psmt = con.prepareStatement(selectQuery);
			psmt.setString(1, loginName);
			rs = psmt.executeQuery();

			// 결과값 반환
			if (rs.next())
				return rs.getString(1);
			return null;

		} catch (Exception e) {
			throw new RuntimeException(e);
		} finally {
			close(rs, psmt, con);
		}
	}

	@Override
	public SqlAuthResult verifyUser(String loginName) {
		AccessGateway gw = gatewayApi.getCurrentAccessGateway();
		if (gw == null || !gw.isUseSqlAuth()) {
			logger.warn("frodo-sql-auth-service: config not set");
			throw new RpcException("config not set");
		}

		String selectQuery = gw.getUserSql();

		if (selectQuery == null) {
			logger.warn("frodo-sql-auth-service: select user query not set");
			throw new RpcException("select subjectDn user query not set");
		}

		Connection con = null;
		PreparedStatement psmt = null;
		ResultSet rs = null;

		SqlAuthResult result = new SqlAuthResult();
		try {

			logger.trace("frodo-sql-auth-service: returning idn for [{}]", loginName);
			con = getConnection();

			if (con == null) {
				logger.warn("frodo-sql-auth-service: connection error");
				throw new RpcException("connection error");
			}

			psmt = con.prepareStatement(selectQuery);
			psmt.setString(1, loginName);
			rs = psmt.executeQuery();

			// 결과값 반환
			if (rs.next()) {
				result.setName(rs.getString(1));
				if (result.getName().isEmpty())
					result.setName(loginName);

				result.setOrgUnitName(rs.getString(2));
				result.setSuccess(true);
			} else {
				result.setSuccess(false);
			}

		} catch (Exception e) {
			throw new RuntimeException(e);
		} finally {
			close(rs, psmt, con);
		}

		return result;
	}

	@Override
	public SqlAuthResult verifyPassword(String loginName, String password) {
		AccessGateway gw = gatewayApi.getCurrentAccessGateway();
		if (gw == null || !gw.isUseSqlAuth()) {
			logger.warn("frodo-sql-auth-service: sql auth, config not set");
			throw new RpcException("config not set");
		}

		if (loginName == null || password == null) {
			logger.debug("frodo-sql-auth-service: sql auth, login name [{}] or password [{}] is null", loginName, password);
			throw new RpcException("login name or password is null");
		}

		String passwordHashType = gw.getPasswordHashType();

		logger.debug("frodo-sql-auth-service: sql auth, passwordHashType=[{}]", passwordHashType);

		if (passwordHashType != null) {

			String salt = getSalt(loginName);
			String hashSource = password;

			logger.debug("frodo-sql-auth-service: sql auth, login name=[{}], source=[{}]", loginName, hashSource);
			logger.debug("frodo-sql-auth-service: sql auth, salt=[{}]", salt);

			if (salt != null && !salt.isEmpty()) {

				// front(1), rear(2)
				Integer saltLocation = gw.getSaltLocation();

				if (saltLocation == null) {
					logger.warn("frodo-sql-auth-service: sql auth, salt location not set");
					gw.setSaltLocation(0);
				}

				if (saltLocation == 1)
					hashSource = salt + password;
				else if (saltLocation == 2)
					hashSource = password + salt;
			}

			password = PasswordHash.makeHash(passwordHashType, hashSource, gw.getPasswordEncoding());

			logger.debug("frodo-sql-auth-service: sql auth, makeHash=[{}]", password);
		}

		String selectQuery = gw.getAuthSql();

		if (selectQuery == null) {
			logger.warn("frodo-sql-auth-service: select auth query not set");
			throw new RpcException("select subjectDn auth query not set");
		}

		Connection con = null;
		PreparedStatement psmt = null;
		ResultSet rs = null;

		SqlAuthResult result = new SqlAuthResult();

		try {

			con = getConnection();

			if (con == null) {
				logger.warn("frodo-sql-auth-service: sql auth, connection error");
				throw new RpcException("connection error");
			}

			psmt = con.prepareStatement(selectQuery);
			psmt.setString(1, loginName);
			psmt.setString(2, password);
			rs = psmt.executeQuery();

			// 결과값 반환
			if (rs.next()) {
				result.setName(rs.getString(1));
				result.setOrgUnitName(rs.getString(2));
				result.setSuccess(true);
			}

		} catch (Exception e) {
			throw new RuntimeException(e);
		} finally {
			close(rs, psmt, con);
		}

		return result;

	}

	private Connection getConnection() throws SQLException, ClassNotFoundException {
		if(enableConnPool != null && enableConnPool)
			return getConnFromPool();
		else
			return getConnInstantly();
	}
	
	private Connection getConnFromPool() throws SQLException {
		if(connectionPool == null) {
			resetConnectionPool();
		}
		
		if(connectionPool != null)
			return connectionPool.getConnection();
		
		return null;
	}
	
	private Connection getConnInstantly() throws ClassNotFoundException, SQLException {
		AccessGateway gw = gatewayApi.getCurrentAccessGateway();
		String url = gw.getDbConnectionString();
		String id = gw.getDbAccount();
		String pw = gw.getDbPassword();

		logger.debug("frodo core: sql auth, connection string [{}]", url);

		String[] urlSplit = url.split(":");
		String urlKey = urlSplit[1].toLowerCase();

		if (urlKey.equals("oracle")) {
			// oracle
			Class.forName("oracle.jdbc.OracleDriver");
		} else if (urlKey.equals("sqlserver")) {
			// mssql
			Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
		} else if (urlKey.equals("mysql")) {
			// mysql
			Class.forName("com.mysql.jdbc.Driver");
		} else if (urlKey.equals("sybase")) {
			// sybase
			Class.forName("com.sybase.jdbc4.jdbc.SybDriver");
		}

		// Set Connection Time out in second
		DriverManager.setLoginTimeout(10);

		// return Connection
		Connection con = DriverManager.getConnection(url, id, pw);
		return con;
	}
	
	private void createConnectionPool(String url, String id, String pw) {
		if(connectionPool == null) {
			BoneCPConfig bonecpConfig = new BoneCPConfig();
			bonecpConfig.setJdbcUrl(url); 
			bonecpConfig.setUsername(id); 
			bonecpConfig.setPassword(pw);
	        
	      GlobalConfig config = configApi.getGlobalConfig();
	      String sqlConnPoolConfig = config.getSqlConnPoolConfig();
	    	String[] val = sqlConnPoolConfig.split(",");
        	bonecpConfig.setPartitionCount(Integer.parseInt(val[0]));
        	bonecpConfig.setMinConnectionsPerPartition(Integer.parseInt(val[1]));
	      bonecpConfig.setMaxConnectionsPerPartition(Integer.parseInt(val[2]));
	      bonecpConfig.setConnectionTimeout(Integer.parseInt(val[3]), TimeUnit.SECONDS);
	      bonecpConfig.setIdleConnectionTestPeriodInMinutes(Integer.parseInt(val[4]));
	      bonecpConfig.setConnectionTestStatement(val[5]);
	      
	      try {
				connectionPool = new BoneCP(bonecpConfig); // setup the connection pool
				String log = "frodo-sql-auth-service: connection pool created.\n" 
						+ "partition [{}], min [{}], max [{}], timeout(seconds) [{}], "
						+ "connection test period(minutes) [{}], connection test statement [{}]";
					logger.info(log, new Object[]{val[0], val[1], val[2], val[3], val[4], val[5]});
			} catch (SQLException e) {
				logger.error("frodo-sql-auth-service: create connection pool failed", e);
			}
		}
	}
	
	@Override
	public void resetConnectionPool() {
		if(enableConnPool == null || enableConnPool == false)
			return;
		
		shutdownConnectionPool();
		
		AccessGateway gw = gatewayApi.getCurrentAccessGateway();
		if(gw != null && gw.isUseSqlAuth()) {
			String url = gw.getDbConnectionString();
			String id = gw.getDbAccount();
			String pw = gw.getDbPassword();
			
			logger.debug("frodo-sql-auth-service: sql auth, connection string [{}]", url);
	
			String[] urlSplit = url.split(":");
			String urlKey = urlSplit[1].toLowerCase();
			try {
				if (urlKey.equals("oracle")) {
					// oracle
					Class.forName("oracle.jdbc.OracleDriver");
				} else if (urlKey.equals("sqlserver")) {
					// mssql
					Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
				} else if (urlKey.equals("mysql")) {
					// mysql
					Class.forName("com.mysql.jdbc.Driver");
				} else if (urlKey.equals("sybase")) {
					// sybase
					Class.forName("com.sybase.jdbc4.jdbc.SybDriver");
				}
				
				createConnectionPool(url, id, pw);
				
			} catch (ClassNotFoundException e) {
				logger.error("frodo-sql-auth-service: error occurred during reset connection pool", e);
			}
		}
	}
	
	private void shutdownConnectionPool() {
		if(connectionPool != null) {
			connectionPool.shutdown();
			connectionPool = null;
			logger.info("frodo-sql-auth-service: connection pool shutdowned.");
		}
	}

	private void close(ResultSet rs, PreparedStatement psmt, Connection con) {
		logger.trace("frodo-sql-auth-service: close sql jdbc");
		if(rs != null){
			try {
				rs.close();
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		if (psmt != null) {
			try {
				psmt.close();
			} catch (SQLException e) {
			}
		}
		if (con != null) {
			try {
				con.close();
			} catch (SQLException e) {
			}
		}
	}

	private String getSalt(String loginName) {
		AccessGateway gw = gatewayApi.getCurrentAccessGateway();
		if (gw == null) {
			logger.warn("frodo-sql-auth-service: sql auth, config not set");
			return null;
		}

		if (loginName == null) {
			logger.warn("frodo-sql-auth-service: sql auth, login name is null");
			return null;
		}

		String selectQuery = gw.getSaltSql();

		if (selectQuery == null) {
			logger.debug("frodo-sql-auth-service: sql auth, select salt query not set");
			return null;
		}

		Connection con = null;
		PreparedStatement psmt = null;
		ResultSet rs = null;
		String result = null;

		try {

			con = getConnection();
			psmt = con.prepareStatement(selectQuery);
			psmt.setString(1, loginName);
			rs = psmt.executeQuery();

			// 결과값 반환
			if (rs.next()) {
				result = rs.getString(1);
			}

		} catch (Exception e) {
			throw new RuntimeException(e);
		} finally {
			close(rs, psmt, con);
		}

		return result;
	}

	@Override
	public String getSubjectDn(String loginName) {
		AccessGateway gw = gatewayApi.getCurrentAccessGateway();
		if (gw == null || !gw.isUseSqlAuth()) {
			logger.warn("frodo-sql-auth-service: config not set");
			throw new RpcException("config not set");
		}

		String selectQuery = gw.getSubjectDnSql();

		if (selectQuery == null) {
			logger.warn("frodo-sql-auth-service: select subjectDn query not set");
			throw new RpcException("select subjectDn query not set");
		}

		Connection con = null;
		PreparedStatement psmt = null;
		ResultSet rs = null;

		try {

			logger.trace("frodo-sql-auth-service: returning subjectDn for [{}]", loginName);
			con = getConnection();

			if (con == null) {
				logger.warn("frodo-sql-auth-service: connection error");
				throw new RpcException("connection error");
			}

			psmt = con.prepareStatement(selectQuery);
			psmt.setString(1, loginName);
			rs = psmt.executeQuery();

			// 결과값 반환
			if (rs.next())
				return rs.getString(1);
			return null;

		} catch (Exception e) {
			throw new RuntimeException(e);
		} finally {
			close(rs, psmt, con);
		}
	}

	@Override
	public boolean isPasswordExpirySupported() {
		AccessGateway gw = gatewayApi.getCurrentAccessGateway();
		String passwordExpirySql = gw.getPasswordExpirySql();
		if (passwordExpirySql == null || passwordExpirySql.trim().isEmpty())
			return false;

		return true;
	}

	@Override
	public long getPasswordExpiry(String loginName) {
		AccessGateway gw = gatewayApi.getCurrentAccessGateway();
		String passwordExpirySql = gw.getPasswordExpirySql();

		if (passwordExpirySql == null || passwordExpirySql.trim().isEmpty()) {
			logger.error("frodo-sql-auth-service: password expiry query not set");
			throw new RpcException("password expiry query not set");
		}

		Connection con = null;
		PreparedStatement psmt = null;
		ResultSet rs = null;
		java.sql.Date sqlDate = null;
		try {
			logger.trace("frodo-sql-auth-service: returning password expiry for [{}]", loginName);
			con = getConnection();

			psmt = con.prepareStatement(passwordExpirySql);
			psmt.setString(1, loginName);
			rs = psmt.executeQuery();

			// 결과값 반환
			if (rs.next())
				sqlDate = rs.getDate(1);

		} catch (Exception e) {
			throw new RuntimeException(e);
		} finally {
			close(rs, psmt, con);
		}

		return (sqlDate.getTime() - new Date().getTime()) / 1000;
	}
	
	//pks: 2014-04-30. to apply changed setting to connection pool. this method is called when sslvpn setting changed.
	//create connection pool can takes long time so use thread to create it when setting changed.
	@Override
	public void settingChanged() {
		if(enableConnPool != null && enableConnPool)
			new Thread(new CreateConnectionPool(this)).start();
	}
	
	private class CreateConnectionPool implements Runnable {
		private SqlAuthService sqlAuthService;
		
		public CreateConnectionPool(SqlAuthService sqlAuthService) {
			this.sqlAuthService = sqlAuthService;
		}
		
		@Override
		public void run() {
			sqlAuthService.resetConnectionPool();
		}
	}
	
	@Override
	public void enableConnPool(Boolean enableConnPool) {
		this.enableConnPool = enableConnPool;
		if(enableConnPool)
			resetConnectionPool();
		else
			shutdownConnectionPool();
	}
}