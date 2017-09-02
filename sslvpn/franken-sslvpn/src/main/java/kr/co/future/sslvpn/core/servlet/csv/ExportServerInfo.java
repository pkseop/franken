package kr.co.future.sslvpn.core.servlet.csv;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import kr.co.future.sslvpn.model.IpEndpoint;
import kr.co.future.sslvpn.model.PortRange;
import kr.co.future.sslvpn.model.Server;

public class ExportServerInfo implements ExportObject {

	@SuppressWarnings("unchecked")
   @Override
   public List<Map<String, Object>> convertToListMap(List<?> list) {
		List<Map<String, Object>> resultList = new ArrayList<Map<String, Object>>();
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ssZ");
		
		List<Server> servers = (List<Server>)list;
		for(Server server : servers) {
			Map<String, Object> m = new HashMap<String, Object>();
			
			m.put("guid", server.getGuid());
			m.put("name", server.getName());
			m.put("description", server.getDescription());
			m.put("operator", server.getOperator());
			m.put("phone", server.getPhone());
			m.put("created_at", dateFormat.format(server.getCreateDateTime()));
			m.put("updated_at", dateFormat.format(server.getUpdateDateTime()));
			m.put("endpoints", getEndPointInfoAsStr(server.getEndpoints()));
			
			resultList.add(m);
		}
		
	   return resultList;
   }
	
	private String getEndPointInfoAsStr(List<IpEndpoint> endpoints) {
		String result = "";
		for(IpEndpoint endpoint : endpoints) {
			if(result.length() > 0)
				result += DELIM_1ST;
			result += endpoint.getIp4Address();
			result += DELIM_2ND;			
			result += endpoint.getIp4Mask();
			result += DELIM_2ND;
			result += endpoint.getIp6Address();
			result += DELIM_2ND;
			result += endpoint.getIp6Mask();
			result += DELIM_2ND;
			result += endpoint.getDescription() == null ? "" : endpoint.getDescription();
			result += DELIM_2ND;
			result += getPortRangeInfo(endpoint.getPortRanges());		
			
		}
		return result;
	}
	
	private String getPortRangeInfo(List<PortRange> portRanges) {
		String result = "";
		for(PortRange portRange : portRanges) {
			if(result.length() > 0)
				result += DELIM_3RD;
			result += portRange.getProtocol();
			result += DELIM_4TH;
			result += portRange.getPortFrom();
			result += DELIM_4TH;
			result += portRange.getPortTo();
		}
		
		return result;
	}

	@Override
   public List<String> getKeyList(List<Map<String, Object>> list) {
		List<String> keyList = new ArrayList<String>();
		keyList.add("guid");
		keyList.add("name");
		keyList.add("description");
		keyList.add("operator");
		keyList.add("phone");
		keyList.add("created_at");
		keyList.add("updated_at");
		keyList.add("endpoints");
		
		return keyList;
   }

	

}
