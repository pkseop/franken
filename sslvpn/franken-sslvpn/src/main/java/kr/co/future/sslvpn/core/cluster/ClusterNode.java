package kr.co.future.sslvpn.core.cluster;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Date;

import kr.co.future.sslvpn.core.cluster.ClusterNode;
import kr.co.future.rpc.RpcClient;
import kr.co.future.rpc.RpcSession;

public class ClusterNode {
	private int id;

	private InetAddress publicIp;

	private InetSocketAddress address;

	// fetch using rpc
	private Date timestamp;

	// fetch using rpc
	private boolean available;

	private RpcClient client;

	private RpcSession session;

	// fetch using rpc
	private int tunnelCount;

	public int getTunnelCount() {
		return tunnelCount;
	}

	public void setTunnelCount(int tunnelCount) {
		this.tunnelCount = tunnelCount;
	}

	public ClusterNode(int id, InetSocketAddress address) {
		this(id, address, address.getAddress());
	}

	public ClusterNode(int id, InetSocketAddress address, InetAddress publicIp) {
		this.id = id;
		this.address = address;
		this.publicIp = publicIp;
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public InetAddress getPublicIp() {
		return publicIp;
	}

	public void setPublicIp(InetAddress publicIp) {
		this.publicIp = publicIp;
	}

	public InetSocketAddress getAddress() {
		return address;
	}

	public void setAddress(InetSocketAddress address) {
		this.address = address;
	}

	public Date getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(Date timestamp) {
		this.timestamp = timestamp;
	}

	public boolean isAvailable() {
		return available;
	}

	public void setAvailable(boolean available) {
		this.available = available;
	}

	public RpcClient getClient() {
		return client;
	}

	public void setClient(RpcClient client) {
		this.client = client;
	}

	public RpcSession getSession() {
		return session;
	}

	public void setSession(RpcSession session) {
		this.session = session;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + id;
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ClusterNode other = (ClusterNode) obj;
		if (id != other.id)
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "id=" + id + ", public ip=" + publicIp + ", address=" + address + ", timestamp=" + timestamp + ", available="
				+ available + ", tunnel count=" + tunnelCount + ", session=" + (session != null);
	}
}
