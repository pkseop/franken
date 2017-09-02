package kr.co.future.sslvpn.authui.impl;

import kr.co.future.sslvpn.authui.MailTemplate;
import kr.co.future.sslvpn.authui.PasswordResetService;
import kr.co.future.api.Script;
import kr.co.future.api.ScriptArgument;
import kr.co.future.api.ScriptContext;
import kr.co.future.api.ScriptUsage;

public class PasswordResetScript implements Script {
	private PasswordResetService reset;
	private ScriptContext context;

	public PasswordResetScript(PasswordResetService reset) {
		this.reset = reset;
	}

	@Override
	public void setScriptContext(ScriptContext context) {
		this.context = context;
	}

	@ScriptUsage(description = "view or set reset mail subject", arguments = { @ScriptArgument(name = "subject", type = "string", description = "reset mail subject", optional = true) })
	public void subject(String[] args) {
		if (args.length > 0) {
			reset.setTemplate(new MailTemplate(args[0]));
			context.println("set");
		} else {
			MailTemplate t = reset.getTemplate();
			if (t == null)
				context.println("not set");
			else
				context.println(t.getSubject());
		}
	}
}
