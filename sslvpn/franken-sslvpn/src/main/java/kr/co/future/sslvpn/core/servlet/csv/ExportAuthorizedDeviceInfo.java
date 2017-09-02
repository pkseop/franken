package kr.co.future.sslvpn.core.servlet.csv;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import kr.co.future.sslvpn.model.AuthorizedDevice;

public class ExportAuthorizedDeviceInfo implements ExportObject {

	@SuppressWarnings("unchecked")
   @Override
   public List<Map<String, Object>> convertToListMap(List<?> list) {
		List<Map<String, Object>> resultList = new ArrayList<Map<String, Object>>();
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ssZ");
		
		List<AuthorizedDevice> authDevices = (List<AuthorizedDevice>)list;
		for (AuthorizedDevice authDevice : authDevices) {
			Map<String, Object> m = new HashMap<String, Object>();
			
			m.put("guid", authDevice.getGuid());
			m.put("device_key", authDevice.getDeviceKey());
			m.put("type", authDevice.getType());
			m.put("host_name", authDevice.getHostName());
			m.put("description", authDevice.getDescription());
			m.put("owner", authDevice.getOwner());
			m.put("remote_ip", authDevice.getRemoteIp());
			m.put("is_blocked", authDevice.isBlocked());
			m.put("expiration", authDevice.getExpiration() == null ? null : dateFormat.format(authDevice.getExpiration()));
			m.put("login_name", authDevice.getLoginName());
			m.put("hdd_serial", authDevice.getHddSerial());
			m.put("mac_address", authDevice.getMacAddress());
			m.put("remote_client_ip", authDevice.getRemoteClientip());
			m.put("is_authorized", authDevice.getIsAuthorized());
			m.put("org_unit_name", authDevice.getOrgUnitName());
			
			resultList.add(m);
		}
	   return resultList;
   }

	@Override
   public List<String> getKeyList(List<Map<String, Object>> list) {
		List<String> keyList = new ArrayList<String>();
		keyList.add("guid");
		keyList.add("device_key");
		keyList.add("type");
		keyList.add("host_name");
		keyList.add("owner");
		keyList.add("remote_ip");
		keyList.add("is_blocked");
		keyList.add("expiration");
		keyList.add("login_name");
		keyList.add("hdd_serial");
		keyList.add("mac_address");
		keyList.add("remote_client_ip");
		keyList.add("is_authorized");
		keyList.add("org_unit_name");
		
		return keyList;
   }

}
