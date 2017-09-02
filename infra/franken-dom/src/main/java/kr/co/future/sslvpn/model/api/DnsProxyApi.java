package kr.co.future.sslvpn.model.api;

import java.net.InetAddress;
import java.util.List;

import kr.co.future.sslvpn.model.DnsZone;
import kr.co.future.dom.api.EntityEventProvider;

public interface DnsProxyApi extends EntityEventProvider<DnsZone>  {
	List<DnsZone> getDnsZones();

	DnsZone getDnsZone(String domain);
	
	void createDnsZone(DnsZone dns);
	
	void updateDnsZone(DnsZone dns);
	
	void removeDnsZones(List<String> domains);	
	
	List<InetAddress> getDnsIpList(String domain);
}
