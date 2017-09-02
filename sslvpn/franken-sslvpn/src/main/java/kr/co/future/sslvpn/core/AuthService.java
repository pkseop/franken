package kr.co.future.sslvpn.core;

import java.util.Map;

import kr.co.future.sslvpn.core.AuthCode;
import kr.co.future.sslvpn.core.TunnelEventListener;
import kr.co.future.sslvpn.model.AccessProfile;
import kr.co.future.dom.model.User;

public interface AuthService {
//	Collection<Tunnel> getTunnels();

	/**
	 * Can fetch only SSLVPN tunnel
	 * 
	 * @param id
	 *            the tunnel id
	 * @return the sslvpn tunnel
	 */
//	Tunnel getTunnel(int id);

	/**
	 * Fetch tunnel by lease ip
	 * 
	 * @param leaseIp
	 *            the lease ip
	 * @return the all kinds of tunnel
	 */
//	Tunnel getTunnel(InetAddress leaseIp);

	/**
	 * 외부 연동(RADIUS, LDAP, SQL, EXTERNAL)까지 상정하여 적용될 정책을 계산해야 하는 경우에 이 메소드를 사용.
	 */
	AccessProfile determineProfile(String loginName);

	AccessProfile determineProfile(String loginName, User user);

	void deployPolicy();

//	void openL2TPTunnel(String loginName, InetAddress leaseIp, InetSocketAddress remote);

//	void closeL2TPTunnel(InetAddress leaseIp);

//	void openXAuthTunnel(String loginName, InetAddress leaseIp, InetSocketAddress remote);

//	void closeXAuthTunnel(InetAddress leaseIp);

	void killTunnel(int id);

//	void syncTunnel() throws RpcException, InterruptedException;

	Map<String, Object> login(Map<String, Object> props);

	AuthCode checkPassword(String loginName, String password);

	void logout(int tunnelId);

	void addTunnelEventListener(TunnelEventListener listener);

	void removeTunnelEventListener(TunnelEventListener listener);

	void changePassword(String loginName, String newPassword);
}
