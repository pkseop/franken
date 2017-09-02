package kr.co.future.sslvpn.xtmconf.waf;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import kr.co.future.msgbus.Marshaler;
import kr.co.future.sslvpn.xtmconf.XtmConfig;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

public abstract class WafObject extends XtmConfig {
	protected String cid;
	protected List<Pattern> pattern = new ArrayList<Pattern>();

	@Override
	public String getRootTagName() {
		return "waf";
	}

	public String getCid() {
		return cid;
	}

	public void setCid(String cid) {
		this.cid = cid;
	}

	public List<Pattern> getPattern() {
		return pattern;
	}

	public void setPattern(List<Pattern> pattern) {
		this.pattern = pattern;
	}

	@Override
	protected Element convertToElement(Document doc) {
		Element e = doc.createElement("pattern");

		e.setAttribute("cid", cid);
		for (Pattern p : pattern)
			e.appendChild(p.toElement(doc));

		return e;
	}

	@Override
	public Map<String, Object> marshal() {
		Map<String, Object> m = new HashMap<String, Object>();
		m.put("cid", cid);
		m.put("pattern", Marshaler.marshal(pattern));

		return m;
	}
}
