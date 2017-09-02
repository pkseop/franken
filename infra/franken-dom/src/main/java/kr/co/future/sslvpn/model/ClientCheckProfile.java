package kr.co.future.sslvpn.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import kr.co.future.api.CollectionTypeHint;
import kr.co.future.api.FieldOption;
import kr.co.future.confdb.CollectionName;
import kr.co.future.msgbus.Marshalable;
import kr.co.future.msgbus.Marshaler;

@CollectionName("client_check_profiles")
public class ClientCheckProfile implements Marshalable {

	private String name;

	private String guid = UUID.randomUUID().toString();

	@FieldOption(name = "use_wifi_check")
	private Boolean useWifiCheck;

	@FieldOption(name = "wifi_check_msg")
	private String wifiCheckMsg;

	@FieldOption(name = "use_android_rooting_check")
	private Boolean useAndroidRootingCheck;

	@FieldOption(name = "android_rooting_check_fail_msg")
	private String androidRootingCheckFailMessage;

	@FieldOption(name = "use_antivirus_check")
	private boolean useAntivirusCheck;
	
	@FieldOption(name = "use_and_check")
	private Boolean useAndCheck;

	@FieldOption(name = "use_windows_hardening_check")
	private boolean useWindowsHardeningCheck;

	@FieldOption(name = "use_file_check")
	private boolean useFileCheck;

	@FieldOption(name = "use_registry_check")
	private boolean useRegistryCheck;

	@FieldOption(name = "file_check_fail_msg")
	private String fileCheckFailMessage;

	@FieldOption(name = "registry_check_fail_msg")
	private String registryCheckFailMessage;

	@FieldOption(name = "antivirus_fail_msg")
	private String antivirusFailMessage;

	@CollectionTypeHint(String.class)
	@FieldOption(name = "antivirus_checklist")
	private List<String> antivirusCheckList = new ArrayList<String>();

	@CollectionTypeHint(AntiVirusCheck.class)
	@FieldOption(name = "antivirus_checks")
	private List<AntiVirusCheck> antiVirusCheckList = new ArrayList<AntiVirusCheck>();

	@CollectionTypeHint(WindowsHardeningCheck.class)
	@FieldOption(name = "hardening_checklist")
	private List<WindowsHardeningCheck> windowsChecklist = new ArrayList<WindowsHardeningCheck>();

	@CollectionTypeHint(FileCheck.class)
	@FieldOption(name = "file_checklist")
	private List<FileCheck> fileChecks = new ArrayList<FileCheck>();

	@CollectionTypeHint(RegistryCheck.class)
	@FieldOption(name = "registry_checklist")
	private List<RegistryCheck> registryChecks = new ArrayList<RegistryCheck>();

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getGuid() {
		return guid;
	}

	public void setGuid(String guid) {
		this.guid = guid;
	}

	public Boolean getUseWifiCheck() {
		return useWifiCheck;
	}

	public void setUseWifiCheck(Boolean useWifiCheck) {
		this.useWifiCheck = useWifiCheck;
	}

	public String getWifiCheckMsg() {
		return wifiCheckMsg;
	}

	public void setWifiCheckMsg(String wifiCheckMsg) {
		this.wifiCheckMsg = wifiCheckMsg;
	}

	public Boolean getUseAndroidRootingCheck() {
		return useAndroidRootingCheck;
	}

	public void setUseAndroidRootingCheck(Boolean useAndroidRootingCheck) {
		this.useAndroidRootingCheck = useAndroidRootingCheck;
	}

	public String getAndroidRootingCheckFailMessage() {
		return androidRootingCheckFailMessage;
	}

	public void setAndroidRootingCheckFailMessage(String androidRootingCheckFailMessage) {
		this.androidRootingCheckFailMessage = androidRootingCheckFailMessage;
	}

	public boolean isUseAntivirusCheck() {
		return useAntivirusCheck;
	}

	public void setUseAntivirusCheck(boolean useAntivirusCheck) {
		this.useAntivirusCheck = useAntivirusCheck;
	}
	
	public Boolean getUseAndCheck() {
		return useAndCheck;
	}

	public void setUseAndCheck(Boolean useAndCheck) {
		this.useAndCheck = useAndCheck;
	}

	public boolean isUseWindowsHardeningCheck() {
		return useWindowsHardeningCheck;
	}

	public void setUseWindowsHardeningCheck(boolean useWindowsHardeningCheck) {
		this.useWindowsHardeningCheck = useWindowsHardeningCheck;
	}

	public boolean isUseFileCheck() {
		return useFileCheck;
	}

	public void setUseFileCheck(boolean useFileCheck) {
		this.useFileCheck = useFileCheck;
	}

	public boolean isUseRegistryCheck() {
		return useRegistryCheck;
	}

	public void setUseRegistryCheck(boolean useRegistryCheck) {
		this.useRegistryCheck = useRegistryCheck;
	}

	public String getFileCheckFailMessage() {
		return fileCheckFailMessage;
	}

	public void setFileCheckFailMessage(String fileCheckFailMessage) {
		this.fileCheckFailMessage = fileCheckFailMessage;
	}

	public String getRegistryCheckFailMessage() {
		return registryCheckFailMessage;
	}

	public void setRegistryCheckFailMessage(String registryCheckFailMessage) {
		this.registryCheckFailMessage = registryCheckFailMessage;
	}

	public String getAntivirusFailMessage() {
		return antivirusFailMessage;
	}

	public void setAntivirusFailMessage(String antivirusFailMessage) {
		this.antivirusFailMessage = antivirusFailMessage;
	}

	public List<AntiVirusCheck> getAntivirusCheckList() {
		if (antivirusCheckList != null && !antivirusCheckList.isEmpty()) {
			for (String antiVirusProcess : antivirusCheckList) {
				AntiVirusCheck avc = new AntiVirusCheck();
				avc.setName(antiVirusProcess);
				avc.setProcess(antiVirusProcess);
				antiVirusCheckList.add(avc);
			}
			this.antivirusCheckList = null;
		}

		return antiVirusCheckList;
	}

	public void setAntivirusCheckList(List<AntiVirusCheck> antiVirusCheckList) {
		if (antivirusCheckList != null && !antivirusCheckList.isEmpty()) {
			for (String antiVirusProcess : antivirusCheckList) {
				AntiVirusCheck avc = new AntiVirusCheck();
				avc.setName(antiVirusProcess);
				avc.setProcess(antiVirusProcess);
				antiVirusCheckList.add(avc);
			}
			this.antivirusCheckList = null;
		}
		this.antiVirusCheckList = antiVirusCheckList;
	}

	public List<WindowsHardeningCheck> getWindowsChecklist() {
		return windowsChecklist;
	}

	public void setWindowsChecklist(List<WindowsHardeningCheck> windowsChecklist) {
		this.windowsChecklist = windowsChecklist;
	}

	public List<FileCheck> getFileChecks() {
		return fileChecks;
	}

	public void setFileChecks(List<FileCheck> fileChecks) {
		this.fileChecks = fileChecks;
	}

	public List<RegistryCheck> getRegistryChecks() {
		return registryChecks;
	}

	public void setRegistryChecks(List<RegistryCheck> registryChecks) {
		this.registryChecks = registryChecks;
	}

	@Override
	public String toString() {

		return "ClientCheckProfile [guid=" + guid + ", useWifiCheck=" + useWifiCheck + ", wifiCheckMsg" + wifiCheckMsg
				+ ", useAndroidRootingCheck=" + useAndroidRootingCheck + ", useAntivirusCheck=" + useAntivirusCheck + ", useAndCheck=" + useAndCheck
				+ ", useWindowsHardeningCheck=" + useWindowsHardeningCheck + ", useFileCheck=" + useFileCheck
				+ ", useRegistryCheck=" + useRegistryCheck + ", androidRootingCheckFailMEssage=" + androidRootingCheckFailMessage
				+ ", fileCheckFailMessage=" + fileCheckFailMessage + ", registryCheckFailMessage=" + registryCheckFailMessage
				+ ", antivirusFailMessage=" + antivirusFailMessage + ", antivirusChecks=" + antivirusCheckList
				+ ", windowsChecklist=" + windowsChecklist + ", fileChecks=" + fileChecks + ", registryChecks=" + registryChecks;
	}

	@Override
	public Map<String, Object> marshal() {

		Collection<Object> list = new ArrayList<Object>();

		Map<String, Object> m = new HashMap<String, Object>();
		m.put("guid", guid);
		m.put("name", name);
		m.put("use_wifi_check", useWifiCheck);
		m.put("wifi_check_msg", wifiCheckMsg);
		m.put("use_android_rooting_check", useAndroidRootingCheck);
		m.put("use_antivirus_check", useAntivirusCheck);
		m.put("use_and_check", useAndCheck == null ? false : useAndCheck);
		m.put("use_windows_hardening_check", useWindowsHardeningCheck);
		m.put("use_file_check", useFileCheck);
		m.put("use_registry_check", useRegistryCheck);
		m.put("android_rooting_check_fail_msg", androidRootingCheckFailMessage);
		m.put("file_check_fail_msg", fileCheckFailMessage);
		m.put("registry_check_fail_msg", registryCheckFailMessage);
		m.put("antivirus_fail_msg", antivirusFailMessage);
		m.put("antivirus_checks", Marshaler.marshal(getAntivirusCheckList()));

		for (WindowsHardeningCheck c : windowsChecklist) {
			HashMap<String, Object> mm = new HashMap<String, Object>();
			mm.put("enforce", c.isEnforce());
			mm.put("category", c.getCategory());

			if (c.getBaseline() == null)
				mm.put("baseline", c.getBaseline());
			else
				mm.put("baseline", c.getBaseline());

			list.add(mm);
		}

		m.put("hardening_checklist", list);

		list = new ArrayList<Object>();
		for (FileCheck c : fileChecks) {
			Collection<String> l = new ArrayList<String>();
			l = c.getPaths();
			HashMap<String, Object> mm = new HashMap<String, Object>();
			mm.put("name", c.getName());
			mm.put("paths", l);

			list.add(mm);
		}
		m.put("file_checks", list);

		list = new ArrayList<Object>();
		for (RegistryCheck c : registryChecks) {
			HashMap<String, Object> mm = new HashMap<String, Object>();
			mm.put("key", c.getKey());
			mm.put("type", c.getType());
			mm.put("value", c.getValue());
			mm.put("check_value", c.isCheckValue());
			list.add(mm);
		}
		m.put("registry_checks", list);

		return m;
	}
}
