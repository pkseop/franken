package kr.co.future.sslvpn.xtmconf.sql.csv;

import java.io.File;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ImportCsvSqlDb extends AbstraceSqlDbCsv {
	private final Logger logger = LoggerFactory.getLogger(ImportCsvSqlDb.class.getName());
	
	private static String TRUNCATE_TABLE_QUERY = "TRUNCATE TABLE %s";
	private static String IMPORT_QUERY_FORMAT = "LOAD DATA INFILE '%s' REPLACE INTO TABLE %s FIELDS TERMINATED BY ',' ENCLOSED BY '\"' LINES TERMINATED BY '\n'";
	
	public List<String> importCsv(String dir) {
		Connection conn = getDBConnection();
		if(conn == null)
			return null;
		
		List<String> tablesNames = getDBTablesNames(conn);
		if(tablesNames.size() == 0) {
			try {
	         conn.close();
         } catch (SQLException e) {
	         logger.error("close connection error", e);
         }
			return null;
		}
		
		
		Statement st = null;
		List<String> importedFiles = new ArrayList<String>();
		try {
			st = conn.createStatement();
			for(String name : tablesNames) {
				if(isExcludedTable(name)) 
					continue;
				
				String fileName = dir + "/" + CSV_FILE_PREFIX + name + ".csv";
				logger.debug("import file [{}]", fileName);
				File file = new File(fileName);
				if(file.exists()) {
					//delete all data in the table.
					String truncQuery = String.format(TRUNCATE_TABLE_QUERY, name);
					logger.debug("truncate query [{}]", truncQuery);
					st.addBatch(truncQuery);
					//insert all csv data to the table.
					String query = String.format(IMPORT_QUERY_FORMAT, fileName, name);
					logger.debug("import query [{}]", query);
					st.addBatch(query);

					st.executeBatch();
					
					importedFiles.add(fileName);
				}
			}
		} catch (SQLException e) {
	      logger.error("error while import csv", e);
	      return null;
      } finally {
      	try {
	      	if(st != null)
	      		st.close();
	      	 conn.close();
      	} catch (SQLException e) {
	         logger.error("close connection error", e);
	         return null;
         }
      }
		return importedFiles;
	}
}
