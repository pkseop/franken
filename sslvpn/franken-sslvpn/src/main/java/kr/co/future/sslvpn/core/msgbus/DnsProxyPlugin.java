package kr.co.future.sslvpn.core.msgbus;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.Inet4Address;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import kr.co.future.sslvpn.core.msgbus.DnsProxyPlugin;
import kr.co.future.sslvpn.model.DnsRecord;
import kr.co.future.sslvpn.model.DnsZone;
import kr.co.future.sslvpn.model.api.DnsProxyApi;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Requires;

import kr.co.future.api.PrimitiveConverter;
import kr.co.future.codec.Base64;
import kr.co.future.dns.DnsResourceRecord;
import kr.co.future.msgbus.MsgbusException;
import kr.co.future.msgbus.Request;
import kr.co.future.msgbus.Response;
import kr.co.future.msgbus.handler.MsgbusMethod;
import kr.co.future.msgbus.handler.MsgbusPlugin;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import au.com.bytecode.opencsv.CSVReader;

@Component(name = "frodo-dnsproxy-plugin")
@MsgbusPlugin
public class DnsProxyPlugin {
	private final Logger logger = LoggerFactory.getLogger(DnsProxyPlugin.class.getName());

	@Requires
	private DnsProxyApi dnsApi;

	@MsgbusMethod
	public void getDnsZones(Request req, Response resp) {
		try {
			List<DnsZone> dnsList = (List<DnsZone>) dnsApi.getDnsZones();
			logger.debug("frodo core: getDnsList, dnsList [{}]", dnsList.toString());
			resp.put("dns_list", PrimitiveConverter.serialize(dnsList));
		} catch (IllegalStateException e) {
			throw new MsgbusException("frodo", e.getMessage());
		}
	}

	@MsgbusMethod
	public void getDnsZone(Request req, Response resp) {
		String domain = req.getString("domain");
		try {
			DnsZone dns = dnsApi.getDnsZone(domain);
			resp.put("domain", dns);
		} catch (IllegalStateException e) {
			throw new MsgbusException("frodo", e.getMessage());
		}
	}

	@MsgbusMethod
	public void createDnsZone(Request req, Response resp) {
		DnsZone dns = toDnsZone(req);
		try {
			dnsApi.createDnsZone(dns);
		} catch (IllegalStateException e) {
			throw new MsgbusException("frodo", e.getMessage());
		}
	}

	@SuppressWarnings("unchecked")
	private DnsZone toDnsZone(Request req) {
		DnsZone d = new DnsZone();
		d.setDomain(req.getString("domain"));
		List<DnsRecord> dnsRecordList = (List<DnsRecord>) PrimitiveConverter.parseCollection(DnsRecord.class,
				(Collection<Object>) req.get("dns_record_list"));
		d.setDnsRecordList(dnsRecordList);

		return d;
	}

	@MsgbusMethod
	public void updateDnsZone(Request req, Response resp) {
		DnsZone dns = toDnsZone(req);
		try {
			dnsApi.updateDnsZone(dns);
		} catch (IllegalStateException e) {
			throw new MsgbusException("frodo", e.getMessage());
		}

	}

	@MsgbusMethod
	public void removeDnsZones(Request req, Response resp) {
		@SuppressWarnings("unchecked")
		List<String> domains = (List<String>) req.get("domains");
		try {
			dnsApi.removeDnsZones(domains);
		} catch (IllegalStateException e) {
			throw new MsgbusException("frodo", e.getMessage());
		}
	}

	@MsgbusMethod
	public void importDnsZone(Request req, Response resp) {
		logger.info("frodo core: begin csv import dns, thread [{}]", Thread.currentThread().getId());
		Map<String, Object> invalid = null;
		try {
			byte[] decodeBase64 = Base64.decode(req.getString("csv"));

			InputStream is = new ByteArrayInputStream(decodeBase64);
			BufferedReader br = new BufferedReader(new InputStreamReader(is));
			CSVReader reader = new CSVReader(br);

			invalid = importDnsZones(reader);

		} catch (IOException e) {
			throw new MsgbusException("frodo", "invalid csv");
		}

		resp.put("invalid_import_dns", invalid);
	}

	private Map<String, Object> importDnsZones(CSVReader reader) throws IOException {

		String[] nextLine = reader.readNext();

		if (nextLine == null)
			throw new IllegalStateException("header columns not found");

		if (nextLine.length < 2)
			throw new IllegalStateException("not enough columns (should be 2 or more)");

		Set<String> existingDomains = getDnsDomainSet();

		Map<String, Object> invalid = new HashMap<String, Object>();
		while ((nextLine = reader.readNext()) != null) {
			String domain = nextLine[0];
			List<DnsRecord> dnsRecordList = new ArrayList<DnsRecord>();
			String[] ipList = nextLine[1].split(" ");

			if (ipList.length == 0 || (ipList.length == 1 && ipList[0].trim().isEmpty())) {
				invalid.put(domain, "empty-ip");
				continue;
			}

			boolean ipFormat = true;
			for (String ip : ipList) {
				String trimIp = ip.trim();
				if (trimIp.isEmpty())
					continue;

				try {
					trimIp = Inet4Address.getByName(trimIp).getHostAddress();
				} catch (Throwable t) {
					invalid.put(domain, "invalid-ip");
					ipFormat = false;
					break;
				}

				Map<String, Object> m = new HashMap<String, Object>();
				m.put("ip", trimIp);

				DnsRecord record = new DnsRecord();
				record.setType(DnsResourceRecord.Type.A.getCode());
				record.setData(m);

				dnsRecordList.add(record);
				logger.debug("frodo core: importing dns record [{}]", record);
			}

			if (!ipFormat)
				continue;

			DnsZone dns = new DnsZone();
			dns.setDomain(domain);
			dns.setDnsRecordList(dnsRecordList);

			try {
				if (existingDomains.contains(domain))
					dnsApi.updateDnsZone(dns);
				else
					dnsApi.createDnsZone(dns);

			} catch (IllegalStateException e) {
				invalid.put(domain, e);
				continue;
			}
		}

		return invalid;
	}

	private Set<String> getDnsDomainSet() {
		Set<String> domains = new HashSet<String>();
		List<DnsZone> dnsZoneList = (List<DnsZone>) dnsApi.getDnsZones();
		for (DnsZone dz : dnsZoneList) {
			domains.add(dz.getDomain());
		}

		return domains;
	}

	private boolean checkIpFormat(String str) {
		try {
			Inet4Address.getByName(str);
			return true;
		} catch (Throwable t) {
			return false;
		}
	}
}
