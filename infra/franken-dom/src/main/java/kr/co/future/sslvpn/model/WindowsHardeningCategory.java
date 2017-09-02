package kr.co.future.sslvpn.model;

public enum WindowsHardeningCategory {
	UseFirewall(Type.Boolean), // 방화벽 사용 여부
	UseLogonPassword(Type.Boolean), // 로그인 암호 설정 여부
	UseScreenSaver(Type.Boolean), // 화면보호기 사용 여부
	UseScreenSaverPassword(Type.Boolean), // 화면보호기 암호 사용 여부
	UseShareFolderPassword(Type.Boolean), // 공유 폴더 암호 설정 여부
	UseManagementShareFolder(Type.Boolean), // 관리용 공유 폴더 사용 여부
	UseWindowsUpdate(Type.Boolean), // 윈도우 자동 업데이트 여부
	UseAutoLogin(Type.Boolean), // 자동 로그인 사용 여부
	UseGuestAccount(Type.Boolean), // 게스트 계정 사용 여부
	MinimumPasswordLength(Type.Integer), // 암호 최소 길이
	PasswordExpiry(Type.Integer), // 암호 최대 사용기간
	ScreenSaverIdleInterval(Type.Integer), // 화면보호기 대기시간
	CheckLatestWindowUpdate(Type.Boolean); // Windows 최신 패치 유무 체크

	private WindowsHardeningCategory(Type type) {
		this.type = type;
	}

	public boolean verify(String value, String baseline) {
		if (value == null)
			return false;
		
		if (type == Type.Boolean) {
			boolean v = Boolean.parseBoolean(value);
			boolean base = Boolean.parseBoolean(baseline);
			return v == base;
		} else if (type == Type.Integer) {
			int v = Integer.parseInt(value);
			int base = Integer.parseInt(baseline);
			return v >= base;
		}

		throw new IllegalStateException("unreachable");
	}

	private enum Type {
		Boolean, Integer
	};

	private Type type;
}
