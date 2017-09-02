package kr.co.future.sslvpn.auth.pdas;

import kr.co.future.api.FieldOption;
import kr.co.future.confdb.CollectionName;

@CollectionName("config")
public class PdasConfig {
	@FieldOption(nullable = false)
	private String dbHost;
	@FieldOption(nullable = false)
	private String dbName;
	@FieldOption(nullable = false)
	private String dbAccount;
	@FieldOption(nullable = false)
	private String dbPassword;
	@FieldOption(nullable = false)
	private String tableName;

	public String getDbHost() {
		return dbHost;
	}

	public void setDbHost(String dbHost) {
		this.dbHost = dbHost;
	}

	public String getDbName() {
		return dbName;
	}

	public void setDbName(String dbName) {
		this.dbName = dbName;
	}

	public String getDbAccount() {
		return dbAccount;
	}

	public void setDbAccount(String dbAccount) {
		this.dbAccount = dbAccount;
	}

	public String getDbPassword() {
		return dbPassword;
	}

	public void setDbPassword(String dbPassword) {
		this.dbPassword = dbPassword;
	}

	public String getTableName() {
		return tableName;
	}

	public void setTableName(String tableName) {
		this.tableName = tableName;
	}

	@Override
	public String toString() {
		return "host=" + dbHost + ", dbname=" + dbName + ", account=" + dbAccount + ", password=" + dbPassword + ", table="
				+ tableName;
	}
}
