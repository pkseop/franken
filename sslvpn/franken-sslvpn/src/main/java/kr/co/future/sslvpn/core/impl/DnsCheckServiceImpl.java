package kr.co.future.sslvpn.core.impl;

import java.util.Collection;
import java.util.List;

import kr.co.future.sslvpn.core.DnsCheckService;
import kr.co.future.sslvpn.model.DnsCheck;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import kr.co.future.confdb.Config;
import kr.co.future.confdb.ConfigDatabase;
import kr.co.future.confdb.ConfigIterator;
import kr.co.future.confdb.ConfigService;
import kr.co.future.confdb.Predicates;

@Component(name = "frodo-dns-check-api")
@Provides
public class DnsCheckServiceImpl implements DnsCheckService {
	@Requires
	private ConfigService conf;

	@Override
	public Collection<DnsCheck> getDnsCheckList() {
		ConfigDatabase db = conf.ensureDatabase("frodo");
		ConfigIterator it = db.findAll(DnsCheck.class);
		return it.getDocuments(DnsCheck.class);
	}

	@Override
	public DnsCheck getDnsCheck(String domain) {
		ConfigDatabase db = conf.ensureDatabase("frodo");
		Config c = db.findOne(DnsCheck.class, Predicates.field("domain", domain));
		if (c == null)
			return null;

		return c.getDocument(DnsCheck.class);
	}

	@Override
	public void createDnsCheck(DnsCheck dnsCheck) {

		if (dnsCheck.getDomain() == null || dnsCheck.getDomain().trim().isEmpty())
			throw new IllegalArgumentException("domain is null");

		if (dnsCheck.getDnsIpList() == null || dnsCheck.getDnsIpList().isEmpty())
			throw new IllegalArgumentException("dns ip list is null");

		ConfigDatabase db = conf.ensureDatabase("frodo");
		Config c = db.findOne(DnsCheck.class, Predicates.field("domain", dnsCheck.getDomain()));
		if (c != null)
			throw new IllegalStateException("duplicated dnsCheck: " + dnsCheck.getDomain());

		db.add(dnsCheck);
	}

	@Override
	public void updateDnsCheck(DnsCheck dnsCheck) {

		if (dnsCheck.getDomain() == null || dnsCheck.getDomain().trim().isEmpty())
			throw new IllegalArgumentException("domain is null");

		if (dnsCheck.getDnsIpList() == null || dnsCheck.getDnsIpList().isEmpty())
			throw new IllegalArgumentException("dns ip list is null");

		ConfigDatabase db = conf.ensureDatabase("frodo");
		Config c = db.findOne(DnsCheck.class, Predicates.field("domain", dnsCheck.getDomain()));
		if (c == null)
			throw new IllegalStateException("dnsCheck not found: " + dnsCheck.getDomain());

		db.update(c, dnsCheck);
	}

	@Override
	public void removeDnsChecks(List<String> domains) {
		if (domains == null || domains.isEmpty())
			return;

		ConfigDatabase db = conf.ensureDatabase("frodo");
		ConfigIterator it = db.find(DnsCheck.class, Predicates.in("domain", domains));
		try {
			while (it.hasNext()) {
				Config c = it.next();
				db.remove(c);
			}
		} finally {
			it.close();
		}
	}

}
