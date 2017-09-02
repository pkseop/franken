package kr.co.future.logdb.query.parser;

import static kr.co.future.bnf.Syntax.*;
import kr.co.future.bnf.Binding;
import kr.co.future.bnf.Syntax;
import kr.co.future.logdb.LogQueryParser;
import kr.co.future.logdb.query.StringPlaceholder;
import kr.co.future.logdb.query.command.Term;
import kr.co.future.logdb.query.command.Term.Operator;

public class TermParser implements LogQueryParser {
	@Override
	public void addSyntax(Syntax syntax) {
		// @formatter:off
		syntax.add("term", this, new StringPlaceholder(), 
				choice(rule(choice(k("=="), k("!="), k(">="), k("<="), k(">"), k("<"), k("contain"), k("regexp"), k("in")), 
				new StringPlaceholder()), rule(choice(k("is null"), k("not null")))));
		// @formatter:on
	}

	@Override
	public Object parse(Binding b) {
		Binding[] v = b.getChildren();
		Term term = new Term();
		String lh = v[0].getValue().toString();
		term.setLh(lh);
		if (lh.startsWith("\"") && lh.endsWith("\"")) {
			term.setLhString(true);
			term.setLh(lh.substring(1, lh.length() - 1));
		}

		Object oper = v[1].getValue();
		if (oper == null)
			oper = v[1].getChildren()[0].getValue();
		term.setOperator(Operator.find(oper.toString()));

		if (term.getOperator() != Operator.IsNull && term.getOperator() != Operator.NotNull) {
			String rh = v[1].getChildren()[1].getValue().toString();
			term.setRh(rh);
			if (rh.startsWith("\"") && rh.endsWith("\"")) {
				term.setRhString(true);
				term.setRh(rh.substring(1, rh.length() - 1));
			}
		}

		return term;
	}
}
