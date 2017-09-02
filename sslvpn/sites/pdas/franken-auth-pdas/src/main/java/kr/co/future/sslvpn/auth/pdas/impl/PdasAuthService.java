package kr.co.future.sslvpn.auth.pdas.impl;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import kr.co.future.sslvpn.auth.BaseExternalAuthApi;
import kr.co.future.sslvpn.auth.ExternalAuthApi;
import kr.co.future.sslvpn.auth.pdas.PdasAuthApi;
import kr.co.future.sslvpn.auth.pdas.PdasConfig;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.apache.felix.ipojo.annotations.Validate;

import kr.co.future.confdb.Config;
import kr.co.future.confdb.ConfigDatabase;
import kr.co.future.confdb.ConfigService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(name = "pdas-auth-service")
@Provides
public class PdasAuthService extends BaseExternalAuthApi implements PdasAuthApi {

	private final Logger logger = LoggerFactory.getLogger(PdasAuthService.class.getName());

	@Requires
	private ConfigService conf;

	private PdasConfig config;

	public static void main(String[] args) {
		System.out.println(now());
	}

	@Validate
	public void start() {
		config = getConfig();
	}

	@Override
	public Object login(Map<String, Object> props) {
		String loginName = (String) props.get("id");
		String password = (String) props.get("pw");

		Map<String, Object> m = new HashMap<String, Object>();
		boolean ret = verifyPassword(loginName, password);
		if (ret) {
			m.put("name", loginName);
			m.put("auth_code", 0);
			logger.trace("frodo pdas auth: login [{}] success", loginName);
		} else {
			m.put("auth_code", 2); // password fail
			logger.trace("frodo pdas auth: login [{}] fail", loginName);
		}

		return m;
	}

	@Override
	public boolean verifyPassword(String loginName, String password) {
		if (config == null) {
			logger.warn("frodo pdas auth: config not set");
			return false;
		}

		logger.trace("frodo pdas auth: trying to verify [{}]'s password", loginName);

		Connection con = null;
		PreparedStatement stmt = null;
		try {
			int epoch = now();
			logger.trace("frodo pdas auth: open sql jdbc");
			con = getConnectionString();
			String query = "SELECT COUNT(*) FROM " + config.getTableName()
					+ " WHERE ID = ? AND PWD = ? AND STARTTIME <= ? AND ENDTIME >= ?";
			stmt = con.prepareStatement(query);
			stmt.setString(1, loginName);
			stmt.setString(2, password);
			stmt.setInt(3, epoch);
			stmt.setInt(4, epoch);
			ResultSet rs = stmt.executeQuery();
			if (rs.next())
				return rs.getInt(1) > 0;
			return false;
		} catch (Throwable e) {
			logger.trace("frodo pdas auth: cannot verify password", e);
			throw new RuntimeException(e);
		} finally {
			close(stmt, con);
		}
	}

	private Connection getConnectionString() throws SQLException, ClassNotFoundException {
		Class.forName("com.mysql.jdbc.Driver");
		return DriverManager.getConnection("jdbc:mysql://" + config.getDbHost() + "/" + config.getDbName(),
				config.getDbAccount(), config.getDbPassword());
	}

	private void close(PreparedStatement stmt, Connection con) {
		logger.trace("frodo pdas auth: close sql jdbc");
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

	@Override
	public String getIdn(String loginName) {
		return null;
	}

	@Override
	public Map<String, Object> verifyUser(String loginName) {
		Map<String, Object> m = new HashMap<String, Object>();
		m.put("name", loginName);
		m.put("org_unit_name", null);
		m.put("result", true);
		return m;
	}

	@Override
	public PdasConfig getConfig() {
		ConfigDatabase db = conf.ensureDatabase("pdas");
		Config c = db.findOne(PdasConfig.class, null);
		if (c != null)
			return c.getDocument(PdasConfig.class);
		return null;
	}

	@Override
	public void setConfig(PdasConfig config) {
		ConfigDatabase db = conf.ensureDatabase("pdas");
		Config c = db.findOne(PdasConfig.class, null);
		if (c != null) {
			db.update(c, config);
		} else {
			db.add(config);
		}

		this.config = config;
	}

	private static int now() {
		Date now = new Date();

		Calendar c = Calendar.getInstance();
		c.set(Calendar.ZONE_OFFSET, 0);
		c.set(Calendar.YEAR, 1970);
		c.set(Calendar.MONTH, 1);
		c.set(Calendar.DAY_OF_YEAR, 1);
		c.set(Calendar.HOUR_OF_DAY, 0);
		c.set(Calendar.MINUTE, 0);
		c.set(Calendar.SECOND, 0);
		c.set(Calendar.MILLISECOND, 0);

		return (int) ((now.getTime() - c.getTime().getTime()) / 1000);
	}

	@Override
	public String getSubjectDn(String loginName) {
		throw new UnsupportedOperationException("subject dn not supported");
	}

	@Override
	public boolean isPasswordChangeSupported() {
		return false;
	}

	@Override
	public void changePassword(String account, String newPassword) {
		throw new UnsupportedOperationException("cannot change password");
	}

}
