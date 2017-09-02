package kr.co.future.sslvpn.model;

import java.util.Map;

public class ReferenceException extends RuntimeException {
	private static final long serialVersionUID = 1L;

	private Map<String, Object> references;

	public ReferenceException(Map<String, Object> references) {
		this.references = references;
	}

	public Map<String, Object> getReferences() {
		return references;
	}
}
