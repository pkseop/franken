package kr.co.future.sslvpn.auth.hb.impl;

import java.net.InetSocketAddress;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Invalidate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.apache.felix.ipojo.annotations.Validate;

import kr.co.future.confdb.Config;
import kr.co.future.confdb.ConfigDatabase;
import kr.co.future.confdb.ConfigService;
import kr.co.future.dom.api.UserApi;
import kr.co.future.dom.model.User;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import kr.co.future.sslvpn.core.AuthService;
import kr.co.future.sslvpn.core.Tunnel;
import kr.co.future.sslvpn.core.TunnelEventListener;
//import kr.co.future.sslvpn.model.UserExtension;
import kr.co.future.sslvpn.auth.BaseExternalAuthApi;
import kr.co.future.sslvpn.auth.hb.HbAuthApi;
import kr.co.future.sslvpn.auth.hb.HbConfig;

@Component(name = "hb-auth-service")
@Provides
public class HbAuthService extends BaseExternalAuthApi implements HbAuthApi, TunnelEventListener {
	private final Logger logger = LoggerFactory.getLogger(HbAuthService.class.getName());
	@Requires
	private ConfigService conf;
	
	@Requires
	private UserApi domUserApi;
	
	@Requires
	private kr.co.future.sslvpn.model.api.UserApi userApi;
	
	@Requires
	private AuthService auth;

	private HbConfig config;

	@Validate
	public void start() {
		config = null;
		ConfigDatabase db = conf.ensureDatabase("hb");
		Config c = db.findOne(HbConfig.class, null);
		if (c != null)
			config = c.getDocument(HbConfig.class);
		
		auth.addTunnelEventListener(this);
	}
	
	@Invalidate
	public void stop() {
		if (auth != null)
			auth.removeTunnelEventListener(this);
	}

	@Override
	public Object login(Map<String, Object> props) {
		String loginName = (String) props.get("id");
		String password = (String) props.get("pw");
		String hostName = (String) props.get("host_name");

		Map<String, Object> m = new HashMap<String, Object>();
		if (config == null) {
			logger.warn("frodo hb auth: config not set");
			m.put("auth_code", 2); // password fail
			return m;
		}
		
		Map<String, Object> result = getLoginQueryResult(loginName, password, hostName);
		 
//		if ((Integer) result.get("auth_code") == 0) {
//			//conf db에서 사용자의 그룹 정보를 가져와서 "전산부서_서버접근" 그룹의 사용자이면 MySQL 데이터베이스에 정보를 기록한다.

//		}

		return result;
	}

	@Override
	public Map<String, Object> verifyUser(String loginName) {
		Map<String, Object> m = new HashMap<String, Object>();
		m.put("result", false);
		if (config == null) {
			logger.warn("frodo hb auth: config not set");
			return m;
		}

		logger.trace("frodo hb auth: trying to verify user, login name [{}]", loginName);

		Connection con = null;
		PreparedStatement stmt = null;
		try {
			logger.trace("frodo hb auth: open sql jdbc");
			con = getConnectionString();
			// select EMP_NO, IP, DEPT_NAME, aes_decrypt(unhex(EMP_PASS),
			// 'portablebranch') as EMP_PASS from V_POBR_VPN_USER where
			// EMP_NO=8888;
			String query = "SELECT EMP_NAME, EMP_NO, IP, DEPT_NAME, aes_decrypt(unhex(res_reg_no),'portablebranch') as res_reg_no FROM "
					+ config.getTableName() + " WHERE EMP_NO = ?";
			stmt = con.prepareStatement(query);
			stmt.setString(1, loginName);
			ResultSet rs = stmt.executeQuery();
			if (rs.next()) {
				m.put("result", true);
				if (rs.getString("IP") != null)
					m.put("static_ip", rs.getString("IP"));
				m.put("org_unit_name", rs.getString("DEPT_NAME"));
				m.put("idn", rs.getString("res_reg_no"));
				m.put("name", rs.getString("EMP_NAME"));
				return m;
			} else
				m.put("result", false);
			return m;
		} catch (Throwable e) {
			logger.trace("frodo hb auth: cannot verify user", e);
			throw new RuntimeException(e);
		} finally {
			close(stmt, con);
		}
	}

	@Override
	public HbConfig getConfig() {
		ConfigDatabase db = conf.ensureDatabase("hb");
		Config c = db.findOne(HbConfig.class, null);
		if (c != null)
			return c.getDocument(HbConfig.class);
		return null;
	}

	@Override
	public void setConfig(HbConfig config) {
		ConfigDatabase db = conf.ensureDatabase("hb");
		Config c = db.findOne(HbConfig.class, null);
		if (c != null) {
			db.update(c, config);
		} else {
			db.add(config);
		}

		this.config = config;
	}

	private Connection getConnectionString() throws SQLException, ClassNotFoundException {
		Class.forName("com.mysql.jdbc.Driver");
		return DriverManager.getConnection("jdbc:mysql://" + config.getDbHost() + "/" + config.getDbName(),	config.getDbAccount(), config.getDbPassword());
	}

	private void close(PreparedStatement stmt, Connection con) {
		logger.trace("frodo hb auth: close sql jdbc");
		if (stmt != null)
			try {
				stmt.close();
			} catch (SQLException e) {
			}
		if (con != null)
			try {
				con.close();
			} catch (SQLException e) {
			}
	}

	private Map<String, Object> getLoginQueryResult(String loginName, String password, String hostName) {
		Connection con = null;
		PreparedStatement stmt = null;

		SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
		Map<String, Object> m = new HashMap<String, Object>();
		m.put("auth_code", 0);
		m.put("login_name", loginName);
		try {
			con = getConnectionString();
			String query = "SELECT IP, EMP_NO, PC_NAME, DEPT_NAME, aes_decrypt(unhex(EMP_PASS),'portablebranch') as EMP_PASS, STR_DT, END_DT, STR_TM, END_TM FROM "
					+ config.getTableName() + " WHERE EMP_NO = ? AND EMP_PASS = hex(aes_encrypt(?,?))";
			stmt = con.prepareStatement(query);
			stmt.setString(1, loginName);
			stmt.setString(2, password);
			stmt.setString(3, "portablebranch");
			ResultSet rs = stmt.executeQuery();

			if (!rs.next()) {
				logger.debug("frodo hb auth: cannot found user, login_name [{}]", loginName);
				m.put("auth_code", 1);
				return m;
			}

			m.put("org_unit_name", rs.getString("DEPT_NAME"));
			if (rs.getString("IP") != null)
				m.put("static_ip", rs.getString("IP"));

			if (!rs.getString("EMP_PASS").equals(password)) {
				logger.debug("frodo hb auth: password verify fail, login_name [{}]", loginName);
				m.put("auth_code", 2);
				return m;
			}

			if (!rs.getString("PC_NAME").equals(hostName)) {
				logger.debug("frodo hb auth: invalid host name [{}], login_name [{}]", hostName, loginName);
				m.put("auth_code", 20);
				return m;
			}

			Date now = new Date();
			if (rs.getString("STR_DT") != null) {
				Date startDate = sdf.parse(rs.getString("STR_DT"));
				if (startDate.after(now)) {
					logger.debug("frodo hb auth: invalid login date, login_name [{}]", loginName);
					m.put("auth_code", 7);
					return m;
				}
			}

			if (rs.getString("END_DT") != null) {
				Date endDate = sdf.parse(rs.getString("END_DT"));
				endDate.setTime(endDate.getTime() + (86400 * 1000));
				if (endDate.before(now)) {
					logger.debug("frodo hb auth: invalid login date, login_name [{}]", loginName);
					m.put("auth_code", 7);
					return m;
				}
			}

			if (rs.getString("STR_TM") != null) {
				if (getDate(rs.getString("STR_TM")).after(now)) {
					logger.debug("frodo hb auth: invalid login time, login_name [{}]", loginName);
					m.put("auth_code", 9);
					return m;
				}
			}
			if (rs.getString("END_TM") != null) {
				if (getDate(rs.getString("END_TM")).before(now)) {
					logger.debug("frodo hb auth: invalid login time, login_name [{}]", loginName);
					m.put("auth_code", 9);
					return m;
				}
			}

			return m;
		} catch (Throwable e) {
			throw new RuntimeException(e);
		} finally {
			close(stmt, con);
		}
	}

	private Date getDate(String date) {
		Calendar cal = Calendar.getInstance();
		String[] time = date.trim().split(":");

		cal.set(Calendar.HOUR_OF_DAY, Integer.valueOf(time[0]));
		cal.set(Calendar.MINUTE, Integer.valueOf(time[1]));
		cal.set(Calendar.SECOND, Integer.valueOf(time[2]));
		return cal.getTime();
	}

	@Override
	public void onOpen(Tunnel t) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onClose(Tunnel tunnel) {
		logger.info("frodo core: logout [{}]", tunnel);
		
		String loginName = tunnel.getLoginName();
		
		User user = domUserApi.getUser("localhost", loginName);
//		UserExtension userExtension = userApi.getUserExtension(user);
		
		String orgUnitName = user.getOrgUnit().getName();
		
		if (orgUnitName.equals("전산부서_서버접근")) {
			Date logoutDate = new Date();
			
			SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ssZ");
			
			Date loginDate = tunnel.getLoginDateTime();
			InetSocketAddress inetSocketAddress = tunnel.getRemoteAddress();
			String remoteAddress = inetSocketAddress.getAddress().getHostAddress();
			String remotePort = "" + inetSocketAddress.getPort();
			String userName = user.getName();
			
			logger.debug("loginName : " + loginName);
			logger.debug("userName : " + userName);
			logger.debug("orgUnitName : " + orgUnitName);
			logger.debug("loginDate : " + dateFormat.format(loginDate));
			logger.debug("logoutDate : " + dateFormat.format(logoutDate));
			logger.debug("remoteAddress : " + remoteAddress);
			logger.debug("remotePort : " + remotePort);
			
			Connection con = null;
			PreparedStatement stmt = null;
			
			String insertQuery = "INSERT INTO UserVpnAccessInfoTB (UserID, UserName, LoginTime, LogoutTime, AccessAddress, AccessPort, CreateDate) VALUES (?, ?, ?, ?, ?, ?, ?)";
			
			int insertCnt = 0;
			
			try {
				con = getMySqlConnectionString();
				
				stmt = con.prepareStatement(insertQuery);
				
				stmt.setString(1, loginName);
				stmt.setString(2, userName);
				stmt.setTimestamp(3, new java.sql.Timestamp(loginDate.getTime()));
				stmt.setTimestamp(4, new java.sql.Timestamp(logoutDate.getTime()));
				stmt.setString(5, remoteAddress);
				stmt.setString(6, remotePort);
				stmt.setTimestamp(7, getCurrentTimeStamp());
				
				insertCnt = stmt.executeUpdate();
				
			} catch (ClassNotFoundException e) {
				logger.error("frodo user hb: insert error", e);
			} catch (SQLException e) {
				logger.error("frodo user hb: insert error", e);
			} finally {
				mySqlclose(stmt, con);
			}
			
			if (insertCnt == 1) {
				logger.debug("User Access info insert success");
			} else {
				logger.debug("User Access info insert fail");
			}
		}
	}
	
	private static java.sql.Timestamp getCurrentTimeStamp() {
		java.util.Date today = new java.util.Date();
		return new java.sql.Timestamp(today.getTime());
	}

	@Override
	public void onAllClose() {
		// TODO Auto-generated method stub
		
	}
	
	private Connection getMySqlConnectionString() throws SQLException, ClassNotFoundException {
		Class.forName("com.mysql.jdbc.Driver");
		return DriverManager.getConnection("jdbc:mysql://127.0.0.1:3306/HanaBankDB", "root", "WeGuardia_01");
	}
	
	private void mySqlclose(PreparedStatement stmt, Connection con) {
		logger.trace("frodo hb auth: close sql jdbc");
		if (stmt != null)
			try {
				stmt.close();
			} catch (SQLException e) {
			}
		if (con != null)
			try {
				con.close();
			} catch (SQLException e) {
			}
	}
}
