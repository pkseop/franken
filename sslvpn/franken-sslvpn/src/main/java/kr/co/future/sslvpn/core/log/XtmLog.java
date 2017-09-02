package kr.co.future.sslvpn.core.log;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import kr.co.future.logstorage.Log;

public class XtmLog {
	private Date date;

	// xtm, fw, ddos
	private String type;

	// log generator
	private String originIp;

	private String sourceIp;

	private Integer sourcePort;

	private String natSourceIp;

	private Integer natSourcePort;

	private String destinationIp;

	private Integer destinationPort;

	private String natDestinationIp;

	private Integer natDestinationPort;

	private String protocol;

	private String category;

	private Long logType;

	private int level;

	private String product;

	private String note;

	private String rule;

	private Integer dpiGroupId;

	private Long usage;

	private String user;

	private String iface;

	public Date getDate() {
		return date;
	}

	public void setDate(Date date) {
		this.date = date;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getOriginIp() {
		return originIp;
	}

	public void setOriginIp(String originIp) {
		this.originIp = originIp;
	}

	public String getSourceIp() {
		return sourceIp;
	}

	public void setSourceIp(String sourceIp) {
		this.sourceIp = sourceIp;
	}

	public Integer getSourcePort() {
		return sourcePort;
	}

	public void setSourcePort(Integer sourcePort) {
		this.sourcePort = sourcePort;
	}

	public String getNatSourceIp() {
		return natSourceIp;
	}

	public void setNatSourceIp(String natSourceIp) {
		this.natSourceIp = natSourceIp;
	}

	public Integer getNatSourcePort() {
		return natSourcePort;
	}

	public void setNatSourcePort(Integer natSourcePort) {
		this.natSourcePort = natSourcePort;
	}

	public String getDestinationIp() {
		return destinationIp;
	}

	public void setDestinationIp(String destinationIp) {
		this.destinationIp = destinationIp;
	}

	public Integer getDestinationPort() {
		return destinationPort;
	}

	public void setDestinationPort(Integer destinationPort) {
		this.destinationPort = destinationPort;
	}

	public String getNatDestinationIp() {
		return natDestinationIp;
	}

	public void setNatDestinationIp(String natDestinationIp) {
		this.natDestinationIp = natDestinationIp;
	}

	public Integer getNatDestinationPort() {
		return natDestinationPort;
	}

	public void setNatDestinationPort(Integer natDestinationPort) {
		this.natDestinationPort = natDestinationPort;
	}

	public String getProtocol() {
		return protocol;
	}

	public void setProtocol(String protocol) {
		this.protocol = protocol;
	}

	public String getCategory() {
		return category;
	}

	public void setCategory(String category) {
		this.category = category;
	}

	public Long getLogType() {
		return logType;
	}

	public void setLogType(Long logType) {
		this.logType = logType;
	}

	public int getLevel() {
		return level;
	}

	public void setLevel(int level) {
		this.level = level;
	}

	public String getProduct() {
		return product;
	}

	public void setProduct(String product) {
		this.product = product;
	}

	public String getNote() {
		return note;
	}

	public void setNote(String note) {
		this.note = note;
	}

	public String getRule() {
		return rule;
	}

	public void setRule(String rule) {
		this.rule = rule;
	}

	public Integer getDpiGroupId() {
		return dpiGroupId;
	}

	public void setDpiGroupId(Integer dpiGroupId) {
		this.dpiGroupId = dpiGroupId;
	}

	public Long getUsage() {
		return usage;
	}

	public void setUsage(Long usage) {
		this.usage = usage;
	}

	public String getUser() {
		return user;
	}

	public void setUser(String user) {
		this.user = user;
	}

	public String getIface() {
		return iface;
	}

	public void setIface(String iface) {
		this.iface = iface;
	}

	public Log toLog() {
		Map<String, Object> m = new HashMap<String, Object>();
		m.put("type", type);
		m.put("oip", originIp);
		m.put("sip", sourceIp);
		m.put("dip", destinationIp);
		m.put("sport", sourcePort);
		m.put("dport", destinationPort);
		m.put("nat_sip", natSourceIp);
		m.put("nat_dip", natDestinationIp);
		m.put("nat_sport", natSourcePort);
		m.put("nat_dport", natDestinationPort);
		m.put("proto", protocol);
		m.put("category", category);
		m.put("logtype", logType);
		m.put("level", level);
		m.put("prod", product);
		m.put("note", note);
		m.put("rule", rule);
		m.put("dpi_group", dpiGroupId);
		m.put("usage", usage);
		m.put("user", user);
		m.put("iface", iface);

		return new Log("xtm", date, m);
	}

	@Override
	public String toString() {
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		return String.format("date=%s, category=%s, level=%d, logtype=0x%x, proto=%s, src=%s:%d, dest=%s:%d, note=%s",
				dateFormat.format(date), category, level, logType, protocol, sourceIp, sourcePort, destinationIp,
				destinationPort, note);
	}
}
