package kr.co.future.sslvpn.xtmconf.system;

import java.util.HashMap;
import java.util.Map;

import kr.co.future.sslvpn.xtmconf.AttributeBuilder;
import kr.co.future.sslvpn.xtmconf.MarshalValue;
import kr.co.future.sslvpn.xtmconf.NodeWrapper;
import kr.co.future.sslvpn.xtmconf.XtmConfig;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class Basic extends XtmConfig {
	public static enum TimeZone {
		GMT_1100(-660, "(GMT -11:00) Niue, Samoa, West"), GMT_1000(-600, "(GMT -10:00) Hawaii, Tahiti, Tokelau"), GMT_0930(-570,
				"(GMT -09:30) Marquesas"), GMT_0900(-540, "(GMT -09:00) Alaska Standard, Gambier"), GMT_0800(-480,
				"(GMT -08:00) Pacific Standard, Pacific Standard"), GMT_0700(-420, "(GMT -07:00) Mountain Standard"), GMT_0600(-360,
				"(GMT -06:00) Central Standard, Easter Is., Galapagos"), GMT_0500(-300,
				"(GMT -05:00) Acre, Central Standard, Colombia, Eastern Standard, Ecuador, Peru"), GMT_0400(-240,
				"(GMT -04:00) Amazon, Atlantic, Bolivia, Chile, Falkland Is., Guyana, Paraguay, Venezuela"), GMT_0330(-210,
				"(GMT -03:30) Newfoundland Standard"), GMT_0300(-180,
				"(GMT -03:00) Argentine, Brazil, French Guiana, Pierre and Miquelon, Suriname, Uruguay, Western Greenland"), GMT_0200(-120,
				"(GMT -02:00) Fernando de Noronha, South Georgia"), GMT_0100(-60, "(GMT -01:00) Azores, Cape, Eastern Greenland"), GMT0000(0,
				"(GMT 00:00) Coordinated Universal, Greenwich Mean, Western European"), GMT0100(60, "(GMT 01:00) Central European, Western African"), GMT0200(
				120, "(GMT 02:00) Central African, Eastern European, Israel, South Africa"), GMT0300(180,
				"(GMT 03:00) Arabia, Eastern African, Moscow"), GMT0330(210, "(GMT 03:30) Iran"), GMT0400(240,
				"(GMT 04:00) Aqtau, Armenia, Azerbaijan, Georgia, Gulf, Mauritius, Reunion, Samara, Seychelles"), GMT0430(260,
				"(GMT 04:30) Afghanistan"), GMT0500(
				300,
				"(GMT 05:00) Aqtobe, French Southern and Antarctic Lands, Indian Ocean Territory, Kirgizstan, Maldives, Pakistan, Tajikistan, Turkmenistan, Uzbekistan, Yekaterinburg"), GMT0530(
				330, "(GMT 05:30) India"), GMT0540(340, "(GMT 05:40) Nepal"), GMT0600(360,
				"(GMT 06:00) Alma-Ata, Bangladesh, Bhutan, Mawson, Novosibirsk, Sri Lanka"), GMT0630(390, "(GMT 06:30) Cocos Is., Myanmar"), GMT0700(
				420, "(GMT 07:00) Christmas Is., Indochina, Java, Krasnoyarsk"), GMT0800(480,
				"(GMT 08:00) Borneo, Brunei, China, Hong Kong, Irkutsk, Malaysia, Philippines, Singapore, Ulaanbaatar, Western Standard"), GMT0900(
				540, "(GMT 09:00) Korea, Japan, Jayapura, Palau, Yakutsk"), GMT0930(570, "(GMT 09:30) Central Standard"), GMT1000(600,
				"(GMT 10:00) Chamorro Standard, Dumont-d'Urville, Eastern Standard, Papua New Guinea, Truk, Vladivostok"), GMT1030(630,
				"(GMT 10:30) Load Howe Standard"), GMT1100(660, "(GMT 11:00) Kosrae, Magadan, New Caledonia, Ponape, Solomon Is., Vanuatu"), GMT1130(
				690, "(GMT 11:30) Norfolk"), GMT1200(720,
				"(GMT 12:00) Anadyr, Anadyr, Fiji, Gilbert Is., Marshall Is., Nauru, New Zealand, Petropavlovsk-Kamchatski, Tuvalu, Wake, Wallis and Futuna"), GMT1240(
				760, "(GMT 12:40) Chatham"), GMT1300(780, "(GMT 13:00) Phoenix Is., Tonga"), GMT1400(840, "(GMT 14:00) Line Is");

		private int code;
		private String str;

		private TimeZone(int code, String str) {
			this.code = code;
			this.str = str;
		}

		public static TimeZone get(int code) {
			for (TimeZone t : TimeZone.values()) {
				if (t.getCode() == code)
					return t;
			}
			return null;
		}

		public int getCode() {
			return code;
		}

		@Override
		public String toString() {
			return str;
		}
	}

	public static enum SyncType {
		Rdate, NtpDate;

		public static SyncType get(String str) {
			for (SyncType s : SyncType.values()) {
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

	private TimeZone zone; // 표준시간대
	private boolean useSync;
	private SyncType syncType; // 시간 서버와 동기화
	private String syncServer; // 시간 서버와 동기화

	@Override
	public String getXmlFilename() {
		return "system_basic.xml";
	}

	@Override
	public String getRootTagName() {
		return "system";
	}

	public static Basic parse(NodeWrapper node) {
		if (!node.isName("time"))
			return null;

		Basic b = new Basic();

		for (NodeWrapper c : node.children()) {
			if (c.isName("zone"))
				b.zone = TimeZone.get(c.intValue());
			else if (c.isName("sync")) {
				b.useSync = c.boolAttr("chk_use");
				b.syncType = SyncType.get(c.attr("type"));
				b.syncServer = c.value();
			}
		}

		return b;
	}

	public TimeZone getZone() {
		return zone;
	}

	public void setZone(TimeZone zone) {
		this.zone = zone;
	}

	public boolean isUseSync() {
		return useSync;
	}

	public void setUseSync(boolean useSync) {
		this.useSync = useSync;
	}

	public SyncType getSyncType() {
		return syncType;
	}

	public void setSyncType(SyncType syncType) {
		this.syncType = syncType;
	}

	public String getSyncServer() {
		return syncServer;
	}

	public void setSyncServer(String syncServer) {
		this.syncServer = syncServer;
	}

	@Override
	protected Element convertToElement(Document doc) {
		Element e = doc.createElement("time");

		appendChild(doc, e, "zone", zone.getCode());
		appendChild(doc, e, "sync", syncServer, new AttributeBuilder("chk_use", useSync).put("type", syncType));

		return e;
	}

	@Override
	public Map<String, Object> marshal() {
		Map<String, Object> m = new HashMap<String, Object>();
		m.put("zone", new MarshalValue("code", zone.code).put("name", zone.toString()).get());
		m.put("sync", new MarshalValue("use", useSync).put("type", syncType).put("server", syncServer).get());

		return m;
	}

	@Override
	public String toString() {
		return "Basic [zone=" + zone + ", syncType=" + syncType + ", syncServer=" + syncServer + "]";
	}
}
