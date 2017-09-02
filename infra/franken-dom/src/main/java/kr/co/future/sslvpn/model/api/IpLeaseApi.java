package kr.co.future.sslvpn.model.api;

import java.net.InetAddress;
import java.util.List;

import kr.co.future.dom.model.User;
import kr.co.future.sslvpn.model.IpLease;
import kr.co.future.sslvpn.model.IpLeaseRange;

public interface IpLeaseApi {
	List<IpLeaseRange> getFilteredRanges(List<IpLeaseRange> ranges, long from, long to);

	List<Integer> getPoolSizeOffsets(List<IpLeaseRange> ranges);

	long calculateIpAddress(List<IpLeaseRange> ranges, List<Integer> poolOffsets, int offset);

	List<IpLease> getAllLeases();

	List<IpLease> getLeases(String loginName);

	InetAddress request(int tunnelId, String loginName, InetAddress tapIp, InetAddress tapNetmask);

	InetAddress request(int tunnelId, User user, InetAddress tapIp, InetAddress tapNetmask, boolean retry);

	void release(int tunnelId);

	void addListener(IpLeaseEventListener listener);

	void removeListener(IpLeaseEventListener listener);
}
