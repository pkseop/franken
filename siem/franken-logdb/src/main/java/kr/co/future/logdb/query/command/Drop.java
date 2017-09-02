package kr.co.future.logdb.query.command;

import kr.co.future.logdb.LogQueryCommand;

public class Drop extends LogQueryCommand {
	@Override
	public void push(LogMap m) {
	}

	@Override
	public boolean isReducer() {
		return true;
	}
}
