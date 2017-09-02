package kr.co.future.sslvpn.xtmconf.alg;

public enum Permission {
	Accept, Deny;

	public static Permission get(String str) {
		for (Permission p : Permission.values()) {
			if (p.toString().equals(str))
				return p;
		}
		return null;
	}

	@Override
	public String toString() {
		return name().toLowerCase();
	}
}
