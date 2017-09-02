package kr.co.future.sslvpn.core;

import java.util.List;

import kr.co.future.sslvpn.core.TrendGraphPoint;
import kr.co.future.sslvpn.core.TrendGraphType;
import kr.co.future.sslvpn.core.log.NicLog;

public interface PerformanceMonitor {
	List<TrendGraphPoint> getGraph(TrendGraphType type);
	
	void addNicStat(NicLog log);
}
