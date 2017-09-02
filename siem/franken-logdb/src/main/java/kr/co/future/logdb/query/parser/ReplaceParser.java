package kr.co.future.logdb.query.parser;

import static kr.co.future.bnf.Syntax.k;
import static kr.co.future.bnf.Syntax.ref;
import kr.co.future.bnf.Binding;
import kr.co.future.bnf.Syntax;
import kr.co.future.logdb.LogQueryParser;
import kr.co.future.logdb.query.StringPlaceholder;
import kr.co.future.logdb.query.command.Replace;
import kr.co.future.logdb.query.command.Term;

public class ReplaceParser implements LogQueryParser {
	@Override
	public void addSyntax(Syntax syntax) {
		syntax.add("replace", this, k("replace "), ref("option"), ref("term"), new StringPlaceholder());
		syntax.addRoot("replace");
	}

	@Override
	public Object parse(Binding b) {
		Term term = (Term) b.getChildren()[2].getValue();
		String value = (String) b.getChildren()[3].getValue();
		return new Replace(term, value);
	}
}
