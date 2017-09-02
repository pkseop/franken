package kr.co.future.sslvpn.model;

import java.util.HashMap;
import java.util.Map;

import kr.co.future.api.FieldOption;
import kr.co.future.msgbus.Marshalable;

public class AntiVirusCheck implements Marshalable {

	@FieldOption(name = "name", nullable = false)
	private String name;

	@FieldOption(name = "process", nullable = false)
	private String processName;

	@FieldOption(name = "exec_path", nullable = true)
	private String execPath;

	@FieldOption(name = "install_path", nullable = true)
	private String installPath;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getProcess() {
		return processName;
	}

	public void setProcess(String processName) {
		this.processName = processName;
	}

	public String getExecPath() {
		return execPath;
	}

	public void setExecPath(String execPath) {
		this.execPath = execPath;
	}

	public String getInstallPath() {
		return installPath;
	}

	public void setInstallPath(String installPath) {
		this.installPath = installPath;
	}

	@Override
	public String toString() {
		return "AntiVirusCheck [name=" + name + ", process=" + processName + ", exec_path=" + execPath + ", install_path="
				+ installPath + "]";
	}

	@Override
	public Map<String, Object> marshal() {
		Map<String, Object> m = new HashMap<String, Object>();
		m.put("name", name);
		m.put("process", processName);
		m.put("exec_path", execPath);
		m.put("install_path", installPath);

		return m;
	}
}
