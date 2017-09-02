package kr.co.future.sslvpn.core.backup;

public enum BackupCode {
	Success(0),	
	ConnectionFail(1),
	UploadFileFail(2),
	UploadFileNotFound(3),			
	FtpConifgNotSet(7),			// 0~7 번까지는 FtpCode 값과 같다.
	LocalSuccess(20),			//(S)FTP를 사용하지 않고 장비에 백업데이터를 저장할 경우.
	Rollbacked(30);
	
	BackupCode(int code) {
		this.code = code;
	}
	private int code;
	
	public int getCode() {
		return code;
	}
	
	public static BackupCode parse(int code) {
		for (BackupCode c : values())
			if (c.getCode() == code)
				return c;

		return null;
	}
}
