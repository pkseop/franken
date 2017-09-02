package kr.co.future.sslvpn.model.api;

import java.net.InetAddress;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import kr.co.future.dom.api.UserExtensionProvider;
import kr.co.future.dom.model.User;
import kr.co.future.sslvpn.model.UserExtension;

public interface UserApi extends UserExtensionProvider {
	public UserExtension checkAuthKey(String authKey);

	public List<UserExtension> getUserExtensions();

	public List<UserExtension> getUserExtensions(Set<String> loginNames);

	public List<UserExtension> getUserExtensions(Collection<User> users);

	public UserExtension getUserExtension(User user);

	public UserExtension findUserExtension(String loginName);

	public UserExtension findUserExtension(InetAddress reservedIp);

	public UserExtension findUserExtension(String exceptLoginName, InetAddress reservedIp);

	public void setUserExtension(UserExtension ext);

	public void removeUserExtension(String loginName);
	
	public void setDomUserCount(int count, Boolean flag);
	
	public int getDomUserCount();
	
	public List<UserExtension> getUserExtsWithStaticIp4();
}
