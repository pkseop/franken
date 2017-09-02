package kr.co.future.dom.api;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Created by CRChoi on 2015-02-27.
 */
public interface MySQLConnectionService {
    public Connection getConnection() throws SQLException;
}
