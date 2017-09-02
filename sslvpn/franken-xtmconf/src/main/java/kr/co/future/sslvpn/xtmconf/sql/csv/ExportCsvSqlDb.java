package kr.co.future.sslvpn.xtmconf.sql.csv;

import java.io.File;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ExportCsvSqlDb extends AbstraceSqlDbCsv {
	private final Logger logger = LoggerFactory.getLogger(ExportCsvSqlDb.class.getName());
	
	private static String EXPORT_QUERY_FORMAT = "SELECT * FROM %s INTO OUTFILE '%s' FIELDS TERMINATED BY ',' ENCLOSED BY '\"' LINES TERMINATED BY '\n'";
	
	public List<String> exportCsv(String dir) {
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
		List<String> exportedFiles = new ArrayList<String>();
		try {
			st = conn.createStatement();
			for(String name : tablesNames) {
				if(isExcludedTable(name)) 
					continue;
				
		      String fileName = dir + "/" + CSV_FILE_PREFIX + name + ".csv";
		      File path = new File(dir);
		      if(!path.exists())
		      	path.mkdirs();
		      else {
			      File prevFile = new File(fileName);
			      if(prevFile.exists())	//delete previous csv file.
			      	prevFile.delete();
		      }
		      
		      String query = String.format(EXPORT_QUERY_FORMAT, name, fileName);
		      logger.debug("sql csv export query [{}]", query);
		      
		      st.executeQuery(query);
		      exportedFiles.add(fileName);
			}
		} catch (SQLException e) {
	      logger.error("error while export csv", e);
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
		return exportedFiles;
	}
}
