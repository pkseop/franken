package kr.co.future.sslvpn.model;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import kr.co.future.api.FieldOption;
import kr.co.future.confdb.CollectionName;
import kr.co.future.msgbus.Marshalable;

@CollectionName("default_access_profile")
public class DefaultAccessProfile implements Marshalable{
	private String guid = UUID.randomUUID().toString();
	
	@FieldOption(nullable = false, length = 60)
	private String name;

	public String getGuid() {
	   return guid;
   }

	public void setGuid(String guid) {
	   this.guid = guid;
   }
	
	public String getName() {
		return name;
	}
	
	public void setName(String name) {
		this.name = name;
	}

	@Override
   public Map<String, Object> marshal() {
		Map<String, Object> m = new HashMap<String, Object>();
		m.put("guid", guid);
		m.put("name", name);
		
	   return m;
   }
}
