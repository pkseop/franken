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
package kr.co.future.logdb.query;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArraySet;

import kr.co.future.log.api.LogParserFactoryRegistry;
import kr.co.future.logdb.DataSourceRegistry;
import kr.co.future.logdb.EmptyLogQueryCallback;
import kr.co.future.logdb.LogQuery;
import kr.co.future.logdb.LogQueryEventListener;
import kr.co.future.logdb.LogQueryParser;
import kr.co.future.logdb.LogQueryService;
import kr.co.future.logdb.LogQueryStatus;
import kr.co.future.logdb.LogScriptRegistry;
import kr.co.future.logdb.LookupHandlerRegistry;
import kr.co.future.logdb.SyntaxProvider;
import kr.co.future.logdb.query.parser.DatasourceParser;
import kr.co.future.logdb.query.parser.DropParser;
import kr.co.future.logdb.query.parser.EvalParser;
import kr.co.future.logdb.query.parser.FieldsParser;
import kr.co.future.logdb.query.parser.FulltextParser;
import kr.co.future.logdb.query.parser.FunctionParser;
import kr.co.future.logdb.query.parser.LookupParser;
import kr.co.future.logdb.query.parser.OptionCheckerParser;
import kr.co.future.logdb.query.parser.OptionParser;
import kr.co.future.logdb.query.parser.OutputCsvParser;
import kr.co.future.logdb.query.parser.RenameParser;
import kr.co.future.logdb.query.parser.ReplaceParser;
import kr.co.future.logdb.query.parser.ScriptParser;
import kr.co.future.logdb.query.parser.SearchParser;
import kr.co.future.logdb.query.parser.Sort2Parser;
import kr.co.future.logdb.query.parser.SortParser;
import kr.co.future.logdb.query.parser.Stats2Parser;
import kr.co.future.logdb.query.parser.StatsParser;
import kr.co.future.logdb.query.parser.TableParser;
import kr.co.future.logdb.query.parser.TermParser;
import kr.co.future.logdb.query.parser.TextFileParser;
import kr.co.future.logdb.query.parser.TimechartParser;
import kr.co.future.logdb.query.parser.TimechartParser2;
import kr.co.future.logdb.query.parser.ZipFileParser;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.apache.felix.ipojo.annotations.Validate;
import kr.co.future.logstorage.LogIndexer;
import kr.co.future.logstorage.LogStorage;
import kr.co.future.logstorage.LogTableRegistry;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(name = "logdb-query")
@Provides
public class LogQueryServiceImpl implements LogQueryService {
	private final Logger logger = LoggerFactory.getLogger(LogQueryServiceImpl.class);

	@Requires
	private DataSourceRegistry dataSourceRegistry;

	@Requires
	private LogStorage logStorage;

	@Requires
	private LogIndexer logIndexer;

	@Requires
	private LogTableRegistry tableRegistry;

	@Requires
	private SyntaxProvider syntaxProvider;

	@Requires
	private LookupHandlerRegistry lookupRegistry;

	@Requires
	private LogScriptRegistry scriptRegistry;

	@Requires
	private LogParserFactoryRegistry parserFactoryRegistry;

	private BundleContext bc;
	private ConcurrentMap<Integer, LogQuery> queries;

	private CopyOnWriteArraySet<LogQueryEventListener> callbacks;

	public LogQueryServiceImpl(BundleContext bc) {
		this.bc = bc;
		this.queries = new ConcurrentHashMap<Integer, LogQuery>();
		this.callbacks = new CopyOnWriteArraySet<LogQueryEventListener>();
	}

	@Validate
	public void start() {
		File logdbDir = new File(System.getProperty("kraken.data.dir"), "kraken-logdb/");
		if (!logdbDir.exists())
			logger.info("kraken logdb: create logdb directory [{}]", logdbDir.mkdir());

		File queryDir = new File(logdbDir, "query/");
		if (!queryDir.exists())
			logger.info("kraken logdb: create logdb query directory [{}]", queryDir.mkdir());

		@SuppressWarnings("unchecked")
		List<Class<? extends LogQueryParser>> parserClazzes = Arrays.asList(DropParser.class, EvalParser.class,
				SearchParser.class, FieldsParser.class, FunctionParser.class, OptionCheckerParser.class, OptionParser.class,
				RenameParser.class, ReplaceParser.class, SortParser.class, StatsParser.class, TermParser.class,
				TimechartParser.class, Stats2Parser.class, Sort2Parser.class, TimechartParser2.class);

		List<LogQueryParser> parsers = new ArrayList<LogQueryParser>();
		for (Class<? extends LogQueryParser> clazz : parserClazzes) {
			try {
				parsers.add(clazz.newInstance());
			} catch (Exception e) {
				logger.error("kraken logdb: failed to add syntax: " + clazz.getSimpleName(), e);
			}
		}

		// add table and lookup (need some constructor injection)
		parsers.add(new FulltextParser(logStorage, logIndexer));
		parsers.add(new DatasourceParser(dataSourceRegistry, logStorage, tableRegistry, parserFactoryRegistry));
		parsers.add(new TableParser(logStorage, tableRegistry, parserFactoryRegistry));
		parsers.add(new LookupParser(lookupRegistry));
		parsers.add(new ScriptParser(bc, scriptRegistry));
		parsers.add(new TextFileParser(parserFactoryRegistry));
		parsers.add(new ZipFileParser(parserFactoryRegistry));
		parsers.add(new OutputCsvParser());

		syntaxProvider.addParsers(parsers);

		// receive log table event and register it to data source registry
	}

	@Override
	public LogQuery createQuery(String query) {
		LogQuery lq = new LogQueryImpl(syntaxProvider, query);
		queries.put(lq.getId(), lq);
		lq.registerQueryCallback(new EofReceiver(lq));
		invokeCallbacks(lq, LogQueryStatus.Created);

		return lq;
	}

	@Override
	public void startQuery(int id) {
		LogQuery lq = getQuery(id);
		if (lq == null)
			throw new IllegalArgumentException("invalid log query id: " + id);

		new Thread(lq, "Log Query " + id).start();
		invokeCallbacks(lq, LogQueryStatus.Started);
	}

	@Override
	public void removeQuery(int id) {
		LogQuery lq = queries.remove(id);
		if (lq == null) {
			logger.debug("kraken logdb: query [{}] not found, remove failed", id);
			return;
		}

		try {
			lq.clearTimelineCallbacks();
			lq.clearQueryCallbacks();

			if (!lq.isEnd())
				lq.cancel();
		} catch (Throwable t) {
			logger.error("kraken logdb: cannot cancel query " + lq, t);
		}

		try {
			lq.purge();
		} catch (Throwable t) {
			logger.error("kraken logdb: cannot close file buffer list for query " + lq.getId(), t);
		}

		invokeCallbacks(lq, LogQueryStatus.Removed);
	}

	@Override
	public Collection<LogQuery> getQueries() {
		return queries.values();
	}

	@Override
	public LogQuery getQuery(int id) {
		return queries.get(id);
	}

	@Override
	public void addListener(LogQueryEventListener listener) {
		callbacks.add(listener);
	}

	@Override
	public void removeListener(LogQueryEventListener listener) {
		callbacks.remove(listener);
	}

	private void invokeCallbacks(LogQuery lq, LogQueryStatus status) {
		logger.debug("kraken logdb: invoking callback to notify query [{}], status [{}]", lq.getId(), status);
		for (LogQueryEventListener callback : callbacks) {
			try {
				callback.onQueryStatusChange(lq, status);
			} catch (Exception e) {
				logger.warn("kraken logdb: query event listener should not throw any exception", e);
			}
		}
	}

	private class EofReceiver extends EmptyLogQueryCallback {
		private LogQuery query;

		public EofReceiver(LogQuery query) {
			this.query = query;
		}

		@Override
		public void onEof() {
			invokeCallbacks(query, LogQueryStatus.Eof);
		}
	}
}
