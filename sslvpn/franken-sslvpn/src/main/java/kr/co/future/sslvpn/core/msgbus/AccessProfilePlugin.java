package kr.co.future.sslvpn.core.msgbus;

import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import kr.co.future.sslvpn.core.msgbus.AccessProfilePlugin;
import kr.co.future.sslvpn.model.AccessProfile;
import kr.co.future.sslvpn.model.ClientIpRange;
import kr.co.future.sslvpn.model.ClientApp;
import kr.co.future.sslvpn.model.InternalNetworkRange;
import kr.co.future.sslvpn.model.IpLeaseRange;
import kr.co.future.sslvpn.model.LdapAttribute;
import kr.co.future.sslvpn.model.ReferenceException;
import kr.co.future.sslvpn.model.Server;
import kr.co.future.sslvpn.model.SplitRoutingEntry;
import kr.co.future.sslvpn.model.AccessGateway.DeviceAuthMode;
import kr.co.future.sslvpn.model.api.AccessProfileApi;
import kr.co.future.sslvpn.model.api.ClientAppApi;
import kr.co.future.sslvpn.model.api.ClientCheckProfileApi;
import kr.co.future.sslvpn.model.api.ServerApi;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Requires;

import kr.co.future.api.PrimitiveConverter;
import kr.co.future.api.PrimitiveConverter.SerializeOption;
import kr.co.future.dom.api.OrganizationApi;
import kr.co.future.msgbus.Marshaler;
import kr.co.future.msgbus.MsgbusException;
import kr.co.future.msgbus.Request;
import kr.co.future.msgbus.Response;
import kr.co.future.msgbus.handler.MsgbusMethod;
import kr.co.future.msgbus.handler.MsgbusPlugin;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(name = "frodo-access-profile-plugin")
@MsgbusPlugin
public class AccessProfilePlugin {
	private final Logger logger = LoggerFactory.getLogger(AccessProfilePlugin.class.getName());

	@Requires
	private AccessProfileApi profileApi;

	@Requires
	private ClientAppApi clientAppApi;

	@Requires
	private ServerApi serverApi;

	@Requires
	private OrganizationApi orgApi;

	@Requires
	private ClientCheckProfileApi clientCheckProfileApi;

	@MsgbusMethod
	public void getProfiles(Request req, Response resp) {
		// for keyboard or mouse idle timeout
		Integer max = (Integer) orgApi.getOrganizationParameter(req.getOrgDomain(), "sslvpn_max_client_idle_timeout");

		Collection<AccessProfile> profiles = profileApi.getAccessProfiles();
		for (AccessProfile p : profiles)
			p.setMaxClientTimeout(max);

		if (logger.isDebugEnabled())
			logger.debug("frodo core: acess profiles [{}]", Marshaler.marshal(profiles));

		resp.put("profiles", Marshaler.marshal(profiles));
	}

	@MsgbusMethod
	public void getProfile(Request req, Response resp) {
		// for keyboard or mouse idle timeout
		Integer max = (Integer) orgApi.getOrganizationParameter(req.getOrgDomain(), "sslvpn_max_client_idle_timeout");

		String guid = req.getString("guid");
		AccessProfile profile = profileApi.getAccessProfile(guid);
		profile.setMaxClientTimeout(max);

		// migrate ip lease ranges for primitive converter
		if (profile.getIpFrom() != null && profile.getIpTo() != null) {
			profile.setIpLeaseRanges(profile.getIpLeaseRanges());
		}

		resp.put("profile", PrimitiveConverter.serialize(profile, SerializeOption.INCLUDE_SKIP_FIELD));
	}

	@MsgbusMethod
	public void createProfile(Request req, Response resp) throws UnknownHostException {
		try {
			AccessProfile p = toProfile(req);
			String guid = profileApi.createAccessProfile(p);
			resp.put("guid", guid);
		} catch (IllegalStateException e) {
			if (e.getMessage().contains("duplicated guid"))
				throw new MsgbusException("frodo", "duplicated profile guid");
			else if (e.getMessage().contains("duplicated name"))
				throw new MsgbusException("frodo", "duplicated profile name");
			else if (e.getMessage().contains("duplicated profile"))
				throw new MsgbusException("frodo", "duplicated profile");
			else
				throw e;
		}
	}

	@MsgbusMethod
	public void updateProfile(Request req, Response resp) throws UnknownHostException {
		try {
			AccessProfile p = toProfile(req);
			profileApi.updateAccessProfile(p);
		} catch (IllegalStateException e) {
			if (e.getMessage().contains("duplicated guid"))
				throw new MsgbusException("frodo", "duplicated profile guid");
			else if (e.getMessage().contains("duplicated name"))
				throw new MsgbusException("frodo", "duplicated profile name");
			else if (e.getMessage().contains("duplicated profile"))
				throw new MsgbusException("frodo", "duplicated profile");
			else
				throw e;
		}
	}

	@SuppressWarnings("unchecked")
	private AccessProfile toProfile(Request req) {
		AccessProfile p = new AccessProfile();
		if (req.has("guid") && req.getString("guid") != null)
			p.setGuid(req.getString("guid"));

		p.setName(req.getString("name"));
		p.setDescription(req.getString("description"));
		p.setAllowTimeId(req.getString("allow_time_id"));
		p.setVerifyClientIp(req.getBoolean("verify_client_ip"));
		p.setUseNac(req.getBoolean("use_nac"));
		p.setUseClientTimeout(req.getBoolean("use_client_timeout"));
		p.setClientTimeout(req.getInteger("client_timeout"));
		p.setUseFailLimit(req.getBoolean("use_fail_limit"));
		p.setFailLimitCount(req.getInteger("fail_limit"));
		if (req.has("password_expiry"))
			p.setPasswordExpiry(req.getInteger("password_expiry"));
		else
			p.setPasswordExpiry(365); // support legacy

		List<IpLeaseRange> ipLeaseRanges = (List<IpLeaseRange>) PrimitiveConverter.parseCollection(IpLeaseRange.class,
				(Collection<Object>) req.get("ip_lease_ranges"));

		p.setIpLeaseRanges(ipLeaseRanges);
		p.setNormalAcl(parseServers((List<String>) req.get("normal_acl")));
		p.setQuarantineAcl(parseServers((List<String>) req.get("quarantine_acl")));
		p.setClientApps(parseDeployments((List<String>) req.get("apps")));
		p.setInternalNetworkRanges(parseRanges((List<Object>) req.get("internal")));
		if (req.has("login_method"))
			p.setLoginMethod(req.getInteger("login_method"));
		if (req.has("encryptions"))
			p.setEncryptions((List<String>) req.get("encryptions"));

		String clientCheckProfileGuid = req.getString("client_check_profile");

		p.setPasswordChangeAlert(req.getInteger("password_change_alert"));

		if (clientCheckProfileGuid != null)
			p.setClientCheckProfile(clientCheckProfileApi.getClientCheckProfile(clientCheckProfileGuid));
		else
			p.setClientCheckProfile(null);

		String deviceAuthMode = req.getString("device_auth_mode");
		logger.trace("frodo core: request device auth mode [{}]", deviceAuthMode);
		p.setDeviceAuthMode(deviceAuthMode == null ? null : DeviceAuthMode.valueOf(deviceAuthMode));

		Collection<Object> requestClientIpRanges = (Collection<Object>) req.get("client_ip_ranges");
		List<ClientIpRange> clientIpRanges = new ArrayList<ClientIpRange>();
		if (requestClientIpRanges != null)
			clientIpRanges = (List<ClientIpRange>) PrimitiveConverter.parseCollection(ClientIpRange.class, requestClientIpRanges);
		p.setClientIpRanges(clientIpRanges);

		p.setUseSplitRouting(req.getBoolean("use_split_routing"));
		List<SplitRoutingEntry> splitRoutingEntries = new ArrayList<SplitRoutingEntry>();
		Collection<Object> entries = (Collection<Object>) req.get("split_routing_entries");
		if (entries != null)
			splitRoutingEntries = (List<SplitRoutingEntry>) PrimitiveConverter.parseCollection(SplitRoutingEntry.class, entries);
		p.setSplitRoutingEntries(splitRoutingEntries);

		p.setUserUnlockTime(req.getInteger("user_unlock_time"));
		p.setPopupUrl(req.getString("popup_url"));
		p.setLdapAttributes(PrimitiveConverter.parse(LdapAttribute.class, req.get("ldap_attributes")));
		p.setUseAutoReconnect(req.getBoolean("use_auto_reconnect"));
		p.setAccountExpiryAlert(req.getInteger("account_expiry_alert") == null ? null : req.getInteger("account_expiry_alert")
				.longValue());
		p.setUseClientAutoUninstall(req.getBoolean("use_client_auto_uninstall"));
		p.setUseProxy(req.getBoolean("use_proxy"));
		p.setExpireValidDate(req.getDate("expire_valid_date"));
		p.setStartValidDate(req.getDate("start_valid_date"));
		p.setAccessDir(req.getString("access_dir"));
		p.setAccessDirType(req.getString("access_dir_type"));
		p.setUseIOS(req.getBoolean("use_ios"));

		return p;
	}

	private List<ClientApp> parseDeployments(List<String> appGuids) {
		List<ClientApp> apps = new ArrayList<ClientApp>();
		for (String appGuid : appGuids) {
			ClientApp app = clientAppApi.getClientApp(appGuid);
			apps.add(app);
		}
		return apps;
	}

	private List<Server> parseServers(List<String> guids) {
		List<Server> servers = new ArrayList<Server>();
		for (String guid : guids)
			servers.add(serverApi.getServer(guid));

		return servers;
	}

	@SuppressWarnings("unchecked")
	private List<InternalNetworkRange> parseRanges(List<Object> l) {
		List<InternalNetworkRange> ranges = new ArrayList<InternalNetworkRange>();
		for (Object o : l)
			ranges.add(parseRange((Map<String, Object>) o));

		return ranges;
	}

	private InternalNetworkRange parseRange(Map<String, Object> m) {
		InternalNetworkRange r = new InternalNetworkRange();
		r.setIp((String) m.get("ip"));
		r.setCidr((Integer) m.get("cidr"));
		return r;
	}

	@MsgbusMethod
	public void removeProfile(Request req, Response resp) {
		String guid = req.getString("guid");
		try {
			profileApi.removeAccessProfile(guid);
		} catch (ReferenceException e) {
			throw new MsgbusException("frodo", "used-profile", e.getReferences());
		} catch (IllegalStateException e) {
			throw new MsgbusException("frodo", "last-profile");
		}
	}

	@MsgbusMethod
	public void removeProfiles(Request req, Response resp) {
		@SuppressWarnings("unchecked")
		List<String> profiles = (List<String>) req.get("profiles");

		try {
			profileApi.removeAccessProfiles(profiles);
		} catch (ReferenceException e) {
			throw new MsgbusException("frodo", "used-profile", e.getReferences());
		} catch (IllegalStateException e) {
			throw new MsgbusException("frodo", "last-profile");
		}
	}
}
