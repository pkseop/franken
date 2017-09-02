package kr.co.future.sslvpn.xtmconf.alg;

import java.util.HashMap;
import java.util.Map;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import kr.co.future.sslvpn.xtmconf.NodeWrapper;
import kr.co.future.sslvpn.xtmconf.XtmConfig;

public class TelnetProxy extends XtmConfig {
	private int num;
	private String publicIp; // Public Telnet Server IP
	private String ip; // Real Telnet Server IP
	private int port; // 포트번호
	private int limit; // 시간초과
	private int user; // 최대 접속수
	private Permission upload; // 업로드
	private Permission download; // 다운로드
	private int action; // 동작방식, 일반게이트 방식(0), 투명게이트 방식(1)

	@Override
	public String getXmlFilename() {
		return "alg_telnet_proxy.xml";
	}

	@Override
	public String getRootTagName() {
		return "alg";
	}

	public static TelnetProxy parse(NodeWrapper nw) {
		if (!nw.isName("telnet"))
			return null;

		TelnetProxy tp = new TelnetProxy();
		tp.num = nw.intAttr("num");

		for (NodeWrapper c : nw.children()) {
			if (c.isName("public_ip"))
				tp.publicIp = c.value();
			else if (c.isName("ip"))
				tp.ip = c.value();
			else if (c.isName("port"))
				tp.port = c.intValue();
			else if (c.isName("limit"))
				tp.limit = c.intValue();
			else if (c.isName("user"))
				tp.user = c.intValue();
			else if (c.isName("upload"))
				tp.upload = Permission.get(c.value());
			else if (c.isName("download"))
				tp.download = Permission.get(c.value());
			else if (c.isName("action"))
				tp.action = c.intValue();
		}

		return tp;
	}

	public int getNum() {
		return num;
	}

	public void setNum(int num) {
		this.num = num;
	}

	public String getPublicIp() {
		return publicIp;
	}

	public void setPublicIp(String publicIp) {
		this.publicIp = publicIp;
	}

	public String getIp() {
		return ip;
	}

	public void setIp(String ip) {
		this.ip = ip;
	}

	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}

	public int getLimit() {
		return limit;
	}

	public void setLimit(int limit) {
		this.limit = limit;
	}

	public int getUser() {
		return user;
	}

	public void setUser(int user) {
		this.user = user;
	}

	public Permission getUpload() {
		return upload;
	}

	public void setUpload(Permission upload) {
		this.upload = upload;
	}

	public Permission getDownload() {
		return download;
	}

	public void setDownload(Permission download) {
		this.download = download;
	}

	public int getAction() {
		return action;
	}

	public void setAction(int action) {
		this.action = action;
	}

	@Override
	protected Element convertToElement(Document doc) {
		Element e = doc.createElement("telnet");

		e.setAttribute("num", String.valueOf(num));
		appendChild(doc, e, "public_ip", publicIp);
		appendChild(doc, e, "ip", ip);
		appendChild(doc, e, "port", port);
		appendChild(doc, e, "limit", limit);
		appendChild(doc, e, "user", user);
		appendChild(doc, e, "upload", upload);
		appendChild(doc, e, "download", download);
		appendChild(doc, e, "action", action);

		return e;
	}

	@Override
	public Map<String, Object> marshal() {
		Map<String, Object> m = new HashMap<String, Object>();
		m.put("num", num);
		m.put("public_ip", publicIp);
		m.put("ip", ip);
		m.put("port", port);
		m.put("limit", limit);
		m.put("user", user);
		m.put("upload", upload);
		m.put("download", download);
		m.put("action", action);

		return m;
	}

	@Override
	public String toString() {
		return "TelnetProxy [num=" + num + ", publicIp=" + publicIp + ", ip=" + ip + ", port=" + port + ", limit="
				+ limit + ", user=" + user + ", upload=" + upload + ", download=" + download + ", action=" + action
				+ "]";
	}
}
