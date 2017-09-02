package kr.co.future.sslvpn.core.log;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import kr.co.future.logstorage.Log;

public class LogCapacityLog {

	private Date date;

	private String tableName;

	private long beforeCapacity;

	private long incrementCapacity;

	private long total;

	public Date getDate() {
		return date;
	}

	public void setDate(Date date) {
		this.date = date;
	}

	public String getTableName() {
		return tableName;
	}

	public void setTableName(String table_name) {
		this.tableName = table_name;
	}

	public long getBeforeCapacity() {
		return beforeCapacity;
	}

	public void setBeforeCapacity(long before_capacity) {
		this.beforeCapacity = before_capacity;
	}

	public long getIncrementCapacity() {
		return incrementCapacity;
	}

	public void setIncrementCapacity(long increment_capacity) {
		this.incrementCapacity = increment_capacity;
	}

	public long getTotal() {
		return total;
	}

	public void setTotal(long total) {
		this.total = total;
	}

	public Log toLog() {
		Map<String, Object> m = new HashMap<String, Object>();
		m.put("table_name", tableName);
		m.put("before_capacity", beforeCapacity);
		m.put("increment_capacity", incrementCapacity);
		m.put("total", total);

		return new Log("log-capacity", date, m);
	}

	@Override
	public String toString() {
		return "CapacityLog [date=" + date + ", tableName=" + tableName + ", beforeCapacity=" + beforeCapacity
				+ ", incrementCapacity=" + incrementCapacity + ", total=" + total + "]";
	}
}
