package kr.co.future.sslvpn.xtmconf.system;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import kr.co.future.confdb.file.FileConfigDatabase;
import kr.co.future.msgbus.Session;
import kr.co.future.sslvpn.xtmconf.KLogWriter;
import kr.co.future.sslvpn.xtmconf.XtmConfig;
import kr.co.future.sslvpn.xtmconf.sql.csv.ExportCsvSqlDb;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Backup {
	private static final String OBJECT_DIR = XtmConfig.UTM_LOG + "policy_back/object";
	private static final String baseDir = "";

	public static File backup(Session session, String password) throws IOException, InterruptedException {
		Logger logger = LoggerFactory.getLogger(Backup.class.getName());
		
		logger.info("frodo xtmconf: start backup from session [{}]", session.getRemoteAddress());
		File file = backup(password);
		KLogWriter.write(0x12030013, session, "Backup Object");
		
		return file;
	}
	
	public static File backup(String password) throws IOException, InterruptedException {
		Logger logger = LoggerFactory.getLogger(Backup.class.getName());
		try {
			//logger.info("frodo xtmconf: start backup from session [{}]", session.getRemoteAddress());

			File backupPath = new File(baseDir + XtmConfig.UTM_LOG + "policy_back");
			final String attachFile = "object.zip";

			SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.KOREA);
			Date date = new Date();
			final String fileName = formatter.format(date) + ".gat";

			File objectDir = new File(backupPath.getAbsolutePath() + "/object");
			objectDir.mkdirs();

			BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(new File("/proc/utm/conf/confdev"))));
			String device = br.readLine().trim();
			if (br != null){
				try {
					br.close();
				} catch (IOException e) {
				}
			}

			logger.trace("frodo xtmconf: trying mount [{}]", device);
			CommandUtil.run(new File("/"), "mount", "-t", "ext3", device, "/utm/conf");

			logger.trace("frodo xtmconf: trying to copy xml");
			CommandUtil.run(backupPath, "cp", "-rf", "/etc/webadmin/xml", "/utm/conf");

			logger.trace("frodo xtmconf: trying to copy current conf dir");
			File src = new File(baseDir + "/utm/conf/");
			List<String> commands = new ArrayList<String>();
			commands.add("cp");
			commands.add("-rf");
			for (File backupConfFile : src.listFiles())
				commands.add(backupConfFile.getAbsolutePath());
			commands.add("object/");
			CommandUtil.run(new File(XtmConfig.UTM_LOG + "policy_back"), commands);

			logger.trace("frodo xtmconf: trying umount [{}]", device);
			CommandUtil.run(new File("/"), "umount", "/utm/conf");

			logger.trace("frodo xtmconf: trying to copy routing script files");
			List<String> commands2 = new ArrayList<String>();
			commands2.add("cp");
			commands2.add("-rf");
			File[] scripts = getFileList(new File("/etc/webadmin/conf"), "routing_script");
			for (File script : scripts)
				commands2.add(script.getAbsolutePath());
			commands2.add("object");
			CommandUtil.run(backupPath, commands2);

			logger.trace("frodo xtmconf: trying to copy local.pem");
			CommandUtil.run(backupPath, "cp", "-rf", "/utm/log/kraken/data/kraken-ca/CA/local/local.pem", "object");

			logger.trace("frodo xtmconf: trying to copy kraken data");
			String dataDir = System.getProperty("kraken.data.dir");
			File krakenData = new File(dataDir);

			CommandUtil.run(krakenData, "cp", "-a", "kraken-dom/", OBJECT_DIR + "/");

			new File(OBJECT_DIR + "/kraken-confdb").mkdirs();

			String[] dbNames = { "kraken-ldap", "kraken-dom", "kraken-dom-localhost", "frodo", "kraken-ca-local", "kraken-ca" };
			for (String dbName : dbNames) {
				logger.trace("frodo xtmconf: trying to export data [{}]", dbName);
				exportData(new File(krakenData.getAbsolutePath() + "/kraken-confdb"), OBJECT_DIR + "/kraken-confdb", dbName);
			}
			
			new File(OBJECT_DIR + "/KRAKEN_DB").mkdirs();
			ExportCsvSqlDb exportCsv = new ExportCsvSqlDb();
			exportCsv.exportCsv(OBJECT_DIR + "/KRAKEN_DB");
			
			// zip
			logger.trace("frodo xtmconf: trying to zip object dir");
			CommandUtil.run(backupPath, "zip", "-P", password, "-r", attachFile, "object/");

			// move object.zip to .gat file
			File src2 = new File(baseDir + XtmConfig.UTM_LOG + "policy_back/" + attachFile);
			File dest2 = new File(baseDir + XtmConfig.UTM_LOG + "policy_back/" + fileName);

			logger.trace("frodo xtmconf: trying to rename src [{}] to dest [{}]", src2.getAbsolutePath(), dest2.getAbsolutePath());
			if (!src2.renameTo(dest2)) {
				throw new RuntimeException("mv error: " + src2.getAbsolutePath() + "->" + dest2.getAbsolutePath());
			}

			logger.trace("frodo xtmconf: backup completed");
			//KLogWriter.write(0x12030013, session, "Backup Object");
			return dest2;
		} finally {
			logger.trace("frodo xtmconf: trying to delete " + XtmConfig.UTM_LOG + "policy_back/object/");
			File f3 = new File(baseDir + XtmConfig.UTM_LOG + "policy_back/object/");
			UpgradeLib.deleteDirectory(f3);
		}
	}

	private static void exportData(File dbRoot, String destination, String dbName) throws IOException {
		Logger logger = LoggerFactory.getLogger(Backup.class.getName());
		OutputStream os = null;
		File cdb = null;
		try {
			cdb = new File(destination, dbName + ".cdb");
			FileConfigDatabase fdb = new FileConfigDatabase(dbRoot, dbName);
			os = new FileOutputStream(cdb);
			fdb.exportData(os);
		} catch (IOException e) {
			logger.error("frodo xtmconf: cannot export file " + cdb.getAbsolutePath(), e);
			throw e;
		} finally {
			if (os != null)
				try {
					os.close();
				} catch (IOException e) {
				}
		}
	}

	private static File[] getFileList(File directory, final String keyword) {
		File[] files = directory.listFiles(new FilenameFilter() {
			public boolean accept(File dir, String name) {
				return name.startsWith(keyword);
			}
		});
		return files;
	}
}