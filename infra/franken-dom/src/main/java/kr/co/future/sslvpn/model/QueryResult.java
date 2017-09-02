package kr.co.future.sslvpn.model;

import java.util.List;

public class QueryResult {
	private List<CertificateData> items;
	private int totalCount;

	public List<CertificateData> getItems() {
		return items;
	}

	public void setItems(List<CertificateData> items) {
		this.items = items;
	}

	public int getTotalCount() {
		return totalCount;
	}

	public void setTotalCount(int totalCount) {
		this.totalCount = totalCount;
	}

}
