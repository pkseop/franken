package kr.co.future.sslvpn.auth.hb.impl;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import kr.co.future.cron.PeriodicJob;
import kr.co.future.sslvpn.auth.hb.HbAuthApi;
import kr.co.future.sslvpn.auth.hb.HbConfig;
import kr.co.future.sslvpn.core.xenics.XenicsService;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@PeriodicJob("*/5 * * * *")
@Provides
@Component(name = "hb-schedule-enforcer")
public class HbScheduleEnforcer implements Runnable {
	private final Logger logger = LoggerFactory.getLogger(HbScheduleEnforcer.class.getName());

	@Requires
	private HbAuthApi hbAuth;
	
	@Requires
	private XenicsService xenicsService;

	@Override
	public void run() {
		enforcer();
	}

	private void enforcer() {
		HbConfig config = hbAuth.getConfig();

		if (config == null)
			return;

		SimpleDateFormat sdf = new SimpleDateFormat("HH:dd:ss");
		String limitTime = sdf.format(new Date());
		List<String> loginNames = new ArrayList<String>();
		Connection con = null;
		PreparedStatement stmt = null;
		try {
			logger.trace("frodo hb auth: open sql jdbc");
			con = getConnectionString();
			String query = "SELECT EMP_NO FROM " + config.getTableName() + " WHERE END_TM <= ?";
			stmt = con.prepareStatement(query);
			stmt.setString(1, limitTime);
			ResultSet rs = stmt.executeQuery();

			while (rs.next()) {
				loginNames.add(rs.getString("EMP_NO"));
			}
		} catch (Throwable e) {
			logger.trace("frodo hb auth: cannot find time over user", e);
			throw new RuntimeException(e);
		} finally {
			close(stmt, con);
		}

		if (loginNames.size() > 0) {
			for(String loginName : loginNames) {
				if(xenicsService.isAlreadyLoggedin(loginName)) {
					logger.info("frodo hb auth: kill tunnel of login name [{}]", loginName);
					xenicsService.execkillTunnel(xenicsService.getClientId(loginName));
				}
			}
		}
	}

	private Connection getConnectionString() throws SQLException, ClassNotFoundException {
		HbConfig config = hbAuth.getConfig();
		Class.forName("com.mysql.jdbc.Driver");
		return DriverManager.getConnection("jdbc:mysql://" + config.getDbHost() + "/" + config.getDbName(),
				config.getDbAccount(), config.getDbPassword());
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

}
