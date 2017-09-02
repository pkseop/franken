package kr.co.future.sslvpn.xtmconf.waf;

import java.util.Arrays;
import java.util.List;

import kr.co.future.sslvpn.xtmconf.NodeWrapper;

public class Dir extends WafObject {
	private static final List<Integer> ids = Arrays.asList(2001);

	@Override
	public String getXmlFilename() {
		return "waf_dir.xml";
	}

	public static Dir parse(NodeWrapper nw) {
		if (!nw.isName("pattern"))
			return null;

		Dir d = new Dir();
		d.cid = nw.attr("cid");

		for (NodeWrapper c : nw.children()) {
			if (c.name().startsWith("id_")) {
				try {
					int id = Integer.parseInt(c.name().substring(3));
					if (ids.contains(id))
						d.pattern.add(new Pattern(c));
				} catch (NumberFormatException e) {
				}
			}
		}

		return d;
	}

	public static List<Integer> getIds() {
		return ids;
	}

	@Override
	public String toString() {
		return "Dir [cid=" + cid + ", pattern=" + pattern + "]";
	}
}
