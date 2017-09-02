package kr.co.future.sslvpn.model;

import java.util.HashSet;
import java.util.Set;

public class NacClientPolicy {
	private int configPollingInterval = 60;
	private int fileReportInterval = 600;
	private int registryReportInterval = 600;
	private int hostReportInterval = 600;
	private int processSnapshotInterval = 600;
	private int processReportInterval = 60;
	private Set<String> fileChecklist = new HashSet<String>();
	private Set<String> registryChecklist = new HashSet<String>();

	public int getConfigPollingInterval() {
		return configPollingInterval;
	}

	public void setConfigPollingInterval(int configPollingInterval) {
		this.configPollingInterval = configPollingInterval;
	}

	public int getProcessSnapshotInterval() {
		return processSnapshotInterval;
	}

	public void setProcessSnapshotInterval(int processSnapshotInterval) {
		this.processSnapshotInterval = processSnapshotInterval;
	}

	public int getProcessReportInterval() {
		return processReportInterval;
	}

	public void setProcessReportInterval(int processReportInterval) {
		this.processReportInterval = processReportInterval;
	}
	
	public Set<String> getFileChecklist() {
		return fileChecklist;
	}

	public void setFileChecklist(Set<String> fileChecklist) {
		this.fileChecklist = fileChecklist;
	}

	public Set<String> getRegistryChecklist() {
		return registryChecklist;
	}

	public void setRegistryChecklist(Set<String> registryChecklist) {
		this.registryChecklist = registryChecklist;
	}

	public void setFileReportInterval(int fileReportInterval) {
		this.fileReportInterval = fileReportInterval;
	}

	public int getFileReportInterval() {
		return fileReportInterval;
	}

	public void setRegistryReportInterval(int registryReportInterval) {
		this.registryReportInterval = registryReportInterval;
	}

	public int getRegistryReportInterval() {
		return registryReportInterval;
	}

	public void setHostReportInterval(int hostReportInterval) {
		this.hostReportInterval = hostReportInterval;
	}

	public int getHostReportInterval() {
		return hostReportInterval;
	}
}
