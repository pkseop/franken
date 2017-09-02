package kr.co.future.sslvpn.core.log;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import kr.co.future.sslvpn.xtmconf.system.InterfaceInfo;

import kr.co.future.logstorage.Log;

public class SystemLog {

	private Date date;

	// 1. cpu
	private int cpuUsage;
	private int cpuIdle;
	private int cpuUser;
	private int cpuSystem;

	// 2. memory
	private int memoryUsage;
	private long memoryTotal;
	private long memoryCached;
	private long memoryFree;

	// 3. hdd
	private int diskUsage;
	private long diskTotal;
	private long diskFree;

	// 4. Interface 정보 (CRC 에러, Duplex, 속도, IP 정보 등)
	private List<InterfaceInfo> iface;

	// cluster 정보
	private String rpcConnection;

	public List<InterfaceInfo> getIface() {
		return iface;
	}

	public void setIface(List<InterfaceInfo> iface) {
		this.iface = iface;
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

	public String getRpcConnection() {
		return rpcConnection;
	}

	public void setRpcConnection(String rpcConnection) {
		this.rpcConnection = rpcConnection;
	}

	public Date getDate() {
		return date;
	}

	public void setDate(Date date) {
		this.date = date;
	}

	public Log toLog() {
		Map<String, Object> m = new HashMap<String, Object>();
		m.put("cpuUsage", cpuUsage);
		m.put(" cpuIdle", cpuIdle);
		m.put(" cpuUser", cpuUser);
		m.put(" cpuSystem", cpuSystem);
		m.put(" memoryUsage", memoryUsage);
		m.put(" memoryTotal", memoryTotal);
		m.put(" memoryCached", memoryCached);
		m.put(" memoryFree", memoryFree);
		m.put(" diskUsage", diskUsage);
		m.put(" diskTotal", diskTotal);
		m.put(" diskFree", diskFree);
		m.put(" iface", iface);
		m.put(" rpcConnection", rpcConnection);

		return new Log("ssl-system", date, m);
	}
}
