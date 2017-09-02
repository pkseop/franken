package kr.co.future.sslvpn.model.api;

import java.util.Date;
import java.util.List;
import java.util.Set;

import kr.co.future.confdb.Predicate;
import kr.co.future.sslvpn.model.AccessProfile;
import kr.co.future.sslvpn.model.AuthorizedDevice;

public interface AuthorizedDeviceApi {

	int countDevices(Predicate pred);

	List<AuthorizedDevice> getDevices();

	List<AuthorizedDevice> getDevices(Set<String> guids);

	List<AuthorizedDevice> getDevices(int offset, int limit);

	List<AuthorizedDevice> getDevices(int offset, int limit, Predicate pred);

	AuthorizedDevice getDevice(String guid);

	AuthorizedDevice getDevice(Predicate pred);

	AuthorizedDevice findDeviceByKey(String deviceKey, String loginName, int osType, AccessProfile profile);

	boolean isAuthorized(String deviceKey, String loginName, AccessProfile profile);

	void registerDevice(AuthorizedDevice device);

	void registerDevices(List<AuthorizedDevice> devices);

	void unregisterDevice(String guid);

	void unregisterDevices(Set<String> guids);

	void updateDevices(List<AuthorizedDevice> devices);

	// block or unblock
	void blockDevice(String guid, boolean block);

	void setExpiration(String guid, Date expiration);

	void addListener(AuthorizedDeviceEventListener listener);

	void removeListener(AuthorizedDeviceEventListener listener);
}
