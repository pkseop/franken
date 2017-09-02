package kr.co.future.sslvpn.xtmconf.waf;

import java.util.Arrays;
import java.util.List;

import kr.co.future.sslvpn.xtmconf.NodeWrapper;

public class Basic extends WafObject {
	private static final List<Integer> ids = Arrays.asList(3001, 3002, 3003, 3004, 3005, 3006, 3007, 3008, 3009, 3010,
			4016, 4017, 4002, 4006, 4007, 4009, 4010, 4011, 4013, 4014, 4015);

	@Override
	public String getXmlFilename() {
		return "waf_basic.xml";
	}

	public static Basic parse(NodeWrapper nw) {
		if (!nw.isName("pattern"))
			return null;

		Basic b = new Basic();
		b.cid = nw.attr("cid");

		for (NodeWrapper c : nw.children()) {
			if (c.name().startsWith("id_")) {
				try {
					int id = Integer.parseInt(c.name().substring(3));
					if (ids.contains(id))
						b.pattern.add(new Pattern(c));
				} catch (NumberFormatException e) {
				}
			}
		}

		return b;
	}

	public static List<Integer> getIds() {
		return ids;
	}

	@Override
	public String toString() {
		return "Basic [cid=" + cid + ", pattern=" + pattern + "]";
	}
}
