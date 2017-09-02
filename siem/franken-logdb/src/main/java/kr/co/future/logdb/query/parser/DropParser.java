package kr.co.future.logdb.query.parser;

import static kr.co.future.bnf.Syntax.*;
import kr.co.future.bnf.Binding;
import kr.co.future.bnf.Syntax;
import kr.co.future.logdb.LogQueryParser;
import kr.co.future.logdb.query.command.Drop;

public class DropParser implements LogQueryParser {
	@Override
	public Object parse(Binding b) {
		return new Drop();
	}

	@Override
	public void addSyntax(Syntax syntax) {
		syntax.add("drop", this, k("drop"));
		syntax.addRoot("drop");
	}
}
