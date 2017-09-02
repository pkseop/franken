package kr.co.future.sslvpn.core.backup;

public enum BackupType {
	Log(0),
	Policy(1);

	BackupType(int type) {
		this.type = type;
	}
	private int type;
	
	public int getType() {
		return type;
	}
	
	public static BackupType parse(int type) {
		for (BackupType c : values())
			if (c.getType() == type)
				return c;

		return null;
	}
}
