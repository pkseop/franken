package kr.co.future.sslvpn.xtmconf.ha;

public enum ActMode {
	Master, Slave;

	public static ActMode get(String str) {
		for (ActMode a : ActMode.values()) {
			if (a.toString().equals(str))
				return a;
		}
		return null;
	}

	@Override
	public String toString() {
		return name().toLowerCase();
	}

}
