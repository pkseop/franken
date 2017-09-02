package kr.co.future.logdb.query.parser;

import static kr.co.future.bnf.Syntax.*;

import java.util.ArrayList;
import java.util.List;

import kr.co.future.bnf.Binding;
import kr.co.future.bnf.Syntax;
import kr.co.future.logdb.LogQueryParser;
import kr.co.future.logdb.LogScript;
import kr.co.future.logdb.LogScriptRegistry;
import kr.co.future.logdb.query.StringPlaceholder;
import kr.co.future.logdb.query.command.Script;

import org.osgi.framework.BundleContext;

public class ScriptParser implements LogQueryParser {
	private BundleContext bc;
	private LogScriptRegistry scriptRegistry;

	public ScriptParser(BundleContext bc, LogScriptRegistry scriptRegistry) {
		this.bc = bc;
		this.scriptRegistry = scriptRegistry;
	}

	@Override
	public void addSyntax(Syntax syntax) {
		syntax.add("script", this, k("script "), new StringPlaceholder(), repeat(rule(new StringPlaceholder())));
		syntax.addRoot("script");
	}

	@Override
	public Object parse(Binding b) {
		String name = (String) b.getChildren()[1].getValue();
		LogScript script = scriptRegistry.newScript("localhost", name, null);
		if (script == null)
			throw new IllegalArgumentException("log script not found: " + name);
		
		// TODO: parameter passing
		script.init(null);

		List<String> args = new ArrayList<String>();
		if (b.getChildren().length >= 3)
			parseArgs(args, b.getChildren()[2]);

		return new Script(bc, script, args.toArray(new String[0]));
	}

	private void parseArgs(List<String> args, Binding b) {
		if (b.getValue() != null)
			args.add((String) b.getValue());
		else {
			if (b.getChildren() != null) {
				for (Binding c : b.getChildren())
					parseArgs(args, c);
			}
		}
	}
}