package kr.co.future.sslvpn.core;

import kr.co.future.sslvpn.core.OsType;

public enum OsType {
	Windows(1), Linux(2), Android(3), MacOS(4), iOS(5);

	OsType(int osCode) {
		this.osCode = osCode;
	}

	public int getCode() {
		return osCode;
	}

	public static OsType FromType(int typeCode) {
		if (typeCode == 0)
			return OsType.Windows;
		else if (typeCode == 1)
			return OsType.Windows;
		else if (typeCode == 2)
			return OsType.Linux;
		else if (typeCode == 3)
			return OsType.Android;
		else if (typeCode == 4)
			return OsType.MacOS;
		else if (typeCode == 5)
			return OsType.iOS;
		else
			return null;

	}

	private int osCode;
}
