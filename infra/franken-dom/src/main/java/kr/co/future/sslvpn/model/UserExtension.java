package kr.co.future.sslvpn.model;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import kr.co.future.api.CollectionTypeHint;
import kr.co.future.api.FieldOption;
import kr.co.future.api.PrimitiveConverter;
import kr.co.future.api.ReferenceKey;
import kr.co.future.confdb.CollectionName;
import kr.co.future.confdb.ConfigDatabase;
import kr.co.future.confdb.ConfigIterator;
import kr.co.future.confdb.ConfigService;
import kr.co.future.confdb.Predicate;
import kr.co.future.confdb.Predicates;
import kr.co.future.confdb.file.FileConfigService;
import kr.co.future.dom.model.User;
import kr.co.future.msgbus.Marshalable;

@CollectionName("users")
public class UserExtension implements Marshalable {
	
	public static Integer TWOWAY_AUTH_DEPRIVED = 1;
	
	@FieldOption(skip = true)
	private User user;

	@FieldOption(nullable = false)
	private String cid = UUID.randomUUID().toString();

	@FieldOption(name = "static_ip4", length = 20)
	private String staticIp4;

	@FieldOption(name = "allow_ip4_from", length = 20)
	private String allowIp4From;

	@FieldOption(name = "allow_ip4_to", length = 20)
	private String allowIp4To;

	@FieldOption(name = "is_locked", nullable = false)
	private boolean isLocked;
	
	@FieldOption(name = "login_failures", nullable = false)
	private int loginFailures;

	@FieldOption(name = "last_ip", length = 20)
	private String lastIp;

	// for npki IDN hash comparison (sha-256 max 64 chars)
	@FieldOption(name = "vid", length = 128)
	private String vid;

	// salt for idn hash, old default: loginname
	@FieldOption(name = "salt", length = 20)
	private String salt;

	// h(salt + idn)
	@FieldOption(name = "idn_hash", length = 64)
	private String idnHash;

	@FieldOption(name = "subject_dn")
	private String subjectDn;

	@FieldOption(name = "auth_key", length = 40)
	private String deviceAuthKey;

	@FieldOption(name = "device_key_count_setting", nullable = true)
	private Integer deviceKeyCountSetting;

	// expire date of account
	@FieldOption(name = "expire_at")
	private Date expireDateTime;

	@FieldOption(name = "start_at", nullable = true)
	private Date startDateTime;

	// expire date of auth key
	@FieldOption(name = "key_expire_at")
	private Date keyExpireDateTime;

	@FieldOption(name = "last_login_at")
	private Date lastLoginTime;

	@FieldOption(name = "last_logout_at")
	private Date lastLogoutTime;

	// sha256(salt + password)
	@FieldOption(name = "last_password_hash", nullable = true)
	private String lastPasswordHash;

	// last password change date
	@FieldOption(name = "last_password_change", nullable = true)
	private Date lastPasswordChange;

	@FieldOption(name = "created_at", nullable = false)
	private Date createDateTime = new Date();

	@FieldOption(name = "updated_at", nullable = false)
	private Date updateDateTime = new Date();

//	@ReferenceKey("guid")
	private AccessProfile profile;

	@FieldOption(name = "force_password_change", nullable = false)
	private boolean forcePasswordChange;

	@CollectionTypeHint(ClientIpRange.class)
	@FieldOption(name = "allow_ip_ranges", nullable = true)
	private List<ClientIpRange> allowIpRanges = new ArrayList<ClientIpRange>();

	@FieldOption(name = "last_password_fail_time", nullable = true)
	private Date lastPasswordFailTime;

	@FieldOption(name = "source_type", nullable = true)
	private String sourceType;
	
	@FieldOption(name = "cert_type", nullable = true)
	private String certType;
	
	@FieldOption(name = "is_auto_locked", nullable = true)
	private Boolean isAutoLocked;
	
	@FieldOption(name = "auto_lock_released_time", nullable = true)
	private Date autoLockReleasedTime;
	
	@FieldOption(name = "twoway_auth_status", nullable = true)
	private Integer twowayAuthStatus;
	
	@FieldOption(name = "allow_time_table_id", nullable = true)
	private String allowTimeTableId;
	
	public String getAllowTimeTableId() {
		return allowTimeTableId;
	}
	
	public void setAllowTimeTableId(String allowTimeTableId) {
		this.allowTimeTableId = allowTimeTableId;
	}
	
	public Integer getTwowayAuthStatus() {
		return twowayAuthStatus;
	}
	
	public void setTwowayAuthStatus(Integer twowayAuthStatus) {
		this.twowayAuthStatus = twowayAuthStatus;
	}
	
	public Date getAutoLockReleasedTime() {
		return autoLockReleasedTime;
	}
	
	public void setAutoLockReleasedTime(Date autoLockReleasedTime) {
		this.autoLockReleasedTime = autoLockReleasedTime;
	}
	
	public Boolean isAutoLocked() {
		return isAutoLocked;
	}

	public void setAutoLocked(Boolean isAutoLocked) {
		this.isAutoLocked = isAutoLocked;
	}
	
	public String getCertType() {
		return certType;
	}
	
	public void setCertType(String certType) {
		this.certType = certType;
	}
	
	public String getSourceType() {
		return sourceType;
	}

	public void setSourceType(String sourceType) {
		this.sourceType = sourceType;
	}

	public Date getLastPasswordFailTime() {
		return lastPasswordFailTime;
	}

	public void setLastPasswordFailTime(Date lastPasswordFailTime) {
		this.lastPasswordFailTime = lastPasswordFailTime;
	}

	public User getUser() {
		return user;
	}

	public void setUser(User user) {
		this.user = user;
	}

	public String getCid() {
		return cid;
	}

	public void setCid(String cid) {
		this.cid = cid;
	}

	public String getStaticIp4() {
		return staticIp4;
	}

	public void setStaticIp4(String staticIp4) {
		this.staticIp4 = staticIp4;
	}

	public String getAllowIp4From() {
		return allowIp4From;
	}

	public void setAllowIp4From(String allowIp4From) {
		this.allowIp4From = allowIp4From;
	}

	public String getAllowIp4To() {
		return allowIp4To;
	}

	public void setAllowIp4To(String allowIp4To) {
		this.allowIp4To = allowIp4To;
	}

	public boolean isLocked() {
		return isLocked;
	}

	public void setLocked(boolean isLocked) {
		this.isLocked = isLocked;
		if(isLocked == false && (isAutoLocked() == null ? false : isAutoLocked)) {
			isAutoLocked = null;								//계정잠금이 풀리면 자동 잠금도 해제해야함. 단순히 null처리함.
			setAutoLockReleasedTime(new Date());		//휴면계정잠금일 경우, 현재 시간부터 휴면계정잠금을 적용할 수 있도록 함.
		}
	}
	
	public int getLoginFailures() {
		return loginFailures;
	}

	public void setLoginFailures(int loginFailures) {
		this.loginFailures = loginFailures;
	}

	public String getLastIp() {
		return lastIp;
	}

	public void setLastIp(String lastIp) {
		this.lastIp = lastIp;
	}

	public String getVid() {
		return vid;
	}

	public void setVid(String vid) {
		this.vid = vid;
	}

	public String getSalt() {
		return salt;
	}

	public void setSalt(String salt) {
		this.salt = salt;
	}

	public String getIdnHash() {
		return idnHash;
	}

	public void setIdnHash(String idnHash) {
		this.idnHash = idnHash;
	}

	public String getDeviceAuthKey() {
		return deviceAuthKey;
	}

	public void setDeviceAuthKey(String deviceAuthKey) {
		this.deviceAuthKey = deviceAuthKey;
	}
	
	public Integer getAuthDeviceCount() {
		Predicate pred = Predicates.or(Predicates.field("is_authorized", true), Predicates.field("is_authorized", null));
		pred = Predicates.and(pred, Predicates.field("login_name", user.getLoginName()));
		
		ConfigService conf = null;
      try {
	      conf = new FileConfigService();
      } catch (IOException e) {
	      e.printStackTrace();
	      return null;
      }
		ConfigIterator it = null;
		try {
			ConfigDatabase db = conf.ensureDatabase("frodo");
			it = db.find(AuthorizedDevice.class, pred);
			return it.count();
		} finally {
			if (it != null)
				it.close();
		}
	}

	public Integer getDeviceKeyCount() {
		if (deviceAuthKey != null) {
			if (deviceKeyCountSetting == null)
				this.deviceKeyCountSetting = 1;
			else
				this.deviceKeyCountSetting = this.deviceKeyCountSetting + 1;

			this.deviceAuthKey = null;
		}
		if(deviceKeyCountSetting == null)
			return null;
		Integer deviceKeyCount = deviceKeyCountSetting - getAuthDeviceCount();		
		if(deviceKeyCount < 0)
			deviceKeyCount = 0;
		
		return deviceKeyCount;
	}
	
	public Integer getDeviceKeyCountSetting() {
		if(deviceKeyCountSetting == null)
			return 0;
		else
			return deviceKeyCountSetting;
	}
	
	public void setDeviceKeyCountSetting(Integer deviceKeyCountSetting) {
		if (deviceKeyCountSetting < 0)
			deviceKeyCountSetting = 0;
		this.deviceAuthKey = null;
		this.deviceKeyCountSetting = deviceKeyCountSetting;
	}

	public Date getExpireDateTime() {
		return expireDateTime;
	}

	public void setExpireDateTime(Date expireDateTime) {
		this.expireDateTime = expireDateTime;
	}

	public Date getStartDateTime() {
		return startDateTime;
	}

	public void setStartDateTime(Date startDateTime) {
		this.startDateTime = startDateTime;
	}

	public Date getKeyExpireDateTime() {
		return keyExpireDateTime;
	}

	public void setKeyExpireDateTime(Date keyExpireDateTime) {
		this.keyExpireDateTime = keyExpireDateTime;
	}

	public Date getLastLoginTime() {
		return lastLoginTime;
	}

	public void setLastLoginTime(Date lastLoginTime) {
		this.lastLoginTime = lastLoginTime;
	}

	public Date getLastLogoutTime() {
		return lastLogoutTime;
	}

	public void setLastLogoutTime(Date lastLogoutTime) {
		this.lastLogoutTime = lastLogoutTime;
	}

	public String getLastPasswordHash() {
		return lastPasswordHash;
	}

	public void setLastPasswordHash(String lastPasswordHash) {
		this.lastPasswordHash = lastPasswordHash;
	}

	public Date getLastPasswordChange() {
		return lastPasswordChange;
	}

	public void setLastPasswordChange(Date lastPasswordChange) {
		this.lastPasswordChange = lastPasswordChange;
	}

	public Date getCreateDateTime() {
		return createDateTime;
	}

	public void setCreateDateTime(Date createDateTime) {
		this.createDateTime = createDateTime;
	}

	public Date getUpdateDateTime() {
		return updateDateTime;
	}

	public void setUpdateDateTime(Date updateDateTime) {
		this.updateDateTime = updateDateTime;
	}

	public AccessProfile getProfile() {
		return profile;
	}

	public void setProfile(AccessProfile profile) {
		this.profile = profile;
	}

	public String getSubjectDn() {
		return subjectDn;
	}

	public void setSubjectDn(String subjectDn) {
		this.subjectDn = subjectDn;
	}

	public List<ClientIpRange> getAllowIpRanges() {
		if (allowIp4From != null && allowIp4To != null) {
			ClientIpRange range = new ClientIpRange();
			range.setIpFrom(allowIp4From);
			range.setIpTo(allowIp4To);
			allowIpRanges.add(range);
		}
		return allowIpRanges;
	}

	public void setAllowIpRanges(List<ClientIpRange> allowIpRanges) {
		if (allowIp4From != null && allowIp4To != null) {
			ClientIpRange range = new ClientIpRange();
			range.setIpFrom(allowIp4From);
			range.setIpTo(allowIp4To);
			allowIpRanges.add(range);
			this.allowIp4From = null;
			this.allowIp4To = null;
		}
		this.allowIpRanges = allowIpRanges;
	}

	// 패스워드 만료 기한을 초 단위로 반환
	public long getPasswordChangeDeadline(int expiryDays) {
		long expire = (expiryDays * 86400);
		if (lastPasswordChange == null)
			return expire;
		return expire - ((new Date().getTime() - lastPasswordChange.getTime()) / 1000);
	}

	public static boolean getPasswordResetable(User user, int loginMethod, boolean canChangeExternalPassword) {
		if (loginMethod == 2 || loginMethod == 4)
			return false;

		boolean resetable = false;
		String sourceType = user.getSourceType();

		// 만일 어디서 생성되었는지를 모른다면 패스워드의 유무룰 가지고 변경 가부를 결정한다.
		if (sourceType == null) {
			if (user.getPassword() != null)
				resetable = true;
		} else {
			// 어디서 생성되었는지를 안다면 변경이 허용된 타입인지 확인. 후에 추가(ldap 등)가 필요.
			if (sourceType.equals("local") || sourceType.equals("ldap"))
				resetable = true;
			else if (loginMethod == 6)	// LoginMethod_PW_OTP
				resetable = true;
			else if (sourceType.equals("external"))
				resetable = canChangeExternalPassword;
		}

		return resetable;
	}

	public boolean isForcePasswordChange() {
		return forcePasswordChange;
	}

	public void setForcePasswordChange(boolean forcePasswordChange) {
		this.forcePasswordChange = forcePasswordChange;
	}

	@Override
	public Map<String, Object> marshal() {
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ssZ");
		Map<String, Object> m = new HashMap<String, Object>();
		m.put("cid", cid);
		m.put("profile_id", profile == null ? null : profile.getGuid());
		m.put("static_ip4", staticIp4);
		m.put("allow_ip4_from", allowIp4From);
		m.put("allow_ip4_to", allowIp4To);
		m.put("allow_ip_ranges", PrimitiveConverter.serialize(getAllowIpRanges()));
		m.put("is_locked", isLocked);
		m.put("login_failures", loginFailures);
		m.put("last_ip", lastIp);
		m.put("device_auth_key", deviceAuthKey);
		m.put("device_key_count_setting", deviceKeyCountSetting);
		m.put("authorized_device_count", getAuthDeviceCount());
		m.put("has_vid", vid != null);
		m.put("has_idn_hash", idnHash != null);
		m.put("expire_at", expireDateTime == null ? null : dateFormat.format(expireDateTime));
		m.put("start_at", startDateTime == null ? null : dateFormat.format(startDateTime));
		m.put("key_expire_at", keyExpireDateTime == null ? null : dateFormat.format(keyExpireDateTime));
		m.put("last_login_at", lastLoginTime == null ? null : dateFormat.format(lastLoginTime));
		m.put("last_logout_at", lastLogoutTime == null ? null : dateFormat.format(lastLogoutTime));
		m.put("last_password_hash", lastPasswordHash);
		m.put("last_password_change", lastPasswordChange == null ? null : dateFormat.format(lastPasswordChange));
		m.put("created_at", dateFormat.format(createDateTime));
		m.put("updated_at", dateFormat.format(updateDateTime));
		m.put("subject_dn", subjectDn);
		m.put("force_password_change", forcePasswordChange);
		m.put("last_password_fail_time", lastPasswordFailTime);
		m.put("source_type", sourceType);
		m.put("cert_type", certType);
		m.put("is_auto_locked", isAutoLocked);
		m.put("auto_lock_released_time", autoLockReleasedTime);
		m.put("allow_time_table_id", allowTimeTableId);
		m.put("twoway_auth_status", twowayAuthStatus);
		
		return m;
	}

	@Override
	public String toString() {
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		return String.format("login_name=%s, is_locked=%s, last_login=%s, last_logout=%s, failures=%d", user.getLoginName(),
				isLocked, (lastLoginTime != null ? dateFormat.format(lastLoginTime) : null),
				(lastLogoutTime != null ? dateFormat.format(lastLogoutTime) : null), loginFailures);
	}
}
