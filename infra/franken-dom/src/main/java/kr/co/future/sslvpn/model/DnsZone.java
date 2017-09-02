package kr.co.future.sslvpn.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import kr.co.future.api.CollectionTypeHint;
import kr.co.future.api.FieldOption;
import kr.co.future.api.PrimitiveConverter;
import kr.co.future.confdb.CollectionName;
import kr.co.future.msgbus.Marshalable;

@CollectionName("dns_zone")
public class DnsZone implements Marshalable {
	@FieldOption(name = "domain")
	private String domain;

	@FieldOption(name = "dns_record_list")
	@CollectionTypeHint(DnsRecord.class)
	private List<DnsRecord> dnsRecordList = new ArrayList<DnsRecord>();

	public String getDomain() {
		return domain;
	}

	public void setDomain(String domain) {
		this.domain = domain;
	}

	public List<DnsRecord> getDnsRecordList() {
		return dnsRecordList;
	}

	public void setDnsRecordList(List<DnsRecord> dnsRecordList) {
		this.dnsRecordList = dnsRecordList;
	}

	@Override
	public Map<String, Object> marshal() {
		Map<String, Object> m = new HashMap<String, Object>();
		m.put("domain", domain);
		m.put("dns_record_list", PrimitiveConverter.serialize(dnsRecordList));
		return m;
	}
}