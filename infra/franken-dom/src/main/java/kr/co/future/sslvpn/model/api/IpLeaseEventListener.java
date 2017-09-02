package kr.co.future.sslvpn.model.api;

import kr.co.future.sslvpn.model.IpLease;

public interface IpLeaseEventListener {
	void onLease(IpLease lease);

	void onExtend(IpLease lease);

	void onRelease(IpLease lease);
}
