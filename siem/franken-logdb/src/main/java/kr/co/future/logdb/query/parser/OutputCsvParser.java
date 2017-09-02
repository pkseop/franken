package kr.co.future.logdb.query.parser;

import static kr.co.future.bnf.Syntax.k;
import static kr.co.future.bnf.Syntax.repeat;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;

import kr.co.future.bnf.Binding;
import kr.co.future.bnf.Syntax;
import kr.co.future.logdb.LogQueryParser;
import kr.co.future.logdb.query.StringPlaceholder;
import kr.co.future.logdb.query.command.OutputCsv;

public class OutputCsvParser implements LogQueryParser {

	@Override
	public void addSyntax(Syntax syntax) {
		syntax.add("outputcsv", this, k("outputcsv "), new StringPlaceholder(), repeat(new StringPlaceholder(new char[] { ' ',
				',' })));
		syntax.addRoot("outputcsv");
	}

	@Override
	public Object parse(Binding b) {
		List<String> fields = new ArrayList<String>();
		String csvPath = (String) b.getChildren()[1].getValue();

		Binding c = b.getChildren()[2];

		if (c.getValue() != null) {
			fields.add((String) c.getValue());

			if (c.getChildren() != null) {
				for (int i = 0; i < c.getChildren().length; i++)
					parse(c.getChildren()[i], fields);
			}
		} else {
			if (c.getChildren() != null) {
				int i = 0;

				for (; i < c.getChildren().length; i++)
					parse(c.getChildren()[i], fields);
			}
		}

		File csvFile = new File(csvPath);
		if (csvFile.exists())
			throw new IllegalStateException("csv file exists: " + csvFile.getAbsolutePath());

		try {
			csvFile.getParentFile().mkdirs();
			FileOutputStream os = new FileOutputStream(csvFile);
			return new OutputCsv(os, fields);
		} catch (FileNotFoundException e) {
			throw new IllegalStateException(e);
		}
	}

	private void parse(Binding b, List<String> fields) {
		if (b.getValue() != null)
			fields.add((String) b.getValue());

		if (b.getChildren() != null) {
			for (int i = 0; i < b.getChildren().length; i++)
				parse(b.getChildren()[i], fields);
		}
	}

}
