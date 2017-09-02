package kr.co.future.sslvpn.core.cluster;

public class ClusterOperation {
	private String category;
	private String operation;
	private Object obj;

	public ClusterOperation() {
	}

	public ClusterOperation(String category, String operation, Object obj) {
		this.category = category;
		this.operation = operation;
		this.obj = obj;
	}

	public String getCategory() {
		return category;
	}

	public void setCategory(String category) {
		this.category = category;
	}

	public String getOperation() {
		return operation;
	}

	public void setOperation(String operation) {
		this.operation = operation;
	}

	public Object getObj() {
		return obj;
	}

	public void setObj(Object obj) {
		this.obj = obj;
	}

	@Override
	public String toString() {
		return "category=" + category + ", operation=" + operation + ", obj=" + obj;
	}

}
