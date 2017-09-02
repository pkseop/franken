package kr.co.future.logdb;

public interface LogQueryEventListener {
	void onQueryStatusChange(LogQuery query, LogQueryStatus status);
}
