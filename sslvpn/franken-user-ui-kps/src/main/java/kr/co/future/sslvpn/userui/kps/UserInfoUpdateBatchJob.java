package kr.co.future.sslvpn.userui.kps;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
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

@Component(name = "frodo-userui-userinfo-update-batchjob")
@Provides
public class UserInfoUpdateBatchJob implements Runnable {

	private Logger logger = LoggerFactory.getLogger(UserInfoUpdateBatchJob.class);

	@ServiceProperty(name = "instance.name")
	private String instanceName;

	@Requires
	private CronService cronService;
	
	@Requires
	private UserApi domUserApi;
	
	@Requires
	private kr.co.future.sslvpn.model.api.UserApi userApi;
	
	private static final String PERSD_CODE_1 = "1";
	private static final String PERSD_CODE_2 = "2";
	private static final String PERSD_CODE_3 = "3";

	@Validate
	public void validate() {
		Integer cronJobId = getCronJobId();
		if (cronJobId == null) {
			try {
				Schedule schedule = new Schedule.Builder(instanceName).build("0 3 * * *");
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
		logger.info("frodo-user-ui-kps : Check Today Update User info");
		
		int updateCount = setNullOldOrgehFromOrgeh();
		
		logger.info("setNullOldOrgehFromOrgeh update count : " + updateCount);
		
		ArrayList<Map<String, Object>> todayUpdateUserList = getTodayUpdateUserInfoList();
		
		if (todayUpdateUserList != null) {
			logger.info("todayUpdateUserList size: " + todayUpdateUserList.size());
			for (int i = 0; i < todayUpdateUserList.size(); i++) {
				logger.info("===================================================================");
				Map<String, Object> updateUserInfo = todayUpdateUserList.get(i);
				updateVpnUserInfo(updateUserInfo);
			}
			logger.info("===================================================================");
		}
	}
	
	private boolean updateVpnUserInfo(Map<String, Object> userInfo) {
		try {
			User user = domUserApi.getUser("localhost", "" + (Integer) userInfo.get("PERNR"));
			UserExtension userExtension = userApi.getUserExtension(user);
			
			user.setName((String)userInfo.get("ENAME"));
			user.setEmail((String)userInfo.get("EMAIL"));
			
			int orgeh = (Integer)userInfo.get("ORGEH");
			int oldOrgeh = (Integer)userInfo.get("OLD_ORGEH");

			String pernr = "" + (Integer)userInfo.get("PERNR");
			
			logger.info(pernr + ", " + user.getName() + ", "  + user.getEmail() + ", " + orgeh + ", " + oldOrgeh);
			
			if (orgeh != oldOrgeh) {
				//최신 업데이트된 조직코드를 올드조직코드로 업데이트 한다.
				int result = updateUserOldOrgehCode(pernr, orgeh);
				//계정을 잠근다.
				
				logger.info("updateUserOldOrgehCode result : " + result);
				
				if (result == 1) {
					logger.info("user account locked");
					userExtension.setLocked(true);
				}
			}
			
			String persg = (String)userInfo.get("PERSG");
			
			logger.info("persg : " + persg);
			
			if (persg.equals(PERSD_CODE_1)) {
				if (userExtension.isLocked()) {
					logger.info("user account unlocked");
					userExtension.setLocked(false);
				}
			} else if (persg.equals(PERSD_CODE_2)) {
				if (!userExtension.isLocked()) {
					logger.info("user account locked");
					userExtension.setLocked(true);
				}
			} else if (persg.equals(PERSD_CODE_3)) {
				logger.info((Integer) userInfo.get("PERNR") + " delete");
				domUserApi.removeUser("localhost", "" + (Integer) userInfo.get("PERNR"));
				return true;
			}

			userExtension.setUpdateDateTime(new java.util.Date());

			userApi.setUserExtension(userExtension);
			
			return true;
		} catch (Exception e) {
			logger.error("error : ", e);
			return false;
		}
	}
	
	private int updateUserOldOrgehCode(String pernr, int oldOrgeh) {
		Connection con = null;
		PreparedStatement stmt = null;
		
		int result = 0;
		
		try {
			con = getConnectionString();
			
			String update_query = "update KPS_USER_TB set OLD_ORGEH = ? where PERNR = ?";
			stmt = con.prepareStatement(update_query);
			stmt.setInt(1, oldOrgeh);
			stmt.setInt(2, Integer.parseInt(pernr));
			
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
	
	private ArrayList<Map<String, Object>> getTodayUpdateUserInfoList() {
		Connection con = null;
		PreparedStatement stmt = null;
		
		try {
			con = getConnectionString();
			
			String select_query = "select A.PERNR, ENAME, EMAIL, ORGEH, OLD_ORGEH, PERSG from KPS_USER_TB A, KPS_VPN_USER_TB B where A.PERNR = B.PERNR AND B.DECISION_STATUS = 4 AND XI_ACT_DATE = CURRENT_DATE()";
			stmt = con.prepareStatement(select_query);
			
			ResultSet rs = stmt.executeQuery();
			
			ArrayList<Map<String, Object>> updateUserList = new ArrayList<Map<String, Object>>();
			
			Map<String, Object> userInfo;
			
			while (rs.next()) {
				userInfo = new HashMap<String, Object>();
				
				userInfo.put("PERNR", rs.getInt("PERNR"));
				userInfo.put("ENAME", rs.getString("ENAME"));
				userInfo.put("EMAIL", rs.getString("EMAIL"));
				userInfo.put("ORGEH", rs.getInt("ORGEH"));
				userInfo.put("OLD_ORGEH", rs.getInt("OLD_ORGEH"));
				userInfo.put("PERSG", rs.getString("PERSG"));
				
				updateUserList.add(userInfo);
			} 
			
			return updateUserList;
		} catch (ClassNotFoundException e1) {
			logger.error("frodo user kps ui: db error", e1);
		} catch (SQLException e1) {
			logger.error("frodo user kps ui: db error", e1);
		} finally {
			close(stmt, con);
		}
		
		return null;
	}
	
	private int setNullOldOrgehFromOrgeh() {
		Connection con = null;
		PreparedStatement stmt = null;
		
		int updateCount= 0;
		
		try {
			con = getConnectionString();
			
			String selectQuery = "select PERNR, ORGEH from KPS_USER_TB where OLD_ORGEH is null";
			stmt = con.prepareStatement(selectQuery);
			
			ResultSet rs = stmt.executeQuery();
			
			while (rs.next()) {
				updateUserOldOrgehCode(""+rs.getInt("PERNR"), +rs.getInt("ORGEH"));
				updateCount++;
			}
		} catch (ClassNotFoundException e1) {
			logger.error("frodo user kps ui: db error", e1);
		} catch (SQLException e1) {
			logger.error("frodo user kps ui: db error", e1);
		} finally {
			close(stmt, con);
		}
		
		return updateCount;
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
