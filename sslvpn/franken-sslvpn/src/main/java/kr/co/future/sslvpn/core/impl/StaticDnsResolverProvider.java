package kr.co.future.sslvpn.core.impl;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.util.Date;
import java.util.List;

import kr.co.future.sslvpn.core.GlobalConfig;
import kr.co.future.sslvpn.core.GlobalConfigApi;
import kr.co.future.sslvpn.core.log.DnsLog;
import kr.co.future.sslvpn.model.api.DnsProxyApi;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Invalidate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.apache.felix.ipojo.annotations.Validate;

import kr.co.future.dns.DnsEventListener;
import kr.co.future.dns.DnsFlags;
import kr.co.future.dns.DnsMessage;
import kr.co.future.dns.DnsResolver;
import kr.co.future.dns.DnsResolverProvider;
import kr.co.future.dns.DnsResourceRecord;
import kr.co.future.dns.DnsService;
import kr.co.future.dns.rr.A;
import kr.co.future.logstorage.Log;
import kr.co.future.logstorage.LogStorage;
import kr.co.future.logstorage.LogStorageStatus;
import kr.co.future.sslvpn.core.impl.StaticDnsResolverProvider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(name = "frodo-dnsproxy-resolver")
@Provides
public class StaticDnsResolverProvider implements DnsResolverProvider, DnsEventListener {
	private final Logger logger = LoggerFactory.getLogger(StaticDnsResolverProvider.class);

	@Requires
	private DnsService dns;

	@Requires
	private DnsProxyApi dnsApi;

	@Requires
	private LogStorage logStorage;

	@Requires
	private GlobalConfigApi configApi;
	
	@Validate
	public void start() {
		dns.registerProvider(this);
		dns.addListener(this);
		GlobalConfig config = configApi.getGlobalConfig();
		if (config.getDefaultDnsResolverProvider() != null){
			for (DnsResolverProvider provider : dns.getResolverProviders()) {
				if (provider.getName() == config.getDefaultDnsResolverProvider()){
					dns.setDefaultResolverProvider(config.getDefaultDnsResolverProvider());
				}
			} 
		} else 
			dns.setDefaultResolverProvider("frodo-proxy");
		
		if (config.getEnableDnsResolver() != null && config.getEnableDnsResolver()) {
			try {
				dns.open();
				logger.info("dns service opened.");
			} catch (IOException e) {
				logger.info("dns server is already listening.");
			}
		}
	}

	@Invalidate
	public void stop() {
		if (dns != null) {
			dns.unregisterProvider(this);
			dns.removeListener(this);
			dns.setDefaultResolverProvider("proxy");
		}
	}

	@Override
	public String getName() {
		return "frodo-proxy";
	}

	@Override
	public DnsResolver newResolver() {
		return new ProxyResolver();
	}

	private class ProxyResolver implements DnsResolver {
		@Override
		public DnsMessage resolve(DnsMessage query) throws IOException {

			DnsResourceRecord qr = query.getQuestions().get(0);
			if (qr.getType() != DnsResourceRecord.Type.A.getCode())
				return null;
			
			String domain = qr.getName();
			List<InetAddress> ipList = dnsApi.getDnsIpList(domain);
			if (ipList == null)
				return null;
			
			DnsMessage reply = new DnsMessage();
			reply.setId(query.getId());
			DnsFlags flags = new DnsFlags();
			flags.setQuery(false);
			reply.setFlags(flags);
			reply.setQuestionCount(1);
			reply.setAnswerCount(ipList.size());
			reply.addQuestion(qr);

			for (InetAddress ip : ipList) {
				reply.addAnswer(new A(domain, ip, 0));
			}
			
			return reply;			
		}
	}

	@Override
	public void onReceive(DatagramPacket queryPacket, DnsMessage query) {
	}

	@Override
	public void onSend(DatagramPacket queryPacket, DnsMessage query, DatagramPacket responsePacket, DnsMessage response) {
	}

	@Override
	public void onError(DatagramPacket packet, Throwable t) {
		String hexDump = toHexDump(packet.getData(), packet.getOffset(), packet.getLength());
		String client = packet.getAddress().getHostAddress();
		writeLog(client, "error", null, hexDump);
	}

	@Override
	public void onDrop(DatagramPacket queryPacket, DnsMessage query, Throwable t) {
		String domain = query.getQuestions().get(0).getName();
		String client = queryPacket.getAddress().getHostAddress();
		writeLog(client, "drop", domain, query.toString());

	}

	private void writeLog(String clientIp, String type, String domain, String msg) {
		DnsLog log = new DnsLog();
		log.setClientIp(clientIp);
		log.setType(type);
		log.setDomain(domain);
		log.setMsg(msg);
		log.setDate(new Date());
		Log l = log.toLog();

		if (logStorage.getStatus() == LogStorageStatus.Open)
			logStorage.write(l);
	}

	private String toHexDump(byte[] buf, int offset, int length) {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < length; i++) {
			if (i != 0)
				sb.append(" ");
			sb.append(String.format("%02X", buf[i + offset]));
		}

		return sb.toString();
	}
}
