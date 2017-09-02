package kr.co.future.sslvpn.core.msgbus;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import kr.co.future.sslvpn.core.GlobalConfig;
import kr.co.future.sslvpn.core.GlobalConfigApi;
import kr.co.future.sslvpn.model.AuthorizedDevice;
import kr.co.future.sslvpn.model.UserExtension;
import kr.co.future.sslvpn.model.api.AccessGatewayApi;
import kr.co.future.sslvpn.model.api.AuthorizedDeviceApi;
import kr.co.future.sslvpn.model.api.UserApi;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Requires;

import kr.co.future.confdb.Config;
import kr.co.future.confdb.Predicate;
import kr.co.future.confdb.Predicates;
import kr.co.future.dom.api.AdminApi;
import kr.co.future.dom.model.Admin;
import kr.co.future.dom.model.Permission;
import kr.co.future.dom.model.User;
import kr.co.future.msgbus.Marshaler;
import kr.co.future.msgbus.MsgbusException;
import kr.co.future.msgbus.Request;
import kr.co.future.msgbus.Response;
import kr.co.future.msgbus.handler.MsgbusMethod;
import kr.co.future.msgbus.handler.MsgbusPermission;
import kr.co.future.msgbus.handler.MsgbusPlugin;
import kr.co.future.sslvpn.core.impl.SaltGenerator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(name = "frodo-auth-device-plugin")
@MsgbusPlugin
public class AuthorizedDevicePlugin {
	private final Logger logger = LoggerFactory.getLogger(AuthorizedDevicePlugin.class.getName());

	@Requires
	private AuthorizedDeviceApi authDeviceApi;

	@Requires
	private UserApi userApi;

	@Requires
	private kr.co.future.dom.api.UserApi domUserApi;

	@Requires
	private AdminApi adminApi;

	@Requires
	private GlobalConfigApi gcApi;

	@Requires
	private AccessGatewayApi gwApi;

	@MsgbusMethod
	@MsgbusPermission(group = "frodo", code = "device_transfer")
	public void removeAndGenerateKey(Request req, Response resp) {
		if (!req.has("guid")) {
			throw new MsgbusException("frodo", "guid-not-found");
		}

		String guid = req.getString("guid");
		AuthorizedDevice device = authDeviceApi.getDevice(guid);
		if (device == null)
			throw new MsgbusException("frodo", "device-not-found");

		String requestLoginName = req.getAdminLoginName();
		String targetLoginName = device.getLoginName();
		if (targetLoginName == null) {
			logger.info("frodo core: request from [{}], device login name not found, unregistered device guid [{}]",
					requestLoginName, guid);
			authDeviceApi.unregisterDevice(guid);
			return;
		}

		User user = domUserApi.findUser("localhost", targetLoginName);
		if (user == null) {
			logger.info("frodo core: request from [{}], user login name [{}] not found, unregistered device guid [{}]",
					new Object[] { requestLoginName, targetLoginName, guid });
			authDeviceApi.unregisterDevice(guid);
			return;
		}

		if (!canManage(requestLoginName, targetLoginName))
			throw new MsgbusException("frodo", "no-permission");

		UserExtension ext = userApi.findUserExtension(targetLoginName);

		if (ext == null) {
			ext = new UserExtension();
			ext.setSalt(SaltGenerator.createSalt(20));
			ext.setUser(user);
			logger.info("frodo core: request from [{}], generated salt [{}] for login name [{}]", new Object[] {
					requestLoginName, ext.getSalt(), targetLoginName });
		}
		
		Integer beforeUnregister = ext.getDeviceKeyCount();
		if(beforeUnregister == null)
			beforeUnregister = 0;

		authDeviceApi.unregisterDevice(guid);

		Integer afterUnregister = ext.getDeviceKeyCount();
		if(afterUnregister == null)
			afterUnregister = 0;
		if(afterUnregister - beforeUnregister < 1) {
			ext.setDeviceKeyCountSetting(ext.getDeviceKeyCountSetting() == null ? 1 : ext.getDeviceKeyCountSetting() + 1);
			setDeviceKeyExpireTime(ext);
			userApi.setUserExtension(ext);
		}
		
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		Date keyExpireDateTime = ext.getKeyExpireDateTime();
		logger.info(
				"frodo core: request from [{}], unregistered device guid [{}], generated device auth key count [{}] for user [{}], it will be expired at [{}]",
				new Object[] { requestLoginName, guid, ext.getDeviceKeyCount(), targetLoginName,
						keyExpireDateTime == null ? null : dateFormat.format(keyExpireDateTime) });
	}

	@MsgbusMethod
	@MsgbusPermission(group = "frodo", code = "device_view")
	public void getDevices(Request req, Response resp) {
		OwnerMatcher ownerPred = req.has("find_by_owner") ? new OwnerMatcher(req.getString("find_by_owner")) : null;
		Predicate pred = Predicates.and(ownerPred,
				Predicates.or(Predicates.field("is_authorized", null), Predicates.field("is_authorized", true)));

		resp.put("devices", getDevices(req.getInteger("offset"), req.getInteger("limit"), pred));
		resp.put("total_count", authDeviceApi.countDevices(pred));
	}

	@MsgbusMethod
	@MsgbusPermission(group = "frodo", code = "device_view")
	public void getUnauthorizedDevices(Request req, Response resp) {
		OwnerMatcher ownerPred = req.has("find_by_owner") ? new OwnerMatcher(req.getString("find_by_owner")) : null;
		Predicate pred = Predicates.and(ownerPred, Predicates.field("is_authorized", false));

		resp.put("devices", getDevices(req.getInteger("offset"), req.getInteger("limit"), pred));
		resp.put("total_count", authDeviceApi.countDevices(pred));
	}

	private List<Object> getDevices(Integer reqOffset, Integer reqLimit, Predicate pred) {
		int offset = 0;
		int limit = Integer.MAX_VALUE;
		if (reqOffset != null)
			offset = reqOffset;
		if (reqLimit != null)
			limit = reqLimit;

		List<Object> devices = new ArrayList<Object>();
		if (gwApi.getCurrentAccessGateway().getDeviceKeyType().contains("MAC"))
			devices = Marshaler.marshal(authDeviceApi.getDevices(offset, limit, pred));
		else {
			for (AuthorizedDevice authDevice : authDeviceApi.getDevices(offset, limit, pred)) {
				Map<String, Object> m = authDevice.marshal();
				m.remove("mac_address");
				devices.add(m);
			}
		}

		return devices;
	}

	@MsgbusMethod
	@MsgbusPermission(group = "frodo", code = "device_edit")
	public void authorizeDevice(Request req, Response resp) {
		if (!req.has("guid"))
			throw new MsgbusException("frodo", "guid-not-found");

		Admin admin = adminApi.findAdmin(req.getOrgDomain(), req.getAdminLoginName());
		if (admin == null)
			throw new MsgbusException("frodo", "admin-not-found");

		Map<String, Object> filter = new HashMap<String, Object>();
		filter.put("guid", req.getString("guid"));
		filter.put("is_authorized", false);

		AuthorizedDevice device = authDeviceApi.getDevice(Predicates.field(filter));
		if (device == null)
			throw new MsgbusException("frodo", "device-not-found");
		if (logger.isDebugEnabled())
			logger.debug("frodo core: target unauthorized device [{}]", device.marshal());

		User user = domUserApi.findUser(req.getOrgDomain(), device.getLoginName());
		if (user == null) {
			authDeviceApi.unregisterDevice(device.getGuid());
			throw new MsgbusException("frodo", "user-not-found");
		}

		if (!canManage(req.getOrgDomain(), admin, user)) {
			logger.trace("frodo core: cannot authorize device user=[{}] because no permission [{}]", user.getLoginName(), admin
					.getUser().getLoginName());
			throw new MsgbusException("frodo", "no-permission");
		}

		device.setIsAuthorized(true);
		authDeviceApi.updateDevices(Arrays.asList(device));
	}

	@MsgbusMethod
	@MsgbusPermission(group = "frodo", code = "device_edit")
	public void authorizeDevices(Request req, Response resp) {
		if (!req.has("guids"))
			throw new MsgbusException("frodo", "guids-not-found");

		@SuppressWarnings("unchecked")
		List<String> guids = (List<String>) req.get("guids");

		Admin admin = adminApi.findAdmin(req.getOrgDomain(), req.getAdminLoginName());
		if (admin == null)
			throw new MsgbusException("frodo", "admin-not-found");

		List<AuthorizedDevice> authDevices = authDeviceApi.getDevices(0, Integer.MAX_VALUE,
				Predicates.and(Predicates.in("guid", guids), Predicates.field("is_authorized", false)));

		Set<String> loginNames = new HashSet<String>();
		for (AuthorizedDevice authDevice : authDevices)
			loginNames.add(authDevice.getLoginName());

		Map<String, User> userMap = new HashMap<String, User>();
		for (User user : domUserApi.getUsers("localhost", loginNames))
			userMap.put(user.getLoginName(), user);

		Map<String, String> failedList = new HashMap<String, String>();
		List<AuthorizedDevice> authroizeDevice = new ArrayList<AuthorizedDevice>();
		Set<String> removeGuids = new HashSet<String>();
		for (AuthorizedDevice authDevice : authDevices) {
			User user = userMap.get(authDevice.getLoginName());
			if (user == null) {
				logger.debug("frodo core: does not exist user [{}], remove authorized device, guid [{}]",
						authDevice.getLoginName(), authDevice.getGuid());
				failedList.put(authDevice.getLoginName(), "no-user");
				removeGuids.add(authDevice.getGuid());
				continue;
			}

			if (!canManage(req.getOrgDomain(), admin, user)) {
				failedList.put(user.getLoginName(), "no-permission");
				continue;
			}

			authDevice.setIsAuthorized(true);
			authroizeDevice.add(authDevice);
		}
		authDeviceApi.updateDevices(authroizeDevice);
		authDeviceApi.unregisterDevices(removeGuids);
		resp.put("failed_list", failedList);
	}

	@MsgbusMethod
	@MsgbusPermission(group = "frodo", code = "device_edit")
	public void removeDevice(Request req, Response resp) {
		if (!req.has("guid")) {
			throw new MsgbusException("frodo", "guid-not-found");
		}

		String guid = req.getString("guid");
		AuthorizedDevice device = authDeviceApi.getDevice(guid);
		if (device == null)
			throw new MsgbusException("frodo", "device-not-found");
		String deviceLoginName = device.getLoginName();

		if (!canManage(req.getAdminLoginName(), deviceLoginName))
			throw new MsgbusException("frodo", "no-permission");

		authDeviceApi.unregisterDevice(guid);
		logger.info("frodo core: request from [{}], unregistered device [{}] for login name [{}]",
				new Object[] { req.getAdminLoginName(), guid, deviceLoginName });
	}

	@MsgbusMethod
	@MsgbusPermission(group = "frodo", code = "device_edit")
	public void removeDevices(Request req, Response resp) {
		if (!req.has("guids")) {
			throw new MsgbusException("frodo", "guids-not-found");
		}

		@SuppressWarnings("unchecked")
		Set<String> guids = new HashSet<String>((List<String>) req.get("guids"));

		Admin admin = adminApi.findAdmin(req.getOrgDomain(), req.getAdminLoginName());
		if (admin == null)
			throw new MsgbusException("frodo", "admin-not-found");

		List<AuthorizedDevice> authDevices = authDeviceApi.getDevices(0, Integer.MAX_VALUE,
				Predicates.and(Predicates.in("guid", guids)));

		Set<String> loginNames = new HashSet<String>();
		for (AuthorizedDevice authDevice : authDevices)
			loginNames.add(authDevice.getLoginName());

		Map<String, User> userMap = new HashMap<String, User>();
		for (User user : domUserApi.getUsers("localhost", loginNames))
			userMap.put(user.getLoginName(), user);

		Map<String, String> failedList = new HashMap<String, String>();
		Set<String> removeGuids = new HashSet<String>();
		for (AuthorizedDevice authDevice : authDevices) {
			User user = userMap.get(authDevice.getLoginName());
			if (user == null) {
				removeGuids.add(authDevice.getGuid());
				continue;
			}

			if (!canManage(req.getOrgDomain(), admin, user)) {
				logger.trace("frodo core: unregister device fail by no-permission, login_name=[{}]", authDevice.getLoginName());
				failedList.put(user.getLoginName(), "no-permission");
				continue;
			}
			removeGuids.add(authDevice.getGuid());
		}

		authDeviceApi.unregisterDevices(removeGuids);
		resp.put("failed_list", failedList);
		logger.info("frodo core: request from [{}], unregistered device guid [{}]", req.getAdminLoginName(), removeGuids);
	}

	@MsgbusMethod
	@MsgbusPermission(group = "frodo", code = "device_edit")
	public void setExpiration(Request req, Response resp) throws ParseException {
		if (!req.has("guid"))
			throw new MsgbusException("frodo", "guid-not-found");
		if (!req.has("expiration"))
			throw new MsgbusException("frodo", "expiration-not-found");

		String guid = req.getString("guid");
		AuthorizedDevice device = authDeviceApi.getDevice(guid);
		if (device == null)
			throw new MsgbusException("frodo", "device-not-found");

		if (!canManage(req.getAdminLoginName(), device.getLoginName()))
			throw new MsgbusException("frodo", "no-permission");

		authDeviceApi.setExpiration(guid, req.getDate("expiration"));
		logger.info("frodo core: request from [{}], setted device guid [{}] expiration ", req.getAdminLoginName(), guid);
	}

	@MsgbusMethod
	@MsgbusPermission(group = "frodo", code = "device_edit")
	public void blockDevice(Request req, Response resp) {
		if (!req.has("guid"))
			throw new MsgbusException("frodo", "guid-not-found");
		if (!req.has("block"))
			throw new MsgbusException("frodo", "block-not-found");

		Boolean block = req.getBoolean("block");
		String guid = req.getString("guid");

		AuthorizedDevice device = authDeviceApi.getDevice(guid);
		if (device == null)
			throw new MsgbusException("frodo", "device-not-found");

		if (!canManage(req.getAdminLoginName(), device.getLoginName()))
			throw new MsgbusException("frodo", "no-permission");

		authDeviceApi.blockDevice(guid, block);
		resp.put("block", block);
		logger.info("frodo core: request from [{}], blocked device guid [{}] ", req.getAdminLoginName(), guid);
	}

	private boolean canManage(String domain, Admin admin, User user) {
		String loginName = user.getLoginName();
		if (!admin.getRole().getPermissions().contains(new Permission("frodo", "device_edit")))
			return false;

		Object ext = user.getExt().get("admin");
		if (ext == null)
			return true;

		Admin targetAdmin = adminApi.getAdmin("localhost", user);
		if (targetAdmin != null && !loginName.equals(admin.getUser().getLoginName())
				&& targetAdmin.getRole().getLevel() >= admin.getRole().getLevel()) {
			return false;
		}
		return true;
	}

	private boolean canManage(String requestLoginName, String targetLoginName) {
		if (targetLoginName == null)
			return true;

		return canManage(requestLoginName, new HashSet<String>(Arrays.asList(targetLoginName)));
	}

	private boolean canManage(String requestLoginName, Set<String> targetLoginNames) {
		Admin requestAdmin = adminApi.findAdmin("localhost", requestLoginName);

		if (requestAdmin == null)
			return false;

		if (targetLoginNames.isEmpty())
			return true;

		Collection<User> users = domUserApi.getUsers("localhost", targetLoginNames);

		int requestLevel = requestAdmin.getRole().getLevel();

		for (User user : users) {
			Admin targetAdmin = adminApi.getAdmin("localhost", user);
			if (targetAdmin != null) {
				int targetLevel = targetAdmin.getRole().getLevel();

				if (!requestLoginName.equals(user.getLoginName()) && targetLevel >= requestLevel)
					return false;
			}
		}
		return true;
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

	private class OwnerMatcher implements Predicate {
		private String target;

		public OwnerMatcher(String target) {
			this.target = target;
		}

		@Override
		public boolean eval(Config c) {
			@SuppressWarnings("unchecked")
			Map<String, Object> m = (Map<String, Object>) c.getDocument();

			String owner = (String) m.get("owner");
			if (owner != null && owner.contains(target))
				return true;

			String loginName = (String) m.get("login_name");
			if (loginName != null && loginName.contains(target))
				return true;

			return false;
		}
	}
}
