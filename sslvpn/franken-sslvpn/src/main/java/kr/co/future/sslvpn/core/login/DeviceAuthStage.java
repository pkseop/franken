package kr.co.future.sslvpn.core.login;

import java.net.InetAddress;
import java.util.Date;

import kr.co.future.sslvpn.core.AuthCode;
import kr.co.future.sslvpn.core.OsType;
import kr.co.future.sslvpn.model.AccessGateway;
import kr.co.future.sslvpn.model.AccessProfile;
import kr.co.future.sslvpn.model.AuthorizedDevice;
import kr.co.future.sslvpn.model.UserExtension;
import kr.co.future.sslvpn.model.AccessGateway.DeviceAuthMode;
import kr.co.future.sslvpn.core.pipeline.BaseError;
import kr.co.future.sslvpn.core.pipeline.PipelineContext;
import kr.co.future.sslvpn.core.pipeline.Stage;
import kr.co.future.sslvpn.core.servlet.AuthServiceServlet;
import kr.co.future.dom.model.User;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DeviceAuthStage extends Stage{
	private final Logger logger = LoggerFactory.getLogger(AuthServiceServlet.class.getName());
	
	@Override
   public void doExecute(PipelineContext context) {
		LoginContext loginContext = (LoginContext)context;
		LoginUtil loginUtil = loginContext.loginUtil;
		
		String loginName = loginContext.getLoginName();
		User user = loginContext.getUser();
		AccessProfile profile = loginContext.getProfile();
		AccessGateway gw = loginContext.getGw();
		String deviceKey = loginContext.getDeviceKey();
		UserExtension ext = loginContext.getUserExt();
		InetAddress remoteIp = loginContext.getRemoteIp();
		String macAddress = loginContext.getMacAddress();
		String hddSerial = loginContext.getHddSerial();
		String remoteClientIp = loginContext.getRemoteClientIp();
		AuthCode authCode = loginContext.getAuthCode();
		
		try{
			boolean useDeviceAuth = gw.isUseDeviceAuth();
			
			if (profile.getDeviceAuthMode() != null) {
				useDeviceAuth = profile.getDeviceAuthMode() != DeviceAuthMode.None;
			}
			
			if (useDeviceAuth) {
				OsType osType = null;
				
				// ignore null device key
				if (deviceKey != null && !deviceKey.isEmpty() && !deviceKey.equals("0000000000000000000000000000000000000000")) {
					osType = OsType.FromType((Integer) loginContext.props.get("os_type"));
				}

				logger.trace("frodo core: device auth type={}", osType == null ? "null" : osType.toString());

				if (osType == null) {
					loginUtil.registerUnauthorizedDevice(user, profile, deviceKey, remoteIp, macAddress, hddSerial, remoteClientIp, osType);
					loginContext.setResult(loginUtil.fail(AuthCode.DeviceFail, null));
					return;
				} else if (!(osType == OsType.Windows || osType == OsType.Linux || osType == OsType.Android)) {
					loginUtil.registerUnauthorizedDevice(user, profile, deviceKey, remoteIp, macAddress, hddSerial, remoteClientIp, osType);
					loginContext.setResult(loginUtil.fail(AuthCode.DeviceFail, null));
					return;
				}

				if (!loginUtil.authDeviceApi.isAuthorized(deviceKey, loginName, profile)) {
					loginContext.setResult(loginUtil.fail(AuthCode.DeviceFail, profile));
					return;
				}

				Boolean tryReg = true;
				AuthorizedDevice authDevice = loginUtil.authDeviceApi.findDeviceByKey(deviceKey, loginName, osType.getCode(), profile);
				DeviceAuthMode deviceAuthMode = gw.getDeviceAuthMode();		//상속 처리.
				if (profile != null && profile.getDeviceAuthMode() != null)
					deviceAuthMode = profile.getDeviceAuthMode();
				
				if (authDevice != null) {
					if (!authDevice.getLoginName().equals(loginName)) {
						if (deviceAuthMode == DeviceAuthMode.OneToOne) {
							loginContext.setResult(loginUtil.fail(AuthCode.DeviceFail, profile));
							return;
						}
					} else {
						if (authDevice.isBlocked()) {
							loginContext.setResult(loginUtil.fail(AuthCode.DeviceLocked, profile));
							return;
						}
						if (isDeviceExpired(authDevice)) {
							loginContext.setResult(loginUtil.fail(AuthCode.DeviceExpired, profile));
							return;
						}
						
						tryReg = false;
					}
				}
				
				if (tryReg) {
					logger.trace("frodo core: try to register device. device key=[" + deviceKey + "]");
					
					//pks. 2014-12-19. 외부연동으로 사용할 경우 기본 단말키 1개를 주도록 함.
					if (authCode.toString().equals("RadiusSuccess") ||
						authCode.toString().equals("SqlSuccess") ||
						authCode.toString().equals("LdapSuccess")) {
						if (ext == null) {
							ext = loginUtil.createUserExtension(user, null);
						}
						
						if(ext.getDeviceKeyCountSetting() == null || ext.getDeviceKeyCountSetting() <= 0) {
							logger.trace("frodo core: set DeviceKeyCount to 1 in case of radius, sql, ldap Auth User using Device Auth");
							ext.setDeviceKeyCountSetting(1);
						}
					}
					else {				
						if (ext == null) {
							logger.trace("frodo core: user extension not found for login name [{}]", user.getLoginName());
							loginUtil.registerUnauthorizedDevice(user, profile, deviceKey, remoteIp, macAddress, hddSerial, remoteClientIp,
									osType);
							loginContext.setResult(loginUtil.fail(AuthCode.DeviceFail, profile));
							return;
						}
					}
					
					//To authenticate user at the first login by device.
					if(ext.getDeviceKeyCountSetting() == null)
						ext.setDeviceKeyCountSetting(1);

					Integer deviceCount = ext.getDeviceKeyCount();
					if (deviceCount == null || deviceCount == 0) {
						logger.trace("frodo core: user auth key not found for login name [{}]", user.getLoginName());
						loginUtil.registerUnauthorizedDevice(user, profile, deviceKey, remoteIp, macAddress, hddSerial, remoteClientIp,
								osType);
						loginContext.setResult(loginUtil.fail(AuthCode.DeviceFail, profile));
						return;
					}

					// check
					if (ext.getKeyExpireDateTime() != null && new Date().after(ext.getKeyExpireDateTime())) {
						logger.trace("frodo core: device key is expired, login_name=[" + user.getLoginName() + "]");
						loginUtil.registerUnauthorizedDevice(user, profile, deviceKey, remoteIp, macAddress, hddSerial, remoteClientIp,
								osType);
						loginContext.setResult(loginUtil.fail(AuthCode.DeviceFail, profile));
						return;
					}

					authDevice = new AuthorizedDevice();
					authDevice.setType(osType.getCode());
					authDevice.setDeviceKey(deviceKey);
					authDevice.setHostName(null);
					authDevice.setDescription(null);
					authDevice.setOwner(user.getName());
					if (macAddress != null && !macAddress.isEmpty())
						authDevice.setMacAddress(macAddress);
					if (hddSerial != null && !hddSerial.isEmpty())
						authDevice.setHddSerial(hddSerial);
					if (remoteClientIp != null && !remoteClientIp.isEmpty())
						authDevice.setRemoteClientip(remoteClientIp);
					authDevice.setRemoteIp(remoteIp.getHostAddress());
					authDevice.setBlocked(false);
					authDevice.setExpiration(null);
					authDevice.setLoginName(loginName);
					authDevice.setIsAuthorized(true);
					if(user.getOrgUnit() != null)
						authDevice.setOrgUnitName(user.getOrgUnit().getName());

					if (ext.getDeviceKeyCount() == null || ext.getDeviceKeyCount() == 0)
						ext.setKeyExpireDateTime(null);

					loginUtil.submitUpdate(ext.getUser());
					loginUtil.authDeviceApi.registerDevice(authDevice);
				}
			}
			
			loginContext.setUserExt(ext);
		} catch (Exception e) {
			BaseError error = new BaseError("login fail", "cannot login", e);
			loginContext.addError(error);
		}
	   
   }
	
	private boolean isDeviceExpired(AuthorizedDevice device) {
		Date expiration = device.getExpiration();
		if (expiration == null)
			return false;
		else {
			// expiration < current
			return device.getExpiration().compareTo(new Date()) < 0;
		}
	}
}
