package kr.co.future.sslvpn.core.login;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.Map;

import kr.co.future.sslvpn.core.AuthCode;
import kr.co.future.sslvpn.core.OsType;
import kr.co.future.sslvpn.core.pipeline.PipelineContextAdaptor;
import kr.co.future.sslvpn.model.AccessGateway;
import kr.co.future.sslvpn.model.AccessProfile;
import kr.co.future.sslvpn.model.UserExtension;

import org.bouncycastle.util.encoders.Base64;

import kr.co.future.dom.model.User;

import com.google.common.base.Strings;

public class LoginContext extends PipelineContextAdaptor {
	public LoginUtil loginUtil;
	private Map<String, Object> result;
	
	public Map<String, Object> props;
	
	private InetAddress remoteIp;
	private int remotePort;
	private InetSocketAddress remote;
	private int tunnelId;
	private String loginName;
	private String pw;
	private String idn;
	private String deviceKey;
	private String subjectDn;
	private String os_type;
	private String macAddress;
	private String hddSerial;
	private String remoteClientIp;

	// for NPKI, see
	// http://rootca.or.kr/kcac/down/1.5-Subscriber%20Identification%20Based%20on%20Virtual%20ID.pdf
	private byte[] r;
	private byte[] vid;
	private String issuerCN;
	private String crl;
	private String hashOid;
	private String subjectCn;

	private User user;
	private UserExtension userExt;
	private AccessGateway gw;
	private AccessProfile profile;
	private AuthCode authCode;
	private InetAddress leaseIp;
	private Integer loginMethod;
	private boolean isPwAuthOk;

	public LoginContext(LoginUtil loginUtil, Map<String, Object> props) throws UnknownHostException {
		this.result = null;
		this.loginUtil = loginUtil;
		this.props = props;		
		
		String strR = (String)props.get("r");
		if(!Strings.isNullOrEmpty(strR))
			props.put("r",Base64.decode(strR));
		String strVid = (String)props.get("vid");
		if(!Strings.isNullOrEmpty(strVid))
			props.put("vid", Base64.decode(strVid));
		
		loginUtil.removeEmptyR(props);
		loginUtil.traceAuth(props);
		
		// parse parameters
		this.remoteIp = InetAddress.getByName((String) props.get("remote_ip"));
		this.remotePort = (Integer) props.get("remote_port");
		this.remote = new InetSocketAddress(remoteIp, remotePort);
		this.tunnelId = (Integer) props.get("tunnel");
		this.loginName = (String) props.get("id");
		this.pw = (String) props.get("pw");
		this.idn = (String) props.get("idn");
		this.deviceKey = (String) props.get("device_key");
		this.subjectDn = (String) props.get("subject_dn");
		this.os_type = null;
		if (props.containsKey("os_type"))
			this.os_type = (OsType.FromType((Integer) props.get("os_type"))).toString();
		this.macAddress = (String) props.get("mac_address");
		this.hddSerial = (String) props.get("hdd_serial");
		this.remoteClientIp = (String) props.get("remote_client_ip");
		this.r = (byte[])props.get("r");
		this.vid = (byte[])props.get("vid");
		this.issuerCN = (String) props.get("issuer_cn");
		this.crl = (String) props.get("crl");
		this.hashOid = (String) props.get("hash_type");
		this.subjectCn = (String) props.get("subject_cn");
		
		isPwAuthOk = false;
	}
	
	public Map<String, Object> getResult() {
		return result;
	}

	public void setResult(Map<String, Object> result) {
		this.result = result;
	}
	
	public InetAddress getRemoteIp() {
		return remoteIp;
	}

	public void setRemoteIp(InetAddress remoteIp) {
		this.remoteIp = remoteIp;
	}
	
	public int getRemotePort() {
		return remotePort;
	}

	public void setRemotePort(int remotePort) {
		this.remotePort = remotePort;
	}
	
	public InetSocketAddress getRemote() {
		return remote;
	}

	public void setRemote(InetSocketAddress remote) {
		this.remote = remote;
	}
	
	public int getTunnelId() {
		return tunnelId;
	}

	public void setTunnelId(int tunnelId) {
		this.tunnelId = tunnelId;
	}
	
	public String getLoginName() {
		return loginName;
	}

	public void setLoginName(String loginName) {
		this.loginName = loginName;
	}
	
	public String getPw() {
		return pw;
	}

	public void setPw(String pw) {
		this.pw = pw;
	}
	
	public String getIdn() {
		return idn;
	}

	public void setIdn(String idn) {
		this.idn = idn;
	}
	
	public String getDeviceKey() {
		return deviceKey;
	}

	public void setDeviceKey(String deviceKey) {
		this.deviceKey = deviceKey;
	}
	
	public String getSubjectDn() {
		return subjectDn;
	}

	public void setSubjectDn(String subjectDn) {
		this.subjectDn = subjectDn;
	}
	
	public String getOs_type() {
		return os_type;
	}

	public void setOs_type(String os_type) {
		this.os_type = os_type;
	}
	
	public String getMacAddress() {
		return macAddress;
	}

	public void setMacAddress(String macAddress) {
		this.macAddress = macAddress;
	}
	
	public String getHddSerial() {
		return hddSerial;
	}

	public void setHddSerial(String hddSerial) {
		this.hddSerial = hddSerial;
	}
	
	public String getRemoteClientIp() {
		return remoteClientIp;
	}

	public void setRemoteClientIp(String remoteClientIp) {
		this.remoteClientIp = remoteClientIp;
	}
	
	public byte[] getR() {
		return r;
	}

	public void setR(byte[] r) {
		this.r = r;
	}
	
	public byte[] getVid() {
		return vid;
	}

	public void setVid(byte[] vid) {
		this.vid = vid;
	}
	
	public User getUser() {
		return user;
	}

	public void setUser(User user) {
		this.user = user;
	}
	
	public UserExtension getUserExt() {
		return userExt;
	}

	public void setUserExt(UserExtension userExt) {
		this.userExt = userExt;
	}
	
	public AccessGateway getGw() {
		return gw;
	}

	public void setGw(AccessGateway gw) {
		this.gw = gw;
	}
	
	public AccessProfile getProfile() {
		return profile;
	}

	public void setProfile(AccessProfile profile) {
		this.profile = profile;
	}
	
	public AuthCode getAuthCode() {
		return authCode;
	}

	public void setAuthCode(AuthCode authCode) {
		this.authCode = authCode;
	}
	
	public InetAddress getLeaseIp() {
		return leaseIp;
	}

	public void setLeaseIp(InetAddress leaseIp) {
		this.leaseIp = leaseIp;
	}
	
	public String getIssuerCN() {
		return issuerCN;
	}

	public void setIssuerCN(String issuerCN) {
		this.issuerCN = issuerCN;
	}
	
	public String getCrl() {
		return crl;
	}

	public void setCrl(String crl) {
		this.crl = crl;
	}
	
	public String getHashOid() {
		return hashOid;
	}

	public void setHashOid(String hashOid) {
		this.hashOid = hashOid;
	}
	
	public String getSubjectCn() {
		return subjectCn;
	}

	public void setSubjectCn(String subjectCn) {
		this.subjectCn = subjectCn;
	}
	
	public Integer getLoginMethod() {
		return loginMethod;
	}

	public void setLoginMethod(Integer loginMethod) {
		this.loginMethod = loginMethod;
	}
	
	public boolean isPwAuthOk() {
		return isPwAuthOk;
	}

	public void setPwAuthOk(boolean isPwAuthOk) {
		this.isPwAuthOk = isPwAuthOk;
	}
}
