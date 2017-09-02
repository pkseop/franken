package kr.co.future.dom.api.impl;

import com.jolbox.bonecp.BoneCP;
import com.jolbox.bonecp.BoneCPConfig;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Invalidate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.concurrent.TimeUnit;

import kr.co.future.dom.api.MySQLConnectionService;

/**
 * Created by CRChoi on 2015-02-27.
 */
@Component(name = "mysql-connection-pool-service")
@Provides
public class MySQLConnectionServiceImpl implements MySQLConnectionService {
    private final Logger logger = LoggerFactory.getLogger(MySQLConnectionServiceImpl.class.getName());

    private BoneCP  connectionPool = null;

    @Validate
    public void start() {
        createDatabase();
        createConnectionPool();
        createOrganizationUnitTable();
        createUserTable();
        createUserExtensionTable();
        createAdminExtensionTable();
        createClientIpRangeTable();
        createPermissionTable();
        createProgramTable();
        createProgramProfileTable();
        createProgramProfileRelTable();
        createRoleTable();
        createRolePermissionTable();
        createProgramPackTable();
    }

    @Invalidate
    public void stop() {
        shutdownConnectionPool();
    }

    private void createDatabase() {
        try {
            Class.forName("com.mysql.jdbc.Driver");

            Connection conn = DriverManager.getConnection("jdbc:mysql://localhost/?user=root&password=WeGuardia_01");
            Statement s = conn.createStatement();
            int Result = s.executeUpdate("CREATE DATABASE IF NOT EXISTS KRAKEN_DB");
            logger.info("Check Kraken Database: OK");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (SQLException e) {
            e.printStackTrace();
        }

    }

    private void createConnectionPool() {
        String url = "jdbc:mysql://127.0.0.1:3306/KRAKEN_DB?autoReconnect=true";
        String id = "root";
        String pw = "WeGuardia_01";

        try {
            Class.forName("com.mysql.jdbc.Driver");
            createConnectionPool(url, id, pw);
        } catch (ClassNotFoundException e) {
            logger.error("mysql-connection-pool-service: error occurred during create connection pool", e);
        }
    }

    private void createConnectionPool(String url, String id, String pw) {
        if(connectionPool == null) {
            BoneCPConfig bonecpConfig = new BoneCPConfig();
            bonecpConfig.setJdbcUrl(url);
            bonecpConfig.setUsername(id);
            bonecpConfig.setPassword(pw);
            bonecpConfig.setPartitionCount(3);
            bonecpConfig.setMinConnectionsPerPartition(3);
            bonecpConfig.setMaxConnectionsPerPartition(10);
            bonecpConfig.setConnectionTimeout(30, TimeUnit.SECONDS);
            bonecpConfig.setIdleConnectionTestPeriodInMinutes(5);
            bonecpConfig.setConnectionTestStatement("SELECT 1");

            try {
                connectionPool = new BoneCP(bonecpConfig); // setup the connection pool
                String log = "mysql-connection-pool-service: kraken connection pool created.\n"
                        + "partition [{}], min [{}], max [{}], timeout(seconds) [{}], "
                        + "connection test period(minutes) [{}], connection test statement [{}]";
                logger.info(log, new Object[]{3, 3, 10, 30, 5, "SELECT 1"});
            } catch (SQLException e) {
                logger.error("mysql-connection-pool-service: create connection pool failed", e);
            }
        }
    }

    private void shutdownConnectionPool() {
        if(connectionPool != null) {
            connectionPool.shutdown();
            connectionPool = null;
            logger.info("mysql-connection-pool-service: connection pool shutdowned.");
        }
    }

    @Override
    public Connection getConnection() throws SQLException {
        if(connectionPool != null)
            return connectionPool.getConnection();

        return null;
    }

    private void createOrganizationUnitTable() {
        String query = "CREATE TABLE IF NOT EXISTS OrganizationUnit\n" +
                "(\n" +
                "  guid VARCHAR(40) PRIMARY KEY NOT NULL,\n" +
                "  name VARCHAR(60) NOT NULL,\n" +
                "  description VARCHAR(100),\n" +
                "  parent VARCHAR(40),\n" +
                "  source_type VARCHAR(10),\n" +
                "  created DATETIME NOT NULL,\n" +
                "  updated DATETIME NOT NULL\n" +
                ")";
        runQuery(query);

        String query2 = "CREATE UNIQUE INDEX unique_guid ON OrganizationUnit (guid)";
        runQuery(query2);

        logger.info("Check OrganizationUnit Table: OK");
    }

    private void createUserTable() {
        String query = "CREATE TABLE IF NOT EXISTS User\n" +
                "(\n" +
                "    loginName VARCHAR(60) PRIMARY KEY NOT NULL,\n" +
                "    guid VARCHAR(40),\n" +
                "    name VARCHAR(64) NOT NULL,\n" +
                "    description VARCHAR(250),\n" +
                "    password VARCHAR(64),\n" +
                "    salt VARCHAR(20),\n" +
                "    title VARCHAR(60),\n" +
                "    email VARCHAR(60),\n" +
                "    phone VARCHAR(60),\n" +
                "    lastPasswordChange TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,\n" +
                "    sourceType VARCHAR(30),\n" +
                "    created TIMESTAMP NOT NULL,\n" +
                "    updated TIMESTAMP NOT NULL\n" +
                ");\n";
        runQuery(query);

        logger.info("Check User Table: OK");
    }

    private void createUserExtensionTable() {
        String query = "CREATE TABLE IF NOT EXISTS UserExtension\n" +
                "(\n" +
                "    loginName VARCHAR(60) PRIMARY KEY NOT NULL,\n" +
                "    cid VARCHAR(40) NOT NULL,\n" +
                "    staticIp4 VARCHAR(20),\n" +
                "    allowIp4From VARCHAR(20),\n" +
                "    allowIp4To VARCHAR(20),\n" +
                "    isLocked TINYINT NOT NULL,\n" +
                "    loginFailures INT DEFAULT 0 NOT NULL,\n" +
                "    lastIp VARCHAR(20),\n" +
                "    vid VARCHAR(128),\n" +
                "    salt VARCHAR(20),\n" +
                "    idnHash VARCHAR(64),\n" +
                "    subjectDn VARCHAR(128),\n" +
                "    deviceAuthKey VARCHAR(40),\n" +
                "    deviceKeyCountSetting INT,\n" +
                "    expireDateTime TIMESTAMP NULL DEFAULT NULL,\n" +
                "    startDateTime TIMESTAMP NULL DEFAULT NULL,\n" +
                "    keyExpireDateTime TIMESTAMP  NULL DEFAULT NULL,\n" +
                "    lastLoginTime TIMESTAMP NULL DEFAULT NULL,\n" +
                "    lastLogoutTime TIMESTAMP NULL DEFAULT NULL,\n" +
                "    lastPasswordHash VARCHAR(128),\n" +
                "    lastPasswordChange TIMESTAMP NULL DEFAULT NULL,\n" +
                "    createDateTime TIMESTAMP NOT NULL,\n" +
                "    updateDateTime TIMESTAMP NOT NULL,\n" +
                "    accessProfileGuid VARCHAR(40),\n" +
                "    forcePasswordChange TINYINT DEFAULT 0 NOT NULL,\n" +
                "    lastPasswordFailTime TIMESTAMP NULL DEFAULT NULL,\n" +
                "    sourceType VARCHAR(30),\n" +
                "    certType VARCHAR(32),\n" +
                "    isAutoLocked TINYINT,\n" +
                "    autoLockReleasedTime TIMESTAMP NULL DEFAULT NULL,\n" +
                "    twowayAuthStatus INT,\n" +
                "    allowTimeTableId VARCHAR(40)\n" +
                ");\n";

        runQuery(query);

        logger.info("Check UserExtension Table: OK");
    }

    private void createAdminExtensionTable() {
        String query = "CREATE TABLE IF NOT EXISTS AdminExtension\n" +
                "(\n" +
                "    loginName VARCHAR(60) PRIMARY KEY NOT NULL,\n" +
                "    roleName VARCHAR(20) NOT NULL,\n" +
                "    programProfileName VARCHAR(20) NOT NULL,\n" +
                "    lang VARCHAR(16) NOT NULL,\n" +
                "    useLoginLock TINYINT NOT NULL,\n" +
                "    loginLockCount INT NOT NULL,\n" +
                "    loginFailures INT DEFAULT 0 NOT NULL,\n" +
                "    lastLoginFailedDateTime TIMESTAMP,\n" +
                "    useIdleTimeout TINYINT NOT NULL,\n" +
                "    idleTimeout INT NOT NULL,\n" +
                "    lastLoginDateTime TIMESTAMP,\n" +
                "    isEnabled TINYINT NOT NULL,\n" +
                "    useOtp TINYINT DEFAULT 0 NOT NULL,\n" +
                "    otpSeed VARCHAR(20),\n" +
                "    useAcl TINYINT DEFAULT 0 NOT NULL,\n" +
                "    created TIMESTAMP NOT NULL\n" +
                ");\n";

        runQuery(query);

        logger.info("Check AdminExtension Table: OK");
    }

    private void createClientIpRangeTable() {
        String query = "CREATE TABLE IF NOT EXISTS ClientIpRange\n" +
                "(\n" +
                "    loginName VARCHAR(60) NOT NULL,\n" +
                "    ipFrom VARCHAR(20) NOT NULL,\n" +
                "    ipTo VARCHAR(20) NOT NULL\n" +
                ");\n";

        runQuery(query);

        logger.info("Check ClientIpRange Table: OK");
    }

    private void createPermissionTable() {
        String query = "CREATE TABLE IF NOT EXISTS Permission\n" +
                "(\n" +
                "    groupName VARCHAR(60) NOT NULL,\n" +
                "    permission VARCHAR(60) NOT NULL,\n" +
                "    description VARCHAR(256)\n" +
                ");\n";

        runQuery(query);

        logger.info("Check Permission Table: OK");
    }

    private void createProgramTable() {
        String query = "CREATE TABLE IF NOT EXISTS Program\n" +
                "(\n" +
                "    name VARCHAR(60) PRIMARY KEY NOT NULL,\n" +
                "    pack VARCHAR(32) NOT NULL,\n" +
                "    description VARCHAR(256),\n" +
                "    path VARCHAR(256) NOT NULL,\n" +
                "    visible TINYINT NOT NULL,\n" +
                "    seq INT NOT NULL,\n" +
                "    created TIMESTAMP NOT NULL,\n" +
                "    updated TIMESTAMP NOT NULL\n" +
                ");\n";

        runQuery(query);

        logger.info("Check Program Table: OK");
    }

    private void createProgramProfileTable() {
        String query = "CREATE TABLE IF NOT EXISTS ProgramProfile\n" +
                "(\n" +
                "    name VARCHAR(60) PRIMARY KEY NOT NULL,\n" +
                "    description VARCHAR(256),\n" +
                "    created TIMESTAMP NOT NULL,\n" +
                "    updated TIMESTAMP NOT NULL\n" +
                ");\n";

        runQuery(query);

        logger.info("Check ProgramProfile Table: OK");
    }

    private void createProgramProfileRelTable() {
        String query = "CREATE TABLE IF NOT EXISTS ProgramProfileRel\n" +
                "(\n" +
                "    programProfileName VARCHAR(60) NOT NULL,\n" +
                "    programName VARCHAR(60) NOT NULL\n" +
                ");\n";

        runQuery(query);

        logger.info("Check ProgramProfileRel Table: OK");
    }

    private void createRoleTable() {
        String query = "CREATE TABLE IF NOT EXISTS Role\n" +
                "(\n" +
                "    name VARCHAR(60) PRIMARY KEY NOT NULL,\n" +
                "    level INT NOT NULL,\n" +
                "    created TIMESTAMP NOT NULL,\n" +
                "    updated TIMESTAMP NOT NULL\n" +
                ");\n";

        runQuery(query);

        logger.info("Check Role Table: OK");
    }

    private void createRolePermissionTable() {
        String query = "CREATE TABLE IF NOT EXISTS RolePermission\n" +
                "(\n" +
                "    roleName VARCHAR(60) NOT NULL,\n" +
                "    groupName VARCHAR(60) NOT NULL,\n" +
                "    permission VARCHAR(60) NOT NULL\n" +
                ");\n";

        runQuery(query);

        logger.info("Check RolePermission Table: OK");
    }
    
    private void createProgramPackTable() {
        String query = "CREATE TABLE IF NOT EXISTS ProgramPack\n" +
                "(\n" +
                "    name VARCHAR(60) PRIMARY KEY NOT NULL,\n" +
                "    dll VARCHAR(255) NOT NULL,\n" +
                "    description VARCHAR(60),\n" +
                "    starter VARCHAR(60),\n" +
                "    seq INT NOT NULL,\n" +
                "    created TIMESTAMP NOT NULL,\n" +
                "    updated TIMESTAMP NOT NULL\n" +
                ");\n";

        runQuery(query);

        logger.info("Check ProgramPack Table: OK");
    }

    private void runQuery(String query) {
        Connection con = null;
        PreparedStatement psmt = null;

        try {
            con = getConnection();

            if (con == null) {
                logger.error("Can't connect to database. check database status.");
                throw new Exception("database connect fail");
            }

            psmt = con.prepareStatement(query);
            psmt.executeUpdate();

            logger.debug("Create Table success!");
        } catch (SQLException e) {
//            if (!e.getMessage().contains("Duplicate key name")) {
//                logger.info(e.getMessage());
//                logger.error("MySQLConnectionServiceImpl Create Table error", e);
//                if (!e.getMessage().contains("already exists")) {
//
//                }
//            }
//            // || !e.getMessage().contains("already exists")
        } catch (Exception e) {
            logger.info(e.getMessage());
            logger.error("Exception Create Table error", e);
        } finally {
            close(psmt, con);
        }
    }

    private void close(PreparedStatement psmt, Connection con) {
        logger.trace("MySQLConnectionServiceImpl: close sql jdbc");
        if (psmt != null)
            try {
                psmt.close();
            } catch (SQLException e) {
            }
        if (con != null)
            try {
                con.close();
            } catch (SQLException e) {
            }
    }
}
