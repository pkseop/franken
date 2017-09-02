package kr.co.future.sslvpn.core.servlet.csv;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import kr.co.future.sslvpn.model.AccessProfile;
import kr.co.future.sslvpn.model.ClientApp;
import kr.co.future.sslvpn.model.ClientIpRange;
import kr.co.future.sslvpn.model.IpLeaseRange;
import kr.co.future.sslvpn.model.Server;
import kr.co.future.sslvpn.model.SplitRoutingEntry;

public class ExportAccessProfileInfo implements ExportObject{	
	
	@SuppressWarnings("unchecked")
   public List<Map<String, Object>> convertToListMap(List<?> list)  {
		List<Map<String, Object>> resultList = new ArrayList<Map<String, Object>>();
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ssZ");
		
		List<AccessProfile> profiles = (List<AccessProfile>)list;
		for (AccessProfile profile : profiles) {
			Map<String, Object> m = new HashMap<String, Object>();
			
			m.put("id", profile.getId());
			m.put("guid", profile.getGuid());
			m.put("name", profile.getName());
			m.put("description", profile.getDescription());
			m.put("allow_time_id", profile.getAllowTimeId());
			m.put("verify_client_ip", profile.isVerifyClientIp());
			m.put("use_nac", profile.isUseNac());
			m.put("ip_from", profile.getIpFrom());
			m.put("ip_to", profile.getIpTo());
			m.put("ip_lease_ranges", profile.getIpLeaseRanges() == null ? null : getIpLeaseRangeInfoAsStr(profile.getIpLeaseRanges()));
			m.put("use_client_timeout", profile.isUseClientTimeout());
			m.put("max_client_timeout", profile.getMaxClientTimeout());
			m.put("client_timeout", profile.getClientTimeout());
			m.put("use_fail_limit", profile.isUseFailLimit());
			m.put("fail_limit", profile.getFailLimitCount());
			m.put("password_expiry", profile.getPasswordExpiry());
			m.put("client_check_profile", profile.getClientCheckProfile() == null ? null : profile.getClientCheckProfile().getGuid());
			m.put("created_at", dateFormat.format(profile.getCreateDateTime()));
			m.put("updated_at", dateFormat.format(profile.getUpdateDateTime()));
			m.put("login_method", profile.getLoginMethod());
			m.put("encryptions", profile.getEncryptions() == null ? null : getEncryptionsInfoAsStr(profile.getEncryptions()));
			m.put("password_change_alert", profile.getPasswordChangeAlert());
			m.put("account_expiry_alert", profile.getAccountExpiryAlert());
			m.put("device_auth_mode", profile.getDeviceAuthMode() == null ? null : profile.getDeviceAuthMode().toString());
			m.put("client_ip_ranges", profile.getClientIpRanges() == null ? null : getClientIpRangeInfoAsStr(profile.getClientIpRanges()));
			m.put("use_split_routing", profile.getUseSplitRouting());
			m.put("split_routing_entries", profile.getSplitRoutingEntries() == null ? null : getSplitRoutingEntriesInfoAsStr(profile.getSplitRoutingEntries()));
			m.put("user_unlock_time", profile.getUserUnlockTime());
			m.put("popup_url", profile.getPopupUrl());
			m.put("ldap_attributes", profile.getLdapAttributes() == null ? null : profile.getLdapAttributes().isUsePasswordChange());
			m.put("use_auto_reconnect", profile.getUseAutoReconnect());
			m.put("use_client_auto_uninstall", profile.getUseClientAutoUninstall());
			m.put("use_proxy", profile.getUseProxy());

			//insert server list
			List<Server> normalAcl = profile.getNormalAcl();
			if(normalAcl != null && normalAcl.size() > 0) {
				String data = "";
				for(Server server : normalAcl) {
					if(data.length() > 0)
						data += DELIM_1ST;
					data += server.getName() == null ? "" : server.getName();
					data += DELIM_2ND;
					data += server.getGuid() == null ? "" : server.getGuid();
				}
				m.put("normal_acl", data == null ? null : data);
			} 
			
			List<Server> quarantineAcl = profile.getQuarantineAcl();
			if(quarantineAcl != null && quarantineAcl.size() > 0) {
				String data = "";
				for(Server server : normalAcl) {
					if(data.length() > 0)
						data += DELIM_1ST;
					data += server.getName() == null ? "" : server.getName();
					data += DELIM_2ND;
					data += server.getGuid() == null ? "" : server.getGuid();
				}
				m.put("quarantine_acl", data == null ? null : data);
			}
			
			List<ClientApp> clientApps = profile.getClientApps();
			if(clientApps != null && clientApps.size() > 0) {
				String data = "";
				for(ClientApp clientApp : clientApps) {
					if(data.length() > 0)
						data += DELIM_1ST;
					data += clientApp.getName() == null ? "" : clientApp.getName();
					data += DELIM_2ND;
					data += clientApp.getGuid() == null ? "" : clientApp.getGuid();
				}
				m.put("client_app", data == null ? null : data);
			}
			
			resultList.add(m);
		}
		return resultList;
	}
	
	private String getEncryptionsInfoAsStr(List<String> encryptions) {
		String result = "";
		for(String encryption : encryptions) {
			if(result.length() > 0)
				result += DELIM_2ND;
			result += encryption;
		}
		return result;
	}
	
	private String getIpLeaseRangeInfoAsStr(List<IpLeaseRange> ipLeaseRanges) {
		String result = "";
		for(IpLeaseRange ipLeaseRange : ipLeaseRanges) {
			if(result.length() > 0)
				result += DELIM_1ST;
			result += ipLeaseRange.getIpFrom();
			result += DELIM_2ND;			
			result += ipLeaseRange.getIpTo();
		}
		return result;
	}
	
	private String getClientIpRangeInfoAsStr(List<ClientIpRange> clientIpRanges) {
		String result = "";
		for(ClientIpRange clientIpRange : clientIpRanges) {
			if(result.length() > 0)
				result += DELIM_1ST;
			result += clientIpRange.getIpFrom();
			result += DELIM_2ND;			
			result += clientIpRange.getIpTo();
		}
		return result;
	}
	
	private String getSplitRoutingEntriesInfoAsStr(List<SplitRoutingEntry> splitRoutingEntries) {
		String result = "";
		for(SplitRoutingEntry splitRoutingEntry : splitRoutingEntries) {
			if(result.length() > 0)
				result += DELIM_1ST;
			result += splitRoutingEntry.getIp();
			result += DELIM_2ND;			
			result += splitRoutingEntry.getCidr();
			result += DELIM_2ND;
			result += splitRoutingEntry.getGateway() == null ? "" : splitRoutingEntry.getGateway();
		}
		return result;
	}
	
	public List<String> getKeyList(List<Map<String, Object>> list) {
		List<String> keyList = new ArrayList<String>();
		keyList.add("id");
		keyList.add("guid");
		keyList.add("name");
		keyList.add("description");
		keyList.add("allow_time_id");
		keyList.add("verify_client_ip");
		keyList.add("use_nac");
		keyList.add("ip_from");
		keyList.add("ip_to");
		keyList.add("ip_lease_ranges");		
		keyList.add("use_client_timeout");
		keyList.add("max_client_timeout");
		keyList.add("client_timeout");
		keyList.add("use_fail_limit");
		keyList.add("fail_limit");
		keyList.add("password_expiry");
		keyList.add("client_check_profile");
		keyList.add("created_at");
		keyList.add("updated_at");
		keyList.add("login_method");
		keyList.add("encryptions");
		keyList.add("password_change_alert");
		keyList.add("account_expiry_alert");
		keyList.add("device_auth_mode");
		keyList.add("client_ip_ranges");
		keyList.add("use_split_routing");
		keyList.add("split_routing_entries");
		keyList.add("user_unlock_time");
		keyList.add("popup_url");
		keyList.add("ldap_attributes");
		keyList.add("use_auto_reconnect");
		keyList.add("use_client_auto_uninstall");
		keyList.add("normal_acl");
		keyList.add("quarantine_acl");
		keyList.add("client_app");
		keyList.add("use_proxy");
		
		return keyList;
	}
}
