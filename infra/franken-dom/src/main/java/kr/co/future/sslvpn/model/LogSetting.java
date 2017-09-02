package kr.co.future.sslvpn.model;

import java.util.HashMap;
import java.util.Map;

import kr.co.future.api.FieldOption;
import kr.co.future.confdb.CollectionName;
import kr.co.future.msgbus.Marshalable;

@CollectionName("log_setting")
public class LogSetting implements Marshalable{
	
	@FieldOption(name = "id", nullable = false)
	private int id;	

	@FieldOption(name = "ftp_use", nullable=false)
	private Boolean ftpUse;

	@FieldOption(name = "ftp_type", nullable=false)
	private Boolean ftpType;			//true is sftp. false is ftp.
	
	@FieldOption(name = "ftp_id")
	private String ftpId;
	
	@FieldOption(name = "ftp_pw")
	private String ftpPw;
	
	@FieldOption(name = "ftp_ip")
	private String ftpIp;
	
	@FieldOption(name = "ftp_path")
	private String ftpPath;	

	@FieldOption(name = "backup_use", nullable=false)
	private Boolean backupUse;
	
	@FieldOption(name = "backup_maintain_days", nullable=false)
	private Integer backupMaintainDays;
	
	@FieldOption(name = "backup_schedule")
	private String backupSchedule;
	
	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}
	
	public Boolean getFtpUse() {
		return ftpUse;
	}

	public void setFtpUse(Boolean ftpUse) {
		this.ftpUse = ftpUse;
	}

	public Boolean getFtpType() {
		return ftpType;
	}

	public void setFtpType(Boolean ftpType) {
		this.ftpType = ftpType;
	}

	public String getFtpId() {
		return ftpId;
	}

	public void setFtpId(String ftpId) {
		this.ftpId = ftpId;
	}

	public String getFtpPw() {
		return ftpPw;
	}

	public void setFtpPw(String ftpPw) {
		this.ftpPw = ftpPw;
	}

	public String getFtpIp() {
		return ftpIp;
	}

	public void setFtpIp(String ftpIp) {
		this.ftpIp = ftpIp;
	}
	
	public String getFtpPath() {
		return ftpPath;
	}

	public void setFtpPath(String ftpPath) {
		this.ftpPath = ftpPath;
	}

	public Boolean getBackupUse() {
		return backupUse;
	}

	public void setBackupUse(Boolean backupUse) {
		this.backupUse = backupUse;
	}

	public Integer getBackupMaintainDays() {
		return backupMaintainDays;
	}

	public void setBackupMaintainDays(Integer backupMaintainDays) {
		this.backupMaintainDays = backupMaintainDays;
	}

	public String getBackupSchedule() {
		return backupSchedule;
	}

	public void setBackupSchedule(String backupSchedule) {
		this.backupSchedule = backupSchedule;
	}
	
	@Override
   public Map<String, Object> marshal() {
		Map<String, Object> m = new HashMap<String, Object>();
		m.put("id", id);
		m.put("ftp_use", ftpUse);
		m.put("ftp_type", ftpType);
		m.put("ftp_id", ftpId);
		m.put("ftp_pw", ftpPw);
		m.put("ftp_ip", ftpIp);
		m.put("ftp_path", ftpPath);
		m.put("backup_use", backupUse);
		m.put("backup_maintain_days", backupMaintainDays);
		m.put("backup_schedule", backupSchedule);
		
	   return m;
   }
}
