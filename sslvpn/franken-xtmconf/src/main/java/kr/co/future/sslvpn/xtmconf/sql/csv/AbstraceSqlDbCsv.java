package kr.co.future.sslvpn.xtmconf.sql.csv;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AbstraceSqlDbCsv {
	private static final String DB_DRIVER = "com.mysql.jdbc.Driver";
	private static final String DB_CONNECTION = "jdbc:mysql://localhost:3306/KRAKEN_DB";
	private static final String DB_USER = "root";
	private static final String DB_PASSWORD = "WeGuardia_01";
	
	private static final String FETCH_DB_TABLES_NAMES_QUERY = "SELECT table_name FROM INFORMATION_SCHEMA.TABLES WHERE table_schema = 'KRAKEN_DB';";
	private static final String FETCH_TABLE_FIELD_NAMES = "SELECT COLUMN_NAME from INFORMATION_SCHEMA.COLUMNS where TABLE_NAME = '%s'";
	protected static final String CSV_FILE_PREFIX = "SSLplus_";
	
	protected static String[] EXCLUDED_TABLES = {"IpLease"};
	
	private final Logger logger = LoggerFactory.getLogger(AbstraceSqlDbCsv.class.getName());
	
	protected Connection getDBConnection() {
		Connection dbConnection = null;
		try {
			Class.forName(DB_DRIVER);
		} catch (ClassNotFoundException e) {
			logger.error("driver not found. connecting failed.", e);
			return null;
		}
		
		try {
			dbConnection = (Connection) DriverManager.getConnection(DB_CONNECTION, DB_USER,DB_PASSWORD);
		} catch (SQLException e) {
			logger.error("connecting db failed.", e);
		}
		return dbConnection;
	}
	
	protected List<String> getDBTablesNames(Connection conn) {
		Statement st = null;
		ResultSet rs = null;
		//Preparing List of table Names
      List <String> tableNameList = new ArrayList<String>();
      try {
	      st = conn.createStatement();
	      rs = st.executeQuery(FETCH_DB_TABLES_NAMES_QUERY);
	      
	      while(rs.next()) {
	          tableNameList.add(rs.getString(1));
	      }
      } catch (SQLException e) {
	      logger.error("failed to fetch names of tables", e);
      } finally {
      	try{
	      	if(rs != null)
	      		rs.close();
	      	if(st != null)
	      		st.close();
      	} catch (SQLException e) {
      		logger.error("error occurred while closing ResulteSet or Statement", e);
         }
      }
      
      return tableNameList;
	}
	
	protected List<String> getFieldNames(Connection conn, String table) {
		Statement st = null;
		ResultSet rs = null;
		//Preparing List of table Names
      List <String> fieldNames = new ArrayList<String>();
      String query = String.format(FETCH_TABLE_FIELD_NAMES, table);
      try {
	      st = conn.createStatement();
	      rs = st.executeQuery(query);
	      
	      while(rs.next()) {
	      	fieldNames.add(rs.getString(1));
	      }
      } catch (SQLException e) {
	      logger.error("failed to fetch names of tables", e);
      } finally {
      	try{
	      	if(rs != null)
	      		rs.close();
	      	if(st != null)
	      		st.close();
      	} catch (SQLException e) {
      		logger.error("error occurred while closing ResulteSet or Statement", e);
         }
      }
      
      return fieldNames;
	}
	
	protected boolean isExcludedTable(String tableName) {
		for(String s : EXCLUDED_TABLES) {
			if(tableName.equals(s))
				return true;
		}
		return false;
	}
}
