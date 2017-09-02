package kr.co.future.sslvpn.core.msgbus;

import java.util.List;

import kr.co.future.sslvpn.core.DnsCheckService;
import kr.co.future.sslvpn.core.msgbus.DnsCheckPlugin;
import kr.co.future.sslvpn.model.DnsCheck;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Requires;

import kr.co.future.api.PrimitiveConverter;
import kr.co.future.msgbus.MsgbusException;
import kr.co.future.msgbus.Request;
import kr.co.future.msgbus.Response;
import kr.co.future.msgbus.handler.MsgbusMethod;
import kr.co.future.msgbus.handler.MsgbusPlugin;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(name = "frodo-dns-check-plugin")
@MsgbusPlugin
public class DnsCheckPlugin {
	private final Logger logger = LoggerFactory.getLogger(DnsCheckPlugin.class.getName());

	@Requires
	private DnsCheckService dnsCheckApi;

	@MsgbusMethod
	public void getDnsCheckList(Request req, Response resp) {
		try {
			List<DnsCheck> dnsCheckList = (List<DnsCheck>) dnsCheckApi.getDnsCheckList();
			logger.debug("frodo core: getDnsCheckList, dnsCheck [{}]", dnsCheckList.toString());
			resp.put("dns_check_list", PrimitiveConverter.serialize(dnsCheckList));
		} catch (IllegalStateException e) {
			throw new MsgbusException("frodo", e.getMessage());
		}
	}

	@MsgbusMethod
	public void getDnsCheck(Request req, Response resp) {
		String domain = req.getString("domain");
		try {
			DnsCheck dnsCheck = dnsCheckApi.getDnsCheck(domain);
			logger.debug("frodo core: getDnsCheck, dnsCheck [{}]", dnsCheck.toString());
			resp.put("dns_check", dnsCheck);
		} catch (IllegalStateException e) {
			throw new MsgbusException("frodo", e.getMessage());
		}
	}

	@MsgbusMethod
	public void createDnsCheck(Request req, Response resp) {
		DnsCheck dnsCheck = (DnsCheck) PrimitiveConverter.parse(DnsCheck.class, req.getParams());
		try {
			logger.debug("frodo core: createDnsCheck, parse json to dnsCheck [{}]", dnsCheck.toString());
			dnsCheckApi.createDnsCheck(dnsCheck);
		} catch (IllegalStateException e) {
			throw new MsgbusException("frodo", e.getMessage());
		}
	}

	@MsgbusMethod
	public void udpateDnsCheck(Request req, Response resp) {
		DnsCheck dnsCheck = (DnsCheck) PrimitiveConverter.parse(DnsCheck.class, req.getParams());
		try {
			logger.debug("frodo core: updateDnsCheck, parse json to dnsCheck [{}]", dnsCheck.toString());
			dnsCheckApi.updateDnsCheck(dnsCheck);
		} catch (IllegalStateException e) {
			throw new MsgbusException("frodo", e.getMessage());
		}
	}

	@MsgbusMethod
	public void removeDnsChecks(Request req, Response resp) {
		@SuppressWarnings("unchecked")
		List<String> domains = (List<String>) req.get("domains");
		try {
			dnsCheckApi.removeDnsChecks(domains);
		} catch (IllegalStateException e) {
			throw new MsgbusException("frodo", e.getMessage());
		}

	}
}
