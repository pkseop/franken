package kr.co.future.sslvpn.xtmconf.system;

import java.util.HashMap;
import java.util.Map;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import kr.co.future.sslvpn.xtmconf.AttributeBuilder;
import kr.co.future.sslvpn.xtmconf.MarshalValue;
import kr.co.future.sslvpn.xtmconf.NodeWrapper;
import kr.co.future.sslvpn.xtmconf.XtmConfig;

public class SettingOption extends XtmConfig {
	private String name; // 장비 이름 설정
	private boolean stateful; // Stateful inspection 사용
	private int timeout; // WebSMC 타임아웃 설정
	private int policyPort; // WebSMC 접속포트 설정
	private int sshPort; // SSH 접속포트 설정
	private boolean bp1; // bypass 설정
	private boolean bp2; // bypass 설정
	private boolean bp3; // bypass 설정
	private boolean bp4; // bypass 설정

	@Override
	public String getXmlFilename() {
		return "system_setting_option.xml";
	}

	@Override
	public String getRootTagName() {
		return "system";
	}

	public static SettingOption parse(NodeWrapper nw) {
		if (!nw.isName("option"))
			return null;

		SettingOption so = new SettingOption();

		for (NodeWrapper c : nw.children()) {
			if (c.isName("name"))
				so.name = c.value();
			else if (c.isName("stateful"))
				so.stateful = c.boolAttr("chk_use");
			else if (c.isName("timeout"))
				so.timeout = c.intValue();
			else if (c.isName("policy_port"))
				so.policyPort = c.intValue();
			else if (c.isName("ssh_port"))
				so.sshPort = c.intValue();
			else if (c.isName("bypass")) {
				so.bp1 = c.boolAttr("bp1");
				so.bp2 = c.boolAttr("bp2");
				so.bp3 = c.boolAttr("bp3");
				so.bp4 = c.boolAttr("bp4");
			}
		}

		return so;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public boolean isStateful() {
		return stateful;
	}

	public void setStateful(boolean stateful) {
		this.stateful = stateful;
	}

	public int getTimeout() {
		return timeout;
	}

	public void setTimeout(int timeout) {
		this.timeout = timeout;
	}

	public int getPolicyPort() {
		return policyPort;
	}

	public void setPolicyPort(int policyPort) {
		this.policyPort = policyPort;
	}

	public int getSshPort() {
		return sshPort;
	}

	public void setSshPort(int sshPort) {
		this.sshPort = sshPort;
	}

	public boolean isBp1() {
		return bp1;
	}

	public void setBp1(boolean bp1) {
		this.bp1 = bp1;
	}

	public boolean isBp2() {
		return bp2;
	}

	public void setBp2(boolean bp2) {
		this.bp2 = bp2;
	}

	public boolean isBp3() {
		return bp3;
	}

	public void setBp3(boolean bp3) {
		this.bp3 = bp3;
	}

	public boolean isBp4() {
		return bp4;
	}

	public void setBp4(boolean bp4) {
		this.bp4 = bp4;
	}

	@Override
	protected Element convertToElement(Document doc) {
		Element e = doc.createElement("option");

		appendChild(doc, e, "name", name);
		appendChild(doc, e, "stateful", null, new AttributeBuilder("chk_use", stateful));
		appendChild(doc, e, "timeout", timeout);
		appendChild(doc, e, "port", 8443);
		appendChild(doc, e, "policy_port", policyPort);
		appendChild(doc, e, "ssh_port", sshPort);
		AttributeBuilder bpAttr = new AttributeBuilder("bp1", bp1).put("bp2", bp2).put("bp3", bp3).put("bp4", bp4);
		appendChild(doc, e, "bypass", null, bpAttr);

		return e;
	}

	@Override
	public Map<String, Object> marshal() {
		Map<String, Object> m = new HashMap<String, Object>();
		m.put("name", name);
		m.put("stateful", stateful);
		m.put("timeout", timeout);
		m.put("policy_port", policyPort);
		m.put("ssh_port", sshPort);
		m.put("bypass", new MarshalValue("bp1", bp1).put("bp2", bp2).put("bp3", bp3).put("bp4", bp4).get());

		return m;
	}

	@Override
	public String toString() {
		return "SettingOption [name=" + name + ", stateful=" + stateful + ", timeout=" + timeout + ", policyPort="
				+ policyPort + ", sshPort=" + sshPort + ", bp1=" + bp1 + ", bp2=" + bp2 + ", bp3=" + bp3 + ", bp4="
				+ bp4 + "]";
	}
}
