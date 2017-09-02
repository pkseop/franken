package kr.co.future.sslvpn.xtmconf.network;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import kr.co.future.sslvpn.xtmconf.NodeWrapper;
import kr.co.future.sslvpn.xtmconf.XtmConfig;

public class RouterScript extends XtmConfig {

	private boolean useScript;

	public boolean isUseScript() {
		return useScript;
	}

	public void setUseScript(boolean useScript) {
		this.useScript = useScript;
	}

	public static RouterScript parse(NodeWrapper nw) {
		if (!nw.isName("router_script"))
			return null;

		RouterScript rs = new RouterScript();
		for (NodeWrapper c : nw.children())
			if (c.isName("useScript"))
				rs.setUseScript(c.boolValue());

		return rs;
	}

	@Override
	public Map<String, Object> marshal() {
		String script = "";

		File f = new File("/etc/webadmin/conf/routing_script");
		if (f.exists() && f.canRead()) {
			FileInputStream is = null;
			BufferedReader br = null;
			try {
				is = new FileInputStream(f);
				br = new BufferedReader(new InputStreamReader(is));

				while (true) {
					String line = br.readLine();
					if (line == null)
						break;

					script += line + "\n";
				}
			} catch (FileNotFoundException e) {
			} catch (IOException e) {
			} finally {
				if (br != null)
					try {
						br.close();
					} catch (IOException e) {
				}
				if (is != null)
					try {
						is.close();
					} catch (IOException e) {
					}
			}
		}

		Map<String, Object> m = new HashMap<String, Object>();
		m.put("use_script", useScript);
		m.put("script", script);
		return m;
	}

	@Override
	public String getXmlFilename() {
		return "network_router_script.xml";
	}

	@Override
	public String getRootTagName() {
		return "network_router_script";
	}

	@Override
	protected Element convertToElement(Document doc) {
		Element e = doc.createElement("router_script");
		appendChild(doc, e, "useScript", useScript);
		return e;
	}

}
