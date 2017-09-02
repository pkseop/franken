package kr.co.future.sslvpn.core.cluster;

public class ClusterContext {
	public static ThreadLocal<Boolean> replicated = new ThreadLocal<Boolean>();
}
