package kr.co.future.sslvpn.model;

import java.util.List;

public class FileCheck {
	private String name;

	// c:\Program Files\system32\notepad.exe
	private List<String> paths;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public List<String> getPaths() {
		return paths;
	}

	public void setPaths(List<String> paths) {
		this.paths = paths;
	}
}
