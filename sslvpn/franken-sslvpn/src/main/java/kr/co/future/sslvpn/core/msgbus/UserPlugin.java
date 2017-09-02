package kr.co.future.sslvpn.core.msgbus;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.charset.Charset;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import kr.co.future.api.PrimitiveConverter;
import kr.co.future.confdb.Config;
import kr.co.future.confdb.Predicate;
import kr.co.future.confdb.Predicates;
import kr.co.future.dom.api.AdminApi;
import kr.co.future.dom.api.ConfigManager;
import kr.co.future.dom.api.DOMException;
import kr.co.future.dom.api.OrganizationUnitApi;
import kr.co.future.dom.model.Admin;
import kr.co.future.dom.model.OrganizationUnit;
import kr.co.future.dom.model.ProgramProfile;
import kr.co.future.dom.model.Role;
import kr.co.future.dom.model.User;
import kr.co.future.msgbus.MsgbusException;
import kr.co.future.msgbus.Request;
import kr.co.future.msgbus.Response;
import kr.co.future.msgbus.handler.MsgbusMethod;
import kr.co.future.msgbus.handler.MsgbusPermission;
import kr.co.future.msgbus.handler.MsgbusPlugin;
import kr.co.future.sslvpn.core.GlobalConfig;
import kr.co.future.sslvpn.core.GlobalConfigApi;
import kr.co.future.sslvpn.core.UserLimitService;
import kr.co.future.sslvpn.core.impl.SaltGenerator;
import kr.co.future.sslvpn.model.AccessProfile;
import kr.co.future.sslvpn.model.AuthorizedDevice;
import kr.co.future.sslvpn.model.ClientIpRange;
import kr.co.future.sslvpn.model.OrgUnitExtension;
import kr.co.future.sslvpn.model.UserExtension;
import kr.co.future.sslvpn.model.api.AccessProfileApi;
import kr.co.future.sslvpn.model.api.AuthorizedDeviceApi;
import kr.co.future.sslvpn.model.api.UserApi;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Requires;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(name = "frodo-user-plugin")
@MsgbusPlugin
public class UserPlugin {
	private static final String LOCALHOST = "localhost";

	private Logger logger = LoggerFactory.getLogger(UserPlugin.class);

	@Requires
	private OrganizationUnitApi orgUnitApi;

	@Requires
	private kr.co.future.dom.api.UserApi domUserApi;

	@Requires
	private UserApi userApi;

	@Requires
	private AccessProfileApi profileApi;

	@Requires
	private AdminApi adminApi;

	@Requires
	private AuthorizedDeviceApi deviceApi;

	@Requires
	private ConfigManager cfg;

	@Requires
	private UserLimitService userLimitApi;

	@Requires
	private GlobalConfigApi gcApi;

	@MsgbusMethod
	@MsgbusPermission(group = "frodo", code = "user_edit")
	public void resetVids(Request req, Response resp) {
		String requestName = req.getAdminLoginName();
		@SuppressWarnings("unchecked")
		List<String> loginNames = (List<String>) req.get("login_names");

		Admin admin = adminApi.findAdmin(req.getOrgDomain(), req.getAdminLoginName());
		if (admin == null)
			throw new MsgbusException("frodo", "admin-not-found");

		Collection<User> users = domUserApi.getUsers(req.getOrgDomain(), loginNames);
		Map<String, UserExtension> extMap = toExtensionMap(userApi.getUserExtensions(users));

		Map<String, String> failedList = new HashMap<String, String>();
		for (User u : users) {
			if (admin.getRole().getLevel() == 2) {
				if (requestName.equals(u.getLoginName())) {
					UserExtension ext = extMap.get(u.getLoginName());
					if (ext == null)
						continue;

					ext.setVid(null);

					userApi.setUserExtension(ext);
				} else if (adminApi.canManage(LOCALHOST, admin, u)) {
					UserExtension ext = extMap.get(u.getLoginName());
					if (ext == null)
						continue;

					ext.setVid(null);
					
					userApi.setUserExtension(ext);
				} else {
					failedList.put(u.getLoginName(), "no-permission");
				}

				continue;
			}

			if (!adminApi.canManage(req.getOrgDomain(), admin, u)) {
				failedList.put(u.getLoginName(), "no-permission");
				continue;
			}

			UserExtension ext = extMap.get(u.getLoginName());
			if (ext != null) {
				ext.setVid(null);
				userApi.setUserExtension(ext);
			}
		}

		resp.put("failed_list", failedList);
	}

	@MsgbusMethod
	public void getOrgUnitExtension(Request req, Response resp) {
		String orgUnitId = req.getString("org_unit_id");
		OrgUnitExtension ext = profileApi.findOrgUnitExtension(orgUnitId);
		resp.put("ext", ext == null ? null : ext.marshal());
	}

	@MsgbusMethod
	public void setOrgUnitExtension(Request req, Response resp) {
		String orgUnitId = req.getString("org_unit_id");
		String profileId = req.getString("profile_id");
		boolean recursive = req.has("recursive") && req.getBoolean("recursive");
		profileApi.setOrgUnitExtension(orgUnitId, profileId, recursive);
	}

	@MsgbusMethod
	public void getUserExtensions(Request req, Response resp) {
		String orgUnitId = req.getString("org_unit_id");
        String filter = "";

		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ssZ");

		Predicate pred = null;
		if (req.has("filter_name") || req.has("filter_login_name")) {
			String name = req.getString("filter_name");
			String loginName = req.getString("filter_login_name");

            if (name.length() > 0)
                filter = name;
            if (loginName.length() > 0)
                filter = loginName;

			pred = new Matched(name, loginName);
		}

		int offset = 0;
		int limit = Integer.MAX_VALUE;

		if (req.has("offset"))
			offset = req.getInteger("offset");
		if (req.has("limit"))
			limit = req.getInteger("limit");

		int total = domUserApi.countUsers(LOCALHOST, orgUnitId, true, filter, false);

		Collection<User> users = domUserApi.getUsers(LOCALHOST, orgUnitId, true, filter, offset, limit, false);

		if (logger.isTraceEnabled())
			logger.trace("frodo core: fetched users {}, total {}", users.size(), total);

		Map<String, UserExtension> extMap = toExtensionMap(userApi.getUserExtensions(users));
		ArrayList<Object> l = new ArrayList<Object>(Math.min(users.size(), limit));
		for (User user : users)
			l.add(toVpnUser(dateFormat, user, extMap));

		resp.put("users", l);
		resp.put("total", total);
	}

	private static class Matched implements Predicate {
		private String userName;
		private String loginName;

		public Matched(String userName, String loginName) {
			this.userName = userName;
			this.loginName = loginName;
		}

		@Override
		public boolean eval(Config c) {
			@SuppressWarnings("unchecked")
			Map<String, Object> m = (Map<String, Object>) c.getDocument();

			String name = (String) m.get("name");
			String login = (String) m.get("login_name");

			if (name.contains(userName))
				return true;
			if (login.contains(loginName))
				return true;

			return false;
		}
	}

	private Object toVpnUser(SimpleDateFormat dateFormat, User user, Map<String, UserExtension> extMap) {
		UserExtension ext = extMap.get(user.getLoginName());
		OrganizationUnit orgUnit = user.getOrgUnit();

		Map<String, Object> m = new HashMap<String, Object>();
		m.put("user_id", user.getLoginName());
		m.put("org_unit_id", orgUnit == null ? null : orgUnit.getGuid());
		m.put("org_unit_name", orgUnit == null ? null : orgUnit.getName());
		m.put("name", user.getName());
		m.put("login_name", user.getLoginName());
		m.put("title", user.getTitle());
		m.put("force_password_change", false);

		AccessProfile profile = profileApi.determineProfile(user);
		if (ext != null) {
			m.put("profile", ext.getProfile() == null ? null : ext.getProfile().getName());
			m.put("determine_profile", profile == null ? null : profile.getName());
			m.put("is_locked", ext.isLocked());
			m.put("is_auto_locked", ext.isAutoLocked() == null ? false : ext.isAutoLocked());
			m.put("expire_at", ext.getExpireDateTime() == null ? null : dateFormat.format(ext.getExpireDateTime()));
			m.put("start_at", ext.getStartDateTime() == null ? null : dateFormat.format(ext.getStartDateTime()));
			m.put("last_ip", ext.getLastIp());
			m.put("last_login_at", ext.getLastLoginTime() == null ? null : dateFormat.format(ext.getLastLoginTime()));
			m.put("last_logout_at", ext.getLastLogoutTime() == null ? null : dateFormat.format(ext.getLastLogoutTime()));
			m.put("device_auth_key", ext.getDeviceAuthKey());
			m.put("key_expire_at", ext.getKeyExpireDateTime() == null ? null : dateFormat.format(ext.getKeyExpireDateTime()));
		} else {
			m.put("profile", null);
			m.put("determine_profile", profile == null ? null : profile.getName());
			m.put("is_locked", false);
			m.put("is_auto_locked", false);
			m.put("expire_at", null);
			m.put("start_at", null);
			m.put("last_ip", null);
			m.put("last_login_at", null);
			m.put("last_logout_at", null);
			m.put("device_auth_key", null);
			m.put("key_expire_at", null);
		}

		return m;
	}

	private Map<String, UserExtension> toExtensionMap(List<UserExtension> exts) {
		Map<String, UserExtension> m = new HashMap<String, UserExtension>();
		for (UserExtension e : exts)
			m.put(e.getUser().getLoginName(), e);
		return m;
	}

	@MsgbusMethod
	public void getUserExtension(Request req, Response resp) {
		String loginName = req.getString("login_name");

		UserExtension ext = userApi.findUserExtension(loginName);
		if (ext == null)
			resp.put("user", null);
		else {
			Map<String, Object> m = ext.marshal();
            Map<String, Object> profile2 = (Map<String, Object>)m.get("profile");

			m.put("device_key_count", ext.getDeviceKeyCount());		//pks: 2013-12-08. The "device_key_count" does't included db table. To show it put it to the map instance.
			AccessProfile profile = profileApi.determineProfile(loginName);
			m.put("determine_profile_id", (profile != null) ? profile.getGuid() : null);

			resp.put("user", m);
		}
	}

	@MsgbusMethod
	@MsgbusPermission(group = "frodo", code = "user_edit")
	public void importUsers(Request req, Response resp) {
		logger.info("frodo core: begin csv import users, thread [{}]", Thread.currentThread().getId());
		long start = new Date().getTime();

		SimpleDateFormat[] sdfs = { new SimpleDateFormat("yyyy-MM-dd HH:mm") };
		List<Map<String, String>> datas = null;
		
		try {
			datas = parseCsv(req.getString("csv"), req.getString("charset"));
		} catch (IOException e) {
			throw new MsgbusException("frodo", "invalid csv");
		}

		Map<String, List<String>> invalids = new HashMap<String, List<String>>();
		List<User> create = new ArrayList<User>();
		List<User> update = new ArrayList<User>();

		Map<String, OrganizationUnit> orgUnits = new HashMap<String, OrganizationUnit>();
		for (OrganizationUnit ou : orgUnitApi.getOrganizationUnits(LOCALHOST))
			orgUnits.put(ou.getName(), ou);

		for (Map<String, String> data : datas) {
			String deptNames = data.get("dept");
			if (deptNames == null)
				continue;

			String[] splitDeptNames = deptNames.split("#");

			String parentGuid = null;
			for (String deptName : splitDeptNames) {
				OrganizationUnit orgUnit = null;
				if (!orgUnits.containsKey(deptName)) {
					orgUnit = new OrganizationUnit();
					orgUnit.setName(deptName);
					orgUnit.setParent(parentGuid);

					logger.trace("frodo core: importing org unit [{}] for data [{}] dept name [{}, {}]", new Object[] { orgUnit,
							data, deptNames.length(), deptNames.hashCode() });
					orgUnitApi.createOrganizationUnit(LOCALHOST, orgUnit);
				} else {
					orgUnit = orgUnits.get(deptName);
					String parent = orgUnit.getParent();
					if ((parent == null && parentGuid != null) || (parent != null && parentGuid == null)
							|| (parent != null && parentGuid != null && !parent.equals(parentGuid))) {
						orgUnit.setParent(parentGuid);
						orgUnitApi.updateOrganizationUnit(LOCALHOST, orgUnit);
					}
				}
				orgUnits.put(orgUnit.getName(), orgUnit);
				parentGuid = orgUnit.getGuid();
			}
		}

		int count = 0;
		int userLimit = userLimitApi.getUserLimit();
		int availableUserCount = userLimit - domUserApi.getAllUserCount();
		logger.debug("frodo core: available user=[{}] / limit=[{}]", availableUserCount, userLimit);
		
		for (Map<String, String> data : datas) {
			List<String> invalid = new ArrayList<String>();

			String loginName = data.get("login_name");
			User user = domUserApi.findUser(LOCALHOST, loginName);
			
			boolean isUserUpdate = false;
			
			if (user == null) {
				if (!data.containsKey("name")) {
					invalid.add("name");
					invalids.put(loginName, invalid);
					logger.error("frodo core: user create failed [{}]", loginName);
					continue;
				}
				user = new User();
				user.setLoginName(loginName);
				user.setName(data.get("name"));
				user.setPassword(data.get("password"));
			} else {
				update.add(user);
				isUserUpdate = true;
				if (data.containsKey("password"))
					user.setPassword(domUserApi.hashPassword(null, data.get("password")));
			}

			if (data.containsKey("dept")) {
				String deptName = null;
				for (String dept : data.get("dept").toString().split("#")) {
					deptName = dept;
				}
				user.setOrgUnit(orgUnits.get(deptName));
			}
			if (data.containsKey("name"))
				user.setName(data.get("name"));
			if (data.containsKey("description"))
				user.setDescription(data.get("description"));
			if (data.containsKey("title"))
				user.setTitle(data.get("title"));
			if (data.containsKey("email")) {
				String email = data.get("email");
				if (email.matches(".+@.+\\..+"))
					user.setEmail(email);
				else
					invalid.add("email");
			}
			if (data.containsKey("phone"))
				user.setPhone(data.get("phone"));
			if(data.containsKey("source_type"))
				user.setSourceType(data.get("source_type"));

			UserExtension ext = userApi.getUserExtension(user);
			if (ext == null) {
				ext = new UserExtension();
				ext.setUser(user);
				ext.setSalt(SaltGenerator.createSalt(20));
			}

			String useDevice = data.get("use_device");
			if (useDevice != null && useDevice.toLowerCase().equals("true")) {
				if (canGenerate(ext, toDeviceMap(deviceApi.getDevices()), user.getLoginName())) {
					ext.setDeviceKeyCountSetting(ext.getDeviceKeyCountSetting() == null ? 1 : ext.getDeviceKeyCountSetting() + 1);
					setDeviceKeyExpireTime(ext);
				}
			} else if (useDevice != null && !useDevice.toLowerCase().equals("false"))
				invalid.add("use_device");

			if (data.containsKey("static_ip")) {
				try {
					InetAddress.getByName(data.get("static_ip"));
					ext.setStaticIp4(data.get("static_ip"));
				} catch (UnknownHostException e) {
					invalid.add("static_ip");
				}
			}
			if (data.containsKey("allow_ip_ranges")) {
				String requestAllowIpRanges = data.get("allow_ip_ranges");
				try {
					List<ClientIpRange> allowIpRanges = new ArrayList<ClientIpRange>();
					for (String allowIpRange : requestAllowIpRanges.trim().split(" ")) {
						if (allowIpRange.trim().isEmpty())
							continue;
						String[] range = allowIpRange.split("-");
						InetAddress.getByName(range[0]);
						InetAddress.getByName(range[1]);
						ClientIpRange ipRange = new ClientIpRange();
						ipRange.setIpFrom(range[0]);
						ipRange.setIpTo(range[1]);
						allowIpRanges.add(ipRange);
					}
					ext.setAllowIpRanges(allowIpRanges);
				} catch (UnknownHostException e) {
					invalid.add("allow_ip_ranges");
				}
			}
			if (data.containsKey("allow_ip_from")) {
				try {
					InetAddress.getByName(data.get("allow_ip_from"));
					ext.setAllowIp4From(data.get("allow_ip_from"));
				} catch (UnknownHostException e) {
					invalid.add("allow_ip_from");
				}
			}
			if (data.containsKey("allow_ip_to")) {
				try {
					InetAddress.getByName(data.get("allow_ip_to"));
					ext.setAllowIp4To(data.get("allow_ip_to"));
				} catch (UnknownHostException e) {
					invalid.add("allow_ip_to");
				}
			}
			if (data.containsKey("is_locked"))
				ext.setLocked(Boolean.parseBoolean(data.get("is_locked")));
			if (data.containsKey("idn")) {
				String idn = data.get("idn");
				if (idn.matches("\\d{6}-\\d{7}") || idn.matches("\\d{13}"))
					ext.setIdnHash(domUserApi.hashPassword(ext.getSalt(), data.get("idn").replace("-", "")));
				else
					invalid.add("idn");
			}
			
			if(data.containsKey("start")) {
				Date startDate = null;
				for (SimpleDateFormat sdf : sdfs)
					startDate = dateParse(data.get("start"), sdf);
				if (startDate != null)
					ext.setStartDateTime(startDate);
				else
					invalid.add("start");
			}
			
			if (data.containsKey("expire")) {
				Date expire = null;
				for (SimpleDateFormat sdf : sdfs)
					expire = dateParse(data.get("expire"), sdf);
				if (expire != null)
					ext.setExpireDateTime(expire);
				else
					invalid.add("expire");
			}
			if(data.containsKey("access_profile")) {
				String str = (String)data.get("access_profile");
				String[] arr = str.split("\\|");
				AccessProfile profile = profileApi.getAccessProfile(arr[1]);
				if(profile != null)
					ext.setProfile(profile);
			}
			if(data.containsKey("admin_role") && data.containsKey("admin_profile")) {
				Role role = new Role();
				role.setName(data.get("admin_role"));
				
				ProgramProfile profile = new ProgramProfile();
				profile.setName(data.get("admin_profile"));
								
				Admin admin = new Admin();
				admin.setUseLoginLock(true);
				admin.setUseIdleTimeout(true);
				admin.setLoginLockCount(3);
				admin.setIdleTimeout(3600);
				admin.setRole(role);
				admin.setProfile(profile);
				admin.setEnabled(true);
				admin.setUseAcl(false);
				
				user.getExt().put("admin", admin);
			}
			if(data.containsKey("device_key_count_setting")) {
				String deviceKeyCountSetting = data.get("device_key_count_setting");
				if(deviceKeyCountSetting != null) {
					ext.setDeviceKeyCountSetting(Integer.parseInt(deviceKeyCountSetting));
				}
			}
			
			if(data.containsKey("last_login_at")) {
				String lastLoginAt = data.get("last_login_at");
				if(lastLoginAt != null && !lastLoginAt.equals("")) { 
					Date lastLoginTime = dateParse(data.get("last_login_at"), sdfs[0]);
					ext.setLastLoginTime(lastLoginTime);
				}
			}
			
			if(data.containsKey("last_logout_at")) {
				String lastLogoutAt = data.get("last_logout_at");
				if(lastLogoutAt != null && !lastLogoutAt.equals("")) { 
					Date lastLogoutTime = dateParse(data.get("last_logout_at"), sdfs[0]);
					ext.setLastLogoutTime(lastLogoutTime);
				}
			}
			
			user.getExt().put(userApi.getExtensionName(), ext);

			if (invalid.isEmpty()) {
				if (!isUserUpdate) {
					if (count < availableUserCount) {
						create.add(user);
						count++;
					} else {
						invalid.add("user_limit");
						invalids.put(user.getLoginName(), invalid);
						if (logger.isDebugEnabled())
							logger.debug("invalid user [{}]", user.getLoginName());
					}
				}
				continue;
			}

			logger.error("frodo core: ignored user [{}] ext columns [{}]", loginName, invalid);
			invalids.put(user.getLoginName(), invalid);
		}

		domUserApi.createUsers("localhost", create);
		domUserApi.updateUsers("localhost", update, false);

		long end = new Date().getTime();

		if (logger.isTraceEnabled())
			logger.trace("frodo core: import [{}] user, [{}] milliseconds elapse", datas.size(), end - start);

		resp.put("invalid", invalids);
	}

	private boolean canGenerate(UserExtension ext, Map<String, AuthorizedDevice> deviceMap, String loginName) {
		return !deviceMap.containsKey(loginName);
	}

	private Date dateParse(String source, SimpleDateFormat sdf) {
		try {
			return sdf.parse(source);
		} catch (ParseException e) {
			return null;
		}
	}

	private List<Map<String, String>> parseCsv(String csv, String charset) throws IOException {
		List<Map<String, String>> result = new ArrayList<Map<String, String>>();
		String[] header = null;
		String decoded = new String(decodeBase64(csv), Charset.forName(charset));
		for (String line : decoded.split("\n")) {
			line = line.replace("\r", "");
			if (line.trim().isEmpty())
				continue;
			String[] values = parseCsvLine(line);
			if (header == null)
				header = values;
			else {
				try {
					Map<String, String> m = new HashMap<String, String>();
					for (int i = 0; i < header.length; i++) {
						if (!values[i].trim().isEmpty())
							m.put(header[i], values[i].trim());
					}
					if (!m.containsKey("login_name")) {
						logger.error("frodo core: login_name not found [{}]", line);
						continue;
					}
					result.add(m);
				} catch (Exception e) {
					logger.error("frodo core: user import failed [{}]", line);
				}
			}
		}
		return result;
	}

	private static String[] parseCsvLine(String str) throws IOException {
		List<String> result = new ArrayList<String>();
		StringBuilder sb = new StringBuilder();
		char[] c = str.toCharArray();
		boolean quote = false;
		boolean begin = false;
		for (int i = 0; i < c.length; i++) {
			if ((quote && c[i] == '"' && (lookup(c, i + 1) == -1 || c[lookup(c, i + 1)] == ',')) || (!quote && c[i] == ',')) {
				if (quote) {
					i = lookup(c, i + 1);
					if (i == -1)
						i = c.length;
				}
				result.add(sb.toString());
				sb = new StringBuilder();
				begin = false;
				quote = false;
				continue;
			}

			if (begin) {
				if (c[i] == '"') {
					if (c[i + 1] == '"') {
						sb.append('"');
						i++;
					} else {
						if (!quote || c[lookup(c, i + 1)] != ',')
							throw new IOException("invalid format, index " + i);
					}
				} else
					sb.append(c[i]);
			} else {
				begin = true;
				if (c[lookup(c, i)] == '"') {
					i = lookup(c, i);
					quote = true;
				} else
					sb.append(c[i]);
			}
		}
		if (begin)
			result.add(sb.toString());
		if (str.endsWith(","))
			result.add("");
		return result.toArray(new String[0]);
	}

	private static int lookup(char[] c, int index) {
		try {
			while (true) {
				if (c[index] != ' ')
					return index;
				index++;
			}
		} catch (IndexOutOfBoundsException e) {
			return -1;
		}
	}

	private static byte[] decodeMap = new byte[128];
	static {
		int i = 0;
		byte b = 'A';
		for (; i < 26; i++)
			decodeMap[b++] = (byte) i;
		b = 'a';
		for (; i < 52; i++)
			decodeMap[b++] = (byte) i;
		b = '0';
		for (; i < 62; i++)
			decodeMap[b++] = (byte) i;
		decodeMap['+'] = 62;
		decodeMap['/'] = 63;
	}

	private byte[] decodeBase64(String src) {
		char[] ch = src.toCharArray();

		int len = ch.length / 4 * 3;
		if (ch[len - 1] == '=')
			len--;
		if (ch[len - 2] == '=')
			len--;

		byte[] result = new byte[len];
		for (int i = 0; i < ch.length / 4; i++) {
			byte a = decodeMap[ch[i * 4]];
			byte b = decodeMap[ch[i * 4 + 1]];
			byte c = decodeMap[ch[i * 4 + 2]];
			byte d = decodeMap[ch[i * 4 + 3]];

			long l = (a & 0x3F) << 18 | (b & 0x3F) << 12 | (c & 0x3F) << 6 | (d & 0x3F);
			for (int j = 2; j >= 0; j--) {
				if (i * 3 + j < result.length)
					result[i * 3 + j] = (byte) (l & 0xFF);
				l >>= 8;
			}
		}
		return result;
	}

//	@Deprecated
//	@SuppressWarnings("unchecked")
//	@MsgbusMethod
//	@MsgbusPermission(group = "frodo", code = "user_edit")
//	public void generateAuthKeys(Request req, Response resp) {
//		Admin admin = adminApi.getAdmin(req.getOrgDomain(), req.getAdminLoginName());
//
//		Map<String, UserExtension> extMap = toExtensionMap(userApi.getUserExtensions());
//		Map<String, AuthorizedDevice> deviceMap = toDeviceMap(deviceApi.getDevices());
//
//		List<Config> userConfigs = null;
//
//		long start = new Date().getTime();
//		String target = req.getString("target");
//		if (target != null && target.equals("no_key_and_no_device")) {
//			userConfigs = domUserApi
//					.getConfigs(LOCALHOST, null, true, new DeviceChecker(extMap, deviceMap), 0, Integer.MAX_VALUE);
//		} else {
//			if (req.get("login_names") != null) {
//				HashSet<String> loginNames = new HashSet<String>((List<String>) req.get("login_names"));
//				userConfigs = domUserApi.getConfigs(LOCALHOST, null, true, Predicates.in("login_name", loginNames), 0,
//						Integer.MAX_VALUE);
//			} else
//				userConfigs = domUserApi.getConfigs(LOCALHOST, null, true, null, 0, Integer.MAX_VALUE);
//		}
//
//		long end = new Date().getTime();
//		logger.trace("frodo core: time for search user [{}]", end - start);
//
//		start = new Date().getTime();
//		List<String> failedLoginNames = new ArrayList<String>();
//		List<ConfigUpdateRequest<User>> targets = new ArrayList<ConfigUpdateRequest<User>>();
//		PrimitiveParseCallback parseCallback = cfg.getParseCallback(LOCALHOST);
//		UserConfigParser userConfigParser = new UserConfigParser();
//		for (Config userConfig : userConfigs) {
//			User user = (User) userConfigParser.parse(userConfig.getDocument(), parseCallback);
//			if (!adminApi.canManage(LOCALHOST, admin, user)) {
//				failedLoginNames.add(user.getLoginName());
//				continue;
//			}
//
//			UserExtension ext = extMap.get(user.getLoginName());
//
//			if (ext == null) {
//				ext = new UserExtension();
//				ext.setUser(user);
//				ext.setCreateDateTime(new Date());
//				ext.setUpdateDateTime(new Date());
//			}
//
//			ext.setDeviceAuthKey(UUID.randomUUID().toString());
//			setDeviceKeyExpireTime(ext);
//			user.getExt().put("frodo", ext);
//
//			user.setUpdated(new Date());
//			targets.add(new ConfigUpdateRequest<User>(userConfig, user));
//		}
//		end = new Date().getTime();
//		logger.trace("frodo core: time for setting extension [{}]", end - start);
//
//		start = new Date().getTime();
//		domUserApi.updateUsers(LOCALHOST, targets, false);
//		end = new Date().getTime();
//
//		logger.trace("frodo core: time for update user[{}]", end - start);
//
//		logger.trace("frodo core: generated [{}] device auth key(s)", targets.size());
//		resp.put("failed_login_names", failedLoginNames);
//	}
//
//	private class DeviceChecker implements Predicate {
//
//		private Map<String, UserExtension> extMap;
//		private Map<String, AuthorizedDevice> deviceMap;
//
//		public DeviceChecker(Map<String, UserExtension> extMap, Map<String, AuthorizedDevice> deviceMap) {
//			this.extMap = extMap;
//			this.deviceMap = deviceMap;
//		}
//
//		@Override
//		public boolean eval(Config c) {
//			@SuppressWarnings("unchecked")
//			Map<String, Object> m = (Map<String, Object>) c.getDocument();
//			if (m == null)
//				return false;
//
//			String loginName = (String) m.get("login_name");
//
//			UserExtension ext = extMap.get(loginName);
//			return canGenerate(ext, deviceMap, loginName);
//		}
//
//	}

	private Map<String, AuthorizedDevice> toDeviceMap(List<AuthorizedDevice> devices) {
		Map<String, AuthorizedDevice> m = new HashMap<String, AuthorizedDevice>();

		for (AuthorizedDevice device : devices)
			m.put(device.getLoginName(), device);

		return m;
	}

//	@Deprecated
//	@MsgbusMethod
//	@MsgbusPermission(group = "frodo", code = "user_edit")
//	public void removeAuthKeys(Request req, Response resp) {
//		long start = new Date().getTime();
//		Admin admin = adminApi.getAdmin(req.getOrgDomain(), req.getAdminLoginName());
//		List<Config> userConfigs = null;
//		if (req.get("login_names") != null) {
//			@SuppressWarnings("unchecked")
//			Set<String> loginNames = new HashSet<String>((List<String>) req.get("login_names"));
//			userConfigs = domUserApi.getConfigs(LOCALHOST, null, true, Predicates.in("login_name", loginNames), 0,
//					Integer.MAX_VALUE);
//		} else
//			userConfigs = domUserApi.getConfigs(LOCALHOST, null, true, null, 0, Integer.MAX_VALUE);
//
//		List<ConfigUpdateRequest<User>> targets = new ArrayList<ConfigUpdateRequest<User>>();
//		List<String> failedUserNames = new ArrayList<String>();
//		PrimitiveParseCallback parseCallback = cfg.getParseCallback(LOCALHOST);
//		UserConfigParser userConfigParser = new UserConfigParser();
//		for (Config userConfig : userConfigs) {
//			User user = (User) userConfigParser.parse(userConfig.getDocument(), parseCallback);
//
//			if (!adminApi.canManage(LOCALHOST, admin, user)) {
//				failedUserNames.add(user.getLoginName());
//				continue;
//			}
//
//			UserExtension ext = userApi.getUserExtension(user);
//			if (ext != null) {
//				ext.setDeviceAuthKey(null);
//				ext.setKeyExpireDateTime(null);
//
//				user.setUpdated(new Date());
//
//				if (logger.isDebugEnabled())
//					logger.debug("frodo-core: removing auth key for user={}", user);
//
//				user.getExt().put("frodo", ext);
//				targets.add(new ConfigUpdateRequest<User>(userConfig, user));
//			}
//		}
//
//		domUserApi.updateUsers(LOCALHOST, targets, false);
//		if (logger.isDebugEnabled())
//			logger.debug("frodo core: [{}] user remove auth device key, [{}] milliseconds elapse", targets.size(),
//					new Date().getTime() - start);
//		resp.put("failed_login_names", failedUserNames);
//	}

	@MsgbusMethod
	@MsgbusPermission(group = "frodo", code = "user_edit")
	public void setDeviceKeyCount(Request req, Response resp) {
		Admin admin = adminApi.getAdmin(req.getOrgDomain(), req.getAdminLoginName());
		String loginName = req.getString("login_name");
		Integer count = req.getInteger("count");

		if (loginName == null)
			throw new MsgbusException("dom", "login-name-not-found");

		if (count == null)
			throw new MsgbusException("dom", "count-not-found");

		User user = domUserApi.getUser(LOCALHOST, loginName);

		if (!adminApi.canManage(LOCALHOST, admin, user))
			throw new MsgbusException("dom", "no-permission");

		UserExtension ext = userApi.getUserExtension(user);
		if (ext == null) {
			ext = new UserExtension();
			ext.setUser(user);
			ext.setCreateDateTime(new Date());
			ext.setUpdateDateTime(new Date());
		}

		ext.setDeviceKeyCountSetting(count);
		setDeviceKeyExpireTime(ext);
		userApi.setUserExtension(ext);
		resp.put("count", ext.getDeviceKeyCount());
		resp.put("count_setting", ext.getDeviceKeyCountSetting());
	}

	@MsgbusMethod
	@MsgbusPermission(group = "frodo", code = "user_edit")
	public void setDeviceKeyCounts(Request req, Response resp) {
		Integer requestCount = req.getInteger("count");
		if (requestCount == null)
			throw new MsgbusException("dom", "count-not-found");

		long start = new Date().getTime();
		// login_name, device count
//		Map<String, Integer> deviceCountMap = null;
		Collection<User> users = null;
		if (req.has("login_names")) {
			@SuppressWarnings("unchecked")
			HashSet<String> loginNames = new HashSet<String>((List<String>) req.get("login_names"));
//			Predicate pred = Predicates.or(Predicates.field("is_authorized", true), Predicates.field("is_authorized", null));
//			pred = Predicates.and(pred, Predicates.in("login_name", loginNames));
//			deviceCountMap = toDeviceCountMap(deviceApi.getDevices(0, Integer.MAX_VALUE, pred));
			users = domUserApi.getUsers(LOCALHOST, loginNames);
		} else {
//			deviceCountMap = toDeviceCountMap(deviceApi.getDevices());
			users = domUserApi.getUsers(LOCALHOST);
		}

		Date expireTime = null;
		GlobalConfig gc = gcApi.getGlobalConfig();
		if (gc != null && gc.getDeviceKeyExpiryDuration() != null) {
			Calendar c = Calendar.getInstance();
			c.setTime(new Date());
			c.add(Calendar.DAY_OF_MONTH, gc.getDeviceKeyExpiryDuration());
			expireTime = c.getTime();
		}

		List<User> targets = new ArrayList<User>();
		Map<String, String> failedList = new HashMap<String, String>();
		Admin admin = adminApi.getAdmin(req.getOrgDomain(), req.getAdminLoginName());
		for (User user : users) {
			if (!adminApi.canManage(LOCALHOST, admin, user)) {
				failedList.put(user.getLoginName(), "no-permission");
				continue;
			}

			@SuppressWarnings("unchecked")
			Map<String, Object> ext = (Map<String, Object>) user.getExt().get("frodo");
			if (ext == null) {
				ext = new HashMap<String, Object>();
				ext.put("cid", UUID.randomUUID().toString());
				ext.put("is_locked", false);
				ext.put("login_failures", 0);
				ext.put("created_at", new Date());
			}
			ext.put("updated_at", new Date());
			ext.put("device_key_count_setting", requestCount);
			ext.put("key_expire_at", expireTime);
			ext.put("auth_key", null);
			user.getExt().put("frodo", ext);

			user.setUpdated(new Date());
			targets.add(user);
		}

		long end = new Date().getTime();
		logger.trace("frodo core: time for set device key count [{}]", end - start);

		start = new Date().getTime();
		domUserApi.updateUsers(LOCALHOST, targets, false);
		end = new Date().getTime();
		logger.trace("frodo core: time for update user [{}]", end - start);

		resp.put("failed_list", failedList);
	}

//	private Map<String, Integer> toDeviceCountMap(List<AuthorizedDevice> devices) {
//		Map<String, Integer> countMap = new HashMap<String, Integer>();
//		for (AuthorizedDevice device : devices) {
//			String loginName = device.getLoginName();
//			if (countMap.containsKey(loginName))
//				countMap.put(loginName, countMap.get(loginName) + 1);
//			else
//				countMap.put(loginName, 1);
//		}
//		return countMap;
//	}

	@Deprecated
	@MsgbusMethod
	@MsgbusPermission(group = "frodo", code = "user_edit")
	public void generateAuthKey(Request req, Response resp) {
		Admin admin = adminApi.getAdmin(req.getOrgDomain(), req.getAdminLoginName());
		String loginName = req.getString("login_name");

		if (loginName == null)
			throw new MsgbusException("dom", "login-name-not-found");

		User user = domUserApi.getUser(LOCALHOST, loginName);

		if (!adminApi.canManage(LOCALHOST, admin, user))
			throw new MsgbusException("dom", "no-permission");

		UserExtension ext = userApi.findUserExtension(loginName);
		if (ext == null) {
			ext = new UserExtension();
			ext.setUser(user);
			ext.setCreateDateTime(new Date());
			ext.setUpdateDateTime(new Date());
		}

		ext.setDeviceAuthKey(UUID.randomUUID().toString());
		setDeviceKeyExpireTime(ext);

		userApi.setUserExtension(ext);

		resp.put("device_auth_key", ext.getDeviceAuthKey());
		resp.put("key_expire_at", ext.getKeyExpireDateTime());
	}

	private void setDeviceKeyExpireTime(UserExtension ext) {
		GlobalConfig gc = gcApi.getGlobalConfig();
		if (gc == null || gc.getDeviceKeyExpiryDuration() == null)
			ext.setKeyExpireDateTime(null);
		else {
			Calendar c = Calendar.getInstance();
			c.setTime(new Date());
			c.add(Calendar.DAY_OF_MONTH, gc.getDeviceKeyExpiryDuration());
			ext.setKeyExpireDateTime(c.getTime());
		}
	}

	@MsgbusMethod
	@MsgbusPermission(group = "frodo", code = "user_edit")
	public void setUserExtension(Request req, Response resp) {

		Admin admin = adminApi.getAdmin(req.getOrgDomain(), req.getAdminLoginName());
		String loginName = req.getString("login_name");

		try {
			UserExtension ext = userApi.findUserExtension(loginName, InetAddress.getByName(req.getString("static_ip4")));
			if (ext != null)
				throw new MsgbusException("frodo", "already used static ip");
		} catch (UnknownHostException e) {
		}

		String profileId = req.getString("profile_id");
		User user = domUserApi.getUser(req.getOrgDomain(), loginName);

		boolean ban = false;

		if (!adminApi.canManage(LOCALHOST, admin, user))
			ban = !req.getAdminLoginName().equals(loginName);

		if (ban)
			throw new MsgbusException("dom", "no-permission");

		UserExtension ext = userApi.getUserExtension(user);
		if (ext == null) {
            ext = new UserExtension();
        }

		if (ext.isLocked() && !req.getBoolean("is_locked"))
			ext.setLoginFailures(0);

		ext.setUser(user);
		ext.setStaticIp4(req.getString("static_ip4"));
		ext.setAllowIp4From(req.getString("allow_ip4_from"));
		ext.setAllowIp4To(req.getString("allow_ip4_to"));
		@SuppressWarnings("unchecked")
		Collection<Object> requestAllowIpRanges = (Collection<Object>) req.get("allow_ip_ranges");
		List<ClientIpRange> allowIpRanges = new ArrayList<ClientIpRange>();
		if (requestAllowIpRanges != null)
			allowIpRanges = (List<ClientIpRange>) PrimitiveConverter.parseCollection(ClientIpRange.class, requestAllowIpRanges);
		ext.setAllowIpRanges(allowIpRanges);
		ext.setLocked(req.getBoolean("is_locked"));
		if (ext.getSalt() == null)
			ext.setSalt(SaltGenerator.createSalt(20));
		if (req.has("idn") && req.getString("idn") != null)
			ext.setIdnHash(domUserApi.hashPassword(ext.getSalt(), req.getString("idn")));
		ext.setExpireDateTime(req.getDate("expire_at"));
		ext.setStartDateTime(req.getDate("start_at"));
		if (profileId != null) {
            ext.setProfile(profileApi.getAccessProfile(profileId));
        } else
			ext.setProfile(null);

		ext.setSubjectDn(req.getString("subject_dn"));
		ext.setForcePasswordChange(req.getBoolean("force_password_change"));
		ext.setCertType(req.getString("cert_type"));
        ext.setAllowTimeTableId(req.getString("allow_time_table_id"));
        ext.setTwowayAuthStatus(req.getInteger("twoway_auth_status"));

		userApi.setUserExtension(ext);
	}

	@MsgbusMethod
	@MsgbusPermission(group = "frodo", code = "user_edit")
	public void changeLockStatus(Request req, Response resp) {
		String loginName = req.getString("login_name");
		boolean isLocked = req.getBoolean("is_locked");

		Admin admin = adminApi.getAdmin(req.getOrgDomain(), req.getAdminLoginName());
		if (admin == null)
			throw new MsgbusException("frodo", "admin-not-found");

		User user = domUserApi.getUser("localhost", loginName);

		if (!adminApi.canManage(req.getOrgDomain(), admin, user)) {
			logger.trace("frodo core: user [{}] has not permission for change lock status user [{}]", admin.getUser()
					.getLoginName(), user.getLoginName());
			throw new MsgbusException("frodo", "no-permission");
		}

		UserExtension ext = userApi.findUserExtension(loginName);
		if (ext == null) {
			ext = new UserExtension();
			ext.setUser(user);
		}

		ext.setLocked(isLocked);
		ext.setLoginFailures(0);
		ext.setLastPasswordFailTime(null);
		userApi.setUserExtension(ext);
	}

	@MsgbusMethod
	@MsgbusPermission(group = "frodo", code = "user_edit")
	public void setForcePasswordChanges(Request req, Response resp) {
		@SuppressWarnings("unchecked")
		Collection<String> loginNames = (Collection<String>) req.get("login_names");
		Map<String, String> failedUsers = setForcePasswordChanges(req.getOrgDomain(), req.getAdminLoginName(), loginNames, true);
		resp.put("failed_list", failedUsers);
	}

	@MsgbusMethod
	@MsgbusPermission(group = "frodo", code = "user_edit")
	public void cancelForcePasswordChanges(Request req, Response resp) {
		@SuppressWarnings("unchecked")
		Collection<String> loginNames = (Collection<String>) req.get("login_names");
		Map<String, String> failedUsers = setForcePasswordChanges(req.getOrgDomain(), req.getAdminLoginName(), loginNames, false);
		resp.put("failed_list", failedUsers);
	}

	private Map<String, String> setForcePasswordChanges(String domain, String adminLoginName, Collection<String> loginNames,
			boolean forcePasswordChange) {

		List<User> updateUsers = new ArrayList<User>();
		Collection<User> users = null;
		Map<String, String> failed_list = new HashMap<String, String>();

		if (loginNames == null)
			users = domUserApi.getUsers(domain);
		else
			users = domUserApi.getUsers(domain, loginNames);

		if (users == null || users.size() == 0)
			throw new IllegalArgumentException("userConfigs are empty.");

		Admin admin = adminApi.findAdmin(domain, adminLoginName);
		if (admin == null)
			throw new DOMException("admin-not-found");

		for (User user : users) {
			if (!adminApi.canManage(domain, admin, user)) {
				failed_list.put(user.getLoginName(), "no-permission");
				continue;
			}

			Map<String, Object> ext = user.getExt();
			@SuppressWarnings("unchecked")
			Map<String, Object> frodo = (Map<String, Object>) ext.get("frodo");
			if (frodo == null) {
				frodo = new HashMap<String, Object>();
				frodo.put("cid", UUID.randomUUID().toString());
				frodo.put("is_locked", false);
				frodo.put("login_failures", 0);
				frodo.put("created_at", new Date());
			}
			frodo.put("updated_at", new Date());
			frodo.put("force_password_change", forcePasswordChange);
			ext.put("frodo", frodo);
			user.setExt(ext);

			updateUsers.add(user);
		}

		// updateUser 실행
		if (updateUsers.size() > 0) {
			try {
				domUserApi.updateUsers(domain, updateUsers, false);
				logger.trace("kraken dom: updated [{}] users", updateUsers.size());
			} catch (Throwable t) {
				logger.error("kraken dom: user update failed", t);
			}
		}
		return failed_list;
	}
	
	@MsgbusMethod
	public void getAdminUserExtensions(Request req, Response resp) {
		String orgUnitId = req.getString("org_unit_id");
        String filter = "";
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ssZ");

		Predicate pred = null;
		if (req.has("filter_name") || req.has("filter_login_name")) {
			String name = req.getString("filter_name");
			String loginName = req.getString("filter_login_name");

            if (name.length() > 0)
                filter = name;
            if (loginName.length() > 0)
                filter = loginName;

			pred = new Matched(name, loginName);
		}
		
		Predicate predResult = null;
		if(pred == null) {
			predResult = Predicates.has("ext/admin");
		} else {
			predResult = Predicates.and(pred, Predicates.has("ext/admin"));
		}

		int offset = 0;
		int limit = Integer.MAX_VALUE;

		if (req.has("offset"))
			offset = req.getInteger("offset");
		if (req.has("limit"))
			limit = req.getInteger("limit");

		int total = domUserApi.countUsers(LOCALHOST, orgUnitId, true, filter, true);
		Collection<User> users = domUserApi.getUsers(LOCALHOST, orgUnitId, true, filter, offset, limit, true);

		if (logger.isTraceEnabled())
			logger.trace("frodo core: fetched users {}, total {}", users.size(), total);

		Map<String, UserExtension> extMap = toExtensionMap(userApi.getUserExtensions(users));
		ArrayList<Object> l = new ArrayList<Object>(Math.min(users.size(), limit));
		for (User user : users)
			l.add(toVpnUser(dateFormat, user, extMap));

		resp.put("users", l);
		resp.put("total", total);
	}
	
	@MsgbusMethod
	public void getAdminUsers(Request req, Response resp) {
		String orgUnitGuid = req.getString("ou_guid");
        String filter = "";

		Predicate pred = null;
		if (req.has("filter_name") || req.has("filter_login_name")) {
			String name = req.getString("filter_name");
			String loginName = req.getString("filter_login_name");

            if (name.length() > 0)
                filter = name;
            if (loginName.length() > 0)
                filter = loginName;

			pred = new Matched(name, loginName);
		}
		
		Predicate predResult = null;
		if(pred == null) {
			predResult = Predicates.has("ext/admin");
		} else {
			predResult = Predicates.and(pred, Predicates.has("ext/admin"));
		}

		int offset = 0;
		int limit = Integer.MAX_VALUE;

		if (req.has("offset"))
			offset = req.getInteger("offset");
		if (req.has("limit"))
			limit = req.getInteger("limit");

		int total = domUserApi.countUsers(req.getOrgDomain(), orgUnitGuid, true, filter, true);
		Collection<User> users = domUserApi.getUsers(req.getOrgDomain(), orgUnitGuid, true, filter, offset, limit, true);

		resp.put("users", PrimitiveConverter.serialize(users));
		resp.put("total", total);
	}

	
	@MsgbusMethod
	@MsgbusPermission(group = "frodo", code = "user_edit")
	public void resetIdns(Request req, Response resp) {
		String requestName = req.getAdminLoginName();
		@SuppressWarnings("unchecked")
		List<String> loginNames = (List<String>) req.get("login_names");

		Admin admin = adminApi.findAdmin(req.getOrgDomain(), req.getAdminLoginName());
		if (admin == null)
			throw new MsgbusException("frodo", "admin-not-found");

		Collection<User> users = domUserApi.getUsers(req.getOrgDomain(), loginNames);
		Map<String, UserExtension> extMap = toExtensionMap(userApi.getUserExtensions(users));

		Map<String, String> failedList = new HashMap<String, String>();
		for (User u : users) {
			if (admin.getRole().getLevel() == 2) {
				if (requestName.equals(u.getLoginName())) {
					UserExtension ext = extMap.get(u.getLoginName());
					if (ext == null)
						continue;

					ext.setCertType(null);
					ext.setIdnHash(null);

					userApi.setUserExtension(ext);
				} else if (adminApi.canManage(LOCALHOST, admin, u)) {
					UserExtension ext = extMap.get(u.getLoginName());
					if (ext == null)
						continue;

					ext.setCertType(null);
					ext.setIdnHash(null);

					userApi.setUserExtension(ext);
				} else {
					failedList.put(u.getLoginName(), "no-permission");
				}

				continue;
			}

			if (!adminApi.canManage(req.getOrgDomain(), admin, u)) {
				failedList.put(u.getLoginName(), "no-permission");
				continue;
			}

			UserExtension ext = extMap.get(u.getLoginName());
			if (ext != null) {
				ext.setCertType(null);
				ext.setIdnHash(null);
				userApi.setUserExtension(ext);
			}
		}

		resp.put("failed_list", failedList);
	}
}
