package kr.co.future.logdb.query.command;

import kr.co.future.logdb.LogQueryCommand;

public class Eval extends LogQueryCommand {
	private Term term;
	private String column;

	public Eval(Term term) {
		this(term, term.toString());
	}

	public Eval(Term term, String column) {
		this.term = term;
		this.column = column;
	}

	@Override
	public void push(LogMap m) {
		m.put(column, term.eval(m));
		write(m);
	}

	@Override
	public boolean isReducer() {
		return false;
	}
}
