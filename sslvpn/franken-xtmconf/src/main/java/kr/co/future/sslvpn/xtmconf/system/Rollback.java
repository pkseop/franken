package kr.co.future.sslvpn.xtmconf.system;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import kr.co.future.sslvpn.xtmconf.XtmConfig;

public class Rollback {
	public static String rollback(String type, String version) throws IOException, RuntimeException,
			InterruptedException {
		boolean searchFlag = false;
		String rollbackPath = XtmConfig.UTM_LOG + "upgrade/backup_db";
		File rollbackDir;
		File rollbackFile = null;

		if (type.equals("firmware") || type.equals("ramdisk") || type.equals("appimg") || type.equals("jre")
				|| type.equals("sslvpn")) {
			rollbackDir = new File(rollbackPath);
			File[] fileList = getFileList(rollbackDir, type);
			for (File file : fileList) {
				String upgradeDate = getUpgradeTime(file);
				if (upgradeDate.equals(version)) {
					rollbackFile = file;
					searchFlag = true;
					break;
				}
			}
		} else
			return type + " RollBack Fail. [reason : can't support this type]";

		if (rollbackFile == null)
			return type + " RollBack Fail. [reason : rollback file is not existed]";

		if (rollbackFile.exists() && searchFlag) {
			List<Object> fileType = Upgrade.getFileType(rollbackFile);
			if (UpgradeLib.applyToSystem(fileType, rollbackFile, 1)) {
				return type + " RollBack Complete. [version : " + version + "]";
			} else {
				return type + " RollBack Fail. [reason : apply to system fail]";
			}
		} else {
			return type + " RollBack Fail. [reason : rollback file is not existed]";
		}
	}

	public static Map<String, String> getCurrentVersion() throws IOException {
		String saveDir = XtmConfig.UTM_LOG + "upgrade/backup_db";
		String verInfo = saveDir + "/versioninfo";

		File f = new File(verInfo);
		BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(f)));
		Map<String, String> currentVersion = new HashMap<String, String>();
		String line;
		try{
			while ((line = br.readLine()) != null) {
				String[] token = line.split(":");
	
				if (token.length >= 2) {
					String key = token[0].trim();
					if (key.equals("dpipt") || key.equals("viruspt") || key.equals("userdb"))
						continue;
	
					String value = token[1].trim();
					if (value.equals("")) {
						currentVersion.put(key, null);
					} else {
						value = getUpgradeTime(new File(saveDir + "/" + key + "-" + token[1].trim()));
						currentVersion.put(key, value);
					}
				} else {
					String key = token[0].trim();
					if (key.equals("dpipt") || key.equals("viruspt") || key.equals("userdb"))
						continue;
	
					String value = null;
					currentVersion.put(key, value);
				}
			} 
		} finally{
			if (br != null){
				try {
					br.close();
				} catch (IOException e) {
				}
			}
		}
		return currentVersion;
	}

	public static Map<String, Set<String>> getRollbackList() {
		String saveDir = XtmConfig.UTM_LOG + "upgrade/backup_db";
		File dir = new File(saveDir);

		Map<String, Set<String>> rbMap;
		if (dir.exists() && dir.isDirectory()) {
			rbMap = new HashMap<String, Set<String>>();

			addRollbackList(rbMap, dir, "firmware");
			addRollbackList(rbMap, dir, "ramdisk");
			addRollbackList(rbMap, dir, "appimg");
			addRollbackList(rbMap, dir, "jre");
			addRollbackList(rbMap, dir, "sslvpn");
			return rbMap;
		} else
			return null;
	}

	private static void addRollbackList(Map<String, Set<String>> rbMap, File directory, String keyword) {
		File[] fs = getFileList(directory, keyword);
		Set<String> value = new HashSet<String>();
		for (File f : fs) {
			value.add(getUpgradeTime(f));
		}
		rbMap.put(keyword, value);
	}

	private static File[] getFileList(File directory, final String keyword) {
		File[] files = directory.listFiles(new FilenameFilter() {
			public boolean accept(File dir, String name) {
				return name.startsWith(keyword);
			}
		});
		return files;
	}

	public static String getUpgradeTime(File file) {
		long modTime = file.lastModified();
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd_HH:mm");
		return sdf.format(new Date(modTime));
	}
}