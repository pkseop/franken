package kr.co.future.logdb.query.command;

import kr.co.future.logdb.LogQueryCommand;

public class Replace extends LogQueryCommand {
	private Term term;
	private String value;

	public Replace(Term term, String value) {
		this.term = term;
		this.value = value;
	}

	@Override
	public void push(LogMap m) {
		if (term.eval(m))
			m.put(term.getLh().toString(), value);
		write(m);
	}

	@Override
	public boolean isReducer() {
		return false;
	}
}
