package kr.co.future.sslvpn.core;

import java.util.Collection;
import java.util.List;

import kr.co.future.sslvpn.model.DnsCheck;

public interface DnsCheckService {
	Collection<DnsCheck> getDnsCheckList();

	DnsCheck getDnsCheck(String domain);

	void createDnsCheck(DnsCheck dnsCheck);

	void updateDnsCheck(DnsCheck dnsCheck);

	void removeDnsChecks(List<String> domains);
}
