package kr.co.future.sslvpn.core.impl;

import java.util.Locale;
import java.util.Map;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Invalidate;
import org.apache.felix.ipojo.annotations.Requires;
import org.apache.felix.ipojo.annotations.Validate;
import kr.co.future.msgbus.ResourceApi;
import kr.co.future.msgbus.ResourceHandler;

@Component(name = "frodo-msg-localizer")
public class FrodoMessageLocalizer implements ResourceHandler {

	@Requires
	private ResourceApi resourceApi;

	@Validate
	public void start() {
		resourceApi.register("frodo", this);
	}

	@Invalidate
	public void stop() {
		if (resourceApi != null)
			resourceApi.unregister("frodo", this);
	}

	@Override
	public String formatText(String key, Locale locale, String[] params) {
		return null;
	}

	@Override
	public String formatText(String key, Locale locale, Map<String, Object> properties) {
		if (key == null)
			return null;

		if (key.equals("rawmsg"))
			return (String) properties.get("msg");

		return null;
	}

	@Override
	public String getText(String key, Locale locale) {
		return null;
	}

}
