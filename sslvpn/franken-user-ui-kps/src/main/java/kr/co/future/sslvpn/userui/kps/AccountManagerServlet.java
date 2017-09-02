package kr.co.future.sslvpn.userui.kps;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.URL;
import java.nio.charset.Charset;
import java.sql.Connection;
import java.sql.Date;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMessage.RecipientType;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import kr.co.future.sslvpn.core.ActivationListener;
import kr.co.future.sslvpn.model.UserExtension;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Invalidate;
import org.apache.felix.ipojo.annotations.Requires;
import org.apache.felix.ipojo.annotations.Validate;
import org.json.JSONConverter;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;
import kr.co.future.codec.Base64;
import kr.co.future.dom.api.OrganizationUnitApi;
import kr.co.future.dom.api.UserApi;
import kr.co.future.dom.model.OrganizationUnit;
import kr.co.future.dom.model.User;
import kr.co.future.httpd.HttpContext;
import kr.co.future.httpd.HttpService;
import kr.co.future.httpd.MimeTypes;
import kr.co.future.mail.MailerConfig;
import kr.co.future.mail.MailerRegistry;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(name = "frodo-userui-loader-kps")
public class AccountManagerServlet extends HttpServlet implements ActivationListener {
	private static final long serialVersionUID = 1L;
	private final Logger logger = LoggerFactory.getLogger(AccountManagerServlet.class.getName());

	private static final String RESULT = "Result";
	private static final String RESULT_SUCCESS = "Success";
	private static final String RESULT_FAIL = "Fail";
	private static final String RESULT_MESSAGE = "ResultMessage";
	
	private static final String RESULT_USER_NOT_EXIST = "can not find user. PERNR : ";
	private static final String RESULT_PERNR_PARA_IS_NULL = "PERNR parameter is null. must set PERNR value!";
	private static final String RESULT_VPN_ACCOUNT_REQUEST_SUCCESS = "vpn user account request success";
	private static final String RESULT_VPN_ACCOUNT_REQUEST_FAIL = "vpn user account request fail";
	private static final String RESULT_VPN_ACCOUNT_REQUEST_PROGRESS = "VPN 계정 신청 중에 있습니다.";
	private static final String RESULT_VPN_DECISION_REQUEST_SUCCESS = "vpn user account request decison success";
	private static final String RESULT_VPN_DECISION_REQUEST_APPROVE = "VPN 계정 신청에 대한 요청을 결재 처리하였습니다.";
	private static final String RESULT_VPN_DECISION_REQUEST_REJECT = "VPN 계정 신청에 대한 요청을 반려 처리 하였습니다.";
	private static final String RESULT_VPN_DECISION_REQUEST_DUPLICATION = "VPN 계정 신청에 대한 요청을 이미 처리 하였습니다.";
	private static final String RESULT_VPN_DECISION_REQUEST_FAIL = "vpn user account request decision fail";
	
	private static final String RESULT_VPN_SECMANAGER_REQUEST_SUCCESS = "vpn secmanager change success";
	private static final String RESULT_VPN_SECMANAGER_REQUEST_FAIL = "vpn secmanager change fail";
	
	private static final int DECISION_STATUS_REQUEST = 0;
	private static final int DECISION_STATUS_1ST = 1;
	private static final int DECISION_STATUS_2ND = 2;
	private static final int DECISION_STATUS_3RD = 3;
	private static final int DECISION_STATUS_4TH = 4;
	private static final int DECISION_REJECT_STATUS_1ST = 5;
	private static final int DECISION_REJECT_STATUS_2ND = 6;
	private static final int DECISION_REJECT_STATUS_3RD = 7;
	private static final int DECISION_REJECT_STATUS_4TH = 8;
	
	private static final String DECISION_PENDING_CHECK = "X";
	
	private static final String RESULT_APPROVE = "approve";
	private static final String RESULT_REJECT = "reject";
	
	private static final String MAIL_SUBJECT = "VPN 계정 신청 승인 요청";
	private static final String MAIL_APPROVE_SUBJECT = "VPN 사용 승인 완료";
	
	private static final String INIT_PASSWD = "kpsvpn12#";
    private static final String PASSWD_INFO = "기존 패스워드 사용";
	
	@Requires
	private HttpService httpd;
	private BundleContext bc;
	
	@Requires
	private OrganizationUnitApi orgUnitApi;
	
	@Requires
	private UserApi domUserApi;
	
	@Requires
	private MailerRegistry mailer;
	
	@Requires
	private kr.co.future.sslvpn.model.api.UserApi userApi;
	
	private ExecutorService threadPool;

	public AccountManagerServlet(BundleContext bc) {
		this.bc = bc;
	}

	@Validate
	public void start() {
		HttpContext ctx = httpd.ensureContext("frodo");
		ctx.addServlet("account", this, "/vpn/*");
		threadPool = Executors.newCachedThreadPool();
	}

	@Invalidate
	public void stop() {
		threadPool.shutdown();
		
		if (httpd != null) {
			HttpContext ctx = httpd.ensureContext("frodo");
			ctx.removeServlet("account");
		}
	}

	@Override
	public void onActivated() {
		logger.trace("frodo core: adding account management page to /vpn/*");
		HttpContext ctx = httpd.ensureContext("frodo");
		ctx.addServlet("account", this, "/vpn/*");
	}

	@SuppressWarnings("resource")
	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		String host = req.getHeader("Host");
		String path = req.getPathInfo();
		logger.trace("frodo user kps ui: get host [{}] path [{}]", host, path);
		
		PrintWriter out = null;
		
		if (path.equals("/getUserInfo")) {
			setHttpServletResponseHeader(resp);
			
			out = resp.getWriter();
			
			String pernr = req.getParameter("applicantPernr");
			
			logger.debug("recevice PERNR : " + pernr);
			
			Map<String, Object> returnJsonMsg = new HashMap<String, Object>();
			
			if (pernr == null || pernr.length() == 0) {
				returnJsonMsg.put(RESULT, RESULT_FAIL);
				returnJsonMsg.put(RESULT_MESSAGE, RESULT_PERNR_PARA_IS_NULL);
				
				jsonConverter(out, resp, returnJsonMsg);
				
				return;
			}
			
			Connection con = null;
			PreparedStatement stmt = null;
			
			try {
				con = getConnectionString();
				
				String select_query = "SELECT PERNR, ENAME, CONCAT(CONCAT((SELECT ORGTX FROM KPS_ORGEH_TB WHERE ORGEH = (SELECT PORGEH FROM KPS_ORGEH_TB WHERE ORGEH = A.ORGEH)),' '), (select ORGTX from KPS_ORGEH_TB where ORGEH = A.ORGEH)) AS ORGTXT, JIKWI FROM KPS_USER_TB A WHERE PERNR = ?";
				stmt = con.prepareStatement(select_query);
				stmt.setInt(1, Integer.parseInt(pernr));
				
				ResultSet rs = stmt.executeQuery();
				
				if (rs.next()) {
					returnJsonMsg.put(RESULT, RESULT_SUCCESS);
					returnJsonMsg.put("PERNR", "" + rs.getInt("PERNR"));
					returnJsonMsg.put("ENAME", rs.getString("ENAME"));
					returnJsonMsg.put("ORGTXT", rs.getString("ORGTXT"));
					returnJsonMsg.put("JIKWI", rs.getString("JIKWI"));

                    select_query = "SELECT PERNR, ENAME FROM KPS_USER_TB WHERE ORGEH = (SELECT ORGEH FROM KPS_USER_TB WHERE PERNR = ?) AND REDER_CODE = 'X'";

                    stmt = con.prepareStatement(select_query);
                    stmt.setInt(1, Integer.parseInt(pernr));

                    rs = stmt.executeQuery();

                    if (rs.next()) {
                        String grantPernr = "" + rs.getInt("PERNR");

                        if (returnJsonMsg.get("PERNR").equals(grantPernr)) {
                            Connection con2 = getConnectionString();
                            PreparedStatement stmt2 = null;

                            select_query = "select PERNR, ENAME from KPS_USER_TB where SECMANAGER = 'X'";
                            stmt2 = con2.prepareStatement(select_query);

                            ResultSet rs2 = stmt2.executeQuery();

                            if (rs2.next()) {
                                returnJsonMsg.put("GRANT_PERNR", rs2.getInt("PERNR"));
                                returnJsonMsg.put("GRANT_ENAME", rs2.getString("ENAME"));
                            } else {
                                returnJsonMsg.put("GRANT_PERNR", "결재자 정보를 찾을 수 없습니다. VPN 담당자에게 문의 바랍니다.");
                                returnJsonMsg.put("GRANT_ENAME", "");
                            }
                        } else {
                            returnJsonMsg.put("GRANT_PERNR", grantPernr);
                            returnJsonMsg.put("GRANT_ENAME", rs.getString("ENAME"));
                        }
                    } else {
                        returnJsonMsg.put("GRANT_PERNR", "결재자 정보를 찾을 수 없습니다. VPN 담당자에게 문의 바랍니다.");
                        returnJsonMsg.put("GRANT_ENAME", "");
                    }
				} else {
					returnJsonMsg.put(RESULT, RESULT_FAIL);
					returnJsonMsg.put(RESULT_MESSAGE, RESULT_USER_NOT_EXIST + pernr);
				}

                jsonConverter(out, resp, returnJsonMsg);
				
				return;
			} catch (ClassNotFoundException e1) {
				logger.debug("frodo user kps ui: cannot convert object to json string", e1);
				resp.sendError(500);
			} catch (SQLException e1) {
				logger.debug("frodo user kps ui: cannot convert object to json string", e1);
				resp.sendError(500);
			} finally {
				close(stmt, con);
			}
			
		}
		
		if (path.equals("/getVpnUserSimpleInfo")) {
			setHttpServletResponseHeader(resp);
			
			out = resp.getWriter();
			
			String applicantPernr = req.getParameter("applicantPernr");
			
			logger.debug("recevice applicantPernr : " + applicantPernr);
			
			Map<String, Object> returnJsonMsg = new HashMap<String, Object>();
			
			if (applicantPernr == null || applicantPernr.length() == 0) {
				returnJsonMsg.put(RESULT, RESULT_FAIL);
				returnJsonMsg.put(RESULT_MESSAGE, RESULT_PERNR_PARA_IS_NULL);
				
				jsonConverter(out, resp, returnJsonMsg);
				
				return;
			}
			
			int userMaxDegree = getUserMaxRequestDegree(applicantPernr);
			
			Connection con = null;
			PreparedStatement stmt = null;
			
			try {
				con = getConnectionString();
				
				String select_query = "select A.PERNR, A.ENAME, CONCAT(CONCAT((SELECT ORGTX FROM KPS_ORGEH_TB WHERE ORGEH = (SELECT PORGEH FROM KPS_ORGEH_TB WHERE ORGEH = A.ORGEH)),' '), (select ORGTX from KPS_ORGEH_TB where ORGEH = A.ORGEH)) AS ORGTXT, B.START_DATE, B.END_DATE, B.REQUEST_REASON, B.RELEVANT_BASE, B.DECISION_STATUS "
						+ "from KPS_USER_TB A, KPS_VPN_USER_TB B where A.PERNR = ? and A.PERNR = B.PERNR AND B.REQUEST_DEGREE = ?";
				stmt = con.prepareStatement(select_query);
				stmt.setInt(1, Integer.parseInt(applicantPernr));
				stmt.setInt(2, userMaxDegree);
				
				ResultSet rs = stmt.executeQuery();
				
				if (rs.next()) {
					Map<String, Object> applicantReqInfo = new HashMap<String, Object>();
					
					returnJsonMsg.put(RESULT, RESULT_SUCCESS);
					applicantReqInfo.put("PERNR", "" + rs.getInt("PERNR"));
					applicantReqInfo.put("ENAME", rs.getString("ENAME"));
					applicantReqInfo.put("ORGTXT", rs.getString("ORGTXT"));
					
					SimpleDateFormat format = new SimpleDateFormat("yyyy.MM.dd");

					Date startDate = rs.getDate("START_DATE");
					applicantReqInfo.put("START_DATE", format.format(startDate));

					Date endDate = rs.getDate("END_DATE");
					applicantReqInfo.put("END_DATE", format.format(endDate));

					applicantReqInfo.put("REQUEST_REASON", rs.getString("REQUEST_REASON"));
					applicantReqInfo.put("RELEVANT_BASE", rs.getString("RELEVANT_BASE"));
					
					int approveStatus = rs.getInt("DECISION_STATUS");
					
					int decisionMakerPernr = findDicisionMaker(applicantPernr, approveStatus);
					
					applicantReqInfo.put("DECISION_MAKER_PERNR", "" + decisionMakerPernr);
					
					returnJsonMsg.put("Data", applicantReqInfo);
				} else {
					returnJsonMsg.put(RESULT, RESULT_FAIL);
					returnJsonMsg.put(RESULT_MESSAGE, RESULT_USER_NOT_EXIST + applicantPernr);
				}
				
				jsonConverter(out, resp, returnJsonMsg);
				
				return;
			} catch (ClassNotFoundException e1) {
				logger.debug("frodo user kps ui: cannot convert object to json string", e1);
				resp.sendError(500);
			} catch (SQLException e1) {
				logger.debug("frodo user kps ui: cannot convert object to json string", e1);
				resp.sendError(500);
			} finally {
				close(stmt, con);
			}
			
		}
		
		if(path.equals("/searchVpnUserList")) {			
			searchVpnUserList(req, resp);
			return;
		}
		
		if (path.equals("/getVpnUserList")) {
			setHttpServletResponseHeader(resp);
			
			out = resp.getWriter();
			
			String pernr = req.getParameter("PERNR");
			
			logger.debug("recevice PERNR : " + pernr);
			
			Map<String, Object> returnJsonMsg = new HashMap<String, Object>();
			
			if (pernr == null || pernr.length() == 0) {
				returnJsonMsg.put(RESULT, RESULT_FAIL);
				returnJsonMsg.put(RESULT_MESSAGE, RESULT_PERNR_PARA_IS_NULL);
				
				jsonConverter(out, resp, returnJsonMsg);
				
				return;
			}
			
			String select_query = "select A.PERNR, A.ENAME, CONCAT(CONCAT((SELECT ORGTX FROM KPS_ORGEH_TB WHERE ORGEH = (SELECT PORGEH FROM KPS_ORGEH_TB WHERE ORGEH = A.ORGEH)),' '), (select ORGTX from KPS_ORGEH_TB where ORGEH = A.ORGEH)) AS ORGTXT, A.JIKWINM, B.REQUEST_DEGREE, B.REQUEST_DATE, B.DECISION_STATUS "
					+ "from KPS_USER_TB A, KPS_VPN_USER_TB B where A.PERNR = B.PERNR order by B.REQUEST_DATE desc"; //신청리자 순 정렬.
			//for restful service.
			int[] range = readRange(req);
			if(range != null) {
				//인자 Range 값의 두번째는 인덱스 번호지만 sql에서 사용되야할 값은 row의 개수 이므로. 
				int count = range[1] - range[0] + 1;
				select_query += " limit " + range[0] + ", " + count;
				int totalCount = selectCountOfVpnUserList(resp);
				if(totalCount == -1)
					return;
				String contentRange = String.format("items %d-%d/%d", range[0], range[1], totalCount);
				resp.setHeader("Content-Range", contentRange);
			}
			
			logger.debug("frodo user kps ui: getVpnUserList query. [{}]", select_query);
			
			ArrayList<Map<String, Object>> vpnUserList = new ArrayList<Map<String, Object>>();
			retrieveVpnUserList(resp, select_query, vpnUserList);
			jsonConverter(out, resp, vpnUserList);
			
			return;
		}
		
		if (path.equals("/getUserList")) {
			setHttpServletResponseHeader(resp);
			
			out = resp.getWriter();
			
			Map<String, Object> returnJsonMsg = new HashMap<String, Object>();
			
			Connection con = null;
			PreparedStatement stmt = null;
			
			try {
				con = getConnectionString();
				
				String select_query = "select PERNR, ENAME, CONCAT(CONCAT((SELECT ORGTX FROM KPS_ORGEH_TB WHERE ORGEH = (SELECT PORGEH FROM KPS_ORGEH_TB WHERE ORGEH = A.ORGEH)),' '), (select ORGTX from KPS_ORGEH_TB where ORGEH = A.ORGEH)) AS ORGTXT, JIKWINM, SECMANAGER from KPS_USER_TB A order by PERNR desc";
				stmt = con.prepareStatement(select_query);
				
				ResultSet rs = stmt.executeQuery();
				
				ArrayList<Map<String, Object>> vpnUserList = new ArrayList<Map<String, Object>>();
				
				Map<String, Object> userInfo;
				
				while (rs.next()) {
					userInfo = new HashMap<String, Object>();
					
					int applicantPernr = rs.getInt("PERNR");
					userInfo.put("PERNR", "" + applicantPernr);
					userInfo.put("ENAME", rs.getString("ENAME"));
					userInfo.put("ORGTXT", rs.getString("ORGTXT"));
					userInfo.put("JIKWINM", rs.getString("JIKWINM"));
					userInfo.put("SECMANAGER", rs.getString("SECMANAGER"));
					
					vpnUserList.add(userInfo);
				} 
				
				if (vpnUserList.size() > 0) {
					returnJsonMsg.put(RESULT, RESULT_SUCCESS);
					returnJsonMsg.put("Data", vpnUserList);
				} else {
					returnJsonMsg.put(RESULT, RESULT_FAIL);
					returnJsonMsg.put(RESULT_MESSAGE, RESULT_USER_NOT_EXIST);
				}
				
				jsonConverter(out, resp, returnJsonMsg);
				
				return;
			} catch (ClassNotFoundException e1) {
				logger.debug("frodo user kps ui: cannot convert object to json string", e1);
				resp.sendError(500);
			} catch (SQLException e1) {
				logger.debug("frodo user kps ui: cannot convert object to json string", e1);
				resp.sendError(500);
			} finally {
				close(stmt, con);
			}
		}
		
		if (path.equals("/setSecManager")) {
			setHttpServletResponseHeader(resp);
			
			out = resp.getWriter();
			
			String pernr = req.getParameter("PERNR");
			
			logger.debug("recevice PERNR : " + pernr);
			
			Map<String, Object> returnJsonMsg = new HashMap<String, Object>();
			
			if (pernr == null || pernr.length() == 0) {
				returnJsonMsg.put(RESULT, RESULT_FAIL);
				returnJsonMsg.put(RESULT_MESSAGE, RESULT_PERNR_PARA_IS_NULL);
				
				jsonConverter(out, resp, returnJsonMsg);
				
				return;
			}
			
			int updateCnt = 0;
			
			String existSecManager = getExistSecManager();
			
			if (existSecManager != null) {
				updateCnt = updateSecManagerInfo(existSecManager, null);
			} else {
				updateCnt = 1;
			}
			
			if (updateCnt == 1) {
				updateCnt = updateSecManagerInfo(pernr, "X");
				
				if (updateCnt == 1) {
					updateCnt = updateSecManagerInfo(pernr, "X");
				} else {
					if (existSecManager != null)
						updateCnt = updateSecManagerInfo(existSecManager, "X");
				}
			}
			
			if (updateCnt == 1) {
				returnJsonMsg.put(RESULT, RESULT_SUCCESS);
				returnJsonMsg.put(RESULT_MESSAGE, RESULT_VPN_SECMANAGER_REQUEST_SUCCESS);
			} else {
				returnJsonMsg.put(RESULT, RESULT_FAIL);
				returnJsonMsg.put(RESULT_MESSAGE, RESULT_VPN_SECMANAGER_REQUEST_FAIL);
			}
			
			try {
				logger.debug("" + JSONConverter.jsonize(returnJsonMsg));
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			jsonConverter(out, resp, returnJsonMsg);
			
			return;
		}
		
		if (path.equals("/getVpnUserInfo")) {
			setHttpServletResponseHeader(resp);
			
			out = resp.getWriter();
			
			String pernr = req.getParameter("PERNR");
			String reqDegree = req.getParameter("DEGREE");
			
			logger.debug("recevice PERNR : " + pernr + " reqDegree : " + reqDegree);
			
			Map<String, Object> returnJsonMsg = new HashMap<String, Object>();
			
			if (pernr == null || pernr.length() == 0) {
				returnJsonMsg.put(RESULT, RESULT_FAIL);
				returnJsonMsg.put(RESULT_MESSAGE, RESULT_PERNR_PARA_IS_NULL);
				
				jsonConverter(out, resp, returnJsonMsg);
				
				return;
			}
			
			Connection con = null;
			PreparedStatement stmt = null;
			
			try {
				con = getConnectionString();
				
				String select_query = "select A.PERNR, A.ENAME, CONCAT(CONCAT((SELECT ORGTX FROM KPS_ORGEH_TB WHERE ORGEH = (SELECT PORGEH FROM KPS_ORGEH_TB WHERE ORGEH = A.ORGEH)),' '), (select ORGTX from KPS_ORGEH_TB where ORGEH = A.ORGEH)) AS ORGTXT, B.START_DATE, B.END_DATE, B.REQUEST_REASON, B.RELEVANT_BASE, B.DECISION_STATUS, "
						+ "(select CONCAT(CONCAT((SELECT ORGTX FROM KPS_ORGEH_TB WHERE ORGEH = (SELECT PORGEH FROM KPS_ORGEH_TB WHERE ORGEH = A.ORGEH)),' '), (select ORGTX from KPS_ORGEH_TB where ORGEH = A.ORGEH)) AS ORGTXT from KPS_USER_TB A where PERNR = B.DECISION_1ST_PERNR) as DECISION_1ST_ORGTXT, B.DECISION_1ST_RESULT, "
						+ "(select ENAME from KPS_USER_TB where PERNR = B.DECISION_1ST_PERNR) as DECISION_1ST_ENAME, "
						+ "(select JIKWINM from KPS_USER_TB where PERNR = B.DECISION_1ST_PERNR) as DECISION_1ST_JIKWINM, B.DECISION_1ST_DATE, "
						+ "(select CONCAT(CONCAT((SELECT ORGTX FROM KPS_ORGEH_TB WHERE ORGEH = (SELECT PORGEH FROM KPS_ORGEH_TB WHERE ORGEH = A.ORGEH)),' '), (select ORGTX from KPS_ORGEH_TB where ORGEH = A.ORGEH)) AS ORGTXT from KPS_USER_TB A where PERNR = B.DECISION_2ND_PERNR) as DECISION_2ND_ORGTXT, B.DECISION_2ND_RESULT, "
						+ "(select ENAME from KPS_USER_TB where PERNR = B.DECISION_2ND_PERNR) as DECISION_2ND_ENAME, "
						+ "(select JIKWINM from KPS_USER_TB where PERNR = B.DECISION_2ND_PERNR) as DECISION_2ND_JIKWINM, B.DECISION_2ND_DATE, "
						+ "(select CONCAT(CONCAT((SELECT ORGTX FROM KPS_ORGEH_TB WHERE ORGEH = (SELECT PORGEH FROM KPS_ORGEH_TB WHERE ORGEH = A.ORGEH)),' '), (select ORGTX from KPS_ORGEH_TB where ORGEH = A.ORGEH)) AS ORGTXT from KPS_USER_TB A where PERNR = B.DECISION_3RD_PERNR) as DECISION_3RD_ORGTXT, B.DECISION_3RD_RESULT, "
						+ "(select ENAME from KPS_USER_TB where PERNR = B.DECISION_3RD_PERNR) as DECISION_3RD_ENAME, "
						+ "(select JIKWINM from KPS_USER_TB where PERNR = B.DECISION_3RD_PERNR) as DECISION_3RD_JIKWINM, B.DECISION_3RD_DATE, "
						+ "(select CONCAT(CONCAT((SELECT ORGTX FROM KPS_ORGEH_TB WHERE ORGEH = (SELECT PORGEH FROM KPS_ORGEH_TB WHERE ORGEH = A.ORGEH)),' '), (select ORGTX from KPS_ORGEH_TB where ORGEH = A.ORGEH)) AS ORGTXT from KPS_USER_TB A where PERNR = B.DECISION_4TH_PERNR) as DECISION_4TH_ORGTXT, B.DECISION_4TH_RESULT, "
						+ "(select ENAME from KPS_USER_TB where PERNR = B.DECISION_4TH_PERNR) as DECISION_4TH_ENAME, "
						+ "(select JIKWINM from KPS_USER_TB where PERNR = B.DECISION_4TH_PERNR) as DECISION_4TH_JIKWINM, B.DECISION_4TH_DATE "
						+ "from KPS_USER_TB A, KPS_VPN_USER_TB B where A.PERNR = ? AND B.REQUEST_DEGREE = ? and A.PERNR = B.PERNR";
				
				logger.debug("select_query : " + select_query);
				
				stmt = con.prepareStatement(select_query);
				stmt.setInt(1, Integer.parseInt(pernr));
				stmt.setInt(2, Integer.parseInt(reqDegree));
				
				ResultSet rs = stmt.executeQuery();
				
				Map<String, Object> userInfo;
				
				if (rs.next()) {
					userInfo = new HashMap<String, Object>();
					
					userInfo.put("PERNR", "" + rs.getInt("PERNR"));
					userInfo.put("ENAME", rs.getString("ENAME"));
					userInfo.put("ORGTXT", rs.getString("ORGTXT"));
					
					SimpleDateFormat format = new SimpleDateFormat("yyyy.MM.dd");
					
					Date startDate = rs.getDate("START_DATE");
					userInfo.put("START_DATE", format.format(startDate));
					
					Date endDate = rs.getDate("END_DATE");
					userInfo.put("END_DATE", format.format(endDate));
					
					userInfo.put("REQUEST_REASON", rs.getString("REQUEST_REASON"));
					userInfo.put("RELEVANT_BASE", rs.getString("RELEVANT_BASE"));
					
					int approveStatus = rs.getInt("DECISION_STATUS");
					userInfo.put("DECISION_REQUEST", approveStatus);
					
					ArrayList<HashMap<String, Object>> appRejList = new ArrayList<HashMap<String,Object>>();
					
					SimpleDateFormat format2 = new SimpleDateFormat("yyyy.MM.dd hh:mm:ss a");
					
					if (approveStatus == DECISION_STATUS_1ST) {
						appRejList.add(setAppRejResult(rs, format2, "" + DECISION_STATUS_1ST));
					} else if (approveStatus == DECISION_STATUS_2ND) {
						appRejList.add(setAppRejResult(rs, format2, "" + DECISION_STATUS_1ST));
						appRejList.add(setAppRejResult(rs, format2, "" + DECISION_STATUS_2ND));
					} else if (approveStatus == DECISION_STATUS_3RD) {
						appRejList.add(setAppRejResult(rs, format2, "" + DECISION_STATUS_1ST));
						appRejList.add(setAppRejResult(rs, format2, "" + DECISION_STATUS_2ND));
						appRejList.add(setAppRejResult(rs, format2, "" + DECISION_STATUS_3RD));
					} else if (approveStatus == DECISION_STATUS_4TH) {
						appRejList.add(setAppRejResult(rs, format2, "" + DECISION_STATUS_1ST));
						appRejList.add(setAppRejResult(rs, format2, "" + DECISION_STATUS_2ND));
						appRejList.add(setAppRejResult(rs, format2, "" + DECISION_STATUS_3RD));
						appRejList.add(setAppRejResult(rs, format2, "" + DECISION_STATUS_4TH));
					} else if (approveStatus == DECISION_REJECT_STATUS_1ST) {
						appRejList.add(setAppRejResult(rs, format2, "" + DECISION_STATUS_1ST));
					} else if (approveStatus == DECISION_REJECT_STATUS_2ND) {
						appRejList.add(setAppRejResult(rs, format2, "" + DECISION_STATUS_1ST));
						appRejList.add(setAppRejResult(rs, format2, "" + DECISION_STATUS_2ND));
					} else if (approveStatus == DECISION_REJECT_STATUS_3RD) {
						appRejList.add(setAppRejResult(rs, format2, "" + DECISION_STATUS_1ST));
						appRejList.add(setAppRejResult(rs, format2, "" + DECISION_STATUS_2ND));
						appRejList.add(setAppRejResult(rs, format2, "" + DECISION_STATUS_3RD));
					} else if (approveStatus == DECISION_REJECT_STATUS_4TH) {
						appRejList.add(setAppRejResult(rs, format2, "" + DECISION_STATUS_1ST));
						appRejList.add(setAppRejResult(rs, format2, "" + DECISION_STATUS_2ND));
						appRejList.add(setAppRejResult(rs, format2, "" + DECISION_STATUS_3RD));
						appRejList.add(setAppRejResult(rs, format2, "" + DECISION_STATUS_4TH));
					}
					
					userInfo.put("DECISION_LIST", appRejList);
					
					returnJsonMsg.put(RESULT, RESULT_SUCCESS);
					returnJsonMsg.put("Data", userInfo);
				} else {
					returnJsonMsg.put(RESULT, RESULT_FAIL);
					returnJsonMsg.put(RESULT_MESSAGE, RESULT_USER_NOT_EXIST + pernr);
				}
				
				logger.debug("" + JSONConverter.jsonize(returnJsonMsg));
				
				jsonConverter(out, resp, returnJsonMsg);
				
				return;
			} catch (Exception e1) {
				logger.error("frodo user kps ui: cannot convert object to json string", e1);
				resp.sendError(500);
				return;
			} finally {
				close(stmt, con);
			}
		}
		
		if (path.equals("/restore")) {
			Connection con = null;
			PreparedStatement stmt = null;
			
			try {
				con = getConnectionString();
				
				String select_query = "select distinct pernr FROM KPS_VPN_USER_TB where DECISION_STATUS = 4 and END_DATE > now() and (REQUEST_DEGREE = 1 or REQUEST_DEGREE = 2 or REQUEST_DEGREE = 3)";
				stmt = con.prepareStatement(select_query);
				
				ResultSet rs = stmt.executeQuery();
				
				while (rs.next()) {
					String pernr = "" + rs.getInt("PERNR");
					logger.info("create user : " + pernr);
					registerVpnSSLUserWithoutSendMail(pernr);
				}
				
				String select_query2 = "select distinct a.pernr FROM KPS_VPN_USER_TB a, KPS_USER_TB b where a.DECISION_STATUS = 3  and END_DATE > now() and (REQUEST_DEGREE = 1 or REQUEST_DEGREE = 2 or REQUEST_DEGREE = 3) and a.PERNR = b.PERNR and b.REDER_CODE = 'X'";
				stmt = con.prepareStatement(select_query2);
				
				ResultSet rs2 = stmt.executeQuery();
				
				while (rs2.next()) {
					String pernr = "" + rs2.getInt("PERNR");
					logger.info("create user : " + pernr);
					
					if (!pernr.equals("1862913")) {
						try {
							registerVpnSSLUserWithoutSendMail(pernr);
						} catch (Exception e) {
							
						}
					}
				}
				
				return;
			} catch (ClassNotFoundException e1) {
				logger.debug("frodo user kps ui: cannot convert object to json string", e1);
				resp.sendError(500);
			} catch (SQLException e1) {
				logger.debug("frodo user kps ui: cannot convert object to json string", e1);
				resp.sendError(500);
			} finally {
				close(stmt, con);
			}
		}
		
		if (path.equals("/vpn_user_delete")) {
			Connection con = null;
			PreparedStatement stmt = null;
			
			try {
				con = getConnectionString();
				
				String select_query = "delete from KPS_VPN_USER_TB";
				stmt = con.prepareStatement(select_query);
				
				stmt.executeUpdate(select_query);
			} catch (ClassNotFoundException e1) {
			} catch (SQLException e1) {
			} finally {
				close(stmt, con);
			}
			
			resp.sendError(500);
			return;
		}
		
		if (path.equals("/vpn_grant.html")) {
			String decisionPernr = Base64.decodeString(req.getParameter("decisionPernr"));
			String applicantPernr = Base64.decodeString(req.getParameter("applicantPernr"));
			
			
			
			String secManagerPernr = getSecManagerPernr();
			String secManagerYN;
			
			if (secManagerPernr != null && secManagerPernr.equals(decisionPernr)) {
				secManagerYN = "true";
				
				if (getApplicantTeamManager(applicantPernr) == null && getApplicantDecisionStatus(applicantPernr).equals("0")) {
					secManagerYN = "false";
				}
			} else {
				secManagerYN = "false";
			}
			
			String decision = getDecision();
			
			decision = decision.replaceAll("#DECISON_PERNR", decisionPernr);
			decision = decision.replaceAll("#APPLICANT_PERNR", applicantPernr);
			decision = decision.replaceAll("#SECMANAGER", secManagerYN);
			
			resp.setHeader("Content-Type", "text/html");
			resp.getOutputStream().write(decision.getBytes(Charset.forName("utf-8")));
			resp.getOutputStream().flush();
		} else if (path.equals("/vpn_welcome.html")) {
			String html = getHtml("vpn_welcome.html");
			
			resp.setHeader("Content-Type", "text/html");
			resp.getOutputStream().write(html.getBytes(Charset.forName("utf-8")));
			resp.getOutputStream().flush();
		} else if (path.equals("/vpn_request.html")) {
			String html = getHtml("vpn_request.html");
			
			resp.setHeader("Content-Type", "text/html");
			resp.getOutputStream().write(html.getBytes(Charset.forName("utf-8")));
			resp.getOutputStream().flush();
		} else if (path.equals("/vpn_manager.html")) {
			String html = getHtml("vpn_manager.html");
			
			resp.setHeader("Content-Type", "text/html");
			resp.getOutputStream().write(html.getBytes(Charset.forName("utf-8")));
			resp.getOutputStream().flush();
		} else if (path.isEmpty() || path.equals("/")) {
			resp.sendError(404);
		} else {
			sendFile(req, resp);
		}
	}
	
	private int updateSecManagerInfo(String pernr, String secManager) {
		Connection con = null;
		PreparedStatement stmt = null;
		
		String updateQuery = "update KPS_USER_TB set SECMANAGER = ? where PERNR = ?";
		
		try {
			con = getConnectionString();
			
			stmt = con.prepareStatement(updateQuery);
			
			stmt.setString(1, secManager);
			stmt.setInt(2, Integer.parseInt(pernr));
			
			return stmt.executeUpdate();
			
		} catch (ClassNotFoundException e) {
			logger.error("frodo user kps ui: vpn account request insert error", e);
		} catch (SQLException e) {
			logger.error("frodo user kps ui: vpn account request insert error", e);
		} finally {
			close(stmt, con);
		}
		return 0;

	}
	
	private int findDicisionMaker(String applicantPernr, int approveStatus) {
		// TODO 계정 요청자의 사번과 현재 계정 승인 상태를 받아서 다음 승인자의 사번을 리턴 
		return 0;
	}

	private HashMap<String, Object> setAppRejResult (ResultSet rs, SimpleDateFormat format, String decisionId) throws SQLException {
		HashMap<String, Object> appRejInfo = new HashMap<String, Object>();
		
		appRejInfo.put("DECISION_ID", decisionId);
		
		String DECISION_RESULT = "DECISION_APPREJID_RESULT";
		String DECISION_ENAME = "DECISION_APPREJID_ENAME";
		String DECISION_JIKWINM = "DECISION_APPREJID_JIKWINM";
		String DECISION_DATE = "DECISION_APPREJID_DATE";
		String DECISION_ORGTXT = "DECISION_APPREJID_ORGTXT";
		
		if (decisionId.equals("1") || decisionId.equals("5")) {
			DECISION_RESULT = DECISION_RESULT.replace("APPREJID", "1ST");
			DECISION_ENAME = DECISION_ENAME.replace("APPREJID", "1ST");
			DECISION_JIKWINM = DECISION_JIKWINM.replace("APPREJID", "1ST");
			DECISION_DATE = DECISION_DATE.replace("APPREJID", "1ST");
			DECISION_ORGTXT = DECISION_ORGTXT.replace("APPREJID", "1ST");
		} else if (decisionId.equals("2") || decisionId.equals("6")) {
			DECISION_RESULT = DECISION_RESULT.replace("APPREJID", "2ND");
			DECISION_ENAME = DECISION_ENAME.replace("APPREJID", "2ND");
			DECISION_JIKWINM = DECISION_JIKWINM.replace("APPREJID", "2ND");
			DECISION_DATE = DECISION_DATE.replace("APPREJID", "2ND");
			DECISION_ORGTXT = DECISION_ORGTXT.replace("APPREJID", "2ND");
		} else if (decisionId.equals("3") || decisionId.equals("7")) {
			DECISION_RESULT = DECISION_RESULT.replace("APPREJID", "3RD");
			DECISION_ENAME = DECISION_ENAME.replace("APPREJID", "3RD");
			DECISION_JIKWINM = DECISION_JIKWINM.replace("APPREJID", "3RD");
			DECISION_DATE = DECISION_DATE.replace("APPREJID", "3RD");
			DECISION_ORGTXT = DECISION_ORGTXT.replace("APPREJID", "3RD");
		} else if (decisionId.equals("4") || decisionId.equals("8")) {
			DECISION_RESULT = DECISION_RESULT.replace("APPREJID", "4TH");
			DECISION_ENAME = DECISION_ENAME.replace("APPREJID", "4TH");
			DECISION_JIKWINM = DECISION_JIKWINM.replace("APPREJID", "4TH");
			DECISION_DATE = DECISION_DATE.replace("APPREJID", "4TH");
			DECISION_ORGTXT = DECISION_ORGTXT.replace("APPREJID", "4TH");
		}
		
		if (rs.getString(DECISION_RESULT).equals(RESULT_APPROVE)) {
			appRejInfo.put("DECISION_RESULT", "승인");
		} else {
			appRejInfo.put("DECISION_RESULT", "반려");
		}
		
		appRejInfo.put("DECISION_ENAME", rs.getString(DECISION_ENAME));
		appRejInfo.put("DECISION_JIKWINM", rs.getString(DECISION_JIKWINM));
		appRejInfo.put("DECISION_ORGTXT", rs.getString(DECISION_ORGTXT));
		
		Timestamp appRejDate = rs.getTimestamp(DECISION_DATE);
		appRejInfo.put("DECISION_DATE", format.format(appRejDate));
		
		return appRejInfo;
	}
	
	private void setHttpServletResponseHeader(HttpServletResponse resp) {
		resp.setHeader("Access-Control-Allow-Origin", "*");
		resp.setHeader("Access-Control-Allow-Methods", "GET,POST");
		resp.setHeader("Access-Control-Max-Age", "360");
		resp.setHeader("Access-Control-Allow-Headers", "X-PINGOTHER, Origin, X-Requested-With, Content-Type, Accept");
		resp.setHeader("Access-Control-Allow-Credentials", "true");
		resp.setContentType("application/json; charset=UTF-8");
	}
	
	private String getApproveStatus(int status) {
		if (status == 0) {
			return new String("1차 승인 대기 중");
		} else if (status == 1) {
			return new String("2차 승인 대기 중");
		} else if (status == 2) {
			return new String("3차 승인 대기 중");
		} else if (status == 3) {
			return new String("최종 승인 대기 중");
		} else if (status == 4) {
			return new String("계정생성완료");
		} else if (status == 5) {
			return new String("1차 승인 반려");
		} else if (status == 6) {
			return new String("2차 승인 반려");
		} else if (status == 7) {
			return new String("3차 승인 반려");
		} else if (status == 8) {
			return new String("최종 승인 반려");
		}
		
		return new String("error");
	}
	
	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		if (req.getPathInfo() == null) {
			resp.sendError(404);
			return;
		}
		
		PrintWriter out = null;
		
		String host = req.getHeader("Host");
		String path = req.getPathInfo();
		logger.trace("frodo user kps ui: get host [{}] path [{}]", host, path);
		
		if (path.equals("/reqVpnAccount")) {
			setHttpServletResponseHeader(resp);
			
			out = resp.getWriter();
			
			String perNr = "";
			Date startDate = null;
			Date endDate = null;
			String requestReason = "";
			String relevantBase = "";
			
			BufferedReader reader = new BufferedReader(new InputStreamReader(req.getInputStream(), "UTF-8"));
			String json = reader.readLine();
			
			logger.debug("json : " + json);
			
			JSONTokener tokener = new JSONTokener(json);
			JSONObject jsonObject = null;
			
			try {
				jsonObject = new JSONObject(tokener);
			} catch (JSONException e) {
				logger.error("frodo user kps ui: cannot convert object to json string", e);
			}
			
			Map<String, Object> recevicedMsg = null;
			
			recevicedMsg = (Map<String, Object>) JSONConverter.parse(jsonObject);
			
			logger.debug("PERNR : " + recevicedMsg.get("PERNR"));
			logger.debug("StartDate : " + recevicedMsg.get("startDate"));
			logger.debug("EndDate : " + recevicedMsg.get("endDate"));
			logger.debug("RequestReason : " + recevicedMsg.get("requestReason"));
			logger.debug("RelevantBase : " + recevicedMsg.get("relevantBase"));
			
			perNr = (String) recevicedMsg.get("PERNR");
			
			SimpleDateFormat format = new SimpleDateFormat("yyyy.MM.dd");
			
			try {
				startDate =  new Date(format.parse((String) recevicedMsg.get("startDate")).getTime());
			} catch (ParseException e) {
				logger.error("frodo user kps ui: cannot convert object to json string", e);
			}
			
			try {
				endDate =  new Date(format.parse((String) recevicedMsg.get("endDate")).getTime());
			} catch (ParseException e) {
				logger.error("frodo user kps ui: cannot convert object to json string", e);
			}
			
			requestReason = (String) recevicedMsg.get("requestReason");
			relevantBase = (String) recevicedMsg.get("relevantBase");
			
			HashMap<String, String> userInfo = getUserInfo(perNr);
			
			if (userInfo != null) {
				String decisionStatus = userInfo.get("DECISIONSTATUS");
				String applicantTeamManager = getApplicantTeamManager(perNr);
				
				if (applicantTeamManager == null) {
					applicantTeamManager = getSecManagerPernr();
				}
				
				if (decisionStatus != null) {
					if (decisionStatus.equals("" + DECISION_REJECT_STATUS_1ST) || decisionStatus.equals("" + DECISION_REJECT_STATUS_2ND) || decisionStatus.equals("" + DECISION_REJECT_STATUS_3RD) || decisionStatus.equals("" + DECISION_REJECT_STATUS_4TH)) {
						//거절된 사람이 다시 신청하는 것이므로 고객님의 결정에 따라 기존 신청 정보를 날리고 다시 신청을 처음부터 하는 것으로 진행한다.
						int deleteCnt = deleteRejectRequest(perNr);
						logger.debug("Delete Count: " + deleteCnt);
					} else if (applicantTeamManager.equals(perNr) && !decisionStatus.equals("" + DECISION_STATUS_3RD)) {
						if (!decisionStatus.equals("" + DECISION_STATUS_4TH)) {
							//결제 프로세스가 완전히 끝나지 않은 사용자는 신청 중이라는 메시지를 띄운다.
							Map<String, Object> returnJsonMsg = new HashMap<String, Object>();
							
							returnJsonMsg.put(RESULT, RESULT_FAIL);
							returnJsonMsg.put(RESULT_MESSAGE, RESULT_VPN_ACCOUNT_REQUEST_PROGRESS);
							
							jsonConverter(out, resp, returnJsonMsg);
							
							return;
						}
					} else if (!applicantTeamManager.equals(perNr) &&!decisionStatus.equals("" + DECISION_STATUS_4TH)) {
						//결제 프로세스가 완전히 끝나지 않은 사용자는 신청 중이라는 메시지를 띄운다.
						Map<String, Object> returnJsonMsg = new HashMap<String, Object>();
						
						returnJsonMsg.put(RESULT, RESULT_FAIL);
						returnJsonMsg.put(RESULT_MESSAGE, RESULT_VPN_ACCOUNT_REQUEST_PROGRESS);
						
						jsonConverter(out, resp, returnJsonMsg);
						
						return;
					}
				}
			}
			
			int userRequestDegree = 1;
			
			String userMaxDegree = getUserRequestDegree(perNr);
			
			if (userMaxDegree != null) {
				userRequestDegree = Integer.parseInt(userMaxDegree) + 1;
			}
			
			logger.debug("userRequestDegree : " + userRequestDegree);
			
			Connection con = null;
			PreparedStatement stmt = null;
			
			String requestQuery = "INSERT INTO KPS_VPN_USER_TB (PERNR, REQUEST_DEGREE, DECISION_STATUS, START_DATE, END_DATE, REQUEST_REASON, RELEVANT_BASE, REQUEST_DATE) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
			
			int insertCnt = 0;
			
			try {
				con = getConnectionString();
				
				stmt = con.prepareStatement(requestQuery);
				
				stmt.setString(1, perNr);
				stmt.setInt(2, userRequestDegree);
				stmt.setInt(3, DECISION_STATUS_REQUEST);
				stmt.setDate(4, startDate);
				stmt.setDate(5, endDate);
				stmt.setString(6, requestReason);
				stmt.setString(7, relevantBase);
				stmt.setTimestamp(8, getCurrentTimeStamp());
				
				insertCnt = stmt.executeUpdate();
				
				logger.debug("frodo user kps ui: accept " + perNr + " VPN account request");
			} catch (ClassNotFoundException e) {
				logger.error("frodo user kps ui: vpn account request insert error", e);
			} catch (SQLException e) {
				logger.error("frodo user kps ui: vpn account request insert error", e);
			} finally {
				close(stmt, con);
			}
			
			//신청자가 팀원인지 팀장이상인지를 구분하여 다음 결제자에게 결제를 위한 URL을 포함한 메일을 보낸다.
			//팀원의 경우에는 해당 팀원의 팀장에게 메일을 보내고, 팀장이상일 경우에는 정보보안 담당자에게 메일을 보낸다.
			
			String decisionMakerPernr = get1stDicisionMaker(perNr);
			
			logger.debug("decisionMakerPernr : " + decisionMakerPernr);
			
			
			if (decisionMakerPernr == null || decisionMakerPernr.equals(perNr)) {
				//신청자가 팀장이상이므로 1차 결재 메일을 정보보안담당자에게 보낸다.
				logger.debug("신청자가 팀장이상이거나 팀장이 없는 팀원이므로 1차 결재 메일을 정보보안담당자에게 보낸다.");
				sendMail(getSecManagerPernr(), perNr);
			} else {
				//신청자의 팀장에게 메일을 보낸다.
				logger.debug("신청자의 팀장에게 메일을 보낸다.");
				sendMail(decisionMakerPernr, perNr);
			}
			
			Map<String, Object> returnJsonMsg = new HashMap<String, Object>();
			
			if (insertCnt == 1) {
				returnJsonMsg.put(RESULT, RESULT_SUCCESS);
				returnJsonMsg.put(RESULT_MESSAGE, RESULT_VPN_ACCOUNT_REQUEST_SUCCESS);
			} else {
				returnJsonMsg.put(RESULT, RESULT_FAIL);
				returnJsonMsg.put(RESULT_MESSAGE, RESULT_VPN_ACCOUNT_REQUEST_FAIL);
			}
			
			jsonConverter(out, resp, returnJsonMsg);
			
			return;
		}
		
		if (path.equals("/decisionVpnAccount")) {
			setHttpServletResponseHeader(resp);
			
			out = resp.getWriter();
			
//			String applicantPernr = req.getParameter("applicant");
//			String decisionPernr = req.getParameter("decision");
//			
			BufferedReader reader = new BufferedReader(new InputStreamReader(req.getInputStream(), "UTF-8"));
			String json = reader.readLine();
			
			logger.debug("json : " + json);
			
			JSONTokener tokener = new JSONTokener(json);
			JSONObject jsonObject = null;
			
			try {
				jsonObject = new JSONObject(tokener);
			} catch (JSONException e) {
				logger.error("frodo user kps ui: cannot convert object to json string", e);
			}
			
			Map<String, Object> recevicedMsg = null;
			
			recevicedMsg = (Map<String, Object>) JSONConverter.parse(jsonObject);
			
			logger.debug("APPLICANT_PERNR : " + recevicedMsg.get("applicantPernr"));
			logger.debug("DECISION_MAKER_PERNR : " + recevicedMsg.get("decisionPernr"));
			logger.debug("DECISION_RESULT : " + recevicedMsg.get("decisionResult"));
			logger.debug("DECISION_REASON : " + recevicedMsg.get("decisionReason"));
			
			String applicantPernr = "" + (Integer) recevicedMsg.get("applicantPernr");
			String decisionPernr = "" + (Integer) recevicedMsg.get("decisionPernr");
			String decisionResult = (String) recevicedMsg.get("decisionResult");
			String decisionReason = (String) recevicedMsg.get("decisionReason");
			
			String nextDecisionPernr = null;
			
			if (getSecManagerPernr().equals(decisionPernr)) {
				logger.debug("nextDecisionPernr : " + recevicedMsg.get("nextDecisionPernr"));
				nextDecisionPernr = (String) recevicedMsg.get("nextDecisionPernr");
			}
			
			String preDecisionStatus = getApplicantDecisionStatus(applicantPernr);
			
			if (!preDecisionStatus.equals("0") && isDuplicationRequest(preDecisionStatus, applicantPernr, decisionPernr, nextDecisionPernr)) {
				Map<String, Object> returnJsonMsg = new HashMap<String, Object>();
				
				returnJsonMsg.put(RESULT, RESULT_SUCCESS);
				returnJsonMsg.put(RESULT_MESSAGE, RESULT_VPN_DECISION_REQUEST_DUPLICATION);
				
				jsonConverter(out, resp, returnJsonMsg);
				
				return;
			}
			
			int decisionStatus = -1;
			String degree = null;
			
			if (preDecisionStatus.equals("0")) {
				decisionStatus = DECISION_STATUS_1ST;
				degree = "1ST";
			} else if (preDecisionStatus.equals("1")) {
				decisionStatus = DECISION_STATUS_2ND;
				degree = "2ND";
			} else if (preDecisionStatus.equals("2")) {
				decisionStatus = DECISION_STATUS_3RD;
				degree = "3RD";
			} else if (preDecisionStatus.equals("3")) {
				decisionStatus = DECISION_STATUS_4TH;
				degree = "4TH";
			}
			
			if (decisionResult.equals(RESULT_REJECT)) {
				if (preDecisionStatus.equals("0")) {
					decisionStatus = DECISION_REJECT_STATUS_1ST;
					degree = "1ST";
				} else if (preDecisionStatus.equals("1")) {
					decisionStatus = DECISION_REJECT_STATUS_2ND;
					degree = "2ND";
				} else if (preDecisionStatus.equals("2")) {
					decisionStatus = DECISION_REJECT_STATUS_3RD;
					degree = "3RD";
				} else if (preDecisionStatus.equals("3")) {
					decisionStatus = DECISION_REJECT_STATUS_4TH;
					degree = "4TH";
				}
			}
			
			int userRequestMaxDegree = getUserMaxRequestDegree(applicantPernr);
			
			Connection con = null;
			PreparedStatement stmt = null;
			
			String updateQuery = "update KPS_VPN_USER_TB set DECISION_STATUS = ?, DECISION_#DEGREE_RESULT = ?, DECISION_#DEGREE_PERNR = ?, DECISION_#DEGREE_REASON = ?, DECISION_#DEGREE_DATE = ? where PERNR = ? AND REQUEST_DEGREE = ?";
			
			updateQuery = updateQuery.replaceAll("#DEGREE", degree);
			
			int updateCnt = 0;
			
			try {
				con = getConnectionString();
				
				stmt = con.prepareStatement(updateQuery);
				
				stmt.setInt(1, decisionStatus);
				stmt.setString(2, decisionResult);
				stmt.setString(3, decisionPernr);
				stmt.setString(4, decisionReason);
				stmt.setTimestamp(5, getCurrentTimeStamp());
				stmt.setInt(6, Integer.parseInt(applicantPernr));
				stmt.setInt(7, userRequestMaxDegree);
				
				updateCnt = stmt.executeUpdate();
				
			} catch (ClassNotFoundException e) {
				logger.error("frodo user kps ui: vpn account request insert error", e);
			} catch (SQLException e) {
				logger.error("frodo user kps ui: vpn account request insert error", e);
			} finally {
				close(stmt, con);
			}
			
			// 반려일 경우 사용자에게 반려 사유 메일을 보낸다.
			
			if (decisionResult.equals(RESULT_REJECT)) {
				sendRejectMail(applicantPernr, decisionStatus);
			}
			
			//최종 결제가 아니거나 결제 결과가 승인일 때만 다음 결제자에게 메일을 보낸다.
			
			logger.debug("decisionStatus : " +  decisionStatus + ", decisionResult : " + decisionResult);
			
			String applicantTeamManager = getApplicantTeamManager(applicantPernr);
			
			if (applicantTeamManager == null) {
				applicantTeamManager = getSecManagerPernr();
			}
			
			//신청자가 보안담당자이고 결제자가 팀장일 때 메일을 vpn 관리자에게 보냄
			if (getSecManagerPernr().equals(applicantPernr) && applicantTeamManager.equals(decisionPernr)) {
				sendMail(getVpnAdminPernr(), applicantPernr);
			} else if (getSecManagerPernr().equals(applicantPernr) && getVpnAdminPernr().equals(decisionPernr)) {
				//메일을 보내지 않음
			} else {//신청자가 보안담당자가 아닐때
				//신청자가 팀장이 아닐때
				if (!applicantTeamManager.equals(applicantPernr)) {
					if (decisionStatus != 4 && decisionResult.equals(RESULT_APPROVE)) {
						String decisionMakerPernr;
						
						if (nextDecisionPernr != null) {
							logger.debug("nextDecisionPernr != null nextDecisionPernr: " + nextDecisionPernr);
							decisionMakerPernr = nextDecisionPernr;
						} else {
							if (decisionStatus == DECISION_STATUS_3RD) {
								nextDecisionPernr = decisionPernr;
								logger.debug("decisionStatus == DECISION_STATUS_3RD, nextDecisionPernr: " + nextDecisionPernr);
							} else if (applicantTeamManager.equals(applicantPernr) && decisionStatus == DECISION_STATUS_2ND) {
								nextDecisionPernr = decisionPernr;
								logger.debug("applicantTeamManager.equals(applicantPernr) && decisionStatus == DECISION_STATUS_2ND, nextDecisionPernr: " + nextDecisionPernr);
							}
							
							
							decisionMakerPernr = getNextDicisionMaker(applicantPernr, decisionPernr, nextDecisionPernr, decisionStatus);
						}
						
						logger.debug("decisionMakerPernr : " + decisionMakerPernr);

                        if (decisionMakerPernr == null) {
                            sendMail(getSecManagerPernr(), applicantPernr);
                        } else {
                            sendMail(decisionMakerPernr, applicantPernr);
                        }


					}
				} else { //신청자가 팀장일때
					if (decisionStatus != 3 && decisionResult.equals(RESULT_APPROVE)) {
						String decisionMakerPernr;
						
						if (nextDecisionPernr != null) {
							decisionMakerPernr = nextDecisionPernr;
						} else {
							if (decisionStatus == DECISION_STATUS_3RD) {
								nextDecisionPernr = decisionPernr;
							} else if (applicantTeamManager.equals(applicantPernr) && decisionStatus == DECISION_STATUS_2ND) {
								nextDecisionPernr = decisionPernr;
							}
							decisionMakerPernr = getNextDicisionMaker(applicantPernr, decisionPernr, nextDecisionPernr, decisionStatus);
						}
						
						logger.debug("decisionMakerPernr : " + decisionMakerPernr);

                        if (decisionMakerPernr == null) {
                            sendMail(getSecManagerPernr(), applicantPernr);
                        } else {
                            sendMail(decisionMakerPernr, applicantPernr);
                        }
					}
				}
			}
			
			//일반 사원이 신청한 경우
			//처음 계정 신청하는 것임므로 사용자를 생성한다.
			if (userRequestMaxDegree == 1 || getPreUserDecisionStatus(applicantPernr, userRequestMaxDegree) >= 5) {
				if (!applicantTeamManager.equals(applicantPernr)) {
					if (decisionStatus == 4 && decisionResult.equals(RESULT_APPROVE)) {
						registerVpnSSLUser(applicantPernr);
					} else if (getSecManagerPernr().equals(applicantPernr) && decisionStatus == 2 && decisionResult.equals(RESULT_APPROVE)) {
						registerVpnSSLUser(applicantPernr);
					}
				} else { //팀장이 신청한 경우
					if (decisionStatus == 3 && decisionResult.equals(RESULT_APPROVE)) {
						registerVpnSSLUser(applicantPernr);
					}
				}
			} else { //계정을 연장 신청하거나 유료기간 만료로 신청하는 경우이므로 신청자의 정보를 업데이트 해야함. 이슈 : 정상 사용 기간인 사람이 미리 신청을 한 경우에는 어떻게 처리 할것인가..
				if (getPreUserDecisionStatus(applicantPernr, userRequestMaxDegree) < 5) {
					User user = domUserApi.getUser("localhost", applicantPernr);
					UserExtension userExtension = userApi.getUserExtension(user);
					java.util.Date userExpireDate = userExtension.getExpireDateTime();
					java.util.Date userRequestDate = getVpnRequestDate(applicantPernr);
	
					if (userRequestDate.compareTo(userExpireDate) > 0) {
						//신청자의 현재 유효기간이 신청한 날짜의 시작날짜보다 뒤이면 이미 유효기간이 지나버린 뒤에 재신청하는 것이므로 유효기간만을 업데이트 해준다.
						if (!applicantTeamManager.equals(applicantPernr)) {
							if (decisionStatus == 4 && decisionResult.equals(RESULT_APPROVE)) {
								updateVpnSSLUser(applicantPernr);
							} else if (getSecManagerPernr().equals(applicantPernr) && decisionStatus == 2 && decisionResult.equals(RESULT_APPROVE)) {
								updateVpnSSLUser(applicantPernr);
							}
						} else { //팀장이 신청한 경우
							if (decisionStatus == 3 && decisionResult.equals(RESULT_APPROVE)) {
								updateVpnSSLUser(applicantPernr);
							}
						}
					} else {
                        updateVpnSSLUser(applicantPernr); //한전 협의로 인하여 유효기간이 만료 전이라도 신청한 시간이로 무조건 업데이트한다.
                        logger.info("User sslplus using date updated. id: " + applicantPernr);
                        //updateUserRequestPendingCheck(applicantPernr);//현재 유효기간이 만료되기 전에 신청하는 것으므로 REQUEST_PENDING 컬럼에 X 표시를 하여 체크해 놓는다.
					}
				}
			}
			
			Map<String, Object> returnJsonMsg = new HashMap<String, Object>();
			
			if (updateCnt == 1) {
				returnJsonMsg.put(RESULT, RESULT_SUCCESS);
				
				if (decisionResult.equals(RESULT_REJECT)) {
					returnJsonMsg.put(RESULT_MESSAGE, RESULT_VPN_DECISION_REQUEST_REJECT);
				} else {
					returnJsonMsg.put(RESULT_MESSAGE, RESULT_VPN_DECISION_REQUEST_APPROVE);
				}
			} else {
				returnJsonMsg.put(RESULT, RESULT_FAIL);
				returnJsonMsg.put(RESULT_MESSAGE, RESULT_VPN_DECISION_REQUEST_FAIL);
			}
			
			jsonConverter(out, resp, returnJsonMsg);
			
			return;
		}
	}
	
	private int getPreUserDecisionStatus(String applicantPernr, int userRequestMaxDegree) {
		Connection con = null;
		PreparedStatement stmt = null;
		
		try {
			con = getConnectionString();
			
			String select_query = "select DECISION_STATUS from KPS_VPN_USER_TB where PERNR = ? and REQUEST_DEGREE = ?";
			stmt = con.prepareStatement(select_query);
			stmt.setInt(1, Integer.parseInt(applicantPernr));
			stmt.setInt(2, userRequestMaxDegree - 1);
			
			ResultSet rs = stmt.executeQuery();
			
			if (rs.next()) {
				return rs.getInt("DECISION_STATUS");
			}
		} catch (ClassNotFoundException e1) {
			return -1;
		} catch (SQLException e1) {
			return -1;
		} finally {
			close(stmt, con);
		}
		
		return -1;
	}

	private int updateUserRequestPendingCheck(String applicantPernr) {
		int userMaxDegree = getUserMaxRequestDegree(applicantPernr);
		
		Connection con = null;
		PreparedStatement stmt = null;
		
		int result = 0;
		
		try {
			con = getConnectionString();
			
			String update_query = "update KPS_VPN_USER_TB set REQUEST_PENDING = ? where PERNR = ? AND REQUEST_DEGREE = ?";
			stmt = con.prepareStatement(update_query);
			stmt.setString(1, DECISION_PENDING_CHECK);
			stmt.setInt(2, Integer.parseInt(applicantPernr));
			stmt.setInt(3, userMaxDegree);
			
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

	private java.util.Date getVpnRequestDate(String applicantPernr) {
		int userMaxDegree = getUserMaxRequestDegree(applicantPernr);
		
		Connection con = null;
		PreparedStatement stmt = null;
		
		try {
			con = getConnectionString();
			
			String select_query = "select START_DATE from KPS_VPN_USER_TB where PERNR = ? AND REQUEST_DEGREE = ?";
			stmt = con.prepareStatement(select_query);
			stmt.setInt(1, Integer.parseInt(applicantPernr));
			stmt.setInt(2, userMaxDegree);
			
			ResultSet rs = stmt.executeQuery();
			
			if (rs.next()) {
				return rs.getDate("START_DATE");
			}
		} catch (ClassNotFoundException e1) {
			return null;
		} catch (SQLException e1) {
			return null;
		} finally {
			close(stmt, con);
		}
		
		return null;
	}

	private void registerVpnSSLUser(String applicantPernr) {
		HashMap<String, String> userInfo = getUserInfo(applicantPernr);
		User user = createLocalUser(applicantPernr, userInfo.get("ENAME"), userInfo.get("ORGTXT"), userInfo.get("EMAIL"));
		
		UserExtension ext = null;

		if (ext == null) {
			java.util.Date startDate = null;
			java.util.Date endDate = null;
			
			ext = new UserExtension();
			ext.setUser(user);
			
			try {
				SimpleDateFormat format = new SimpleDateFormat("yyyy.MM.dd");
				startDate =  new java.util.Date(format.parse((String) userInfo.get("STARTDATE")).getTime());
			} catch (ParseException e) {
				logger.error("frodo user kps ui: cannot convert object to json string", e);
			}
			
			try {
				endDate =  getExpireDate((String) userInfo.get("ENDDATE"));
			} catch (ParseException e) {
				logger.error("frodo user kps ui: cannot convert object to json string", e);
			}
			
			ext.setForcePasswordChange(true);
			ext.setStartDateTime(startDate);
			ext.setExpireDateTime(endDate);
			ext.setCreateDateTime(new java.util.Date());
			ext.setUpdateDateTime(new java.util.Date());
		}

		userApi.setUserExtension(ext);
		
		sendApproveMail(applicantPernr, false);
	}
	
	private void registerVpnSSLUserWithoutSendMail(String applicantPernr) {
		HashMap<String, String> userInfo = getUserInfo(applicantPernr);
		User user = createLocalUser(applicantPernr, userInfo.get("ENAME"), userInfo.get("ORGTXT"), userInfo.get("EMAIL"));
		
		UserExtension ext = null;

		if (ext == null) {
			java.util.Date startDate = null;
			java.util.Date endDate = null;
			
			ext = new UserExtension();
			ext.setUser(user);
			
			try {
				SimpleDateFormat format = new SimpleDateFormat("yyyy.MM.dd");
				startDate =  new java.util.Date(format.parse((String) userInfo.get("STARTDATE")).getTime());
			} catch (ParseException e) {
				logger.error("frodo user kps ui: cannot convert object to json string", e);
			}
			
			try {
				endDate =  getExpireDate((String) userInfo.get("ENDDATE"));
			} catch (ParseException e) {
				logger.error("frodo user kps ui: cannot convert object to json string", e);
			}
			
			ext.setForcePasswordChange(true);
			ext.setStartDateTime(startDate);
			ext.setExpireDateTime(endDate);
			ext.setCreateDateTime(new java.util.Date());
			ext.setUpdateDateTime(new java.util.Date());
		}

		userApi.setUserExtension(ext);
	}
	
	private void updateVpnSSLUser(String applicantPernr) {
		HashMap<String, String> userInfo = getUserInfo(applicantPernr);
		User user = domUserApi.getUser("localhost", applicantPernr);
		UserExtension userExtension = userApi.getUserExtension(user);
		
		if(userExtension.isLocked()) {
			userExtension.setLocked(false);
		}
		
		java.util.Date startDate = null;
		java.util.Date endDate = null;
			
		userExtension.setUser(user);
			
		try {
			SimpleDateFormat format = new SimpleDateFormat("yyyy.MM.dd");
			startDate =  new java.util.Date(format.parse((String) userInfo.get("STARTDATE")).getTime());
		} catch (ParseException e) {
			logger.error("frodo user kps ui: cannot convert object to json string", e);
		}
		
		try {
			endDate =  getExpireDate((String) userInfo.get("ENDDATE"));
		} catch (ParseException e) {
			logger.error("frodo user kps ui: cannot convert object to json string", e);
		}
		
		userExtension.setStartDateTime(startDate);
		userExtension.setExpireDateTime(endDate);
		userExtension.setUpdateDateTime(new java.util.Date());

		userApi.setUserExtension(userExtension);
		
		sendApproveMail(applicantPernr, true);
	}
	
	private int deleteRejectRequest(String pernr) {
		int userMaxDegree = getUserMaxRequestDegree(pernr);
		
		Connection con = null;
		PreparedStatement stmt = null;
		
		int result = 0;
		
		try {
			con = getConnectionString();
			
			String select_query = "delete from KPS_VPN_USER_TB where PERNR = ? and REQUEST_DEGREE = ?";
			stmt = con.prepareStatement(select_query);
			stmt.setInt(1, Integer.parseInt(pernr));
			stmt.setInt(1, userMaxDegree);
			
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

	private boolean isDuplicationRequest(String preDecisionStatus, String applicantPernr, String decisionPernr, String nextDecisionPernr) {
		logger.info("preDecisionStatus : " + preDecisionStatus + ". applicantPernr : " + applicantPernr + ", decisionPernr : " + decisionPernr);
		int decisionStatus = Integer.parseInt(preDecisionStatus);
		int userMaxDegree = getUserMaxRequestDegree(applicantPernr);
		
		Connection con = null;
		PreparedStatement stmt = null;
		
		try {
			con = getConnectionString();
			
			String select_query = "SELECT A.ENAME, CONCAT(CONCAT((SELECT ORGTX FROM KPS_ORGEH_TB WHERE ORGEH = (SELECT PORGEH FROM KPS_ORGEH_TB WHERE ORGEH = A.ORGEH)),' '), (select ORGTX from KPS_ORGEH_TB where ORGEH = A.ORGEH)) AS ORGTXT, A.EMAIL, (SELECT ENAME FROM KPS_USER_TB WHERE PERNR = B.DECISION_#DEGREE_PERNR) AS DECISION_#DEGREE_PERNR, B.DECISION_#DEGREE_REASON FROM KPS_USER_TB A, KPS_VPN_USER_TB B WHERE A.PERNR = B.PERNR AND A.PERNR = ? AND B.DECISION_#DEGREE_PERNR = ? AND B.REQUEST_DEGREE = ?";
			String decisionDegreePernr = "DECISION_DEGREE_PERNR";
			String decisionDegreeReason = "DECISION_DEGREE_REASON";
			
			if (decisionStatus == 1 || decisionStatus == 5) {
				select_query = select_query.replaceAll("#DEGREE", "1ST");
				decisionDegreePernr = decisionDegreePernr.replaceAll("DEGREE", "1ST");
				decisionDegreeReason = decisionDegreeReason.replaceAll("DEGREE", "1ST");
			} else if (decisionStatus == 2 || decisionStatus == 6) {
				select_query = select_query.replaceAll("#DEGREE", "2ND");
				decisionDegreePernr = decisionDegreePernr.replaceAll("DEGREE", "2ND");
				decisionDegreeReason = decisionDegreeReason.replaceAll("DEGREE", "2ND");
			} else if (decisionStatus == 3 || decisionStatus == 7) {
				select_query = select_query.replaceAll("#DEGREE", "3RD");
				decisionDegreePernr = decisionDegreePernr.replaceAll("DEGREE", "3RD");
				decisionDegreeReason = decisionDegreeReason.replaceAll("DEGREE", "3RD");
			} else if (decisionStatus == 4 || decisionStatus == 8) {
				select_query = select_query.replaceAll("#DEGREE", "4TH");
				decisionDegreePernr = decisionDegreePernr.replaceAll("DEGREE", "4TH");
				decisionDegreeReason = decisionDegreeReason.replaceAll("DEGREE", "4TH");
			}
			
			logger.debug(select_query);
			
			stmt = con.prepareStatement(select_query);
			stmt.setInt(1, Integer.parseInt(applicantPernr));
			stmt.setInt(2, Integer.parseInt(decisionPernr));
			stmt.setInt(3, userMaxDegree);
			
			ResultSet rs = stmt.executeQuery();
			
			if (rs.next()) {
				if (getApplicantTeamManager(applicantPernr) == null && (decisionStatus == 1) && nextDecisionPernr != null) {
					logger.info("return false");
					return false;
				}
				
				logger.info("return true");
				
				return true;
			} else {
				logger.info("return false");
				return false;
			}
		} catch (ClassNotFoundException e1) {
			logger.error("ClassNotFoundException : ", e1);
		} catch (SQLException e1) {
			logger.error("SQLException : ", e1);
		} finally {
			close(stmt, con);
		}
		
		return false;
	}

	private String getNextDicisionMaker(String applicantPernr, String decisionPernr, String nextDicisionPernr, int degree) {
		//신청자의 팀장과 결제자가 같다면 다음 결제자는 정보보안담당자
		
		logger.debug("getNextDicisionMaker applicantPernr: " + applicantPernr + ", decisionPernr:" + decisionPernr + ", nextDicisionPernr:" + nextDicisionPernr + ", degree: " + degree);
		
		String applicantTeamManager = getApplicantTeamManager(applicantPernr);
		
		if (applicantTeamManager == null) {
			applicantTeamManager = getSecManagerPernr();
		}
		
		if (degree == 3 && applicantTeamManager.equals(decisionPernr)) {
			return getVpnAdminPernr();
		} else if (degree != 3 && applicantTeamManager.equals(decisionPernr) && (nextDicisionPernr == null || nextDicisionPernr.length() == 0)) {
			return getSecManagerPernr();
		}
		
		//결제자가 정보보안 담당자이면 다음 결제자는 정보화 전략 팀장
		if (getSecManagerPernr().equals(decisionPernr)) {
			return nextDicisionPernr;
		}
		
		//결제자가 정보화 전략 팀장이면 다음 결제가는 VPN 관리자
		if (decisionPernr.equals(nextDicisionPernr)) {
			return getVpnAdminPernr();
		}
		
		return null;
	}

	private String getExistSecManager() {
		Connection con = null;
		PreparedStatement stmt = null;
		
		try {
			con = getConnectionString();
			
			String select_query = "select PERNR from KPS_USER_TB where SECMANAGER = ?";
			stmt = con.prepareStatement(select_query);
			stmt.setString(1, "X");
			
			ResultSet rs = stmt.executeQuery();
			
			if (rs.next()) {
				return rs.getString("PERNR");
			}
		} catch (ClassNotFoundException e1) {
			return null;
		} catch (SQLException e1) {
			return null;
		} finally {
			close(stmt, con);
		}
		
		return null;
	}
	
	private String getUserRequestDegree(String pernr) {
		Connection con = null;
		PreparedStatement stmt = null;
		
		try {
			con = getConnectionString();
			
			String select_query = "select MAX(REQUEST_DEGREE) AS REQUEST_DEGREE from KPS_VPN_USER_TB where PERNR = ?";
			stmt = con.prepareStatement(select_query);
			stmt.setInt(1, Integer.parseInt(pernr));
			
			ResultSet rs = stmt.executeQuery();
			
			if (rs.next()) {
				return rs.getString("REQUEST_DEGREE");
			}
		} catch (ClassNotFoundException e1) {
			return null;
		} catch (SQLException e1) {
			return null;
		} finally {
			close(stmt, con);
		}
		
		return null;
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
			return -1;
		} catch (SQLException e1) {
			return -1;
		} finally {
			close(stmt, con);
		}
		
		return -1;
	}
	
	private String getUserEMail(String pernr) {
		Connection con = null;
		PreparedStatement stmt = null;
		
		try {
			con = getConnectionString();
			
			String select_query = "select EMAIL from KPS_USER_TB where PERNR = ?";
			stmt = con.prepareStatement(select_query);
			stmt.setInt(1, Integer.parseInt(pernr));
			
			ResultSet rs = stmt.executeQuery();
			
			if (rs.next()) {
				return rs.getString("EMAIL");
			}
		} catch (ClassNotFoundException e1) {
			return null;
		} catch (SQLException e1) {
			return null;
		} finally {
			close(stmt, con);
		}
		
		return null;
	}
	
	private String getApplicantDecisionStatus(String ApplicantPernr) {
		int userMaxDegree = getUserMaxRequestDegree(ApplicantPernr);
		
		Connection con = null;
		PreparedStatement stmt = null;
		
		try {
			con = getConnectionString();
			
			String select_query = "select DECISION_STATUS from KPS_VPN_USER_TB where PERNR = ? and REQUEST_DEGREE = ?";
			stmt = con.prepareStatement(select_query);
			stmt.setInt(1, Integer.parseInt(ApplicantPernr));
			stmt.setInt(2, userMaxDegree);
			
			ResultSet rs = stmt.executeQuery();
			
			if (rs.next()) {
				return "" + rs.getInt("DECISION_STATUS");
			}
		} catch (ClassNotFoundException e1) {
			return null;
		} catch (SQLException e1) {
			return null;
		} finally {
			close(stmt, con);
		}
		
		return null;
	}
	
	private String getSecManagerPernr() {
		Connection con = null;
		PreparedStatement stmt = null;
		
		try {
			con = getConnectionString();
			
			String select_query = "select PERNR from KPS_USER_TB where SECMANAGER = 'X'";
			stmt = con.prepareStatement(select_query);
			
			ResultSet rs = stmt.executeQuery();
			
			if (rs.next()) {
				return rs.getString("PERNR");
			}
		} catch (ClassNotFoundException e1) {
			return null;
		} catch (SQLException e1) {
			return null;
		} finally {
			close(stmt, con);
		}
		
		return null;
	}
	
	private String getVpnAdminPernr() {
		Connection con = null;
		PreparedStatement stmt = null;
		
		try {
			con = getConnectionString();
			
			String select_query = "select PERNR from KPS_USER_TB where VPNADMIN = 'X'";
			stmt = con.prepareStatement(select_query);
			
			ResultSet rs = stmt.executeQuery();
			
			if (rs.next()) {
				return rs.getString("PERNR");
			}
		} catch (ClassNotFoundException e1) {
			return null;
		} catch (SQLException e1) {
			return null;
		} finally {
			close(stmt, con);
		}
		
		return null;
	}
	
	private String get1stDicisionMaker(String applicantPernrn) {
		Connection con = null;
		PreparedStatement stmt = null;
		
		try {
			con = getConnectionString();
			
			String select_query = "select REDER_CODE from KPS_USER_TB where PERNR = ?";
			stmt = con.prepareStatement(select_query);
			stmt.setInt(1, Integer.parseInt(applicantPernrn));
			
			ResultSet rs = stmt.executeQuery();
			if (rs.next()) {
				String rederCode = rs.getString("REDER_CODE");
				if (rederCode == null || rederCode.length() == 0) {
					return getApplicantTeamManager(applicantPernrn);
				} else if (rederCode.equals("X")) {
					return "" + applicantPernrn;
				}
			}
		} catch (ClassNotFoundException e1) {
			logger.error("ClassNotFoundException : ", e1);
			return null;
		} catch (SQLException e1) {
			logger.error("SQLException : ", e1);
			return null;
		} finally {
			close(stmt, con);
		}
		
		return null;
	}
	
	private String getDicisionMakerEMail(String decisionPernrn) {
		Connection con = null;
		PreparedStatement stmt = null;
		
		try {
			con = getConnectionString();
			
			String select_query = "select EMAIL from KPS_USER_TB where PERNR = ?";
			stmt = con.prepareStatement(select_query);
			stmt.setInt(1, Integer.parseInt(decisionPernrn));
			
			ResultSet rs = stmt.executeQuery();
			
			if (rs.next()) {
				return rs.getString("EMAIL");
			}
		} catch (ClassNotFoundException e1) {
			return null;
		} catch (SQLException e1) {
			return null;
		} finally {
			close(stmt, con);
		}
		
		return null;
	}
	
	private String getApplicantTeamManager(String applicantPernr) {
		Connection con = null;
		PreparedStatement stmt = null;
		
		try {
			con = getConnectionString();
			
			String select_query = "select PERNR from KPS_USER_TB where ORGEH = (select ORGEH from KPS_USER_TB where PERNR = ?) AND REDER_CODE = 'X'";
			stmt = con.prepareStatement(select_query);
			stmt.setInt(1, Integer.parseInt(applicantPernr));
			
			ResultSet rs = stmt.executeQuery();
			
			if (rs.next()) {
				return "" + rs.getInt("PERNR");
			}
		} catch (ClassNotFoundException e1) {
			logger.error("ClassNotFoundException : " + e1);
			return null;
		} catch (SQLException e1) {
			logger.error("SQLException : " + e1);
			return null;
		} finally {
			close(stmt, con);
		}
		
		return null;
	}
	
	private static Timestamp getCurrentTimeStamp() {
		java.util.Date today = new java.util.Date();
		return new Timestamp(today.getTime());
	}
	
	private void jsonConverter(PrintWriter out, HttpServletResponse resp, List<Map<String, Object>> returnJsonMsg) {
		try {
			out.write(JSONConverter.jsonize(returnJsonMsg));
		} catch (JSONException e) {
			logger.debug("frodo user kps ui: cannot convert object to json string", e);
			try {
				resp.sendError(500);
			} catch (IOException e1) {
				logger.error("frodo user kps ui: cannot convert object to json string", e1);
			}
		}
	}
	
	private void jsonConverter(PrintWriter out, HttpServletResponse resp, Map<String, Object> returnJsonMsg) {
		try {
			out.write(JSONConverter.jsonize(returnJsonMsg));
		} catch (JSONException e) {
			logger.debug("frodo user kps ui: cannot convert object to json string", e);
			try {
				resp.sendError(500);
			} catch (IOException e1) {
				logger.error("frodo user kps ui: cannot convert object to json string", e1);
			}
		}
	}
	
	private String getDecision() {
		Bundle bundle = bc.getBundle();
		URL url = bundle.getEntry("/WEB-INF/vpn_grant.html");
		byte[] b = new byte[4096];
		InputStream is = null;
		StringBuilder sb = new StringBuilder();

		try {
			is = url.openStream();

			int len;
			while ((len = is.read(b)) != -1)
				sb.append(new String(b, 0, len, Charset.forName("utf-8")));
		} catch (Exception e) {
			logger.error("frodo user kps ui: cannot load index", e);
		} finally {
			if (is != null)
				try {
					is.close();
				} catch (IOException e) {
				}
		}

		return sb.toString();
	}

	/*
	private String getIndex() {
		Bundle bundle = bc.getBundle();
		URL url = bundle.getEntry("/WEB-INF/index.html");
		byte[] b = new byte[4096];
		InputStream is = null;
		StringBuilder sb = new StringBuilder();

		try {
			is = url.openStream();

			int len;
			while ((len = is.read(b)) != -1)
				sb.append(new String(b, 0, len, Charset.forName("utf-8")));
		} catch (Exception e) {
			logger.error("frodo user kps ui: cannot load index", e);
		} finally {
			if (is != null)
				try {
					is.close();
				} catch (IOException e) {
				}
		}

		return sb.toString();
	}
	*/
	
	private String getHtml(String htmlName) {
		Bundle bundle = bc.getBundle();
		URL url = bundle.getEntry("/WEB-INF/" + htmlName);
		byte[] b = new byte[4096];
		InputStream is = null;
		StringBuilder sb = new StringBuilder();

		try {
			is = url.openStream();

			int len;
			while ((len = is.read(b)) != -1)
				sb.append(new String(b, 0, len, Charset.forName("utf-8")));
		} catch (Exception e) {
			logger.error("frodo user kps ui: cannot load index", e);
		} finally {
			if (is != null)
				try {
					is.close();
				} catch (IOException e) {
				}
		}

		return sb.toString();
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
	
	private void sendFile(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		InputStream is = getInputStream(req);

		if (is == null)
			resp.sendError(404);
		else {
			resp.setHeader("Content-Type", MimeTypes.instance().getByFile(req.getPathInfo()));

			try {
				byte[] b = new byte[4096];
				int len;
				while ((len = is.read(b)) != -1)
					resp.getOutputStream().write(b, 0, len);

				resp.getOutputStream().flush();
			} catch (IOException e) {
				throw e;
			} finally {
				is.close();
			}
		}
	}
	
	private InputStream getInputStream(HttpServletRequest req) {
		String path = req.getPathInfo();

		if (path.contains("..")) {
			return null;
		}
		
		Bundle b = bc.getBundle();
		
		try {
			URL url = b.getEntry("/WEB-INF" + path);
			return url.openStream();
		} catch (Exception e) {
			logger.trace("kraken webconsole: cannot open bundle [{}] resource [{}]", b.getBundleId(),
					req.getRequestURI());
			return null;
		}
	}
	
	private User createLocalUser(String loginName, String userName, String orgUnitName, String eMail) {
		User user;
		OrganizationUnit unit = null;
		if (orgUnitName != null) {
			logger.trace("frodo core: org unit [{}] for login [{}]", orgUnitName, loginName);
			unit = orgUnitApi.findOrganizationUnitByName("localhost", orgUnitName);
			if (unit == null) {
				unit = new OrganizationUnit();
				unit.setName(orgUnitName);
				orgUnitApi.createOrganizationUnit("localhost", unit);

			}
		}

		user = new User();
		user.setOrgUnit(unit);
		user.setLoginName(loginName);
		user.setName(userName);
		user.setPassword(INIT_PASSWD);
		user.setCreated(new java.util.Date());
		user.setUpdated(new java.util.Date());
		user.setSourceType("local");
		user.setEmail(eMail);

		logger.trace("frodo auth ice: create local user [{}]", user.toString());

		domUserApi.createUser("localhost", user);
		
		return user;
	}
	
	private String sendMail(String decisionPernr, String applicantPernr) {
		logger.debug("sendMail => 메일 받을 사람 : " + decisionPernr + " 결재 요청한 사람 : " + applicantPernr);
        logger.debug("link=>" + "https://10.83.1.206/vpn/vpn_grant.html?applicantPernr=" + Base64.encodeString(applicantPernr) + "&decisionPernr=" + Base64.encodeString(decisionPernr));
		MailerConfig c = mailer.getConfig("frodo");
		if (c == null) {
			logger.warn("frodo user ui: frodo mailer config not found");
			return "no_config";
		}
		
		String dicisionMakerEMail = getDicisionMakerEMail(decisionPernr);
		HashMap<String, String> userInfo = getUserInfo(applicantPernr);
        HashMap<String, String> grantUserInfo = getDecisionUserInfo(decisionPernr);

		String body = getMailTemplate("mail.html");
		if (body == null) {
			logger.warn("frodo user ui: frodo mailer config not found");
			return "no_config";
		}
		
		body = body.replaceAll("#ORGEH", userInfo.get("ORGTXT"));
        //이름에 사번을 붙여서 보내도록 수정
		body = body.replaceAll("#NAME", userInfo.get("ENAME") + "(" + userInfo.get("PERNR") + ")");
		body = body.replaceAll("#LINK", "https://172.16.130.5/vpn/vpn_grant.html?applicantPernr=" + Base64.encodeString(applicantPernr) + "&decisionPernr=" + Base64.encodeString(decisionPernr));
		//#DEGREE

        logger.debug("link=>" + "https://172.16.130.5/vpn/vpn_grant.html?applicantPernr=" + Base64.encodeString(applicantPernr) + "&decisionPernr=" + Base64.encodeString(decisionPernr));

		int decisionStatus = Integer.parseInt(userInfo.get("DECISIONSTATUS"));
		
		String applicantTeamManager = getApplicantTeamManager(applicantPernr);
		
		if (applicantTeamManager == null) {
			applicantTeamManager = decisionPernr;
		}
		
		if (applicantTeamManager.equals(applicantPernr) && decisionStatus == 2) {
			decisionStatus = decisionStatus + 1;
		}
		
		body = body.replaceAll("#DEGREE", getApproveStatus(decisionStatus));

        body = body.replaceAll("#GRANT_NAME", grantUserInfo.get("ENAME") + "(" + grantUserInfo.get("PERNR") + ")");

        if (grantUserInfo.get("ORGTXT") == null) {
            body = body.replaceAll("#GRANT_ORGEH", "소속없음");
        } else {
            body = body.replaceAll("#GRANT_ORGEH", grantUserInfo.get("ORGTXT"));
        }

		
		Session session = mailer.getSession(c);
		MimeMessage msg = new MimeMessage(session);
		
		try {
			msg.setSentDate(new java.util.Date());
			msg.setFrom(new InternetAddress("vpn@kps.co.kr"));
			msg.setRecipient(RecipientType.TO, new InternetAddress(dicisionMakerEMail));
			msg.setSubject("업무용 VPN 사용승인 결재요청 (신청자: " + userInfo.get("ORGTXT") + " " + userInfo.get("ENAME") + "(" + userInfo.get("PERNR") + "))", "utf-8");
			msg.setContent(body, "text/html; charset=utf-8");
			threadPool.submit(new MailTask(msg));
		} catch (Exception e) {
			logger.warn("frodo user ui: email send error", e);
			return "invalid_email_address";
		}
		return "ok";
	}
	
	private String sendApproveMail(String applicantPernr, boolean updateYN) {
		MailerConfig c = mailer.getConfig("frodo");
		if (c == null) {
			logger.warn("frodo user ui: frodo mailer config not found");
			return "no_config";
		}
		
		HashMap<String, String> userInfo = getUserInfo(applicantPernr);
		
		String body = getMailTemplate("approve_mail.html");
		if (body == null) {
			logger.warn("frodo user ui: frodo mailer config not found");
			return "no_config";
		}
		
		body = body.replaceAll("#NAME", userInfo.get("ENAME"));
		body = body.replaceAll("#PERNR", userInfo.get("PERNR"));

        if (updateYN == false) {
            body = body.replaceAll("#PASSWD", INIT_PASSWD);
        } else {
            body = body.replaceAll("#PASSWD", PASSWD_INFO);
        }

        body = body.replaceAll("#STARTDATE", userInfo.get("STARTDATE"));
		body = body.replaceAll("#ENDDATE", userInfo.get("ENDDATE"));
		
		String applicantEMail = userInfo.get("EMAIL");
		
		Session session = mailer.getSession(c);
		MimeMessage msg = new MimeMessage(session);
		
		try {
			msg.setSentDate(new java.util.Date());
			msg.setFrom(new InternetAddress("vpn@kps.co.kr"));
			msg.setRecipient(RecipientType.TO, new InternetAddress(applicantEMail));
			msg.setSubject(MAIL_APPROVE_SUBJECT, "utf-8");
			msg.setContent(body, "text/html; charset=utf-8");

			threadPool.submit(new MailTask(msg));
		} catch (Exception e) {
			logger.warn("frodo user ui: email address format error", e);
			return "invalid_email_address";
		}
		
		return "ok";
	}
	
	private String sendRejectMail(String applicantPernr, int decisionStatus) {
		MailerConfig c = mailer.getConfig("frodo");
		if (c == null) {
			logger.warn("frodo user ui: frodo mailer config not found");
			return "no_config";
		}
		
		HashMap<String, String> userInfo = getUserRejectInfo(applicantPernr, decisionStatus);
		
		String body = getMailTemplate("reject_mail.html");
		if (body == null) {
			logger.warn("frodo user ui: frodo mailer config not found");
			return "no_config";
		}
		
		body = body.replaceAll("#NAME", userInfo.get("ENAME"));
		body = body.replaceAll("#REJECTNAME", userInfo.get("REJECTNAME"));
		body = body.replaceAll("#REJECTREASON", userInfo.get("REJECTREASON"));
		
		String applicantEMail = userInfo.get("EMAIL");
		
		Session session = mailer.getSession(c);
		MimeMessage msg = new MimeMessage(session);
		
		try {
			msg.setSentDate(new java.util.Date());
			msg.setFrom(new InternetAddress("vpn@kps.co.kr"));
			msg.setRecipient(RecipientType.TO, new InternetAddress(applicantEMail));
			msg.setSubject("VPN 사용 승인 반려", "utf-8");
			msg.setContent(body, "text/html; charset=utf-8");

			threadPool.submit(new MailTask(msg));
		} catch (Exception e) {
			logger.warn("frodo user ui: email address format error", e);
			return "invalid_email_address";
		}
		
		return "ok";
	}
	
	private class MailTask implements Runnable {
		private MimeMessage msg;

		public MailTask(MimeMessage msg) {
			this.msg = msg;
		}

		@Override
		public void run() {
			try {
				Transport.send(msg);
			} catch (MessagingException e) {
				logger.error("frodo user ui: frodo mailer config not found", e);
			}
		}
	}
	
	private HashMap<String, String> getUserInfo(String applicantPernr) {
		int userMaxDegree = getUserMaxRequestDegree(applicantPernr);
		
		HashMap<String, String> userInfo = new HashMap<String, String>();
		
		Connection con = null;
		PreparedStatement stmt = null;
		
		try {
			con = getConnectionString();
			
			String select_query = "SELECT A.ENAME, CONCAT(CONCAT((SELECT ORGTX FROM KPS_ORGEH_TB WHERE ORGEH = (SELECT PORGEH FROM KPS_ORGEH_TB WHERE ORGEH = A.ORGEH)),' '), (select ORGTX from KPS_ORGEH_TB where ORGEH = A.ORGEH)) AS ORGTXT, A.EMAIL, A.PERNR, B.START_DATE, B.END_DATE, B.DECISION_STATUS FROM KPS_USER_TB A, KPS_VPN_USER_TB B WHERE B.REQUEST_DEGREE = ? AND A.PERNR = B.PERNR AND A.PERNR = ?";
			stmt = con.prepareStatement(select_query);
			stmt.setInt(1, userMaxDegree);
			stmt.setInt(2, Integer.parseInt(applicantPernr));
			
			ResultSet rs = stmt.executeQuery();
			
			if (rs.next()) {
				userInfo.put("ENAME", rs.getString("ENAME"));
				userInfo.put("ORGTXT", rs.getString("ORGTXT"));
				userInfo.put("EMAIL", rs.getString("EMAIL"));
				userInfo.put("PERNR", rs.getString("PERNR"));
				userInfo.put("DECISIONSTATUS", "" + rs.getInt("DECISION_STATUS"));
				
				SimpleDateFormat format = new SimpleDateFormat("yyyy.MM.dd");
				
				Date startDate = rs.getDate("START_DATE");
				userInfo.put("STARTDATE", format.format(startDate));
				
				Date endDate = rs.getDate("END_DATE");
				userInfo.put("ENDDATE", format.format(endDate));
				
				return userInfo;
			} 			
		} catch (ClassNotFoundException e1) {
		} catch (SQLException e1) {
		} finally {
			close(stmt, con);
		}
		
		return null;
	}

    private HashMap<String, String> getDecisionUserInfo(String applicantPernr) {
        HashMap<String, String> userInfo = new HashMap<String, String>();

        Connection con = null;
        PreparedStatement stmt = null;

        try {
            con = getConnectionString();

            String select_query = "SELECT PERNR, ENAME, CONCAT(CONCAT((SELECT ORGTX FROM KPS_ORGEH_TB WHERE ORGEH = (SELECT PORGEH FROM KPS_ORGEH_TB WHERE ORGEH = A.ORGEH)),' '), (select ORGTX from KPS_ORGEH_TB where ORGEH = A.ORGEH)) AS ORGTXT, JIKWI FROM KPS_USER_TB A WHERE PERNR = ?";
            stmt = con.prepareStatement(select_query);
            stmt.setInt(1, Integer.parseInt(applicantPernr));

            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                userInfo.put("ENAME", rs.getString("ENAME"));
                userInfo.put("ORGTXT", rs.getString("ORGTXT"));
                userInfo.put("PERNR", rs.getString("PERNR"));

                return userInfo;
            }
        } catch (ClassNotFoundException e1) {
        } catch (SQLException e1) {
        } finally {
            close(stmt, con);
        }

        return null;
    }
	
	private HashMap<String, String> getUserRejectInfo(String applicantPernr, int decisionStatus) {
		HashMap<String, String> userInfo = new HashMap<String, String>();
		
		Connection con = null;
		PreparedStatement stmt = null;
		
		try {
			con = getConnectionString();
			
			String select_query = "SELECT A.ENAME, CONCAT(CONCAT((SELECT ORGTX FROM KPS_ORGEH_TB WHERE ORGEH = (SELECT PORGEH FROM KPS_ORGEH_TB WHERE ORGEH = A.ORGEH)),' '), (select ORGTX from KPS_ORGEH_TB where ORGEH = A.ORGEH)) AS ORGTXT, A.EMAIL, (SELECT ENAME FROM KPS_USER_TB WHERE PERNR = B.DECISION_DEGREE_PERNR) AS DECISION_DEGREE_PERNR, B.DECISION_DEGREE_REASON FROM KPS_USER_TB A, KPS_VPN_USER_TB B WHERE A.PERNR = B.PERNR AND A.PERNR = ?";
			String decisionDegreePernr = "DECISION_DEGREE_PERNR";
			String decisionDegreeReason = "DECISION_DEGREE_REASON";
			
			if (decisionStatus == 5) {
				select_query = select_query.replaceAll("DEGREE", "1ST");
				decisionDegreePernr = decisionDegreePernr.replaceAll("DEGREE", "1ST");
				decisionDegreeReason = decisionDegreeReason.replaceAll("DEGREE", "1ST");
			} else if (decisionStatus == 6) {
				select_query = select_query.replaceAll("DEGREE", "2ND");
				decisionDegreePernr = decisionDegreePernr.replaceAll("DEGREE", "2ND");
				decisionDegreeReason = decisionDegreeReason.replaceAll("DEGREE", "2ND");
			} else if (decisionStatus == 7) {
				select_query = select_query.replaceAll("DEGREE", "3RD");
				decisionDegreePernr = decisionDegreePernr.replaceAll("DEGREE", "3RD");
				decisionDegreeReason = decisionDegreeReason.replaceAll("DEGREE", "3RD");
			} else if (decisionStatus == 8) {
				select_query = select_query.replaceAll("DEGREE", "4TH");
				decisionDegreePernr = decisionDegreePernr.replaceAll("DEGREE", "4TH");
				decisionDegreeReason = decisionDegreeReason.replaceAll("DEGREE", "4TH");
			}
			
			logger.debug(select_query);
			
			stmt = con.prepareStatement(select_query);
			stmt.setInt(1, Integer.parseInt(applicantPernr));
			
			ResultSet rs = stmt.executeQuery();
			
			if (rs.next()) {
				userInfo.put("ENAME", rs.getString("ENAME"));
				userInfo.put("ORGTXT", rs.getString("ORGTXT"));
				userInfo.put("EMAIL", rs.getString("EMAIL"));
				
				userInfo.put("REJECTNAME", rs.getString(decisionDegreePernr));
				userInfo.put("REJECTREASON", rs.getString(decisionDegreeReason));
				
				return userInfo;
			} 			
		} catch (ClassNotFoundException e1) {
		} catch (SQLException e1) {
		} finally {
			close(stmt, con);
		}
		
		return null;
	}
	
	private String getMailTemplate(String mailHtml) {
		Bundle bundle = bc.getBundle();
		URL url = bundle.getEntry("/WEB-INF/" + mailHtml);
		byte[] b = new byte[4096];
		InputStream is = null;
		StringBuilder sb = new StringBuilder();

		try {
			is = url.openStream();

			int len;
			while ((len = is.read(b)) != -1)
				sb.append(new String(b, 0, len, Charset.forName("utf-8")));
		} catch (Exception e) {
			logger.error("frodo user kps ui: cannot load index", e);
		} finally {
			if (is != null)
				try {
					is.close();
				} catch (IOException e) {
				}
		}

		return sb.toString();
	}
	
	public static java.util.Date getExpireDate(String date) throws ParseException {
		SimpleDateFormat format = new SimpleDateFormat("yyyy.MM.dd HH:mm:ss");
		String strExpireDate = date + " 23:59:59";
		java.util.Date expireDate =  new java.util.Date(format.parse(strExpireDate).getTime());
		return expireDate;
	}
	
	private int[] readRange(HttpServletRequest req) {
		try {
			String range = req.getHeader("Range");
			logger.info(range);
			range = range.substring(range.indexOf("=") + 1);
			String[] strArr = range.split("-");
			int[] result = new int[2];		
			for(int i = 0; i < 2; i++) {
				result[i] = Integer.parseInt(strArr[i]);			
			}
			return result;
		} catch(Exception e) {
			logger.error("frodo user kps ui: cannot read range header.", e);
		}
		return null;
	}
	
	private int selectCountOfVpnUserList(HttpServletResponse resp) throws IOException  {
		Connection con = null;
		int count = 0;
		try {
			con = getConnectionString();
			String selectCountQuery = "select count(*) from KPS_USER_TB A, KPS_VPN_USER_TB B where A.PERNR = B.PERNR order by A.PERNR, B.REQUEST_DEGREE desc";
			PreparedStatement stmt = con.prepareStatement(selectCountQuery);
			ResultSet rs = stmt.executeQuery();
			if(rs.next())
				count = rs.getInt(1);
		} catch (ClassNotFoundException e) {
			logger.error("frodo user kps ui: ", e);
			resp.sendError(500);
			return -1;
		} catch (SQLException e) {
			logger.error("frodo user kps ui: ", e);
			return -1;
		} finally {
			if(con != null) {
				try {
					con.close();
				} catch (SQLException e) {
					logger.error("frodo user kps ui: ", e);
					return -1;
				}
			}
		}		
		return count;
	}
	
	private void retrieveVpnUserList(HttpServletResponse resp, String select_query, ArrayList<Map<String, Object>> vpnUserList) throws IOException {
		Connection con = null;
		PreparedStatement stmt = null;
		try{
			con = getConnectionString();
			stmt = con.prepareStatement(select_query);
			
			ResultSet rs = stmt.executeQuery();
			
			Map<String, Object> userInfo;
			while (rs.next()) {
				userInfo = new HashMap<String, Object>();
				
				int applicantPernr = rs.getInt("PERNR");
				userInfo.put("PERNR", "" + applicantPernr);
				userInfo.put("ENAME", rs.getString("ENAME"));
				userInfo.put("ORGTXT", rs.getString("ORGTXT"));
				userInfo.put("JIKWINM", rs.getString("JIKWINM"));
				userInfo.put("REQUEST_DEGREE", rs.getInt("REQUEST_DEGREE"));
				
				Date requestDate = rs.getDate("REQUEST_DATE");
				SimpleDateFormat format = new SimpleDateFormat("yyyy.MM.dd");
				userInfo.put("REQUEST_DATE", format.format(requestDate));
				
				int decisionStatus = rs.getInt("DECISION_STATUS");
				
				String applicantTeamManager = getApplicantTeamManager(""+applicantPernr);
				
				if (applicantTeamManager == null) {
					applicantTeamManager = getSecManagerPernr();
				}
				
				if (applicantTeamManager.equals(""+applicantPernr) && (decisionStatus == 3)) {
					decisionStatus = decisionStatus + 1;
				}
				
				if (getSecManagerPernr().equals(""+applicantPernr) && (decisionStatus == 2)) {
					decisionStatus = decisionStatus + 2;
				}
				
				userInfo.put("DECISION_STATUS", getApproveStatus(decisionStatus));
				
				vpnUserList.add(userInfo);
			}
		} catch (ClassNotFoundException e1) {
			logger.error("frodo user kps ui: cannot convert object to json string", e1);
			resp.sendError(500);
		} catch (SQLException e1) {
			logger.error("frodo user kps ui: cannot convert object to json string", e1);
			resp.sendError(500);
		} finally {
			close(stmt, con);
		}
	}
	
	private void searchVpnUserList(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		setHttpServletResponseHeader(resp);
		PrintWriter out = resp.getWriter();
		
		String decisionStatus = req.getParameter("DECISION_STATUS");
		String requestDegree = req.getParameter("REQUEST_DEGREE");
		String orgtxt = req.getParameter("ORGTXT");
		String pernr = req.getParameter("PERNR");
		String ename = req.getParameter("ENAME");
		
		Map<String, Object> returnJsonMsg = new HashMap<String, Object>();		
		
		if(pernr != null) {	//To check this value has character. It only can has numbers.
			try {
				Integer.parseInt(pernr);
			} catch(NumberFormatException e) {
				logger.error("frodo user kps ui: pernr should number.", e);
				return;
			}
		}
		
		String select_query = "select * from (select A.PERNR, A.ENAME, CONCAT(CONCAT((SELECT ORGTX FROM KPS_ORGEH_TB WHERE ORGEH = (SELECT PORGEH FROM KPS_ORGEH_TB WHERE ORGEH = A.ORGEH)),' '), (select ORGTX from KPS_ORGEH_TB where ORGEH = A.ORGEH)) AS ORGTXT, A.JIKWINM, B.REQUEST_DEGREE, B.REQUEST_DATE, B.DECISION_STATUS "
				+ "from KPS_USER_TB A, KPS_VPN_USER_TB B where A.PERNR = B.PERNR";
		String searchCond = "";
		if(decisionStatus != null) {
			if(decisionStatus.equals("4"))
				searchCond += " and (B.DECISION_STATUS = 4 or (B.DECISION_STATUS = 3 and A.REDER_CODE = 'X'))";
			else if(decisionStatus.equals("3"))
				searchCond += " and (A.REDER_CODE is null and B.DECISION_STATUS = 3)";
			else
				searchCond += " and B.DECISION_STATUS = " + decisionStatus;
		}
		if(requestDegree != null)
			searchCond += " and B.REQUEST_DEGREE = " + requestDegree;
		if(pernr != null)
			searchCond += " and B.PERNR = " + pernr;
		if(ename != null){
			byte[] decoded = Base64.decode(ename);
			ename = new String(decoded, "UTF-8");
			searchCond += " and A.ENAME like '%" + ename +"%'";
		}
		
		if(searchCond.length() > 0)
			select_query += searchCond + " ) C ";
		else
			select_query += " ) C "; 
		
		if(orgtxt != null) {
			byte[] decoded = Base64.decode(orgtxt);
			orgtxt = new String(decoded, "UTF-8");
			select_query += " WHERE ORGTXT like '%" + orgtxt + "%'";
		}
		select_query += " order by REQUEST_DATE desc";	//신청리자 순 정렬.
		
		//for restful service.
		int[] range = readRange(req);
		if(range != null) {
			//인자 Range 값의 두번째는 인덱스 번호지만 sql에서 사용되야할 값은 row의 개수 이므로. 
			int totalCount = getSearchingVpnUserCount(resp, select_query);
			int count = range[1] - range[0] + 1;
			select_query += " limit " + range[0] + ", " + count;
			if(totalCount == -1)
				return;
			String contentRange = String.format("items %d-%d/%d", range[0], range[1], totalCount);
			resp.setHeader("Content-Range", contentRange);
		}
		
		logger.debug("frodo user kps ui: search query statement. [{}]", select_query);
		
		ArrayList<Map<String, Object>> vpnUserList = new ArrayList<Map<String, Object>>();
		retrieveVpnUserList(resp, select_query, vpnUserList);

		jsonConverter(out, resp, vpnUserList);
	}
	
	private int getSearchingVpnUserCount(HttpServletResponse resp, String select_query) throws IOException {
		String selectCountQuery = select_query.replace("*", "count(*)");
		Connection con = null;
		int count = 0;
		try {
			con = getConnectionString();
			PreparedStatement stmt = con.prepareStatement(selectCountQuery);
			ResultSet rs = stmt.executeQuery();
			if(rs.next())
				count = rs.getInt(1);
		} catch (ClassNotFoundException e) {
			logger.error("frodo user kps ui: ", e);
			resp.sendError(500);
			return -1;
		} catch (SQLException e) {
			logger.error("frodo user kps ui: ", e);
			return -1;
		} finally {
			if(con != null) {
				try {
					con.close();
				} catch (SQLException e) {
					logger.error("frodo user kps ui: ", e);
					return -1;
				}
			}
		}		
		return count;
	}
}
