package kr.co.future.sslvpn.core.backup;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import kr.co.future.msgbus.Marshalable;

public class FtpBackup implements Marshalable {
	public String file_name;
	public String type;
	public String file_size;
	public Timestamp backup_date;
	public int state_code;
	
	@Override
	public Map<String, Object> marshal() {
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm");
		Map<String, Object> m = new HashMap<String, Object>();
		m.put("file_name", file_name);
		m.put("type", type);
		m.put("file_size", file_size);
		m.put("backup_date", dateFormat.format(new Date(backup_date.getTime())));
		m.put("state_code", state_code);
		
		return m;
	}
	
}
