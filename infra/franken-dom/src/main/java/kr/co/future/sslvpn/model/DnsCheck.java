package kr.co.future.sslvpn.model;

import java.util.ArrayList;
import java.util.List;

import kr.co.future.api.CollectionTypeHint;
import kr.co.future.api.FieldOption;
import kr.co.future.confdb.CollectionName;

@CollectionName("dns_checklist")
public class DnsCheck {

	private String domain;

	@CollectionTypeHint(String.class)
	@FieldOption(name = "dns_ip_list")
	private List<String> dnsIpList = new ArrayList<String>();

	public String getDomain() {
		return domain;
	}

	public void setDomain(String domain) {
		this.domain = domain;
	}

	public List<String> getDnsIpList() {
		return dnsIpList;
	}

	public void setDnsIpList(List<String> dnsIpList) {
		this.dnsIpList = dnsIpList;
	}

	@Override
	public String toString() {
		return domain + "=" + dnsIpList;
	}

}
