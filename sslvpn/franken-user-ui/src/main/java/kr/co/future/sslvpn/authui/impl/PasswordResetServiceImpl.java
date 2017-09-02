package kr.co.future.sslvpn.authui.impl;

import java.io.File;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.apache.felix.ipojo.annotations.Validate;

import kr.co.future.confdb.Config;
import kr.co.future.confdb.ConfigDatabase;
import kr.co.future.confdb.ConfigService;
import kr.co.future.sslvpn.authui.MailTemplate;
import kr.co.future.sslvpn.authui.PasswordResetService;

@Component(name = "frodo-passwd-reset-service")
@Provides
public class PasswordResetServiceImpl implements PasswordResetService {
	private MailTemplate t;
	private File dir = new File("/usr/sslplus/frodo/reset");

	@Requires
	private ConfigService conf;

	@Validate
	public void start() {
		reload();
	}

	@Override
	public File getResourceDir() {
		return dir;
	}

	@Override
	public long getExpireInterval() {
		// 1-hour by default
		return 3600;
	}

	@Override
	public MailTemplate getTemplate() {
		return t;
	}

	@Override
	public void setTemplate(MailTemplate t) {
		this.t = t;

		ConfigDatabase db = conf.ensureDatabase("frodo");
		Config c = db.findOne(MailTemplate.class, null);
		if (c == null) {
			db.add(t);
		} else {
			db.update(c, t);
		}
	}

	private void reload() {
		ConfigDatabase db = conf.ensureDatabase("frodo");
		Config c = db.findOne(MailTemplate.class, null);
		if (c != null)
			t = c.getDocument(MailTemplate.class);
	}
}
