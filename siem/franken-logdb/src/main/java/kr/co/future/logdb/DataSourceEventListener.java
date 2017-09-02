package kr.co.future.logdb;

public interface DataSourceEventListener {
	void onUpdate(DataSource ds);

	void onRemove(DataSource ds);
}
