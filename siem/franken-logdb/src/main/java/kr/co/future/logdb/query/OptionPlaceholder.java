package kr.co.future.logdb.query;

import java.text.ParseException;

import kr.co.future.bnf.Binding;
import kr.co.future.bnf.ParserContext;
import kr.co.future.bnf.Placeholder;
import kr.co.future.bnf.Result;
import kr.co.future.logdb.query.command.OptionChecker;

public class OptionPlaceholder implements Placeholder {
	@Override
	public Result eval(String text, int position, ParserContext ctx) throws ParseException {
		String str = text.substring(position).trim();
		if (!str.startsWith("(") || !str.contains("=") || !str.contains(")"))
			throw new ParseException("not option format", position);

		int eq = str.indexOf("=");
		int end = str.indexOf(")");
		if (end < eq)
			throw new ParseException("not option format", position);

		String key = str.substring(1, eq).trim();
		String value = str.substring(eq + 1, end).trim();
		if (key.contains("("))
			throw new ParseException("not option format", position);
		return new Result(new Binding(this, new OptionChecker(key, value)), text.indexOf(")", position) + 1);
	}
}
