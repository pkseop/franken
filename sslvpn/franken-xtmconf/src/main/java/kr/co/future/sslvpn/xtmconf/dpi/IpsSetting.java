package kr.co.future.sslvpn.xtmconf.dpi;

import java.util.HashMap;
import java.util.Map;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import kr.co.future.sslvpn.xtmconf.AttributeBuilder;
import kr.co.future.sslvpn.xtmconf.NodeWrapper;
import kr.co.future.sslvpn.xtmconf.XtmConfig;

public class IpsSetting extends XtmConfig {
	private boolean reassemble; // TCP Client 재조합
	private boolean tcpServer; // TCP Server 재조합
	private boolean decompression; // ?
	private boolean ids; // IDS Mode로 동작
	private boolean asymmetric; // Flow 기반탐지
	private boolean bypass; // ?

	@Override
	public String getXmlFilename() {
		return "ips_setting.xml";
	}

	@Override
	public String getRootTagName() {
		return "ips";
	}

	public static IpsSetting parse(NodeWrapper nw) {
		if (!nw.isName("setting"))
			return null;

		IpsSetting is = new IpsSetting();

		for (NodeWrapper c : nw.children()) {
			if (c.isName("reassemble"))
				is.reassemble = c.boolAttr("chk_use");
			else if (c.isName("tcp_server"))
				is.tcpServer = c.boolAttr("chk_use");
			else if (c.isName("decompression"))
				is.decompression = c.boolAttr("chk_use");
			else if (c.isName("ids"))
				is.ids = c.boolAttr("chk_use");
			else if (c.isName("asymmetric"))
				is.asymmetric = c.boolAttr("chk_use");
			else if (c.isName("bypass"))
				is.bypass = c.boolAttr("chk_use");
		}

		return is;
	}

	public boolean isReassemble() {
		return reassemble;
	}

	public void setReassemble(boolean reassemble) {
		this.reassemble = reassemble;
	}

	public boolean isTcpServer() {
		return tcpServer;
	}

	public void setTcpServer(boolean tcpServer) {
		this.tcpServer = tcpServer;
	}

	public boolean isDecompression() {
		return decompression;
	}

	public void setDecompression(boolean decompression) {
		this.decompression = decompression;
	}

	public boolean isIds() {
		return ids;
	}

	public void setIds(boolean ids) {
		this.ids = ids;
	}

	public boolean isAsymmetric() {
		return asymmetric;
	}

	public void setAsymmetric(boolean asymmetric) {
		this.asymmetric = asymmetric;
	}

	public boolean isBypass() {
		return bypass;
	}

	public void setBypass(boolean bypass) {
		this.bypass = bypass;
	}

	@Override
	protected Element convertToElement(Document doc) {
		Element e = doc.createElement("setting");

		appendChild(doc, e, "reassemble", null, new AttributeBuilder("chk_use", reassemble));
		appendChild(doc, e, "tcp_server", null, new AttributeBuilder("chk_use", tcpServer));
		appendChild(doc, e, "decompression", null, new AttributeBuilder("chk_use", decompression));
		appendChild(doc, e, "ids", null, new AttributeBuilder("chk_use", ids));
		appendChild(doc, e, "asymmetric", null, new AttributeBuilder("chk_use", asymmetric));
		appendChild(doc, e, "bypass", null, new AttributeBuilder("chk_use", bypass));

		return e;
	}

	@Override
	public Map<String, Object> marshal() {
		Map<String, Object> m = new HashMap<String, Object>();
		m.put("reassemble", reassemble);
		m.put("tcp_server", tcpServer);
		m.put("decompression", decompression);
		m.put("ids", ids);
		m.put("asymmetric", asymmetric);
		m.put("bypass", bypass);

		return m;
	}

	@Override
	public String toString() {
		return "IpsSetting [reassemble=" + reassemble + ", tcpServer=" + tcpServer + ", decompression=" + decompression
				+ ", ids=" + ids + ", asymmetric=" + asymmetric + ", bypass=" + bypass + "]";
	}
}
