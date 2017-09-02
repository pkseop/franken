package kr.co.future.sslvpn.model;

import java.util.HashMap;
import java.util.Map;

import kr.co.future.api.FieldOption;
import kr.co.future.confdb.CollectionName;
import kr.co.future.msgbus.Marshalable;

@CollectionName("twoway_auth_config")
public class TwowayAuthConfig implements Marshalable{
	@FieldOption(name = "ip", length = 20, nullable = false)
	private String ip;

	@FieldOption(name = "port", nullable = false)
	private Integer port;
	
	@FieldOption(name = "sms_mcs_msg", length = 400, nullable = false)
	private String smsMcsMsg;
	
	public String getIp() {
		return ip;
	}
	
	public void setIp(String ip) {
		this.ip = ip;
	}
	
	public Integer getPort() {
		return port;
	}
	
	public void setPort(Integer port) {
		this.port = port;
	}
	
	public String getSmsMcsMsg() {
		return smsMcsMsg;
	}
	
	public void setSmsMcsMsg(String smsMcsMsg) {
		this.smsMcsMsg = smsMcsMsg;
	}

	@Override
   public Map<String, Object> marshal() {
		Map<String, Object> m = new HashMap<String, Object>();
		m.put("ip", ip);
		m.put("port", port);
		m.put("sms_mcs_msg", smsMcsMsg);
	   return m;
   }
}
