package kr.co.future.sslvpn.core.ftp;

public enum FtpCode {
	Success(0),
	ConnectionFail(1),
	UploadFileFail(2),
	UploadFileNotFound(3),
	DownloadFileFail(4),
	DownloadFileNotFound(5),
	DeleteFail(6);
	
	FtpCode(int code) {
		this.code = code;
	}
	private int code;
	
	public int getCode() {
		return code;
	}
	
	public static FtpCode parse(int code) {
		for (FtpCode c : values())
			if (c.getCode() == code)
				return c;

		return null;
	}
}
