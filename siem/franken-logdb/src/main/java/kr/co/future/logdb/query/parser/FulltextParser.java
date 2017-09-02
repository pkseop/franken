/*
 * Copyright 2013 Future Systems
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

import java.util.Map;

import kr.co.future.bnf.Binding;
import kr.co.future.bnf.Syntax;
import kr.co.future.logdb.LogQueryParser;
import kr.co.future.logdb.query.StringPlaceholder;
import kr.co.future.logdb.query.command.Fulltext;

import kr.co.future.logstorage.LogIndexQuery;
import kr.co.future.logstorage.LogIndexer;
import kr.co.future.logstorage.LogStorage;

/**
 * @since 0.9
 * @author xeraph
 */
public class FulltextParser implements LogQueryParser {
	private LogStorage storage;
	private LogIndexer indexer;

	public FulltextParser(LogStorage storage, LogIndexer indexer) {
		this.storage = storage;
		this.indexer = indexer;
	}

	@Override
	public void addSyntax(Syntax syntax) {
		syntax.add("fulltext", this, k("fulltext "), ref("option"), new StringPlaceholder());
		syntax.addRoot("fulltext");
	}

	@SuppressWarnings("unchecked")
	@Override
	public Object parse(Binding b) {
		Map<String, String> option = (Map<String, String>) b.getChildren()[1].getValue();

		LogIndexQuery indexQuery = new LogIndexQuery();
		indexQuery.setTableName(option.get("table"));
		indexQuery.setIndexName(option.get("index"));

		// List<Term> terms = (List<Term>) b.getChildren()[2].getValue();
		indexQuery.setTerm((String) b.getChildren()[2].getValue());

		return new Fulltext(storage, indexer, indexQuery);
	}

}
