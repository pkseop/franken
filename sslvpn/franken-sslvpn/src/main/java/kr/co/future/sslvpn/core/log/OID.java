package kr.co.future.sslvpn.core.log;

public class OID {
	public static final String SERIAL = "1.3.6.1.4.1.2142.11.1";
	public static final String MSGID = "1.3.6.1.4.1.2142.11.2";

	public static final String AUTH = "1.3.6.1.4.1.2142.11.10";
	public static final String ACCESS = "1.3.6.1.4.1.2142.11.11";
	public static final String FLOW = "1.3.6.1.4.1.2142.11.12";
	public static final String NIC = "1.3.6.1.4.1.2142.11.13";
	public static final String SYSTEM = "1.3.6.1.4.1.2142.11.14";

	public static final String AUTH_TYPE = AUTH + ".1";
	public static final String AUTH_CODE = AUTH + ".2";
	public static final String AUTH_LOGIN = AUTH + ".3";
	public static final String AUTH_PROFILE = AUTH + ".4";
	public static final String AUTH_REMOTE_IP = AUTH + ".5";
	public static final String AUTH_REMOTE_PORT = AUTH + ".6";
	public static final String AUTH_TUNNEL = AUTH + ".7";
	public static final String AUTH_NAT_IP = AUTH + ".8";
	public static final String AUTH_OS_TYPE = AUTH + ".9";
	public static final String AUTH_DEVICE_KEY = AUTH + ".10";
	public static final String AUTH_DISCONNECT_FORCED = AUTH + ".11";

	public static final String ACCESS_LOGIN = ACCESS + ".1";
	public static final String ACCESS_TUNNEL = ACCESS + ".2";
	public static final String ACCESS_SESSION = ACCESS + ".3";
	public static final String ACCESS_ACTION = ACCESS + ".4";
	public static final String ACCESS_CLIENT_IP = ACCESS + ".5";
	public static final String ACCESS_CLIENT_PORT = ACCESS + ".6";
	public static final String ACCESS_SERVER_IP = ACCESS + ".7";
	public static final String ACCESS_SERVER_PORT = ACCESS + ".8";
	public static final String ACCESS_PROTOCOL = ACCESS + ".9";
	
	public static final String FLOW_LOGIN = FLOW + ".1";
	public static final String FLOW_TUNNEL = FLOW + ".2";
	public static final String FLOW_SESSION = FLOW + ".3";
	public static final String FLOW_CLIENT_IP = FLOW + ".4";
	public static final String FLOW_CLIENT_PORT = FLOW + ".5";
	public static final String FLOW_SERVER_IP = FLOW + ".6";
	public static final String FLOW_SERVER_PORT = FLOW + ".7";
	public static final String FLOW_PROTOCOL = FLOW + ".8";
	public static final String FLOW_TX_BYTES = FLOW + ".9";
	public static final String FLOW_RX_BYTES = FLOW + ".10";
	public static final String FLOW_TX_PACKETS = FLOW + ".11";
	public static final String FLOW_RX_PACKETS = FLOW + ".12";
	public static final String FLOW_EOS = FLOW + ".13";

	public static final String NIC_IFACE = NIC + ".1";
	public static final String NIC_TX_BYTES = NIC + ".2";
	public static final String NIC_RX_BYTES = NIC + ".3";
	public static final String NIC_TX_PACKETS = NIC + ".4";
	public static final String NIC_RX_PACKETS = NIC + ".5";
	public static final String NIC_DATE = NIC + ".6";
}
