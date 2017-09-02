/*
 * Copyright 2012 Future Systems
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package kr.co.future.logdb.query.parser;

import static kr.co.future.bnf.Syntax.k;
import static kr.co.future.bnf.Syntax.option;
import static kr.co.future.bnf.Syntax.ref;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import kr.co.future.bnf.Binding;
import kr.co.future.bnf.Parser;
import kr.co.future.bnf.Syntax;
import kr.co.future.logdb.LogQueryParser;
import kr.co.future.logdb.query.StringPlaceholder;
import kr.co.future.logdb.query.command.Function;
import kr.co.future.logdb.query.command.Stats2;

public class Stats2Parser implements LogQueryParser {
	@Override
	public void addSyntax(Syntax syntax) {
		syntax.add("stats", this, k("stats "), ref("option"), ref("function"), option(k("by "), ref("stats_field")));
		syntax.add("stats_field", new StatsFieldParser(), new StringPlaceholder(new char[] { ' ', ',' }),
				option(ref("stats_field")));
		syntax.addRoot("stats");
	}

	@SuppressWarnings("unchecked")
	@Override
	public Object parse(Binding b) {
		List<String> keyFields = null;
		Function[] func = ((List<Function>) b.getChildren()[2].getValue()).toArray(new Function[0]);

		if (b.getChildren().length < 4)
			keyFields = new ArrayList<String>();
		else
			keyFields = (List<String>) b.getChildren()[3].getChildren()[1].getValue();

		return new Stats2(keyFields, func);
	}

	public class StatsFieldParser implements Parser {
		@Override
		public Object parse(Binding b) {
			List<String> fields = new ArrayList<String>();
			parse(b, fields);
			return fields;
		}

		@SuppressWarnings("unchecked")
		private void parse(Binding b, List<String> fields) {
			if (b.getValue() != null)
				fields.add((String) b.getValue());
			else {
				for (Binding c : b.getChildren()) {
					if (c.getValue() != null) {
						if (c.getValue() instanceof Collection)
							fields.addAll((List<? extends String>) c.getValue());
						else
							fields.add((String) c.getValue());
					} else
						parse(c, fields);
				}
			}
		}
	}
}
