package kr.co.future.sslvpn.model.api;

//import kr.co.future.dom.api.EntityEventProvider;
import kr.co.future.sslvpn.model.IOSVpnServerConfig;
import kr.co.future.sslvpn.model.IOSVpnServerConfigParams;
import kr.co.future.sslvpn.model.VpnServerConfig;
import kr.co.future.sslvpn.model.VpnServerConfigParams;

public interface VpnServerConfigApi //extends EntityEventProvider<VpnServerConfig>
{
	public void setVpnServerConfig(VpnServerConfigParams c);
	
	public void setIOSVpnServerConfig(IOSVpnServerConfigParams c);
	
	public VpnServerConfig getCurrentVpnServerConfig();
	
	public IOSVpnServerConfig getCurrentIOSVpnServerConfig();
	
	public void updateVpnServerConfig(VpnServerConfig config);
	
	public void updateIOSVpnServerConfig(IOSVpnServerConfig config);
}
