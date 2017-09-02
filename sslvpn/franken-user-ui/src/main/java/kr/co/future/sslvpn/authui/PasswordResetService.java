package kr.co.future.sslvpn.authui;

import java.io.File;

public interface PasswordResetService {
	File getResourceDir();

	// in seconds
	long getExpireInterval();

	MailTemplate getTemplate();

	void setTemplate(MailTemplate t);
}
