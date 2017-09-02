package kr.co.future.sslvpn.xtmconf.manage;

import java.util.HashMap;
import java.util.Map;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import kr.co.future.sslvpn.xtmconf.AttributeBuilder;
import kr.co.future.sslvpn.xtmconf.MarshalValue;
import kr.co.future.sslvpn.xtmconf.NodeWrapper;
import kr.co.future.sslvpn.xtmconf.Utils;
import kr.co.future.sslvpn.xtmconf.XtmConfig;

public class LogSetting extends XtmConfig {
	public static enum Type {
		Setting, Report/*, FTP*/;

		public static Type get(String str) {
			for (Type t : Type.values()) {
				if (t.toString().equals(str))
					return t;
			}
			return null;
		}

		@Override
		public String toString() {
			return name().toLowerCase();
		}
	}

	public static enum LogLevel {
		NoLog, Debug, Information, Normal, Warning, Serious, Critical
	}

	public static enum FreezeType {
		Stop, Overwrite;

		public static FreezeType get(String str) {
			for (FreezeType f : FreezeType.values()) {
				if (f.toString().equals(str))
					return f;
			}
			return null;
		}

		@Override
		public String toString() {
			return name().toLowerCase();
		}
	}

	public static enum SearchType {
		Local, Outside;

		public static SearchType get(String str) {
			for (SearchType s : SearchType.values()) {
				if (s.toString().equals(str))
					return s;
			}
			return null;
		}

		@Override
		public String toString() {
			return name().toLowerCase();
		}
	}

	public static enum SaveType {
		Byte, Per;

		public static SaveType get(String str) {
			for (SaveType s : SaveType.values()) {
				if (s.toString().equals(str))
					return s;
			}
			return null;
		}

		@Override
		public String toString() {
			return name().toLowerCase();
		}
	}

	public static enum CompressType {
		Weekly, Period, Monthly;

		public static CompressType get(String str) {
			for (CompressType c : CompressType.values()) {
				if (c.toString().equals(str))
					return c;
			}
			return null;
		}

		@Override
		public String toString() {
			return name().toLowerCase();
		}
	}

	public static enum FtpType {
		FTP, SFTP;

		public static FtpType get(String str) {
			for (FtpType f : FtpType.values()) {
				if (f.toString().equals(str))
					return f;
			}
			return null;
		}
	}

	private Type type;
	private LogLevel system; // 로그레벨-System
	private LogLevel network; // 로그레벨-Network
	private LogLevel ipsec; // 로그레벨-IPSec VPN
	private LogLevel nac; // 로그레벨-NAC
	private LogLevel waf; // 로그레벨-WAF
	private LogLevel avas; // 로그레벨-AV/AS
	private LogLevel dpi; // 로그레벨-DPI
	private LogLevel anomaly; // 로그레벨-Traffic Anomaly
	private FreezeType freeze; // 로그기능 불능시 수행설정
	private Integer freezeValue; // 로그기능 불능시 -인 경우 동작
	private boolean optimization; // 로그 최적화 사용안함
	private boolean hddSave; // HDD 로그 저장 사용 안함
	private boolean tcp; // TCP 플래그 로그
	private boolean session; // HDD에 세션 종료로그 생성
	private SearchType search; // 로그검색 서버설정
	private String searchValue; // 로그검색 외부서버 IP
	private boolean rtm; // RTM 사용
	private boolean rtmRpm; // RTM 설정-RPM 정보전송
	private String rtmId; // RTM 설정-ID
	private String rtmPassword; // RTM 설정-Password
	private String rtmServer; // RTM 설정-RTM Server IP
	private boolean save; // HDD용량 알람 사용
	private SaveType saveType; // HDD용량 알람 사용
	private Integer saveValue; // HDD용량 알람 사용
	private boolean compress; // 로그 압축 사용
	private CompressType compressType; // 요일마다/데이터만 남기고/일마다 압축
	private Integer compressSubtype; // 리스트박스
	private boolean compressFtp; // (S)FTP백업 사용
	private boolean compressDel; // (S)FTP백업후 삭제
	private Integer compressBackup; // 로그 압축 설정-시에 로그백업
	private boolean sendPolicy; // 정책전송시 로그남김 사용
	private boolean report; // 일일리포팅 사용
	private int hour; // 일일리포팅 생성 시간 설정
	private boolean trafficSip; // 방화벽-사용자별 Top10
	private boolean trafficDip; // 방화벽-목적지별 Top10
	private boolean trafficService; // 방화벽-서비스별 Top10
	private boolean trafficProtocol; // 방화벽-프로토콜별 Top10
	private boolean dpiSip; // DPI-공격자별 Top10
	private boolean dpiDip; // DPI-공격 목적지별 Top10
	private boolean dpiType; // DPI-공격 유형별 Top10
	private boolean dpiFlow; // DPI-공격 Flow별 Top10
	private boolean urlUser; // URL-URL전체 사용자별 Top10
	private boolean urlDomain; // URL-URL 도메인별 Top10
	private boolean amountInterface; // System-인터페이스별
	private boolean amountSystem; // System-CPU, Disk, Memory, Session
	private boolean amountPolicy; // System-Hit Policy Rule
	private boolean amountLog; // System-Serious Log
	private boolean ftp; // (S)FTP 사용
	private FtpType ftpType; // SFTP/FTP
	private String ftpIp; // 로그 백업 설정-FTP Server IP
	private String ftpId; // 로그 백업 설정-ID
	private String ftpPassword; // 로그 백업 설정-Password
	private String ftpPath; // 로그 백업 설정-저장위치

	@Override
	public String getXmlFilename() {
		return "log_setting.xml";
	}

	@Override
	public String getRootTagName() {
		return "log";
	}

	public static LogSetting parse(NodeWrapper nw) {
		if (Type.get(nw.name()) == null)
			return null;

		LogSetting ls = new LogSetting();
		ls.type = Type.get(nw.name());

		if (ls.type == Type.Setting) {
			for (NodeWrapper c : nw.children()) {
				if (c.isName("level")) {
					for (NodeWrapper l : c.children()) {
						if (l.isName("system"))
							ls.system = LogLevel.valueOf(l.attr("type"));
						else if (l.isName("network"))
							ls.network = LogLevel.valueOf(l.attr("type"));
						else if (l.isName("ipsec"))
							ls.ipsec = LogLevel.valueOf(l.attr("type"));
						else if (l.isName("nac"))
							ls.nac = LogLevel.valueOf(l.attr("type"));
						else if (l.isName("waf"))
							ls.waf = LogLevel.valueOf(l.attr("type"));
						else if (l.isName("avas"))
							ls.avas = LogLevel.valueOf(l.attr("type"));
						else if (l.isName("dpi"))
							ls.dpi = LogLevel.valueOf(l.attr("type"));
						else if (l.isName("anomaly"))
							ls.anomaly = LogLevel.valueOf(l.attr("type"));
					}
				} else if (c.isName("freeze")) {
					ls.freeze = FreezeType.get(c.attr("type"));
					ls.freezeValue = c.intValue();
				} else if (c.isName("optimization"))
					ls.optimization = c.boolAttr("chk_use");
				else if (c.isName("hddsave"))
					ls.hddSave = c.boolAttr("chk_use");
				else if (c.isName("tcp"))
					ls.tcp = c.boolAttr("chk_use");
				else if (c.isName("session"))
					ls.session = c.boolAttr("chk_use");
				else if (c.isName("search")) {
					ls.search = SearchType.get(c.attr("type"));
					ls.searchValue = c.value();
				} else if (c.isName("rtm")) {
					ls.rtm = c.boolAttr("chk_use");
					ls.rtmRpm = c.boolAttr("chk_rpm");
					for (NodeWrapper r : c.children()) {
						if (r.isName("id"))
							ls.rtmId = r.value();
						else if (r.isName("password"))
							ls.rtmPassword = r.value();
						else if (r.isName("server"))
							ls.rtmServer = r.value();
					}
				} else if (c.isName("save")) {
					ls.save = c.boolAttr("chk_use");
					ls.saveType = SaveType.get(c.attr("type"));
					ls.saveValue = c.intAttr("value");
				} else if (c.isName("compress")) {
					ls.compress = c.boolAttr("chk_use");
					ls.compressType = CompressType.get(c.attr("type"));
					ls.compressSubtype = c.intAttr("subtype");
					ls.compressFtp = c.boolAttr("chk_ftp");
					ls.compressDel = c.boolAttr("chk_del");
					ls.compressBackup = c.intAttr("backup_time");
				} else if (c.isName("send_policy"))
					ls.sendPolicy = c.boolAttr("chk_log");
			}
		} else if (ls.type == Type.Report) {
			ls.report = nw.boolAttr("chk_use");
			for (NodeWrapper c : nw.children()) {
				if (c.isName("hour"))
					ls.hour = c.intValue();
				else if (c.isName("traffic")) {
					ls.trafficSip = c.boolAttr("chk_sip");
					ls.trafficDip = c.boolAttr("chk_dip");
					ls.trafficService = c.boolAttr("chk_service");
					ls.trafficProtocol = c.boolAttr("chk_protocol");
				} else if (c.isName("dpi")) {
					ls.dpiSip = c.boolAttr("chk_sip");
					ls.dpiDip = c.boolAttr("chk_dip");
					ls.dpiType = c.boolAttr("chk_type");
					ls.dpiFlow = c.boolAttr("chk_flow");
				} else if (c.isName("url")) {
					ls.urlUser = c.boolAttr("chk_user");
					ls.urlDomain = c.boolAttr("chk_domain");
				} else if (c.isName("amount")) {
					ls.amountInterface = c.boolAttr("chk_interface");
					ls.amountSystem = c.boolAttr("chk_system");
					ls.amountPolicy = c.boolAttr("chk_policy");
					ls.amountLog = c.boolAttr("chk_log");
				}
			}
		} 
//		else if (ls.type == Type.FTP) {
//			ls.ftp = nw.boolAttr("chk_use");
//
//			String t = nw.attr("chk_type");
//			if (t != null)
//				ls.ftpType = FtpType.valueOf(t);
//
//			for (NodeWrapper c : nw.children()) {
//				if (c.isName("ip"))
//					ls.ftpIp = c.value();
//				else if (c.isName("id"))
//					ls.ftpId = c.value();
//				else if (c.isName("password"))
//					ls.ftpPassword = c.value();
//				else if (c.isName("path"))
//					ls.ftpPath = c.value();
//			}
//		}

		return ls;
	}

	public Type getType() {
		return type;
	}

	public void setType(Type type) {
		this.type = type;
	}

	public LogLevel getSystem() {
		return system;
	}

	public void setSystem(LogLevel system) {
		this.system = system;
	}

	public LogLevel getNetwork() {
		return network;
	}

	public void setNetwork(LogLevel network) {
		this.network = network;
	}

	public LogLevel getIpsec() {
		return ipsec;
	}

	public void setIpsec(LogLevel ipsec) {
		this.ipsec = ipsec;
	}

	public LogLevel getNac() {
		return nac;
	}

	public void setNac(LogLevel nac) {
		this.nac = nac;
	}

	public LogLevel getWaf() {
		return waf;
	}

	public void setWaf(LogLevel waf) {
		this.waf = waf;
	}

	public LogLevel getAvas() {
		return avas;
	}

	public void setAvas(LogLevel avas) {
		this.avas = avas;
	}

	public LogLevel getDpi() {
		return dpi;
	}

	public void setDpi(LogLevel dpi) {
		this.dpi = dpi;
	}

	public LogLevel getAnomaly() {
		return anomaly;
	}

	public void setAnomaly(LogLevel anomaly) {
		this.anomaly = anomaly;
	}

	public FreezeType getFreeze() {
		return freeze;
	}

	public void setFreeze(FreezeType freeze) {
		this.freeze = freeze;
	}

	public Integer getFreezeValue() {
		return freezeValue;
	}

	public void setFreezeValue(Integer freezeValue) {
		this.freezeValue = freezeValue;
	}

	public boolean isOptimization() {
		return optimization;
	}

	public void setOptimization(boolean optimization) {
		this.optimization = optimization;
	}

	public boolean isHddSave() {
		return hddSave;
	}

	public void setHddSave(boolean hddSave) {
		this.hddSave = hddSave;
	}

	public boolean isTcp() {
		return tcp;
	}

	public void setTcp(boolean tcp) {
		this.tcp = tcp;
	}

	public boolean isSession() {
		return session;
	}

	public void setSession(boolean session) {
		this.session = session;
	}

	public SearchType getSearch() {
		return search;
	}

	public void setSearch(SearchType search) {
		this.search = search;
	}

	public String getSearchValue() {
		return searchValue;
	}

	public void setSearchValue(String searchValue) {
		this.searchValue = searchValue;
	}

	public boolean isRtm() {
		return rtm;
	}

	public void setRtm(boolean rtm) {
		this.rtm = rtm;
	}

	public boolean isRtmRpm() {
		return rtmRpm;
	}

	public void setRtmRpm(boolean rtmRpm) {
		this.rtmRpm = rtmRpm;
	}

	public String getRtmId() {
		return rtmId;
	}

	public void setRtmId(String rtmId) {
		this.rtmId = rtmId;
	}

	public String getRtmPassword() {
		return rtmPassword;
	}

	public void setRtmPassword(String rtmPassword) {
		this.rtmPassword = rtmPassword;
	}

	public String getRtmServer() {
		return rtmServer;
	}

	public void setRtmServer(String rtmServer) {
		this.rtmServer = rtmServer;
	}

	public boolean isSave() {
		return save;
	}

	public void setSave(boolean save) {
		this.save = save;
	}

	public SaveType getSaveType() {
		return saveType;
	}

	public void setSaveType(SaveType saveType) {
		this.saveType = saveType;
	}

	public Integer getSaveValue() {
		return saveValue;
	}

	public void setSaveValue(Integer saveValue) {
		this.saveValue = saveValue;
	}

	public boolean isCompress() {
		return compress;
	}

	public void setCompress(boolean compress) {
		this.compress = compress;
	}

	public CompressType getCompressType() {
		return compressType;
	}

	public void setCompressType(CompressType compressType) {
		this.compressType = compressType;
	}

	public Integer getCompressSubtype() {
		return compressSubtype;
	}

	public void setCompressSubtype(Integer compressSubtype) {
		this.compressSubtype = compressSubtype;
	}

	public boolean isCompressFtp() {
		return compressFtp;
	}

	public void setCompressFtp(boolean compressFtp) {
		this.compressFtp = compressFtp;
	}

	public boolean isCompressDel() {
		return compressDel;
	}

	public void setCompressDel(boolean compressDel) {
		this.compressDel = compressDel;
	}

	public Integer getCompressBackup() {
		return compressBackup;
	}

	public void setCompressBackup(Integer compressBackup) {
		this.compressBackup = compressBackup;
	}

	public boolean isSendPolicy() {
		return sendPolicy;
	}

	public void setSendPolicy(boolean sendPolicy) {
		this.sendPolicy = sendPolicy;
	}

	public boolean isReport() {
		return report;
	}

	public void setReport(boolean report) {
		this.report = report;
	}

	public int getHour() {
		return hour;
	}

	public void setHour(int hour) {
		this.hour = hour;
	}

	public boolean isTrafficSip() {
		return trafficSip;
	}

	public void setTrafficSip(boolean trafficSip) {
		this.trafficSip = trafficSip;
	}

	public boolean isTrafficDip() {
		return trafficDip;
	}

	public void setTrafficDip(boolean trafficDip) {
		this.trafficDip = trafficDip;
	}

	public boolean isTrafficService() {
		return trafficService;
	}

	public void setTrafficService(boolean trafficService) {
		this.trafficService = trafficService;
	}

	public boolean isTrafficProtocol() {
		return trafficProtocol;
	}

	public void setTrafficProtocol(boolean trafficProtocol) {
		this.trafficProtocol = trafficProtocol;
	}

	public boolean isDpiSip() {
		return dpiSip;
	}

	public void setDpiSip(boolean dpiSip) {
		this.dpiSip = dpiSip;
	}

	public boolean isDpiDip() {
		return dpiDip;
	}

	public void setDpiDip(boolean dpiDip) {
		this.dpiDip = dpiDip;
	}

	public boolean isDpiType() {
		return dpiType;
	}

	public void setDpiType(boolean dpiType) {
		this.dpiType = dpiType;
	}

	public boolean isDpiFlow() {
		return dpiFlow;
	}

	public void setDpiFlow(boolean dpiFlow) {
		this.dpiFlow = dpiFlow;
	}

	public boolean isUrlUser() {
		return urlUser;
	}

	public void setUrlUser(boolean urlUser) {
		this.urlUser = urlUser;
	}

	public boolean isUrlDomain() {
		return urlDomain;
	}

	public void setUrlDomain(boolean urlDomain) {
		this.urlDomain = urlDomain;
	}

	public boolean isAmountInterface() {
		return amountInterface;
	}

	public void setAmountInterface(boolean amountInterface) {
		this.amountInterface = amountInterface;
	}

	public boolean isAmountSystem() {
		return amountSystem;
	}

	public void setAmountSystem(boolean amountSystem) {
		this.amountSystem = amountSystem;
	}

	public boolean isAmountPolicy() {
		return amountPolicy;
	}

	public void setAmountPolicy(boolean amountPolicy) {
		this.amountPolicy = amountPolicy;
	}

	public boolean isAmountLog() {
		return amountLog;
	}

	public void setAmountLog(boolean amountLog) {
		this.amountLog = amountLog;
	}

	public boolean isFtp() {
		return ftp;
	}

	public void setFtp(boolean ftp) {
		this.ftp = ftp;
	}

	public FtpType getFtpType() {
		return ftpType;
	}

	public void setFtpType(FtpType ftpType) {
		this.ftpType = ftpType;
	}

	public String getFtpIp() {
		return ftpIp;
	}

	public void setFtpIp(String ftpIp) {
		this.ftpIp = ftpIp;
	}

	public String getFtpId() {
		return ftpId;
	}

	public void setFtpId(String ftpId) {
		this.ftpId = ftpId;
	}

	public String getFtpPassword() {
		return ftpPassword;
	}

	public void setFtpPassword(String ftpPassword) {
		this.ftpPassword = ftpPassword;
	}

	public String getFtpPath() {
		return ftpPath;
	}

	public void setFtpPath(String ftpPath) {
		this.ftpPath = ftpPath;
	}

	@Override
	protected Element convertToElement(Document doc) {
		Element e = doc.createElement(type.toString());

		if (type == Type.Setting) {
			Element l = appendChild(doc, e, "level", null);
			appendChild(doc, l, "system", null, new AttributeBuilder("type", system));
			appendChild(doc, l, "network", null, new AttributeBuilder("type", network));
			appendChild(doc, l, "ipsec", null, new AttributeBuilder("type", ipsec));
			appendChild(doc, l, "nac", null, new AttributeBuilder("type", nac));
			appendChild(doc, l, "waf", null, new AttributeBuilder("type", waf));
			appendChild(doc, l, "avas", null, new AttributeBuilder("type", avas));
			appendChild(doc, l, "dpi", null, new AttributeBuilder("type", dpi));
			appendChild(doc, l, "anomaly", null, new AttributeBuilder("type", anomaly));
			appendChild(doc, e, "freeze", freezeValue, new AttributeBuilder("type", freeze));
			appendChild(doc, e, "optimization", null, new AttributeBuilder("chk_use", optimization));
			appendChild(doc, e, "hddsave", null, new AttributeBuilder("chk_use", hddSave));
			appendChild(doc, e, "tcp", null, new AttributeBuilder("chk_use", tcp));
			appendChild(doc, e, "session", null, new AttributeBuilder("chk_use", session));
			appendChild(doc, e, "search", searchValue, new AttributeBuilder("type", search));
			Element r = appendChild(doc, e, "rtm", null, new AttributeBuilder("chk_use", rtm).put("chk_rpm", rtmRpm));
			appendChild(doc, r, "id", rtmId);
			appendChild(doc, r, "password", rtmPassword);
			appendChild(doc, r, "server", rtmServer);
			appendChild(doc, e, "save", null,
					new AttributeBuilder("chk_use", save).put("type", saveType).put("value", saveValue));
			AttributeBuilder compressAttr = new AttributeBuilder("chk_use", compress).put("type", compressType)
					.put("subtype", compressSubtype).put("chk_ftp", compressFtp).put("chk_del", compressDel)
					.put("backup_time", compressBackup);
			appendChild(doc, e, "compress", null, compressAttr);
			appendChild(doc, e, "send_policy", null, new AttributeBuilder("chk_log", sendPolicy));
		} else if (type == Type.Report) {
			e.setAttribute("chk_use", Utils.bool(report));
			appendChild(doc, e, "hour", hour);
			AttributeBuilder trafficAttr = new AttributeBuilder("chk_sip", trafficSip).put("chk_dip", trafficDip)
					.put("chk_service", trafficService).put("chk_protocol", trafficProtocol);
			appendChild(doc, e, "traffic", null, trafficAttr);
			AttributeBuilder dpiAttr = new AttributeBuilder("chk_sip", dpiSip).put("chk_dip", dpiDip)
					.put("chk_type", dpiType).put("chk_flow", dpiFlow);
			appendChild(doc, e, "dpi", null, dpiAttr);
			appendChild(doc, e, "url", null, new AttributeBuilder("chk_user", urlUser).put("chk_domain", urlDomain));
			AttributeBuilder amountAttr = new AttributeBuilder("chk_interface", amountInterface)
					.put("chk_system", amountSystem).put("chk_policy", amountPolicy).put("chk_log", amountLog);
			appendChild(doc, e, "amount", null, amountAttr);
		}
//		else if (type == Type.FTP) {
//			e.setAttribute("chk_use", Utils.bool(ftp));
//			if (ftpType != null)
//				e.setAttribute("chk_type", ftpType.toString());
//			appendChild(doc, e, "ip", ftpIp);
//			appendChild(doc, e, "id", ftpId);
//			appendChild(doc, e, "password", ftpPassword);
//			appendChild(doc, e, "path", ftpPath);
//		}

		return e;
	}

	@Override
	public Map<String, Object> marshal() {
		Map<String, Object> m = new HashMap<String, Object>();
		m.put("type", type);

		if (type == Type.Setting) {
			m.put("level",
					new MarshalValue("system", system).put("network", network).put("ipsec", ipsec).put("nac", nac)
							.put("waf", waf).put("avas", avas).put("dpi", dpi).put("anomaly", anomaly).get());
			m.put("freeze", new MarshalValue("type", freeze).put("value", freezeValue).get());
			m.put("optimization", optimization);
			m.put("hdd_save", hddSave);
			m.put("tcp", tcp);
			m.put("session", session);
			m.put("search", new MarshalValue("type", search).put("value", searchValue).get());
			m.put("rtm", new MarshalValue("use", rtm).put("rpm", rtmRpm).put("id", rtmId).put("password", rtmPassword)
					.put("server", rtmServer).get());
			m.put("save", new MarshalValue("use", save).put("type", saveType).put("value", saveValue).get());
			m.put("compress",
					new MarshalValue("use", compress).put("type", compressType).put("subtype", compressSubtype)
							.put("ftp", compressFtp).put("del", compressDel).put("backup", compressBackup).get());
		} else if (type == Type.Report) {
			m.put("use", report);
			m.put("hour", hour);
			m.put("traffic", new MarshalValue("sip", trafficSip).put("dip", trafficDip).put("service", trafficService)
					.put("protocol", trafficProtocol).get());
			m.put("dpi", new MarshalValue("sip", dpiSip).put("dip", dpiDip).put("type", dpiType).put("flow", dpiFlow)
					.get());
			m.put("url", new MarshalValue("user", urlUser).put("domain", urlDomain).get());
			m.put("amount",
					new MarshalValue("interface", amountInterface).put("system", amountSystem)
							.put("policy", amountPolicy).put("log", amountLog).get());
		} 
//		else if (type == Type.FTP) {
//			m.put("use", ftp);
//			m.put("ftp_type", ftpType);
//			m.put("ip", ftpIp);
//			m.put("id", ftpId);
//			m.put("password", ftpPassword);
//			m.put("path", ftpPath);
//		}

		return m;
	}

	@Override
	public String toString() {
		return "LogSetting [type=" + type + ", system=" + system + ", network=" + network + ", ipsec=" + ipsec
				+ ", nac=" + nac + ", waf=" + waf + ", avas=" + avas + ", dpi=" + dpi + ", anomaly=" + anomaly
				+ ", freeze=" + freeze + ", freezeValue=" + freezeValue + ", optimization=" + optimization
				+ ", hddSave=" + hddSave + ", tcp=" + tcp + ", session=" + session + ", search=" + search
				+ ", searchValue=" + searchValue + ", rtm=" + rtm + ", rtmRpm=" + rtmRpm + ", rtmId=" + rtmId
				+ ", rtmPassword=" + rtmPassword + ", rtmServer=" + rtmServer + ", save=" + save + ", saveType="
				+ saveType + ", saveValue=" + saveValue + ", compress=" + compress + ", compressType=" + compressType
				+ ", compressSubtype=" + compressSubtype + ", compressFtp=" + compressFtp + ", compressDel="
				+ compressDel + ", compressBackup=" + compressBackup + ", sendPolicy=" + sendPolicy + ", report="
				+ report + ", hour=" + hour + ", trafficSip=" + trafficSip + ", trafficDip=" + trafficDip
				+ ", trafficService=" + trafficService + ", trafficProtocol=" + trafficProtocol + ", dpiSip=" + dpiSip
				+ ", dpiDip=" + dpiDip + ", dpiType=" + dpiType + ", dpiFlow=" + dpiFlow + ", urlUser=" + urlUser
				+ ", urlDomain=" + urlDomain + ", amountInterface=" + amountInterface + ", amountSystem="
				+ amountSystem + ", amountPolicy=" + amountPolicy + ", amountLog=" + amountLog + ", ftp=" + ftp
				+ ", ftpType=" + ftpType + ", ftpIp=" + ftpIp + ", ftpId=" + ftpId + ", ftpPassword=" + ftpPassword
				+ ", ftpPath=" + ftpPath + "]";
	}
}
