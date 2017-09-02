package kr.co.future.sslvpn.model.api.impl;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import kr.co.future.sslvpn.model.DnsRecord;
import kr.co.future.sslvpn.model.DnsZone;
import kr.co.future.sslvpn.model.api.DnsProxyApi;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Invalidate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.apache.felix.ipojo.annotations.Validate;

import kr.co.future.confdb.BaseConfigDatabaseListener;
import kr.co.future.confdb.BaseConfigServiceListener;
import kr.co.future.confdb.Config;
import kr.co.future.confdb.ConfigDatabase;
import kr.co.future.confdb.ConfigIterator;
import kr.co.future.confdb.ConfigService;
import kr.co.future.confdb.ConfigTransaction;
import kr.co.future.confdb.Predicates;
import kr.co.future.confdb.RollbackException;
import kr.co.future.dns.DnsResourceRecord;
import kr.co.future.dom.api.DefaultEntityEventProvider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(name = "frodo-dnsproxy-api")
@Provides
public class DnsProxyApiImpl extends DefaultEntityEventProvider<DnsZone> implements DnsProxyApi {
	private final Logger logger = LoggerFactory.getLogger(DnsProxyApiImpl.class.getName());

	// cache
	private Map<String, DnsZone> dnsMap = new ConcurrentHashMap<String, DnsZone>();

	@Requires
	private ConfigService conf;

	private ConfDbListener dbListener;
	private ConfServiceListener serviceListener;

	public DnsProxyApiImpl() {
		serviceListener = new ConfServiceListener();
		dbListener = new ConfDbListener();
	}

	@Validate
	public void start() {
		ConfigDatabase db = conf.ensureDatabase("frodo");
		ConfigIterator it = db.findAll(DnsZone.class);

		conf.addListener(serviceListener);
		db.addListener(dbListener);

		// load dnsList on cache
		List<DnsZone> dnsList = (List<DnsZone>) it.getDocuments(DnsZone.class);
		for (DnsZone dnsZone : dnsList)
			dnsMap.put(dnsZone.getDomain(), dnsZone);

	}

	@Invalidate
	public void stop() {
		if (conf != null) {
			conf.removeListener(serviceListener);
			ConfigDatabase db = conf.ensureDatabase("frodo");
			db.removeListener(dbListener);
		}
	}

	@Override
	public List<DnsZone> getDnsZones() {
		return new ArrayList<DnsZone>(dnsMap.values());
	}

	@Override
	public DnsZone getDnsZone(String domain) {
		return dnsMap.get(domain);
	}

	@Override
	public void createDnsZone(DnsZone dns) {
		ConfigDatabase db = conf.ensureDatabase("frodo");
		Config c = db.findOne(DnsZone.class, Predicates.field("domain", dns.getDomain()));

		if (c != null)
			throw new IllegalStateException("duplicated dns zone: " + dns.getDomain());

		db.add(dns);
		dnsMap.put(dns.getDomain(), dns);
		fireEntityAdded("localhost", dns);
	}

	@Override
	public void updateDnsZone(DnsZone dns) {
		ConfigDatabase db = conf.ensureDatabase("frodo");
		Config c = db.findOne(DnsZone.class, Predicates.field("domain", dns.getDomain()));

		if (c == null)
			throw new IllegalStateException("dns zone not found: " + dns.getDomain());

		DnsZone old = c.getDocument(DnsZone.class);
		dns.setDomain(old.getDomain());
		db.update(c, dns, true);
		dnsMap.put(dns.getDomain(), dns);
		fireEntityUpdated("localhost", dns);
	}

	@Override
	public void removeDnsZones(List<String> domains) {
		if (domains == null || domains.isEmpty())
			return;

		ConfigDatabase db = conf.ensureDatabase("frodo");
		ConfigIterator it = db.find(DnsZone.class, Predicates.in("domain", domains));

		if (!it.hasNext()) {
			it.close();
			return;
		}

		ConfigTransaction xact = db.beginTransaction();
		List<DnsZone> dnsZones = new ArrayList<DnsZone>();

		try {
			while (it.hasNext()) {
				Config c = it.next();
				DnsZone dnsZone = c.getDocument(DnsZone.class);
				dnsZones.add(dnsZone);
				db.remove(xact, c, true);
			}
			xact.commit("frodo-model", "remove dns zone" + domains.toString());
		} catch (Throwable t) {
			xact.rollback();
			throw new RollbackException(t);
		} finally {
			if (it != null)
				it.close();
		}

		if (logger.isDebugEnabled())
			logger.debug("frodo model: remove dns zone [{}]", dnsZones.toString());

		for (DnsZone dz : dnsZones) {
			fireEntityRemoved("localhost", dz);
			dnsMap.remove(dz.getDomain());
		}
	}

	private class ConfDbListener extends BaseConfigDatabaseListener {
		@Override
		public void onImport(ConfigDatabase db) {
			if (!db.getName().equals("frodo"))
				return;
			logger.debug("frodo model: receive import event, initialize cache");
			dnsMap.clear();
			ConfigIterator it = db.findAll(DnsZone.class);
			// load dnsList on cache
			List<DnsZone> dnsList = (List<DnsZone>) it.getDocuments(DnsZone.class);
			for (DnsZone dnsZone : dnsList)
				dnsMap.put(dnsZone.getDomain(), dnsZone);
		}
	}

	private class ConfServiceListener extends BaseConfigServiceListener {
		@Override
		public void onCreateDatabase(ConfigDatabase db) {
			if (!db.getName().equals("frodo"))
				return;
			db.addListener(dbListener);
		}
	}

	@Override
	public List<InetAddress> getDnsIpList(String domain) {
		List<DnsZone> dnsList = getDnsZones();
		for (DnsZone dnsZone : dnsList) {
			if (!dnsZone.getDomain().equals(domain))
				continue;

			List<InetAddress> dnsIpList = new ArrayList<InetAddress>();
			for (DnsRecord dnsRecord : dnsZone.getDnsRecordList()) {
				if (dnsRecord.getType() == DnsResourceRecord.Type.A.getCode()) {
					String dnsIp = (String) dnsRecord.getData().get("ip");
					try {
						dnsIpList.add(InetAddress.getByName(dnsIp));
					} catch (UnknownHostException e) {
						logger.error("frodo core: cannot load dnsproxy domain [{}] ip [{}]", domain, dnsIp);
					}
				}
			}

			return dnsIpList;
		}
		return null;
	}
}
