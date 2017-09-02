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

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Map;
import java.util.Properties;

import kr.co.future.bnf.Binding;
import kr.co.future.bnf.Syntax;
import kr.co.future.log.api.LogParser;
import kr.co.future.log.api.LogParserFactory;
import kr.co.future.log.api.LogParserFactoryRegistry;
import kr.co.future.log.api.LoggerConfigOption;
import kr.co.future.logdb.LogQueryParser;
import kr.co.future.logdb.query.StringPlaceholder;
import kr.co.future.logdb.query.command.Table;

import kr.co.future.logstorage.LogStorage;
import kr.co.future.logstorage.LogTableRegistry;

public class TableParser implements LogQueryParser {
	private LogStorage logStorage;
	private LogTableRegistry tableRegistry;
	private LogParserFactoryRegistry parserFactoryRegistry;

	public TableParser(LogStorage logStorage, LogTableRegistry tableRegistry, LogParserFactoryRegistry parserFactoryRegistry) {
		this.logStorage = logStorage;
		this.tableRegistry = tableRegistry;
		this.parserFactoryRegistry = parserFactoryRegistry;
	}

	@Override
	public void addSyntax(Syntax syntax) {
		syntax.add("table", this, k("table "), ref("option"), new StringPlaceholder());
		syntax.addRoot("table");
	}

	@SuppressWarnings("unchecked")
	@Override
	public Object parse(Binding b) {
		Map<String, String> options = (Map<String, String>) b.getChildren()[1].getValue();
		String tableName = (String) b.getChildren()[2].getValue();
		Date from = null;
		Date to = null;
		int offset = 0;
		int limit = 0;

		if (options.containsKey("duration")) {
			String duration = options.get("duration");
			int i;
			for (i = 0; i < duration.length(); i++) {
				char c = duration.charAt(i);
				if (!('0' <= c && c <= '9'))
					break;
			}
			int value = Integer.parseInt(duration.substring(0, i));
			from = getDuration(value, duration.substring(i));
		}

		if (options.containsKey("from"))
			from = getDate(options.get("from"));
		if (options.containsKey("to"))
			to = getDate(options.get("to"));

		if (options.containsKey("offset"))
			offset = Integer.parseInt(options.get("offset"));
		if (options.containsKey("limit"))
			limit = Integer.parseInt(options.get("limit"));

		String parserName = tableRegistry.getTableMetadata(tableName, "logparser");
		LogParserFactory parserFactory = parserFactoryRegistry.get(parserName);
		LogParser parser = null;
		if (parserFactory != null) {
			Properties prop = new Properties();
			for (LoggerConfigOption configOption : parserFactory.getConfigOptions()) {
				String optionName = configOption.getName();
				String optionValue = tableRegistry.getTableMetadata(tableName, optionName);
				if (optionValue == null)
					throw new IllegalArgumentException("require table metadata " + optionName);
				prop.put(optionName, optionValue);
			}
			parser = parserFactory.createParser(prop);
		}
		Table table = new Table(tableName, offset, limit, from, to, parser);
		table.setStorage(logStorage);

		return table;
	}

	private Date getDuration(int value, String field) {
		Calendar c = Calendar.getInstance();
		c.setTimeInMillis(System.currentTimeMillis());
		if (field.equalsIgnoreCase("s"))
			c.add(Calendar.SECOND, -value);
		else if (field.equalsIgnoreCase("m"))
			c.add(Calendar.MINUTE, -value);
		else if (field.equalsIgnoreCase("h"))
			c.add(Calendar.HOUR_OF_DAY, -value);
		else if (field.equalsIgnoreCase("d"))
			c.add(Calendar.DAY_OF_MONTH, -value);
		else if (field.equalsIgnoreCase("w"))
			c.add(Calendar.WEEK_OF_YEAR, -value);
		else if (field.equalsIgnoreCase("mon"))
			c.add(Calendar.MONTH, -value);
		return c.getTime();
	}

	private Date getDate(String value) {
		String type1 = "yyyy";
		String type2 = "yyyyMM";
		String type3 = "yyyyMMdd";
		String type4 = "yyyyMMddHH";
		String type5 = "yyyyMMddHHmm";
		String type6 = "yyyyMMddHHmmss";

		SimpleDateFormat sdf = null;
		if (value.length() == 4)
			sdf = new SimpleDateFormat(type1);
		else if (value.length() == 6)
			sdf = new SimpleDateFormat(type2);
		else if (value.length() == 8)
			sdf = new SimpleDateFormat(type3);
		else if (value.length() == 10)
			sdf = new SimpleDateFormat(type4);
		else if (value.length() == 12)
			sdf = new SimpleDateFormat(type5);
		else if (value.length() == 14)
			sdf = new SimpleDateFormat(type6);

		if (sdf == null)
			throw new IllegalArgumentException();

		try {
			return sdf.parse(value);
		} catch (ParseException e) {
			return null;
		}
	}
}
