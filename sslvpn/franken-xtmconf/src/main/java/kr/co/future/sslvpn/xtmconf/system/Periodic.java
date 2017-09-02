package kr.co.future.sslvpn.xtmconf.system;

public enum Periodic {
	Hour, Day, Weekly, Monthly;

	public static Periodic get(String str) {
		for (Periodic p : Periodic.values()) {
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
