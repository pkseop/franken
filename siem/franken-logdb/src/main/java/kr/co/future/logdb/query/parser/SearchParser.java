/*
 * Copyright 2011 Future Systems
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
import static kr.co.future.bnf.Syntax.ref;

import java.util.Arrays;
import java.util.Map;

import kr.co.future.bnf.Binding;
import kr.co.future.bnf.Syntax;
import kr.co.future.logdb.LogQueryParser;
import kr.co.future.logdb.query.command.Search;
import kr.co.future.logdb.query.command.Term;

public class SearchParser implements LogQueryParser {
	@Override
	public void addSyntax(Syntax syntax) {
		syntax.add("search", this, k("search "), ref("option"), ref("term"));
		syntax.addRoot("search");
	}

	@SuppressWarnings("unchecked")
	@Override
	public Object parse(Binding b) {
		Map<String, String> option = (Map<String, String>) b.getChildren()[1].getValue();
		Integer limit = null;

		if (option.containsKey("limit"))
			limit = Integer.parseInt(option.get("limit"));

		// List<Term> terms = (List<Term>) b.getChildren()[2].getValue();
		Term term = (Term) b.getChildren()[2].getValue();
		return new Search(limit, Arrays.asList(term));
	}
}
