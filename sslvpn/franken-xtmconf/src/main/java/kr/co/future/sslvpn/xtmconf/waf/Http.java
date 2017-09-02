package kr.co.future.sslvpn.xtmconf.waf;

import java.util.Arrays;
import java.util.List;

import kr.co.future.sslvpn.xtmconf.NodeWrapper;

public class Http extends WafObject {
	private static final List<Integer> ids = Arrays.asList(1001, 1002, 1003);

	@Override
	public String getXmlFilename() {
		return "waf_http.xml";
	}

	public static Http parse(NodeWrapper nw) {
		if (!nw.isName("pattern"))
			return null;

		Http h = new Http();
		h.cid = nw.attr("cid");

		for (NodeWrapper c : nw.children()) {
			if (c.name().startsWith("id_")) {
				try {
					int id = Integer.parseInt(c.name().substring(3));
					if (ids.contains(id))
						h.pattern.add(new Pattern(c));
				} catch (NumberFormatException e) {
				}
			}
		}

		return h;
	}

	public static List<Integer> getIds() {
		return ids;
	}

	@Override
	public String toString() {
		return "Http [cid=" + cid + ", pattern=" + pattern + "]";
	}
}
