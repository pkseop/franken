package kr.co.future.sslvpn.model;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import kr.co.future.api.CollectionTypeHint;
import kr.co.future.api.FieldOption;
import kr.co.future.confdb.CollectionName;

@CollectionName("nac_profiles")
public class NacProfile {

	private int id;

	private String guid = UUID.randomUUID().toString();

	private String name;

	private String description;

	private boolean useWindowsHardeningCheck;

	private boolean useProcessCheck;

	private boolean useFileCheck;

	private boolean useRegistryCheck;

	private boolean useWindowsUpdateCheck;

	@CollectionTypeHint(WindowsHardeningCheck.class)
	private List<WindowsHardeningCheck> windowsChecklist = new ArrayList<WindowsHardeningCheck>();

	@CollectionTypeHint(String.class)
	private List<String> requiredProcessNames = new ArrayList<String>();

	@CollectionTypeHint(String.class)
	private List<String> forbiddenProcessNames = new ArrayList<String>();

	@CollectionTypeHint(FileCheck.class)
	private List<FileCheck> fileChecks = new ArrayList<FileCheck>();

	@CollectionTypeHint(RegistryCheck.class)
	private List<RegistryCheck> registryChecks = new ArrayList<RegistryCheck>();

	@FieldOption(name = "created_at", nullable = false)
	private Date createDateTime;

	@FieldOption(name = "updated_at", nullable = false)
	private Date updateDateTime;

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public String getGuid() {
		return guid;
	}

	public void setGuid(String guid) {
		this.guid = guid;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public boolean isUseWindowsHardeningCheck() {
		return useWindowsHardeningCheck;
	}

	public void setUseWindowsHardeningCheck(boolean useWindowsHardeningCheck) {
		this.useWindowsHardeningCheck = useWindowsHardeningCheck;
	}

	public boolean isUseProcessCheck() {
		return useProcessCheck;
	}

	public void setUseProcessCheck(boolean useProcessCheck) {
		this.useProcessCheck = useProcessCheck;
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

	public boolean isUseWindowsUpdateCheck() {
		return useWindowsUpdateCheck;
	}

	public void setUseWindowsUpdateCheck(boolean useWindowsUpdateCheck) {
		this.useWindowsUpdateCheck = useWindowsUpdateCheck;
	}

	public List<WindowsHardeningCheck> getWindowsChecklist() {
		return windowsChecklist;
	}

	public void setWindowsChecklist(List<WindowsHardeningCheck> windowsChecklist) {
		this.windowsChecklist = windowsChecklist;
	}

	public List<String> getRequiredProcessNames() {
		return requiredProcessNames;
	}

	public void setRequiredProcessNames(List<String> requiredProcessNames) {
		this.requiredProcessNames = requiredProcessNames;
	}

	public List<String> getForbiddenProcessNames() {
		return forbiddenProcessNames;
	}

	public void setForbiddenProcessNames(List<String> forbiddenProcessNames) {
		this.forbiddenProcessNames = forbiddenProcessNames;
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

	public Date getCreateDateTime() {
		return createDateTime;
	}

	public void setCreateDateTime(Date createDateTime) {
		this.createDateTime = createDateTime;
	}

	public Date getUpdateDateTime() {
		return updateDateTime;
	}

	public void setUpdateDateTime(Date updateDateTime) {
		this.updateDateTime = updateDateTime;
	}

	@Override
	public String toString() {
		return "NacProfile [id=" + id + ", guid=" + guid + ", name=" + name + ", description=" + description
				+ ", useWindowsHardeningCheck=" + useWindowsHardeningCheck + ", useProcessCheck=" + useProcessCheck
				+ ", useFileCheck=" + useFileCheck + ", useRegistryCheck=" + useRegistryCheck
				+ ", useWindowsUpdateCheck=" + useWindowsUpdateCheck + ", windowsChecklist=" + windowsChecklist
				+ ", requiredProcessNames=" + requiredProcessNames + ", forbiddenProcessNames=" + forbiddenProcessNames
				+ ", fileChecks=" + fileChecks + ", registryChecks=" + registryChecks + ", createDateTime="
				+ createDateTime + ", updateDateTime=" + updateDateTime + "]";
	}

}
