package kr.co.future.sslvpn.xtmconf.waf;

import java.util.Arrays;
import java.util.List;

import kr.co.future.sslvpn.xtmconf.NodeWrapper;

public class Injection extends WafObject {
	private static final List<Integer> ids = Arrays.asList(3001, 3002, 3003, 3004, 3005, 3006, 3007, 3008, 3009, 3010,
			3011);

	@Override
	public String getXmlFilename() {
		return "waf_injection.xml";
	}

	public static Injection parse(NodeWrapper nw) {
		if (!nw.isName("pattern"))
			return null;

		Injection i = new Injection();
		i.cid = nw.attr("cid");

		for (NodeWrapper c : nw.children()) {
			if (c.name().startsWith("id_")) {
				try {
					int id = Integer.parseInt(c.name().substring(3));
					if (ids.contains(id))
						i.pattern.add(new Pattern(c));
				} catch (NumberFormatException e) {
				}
			}
		}

		return i;
	}

	public static List<Integer> getIds() {
		return ids;
	}

	@Override
	public String toString() {
		return "Injection [cid=" + cid + ", pattern=" + pattern + "]";
	}
}
