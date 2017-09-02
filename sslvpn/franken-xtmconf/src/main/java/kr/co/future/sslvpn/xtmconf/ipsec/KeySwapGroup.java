package kr.co.future.sslvpn.xtmconf.ipsec;

public enum KeySwapGroup {
	MODP768, MODP1024, MODP1536, MODP2048, MODP3072;

	public static KeySwapGroup get(String name) {
		if (name == null)
			return null;
		else
			return valueOf(name);
	}
}
