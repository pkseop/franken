package kr.co.future.sslvpn.core.cluster;

import java.net.InetAddress;
import java.net.UnknownHostException;

import kr.co.future.api.FieldOption;
import kr.co.future.sslvpn.core.cluster.ClusterNodeConfig;

public class ClusterNodeConfig {
	@FieldOption(nullable = false)
	private int nodeId;

	@FieldOption(nullable = false)
	private String publicIp;

	@FieldOption(nullable = false)
	private String ip;

	@FieldOption(nullable = false)
	private int port = 7140;

	public ClusterNodeConfig() {
	}

	public ClusterNodeConfig(int nodeId) {
		this.nodeId = nodeId;
	}

	public ClusterNodeConfig(int nodeId, String publicIp, String ip, int port) {
		this.nodeId = nodeId;
		this.publicIp = publicIp;
		this.ip = ip;
		this.port = port;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + nodeId;
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
		ClusterNodeConfig other = (ClusterNodeConfig) obj;
		if (nodeId != other.nodeId)
			return false;
		return true;
	}

	public int getNodeId() {
		return nodeId;
	}

	public void setNodeId(int nodeId) {
		this.nodeId = nodeId;
	}

	public String getPublicIp() {
		return publicIp;
	}

	public void setPublicIp(String publicIp) {
		if (publicIp != null) {
			try {
				InetAddress.getByName(publicIp);
			} catch (UnknownHostException e) {
				throw new IllegalArgumentException("invalid public ip address");
			}
		}

		this.publicIp = publicIp;
	}

	public String getIp() {
		return ip;
	}

	public void setIp(String ip) {
		this.ip = ip;
	}

	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}

	@Override
	public String toString() {
		return "node=" + nodeId + ", public ip=" + publicIp + ", ip=" + ip + ", port=" + port;
	}
}
