package kr.co.future.sslvpn.core.impl;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import kr.co.future.sslvpn.core.AuthService;
import kr.co.future.sslvpn.core.TrendGraphPoint;
import kr.co.future.sslvpn.core.TrendGraphType;
import kr.co.future.sslvpn.core.Tunnel;
import kr.co.future.sslvpn.core.log.NicLog;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Invalidate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.apache.felix.ipojo.annotations.Validate;

import kr.co.future.msgbus.PushApi;
import kr.co.future.linux.api.CpuStat;
import kr.co.future.linux.api.MemoryStat;
import kr.co.future.rrd.ConsolidateFunc;
import kr.co.future.rrd.DefaultRrd;
import kr.co.future.rrd.FetchResult;
import kr.co.future.rrd.Rrd;
import kr.co.future.rrd.RrdConfig;
import kr.co.future.rrd.DataSourceType;
import kr.co.future.rrd.FetchRow;
import kr.co.future.rrd.io.MemoryPersistentLayer;
import kr.co.future.sslvpn.core.PerformanceMonitor;
import kr.co.future.sslvpn.core.impl.PerformanceMonitorImpl;
import kr.co.future.sslvpn.core.xenics.XenicsService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(name = "frodo-perfmon")
@Provides(specifications = { PerformanceMonitor.class })
public class PerformanceMonitorImpl implements PerformanceMonitor, Runnable {
	private final Logger logger = LoggerFactory.getLogger(PerformanceMonitorImpl.class.getName());

	private Rrd cpuRrd;
	private Rrd memRrd;
	private Rrd diskRrd;
	private Rrd rxRrd; // bps
	private Rrd txRrd; // bps
	private Rrd tunnelRrd;

	private Thread t;
	private volatile boolean doStop;

	@Requires
	private PushApi pushApi;

	@Requires
	private AuthService auth;
	
	@Requires
	private XenicsService xenicsService;

	@Override
	public void addNicStat(NicLog log) {
		txRrd.update(log.getDate(), new Double[] { (double) log.getTxBytes() /** 8*/ });  //bit로 보내지므로 받지 곱하기를 하지 않음.
		rxRrd.update(log.getDate(), new Double[] { (double) log.getRxBytes() /** 8 */});
	}

	@Override
	public List<TrendGraphPoint> getGraph(TrendGraphType type) {
		Date end = new Date(new Date().getTime() - 1);
		Date begin = new Date(end.getTime() - 59 * 1000);

		switch (type) {
		case Cpu:
			return convert(cpuRrd.fetch(ConsolidateFunc.AVERAGE, begin, end, 1));
		case Memory:
			return convert(memRrd.fetch(ConsolidateFunc.AVERAGE, begin, end, 1));
		case Disk:
			return convert(diskRrd.fetch(ConsolidateFunc.AVERAGE, begin, end, 1));
		case RxBytes:
			return convert(rxRrd.fetch(ConsolidateFunc.AVERAGE, begin, end, 1));
		case TxBytes:
			return convert(txRrd.fetch(ConsolidateFunc.AVERAGE, begin, end, 1));
		case Tunnel:
			return convert(tunnelRrd.fetch(ConsolidateFunc.AVERAGE, begin, end, 1));
		}

		return null;
	}

	private List<TrendGraphPoint> convert(FetchResult result) {
		List<TrendGraphPoint> l = new ArrayList<TrendGraphPoint>(60);
		for (FetchRow row : result.getRows()) {
			Date date = row.getDate();
			Double value = row.getColumns()[0];

			l.add(new TrendGraphPoint(date, value));
		}

		return l;
	}

	@Validate
	public void start() {
		doStop = false;
		resetRrds();

		t = new Thread(this, "Frodo Perfmon");
		t.start();
	}

	private void resetRrds() {
		cpuRrd = newRrd(DataSourceType.GAUGE);
		memRrd = newRrd(DataSourceType.GAUGE);
		diskRrd = newRrd(DataSourceType.GAUGE);
		tunnelRrd = newRrd(DataSourceType.GAUGE);
		txRrd = newRrd(DataSourceType.ABSOLUTE);		//xenics에서 받은 수치를 그대로 반영하기 위해 ABSOLUTE 방식으로 함.
		rxRrd = newRrd(DataSourceType.ABSOLUTE);
	}

	@Invalidate
	public void stop() {
		doStop = true;
		t.interrupt();
		cpuRrd = memRrd = diskRrd = null;
		t = null;
	}

	@Override
	public void run() {
		doStop = false;
		try {
			while (!doStop) {
				try {
					update();
					Thread.sleep(900);
				} catch (InterruptedException e) {
					logger.trace("frodo core: perfmon interrupted");
				} catch (FileNotFoundException e) {
					logger.trace("frodo core: /proc unmounted", e);
					try {
						Thread.sleep(1000);
					} catch (InterruptedException e1) {
					}
				} catch (Exception e) {
					logger.error("frodo core: cannot update perf metric", e);
				}
			}
		} finally {
			logger.info("frodo core: perfmon thread stopped");
		}
	}

	private void update() throws IOException, InterruptedException {
		try {
			MemoryStat mem = MemoryStat.getMemoryStat();
			int cpuUsage = CpuStat.getCpuUsage(100).getUsage();
			int memUsage = (int) ((mem.getMemTotal() - mem.getCached() - mem.getMemFree()) * 100 / mem.getMemTotal());

			File f = new File("/utm/log");
			long totalDiskSpace = f.getTotalSpace();
			long totalDiskUsage = (f.getTotalSpace() - f.getFreeSpace());
			int diskUsage = (int) (totalDiskUsage * 100 / totalDiskSpace);

			// update rrd
			Date now = new Date();
			cpuRrd.update(now, new Double[] { (double) cpuUsage });
			memRrd.update(now, new Double[] { (double) memUsage });
			diskRrd.update(now, new Double[] { (double) diskUsage });

			int tunnelSize = 0;
			if (auth != null) {
//				Collection<Tunnel> tunnels = auth.getTunnels();
				int tunnelTotalNum = xenicsService.getTotalNumOfTunnels();
				tunnelRrd.update(now, new Double[] { (double)tunnelTotalNum /*tunnels.size()*/ });
				tunnelSize = tunnelTotalNum;//tunnels.size();
			}

			// push
			Map<String, Object> m = new HashMap<String, Object>();
			m.put("cpu", cpuUsage);
			m.put("mem", memUsage);
			m.put("disk", diskUsage);
			m.put("tunnel", tunnelSize);

			pushApi.push("localhost", "frodo-perf", m);
		} catch (IllegalArgumentException e) {
			if (e.getMessage().contains("not after")) {
				resetRrds();
			}
		}
	}

	private Rrd newRrd(DataSourceType type) {
		RrdConfig config = new RrdConfig(new Date(new Date().getTime() - 60 * 1000), 1);
		config.addDataSource("usage", type, 1, 0, 100);
		config.addArchive(ConsolidateFunc.AVERAGE, 0.9, 1, 60);
		DefaultRrd rrd = new DefaultRrd(new MemoryPersistentLayer(), config);
		return rrd;
	}
}
