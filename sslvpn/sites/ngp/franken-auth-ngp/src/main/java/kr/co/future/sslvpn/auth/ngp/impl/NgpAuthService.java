package kr.co.future.sslvpn.auth.ngp.impl;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketTimeoutException;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;

import kr.co.future.sslvpn.auth.BaseExternalAuthApi;
import kr.co.future.sslvpn.auth.ngp.NgpAuthApi;
import kr.co.future.sslvpn.auth.ngp.NgpConfig;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;

import kr.co.future.confdb.Config;
import kr.co.future.confdb.ConfigDatabase;
import kr.co.future.confdb.ConfigService;
import kr.co.future.dom.api.UserApi;
import kr.co.future.dom.model.User;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(name = "ngp-auth-service")
@Provides
public class NgpAuthService extends BaseExternalAuthApi implements NgpAuthApi {

	private static Socket socket;
	private static final int timeout = 1000;

	private final Logger logger = LoggerFactory.getLogger(NgpAuthService.class.getName());

	@Requires
	private ConfigService conf;

	@Requires
	private UserApi domUserApi;

	private NgpConfig config;

	@Override
	public Object login(Map<String, Object> props) {
		String loginName = (String) props.get("id"); // 사용자 ID
		String password = (String) props.get("pw"); // 비밀번호
		String deviceKey = (String) props.get("device_key"); // 디바이스키
		
		config = getConfig();

		logger.debug("deviceKey: " + deviceKey);
		
		// tcp, 21000 번 포트로 소켓통신
		String STX = "\02"; // 시작코드
		String interfaceId = "IF-003"; // 인터페이스 ID
		String num = "001"; // 순번
		String transTime = currentTimeToString("yyyyMMddkkmmss"); // 전송일시
		String recordCount = "0001"; // 레코드 수
		String id = loginName; // 사용자 ID
		String pw = password; // 비밀번호
		String data = id + "|" + pw + "|" + deviceKey; // 데이터
		String dataLength = paddingZero(data); // 데이터길이 0 패딩
		String ETX = "\03"; // 종료코드
		String CR = "\15";
		String LF = "\12";
		
		// send data
		byte[] sendData = handleSendData(STX, interfaceId, num, transTime, recordCount, data, dataLength, ETX, CR, LF);
		logger.debug("frodo-auth-ngp: send data [{}]", new String(sendData));

		// Connection Socket
		String serverName = config.getUrl();
		int port = config.getPort();
		byte[] receive = null;
		Map<String, Object> m = new HashMap<String, Object>();
		try {
			receive = login(serverName, port, sendData);
		} catch (SocketTimeoutException ste) {
//			try {
//				receive = login(serverName, port, sendData);
//			} catch (SocketTimeoutException e) {
//				logger.error("frodo-auth-ngp: login error", e);
//				m.put("auth_code", 12);
//				return m;
//			} catch (Exception e) {
//				logger.error("frodo-auth-ngp: login error", e);
//			}
			logger.error("frodo-auth-ngp: login error", ste);
			m.put("auth_code", 12);
			return m;
		} catch (Exception e) {
			logger.error("frodo-auth-ngp: login error", e);
		}

		// receive data
		Integer verify = handleReceiveData(receive);
		
		if (verify == 1) {
			m.put("name", id);
			logger.trace("frodo-auth-ngp: verify success, name=[{}]", id);
			m.put("auth_code", 0);
			return m;

		} else {
			User user = domUserApi.getUser("localhost", loginName);
			Map<String, Object> ext = user.getExt();
			@SuppressWarnings("unchecked")
			Map<String, Object> frodo = (Map<String, Object>) ext.get("frodo");

			if (frodo == null) {
				logger.debug("frodo-auth-ngp: extension frodo [{}]", frodo);
				domUserApi.removeUser("localhost", loginName);
			} else {
				Date lastLoginAt = (Date) frodo.get("last_login_at");
				logger.debug("frodo-auth-ngp: last login at [{}]", lastLoginAt);

				if (lastLoginAt.toString() == null) {
					domUserApi.removeUser("localhost", loginName);
				}
			}
			m.put("auth_code", 2);
			logger.trace("frodo-auth-ngp: ldap verify password fail");
			return m;
		}
	}
	
	private byte[] login(String serverName, int port, byte[] sendData) throws Exception {
		Socket socket = openSocket(serverName, port, 10);
		writeToServer(socket, sendData);
		byte[] receive = readFromServer(socket);
		socket.close();
		return receive;
	}

	@Override
	public Map<String, Object> verifyUser(String loginName) {

		Map<String, Object> m = new HashMap<String, Object>();

		if (loginName != null) {
			m.put("name", loginName);
			m.put("result", true);
		} else {
			m.put("result", false);
		}

		return m;
	}

	@Override
	public String getIdn(String loginName) {
		return null;
	}

	@Override
	public void changePassword(String account, String newPassword) {
	}

	@Override
	public boolean isPasswordChangeSupported() {
		return false;
	}

	@Override
	public boolean isPasswordExpirySupported() {
		return false;
	}

	@Override
	public long getPasswordExpiry(String loginName) {
		return 0;
	}

	private String currentTimeToString(String string) {
		Date date = new Date();
		SimpleDateFormat format = new SimpleDateFormat("yyyyMMddkkmmss");
		return format.format(date);
	}

	private Integer handleReceiveData(byte[] data) {
		String receive = new String(data);
		
		if (receive == null || receive == "") return 0;
		logger.debug("frodo-auth-ngp: receive data [{}]", receive);

		// String STX = receive.substring(0, 1); // 시작코드
		// String interfaceId = receive.substring(1, 7); // 인터페이스 ID
		// String num = receive.substring(7, 10); // 순번
		// String transTime = receive.substring(10, 24); // 전송일시
		// String dataLength = receive.substring(24, 31); // 데이터길이 0 패딩
		Integer recordCount = Integer.valueOf(receive.substring(31, 35)); // 레코드수
		//Integer verify = Integer.valueOf(receive.substring(35, 36)); // 인증여부
		// String ETX = receive.substring(36, 37); // 종료코드

		if (recordCount > 0) {
			return 1;
		} else {
			return 0;
		}
	}

	private byte[] handleSendData(String STX, String interfaceId, String num, String transTime, String recordNumber, String data,
			String dataLength, String ETX, String CR, String LF) {
		String dataString = STX + interfaceId + num + transTime + dataLength + recordNumber + data + ETX + CR + LF;
		return dataString.getBytes();
	}

	private String paddingZero(String data) {
		DecimalFormat df = new DecimalFormat("0000000");
		String dataLength = df.format(data.length());
		return dataLength;
	}

	private Socket openSocket(String server, int port, int time) throws Exception {
		// socket 생성
		try {

			InetAddress inetAddress = InetAddress.getByName(server);
			SocketAddress socketAddress = new InetSocketAddress(inetAddress, port);

			socket = new Socket();
			socket.setSoTimeout(timeout * time);
			socket.connect(socketAddress, timeout * time);
			return socket;
		} catch (SocketTimeoutException ste) {

			System.err.println("Timed out waiting for the socket");
			ste.printStackTrace();
			throw ste;
		}

	}

	private void writeToServer(Socket socket, byte[] data) {
		OutputStream os = null;
		try {
			os = socket.getOutputStream();
			os.write(data);
			os.flush();
		} catch (IOException e) {
			logger.error("frodo-auth-ngp: write to socket data error", e);
		}
	}

	private byte[] readFromServer(Socket socket) {

		InputStream is = null;
		ByteArrayOutputStream baos = null;
		try {
			is = socket.getInputStream();
			baos = new ByteArrayOutputStream();
			byte[] buf = new byte[1024];
			int length = 0;
			if ((length = is.read(buf)) != -1) {
				baos.write(buf, 0, length);
			}
			baos.flush();
			return baos.toByteArray();
		} catch (IOException e) {
			logger.error("frodo-auth-ngp: read from socket data error", e);
		}

		return null;
	}

	@Override
	public void setConfig(NgpConfig config) {
		ConfigDatabase db = conf.ensureDatabase("ngp");
		Config c = db.findOne(NgpConfig.class, null);
		if (c != null) {
			db.update(c, config);
		} else {
			db.add(config);
		}

		this.config = config;

	}

	@Override
	public NgpConfig getConfig() {
		ConfigDatabase db = conf.ensureDatabase("ngp");
		Config c = db.findOne(NgpConfig.class, null);
		if (c != null)
			return c.getDocument(NgpConfig.class);
		return null;
	}
}
