package kr.co.future.sslvpn.model.api;

public interface ClusteredIpLeaseApi {
	/**
	 * 
	 * @param loginName
	 *            the login name. lease() will use login name's hash code for
	 *            offset allocation
	 * @param profileName
	 *            the profile name
	 * @param poolSize
	 *            the ip address pool size for specified access profile
	 * @return the offset for profile's ip address pool. offset will be unique
	 *         for login name and profile name. -1 for lease fail.
	 */
	int lease(String loginName, String profileName, int poolSize);

	/**
	 * record lease information from cluster master
	 */
	void record(String loginName, String profileName, int poolSize, int offset);
}
