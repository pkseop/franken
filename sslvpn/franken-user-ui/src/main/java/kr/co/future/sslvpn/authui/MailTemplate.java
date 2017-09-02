package kr.co.future.sslvpn.authui;

import kr.co.future.confdb.CollectionName;

@CollectionName("reset_mail_template")
public class MailTemplate {
	private String subject;

	public MailTemplate() {
	}

	public MailTemplate(String subject) {
		this.subject = subject;
	}

	public String getSubject() {
		return subject;
	}

	public void setSubject(String subject) {
		this.subject = subject;
	}
}
