package kr.co.future.sslvpn.xtmconf.waf;

import java.util.Arrays;
import java.util.List;

import kr.co.future.sslvpn.xtmconf.NodeWrapper;

public class Header extends WafObject {
	private static final List<Integer> ids = Arrays.asList(4016, 4017, 4001, 4002, 4004, 4006, 4007, 4008, 4009, 4010,
			4011, 4013, 4014, 4015);

	@Override
	public String getXmlFilename() {
		return "waf_header.xml";
	}

	public static Header parse(NodeWrapper nw) {
		if (!nw.isName("pattern"))
			return null;

		Header h = new Header();
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
		return "Header [cid=" + cid + ", pattern=" + pattern + "]";
	}
}
