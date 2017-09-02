package kr.co.future.sslvpn.core;

import java.util.concurrent.ConcurrentMap;

public interface DupLoginCheck {
	public void setUseDuplicateLoginCheck(boolean use);
	
	public boolean setNodes(String nodes);
	
	public boolean useDuplicateLoginCheck();
	
	public boolean isDuplicateLogin(String loginName);
	
	public void sendLoginInfo(String loginName);
	
	public void sendLogoutInfo(String loginName);
	
	public ConcurrentMap<String, String> getLoginInfoMap();
	
	public void killDuplicateLoginTunnel(String loginName);
	
	public boolean isBlock();
}
