package kr.co.future.sslvpn.xtmconf.network;

public enum DnsType {
	Info, Permit, Domain;

	public static DnsType get(String str) {
		for (DnsType d : DnsType.values()) {
			if (d.toString().equals(str))
				return d;
		}
		return null;
	}

	@Override
	public String toString() {
		return name().toLowerCase();
	}
}
