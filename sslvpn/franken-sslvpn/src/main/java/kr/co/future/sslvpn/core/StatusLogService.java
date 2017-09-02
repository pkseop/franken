package kr.co.future.sslvpn.core;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import kr.co.future.sslvpn.core.AuthService;
import kr.co.future.sslvpn.core.StatusLogService;
import kr.co.future.sslvpn.core.log.IfStatsLog;
import kr.co.future.sslvpn.core.log.PerfLog;
//import kr.co.future.sslvpn.core.log.TunnelCountLog;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;

import kr.co.future.cron.PeriodicJob;
import kr.co.future.linux.api.CpuStat;
import kr.co.future.linux.api.MemoryStat;
import kr.co.future.linux.api.NetworkInterfaceInformation;
import kr.co.future.logstorage.Log;
import kr.co.future.logstorage.LogStorage;
import kr.co.future.logstorage.LogStorageStatus;
import kr.co.future.sslvpn.core.xenics.XenicsService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(name = "frodo-status-log-service")
@Provides
@PeriodicJob("*/5 * * * *")
public class StatusLogService implements Runnable {
	private final Logger logger = LoggerFactory.getLogger(StatusLogService.class.getName());

	@Requires
	private AuthService auth;

	@Requires
	private LogStorage storage;

	@Requires
	private XenicsService xenicsService;
	
	@Override
	public void run() {
		handleStatus();
	}

	private void handleStatus() {
		handleperfLog();
//		handleTunnelCountLog();
		handleIfStatsLog();
	}

	private void handleIfStatsLog() {
		Map<String, Object> network = new HashMap<String, Object>();
		try {
			network = getNetworkInterfaceInforms();
		} catch (IOException e) {
			logger.debug("frodo core: network interface File error");
		}

		// create packetLog
		Collection<Log> logs = new ArrayList<Log>();
		for (String interfaceName : network.keySet()) {
			@SuppressWarnings("unchecked")
			Map<String, Object> data = (Map<String, Object>) network.get(interfaceName);
			IfStatsLog ifStatsLog = new IfStatsLog();
			ifStatsLog.setDate(new Date());
			ifStatsLog.setName(interfaceName);
			ifStatsLog.setRxBytes((Long) data.get("rx_bytes"));
			ifStatsLog.setRxPackets((Long) data.get("rx_packets"));
			ifStatsLog.setRxErrs((Long) data.get("rx_errs"));
			ifStatsLog.setRxDrop((Long) data.get("rx_drop"));
			ifStatsLog.setRxFifo((Long) data.get("rx_fifo"));
			ifStatsLog.setRxFrame((Long) data.get("rx_frame"));
			ifStatsLog.setRxCompressed((Long) data.get("rx_compressed"));
			ifStatsLog.setRxMulticast((Long) data.get("rx_multicast"));
			ifStatsLog.setTxBytes((Long) data.get("tx_bytes"));
			ifStatsLog.setTxPackets((Long) data.get("tx_packets"));
			ifStatsLog.setTxErrs((Long) data.get("tx_errs"));
			ifStatsLog.setTxDrop((Long) data.get("tx_drop"));
			ifStatsLog.setTxFifo((Long) data.get("tx_fifo"));
			ifStatsLog.setTxColls((Long) data.get("tx_colls"));
			ifStatsLog.setTxCarrier((Long) data.get("tx_carrier"));
			ifStatsLog.setTxCompressed((Long) data.get("tx_compressed"));
			Log l = ifStatsLog.toLog();

			logs.add(l);
		}

		if (storage.getStatus() == LogStorageStatus.Open)
			storage.write(logs);
	}

//	private void handleTunnelCountLog() {
//		// create tunnelLog
//		TunnelCountLog tunnelCountLog = new TunnelCountLog();
//		tunnelCountLog.setDate(new Date());
//		tunnelCountLog.setTunnelCount(xenicsService.getTotalNumOfTunnels()/*auth.getTunnels().size()*/);
//		Log l = tunnelCountLog.toLog();
//
//		if (storage.getStatus() == LogStorageStatus.Open)
//			storage.write(l);
//	}

	private void handleperfLog() {
		MemoryStat mem;
		int memoryUsage;
		long memoryTotal = 0;
		long memoryCached = 0;
		long memoryFree = 0;
		try {
			mem = MemoryStat.getMemoryStat();
			memoryUsage = (int) ((mem.getMemTotal() - mem.getMemFree() - mem.getCached()) * 100 / mem.getMemTotal());
			memoryTotal = mem.getMemTotal();
			memoryCached = mem.getCached();
			memoryFree = mem.getMemFree();
		} catch (IOException e) {
			logger.debug("frodo core: MemoryStat File error");
			memoryUsage = 0;
		}

		int cpuUsage;
		int cpuIdle = 0;
		int cpuUser = 0;
		int cpuSystem = 0;

		try {
			cpuUsage = CpuStat.getCpuUsage().getUsage();
			cpuIdle = CpuStat.getCpuUsage().getIdle();
			cpuUser = CpuStat.getCpuUsage().getUser();
			cpuSystem = CpuStat.getCpuUsage().getSystem();

		} catch (InterruptedException e) {
			logger.debug("frodo core: CpuStat Interrupted");
			cpuUsage = 0;
		} catch (IOException e) {
			logger.debug("frodo core: CpuStat File error");
			cpuUsage = 0;
		}

		File f = new File("/utm/log");
		long totalUsed = f.getTotalSpace() - f.getFreeSpace();
		long total = f.getTotalSpace();

		// create statusLog
		PerfLog perfLog = new PerfLog();
		perfLog.setDate(new Date());
		perfLog.setCpuUsage(cpuUsage);
		perfLog.setCpuIdle(cpuIdle);
		perfLog.setCpuUser(cpuUser);
		perfLog.setCpuSystem(cpuSystem);
		perfLog.setMemoryUsage(memoryUsage);
		perfLog.setMemoryCached(memoryCached);
		perfLog.setMemoryFree(memoryFree);
		perfLog.setMemoryTotal(memoryTotal);
		perfLog.setDiskUsage((int) (totalUsed * 100 / total));
		perfLog.setDiskTotal(f.getTotalSpace());
		perfLog.setDiskFree(f.getFreeSpace());
		Log l = perfLog.toLog();

		if (storage.getStatus() == LogStorageStatus.Open)
			storage.write(l);
	}

	private Map<String, Object> getNetworkInterfaceInforms() throws IOException {

		Map<String, Object> result = new HashMap<String, Object>();
		for (NetworkInterfaceInformation ni : NetworkInterfaceInformation.getNetworkInterfaceInformations()) {
			if (ni.getName().contains("eth") || ni.getName().contains("tun")) {
				Map<String, Object> data = new HashMap<String, Object>();
				data.put("rx_bytes", ni.getRxBytes());
				data.put("rx_packets", ni.getRxPackets());
				data.put("rx_errs", ni.getRxErrs());
				data.put("rx_drop", ni.getRxDrop());
				data.put("rx_fifo", ni.getRxFifo());
				data.put("rx_frame", ni.getRxFrame());
				data.put("rx_compressed", ni.getRxCompressed());
				data.put("rx_multicast", ni.getRxMulticast());

				data.put("tx_bytes", ni.getTxBytes());
				data.put("tx_packets", ni.getTxPackets());
				data.put("tx_errs", ni.getTxErrs());
				data.put("tx_drop", ni.getTxDrop());
				data.put("tx_fifo", ni.getTxFifo());
				data.put("tx_colls", ni.getTxColls());
				data.put("tx_carrier", ni.getTxCarrier());
				data.put("tx_compressed", ni.getTxCompressed());

				result.put(ni.getName(), data);
			}
		}
		return result;
	}

}
