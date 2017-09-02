package kr.co.future.sslvpn.userui.kps;

import java.sql.Connection;
import java.sql.Date;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import kr.co.future.sslvpn.model.UserExtension;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Invalidate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.apache.felix.ipojo.annotations.ServiceProperty;
import org.apache.felix.ipojo.annotations.Validate;
import kr.co.future.cron.CronService;
import kr.co.future.cron.Schedule;
import kr.co.future.dom.api.UserApi;
import kr.co.future.dom.model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(name = "frodo-userui-vpnuserinfo-update-batchjob")
@Provides
public class VPNUserInfoUpdateBatchJob implements Runnable {

	private Logger logger = LoggerFactory.getLogger(VPNUserInfoUpdateBatchJob.class);

	@ServiceProperty(name = "instance.name")
	private String instanceName;

	@Requires
	private CronService cronService;
	
	@Requires
	private UserApi domUserApi;
	
	@Requires
	private kr.co.future.sslvpn.model.api.UserApi userApi;

	@Validate
	public void validate() {
		Integer cronJobId = getCronJobId();
		if (cronJobId == null) {
			try {
				Schedule schedule = new Schedule.Builder(instanceName).build("0 4 * * *");
				cronService.registerSchedule(schedule);
			} catch (Exception e) {
				logger.error("frodo-user-ui-kps: server checker cron register failed");
			}
		}
	}

	@Invalidate
	public void invalidate() {
                Integer cronJobId = getCronJobId();
                if (cronJobId != null)
                    cronService.unregisterSchedule(cronJobId);
            }

        private Integer getCronJobId() {
            Map<Integer, Schedule> schedules = cronService.getSchedules();
            for (Integer id : schedules.keySet()) {
                Schedule schedule = schedules.get(id);
                if (schedule.getTaskName().equals(instanceName))
                    return id;
            }
            return null;
        }

        @Override
        public void run() {
            logger.info("frodo-user-ui-kps : Check Today Update User use vpn period");

            ArrayList<Map<String, Object>> todayUpdateUserList = getTodayUpdateUserInfoList();

            if (todayUpdateUserList != null) {
                logger.info("todayUpdateUserList size: " + todayUpdateUserList.size());

                for (int i = 0; i < todayUpdateUserList.size(); i++) {
                    logger.info("===================================================================");
                    Map<String, Object> updateUserInfo = todayUpdateUserList.get(i);

                    if (updateVpnUserUseDate(updateUserInfo)) {
                        updateVpnUserRequestPendingCheck((String)updateUserInfo.get("PERNR"));
                    }
                }
                logger.info("===================================================================");
            }
        }

        private ArrayList<Map<String, Object>> getTodayUpdateUserInfoList() {
            Connection con = null;
            PreparedStatement stmt = null;

            try {
                con = getConnectionString();

                String select_query = "select PERNR, START_DATE, END_DATE from KPS_VPN_USER_TB where REQUEST_PENDING = 'X' AND  START_DATE <= CURRENT_DATE() AND END_DATE >= CURRENT_DATE()";
                stmt = con.prepareStatement(select_query);

                ResultSet rs = stmt.executeQuery();

                ArrayList<Map<String, Object>> pendingUserList = new ArrayList<Map<String, Object>>();

                Map<String, Object> userInfo;

                while (rs.next()) {
                    userInfo = new HashMap<String, Object>();

                    userInfo.put("PERNR", rs.getInt("PERNR"));
                    userInfo.put("START_DATE", rs.getDate("START_DATE"));
                    userInfo.put("END_DATE", rs.getDate("END_DATE"));

                    pendingUserList.add(userInfo);
			} 
			
			return pendingUserList;
		} catch (ClassNotFoundException e1) {
			logger.error("frodo user kps ui: db error", e1);
		} catch (SQLException e1) {
			logger.error("frodo user kps ui: db error", e1);
		} finally {
			close(stmt, con);
		}
		
		return null;
	}
	
	private boolean updateVpnUserUseDate(Map<String, Object> userInfo) {
		try {
			User user = domUserApi.getUser("localhost", "" + userInfo.get("PERNR"));
			UserExtension userExtension = userApi.getUserExtension(user);
			
			logger.info(user.getLoginName() + ", " + user.getName());
			
			if(userExtension.isLocked()) {
				logger.info("user account set unlocked");
				userExtension.setLocked(false);
			}
			
			userExtension.setStartDateTime((Date) userInfo.get("START_DATE"));
			userExtension.setExpireDateTime(AccountManagerServlet.getExpireDate((String) userInfo.get("END_DATE")));
			userExtension.setUpdateDateTime(new java.util.Date());
			
			SimpleDateFormat format = new SimpleDateFormat("yyyy.MM.dd HH:mm:ss");
			logger.info("set startDate: " + format.format(userInfo.get("START_DATE")) + ", expireDateTime: " + format.format(AccountManagerServlet.getExpireDate((String) userInfo.get("END_DATE"))));
			
			userApi.setUserExtension(userExtension);
			
			logger.info("updateVpnUserUseDate return true");
			
			return true;
		} catch (Exception e) {
			logger.error("frodo user kps ui: db error", e);
			return false;
		}
	}
	
	private int updateVpnUserRequestPendingCheck(String pernr) {
		int userMaxDegree = getUserMaxRequestDegree(pernr);
		
		logger.info("PERNR: " + pernr + ", maxRequestDegree: " + userMaxDegree);
		
		Connection con = null;
		PreparedStatement stmt = null;
		
		int result = 0;
		
		try {
			con = getConnectionString();
			
			String update_query = "update KPS_VPN_USER_TB set REQUEST_PENDING = ? where PERNR = ? AND REQUEST_DEGREE = ?";
			stmt = con.prepareStatement(update_query);
			stmt.setString(1, null);
			stmt.setInt(2, Integer.parseInt(pernr));
			stmt.setInt(3, userMaxDegree);
			
			logger.info("set REQUEST_PENDING null");
			
			return result = stmt.executeUpdate();
		} catch (ClassNotFoundException e1) {
			logger.error("error:" + e1);
			return result;
		} catch (SQLException e1) {
			logger.error("error:" + e1);
			return result;
		} finally {
			close(stmt, con);
		}
	}
	
	private int getUserMaxRequestDegree(String pernr) {
		Connection con = null;
		PreparedStatement stmt = null;
		
		try {
			con = getConnectionString();
			
			String select_query = "select max(REQUEST_DEGREE) as MAX_DEGREE from KPS_VPN_USER_TB where PERNR = ?";
			stmt = con.prepareStatement(select_query);
			stmt.setInt(1, Integer.parseInt(pernr));
			
			ResultSet rs = stmt.executeQuery();
			
			if (rs.next()) {
				return rs.getInt("MAX_DEGREE");
			}
		} catch (ClassNotFoundException e1) {
			logger.error("frodo user kps ui: db error", e1);
			return -1;
		} catch (SQLException e1) {
			logger.error("frodo user kps ui: db error", e1);
			return -1;
		} finally {
			close(stmt, con);
		}
		
		return -1;
	}
	
	private Connection getConnectionString() throws SQLException, ClassNotFoundException {
		Class.forName("com.mysql.jdbc.Driver");
		return DriverManager.getConnection("jdbc:mysql://127.0.0.1:3306/KPSDB", "root", "WeGuardia_01");
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
