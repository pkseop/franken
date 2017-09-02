package kr.co.future.sslvpn.core.log;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import kr.co.future.logstorage.Log;

public class PerfLog {
	private Date date;

	int cpuUsage;
	int cpuIdle;
	int cpuUser;
	int cpuSystem;

	int memoryUsage;
	long memoryTotal;
	long memoryCached;
	long memoryFree;

	int diskUsage;
	long diskTotal;
	long diskFree;

	public Date getDate() {
		return date;
	}

	public void setDate(Date date) {
		this.date = date;
	}

	public int getCpuUsage() {
		return cpuUsage;
	}

	public void setCpuUsage(int cpuUsage) {
		this.cpuUsage = cpuUsage;
	}

	public int getCpuIdle() {
		return cpuIdle;
	}

	public void setCpuIdle(int cpuIdle) {
		this.cpuIdle = cpuIdle;
	}

	public int getCpuUser() {
		return cpuUser;
	}

	public void setCpuUser(int cpuUser) {
		this.cpuUser = cpuUser;
	}

	public int getCpuSystem() {
		return cpuSystem;
	}

	public void setCpuSystem(int cpuSystem) {
		this.cpuSystem = cpuSystem;
	}

	public int getMemoryUsage() {
		return memoryUsage;
	}

	public void setMemoryUsage(int memoryUsage) {
		this.memoryUsage = memoryUsage;
	}

	public long getMemoryTotal() {
		return memoryTotal;
	}

	public void setMemoryTotal(long memoryTotal) {
		this.memoryTotal = memoryTotal;
	}

	public long getMemoryCached() {
		return memoryCached;
	}

	public void setMemoryCached(long memoryCached) {
		this.memoryCached = memoryCached;
	}

	public long getMemoryFree() {
		return memoryFree;
	}

	public void setMemoryFree(long memoryFree) {
		this.memoryFree = memoryFree;
	}

	public int getDiskUsage() {
		return diskUsage;
	}

	public void setDiskUsage(int diskUsage) {
		this.diskUsage = diskUsage;
	}

	public long getDiskTotal() {
		return diskTotal;
	}

	public void setDiskTotal(long diskTotal) {
		this.diskTotal = diskTotal;
	}

	public long getDiskFree() {
		return diskFree;
	}

	public void setDiskFree(long diskFree) {
		this.diskFree = diskFree;
	}

	public Log toLog() {
		Map<String, Object> m = new HashMap<String, Object>();
		m.put("cpu_usage", cpuUsage);
		m.put("cpu_idle", cpuIdle);
		m.put("cpu_user", cpuUser);
		m.put("cpu_system", cpuSystem);
		m.put("mem_usage", memoryUsage);
		m.put("mem_total", memoryTotal);
		m.put("mem_cached", memoryCached);
		m.put("mem_free", memoryFree);
		m.put("disk_usage", diskUsage);
		m.put("disk_total", diskTotal);
		m.put("disk_free", diskFree);

		return new Log("perf", date, m);
	}

}
