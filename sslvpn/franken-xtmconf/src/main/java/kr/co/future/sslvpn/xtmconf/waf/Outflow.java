package kr.co.future.sslvpn.xtmconf.waf;

import java.util.Arrays;
import java.util.List;

import kr.co.future.sslvpn.xtmconf.NodeWrapper;

public class Outflow extends WafObject {
	private static final List<Integer> ids = Arrays.asList(5001, 5002, 5003, 5004, 5005, 5006);

	@Override
	public String getXmlFilename() {
		return "waf_outflow.xml";
	}

	public static Outflow parse(NodeWrapper nw) {
		if (!nw.isName("pattern"))
			return null;

		Outflow o = new Outflow();
		o.cid = nw.attr("cid");

		for (NodeWrapper c : nw.children()) {
			if (c.name().startsWith("id_")) {
				try {
					int id = Integer.parseInt(c.name().substring(3));
					if (ids.contains(id))
						o.pattern.add(new Pattern(c));
				} catch (NumberFormatException e) {
				}
			}
		}

		return o;
	}

	public static List<Integer> getIds() {
		return ids;
	}

	@Override
	public String toString() {
		return "Outflow [cid=" + cid + ", pattern=" + pattern + "]";
	}
}
