package kr.co.future.logdb.query.parser;

import static kr.co.future.bnf.Syntax.k;
import static kr.co.future.bnf.Syntax.option;
import static kr.co.future.bnf.Syntax.ref;
import kr.co.future.bnf.Binding;
import kr.co.future.bnf.Syntax;
import kr.co.future.logdb.LogQueryParser;
import kr.co.future.logdb.query.StringPlaceholder;
import kr.co.future.logdb.query.command.Eval;
import kr.co.future.logdb.query.command.Term;

public class EvalParser implements LogQueryParser {
	@Override
	public void addSyntax(Syntax syntax) {
		syntax.add("eval", this, k("eval "), ref("term"), option(k("as "), new StringPlaceholder()));
		syntax.addRoot("eval");
	}

	@Override
	public Object parse(Binding b) {
		Term term = (Term) b.getChildren()[1].getValue();
		String column = term.toString();
		if (b.getChildren().length == 3)
			column = (String) b.getChildren()[2].getChildren()[1].getValue();
		return new Eval(term, column);
	}
}
