package kr.co.future.sslvpn.model;

import java.util.HashMap;
import java.util.Map;

import kr.co.future.api.FieldOption;
import kr.co.future.confdb.CollectionName;
import kr.co.future.msgbus.Marshalable;

@CollectionName("system_status_backup")
public class SystemStatusBackup implements Marshalable {

	@FieldOption(name = "id", nullable = false)
	private int id;	
	
	@FieldOption(name = "password")
	private String password;
	
	@FieldOption(name = "use_backup", nullable=false)
	private Boolean useBackup;
	
	@FieldOption(name = "schedule")
	private String schedule;
	
	@FieldOption(name = "use_ftp", nullable=false)
	private Boolean useFtp;
	
	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public Boolean getUseBackup() {
		return useBackup;
	}

	public void setUseBackup(Boolean useBackup) {
		this.useBackup = useBackup;
	}

	public String getSchedule() {
		return schedule;
	}

	public void setSchedule(String schedule) {
		this.schedule = schedule;
	}

	public Boolean getUseFtp() {
		return useFtp;
	}

	public void setUseFtp(Boolean useFtp) {
		this.useFtp = useFtp;
	}

	@Override
	public Map<String, Object> marshal() {
		Map<String, Object> m = new HashMap<String, Object>();
		m.put("id", id);
		m.put("password", password);
		m.put("use_backup", useBackup);
		m.put("schedule", schedule);
		m.put("use_ftp", useFtp);
		
		return m;
	}

}
