package kr.co.future.sslvpn.xtmconf.network;

public enum AnomalyAction {
	Alert, Drop;

	public static AnomalyAction get(String str) {
		for (AnomalyAction a : AnomalyAction.values()) {
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
